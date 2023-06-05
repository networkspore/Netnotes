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
