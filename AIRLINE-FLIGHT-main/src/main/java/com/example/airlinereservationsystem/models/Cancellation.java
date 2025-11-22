package com.example.airlinereservationsystem.models;

import javafx.beans.property.*;

import java.time.LocalDateTime;

public class Cancellation {
    private final IntegerProperty customerCode;
    private final StringProperty seatClass;
    private final IntegerProperty seatNumber;
    private final DoubleProperty basicAmount;
    private final DoubleProperty cancelAmount;
    private final ObjectProperty<LocalDateTime> dateTime;

    public Cancellation(int customerCode, String seatClass, int seatNumber,
                        double basicAmount, double cancelAmount, LocalDateTime dateTime) {
        this.customerCode = new SimpleIntegerProperty(customerCode);
        this.seatClass = new SimpleStringProperty(seatClass);
        this.seatNumber = new SimpleIntegerProperty(seatNumber);
        this.basicAmount = new SimpleDoubleProperty(basicAmount);
        this.cancelAmount = new SimpleDoubleProperty(cancelAmount);
        this.dateTime = new SimpleObjectProperty<>(dateTime);
    }

    public IntegerProperty customerCodeProperty() { return customerCode; }
    public StringProperty seatClassProperty() { return seatClass; }
    public IntegerProperty seatNumberProperty() { return seatNumber; }
    public DoubleProperty basicAmountProperty() { return basicAmount; }
    public DoubleProperty cancelAmountProperty() { return cancelAmount; }
    public ObjectProperty<LocalDateTime> dateTimeProperty() { return dateTime; }
}
