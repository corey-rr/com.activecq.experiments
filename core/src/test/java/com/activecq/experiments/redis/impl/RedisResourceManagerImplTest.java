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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * User: david
 */
public class RedisResourceManagerImplTest {
    private JedisPool jedisPool;
    private Jedis jedis;

    @Before
    public void setUp() throws Exception {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setTestOnBorrow(true);

        jedisPool = new JedisPool(poolConfig, "127.0.0.1");
        /* Ping Redis on Startup */

        jedis = jedisPool.getResource();
        //jedis.select(new RedisResourceManagerImpl().getRedisDB());
    }

    @After
    public void tearDown() throws Exception {
        jedis.del("cq::resources::crx.default::/a/b/c",
                "cq::resources::crx.default::/exists",

                "cq::resources::crx.default::/a",
                "cq::resources::crx.default::/b",
                "cq::resources::crx.default::/c",
                "cq::resources::crx.default::/a/b/1",
                "cq::resources::crx.default::/a/b/2",
                "cq::resources::crx.default::/a/b/3",
                "cq::resources::crx.default::/a/b/4",
                "cq::resources::crx.default::/a/c",

                "cq::children::crx.default::/a",
                "cq::children::crx.default::/b",
                "cq::children::crx.default::/c",
                "cq::children::crx.default::/a/b/1",
                "cq::children::crx.default::/a/b/2",
                "cq::children::crx.default::/a/b/3",
                "cq::children::crx.default::/a/b/4",
                "cq::children::crx.default::/a/c"
         );

        jedisPool.returnResource(jedis);
    }

    @Test
    public void testGetResourceKey() throws Exception {
        RedisResourceManagerImpl redis = new RedisResourceManagerImpl();
        //redis.setJedisPool(this.jedisPool);

        String expValue = "cq::resources::crx.default::/test/foo";
        String result = redis.getResourceKey("/test/foo");
        Assert.assertEquals(expValue, result);
    }

    @Test
    public void testGetChildrenKey() throws Exception {
        RedisResourceManagerImpl redis = new RedisResourceManagerImpl();
        //redis.setJedisPool(this.jedisPool);

        String expValue = "cq::children::crx.default::/test/foo";
        String result = redis.getChildrenKey("/test/foo");
        Assert.assertEquals(expValue, result);
    }

    @Test
    public void testGetChildren() throws Exception {
        RedisResourceManagerImpl redis = new RedisResourceManagerImpl();
        //redis.setJedisPool(this.jedisPool);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("property", "value");

        redis.addResource("/a/b/1", map);
        redis.addResource("/a/b/2", map);
        redis.addResource("/a/b/3", map);
        redis.addResource("/a/b/4", map);

        Assert.assertTrue(redis.getChildren("/a").contains("/a/b"));

        Collection<String> expValues = new ArrayList<String>();
        expValues.add("/a/b/1");
        expValues.add("/a/b/2");
        expValues.add("/a/b/3");
        expValues.add("/a/b/4");

        Assert.assertTrue(redis.getChildren("/a/b").containsAll(
            expValues
        ));
    }

    @Test
    public void testAddResource() throws Exception {
        RedisResourceManagerImpl redis = new RedisResourceManagerImpl();
        //redis.setJedisPool(this.jedisPool);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("string", "this is a string");
        map.put("double", 100D);
        map.put("int", 10);
        map.put("float", 100.001);
        map.put("boolean-true", true);
        map.put("boolean-false", false);
        map.put("char", 'c');

        redis.addResource("/a/b/c", map);

        Assert.assertEquals(
                jedis.hget("cq::resources::crx.default::/a/b/c", "string"),
                "this is a string");

        Assert.assertEquals(
                jedis.hget("cq::resources::crx.default::/a/b/c", "double"),
                "100.0");

        Assert.assertEquals(
                jedis.hget("cq::resources::crx.default::/a/b/c", "int"),
                "10");

        Assert.assertEquals(
                jedis.hget("cq::resources::crx.default::/a/b/c", "float"),
                "100.001");

        Assert.assertEquals(
                jedis.hget("cq::resources::crx.default::/a/b/c", "boolean-true"),
                "true");

        Assert.assertEquals(
                jedis.hget("cq::resources::crx.default::/a/b/c", "boolean-false"),
                "false");

        Assert.assertEquals(
                jedis.hget("cq::resources::crx.default::/a/b/c", "char"),
                "c");

        Assert.assertTrue(
                jedis.smembers("cq::children::crx.default::/a").contains("/a/b")
        );

        Assert.assertTrue(
                jedis.smembers("cq::children::crx.default::/a/b").contains("/a/b/c")
        );

        Assert.assertTrue(
                jedis.smembers("cq::children::crx.default::/a/b/c").size() == 0
        );
    }

