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

package com.activecq.experiments.activedecorator.resourcedecorators.base;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * User: david
 */
@Component(
        label = "ActiveCQ - Abstract Primary Type Resource Decorator",
        description = "Base abstract implementation for Resource Decorators that implements OSGi Component inputs for Primary Types.",
        metatype = false,
        immediate = false,
        componentAbstract = true,
        inherit = true)
@Properties({
        @Property(
                label="Vendor",
                name= Constants.SERVICE_VENDOR,
                value="ActiveCQ",
                propertyPrivate=true
        ),
        @Property(
                label = "Primary Types",
                description = "Primary Types to decorate",
                value = { "" },
                name = "prop.primary-types"
        )
})
public abstract class AbstractPrimaryTypeResourceDecorator extends AbstractResourceDecorator implements ResourceDecorator {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    protected String[] primaryTypes;

    @Override
    public Resource decorate(Resource resource) {
        return decorate(resource, null);
    }

    @Override
    public Resource decorate(Resource resource, HttpServletRequest request) {
        return resource;
    }

    /**
     * Abstract Methods *
     */
    protected boolean accepts(Resource resource, HttpServletRequest request) {
        if(resource == null)  {
            return false;
        }

        for(final String resourceType : this.primaryTypes) {
            final ValueMap properties = resource.adaptTo(ValueMap.class);
            if(properties == null) { return false; }

            final String slingResourceType = properties.get(JcrConstants.JCR_PRIMARYTYPE, "");

            if(StringUtils.equals(resourceType, slingResourceType)) {
                return true;
            }
        }

        return false;
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

        this.primaryTypes = PropertiesUtil.toStringArray(properties.get("prop.primary-types"), new String[] {});
    }
}
