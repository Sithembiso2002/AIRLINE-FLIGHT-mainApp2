package com.example.airlinereservationsystem.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Fleet {
    private final StringProperty aircraftNumber;
    private final IntegerProperty clubPreCapacity;
    private final IntegerProperty ecoCapacity;
    private final StringProperty engineType;
    private final StringProperty cruiseSpeed;
    private final StringProperty airLength;
    private final StringProperty wingSpan;

    public Fleet(String aircraftNumber, int clubPreCapacity, int ecoCapacity,
                 String engineType, String cruiseSpeed, String airLength, String wingSpan) {
        this.aircraftNumber = new SimpleStringProperty(aircraftNumber);
        this.clubPreCapacity = new SimpleIntegerProperty(clubPreCapacity);
        this.ecoCapacity = new SimpleIntegerProperty(ecoCapacity);
        this.engineType = new SimpleStringProperty(engineType);
        this.cruiseSpeed = new SimpleStringProperty(cruiseSpeed);
        this.airLength = new SimpleStringProperty(airLength);
        this.wingSpan = new SimpleStringProperty(wingSpan);
    }

    public StringProperty aircraftNumberProperty() { return aircraftNumber; }
    public IntegerProperty clubPreCapacityProperty() { return clubPreCapacity; }
    public IntegerProperty ecoCapacityProperty() { return ecoCapacity; }
    public StringProperty engineTypeProperty() { return engineType; }
    public StringProperty cruiseSpeedProperty() { return cruiseSpeed; }
    public StringProperty airLengthProperty() { return airLength; }
    public StringProperty wingSpanProperty() { return wingSpan; }
}
