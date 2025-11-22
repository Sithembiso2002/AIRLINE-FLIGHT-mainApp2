package com.example.airlinereservationsystem.utils;

/**
 * Utility class for calculating fare discounts based on concession categories
 */
public class ConcessionCalculator {
    
    /**
     * Calculate discount amount based on concession type
     * @param baseFare The base fare amount
     * @param concessionType The concession category
     * @return The discount amount
     */
    public static double calculateDiscount(double baseFare, String concessionType) {
        if (concessionType == null || "None".equals(concessionType)) {
            return 0.0;
        }
        
        switch (concessionType) {
            case "Student":
                return baseFare * 0.25; // 25% discount
            case "Senior Citizen":
                return baseFare * 0.13; // 13% discount
            case "Cancer Patient":
                return baseFare * 0.569; // 56.9% discount
            default:
                return 0.0;
        }
    }
    
    /**
     * Calculate final fare after applying discount
     * @param baseFare The base fare amount
     * @param concessionType The concession category
     * @return The final fare after discount
     */
    public static double calculateFinalFare(double baseFare, String concessionType) {
        double discount = calculateDiscount(baseFare, concessionType);
        return baseFare - discount;
    }
    
    /**
     * Get discount percentage for display
     * @param concessionType The concession category
     * @return The discount percentage as string
     */
    public static String getDiscountPercentage(String concessionType) {
        if (concessionType == null || "None".equals(concessionType)) {
            return "0%";
        }
        
        switch (concessionType) {
            case "Student":
                return "25%";
            case "Senior Citizen":
                return "13%";
            case "Cancer Patient":
                return "56.9%";
            default:
                return "0%";
        }
    }
}