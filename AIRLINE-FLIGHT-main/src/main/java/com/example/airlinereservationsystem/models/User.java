package com.example.airlinereservationsystem.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User {
    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private LocalDateTime createdAt;
    
    public User(String username, String password, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }
    
    public User(int userId, String username, String fullName, String role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }
    
    // Getters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // Setters
    public void setUserId(int userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Role checks
    public boolean isAdmin() { return "Admin".equals(role); }
    public boolean isStaff() { return "Staff".equals(role) || isAdmin(); }
    public boolean isCustomer() { return "Customer".equals(role); }
    
    // Formatted created date for display
    public String getFormattedCreatedAt() {
        if (createdAt != null) {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        return "Unknown";
    }
    
    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}