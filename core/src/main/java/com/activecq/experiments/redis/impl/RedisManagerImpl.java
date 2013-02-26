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

import com.activecq.experiments.redis.RedisManager;
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
        description = "Redis resource mananger.",
        metatype = true,
        immediate = true)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ActiveCQ",
                propertyPrivate = true)
})
@Service
public class RedisManagerImpl implements RedisManager {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Jedis jedis;
    private String host = "localhost";
    private int port = 6379;

    public Jedis getJedis() {
        return this.jedis;
    }

    public String getResourceKey(final String path) {
        return REDIS_KEY_PREFIX_RESOURCES + DEFAULT_WORKSPACE + "::" + path;
    }

    public String getChildrenKey(final String path) {
        return REDIS_KEY_PREFIX_CHILDREN + DEFAULT_WORKSPACE + "::" + path;
    }

    @Override
    public Set<String> getChildren(String path) {
        log.debug("Getting children using: " + this.getChildrenKey(path));
        return this.getJedis().smembers(this.getChildrenKey(path));
    }

    /**
     *
     * @param path
     * @param map
     */
    public void addResource(final String path, Map<String, ? extends Object> map) {
        this.addChildren(path);

        final String key = this.getResourceKey(path);

        for(final String property : map.keySet()) {
            final Object obj = map.get(property);
            if(obj instanceof byte[]) {
                final byte[] bytes = (byte[]) obj;
                this.getJedis().hset(key.getBytes(), property.getBytes(), bytes);
            } else {
                this.getJedis().hset(key, property, this.toString(obj));
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
    public void removeResource(final String path) {
        for(final String child : this.getChildren(path)) {
            removeResource(child);
        }

       this.getJedis().del(this.getResourceKey(path), this.getChildrenKey(path));
    }

    @Override
    public boolean resourceExists(final String path) {
        return this.getJedis().exists(this.getResourceKey(path));
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
    private void addChildren(final String path) {
        String[] segments = StringUtils.split(path, "/");

        final ArrayList<String> builder = new ArrayList<String>();

        for(final String segment : segments) {
            final String key = this.getChildrenKey("/" + StringUtils.join(builder, "/"));

            builder.add(segment);

            final String value = "/" + StringUtils.join(builder, "/");

            if(!this.getJedis().exists(this.getResourceKey(value))) {
                this.getJedis().hset(this.getResourceKey(value), JcrConstants.JCR_PRIMARYTYPE, "redis:hash");
            }

            this.getJedis().sadd(key, value);
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
        log.debug("Activate");
        configure(componentContext);

    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
    }

    private void configure(final ComponentContext componentContext) {
        final Map<String, String> properties = (Map<String, String>) componentContext.getProperties();

        this.jedis = new Jedis(host, port);
        log.debug("Jedis ping: " + this.jedis.ping());

        String random = "/var/redis/testing";// + String.valueOf(new Random().nextInt());

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("jcr:title", "this is a string");
        map.put("false", new Boolean(false));
        map.put("true", new Boolean(true));
        map.put("date", new Date());
        map.put("int", 10);
        map.put("long", 100L);
        map.put("double", 1000D);
        map.put("float", new Float(10000));

        this.addResource(random, map);
    }

}
