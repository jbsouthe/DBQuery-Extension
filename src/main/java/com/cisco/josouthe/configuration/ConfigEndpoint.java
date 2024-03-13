package com.cisco.josouthe.configuration;

import com.cisco.josouthe.analytics.Analytics;

import java.util.List;

public class ConfigEndpoint {
    public String connectionString, dbUser, dbPassword;
    public List<Query> queries;
    public AnalyticsEndpoint analytics;
}
