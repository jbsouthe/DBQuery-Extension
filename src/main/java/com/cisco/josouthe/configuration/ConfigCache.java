package com.cisco.josouthe.configuration;

import java.util.ArrayList;
import java.util.List;
public class ConfigCache {
    public long configFileLastReadTimeStamp=0;
    public List<ConfigEndpoint> configEndpointList = new ArrayList<>();

    public void replaceEndpoints( ConfigEndpoint[] endpoints ) {
        this.configEndpointList.clear();
        for( ConfigEndpoint endpoint : endpoints )
            this.configEndpointList.add(endpoint);
    }
}
