package com.example.airlinereservationsystem.models;

import javafx.beans.property.*;

import java.time.LocalDate;

public class ReservedSeat {
    private final StringProperty flightCode;
    private final IntegerProperty reservedEco;
    private final IntegerProperty reservedExe;
    private final ObjectProperty<LocalDate> travelDate;
    private final IntegerProperty waitingNumber;

    public ReservedSeat(String flightCode, int reservedEco, int reservedExe,
                        LocalDate travelDate, int waitingNumber) {
        this.flightCode = new SimpleStringProperty(flightCode);
        this.reservedEco = new SimpleIntegerProperty(reservedEco);
        this.reservedExe = new SimpleIntegerProperty(reservedExe);
        this.travelDate = new SimpleObjectProperty<>(travelDate);
        this.waitingNumber = new SimpleIntegerProperty(waitingNumber);
    }

    public StringProperty flightCodeProperty() { return flightCode; }
    public IntegerProperty reservedEcoProperty() { return reservedEco; }
    public IntegerProperty reservedExeProperty() { return reservedExe; }
    public ObjectProperty<LocalDate> travelDateProperty() { return travelDate; }
    public IntegerProperty waitingNumberProperty() { return waitingNumber; }
}
