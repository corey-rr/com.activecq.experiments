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
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Map;

/**
 * User: david
 */

@Component(
        label = "Experiments - Redis Connection Pool Manager",
        description = "Redis connection pool manager.",
        metatype = true,
        immediate = false)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ActiveCQ",
                propertyPrivate = true)
})
@Service
public class RedisConnectionPoolImpl implements RedisConnectionPool {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Private fields
     */

    private JedisPool jedisPool;

    /**
     * OSGi Properties *
     */

    // Host
    private static final String DEFAULT_HOST = "localhost";
    private String host = DEFAULT_HOST;
    @Property(label = "Redis Host",
            description = "Host or IP to connect to Redis DB",
            value = DEFAULT_HOST)
    private static final String PROP_HOST = "prop.host";

    // Port
    private static final int DEFAULT_PORT = 6379;
    private int port = DEFAULT_PORT;
    @Property(label = "Redis Port",
            description = "Post to connect to Redis DB",
            intValue = DEFAULT_PORT)
    private static final String PROP_PORT = "prop.port";

    // Redis DB
    private static final int DEFAULT_REDISDB = 0;
    private int redisDB = DEFAULT_REDISDB;
    @Property(label = "Default Redis DB",
            description = "Default Redis DB to try to connect to. Standard config only allows for values 1 - 15.",
            intValue = DEFAULT_REDISDB)
    private static final String PROP_REDISDB = "prop.redisdb";

    // Max Active Connections
    private static final int DEFAULT_MAX_ACTIVE = 100;
    private int maxActive = DEFAULT_MAX_ACTIVE;
    @Property(label = "Max Active Connections",
            description = "",
            intValue = DEFAULT_MAX_ACTIVE)
    private static final String PROP_MAX_ACTIVE = "prop.max-active";

    // Min Idle Connections
    private static final int DEFAULT_MIN_IDLE = 100;
    private int minIdle = DEFAULT_MIN_IDLE;
    @Property(label = "Min Idle Connections",
            description = "",
            intValue = DEFAULT_MIN_IDLE)
    private static final String PROP_MIN_IDLE = "prop.min-idle";

    // Max Idle Connections
    private static final int DEFAULT_MAX_IDLE = 100;
    private int maxIdle = DEFAULT_MAX_IDLE;
    @Property(label = "Max Idle Connections",
            description = "",
            intValue = DEFAULT_MAX_IDLE)
    private static final String PROP_MAX_IDLE = "prop.max-idle";

    // Max Wait
    private static final int DEFAULT_MAX_WAIT = 10;
    private int maxWait = DEFAULT_MAX_WAIT;
    @Property(label = "Max Wait Time",
            description = "",
            intValue = DEFAULT_MAX_WAIT)
    private static final String PROP_MAX_WAIT = "prop.max-wait";

    // Test on Borrow
    private static final boolean DEFAULT_TEST_ON_BORROW = true;
    private boolean testOnBorrow = DEFAULT_TEST_ON_BORROW;
    @Property(label = "Test on borrow",
            description = "",
            boolValue = DEFAULT_TEST_ON_BORROW)
    private static final String PROP_TEST_ON_BORROW = "prop.test-on-borrow";


    private static final String DEFAULT_AOFSYNC = "everysec";
    private String aofsync = DEFAULT_AOFSYNC;
    @Property(label = "AOF Fsync",
            description = "",
            cardinality = 1,
            options = {
                    @PropertyOption(value = "Every second", name = "everysec")
            }
    )
    private static final String PROP_AOFSYNC = "prop.aofsync";

    private static final String[] DEFAULT_SNAPSHOTS = new String[] { "300 1" };
    private String[] snapshots = DEFAULT_SNAPSHOTS ;
    @Property(label = "Snapshots",
            description = "",
            cardinality = 10000

    )
    private static final String PROP_SNAPSHOTS = "prop.snapshots";

    /**
     * Methods
     */

    /**
     *
     * @param jedisPool
     */
    public void setJedisPool(final JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public Jedis getJedis() {
        return this.getJedis(this.redisDB);
    }

    public Jedis getJedis(final int redisDB) throws JedisConnectionException {
        final Jedis jedis = this.jedisPool.getResource();

        if(!jedis.isConnected()) {
            jedis.connect();
        }

        return jedis;


/*        if(jedis.select(redisDB) != null) {
            return jedis;
        } else {
            // TODO HANDLE OTHER ERROR CODES
            throw new JedisConnectionException("Could not connect to Redis DB: " + redisDB);
        }
        */
    }

    @Override
    public void returnJedis(Jedis jedis) {
        this.jedisPool.returnResource(jedis);
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        configure(ctx);
    }


    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        if(this.jedisPool != null) {
            this.jedisPool.destroy();
            this.jedisPool = null;
        }

    }

    private void configure(final ComponentContext componentContext) {
        final Map<String, String> properties = (Map<String, String>) componentContext.getProperties();

        this.host = PropertiesUtil.toString(properties.get(PROP_HOST), DEFAULT_HOST);
        this.port = PropertiesUtil.toInteger(properties.get(PROP_PORT), DEFAULT_PORT);
        this.redisDB = PropertiesUtil.toInteger(properties.get(PROP_REDISDB), DEFAULT_REDISDB);

        this.maxActive = PropertiesUtil.toInteger(properties.get(PROP_MAX_ACTIVE), DEFAULT_MAX_ACTIVE);
        this.minIdle = PropertiesUtil.toInteger(properties.get(PROP_MIN_IDLE), DEFAULT_MIN_IDLE);
        this.maxIdle = PropertiesUtil.toInteger(properties.get(PROP_MAX_IDLE), DEFAULT_MAX_IDLE);
        this.maxWait = PropertiesUtil.toInteger(properties.get(PROP_MAX_WAIT), DEFAULT_MAX_WAIT);

        this.aofsync = PropertiesUtil.toString(properties.get(PROP_AOFSYNC), DEFAULT_AOFSYNC);

        this.testOnBorrow = PropertiesUtil.toBoolean(properties.get(PROP_TEST_ON_BORROW), DEFAULT_TEST_ON_BORROW);

        this.snapshots = PropertiesUtil.toStringArray(properties.get(PROP_SNAPSHOTS), DEFAULT_SNAPSHOTS);

        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        /*
        poolConfig.setMaxActive(this.maxActive);
        poolConfig.setMinIdle(this.minIdle);
        poolConfig.setMaxIdle(this.maxIdle);
        poolConfig.setMaxWait(this.maxWait);
        poolConfig.setTestOnBorrow(this.testOnBorrow);
        */

        log.error("host: {} -- port: {}", this.host, this.port);
        this.setJedisPool(new JedisPool(poolConfig, this.host, this.port));


        /* Ping Redis on Startup */

        final Jedis configJedis = this.getJedis();
        configJedis.select(0);

        try {
            log.error("Redis ping: " + configJedis.ping());

            // Set AOF Sync
            //configJedis.configSet("appendfsync", this.aofsync);

            // Set Snapshot config
            String snapshotConfig = "";
            for(String item : this.snapshots) {
                if(StringUtils.isNotBlank(snapshotConfig)) {
                    snapshotConfig += " ";
                }
                snapshotConfig += item;
            }

            //configJedis.configSet("save", snapshotConfig);
        } catch(Exception ex) {
            log.error("Could not acquire a redis connection pool.");
            log.error(ex.getMessage());
        } finally {
            this.returnJedis(configJedis);
        }
    }
}
