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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: david
 */
public interface RedisResourceManager {
    public final static String AUTO_CHILD_NODE_NAME_PREFIX = "node-";
    public final static String AUTO_CHILD_INDICATOR = "*";

    public static final String REDIS_KEY_PREFIX_RESOURCES = "cq:r:";
    public static final  String REDIS_KEY_PREFIX_CHILDREN = "cq:c:";

    public static final String REDIS_KEY_SEARCH_FULLTEXT = "cq:s:ft:";
    public static final String REDIS_KEY_SEARCH_FULLTEXT_LOOKUP = "cq:s:ftl:";

    public static final String REDIS_KEY_SEARCH_PROPERTY = "cq:s:p:";
    public static final String REDIS_KEY_SEARCH_PROPERTY_LOOKUP = "cq:s:pl:";

    public static final String REDIS_KEY_PREFIX_DELIMITER = ":";

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

    public boolean modifyResource(String path, Map<String, ? extends Object> map);

    public boolean removeResource(final String path);

    public String getWorkspace();

    public String getRedisKey(final String keyType, final String key);

    public List<String> search(final String term);

}
