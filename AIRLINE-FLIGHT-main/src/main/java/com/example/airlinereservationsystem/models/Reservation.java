package com.example.airlinereservationsystem.models;

import java.time.LocalDate;

/**
 * Reservation model representing flight reservations
 */
public class Reservation {
    private int reservationId;
    private int custId;
    private int flightCode;
    private String seatClass;
    private int seatNumber;
    private String status;
    private double fare;
    private LocalDate travelDate;
    private String pnr;
    
    // Additional fields for display
    private String passengerName;
    private String flightName;
    
    // Constructors
    public Reservation() {}
    
    public Reservation(int custId, int flightCode, String seatClass, int seatNumber, 
                      String status, double fare, LocalDate travelDate, String pnr) {
        this.custId = custId;
        this.flightCode = flightCode;
        this.seatClass = seatClass;
        this.seatNumber = seatNumber;
        this.status = status;
        this.fare = fare;
        this.travelDate = travelDate;
        this.pnr = pnr;
    }
    
    // Getters and Setters
    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    
    public int getCustId() { return custId; }
    public void setCustId(int custId) { this.custId = custId; }
    
    // Alias for compatibility
    public void setCustomerId(int customerId) { this.custId = customerId; }
    
    public int getFlightCode() { return flightCode; }
    public void setFlightCode(int flightCode) { this.flightCode = flightCode; }
    
    public String getSeatClass() { return seatClass; }
    public void setSeatClass(String seatClass) { this.seatClass = seatClass; }
    
    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }
    
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    
    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }
    
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    
    // Aliases for compatibility
    public void setCustomerName(String customerName) { this.passengerName = customerName; }
    public String getCustomerName() { return passengerName; }
    
    public String getFlightName() { return flightName; }
    public void setFlightName(String flightName) { this.flightName = flightName; }
    
    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId=" + reservationId +
                ", pnr='" + pnr + '\'' +
                ", passengerName='" + passengerName + '\'' +
                ", flightName='" + flightName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}