# Adobe CQ Redis Adapter

This connector uses the Jedis API library to connect and interact with RedisDB.

The primary purpose of this implementation is to allow CQ developers to treat data in Redis similarly to how data in CRX is treated.

## Redis Connection Pool

The Redis Connection Pool Service defines a connection pool to a Redis service, along w the common configurations for Redis.

`.getJedis()` retrieves a Jedis object used to access the RedisDB API.

`.returnJedis(..)` returns the Jedis object back to the pool, and must be called when the interaction with Redis is complete.

## Redis Resource Manager

The Redis Resource Manager defines the core interactions with CQ used by the Redis Resource Provider.

