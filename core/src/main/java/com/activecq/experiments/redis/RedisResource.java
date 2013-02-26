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

import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * User: david
 */
public class RedisResource extends SyntheticResource {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Map<String, Object> valueMap;

    public RedisResource(ResourceResolver resourceResolver, String path, String resourceType, Map<String, String> valueMap) {
        super(resourceResolver, path, resourceType);
        this.valueMap = (Map) valueMap;
    }

    public RedisResource(ResourceResolver resourceResolver, ResourceMetadata rm, String resourceType, Map<String, String> valueMap) {
        super(resourceResolver, rm, resourceType);
        this.valueMap = (Map) valueMap;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type != ValueMap.class) {
            return super.adaptTo(type);
        }

        return (AdapterType) new ValueMapDecorator(this.valueMap);
    }
}
