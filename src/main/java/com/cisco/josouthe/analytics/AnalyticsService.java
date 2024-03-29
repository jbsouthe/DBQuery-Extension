package com.cisco.josouthe.analytics;

import com.cisco.josouthe.configuration.AnalyticsEndpoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;


public class AnalyticsService {
    private static final Logger logger = LogManager.getFormatterLogger();

    public String baseUrl, APIAccountName, APIKey;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private OkHttpClient okHttpClient;

    public AnalyticsService(String urlString, String APIAccountName, String APIKey) {
        if( !urlString.endsWith("/") ) urlString+="/";
        this.baseUrl = urlString;
        this.APIAccountName = APIAccountName;
        this.APIKey = APIKey;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        okHttpClient = builder.build();
    }

    public AnalyticsService(AnalyticsEndpoint analytics) {
        this(analytics.url, analytics.accountName, analytics.APIKey);
    }

    protected String getRequest( String urlRequest ) throws IOException {
        return executeRequest( "GET", urlRequest, null);
    }

    protected String postRequest( String url, String body) throws IOException {
        return executeRequest("POST", url, body);
    }

    protected String executeRequest( String method, String urlRequest, String body) throws IOException {
        RequestBody requestBody = null;
        if( body != null ) {
            requestBody = RequestBody.create(MediaType.parse("application/vnd.appd.events+json;v=2"), body);
        }
        Request request = new Request.Builder()
                .url( this.baseUrl + urlRequest)
                .method(method, requestBody)
                .addHeader("X-Events-API-AccountName", this.APIAccountName)
                .addHeader("X-Events-API-Key", this.APIKey)
                .addHeader("Content-type","application/vnd.appd.events+json;v=2")
                .addHeader("Accept","application/vnd.appd.events+json;v=2")
                .build();
        logger.trace("Request %s",request.toString());
        Response response = okHttpClient.newCall(request).execute();
        String json = response.body().string();
        logger.trace("Response Body: %s",json);
        return json;
    }

    protected String deleteRequest( String urlRequest ) throws IOException {
        return executeRequest("DELETE", urlRequest, null);
    }

    public Schema getSchema( String name ) {
        Schema schema = null;
        try {
            String json = getRequest(String.format("events/schema/%s", name));
            //System.out.println("Schema: "+json);
            schema = gson.fromJson(json, Schema.class);
        } catch (IOException e) {
            logger.warn("Exception in retrieve schema request: %s",e.getMessage());
        }
        return schema;
    }

    public String createSchema( Schema schema ) throws IOException {
        String json = schema.getDefinitionJSON();
        logger.trace("Create Schema JSON: %s",json);
        return postRequest(String.format("events/schema/%s",schema.name), json);
    }

    public String insertSchema( Schema schema, List<Map<String,String>> list) throws AnalyticsSchemaException, IOException {
        Map<String,String>[] maps = list.toArray(new HashMap[list.size()]);
        return insertSchema(schema, maps);
    }

    public String insertSchema(Schema schema, Map<String,String>... data) throws AnalyticsSchemaException, IOException {
        StringBuilder json = new StringBuilder("[ ");
        for( int i=0; i<data.length; i++ ) {
            json.append( schema.getJSON(data[i]) );
            if( i+1 < data.length ) json.append(", ");
        }
        json.append(" ]");
        logger.trace("Insert Schema Data JSON: %s", json);
        System.out.println(String.format("JSON: '%s'",json));
        return postRequest(String.format("events/publish/%s",schema.name), json.toString());
    }

    public String deleteSchema( Schema schema ) throws IOException, AnalyticsSchemaException {
        if( schema == null ) throw new AnalyticsSchemaException("Schema is null in delete request!");
        return deleteSchema( schema.name );
    }

    public String deleteSchema( String name ) throws IOException, AnalyticsSchemaException {
        if( name == null ) throw new AnalyticsSchemaException("Schema name is null in delete request!");
        return deleteRequest("events/schema/"+name );
    }

    @SuppressWarnings("unchecked")
    public void insertAllData( SchemaData schemaData ) throws AnalyticsSchemaException, IOException {
        List<Map<String, String>> data = schemaData.getSchemaData();
        Schema schema = schemaData.getSchemaDefinition();
        Schema checkSchema = getSchema(schema.name);
        if (checkSchema == null || !checkSchema.exists()) createSchema(schema);
        insertSchema(schema, data);
    }

    public void confirmSchema(Schema schemaDefinition) throws IOException, AnalyticsSchemaException {
        Schema checkSchema = getSchema(schemaDefinition.name);
        if( checkSchema == null || !checkSchema.exists() ) {
            createSchema(schemaDefinition);
        }
        if( !checkSchema.equals(schemaDefinition) ) {
            modifySchema(schemaDefinition.name, checkSchema.differences(schemaDefinition));
        }
    }

    private void modifySchema(String name, Map<String, String> differences) throws AnalyticsSchemaException, IOException {
        if( name == null || differences == null || differences.isEmpty() ) throw new AnalyticsSchemaException("Strange call, no differences to modify the schema");
        StringBuilder json = new StringBuilder("[ { \"add\": {");
        Iterator<String> it = differences.keySet().iterator();
        while(it.hasNext()) {
            String key = it.next();
            json.append( String.format("\"%s\": \"%s\"", key, differences.get(key)) );
            if( it.hasNext() ) json.append(", ");
        }
        json.append(" } } ]");
        logger.trace("Modify Schema JSON: %s", json);
        System.out.println(String.format("JSON: '%s'",json));
        executeRequest("PATCH", String.format("events/schema/%s",name), json.toString());
    }
}
