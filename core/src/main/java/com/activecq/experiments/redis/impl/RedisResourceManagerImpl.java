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
        label = "ActiveCQ Experiments - Redis Manager",
        description = "Redis resource manager.",
        enabled = false,
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

    private final static String AUTO_CHILD_NODE_NAME_PREFIX = "node-";
    private final static String AUTO_CHILD_INDICATOR = "*";


    private String host = "localhost";
    private int port = 6379;

    private int redisDB = 10;

    private boolean immediateSave = false;

    @Reference
    private RedisConnectionPool redisConnectionPool;

    public Jedis getJedis() {
        final Jedis jedis = this.redisConnectionPool.getJedis();

        jedis.select(this.getRedisDB());
        return jedis;
    }

    public void returnJedis(final Jedis jedis) {
        this.redisConnectionPool.returnJedis(jedis);
    }

    public int getRedisDB() {
        return this.redisDB;
    }

    public void setRedisDB(final int redisDB) {
        this.redisDB = redisDB;
    }

    public String getResourceKey(final String path) {
        return REDIS_KEY_PREFIX_RESOURCES + DEFAULT_WORKSPACE + REDIS_KEY_PREFIX_DELIMITER + path;
    }

    public String getChildrenKey(final String path) {
        return REDIS_KEY_PREFIX_CHILDREN + DEFAULT_WORKSPACE + REDIS_KEY_PREFIX_DELIMITER + path;
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
     *
     * @param obj
     * @return
     */
    private String toString(Object obj) {
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
