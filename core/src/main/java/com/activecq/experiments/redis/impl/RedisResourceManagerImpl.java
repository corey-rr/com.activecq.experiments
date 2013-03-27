/*
 * Copyright 2012 david gonzalez.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.activecq.experiments.redis.impl;

import com.activecq.experiments.redis.RedisConnectionPool;
import com.activecq.experiments.redis.RedisResourceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.jackrabbit.JcrConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * User: david
 */

@Component(
        label = "Experiments - Redis Manager",
        description = "Redis resource manager.",
        enabled = true,
        metatype = false,
        immediate = false)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ActiveCQ",
                propertyPrivate = true)
})
@Service
public class RedisResourceManagerImpl implements RedisResourceManager {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private boolean immediateSave = false;

    @Reference
    private RedisConnectionPool redisConnectionPool;

    public Jedis getJedis() {
        return this.redisConnectionPool.getJedis();
    }

    public void returnJedis(final Jedis jedis) {
        this.redisConnectionPool.returnJedis(jedis);
    }

    public String getRedisKey(final String keyType, final String key) {
        return keyType + DEFAULT_WORKSPACE + REDIS_KEY_PREFIX_DELIMITER + key;
    }

    public String getResourceKey(final String path) {
        return this.getRedisKey(REDIS_KEY_PREFIX_RESOURCES, path);
    }

    public String getChildrenKey(final String path) {
        return this.getRedisKey(REDIS_KEY_PREFIX_CHILDREN, path);
    }

    @Override
    public Set<String> getChildren(String path) {
        final Jedis jedis = this.getJedis();

        try {
            return jedis.smembers(this.getChildrenKey(path));
        } finally {
            this.returnJedis(jedis);
        }
    }

    /**
     *
     * @param path
     * @param map
     */
    public String addResource(String path, Map<String, ? extends Object> map) {
        int result = 0;
        final Jedis jedis = this.getJedis();

        if(StringUtils.endsWith(path, "/" + AUTO_CHILD_INDICATOR)) {
            final String tmp = StringUtils.removeEnd(path, AUTO_CHILD_INDICATOR);
            path = tmp + AUTO_CHILD_NODE_NAME_PREFIX + UUID.randomUUID();
        }

        try {
            result += this.addChildren(path);

            final String key = this.getResourceKey(path);

            for(final String property : map.keySet()) {
                final Object obj = map.get(property);

                if(obj instanceof byte[]) {
                    final byte[] bytes = (byte[]) obj;
                    result += jedis.hset(key.getBytes(), property.getBytes(), bytes);
                } else {
                    result += jedis.hset(key, property, this.toString(obj));
                }
            }

            this.index(path, map);
            this.save(jedis);

        } finally {
            this.returnJedis(jedis);
        }

        if(result > 0) {
            return path;
        } else {
            return null;
        }
    }

    /**
     * @param path
     * @param map
     */
    public boolean modifyResource(String path, Map<String, ? extends Object> map) {
        if (StringUtils.endsWith(path, "/" + AUTO_CHILD_INDICATOR)) {
            throw new IllegalArgumentException("Cannot update a auto-generation path");
        }

        if(!this.resourceExists(path)) {
            return this.addResource(path, map) != null;
        } else {
            final Jedis jedis = this.getJedis();

            try {
                final String key = this.getResourceKey(path);

                if(map.keySet().isEmpty()) {
                    for (final String s : jedis.hkeys(key)) {
                        jedis.hdel(key, s);
                    }
                } else {
                    jedis.del(key);

                    for (final String property : map.keySet()) {
                        final Object obj = map.get(property);

                        if (obj instanceof byte[]) {
                            final byte[] bytes = (byte[]) obj;
                            jedis.hset(key.getBytes(), property.getBytes(), bytes);
                        } else {
                            jedis.hset(key, property, this.toString(obj));
                        }
                    }
                }

                this.reindex(path, map);
                this.save(jedis);

                return true;
            } finally {
                this.returnJedis(jedis);
            }
        }
    }


