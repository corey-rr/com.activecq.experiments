package com.activecq.experiments.fnordmetric;

import java.util.Map;

/**
 * User: david
 */
public interface FnordmetricManager {
    public static final String TYPE = "_type";

    public String event(final String... data) throws Exception;
    public String event(final Map<String, String> data);

    public String event(final String type, final Map<String, String> data);


}

