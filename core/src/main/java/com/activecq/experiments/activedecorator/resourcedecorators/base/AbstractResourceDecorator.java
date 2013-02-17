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

import com.day.cq.search.QueryBuilder;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Constants;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

/**
 * User: david
 */

@org.apache.felix.scr.annotations.Component(
        label = "ActiveCQ - Abstract Resource Type Resource Decorator",
        description = "Base abstract implementation for Resource Decorators that implements OSGi Component inputs for ResourceTypes.",
        metatype = false,
        immediate = false,
        componentAbstract = true,
        inherit = false)
@Properties({
        @Property(
                label="Vendor",
                name= Constants.SERVICE_VENDOR,
                value="ActiveCQ",
                propertyPrivate=true
        )
})
public abstract class AbstractResourceDecorator {

    /** Request drive methods **/

    public static SlingHttpServletRequest getSlingRequest(HttpServletRequest request) {
        if(request instanceof  SlingHttpServletRequest) {
            return (SlingHttpServletRequest) request;
        }

        return null;
    }

    public static com.day.cq.wcm.api.components.ComponentContext getComponentContext(HttpServletRequest request) {
        return WCMUtils.getComponentContext(getSlingRequest(request));
    }

    public static Resource getResource(HttpServletRequest request) {
        return getSlingRequest(request).getResource();
    }

    /** Resource driven methods **/

    public static ResourceResolver getResourceResolver(final Resource resource) {
        return resource.getResourceResolver();
    }

    public static Page getCurrentPage(final Resource resource) {
        return getPageManager(resource).getContainingPage(resource);
    }

    public static PageManager getPageManager(final Resource resource) {
        return getResourceResolver(resource).adaptTo(PageManager.class);
    }

    public static TagManager getTagManager(final Resource resource) {
        return getResourceResolver(resource).adaptTo(TagManager.class);
    }

    public static Designer getDesigner(final Resource resource) {
        return getResourceResolver(resource).adaptTo(Designer.class);
    }

    public static QueryBuilder getQueryBuilder(final Resource resource) {
        return getResourceResolver(resource).adaptTo(QueryBuilder.class);
    }

    public static Component getComponent(final Resource resource) {
        return WCMUtils.getComponent(resource);
    }


    /** JCR Getters **/

    public static Node getNode(final Resource resource) {
        return WCMUtils.getNode(resource);
    }

    protected boolean hasNode(final Resource resource) {
        return getNode(resource) != null;
    }

    public static Session getSession(final Resource resource) {
        return getResourceResolver(resource).adaptTo(Session.class);
    }

    /** Abstract **/

    protected abstract boolean accepts(Resource resource, HttpServletRequest request);
}