    /**
     * /a/b => { /a/b/c }
     * /a/b/c => { /a/b/c/d }
     *
     *
     * @param path
     * @return
     */
    public boolean removeResource(final String path) {
        int result = 0;

        for(final String child : this.getChildren(path)) {
            if(removeResource(child)) {
                result++;
            }
        }

        final Jedis jedis = this.getJedis();

        try {
            result += jedis.del(this.getResourceKey(path), this.getChildrenKey(path));

            /**
             * Remove dangling children records for parents that now have no children
             * (the removed node was the parents only child)
             */
            String parentPath = StringUtils.substringBeforeLast(path, "/");
            if(StringUtils.isBlank(parentPath)) {
                parentPath = "/";
            }

            if(jedis.exists(this.getChildrenKey(parentPath))) {
                if(jedis.smembers(this.getChildrenKey(parentPath)).size() > 1) {
                    // Remove the path from the set
                    result += jedis.srem(this.getChildrenKey(parentPath), path);
                } else {
                    result += jedis.del(this.getChildrenKey(parentPath));
                }
            }

            this.deindex(path);
            this.save(jedis);

        } finally {
            this.returnJedis(jedis);
        }

        return result > 0;
    }

    @Override
    public Map<String, String> getResourceProperties(final String path) {
        final Jedis jedis = this.getJedis();

        try {
            return jedis.hgetAll(this.getResourceKey(path));
        } finally {
            this.returnJedis(jedis);
        }
    }

    @Override
    public boolean resourceExists(final String path) {
        final Jedis jedis = this.getJedis();

        try {
            return jedis.exists(this.getResourceKey(path));
        } finally {
            this.returnJedis(jedis);
        }
    }

    @Override
    public String getWorkspace() {
        return DEFAULT_WORKSPACE;
    }

    /**
     * For path "/a/b/c/d"
     * Create the following ZSETS
     *
     * cq::children::crx.default::/ /a
     * cq::children::crx.default::/a /b
     * cq::children::crx.default::/b /c
     * cq::children::crx.default::/c /d
     *
     * @param path
     * @return
     */
    private int addChildren(final String path) {
        final Jedis jedis = this.getJedis();
        int result = 0;

        try {
            String[] segments = StringUtils.split(path, "/");

            final ArrayList<String> builder = new ArrayList<String>();

            for(final String segment : segments) {
                final String key = this.getChildrenKey("/" + StringUtils.join(builder, "/"));

                builder.add(segment);

                final String value = "/" + StringUtils.join(builder, "/");

                if(!jedis.exists(this.getResourceKey(value))) {
                    // Create missing "middle nodes"
                    result += jedis.hset(this.getResourceKey(value), JcrConstants.JCR_PRIMARYTYPE, REDIS_JCR_PRIMARY_TYPE);
                }

                result += jedis.sadd(key, value);

                this.save(jedis);
            }
        } finally {
            this.returnJedis(jedis);
        }

        return result;
    }

    private void save(final Jedis jedis) {
        if(this.immediateSave) {
            jedis.bgsave();
       }
    }

    /**
     * SEARCH
     */

    public List<String> search(final String term) {
        List<String> results = new ArrayList<String>();

        final Jedis jedis = this.getJedis();
        try {
            final String key = this.getRedisKey(FULLTEXT, term);
            for(final String path : jedis.smembers(key)) {
                results.add(path);
            }
        } finally {
            this.returnJedis(jedis);
        }

        return results;
    }

    protected int index(final String path, final Map<String, ? extends Object> data) {
        return fulltextIndex(path, data);
    }

