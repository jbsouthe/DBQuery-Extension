package com.cisco.josouthe.configuration;

import java.util.List;

public class ConfigEndpoint {
    public String connectionString, dbUser, dbPassword, dbDriverName;
    public List<Query> queries;
    public AnalyticsEndpoint analytics;
}
