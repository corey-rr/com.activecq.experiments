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

package com.activecq.experiments.activedecorator.resourcewrappers;

import com.activecq.experiments.activedecorator.mapdecorators.MapDecorator;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: david
 */
public class ActiveResourceWrapper extends ResourceWrapper {

    private Map<String, Object> data = new HashMap<String, Object>();
    private List<MapDecorator> mapDecorators = new ArrayList<MapDecorator>();

    /**
     * Creates a new wrapper instance delegating all method calls to the given
     * <code>resource</code>.
     */
    public ActiveResourceWrapper(final Resource resource) {
        super(resource);
        final ValueMap valueMap = super.adaptTo(ValueMap.class);
        this.data.putAll(valueMap);
    }

    public void remove(String key) {
        this.data.remove(key);
    }

    public void removeAll(String... keys) {
        for(final String key : keys) {
            remove(key);
        }
    }

    public void putAll(Map<? extends String, Object> map) {
        this.data.putAll(map);
    }

    public void put(String key, Object value) {
        this.data.put(key, value);
    }

    public void replaceAll(Map<String, Object> map) {
        if(map == null) {
            this.data = new HashMap<String, Object>();
        }  else {
            this.data = map;
        }
    }

    public void addValueMapDecorator(final MapDecorator mapDecorator) {
        mapDecorators.add(mapDecorator);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type != ValueMap.class) {
            return super.adaptTo(type);
        }

        Map<String, Object> map = new HashMap<String, Object>(this.data);

        for(MapDecorator mapDecorator : this.mapDecorators) {
            map.putAll(mapDecorator.decorate(this.data));
        }

        return (AdapterType) new ValueMapDecorator(map);
    }
}