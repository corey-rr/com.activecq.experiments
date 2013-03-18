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

package com.activecq.experiments.fnordmetric.impl;

import com.activecq.experiments.fnordmetric.FnordmetricManager;
import com.activecq.experiments.redis.RedisConnectionPool;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * User: david
 */
@Component(
        label = "ActiveCQ Experiments - Fnordmetric Adapter",
        description = "Fnordmetric Adapter",
        metatype = true,
        immediate = false)
@Properties({
        @Property(
                label="Vendor",
                name= Constants.SERVICE_VENDOR,
                value="ActiveCQ",
                propertyPrivate=true
        ),
        @Property(
                label = "Fnordmetric Key Prefix",
                description = "Redis key prefix",
                value = "fnordmetric-cq",
                name = "prop.fnordmetric-key-prefix"
        ),
        @Property(
                label = "Event Queue TTL",
                description = "Resource Types to decorate",
                intValue = 600,
                name = "prop.event-queue-ttl"
        )
})
@Service
public class FnordmetricManagerImpl implements FnordmetricManager {
    private static final Logger log = LoggerFactory.getLogger(FnordmetricManagerImpl.class.getName());

    public static final String PREFIX = "fnordmetric-cq";
    public static final String STATS = PREFIX + "-stats";
    public static final String QUEUE = PREFIX + "-queue";
    public static final String EVENT = PREFIX + "-event-";
    private static final int EVENT_QUEUE_TTL = 10;
    private static final String EVENTS_RECEIVED = "events_received";


    @Reference
    private RedisConnectionPool redis;

    /**
     * Public API for sending data to Fnordmetric
     *
     * @param data
     * @return
     * @throws IllegalArgumentException
     */
    @Override
    public String event(final String... data) throws IllegalArgumentException {
        if(data == null) {
            throw new IllegalArgumentException("Invalid number (odd) of input parameters");
        }

        final Map<String, String> map = new HashMap<String, String>();
        final boolean typeSignature = (data.length % 2) == 1;

        for(int i = 0; i < data.length; i++) {
            if(typeSignature && i < 1) {
                map.put(FnordmetricManager.TYPE, data[i]);
            } else {
                map.put(data[i], data[i + 1]);
                i++;
            }
        }

        return this.event(map);
    }

    /**
     * Public API for sending data to Fnordmetric
     *
     * @param data
     * @return
     * @throws IllegalArgumentException
     */
    @Override
    public String event(final Map<String, String> data) throws IllegalArgumentException{

        final JSONObject json;
        try {
            json = new JSONObject(data);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse input data into a valid JSON object");
        }

        return this.pushEvent(this.getNextUUID(), json);
    }

    @Override
    public String event(String type, Map<String, String> data) {
        data.put(FnordmetricManager.TYPE, type);
        return this.event(data);
    }

    /**
     * Send Fnordmetric event data to Redis
     *
     * @param eventId
     * @param json
     * @return
     */
    private String pushEvent(final String eventId, final JSONObject json) {
        final Jedis jedis = redis.getJedis();

        try {
            jedis.hincrBy(STATS, EVENTS_RECEIVED, 1);
            jedis.set(EVENT + eventId, json.toString());
            jedis.lpush(QUEUE, eventId);
            jedis.expire(EVENT + eventId, EVENT_QUEUE_TTL);
        } finally {
            redis.returnJedis(jedis);
        }

        return eventId;
    }

    /**
     * Get the next random event Id
     *
     * @return
     */
    private String getNextUUID() {
        return UUID.randomUUID().toString();
        //rand(8**32).to_s(36)
        //return new Random(8 * 32).toString();
    }
}






