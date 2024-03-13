package com.cisco.josouthe.configuration;

import com.cisco.josouthe.analytics.Analytics;

public class AnalyticsEndpoint {
    public String url, accountName, APIKey;
    public transient Analytics analyticsAPI;
}
