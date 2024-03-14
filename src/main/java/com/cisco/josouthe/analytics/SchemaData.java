package com.cisco.josouthe.analytics;

import java.util.List;
import java.util.Map;

public interface SchemaData {

    public Schema getSchemaDefinition() throws AnalyticsSchemaException;
    public List<Map<String,String>> getSchemaData();
}
