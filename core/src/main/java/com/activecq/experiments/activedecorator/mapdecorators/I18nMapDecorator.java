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

package com.activecq.experiments.activedecorator.mapdecorators;

import com.activecq.experiments.activedecorator.mapdecorators.base.AbstractMapDecorator;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * User: david
 */
public class I18nMapDecorator extends AbstractMapDecorator implements MapDecorator {
    public static final String PREFIX_I18N = "i18n_";

    private Resource resource;
    private SlingHttpServletRequest slingRequest;
    private ResourceResolver resourceResolver;
    private PageManager pageManager;
    private Page currentPage;
    private Locale locale;
    private I18n i18n;

    public I18nMapDecorator(Resource resource) {
        this(null, resource);
    }

    public I18nMapDecorator(SlingHttpServletRequest slingRequest) {
        this(slingRequest, null);
    }

    public I18nMapDecorator(SlingHttpServletRequest slingRequest, Resource resource) {
        this.slingRequest = slingRequest;
        this.resource = resource;

        if(this.resource == null && this.slingRequest != null) {
            this.resource = this.slingRequest.getResource();
        }

        if(this.resource != null) {
            this.resourceResolver = this.resource.getResourceResolver();
            this.pageManager = this.resourceResolver.adaptTo(PageManager.class);
            this.currentPage = this.pageManager.getContainingPage(this.resource);
        }

        if(this.currentPage != null && this.slingRequest != null) {
            this.locale = currentPage.getLanguage(false);
            this.i18n = new I18n(this.slingRequest.getResourceBundle(this.locale));
        }
    }

    @Override
    public Map<String, Object> decorate(final Map<String, Object> map) {
        if(this.resource == null || this.slingRequest == null ||
                this.currentPage == null || this.i18n == null) {
            return new HashMap<String, Object>();
        }

        final Map<String, Object> i18nMap = new HashMap<String, Object>();

        for(final String key : map.keySet()) {

            if(!acceptProperty(key)) { continue; }

            final Object value = map.get(key);
            final String i18nKey = PREFIX_I18N + key;

            if(value instanceof String) {

                /** String value **/

                i18nMap.put(i18nKey, i18n.get((String) value));

            } else if (isArray(value)) {

                /** String[] value **/

                final String[] values = toStringArray(value);
                final String i18nArray[] = new String[values.length];

                for(int i = 0; i < values.length; i++) {
                    i18nArray[i] = i18n.get(values[i]);
                }

                i18nMap.put(i18nKey, i18nArray);
            }
        }

        return i18nMap;
    }

    @Override
    public boolean acceptProperty(final String property) {
        return !isSystemProperty(property);
    }
}
