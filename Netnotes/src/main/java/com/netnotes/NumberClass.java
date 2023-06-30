package com.netnotes;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class NumberClass {

    public SimpleDoubleProperty high = new SimpleDoubleProperty();
    public SimpleDoubleProperty low = new SimpleDoubleProperty();
    public SimpleDoubleProperty sum = new SimpleDoubleProperty();
    public SimpleIntegerProperty count = new SimpleIntegerProperty();

    public NumberClass() {
        count.set(0);
        sum.set(0);
        high.set(0);
        low.set(0);
    }

    public double getAverage() {
        return count.get() == 0 ? 0 : sum.get() / count.get();
    }

}

/*
 *     public NumberClass(JsonObject json) {
        JsonElement countElement = json.get("count");
        JsonElement sumElement = json.get("sum");
        JsonElement lowElement = json.get("low");
        JsonElement highElement = json.get("high");

        count.set(countElement == null ? 0 : countElement.getAsInt());
        sum.set(sumElement == null ? 0 : sumElement.getAsDouble());
        high.set(highElement == null ? 0 : highElement.getAsDouble());
        low.set(lowElement == null ? 0 : lowElement.getAsDouble());

   public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("high", high.get());
        jsonObject.addProperty("low", low.get());
        jsonObject.addProperty("sum", sum.get());
        jsonObject.addProperty("count", count.get());

        return jsonObject;
    }
    }
 */
