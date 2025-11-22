package com.example.airlinereservationsystem.models;

import java.time.LocalDate;

/**
 * Customer model representing customer details
 */
public class Customer {
    private int custId;
    private String custName;
    private String fatherName;
    private String gender;
    private LocalDate dob;
    private String address;
    private String telNo;
    private String profession;
    private String security;
    private String concession;
    private LocalDate travelDate;
    
    // Constructors
    public Customer() {}
    
    public Customer(String custName, String fatherName, String gender, LocalDate dob, 
                   String address, String telNo, String profession, String concession) {
        this.custName = custName;
        this.fatherName = fatherName;
        this.gender = gender;
        this.dob = dob;
        this.address = address;
        this.telNo = telNo;
        this.profession = profession;
        this.concession = concession;
    }
    
    // Getters and Setters
    public int getCustId() { return custId; }
    public void setCustId(int custId) { this.custId = custId; }
    
    public String getCustName() { return custName; }
    public void setCustName(String custName) { this.custName = custName; }
    
    public String getName() { return custName; }
    public void setName(String name) { this.custName = name; }
    
    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    
    public LocalDate getDateOfBirth() { return dob; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dob = dateOfBirth; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    
    public String getPhoneNumber() { return telNo; }
    public void setPhoneNumber(String phoneNumber) { this.telNo = phoneNumber; }
    
    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }
    
    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }
    
    public String getSecurityInfo() { return security; }
    public void setSecurityInfo(String securityInfo) { this.security = securityInfo; }
    
    public String getConcession() { return concession; }
    public void setConcession(String concession) { this.concession = concession; }
    
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    
    @Override
    public String toString() {
        return "Customer{" +
                "custId=" + custId +
                ", custName='" + custName + '\'' +
                ", gender='" + gender + '\'' +
                ", concession='" + concession + '\'' +
                '}';
    }
}