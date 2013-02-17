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
import com.adobe.granite.xss.XSSFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * User: david
 */
public class XSSMapDecorator extends AbstractMapDecorator implements MapDecorator {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String PREFIX_XSS = "xss_";

    private final XSSFilter xssFilter;

    public XSSMapDecorator(XSSFilter xssFilter) {
        this.xssFilter = xssFilter;
    }

    @Override
    public Map<String, Object> decorate(final Map<String, Object> map) {
        final Map<String, Object> xssMap = new HashMap<String, Object>();

        for(final String key : map.keySet()) {

            if(!acceptProperty(key)) { continue; }

            final Object value = map.get(key);
            final String xssKey = PREFIX_XSS + key;

            if(value instanceof String) {

                /** String value **/

                xssMap.put(xssKey, xssFilter.filter((String) value));

            } else if (isArray(value)) {

                /** String[] value **/

                final String[] values = toStringArray(value);
                final String xssArray[] = new String[values.length];

                for(int i = 0; i < values.length; i++) {
                    xssArray[i] = xssFilter.filter(values[i]);
                }

                xssMap.put(xssKey, xssArray);
            }
        }

        return xssMap;
    }

    @Override
    public boolean acceptProperty(final String property) {
        return !isSystemProperty(property);
    }
}
