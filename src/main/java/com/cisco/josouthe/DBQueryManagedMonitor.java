package com.cisco.josouthe;

import com.cisco.josouthe.analytics.AnalyticsSchemaException;
import com.cisco.josouthe.analytics.AnalyticsService;
import com.cisco.josouthe.analytics.Schema;
import com.cisco.josouthe.configuration.ConfigCache;
import com.cisco.josouthe.configuration.ConfigEndpoint;
import com.cisco.josouthe.configuration.Query;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Map;

public class DBQueryManagedMonitor extends AManagedMonitor {
    private Logger logger = LogManager.getFormatterLogger();
    private ConfigCache configCache = null;

    ConfigCache readConfiguration(String configFileName) throws TaskExecutionException {
        File configFile = new File(configFileName);
        if( ! configFile.exists() ) throw new TaskExecutionException("Config file does not exist: "+ configFileName);
        if( ! configFile.canRead() ) throw new TaskExecutionException("Config file exists but is not readable: "+ configFileName);
        ConfigEndpoint[] endpoints = null;
        if( this.configCache == null ) this.configCache = new ConfigCache();
        if( configFile.lastModified() > this.configCache.configFileLastReadTimeStamp ) {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                BufferedReader reader = new BufferedReader(new FileReader(configFileName));
                StringBuilder jsonFileContent = new StringBuilder();
                while (reader.ready()) {
                    jsonFileContent.append(reader.readLine());
                }
                endpoints = gson.fromJson(jsonFileContent.toString(), ConfigEndpoint[].class);
                if (endpoints == null) throw new TaskExecutionException("Could not read Configuration JSON File: "+ configFileName);
                this.configCache.replaceEndpoints(endpoints);
                this.logger.info(String.format("DB Query Extension read %d config endpoints, version %s for support: %s", endpoints.length, MetaData.VERSION, MetaData.GITHUB));
            } catch (IOException exception) {
                logger.warn(String.format("Exception while reading the external file %s, message: %s", configFileName, exception));
            }
        }
        return this.configCache;
    }
    
    public Connection getConnection(ConfigEndpoint config ) throws TaskExecutionException {
        Connection connection = null;
        try {
            Class.forName(config.dbDriverName);
            try {
                connection = DriverManager.getConnection(config.connectionString, config.dbUser, config.dbPassword);
            } catch (SQLException ex) {
                throw new TaskExecutionException("Failed to create the database connection to "+ config.connectionString);
            }
        } catch (ClassNotFoundException ex) {
            // log an exception. for example:
            throw new TaskExecutionException("Driver not found for class: "+ config.dbDriverName);
        }
        return connection;
    }

    @Override
    public TaskOutput execute(Map<String, String> configMap, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        this.logger = taskExecutionContext.getLogger();
        StringBuilder statusMessage = new StringBuilder("DB Query Extension Status: ");

        if( configMap.getOrDefault("configFile","unconfigured").equals("unconfigured") ){
            throw new TaskExecutionException("DB Query Config File Not Set, nothing to do");
        } else {
            ConfigCache configuration = readConfiguration(taskExecutionContext.getTaskDir() +"/"+ configMap.get("configFile") );
            if( configuration == null ) throw new TaskExecutionException("No End Points read from configuration, something must be wrong");
        }

        for( ConfigEndpoint configEndpoint : configCache.configEndpointList ) {
            Connection dbConnection = getConnection(configEndpoint);
            if( dbConnection == null ) {
                logger.warn("DB Connection was not successful to "+ configEndpoint.connectionString);
                statusMessage.append("DB Connection was not successful to "+ configEndpoint.connectionString);
                continue;
            }
            AnalyticsService analyticsAPI = new AnalyticsService(configEndpoint.analytics);
            if( analyticsAPI == null ) {
                logger.warn("AppD Analytics Connection was not successful to "+ configEndpoint.analytics.accountName);
                statusMessage.append("AppD Analytics Connection was not successful to "+ configEndpoint.analytics.accountName);
                continue;
            }
            for(Query query : configEndpoint.queries) {
                if( ! query.isTimeToRun() ) continue;
                Schema schema = analyticsAPI.getSchema(query.analyticsTable); //schema will be null if not existing, this is ok
                try {
                    Statement statement = dbConnection.createStatement();
                    ResultSet resultSet = statement.executeQuery(query.sqlQuery);
                    QueryResponse queryResponse = new QueryResponse(resultSet, query.analyticsTable);
                    analyticsAPI.confirmSchema(queryResponse.getSchemaDefinition());
                    analyticsAPI.insertSchema(queryResponse.getSchemaDefinition(), queryResponse.getSchemaData());
                } catch (Exception e) {
                    logger.warn(String.format("Exception executing query '%s', message: %s", query.sqlQuery, e));
                }
            }
            try {
                dbConnection.close();
            } catch (SQLException e) {
                logger.warn(String.format("Error closing DB Connection to %s, error: %s",configEndpoint.connectionString, e));
            }
        }
        return new TaskOutput(statusMessage.toString());
    }
}
