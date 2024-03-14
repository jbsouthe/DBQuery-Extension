package com.cisco.josouthe.analytics;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class Schema extends ErrorReply {
    public String name;
    public Map<String,String> schema;
    private static Pattern startsWithNumber = Pattern.compile("^\\d");
    private static Pattern validName = Pattern.compile("^[a-z|A-Z|_][a-z|A-Z|_|0-9]*$");

    public Schema() {} //for GSON

    public Schema( String name ) {
        this.name = name;
        this.schema = new HashMap<>();
    }

    public Map<String,String> getMap() { return this.schema; }

    public void addField( String fieldName, Object type ) throws AnalyticsSchemaException {
        if( fieldName == null || type == null ) throw new AnalyticsSchemaException("Field name and Type must not be null!");
        if( this.schema.containsKey(fieldName) ) throw new AnalyticsSchemaException(String.format("Field '%s' alreadys exists in this schema",fieldName));
        if( !isValidFieldName(fieldName) ) throw new AnalyticsSchemaException(String.format("Field '%s' must only contain a-z, A-Z, _, 0-9 and can not begin with a number", fieldName));
        if( type instanceof Boolean ) {
            this.schema.put(fieldName,"boolean");
        } else if( type instanceof Date ) {
            this.schema.put(fieldName, "date");
        } else if( type instanceof Integer ) {
            this.schema.put(fieldName,"integer");
        } else if( type instanceof Number ) {
            this.schema.put(fieldName,"float");
        } else if( type instanceof String ) {
            this.schema.put(fieldName,"string");
        } else {
            this.schema.put(fieldName, "object");
        }
    }

    private boolean isValidFieldName( String fieldName ) {
        if( startsWithNumber.matcher(fieldName).matches()) return false;
        if( validName.matcher(fieldName).matches() ) return true;
        return false;
    }

    public boolean exists() { return this.schema != null; }
    public String getDefinitionJSON() {
        if( !exists() ) return null;
        StringBuilder json = new StringBuilder("{ \"schema\" : { ");
        Iterator<String> it = schema.keySet().iterator();
        while( it.hasNext() ) {
            String key = it.next();
            json.append(String.format("\"%s\" : \"%s\"", key, schema.get(key)));
            if( it.hasNext() ) json.append(", ");
        }
        json.append("} }");
        return json.toString();
    }

    public String getJSON( Map<String,String> data ) throws AnalyticsSchemaException {
        StringBuilder json = new StringBuilder("{ ");
        Iterator<String> it = data.keySet().iterator();
        while(it.hasNext()) {
            String key = it.next();
            if( !this.schema.containsKey(key) ) throw new AnalyticsSchemaException(String.format("Error in Schema Data, key '%s' does not exist in this schema!",key));
            json.append(String.format("\"%s\" : ",key));
            switch ( this.schema.get(key) ) {
                case "boolean": { json.append(String.format("%s", Boolean.valueOf(data.get(key)))); break; }
                case "string": {json.append(String.format("\"%s\"", data.get(key))); break; }
                default:
                    { json.append(String.format("%s",data.get(key))); break; }
            }
            if( it.hasNext() ) json.append(", ");
        }
        json.append("} ");
        return json.toString();
    }

    public boolean equals( Schema schema ) {
        if( schema == null ) return false;
        if( !schema.exists() ) return false;
        Map<String,String> otherMap = schema.getMap();
        if( otherMap == null || otherMap.size() != this.schema.size() ) return false;
        for( String key : this.schema.keySet() )
            if( !this.schema.get(key).equals(otherMap.get(key)) ) return false;
        return true; //if the other schema is the same size and all the keys in this schema match that schema, they are equal
    }
}
