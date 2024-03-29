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

package com.activecq.experiments.activedecorator.resourcedecorators;



import com.activecq.experiments.activedecorator.mapdecorators.I18nMapDecorator;
import com.activecq.experiments.activedecorator.mapdecorators.XSSMapDecorator;
import com.activecq.experiments.activedecorator.resourcedecorators.base.AbstractResourceTypeResourceDecorator;
import com.activecq.experiments.activedecorator.resourcewrappers.ActiveResourceWrapper;
import com.adobe.granite.xss.XSSFilter;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * User: david
 */
@Component(
        label = "ActiveCQ Experiments - Resource Type Resource Decorator",
        description = "Sample",
        metatype = true,
        immediate = false,
        inherit = true)
@Properties({
        @Property(
                label="Vendor",
                name= Constants.SERVICE_VENDOR,
                value="ActiveCQ",
                propertyPrivate=true
        ),
        @Property(
                label = "Resource Types",
                description = "Resource Types to decorate",
                value = { "vendors/activecq/samples/components/fake" },
                name = "prop.resource-types"
        )
})
@Service
public class ResourceTypeResourceDecorator extends AbstractResourceTypeResourceDecorator implements ResourceDecorator {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    protected XSSFilter xssFilter;

    @Override
    public Resource decorate(Resource resource) {
        return decorate(resource, null);
    }

    @Override
    public Resource decorate(Resource resource, HttpServletRequest request) {
        if(!accepts(resource, request)) {
            return resource;
        }

        final Map<String, Object> map = new HashMap<String, Object>();
        final ActiveResourceWrapper wrapper = new ActiveResourceWrapper(resource);

        map.put("jcr:title", "Overwritten title");
        map.put("title", "This is the derived title");

        resource.getResourceMetadata().put("metadata-val", "added to resource directly");

        wrapper.putAll(map);

        wrapper.addValueMapDecorator(new I18nMapDecorator((SlingHttpServletRequest) request));
        wrapper.addValueMapDecorator(new XSSMapDecorator(xssFilter));

        return wrapper;
    }
}
