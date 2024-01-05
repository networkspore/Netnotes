package com.netnotes;

import com.google.gson.JsonObject;

import javafx.beans.property.SimpleBooleanProperty;

import com.google.gson.JsonElement;

public class SpectrumSort {
    public static class SortType{
        public final static String LAST_PRICE = "Last Price";
        public final static String BASE_VOL = "Base Volume";
        public final static String QUOTE_VOL = "Quote Volume";
        public final static String LIQUIDITY_VOL = "Liquidity";
    };

    public static class SortDirection{
        public final static String ASC = "Ascending";
        public final static String DSC = "Decending";
    }

    public static class SwapMarket{
        public final static String SWAPPED = "Swapped Target";
        public final static String STANDARD = "Standard Target";
    }

    private String m_type = SortType.LIQUIDITY_VOL;
    private String m_direction = SortDirection.DSC;


    private SimpleBooleanProperty m_isTargetSwappedProperty = new SimpleBooleanProperty(true);

    public SpectrumSort(){

    }

    public SpectrumSort(String type){
        m_type = type;
    }

    public SpectrumSort(String type, String direction){
        m_type = type;
        m_direction = direction;
    }

    public SpectrumSort(String type, String direction, String swapTarget){
        m_type = type;
        m_direction = direction;
        setSwapTarget(swapTarget);        
    }

    public SpectrumSort(JsonObject json){
        if(json != null){
            JsonElement typeElement = json.get("type");
            JsonElement directionElement = json.get("direction");
            JsonElement isSwappedElement = json.get("isTargetSwapped");

            m_type = typeElement != null && typeElement.isJsonPrimitive() ? typeElement.getAsString() : m_type;
            m_direction = directionElement != null && directionElement.isJsonPrimitive() ? directionElement.getAsString() : m_direction;
            m_isTargetSwappedProperty.set(isSwappedElement != null && isSwappedElement.isJsonPrimitive() ? isSwappedElement.getAsBoolean() : true); 
        }
    }

    public String getType(){
        return m_type;
    }

    public void setType(String sortType){
        m_type = sortType;
    }

    public String getDirection(){
        return m_direction;
    }

    public void setDirection(String direction){
        m_direction = direction;
    }

    public boolean isAsc(){
        return m_direction.equals(SortDirection.ASC);
    }

    public void setSwapTarget(String swapTarget){
      
        m_isTargetSwappedProperty.set(swapTarget.equals(SwapMarket.SWAPPED) ? true : false);
    }

    public String getSwaptTarget(){
        return m_isTargetSwappedProperty.get() ? SwapMarket.SWAPPED : SwapMarket.STANDARD;
    }
    
    public SimpleBooleanProperty isTargetSwappedProperty(){
        return m_isTargetSwappedProperty;
    }
    public boolean isTargetSwapped(){
        return m_isTargetSwappedProperty.get();
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
        json.addProperty("type", m_type);
        json.addProperty("direction", m_direction);
        json.addProperty("isTargetSwapped", m_isTargetSwappedProperty.get());
        return json;
    }
}
