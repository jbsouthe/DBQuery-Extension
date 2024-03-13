package com.cisco.josouthe.configuration;

public class Query {
    public String sqlQuery, analyticsTable;
    public long runEveryMinutes;
    public transient long lastRunTimestamp=0;
}
