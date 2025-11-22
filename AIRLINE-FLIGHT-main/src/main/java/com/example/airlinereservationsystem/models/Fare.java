package com.example.airlinereservationsystem.models;

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class Fare {
    private final StringProperty routeCode;
    private final StringProperty source;
    private final StringProperty via;
    private final StringProperty destination;
    private final ObjectProperty<LocalDateTime> departureTime;
    private final ObjectProperty<LocalDateTime> arrivalTime;
    private final StringProperty flightCode;
    private final StringProperty classCode;
    private final DoubleProperty fareAmount;

    public Fare(String routeCode, String source, String via, String destination,
                LocalDateTime departureTime, LocalDateTime arrivalTime,
                String flightCode, String classCode, double fareAmount) {
        this.routeCode = new SimpleStringProperty(routeCode);
        this.source = new SimpleStringProperty(source);
        this.via = new SimpleStringProperty(via);
        this.destination = new SimpleStringProperty(destination);
        this.departureTime = new SimpleObjectProperty<>(departureTime);
        this.arrivalTime = new SimpleObjectProperty<>(arrivalTime);
        this.flightCode = new SimpleStringProperty(flightCode);
        this.classCode = new SimpleStringProperty(classCode);
        this.fareAmount = new SimpleDoubleProperty(fareAmount);
    }

    public StringProperty routeCodeProperty() { return routeCode; }
    public StringProperty sourceProperty() { return source; }
    public StringProperty viaProperty() { return via; }
    public StringProperty destinationProperty() { return destination; }
    public ObjectProperty<LocalDateTime> departureTimeProperty() { return departureTime; }
    public ObjectProperty<LocalDateTime> arrivalTimeProperty() { return arrivalTime; }
    public StringProperty flightCodeProperty() { return flightCode; }
    public StringProperty classCodeProperty() { return classCode; }
    public DoubleProperty fareAmountProperty() { return fareAmount; }
}
