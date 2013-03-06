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

package com.activecq.experiments.redis;

import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.Set;

/**
 * User: david
 */
public interface RedisResourceManager {
    public static final String REDIS_KEY_PREFIX_RESOURCES = "cq::resources::";
    public static final  String REDIS_KEY_PREFIX_CHILDREN = "cq::children::";
    public static final  String REDIS_KEY_PREFIX_DELIMITER = "::";


    public static final  String REDIS_JCR_PRIMARY_TYPE = "redis:hash";
    public static final  String REDIS_SLING_RESOURCE_TYPE = "redis/resource";

    public static final String DEFAULT_WORKSPACE = "crx.default";

    public Jedis getJedis();

    public void returnJedis(final Jedis jedis);

    public boolean resourceExists(final String path);

    public String getResourceKey(final String path);

    public Map<String, String> getResourceProperties(final String path);

    public String getChildrenKey(final String path);

    public Set<String> getChildren(final String path);

    public String addResource(final String path, Map<String, ? extends Object> map);

    public boolean removeResource(final String path);

    public String getWorkspace();
}
