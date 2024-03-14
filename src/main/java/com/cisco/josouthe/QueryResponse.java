package com.cisco.josouthe;

import com.cisco.josouthe.analytics.AnalyticsSchemaException;
import com.cisco.josouthe.analytics.Schema;
import com.cisco.josouthe.analytics.SchemaData;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class QueryResponse implements SchemaData {

    private List<Map<String,String>> schemaData = new ArrayList<>();
    private Schema schema = null;

    public QueryResponse(ResultSet resultSet, String schemaName) throws SQLException, AnalyticsSchemaException {
        Date timestamp = new Date();
        while(resultSet.next()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            if( this.schema == null ) {
                this.schema = new Schema(schemaName);
                for(int i=0; i<metaData.getColumnCount(); i++) {
                    this.schema.addField( metaData.getColumnLabel(i), resultSet.getObject(i));
                }
                this.schema.addField("timestamp", new Date());
            }
            Map<String,String> dataMap = new HashMap<>();
            dataMap.put("timestamp",String.valueOf(timestamp.getTime()));
            for( int i=0; i< metaData.getColumnCount(); i++ ) {
                dataMap.put(metaData.getColumnLabel(i), resultSet.getString(i));
            }
            schemaData.add(dataMap);
        }
    }

    @Override
    public Schema getSchemaDefinition() throws AnalyticsSchemaException {
        return schema;
    }

    @Override
    public List<Map<String, String>> getSchemaData() {
        return schemaData;
    }
}
