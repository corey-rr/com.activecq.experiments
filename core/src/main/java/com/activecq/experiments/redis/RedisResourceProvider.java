/*
 * Copyright 2013 david gonzalez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.activecq.experiments.redis;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 *
 * @author david
 */
@Component(
        label = "ActiveCQ Experiments - Sling Resource Provider for Redis",
        description = "Sample Sling Resource Provider",
        metatype=false,
        immediate=false
)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ActiveCQ",
                propertyPrivate = true
        ),
        @Property(
                label="Root paths",
                description="Root paths this Sling Resource Provider will respond to",
                name= ResourceProvider.ROOTS,
                value={"/var/redis"})
})
@Service
public class RedisResourceProvider implements ResourceProvider {
    private final Logger log = LoggerFactory.getLogger(this.getClass());


    private static final String DEFAULT_PRIMARY_TYPE = RedisManager.REDIS_JCR_PRIMARY_TYPE;
    private static final String DEFAULT_RESOURCE_TYPE = RedisManager.REDIS_SLING_RESOURCE_TYPE;
    private List<String> roots;

    @Reference
    private RedisManager redisManager;

    @Override
    public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request, String path) {
        // For this example the Request is not taken into consideration when evaluating
        // the Resource request, so we just call getResource(rr, path)

        // Remember, since this is a Synthetic resource there are no ACLs applied to this
        // resource. If you would like to restrict access, it must be done programmatically by checking
        // the ResourceResolver's user.
        return getResource(resourceResolver, path);
    }

    @Override
    public Resource getResource(ResourceResolver resourceResolver, String path) {
        return getResource(resourceResolver, path, DEFAULT_RESOURCE_TYPE);
    }

    public Resource getResource(ResourceResolver resourceResolver, String path, String resourceType) {
        log.debug("Redis getResource for: {}", path);
        // Make getResource() return as fast as possible!
        // Return null early if getResource() cannot/should not process the resource request

        // Check the user/group issuing the resource resolution request
        if(!accepts(resourceResolver)) { return null; }

        // Reject any paths that do not match the roots
        if(!accepts(path)) { return null; }

        // If path is a root, return a Synthetic Folder
        // This could be any "type" of Synthetic Resource
        if(isRoot(path)) { return new SyntheticResource(resourceResolver, path, JcrConstants.NT_FOLDER); }

        log.debug("Checking if path {} existing in redis", path);

        // If path does not exist in Redis, then return null immediately
        if(!redisManager.resourceExists(path)) { return null; }

        log.debug("Path {} EXISTS in redis", path);
        final String redisKey = redisManager.getResourceKey(path);

        Map<String, String> redisMap = setDefaultProperties(new HashMap<String, String>());
        redisMap.putAll(this.getJedis().hgetAll(redisKey));

        final ResourceMetadata resourceMetaData = new ResourceMetadata();
        resourceMetaData.setResolutionPath(path);

        log.debug("resourceType used; {}", resourceType);


        return new RedisResource(resourceResolver, resourceMetaData, resourceType, redisMap);
    }

    @Override
    public Iterator<Resource> listChildren(Resource parent) {
        final String path = parent.getPath();
        log.debug("-- Start ---------------------------");
        log.debug("list children for: {}", path);
        // Check the user/group issuing the resource resolution request
        if(!accepts(parent.getResourceResolver())) { return null; }

        // Reject any paths that do not match the roots
        log.debug("accept path {}: {}", path, accepts(path));
        if(!accepts(path)) { return null; }

        final ResourceResolver resourceResolver = parent.getResourceResolver();
        final List<Resource> children = new ArrayList<Resource>();

        final Set<String> redisChildren = redisManager.getChildren(path);
        for(final String redisChild : redisChildren) {
            final Resource resource = this.getResource(resourceResolver, redisChild, RESOURCE_TYPE_SYNTHETIC);

            log.debug("redis child: {} is null: {}", redisChild, resource == null);

            if(resource != null) {
                log.debug("resource is NOT null, add to children!");
                children.add(resource);
            }
        }
        log.debug("-- End Children ---------------------------");

        return children.iterator();
    }

    /**
     * Checks if the provided path is a defined Root path
     *
     * @param path
     * @return
     */
    protected boolean isRoot(String path) {
        for(String root : this.roots) {
            if(StringUtils.equals(path, root)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if this Resource Provider is willing to handle the resource path
     *
     * @param path
     * @return
     */
    protected boolean accepts(String path) {
        for(String root : this.roots) {
            if(StringUtils.startsWith(path, root.concat("/"))) {
                log.debug("Redis resource provider accepts path {}: ", path);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if this Resource Provider is willing to handle the resolution request
     *
     * @param resourceResolver
     * @return
     */
    protected boolean accepts(ResourceResolver resourceResolver) {
        if(resourceResolver == null) { return false; }
        if(StringUtils.equals("anonymous", resourceResolver.getUserID())) {
            // Terrible "anonymous" check, this is just for an example
            return false;
        }

        return true;
    }

    /**
     * Redis specific helpers
     */

    /**
     *
     * @return
     */
    protected Jedis getJedis() {
        return redisManager.getJedis();
    }


    /**
     *
     * @param redisMap
     * @return
     */
    protected String getResourceType(Map<String, String> redisMap) {
        //if(redisMap.containsKey(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
        //   return redisMap.get(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY);
        //}

        return DEFAULT_RESOURCE_TYPE;
    }




    /**
     *
     * @return
     */
    protected String getWorkspace() {
        return redisManager.getWorkspace();
    }

    /**
     *
     * @param redisMap
     * @return
     */
    protected Map<String, String> setDefaultProperties(Map<String, String> redisMap) {
        if(!redisMap.containsKey(JcrConstants.JCR_PRIMARYTYPE)) {
            redisMap.put(JcrConstants.JCR_PRIMARYTYPE, DEFAULT_PRIMARY_TYPE);
        }

        if(!redisMap.containsKey(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)) {
            redisMap.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, DEFAULT_RESOURCE_TYPE);
        }

        return redisMap;
    }

    /**
     * OSGi Component Methods *
     */
    @Activate
    protected void activate(final ComponentContext componentContext) throws Exception {
        configure(componentContext);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
    }

    private void configure(final ComponentContext componentContext) {
        final Map<String, String> properties = (Map<String, String>) componentContext.getProperties();

        // Get Roots from Service properties
        this.roots = new ArrayList<String>();

        String[] rootsArray = PropertiesUtil.toStringArray(properties.get(ResourceProvider.ROOTS), new String[]{});
        for(String root : rootsArray) {
            root = StringUtils.strip(root);
            if(StringUtils.isBlank(root)) {
                continue;
            } else if (StringUtils.equals(root, "/")) {
                // Cowardly refusing to mount the root
                continue;
            }

            this.roots.add(StringUtils.removeEnd(root, "/"));
        }

        log.debug("Redis ping: {} ", this.getJedis().ping());
    }
}