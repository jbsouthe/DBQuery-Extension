package com.cisco.josouthe.configuration;

public class Query {
    public String sqlQuery, analyticsTable;
    public long runEveryMinutes = 1;
    public transient long lastRunTimestamp=0;

    public boolean isTimeToRun() {
        return (System.currentTimeMillis() - this.lastRunTimestamp) >= (this.runEveryMinutes * 60000);
    }
}
