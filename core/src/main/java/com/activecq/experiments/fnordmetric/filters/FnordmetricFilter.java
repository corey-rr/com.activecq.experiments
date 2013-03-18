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
package com.activecq.experiments.fnordmetric.filters;

import com.activecq.experiments.fnordmetric.FnordmetricManager;
import com.day.cq.wcm.api.WCMMode;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author david
 */

@SlingFilter(
        label="ActiveCQ Experiments - Fnordmetric Sling Filter",
        description="Sample implementation of a Sling Filter",
        metatype=true,
        generateComponent=true, // True if you want to leverage activate/deactivate
        generateService=true,
        order=0, // The smaller the number, the earlier in the Filter chain (can go negative); Defaults to Integer.MAX_VALUE which push it at the end of the chain
        scope=SlingFilterScope.REQUEST) // REQUEST, INCLUDE, FORWARD, ERROR, COMPONENT (REQUEST, INCLUDE, COMPONENT)
@Properties({
    @Property(
        label="Vendor",
        name=Constants.SERVICE_VENDOR,
        value="ActiveCQ",
        propertyPrivate=true
    )
})
public class FnordmetricFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(FnordmetricFilter.class.getName());

    @Reference
    FnordmetricManager fnordmetric;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Usually, do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long overheadTime = System.nanoTime();
        final long elapsedTime = System.currentTimeMillis();

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        final Resource resource = slingRequest.getResource();

        final RequestPathInfo pathInfo = slingRequest.getRequestPathInfo();

        final long overheadTimePhase1 = System.nanoTime() - overheadTime;

        chain.doFilter(request, response);

        overheadTime = System.nanoTime();

        try {

            fnordmetric.event("http_request",
                    "method", slingRequest.getMethod(),
                    "resource", resource.getPath(),
                    "name", resource.getName(),
                    "title", resource.adaptTo(ValueMap.class).get("jcr:title", resource.getName()),
                    "selectors", pathInfo.getSelectorString(),
                    "extension", pathInfo.getExtension(),
                    "suffix", pathInfo.getSuffix(),
                    "uri", slingRequest.getRequestURI(),
                    "operation", slingRequest.getParameter(":operation"),
                    "wcmmode", WCMMode.fromRequest(slingRequest).name(),
                    "user", resource.getResourceResolver().getUserID(),
                    "time_elapsed", String.valueOf(System.currentTimeMillis() - elapsedTime));

            fnordmetric.event("fnord_tracking",
                    "overhead", String.valueOf(System.nanoTime() - overheadTime) + overheadTimePhase1);


        } catch (Exception ex) {
            log.error("Could not log to fnordmetric: " + ex.getMessage());
        }

    }


    @Override
    public void destroy() {
        // Usually, do Nothing
    }

    /** OSGi Component Methods **/

    @Activate
    protected void activate(final ComponentContext componentContext) throws Exception {
        final Map<String, String> properties = (Map<String, String>) componentContext.getProperties();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {

    }
}