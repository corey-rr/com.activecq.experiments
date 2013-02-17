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

package com.activecq.experiments.activedecorator.mapdecorators.base;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * User: david
 */
public class AbstractMapDecorator {
    protected String[] SYSTEM_PROPERTY_WHITE_LIST = new String[] {
        "jcr:title",
        "jcr:description"
    };

    protected static boolean isArray(final Object obj) {
        return obj.getClass().isArray();
    }

    protected static String[] toStringArray(final Object obj) {
        final List<String> list = new ArrayList<String>();
        for (final Object o: (Object[]) obj) {
            if(o instanceof String) {
                list.add((String) o);
            }
        }

        return list.toArray(new String[list.size()]);
    }

    protected boolean isSystemProperty(final String key) {

        if(ArrayUtils.contains(SYSTEM_PROPERTY_WHITE_LIST, key)) {
            return false;
        } else if(StringUtils.startsWith(key, "nt:") ||
            StringUtils.startsWith(key, "jcr:") ||
            StringUtils.startsWith(key, "sling:") ||
            StringUtils.startsWith(key, "cq:")) {
            return true;
        }

        return false;
    }
}