    private int fulltextIndex(final String path, final Map<String, ? extends Object> data) {
        int count = 0;

        String tmp = "";

        for (Object value : data.values()) {
            if (value instanceof String) {
                tmp += ((String) value).toLowerCase();
            } else if (value instanceof Boolean) {
                tmp += ((Boolean) value).toString();
            } else if (value instanceof Integer) {
                tmp += ((Integer) value).toString();
            } else if (value instanceof Date) {
                tmp += ((Date) value).toString();
            } else if (value instanceof Double) {
                tmp += ((Double) value).toString();
            } else if (value instanceof Float) {
                tmp += ((Float) value).toString();
            } else if (value instanceof Long) {
                tmp += ((Long) value).toString();
            } else {
                continue;
            }

            tmp += " ";
        }

        final String[] terms = StringUtils.split(tmp, ' ');

        log.debug("Terms: {}", terms);

        if (terms.length == 0) {
            return count;
        }

        final Jedis jedis = this.getJedis();
        try {
            for (final String term : terms) {
                final String fulltextKey = this.getRedisKey(FULLTEXT, term);
                final String fulltextLookupKey = this.getRedisKey(FULLTEXT_LOOKUP, path);

                jedis.sadd(fulltextKey, path);
                jedis.sadd(fulltextLookupKey, term);
                count++;
            }
        } finally {
            this.returnJedis(jedis);
        }

        return count;
    }

    protected int deindex(final String path) {
        return fulltextDeindex(path);
    }

    private int fulltextDeindex(final String path) {
        int count = 0;

        final Jedis jedis = this.getJedis();
        try {
            final String fulltextLookupKey = this.getRedisKey(FULLTEXT_LOOKUP, path);
            final Set<String> terms = jedis.smembers(fulltextLookupKey);

            for (final String term : terms) {
                final String fulltextKey = this.getRedisKey(FULLTEXT, term);
                jedis.srem(fulltextKey, path);
                count++;
            }

            jedis.del(fulltextLookupKey);
        } finally {
            this.returnJedis(jedis);
        }

        return count;
    }

    protected int reindex(final String path, final Map<String, ? extends Object> data) {
        return fulltextReindex(path, data);
    }

    private int fulltextReindex(final String path, final Map<String, ? extends Object> data) {
        this.fulltextDeindex(path);
        return this.fulltextIndex(path, data);
    }

    /**
     *
     * @param obj
     * @return
     */
    public String toString(Object obj) {
        if (obj instanceof Date) {
            final Date tmp = (Date) obj;
            return tmp.toString();
        } else if (obj instanceof Boolean) {
            final Boolean tmp = (Boolean) obj;
            return String.valueOf(tmp);
        } else if (obj instanceof Integer) {
            final Integer tmp = (Integer) obj;
            return String.valueOf(tmp);
        } else if (obj instanceof Short) {
            final Short tmp = (Short) obj;
            return String.valueOf(tmp);
        } else if (obj instanceof Double) {
            final Double tmp = (Double) obj;
            return String.valueOf(tmp);
        } else if (obj instanceof Long) {
            final Long tmp = (Long) obj;
            return String.valueOf(tmp);
        } else if (obj instanceof Float) {
            final Float tmp = (Float) obj;
            return String.valueOf(tmp);
        } else if (obj instanceof Byte) {
            final Byte tmp = (Byte) obj;
            return String.valueOf(tmp);
        } else if (obj instanceof Character) {
            final Character tmp = (Character) obj;
            return String.valueOf(tmp);
        } else if(obj instanceof String) {
            return (String) obj;
        } else {
            return String.valueOf(obj);
        }
    }

    /**
     * OSGi Component Methods *
     */
    @Activate
    protected void activate(final ComponentContext componentContext) throws Exception {
        log.error("Activate");
        configure(componentContext);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
    }

    private void configure(final ComponentContext componentContext) {
        final Map<String, String> properties = (Map<String, String>) componentContext.getProperties();

        final Jedis configJedis = this.getJedis();

        /* Ping Redis on Startup */

        try {
            log.info("Redis ping: " + configJedis.ping());
        } finally {
            this.returnJedis(configJedis);
        }
    }
}
