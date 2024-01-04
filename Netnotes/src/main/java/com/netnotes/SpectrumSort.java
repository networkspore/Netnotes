package com.netnotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

public class SpectrumSort {
    public static class SortType{
        public final static String LAST_PRICE = "Last Price";
        public final static String BASE_VOL = "Base Volume";
        public final static String QUOTE_VOL = "Quote Volume";
    };

    public static class SortDirection{
        public final static String ASC = "Ascending";
        public final static String DSC = "Decending";
    }

    private String m_type = SortType.BASE_VOL;
    private String m_direction = SortDirection.DSC;

    public SpectrumSort(){

    }

    public SpectrumSort(String type){
        m_type = type;
    }

    public SpectrumSort(String type, String direction){
        m_type = type;
        m_direction = direction;
    }

    public SpectrumSort(JsonObject json){
        if(json != null){
            JsonElement typeElement = json.get("type");
            JsonElement directionElement = json.get("direction");

            m_type = typeElement != null && typeElement.isJsonPrimitive() ? typeElement.getAsString() : m_type;
            m_direction = directionElement != null && directionElement.isJsonPrimitive() ? directionElement.getAsString() : m_direction;
        }
    }

    public String getType(){
        return m_type;
    }

    public String getDirection(){
        return m_direction;
    }

    public boolean isAsc(){
        return m_direction.equals(SortDirection.ASC);
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("type", m_type);
        json.addProperty("direction", m_direction);
        return json;
    }
}
