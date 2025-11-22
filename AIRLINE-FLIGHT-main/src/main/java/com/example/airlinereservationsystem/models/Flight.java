package com.example.airlinereservationsystem.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Flight {
    private final StringProperty flightName;
    private final IntegerProperty flightCode;
    private final StringProperty classCode;
    private final IntegerProperty totalExeSeats;
    private final IntegerProperty totalEcoSeats;

    public Flight(String flightName, int flightCode, String classCode, int totalExeSeats, int totalEcoSeats) {
        this.flightName = new SimpleStringProperty(flightName);
        this.flightCode = new SimpleIntegerProperty(flightCode);
        this.classCode = new SimpleStringProperty(classCode);
        this.totalExeSeats = new SimpleIntegerProperty(totalExeSeats);
        this.totalEcoSeats = new SimpleIntegerProperty(totalEcoSeats);
    }

    // Property methods for JavaFX binding
    public StringProperty flightNameProperty() { return flightName; }
    public IntegerProperty flightCodeProperty() { return flightCode; }
    public StringProperty classCodeProperty() { return classCode; }
    public IntegerProperty totalExeSeatsProperty() { return totalExeSeats; }
    public IntegerProperty totalEcoSeatsProperty() { return totalEcoSeats; }
    
    // Getter methods for PropertyValueFactory
    public String getFlightName() { return flightName.get(); }
    public int getFlightCode() { return flightCode.get(); }
    public String getClassCode() { return classCode.get(); }
    public int getTotalExeSeats() { return totalExeSeats.get(); }
    public int getTotalEcoSeats() { return totalEcoSeats.get(); }
    
    // Setter methods
    public void setFlightName(String flightName) { this.flightName.set(flightName); }
    public void setFlightCode(int flightCode) { this.flightCode.set(flightCode); }
    public void setClassCode(String classCode) { this.classCode.set(classCode); }
    public void setTotalExeSeats(int totalExeSeats) { this.totalExeSeats.set(totalExeSeats); }
    public void setTotalEcoSeats(int totalEcoSeats) { this.totalEcoSeats.set(totalEcoSeats); }
    
    // Additional properties for reservation system
    private String route;
    private String departureTime;
    private String arrivalTime;
    private int availableSeats;
    private double baseFare;
    
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    
    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
    
    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
    
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    
    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }
}