    @Test
    public void testRemoveResource() throws Exception {
        RedisResourceManagerImpl redis = new RedisResourceManagerImpl();
       // redis.setJedisPool(this.jedisPool);


        Map<String, Object> map = new HashMap<String, Object>();
        map.put("property", "value");

        redis.addResource("/a/b/1", map);
        redis.addResource("/a/b/2", map);
        redis.addResource("/a/b/3", map);
        redis.addResource("/a/c", map);

        redis.removeResource("/a/b");

        Assert.assertFalse(jedis.exists("cq::resources::crx.default::/a/b/1"));
        Assert.assertFalse(jedis.exists("cq::resources::crx.default::/a/b/2"));
        Assert.assertFalse(jedis.exists("cq::resources::crx.default::/a/b/3"));
        Assert.assertFalse(jedis.exists("cq::resources::crx.default::/a/b"));
        Assert.assertTrue(jedis.exists("cq::resources::crx.default::/a"));
        Assert.assertTrue(jedis.exists("cq::resources::crx.default::/a/c"));

        Assert.assertFalse(jedis.exists("cq::children::crx.default::/a/b/1"));
        Assert.assertFalse(jedis.exists("cq::children::crx.default::/a/b/2"));
        Assert.assertFalse(jedis.exists("cq::children::crx.default::/a/b/3"));
        Assert.assertFalse(jedis.exists("cq::children::crx.default::/a/b"));
        Assert.assertTrue(jedis.exists("cq::children::crx.default::/a"));


        Assert.assertFalse(jedis.exists("cq::children::crx.default::/a/c"));

        Assert.assertTrue(jedis.smembers("cq::children::crx.default::/a").contains("/a/c"));
    }

    @Test
    public void testRemoveResource_2() throws Exception {
        RedisResourceManagerImpl redis = new RedisResourceManagerImpl();
        //redis.setJedisPool(this.jedisPool);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("property", "value");

        redis.addResource("/a/b", map);

        Assert.assertFalse(jedis.exists("cq::children::crx.default::/a/b"));

        redis.removeResource("/a/b");

        Assert.assertFalse(jedis.exists("cq::resources::crx.default::/a/b"));
        Assert.assertFalse(jedis.exists("cq::children::crx.default::/a/b"));

        Assert.assertTrue(jedis.exists("cq::resources::crx.default::/a"));
        Assert.assertFalse(jedis.exists("cq::children::crx.default::/a"));
    }

    @Test
    public void testResourceExists() throws Exception {
        RedisResourceManagerImpl redis = new RedisResourceManagerImpl();
        //redis.setJedisPool(this.jedisPool);

        Assert.assertFalse(redis.resourceExists("/exists"));

        jedis.hset("cq::resources::crx.default::/exists", "property", "value");

        Assert.assertTrue(redis.resourceExists("/exists"));
    }

    @Test
    public void testGetWorkspace() throws Exception {
        RedisResourceManagerImpl redis = new RedisResourceManagerImpl();

        String expValue = "crx.default";
        String result = redis.getWorkspace();
        Assert.assertEquals(expValue, result);
    }
}
