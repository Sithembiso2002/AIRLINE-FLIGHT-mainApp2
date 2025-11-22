package com.example.airlinereservationsystem.services;

import com.example.airlinereservationsystem.DatabaseConnection;
import com.example.airlinereservationsystem.models.*;
import com.example.airlinereservationsystem.utils.ConcessionCalculator;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for handling all reservation-related operations
 * Implements business logic for both Staff and Customer roles
 */
public class ReservationService {
    private static final Logger LOGGER = Logger.getLogger(ReservationService.class.getName());
    
    /**
     * Search available flights based on criteria
     */
    public List<Flight> searchFlights(LocalDate travelDate, String seatClass, String route) throws SQLException {
        List<Flight> flights = new ArrayList<>();
        
        String sql = """
            SELECT f.*, fa.source_place, fa.dest_place, fa.depart_time, fa.arrival_time, fa.fare as base_fare,
                   CASE WHEN ? = 'Economy' THEN f.total_eco_seats ELSE f.total_exe_seats END as total_seats,
                   COALESCE(r.reserved_seats, 0) as reserved_seats,
                   (CASE WHEN ? = 'Economy' THEN f.total_eco_seats ELSE f.total_exe_seats END - COALESCE(r.reserved_seats, 0)) as available_seats
            FROM flights f
            LEFT JOIN fare fa ON f.flight_code = fa.flight_code
            LEFT JOIN (
                SELECT flight_code, seat_class, COUNT(*) as reserved_seats
                FROM reservations 
                WHERE travel_date = ? AND status = 'Confirmed'
                GROUP BY flight_code, seat_class
            ) r ON f.flight_code = r.flight_code AND r.seat_class = ?
            WHERE (CASE WHEN ? = 'Economy' THEN f.total_eco_seats ELSE f.total_exe_seats END - COALESCE(r.reserved_seats, 0)) > 0
              AND (? = 'Any Route' OR CONCAT(COALESCE(fa.source_place, 'Maseru'), ' → ', COALESCE(fa.dest_place, 'Johannesburg')) = ?)
              AND (fa.class_code = ? OR fa.class_code = 'BOTH')
            ORDER BY f.flight_name
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, seatClass);
            ps.setString(2, seatClass);
            ps.setDate(3, Date.valueOf(travelDate));
            ps.setString(4, seatClass);
            ps.setString(5, seatClass);
            ps.setString(6, route);
            ps.setString(7, route);
            ps.setString(8, seatClass);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Flight flight = new Flight(
                        rs.getString("flight_name"),
                        rs.getInt("flight_code"),
                        rs.getString("class_code"),
                        rs.getInt("total_exe_seats"),
                        rs.getInt("total_eco_seats")
                    );
                    
                    flight.setAvailableSeats(rs.getInt("available_seats"));
                    
                    // Get fare from database or use default
                    double dbFare = rs.getDouble("base_fare");
                    flight.setBaseFare(dbFare > 0 ? dbFare : (seatClass.equals("Economy") ? 850.0 : 2040.0));
                    
                    // Get route from database or use default
                    String source = rs.getString("source_place");
                    String dest = rs.getString("dest_place");
                    flight.setRoute(source != null && dest != null ? source + " → " + dest : "Maseru → Johannesburg");
                    
                    // Get times from database or use default
                    Time departTime = rs.getTime("depart_time");
                    Time arrivalTime = rs.getTime("arrival_time");
                    flight.setDepartureTime(departTime != null ? departTime.toString().substring(0,5) : "09:00");
                    flight.setArrivalTime(arrivalTime != null ? arrivalTime.toString().substring(0,5) : "11:30");
                    
                    flights.add(flight);
                }
            }
        }
        
        return flights;
    }
    
    /**
     * Make a reservation with seat assignment
     */
    public ReservationResult makeReservation(Customer customer, int flightCode, String seatClass, 
                                           String seatPreference, LocalDate travelDate) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Insert or update customer
            int customerId = insertOrUpdateCustomer(conn, customer);
            
            // 2. Check seat availability and assign
            int assignedSeat = assignSeat(conn, flightCode, seatClass, seatPreference);
            
            if (assignedSeat > 0) {
                // 3. Create confirmed reservation
                String pnr = generatePNR();
                double baseFare = seatClass.equals("Economy") ? 850.0 : 2040.0;
                double finalFare = ConcessionCalculator.calculateFinalFare(baseFare, customer.getConcession());
                
                insertReservation(conn, customerId, flightCode, seatClass, 
                                assignedSeat, pnr, "Confirmed", finalFare, travelDate);
                
                conn.commit();
                return new ReservationResult(true, pnr, assignedSeat, finalFare, 0);
                
            } else {
                // 4. Add to waiting list
                int waitingNumber = addToWaitingList(conn, customerId, flightCode, seatClass, travelDate);
                
                conn.commit();
                return new ReservationResult(false, null, 0, 0, waitingNumber);
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to close connection", e);
                }
            }
        }
    }
    
    /**
     * Cancel reservation and process refund
     */
    public CancellationResult cancelReservation(String pnr) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Get reservation details
            Reservation reservation = getReservationByPNR(conn, pnr);
            if (reservation == null) {
                throw new SQLException("Reservation not found with PNR: " + pnr);
            }
            
            // 2. Calculate refund
            RefundCalculation refund = calculateRefund(reservation);
            
            // 3. Update reservation status
            updateReservationStatus(conn, reservation.getReservationId(), "Cancelled");
            
            // 4. Insert cancellation record
            insertCancellation(conn, reservation.getReservationId(), refund.getRefundAmount(), 
                             refund.getCancellationFee());
            
            // 5. Free up the seat and promote waiting list
            promoteWaitingList(conn, reservation.getFlightCode(), reservation.getSeatClass(), 
                             reservation.getTravelDate());
            
            conn.commit();
            return new CancellationResult(true, refund.getRefundAmount(), refund.getCancellationFee());
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to close connection", e);
                }
            }
        }
    }
    
    // Private helper methods
    
    private int insertOrUpdateCustomer(Connection conn, Customer customer) throws SQLException {
        // Check if customer exists
        String checkSql = "SELECT cust_id FROM customer_details WHERE tel_no = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, customer.getPhoneNumber());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cust_id");
                }
            }
        }
        
        // Insert new customer
        String insertSql = """
            INSERT INTO customer_details (cust_name, father_name, gender, dob, address, tel_no, profession, concession, travel_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getFatherName());
            ps.setString(3, customer.getGender());
            ps.setDate(4, Date.valueOf(customer.getDateOfBirth()));
            ps.setString(5, customer.getAddress());
            ps.setString(6, customer.getPhoneNumber());
            ps.setString(7, customer.getProfession());
            ps.setString(8, customer.getConcession());
            ps.setDate(9, Date.valueOf(customer.getTravelDate()));
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        throw new SQLException("Failed to insert customer");
    }
    
    private int assignSeat(Connection conn, int flightCode, String seatClass, String seatPreference) throws SQLException {
        // Get total seats for the class
        String seatsSql = """
            SELECT CASE WHEN ? = 'Economy' THEN total_eco_seats ELSE total_exe_seats END as total_seats
            FROM flights WHERE flight_code = ?
        """;
        
        int totalSeats = 0;
        try (PreparedStatement ps = conn.prepareStatement(seatsSql)) {
            ps.setString(1, seatClass);
            ps.setInt(2, flightCode);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalSeats = rs.getInt("total_seats");
                }
            }
        }
        
        if (totalSeats == 0) return 0;
        
        // Get reserved seats
        String reservedSql = """
            SELECT seat_number FROM reservations 
            WHERE flight_code = ? AND seat_class = ? AND status = 'Confirmed'
        """;
        
        boolean[] reserved = new boolean[totalSeats + 1];
        try (PreparedStatement ps = conn.prepareStatement(reservedSql)) {
            ps.setInt(1, flightCode);
            ps.setString(2, seatClass);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reserved[rs.getInt("seat_number")] = true;
                }
            }
        }
        
        // Assign seat based on preference
        if ("Window".equals(seatPreference)) {
            for (int i = 1; i <= totalSeats; i++) {
                if (!reserved[i] && isWindowSeat(i)) {
                    return i;
                }
            }
        }
        
        // Find any available seat
        for (int i = 1; i <= totalSeats; i++) {
            if (!reserved[i]) {
                return i;
            }
        }
        
        return 0;
    }
    
    private boolean isWindowSeat(int seatNumber) {
        int seatInRow = ((seatNumber - 1) % 6) + 1;
        return seatInRow == 1 || seatInRow == 6;
    }
    
    private void insertReservation(Connection conn, int customerId, int flightCode, String seatClass,
                                 int seatNumber, String pnr, String status, double fare, LocalDate travelDate) throws SQLException {
        String sql = """
            INSERT INTO reservations (cust_id, flight_code, seat_class, seat_number, status, fare, travel_date, pnr)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setInt(2, flightCode);
            ps.setString(3, seatClass);
            ps.setInt(4, seatNumber);
            ps.setString(5, status);
            ps.setDouble(6, fare);
            ps.setDate(7, Date.valueOf(travelDate));
            ps.setString(8, pnr);
            
            ps.executeUpdate();
        }
    }
    
    private int addToWaitingList(Connection conn, int customerId, int flightCode, String seatClass, LocalDate travelDate) throws SQLException {
        String countSql = """
            SELECT COALESCE(MAX(waiting_no), 0) + 1 as next_waiting 
            FROM waiting_list 
            WHERE flight_code = ? AND seat_class = ?
        """;
        
        int waitingNumber = 1;
        try (PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setInt(1, flightCode);
            ps.setString(2, seatClass);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    waitingNumber = rs.getInt("next_waiting");
                }
            }
        }
        
        String insertSql = """
            INSERT INTO waiting_list (flight_code, cust_id, seat_class, waiting_no, travel_date)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, flightCode);
            ps.setInt(2, customerId);
            ps.setString(3, seatClass);
            ps.setInt(4, waitingNumber);
            ps.setDate(5, Date.valueOf(travelDate));
            
            ps.executeUpdate();
        }
        
        return waitingNumber;
    }
    
    private String generatePNR() {
        return "PNR" + System.currentTimeMillis() % 1000000;
    }
    
    private Reservation getReservationByPNR(Connection conn, String pnr) throws SQLException {
        String sql = """
            SELECT r.*, f.flight_name, c.cust_name
            FROM reservations r
            JOIN flights f ON r.flight_code = f.flight_code
            JOIN customer_details c ON r.cust_id = c.cust_id
            WHERE r.pnr = ? AND r.status = 'Confirmed'
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pnr);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reservation reservation = new Reservation();
                    reservation.setReservationId(rs.getInt("reservation_id"));
                    reservation.setPnr(rs.getString("pnr"));
                    reservation.setCustomerId(rs.getInt("cust_id"));
                    reservation.setFlightCode(rs.getInt("flight_code"));
                    reservation.setSeatClass(rs.getString("seat_class"));
                    reservation.setSeatNumber(rs.getInt("seat_number"));
                    reservation.setStatus(rs.getString("status"));
                    reservation.setFare(rs.getDouble("fare"));
                    reservation.setTravelDate(rs.getDate("travel_date").toLocalDate());
                    reservation.setFlightName(rs.getString("flight_name"));
                    reservation.setCustomerName(rs.getString("cust_name"));
                    
                    return reservation;
                }
            }
        }
        
        return null;
    }
    
    private RefundCalculation calculateRefund(Reservation reservation) {
        LocalDate travelDate = reservation.getTravelDate();
        LocalDate today = LocalDate.now();
        double fare = reservation.getFare();
        
        long daysUntilTravel = java.time.temporal.ChronoUnit.DAYS.between(today, travelDate);
        
        double cancellationFee;
        if (daysUntilTravel < 0) {
            cancellationFee = fare;
        } else if (daysUntilTravel < 1) {
            cancellationFee = fare * 0.25;
        } else {
            cancellationFee = fare * 0.10;
        }
        
        double refundAmount = fare - cancellationFee;
        return new RefundCalculation(refundAmount, cancellationFee);
    }
    
    private void updateReservationStatus(Connection conn, int reservationId, String status) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        }
    }
    
    private void insertCancellation(Connection conn, int reservationId, double refundAmount, 
                                  double cancellationFee) throws SQLException {
        String sql = """
            INSERT INTO cancellations (reservation_id, cancel_date, refund_amount, cancellation_fee)
            VALUES (?, CURRENT_TIMESTAMP, ?, ?)
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            ps.setDouble(2, refundAmount);
            ps.setDouble(3, cancellationFee);
            ps.executeUpdate();
        }
    }
    
    private void promoteWaitingList(Connection conn, int flightCode, String seatClass, LocalDate travelDate) throws SQLException {
        String waitingSql = """
            SELECT w.*, c.cust_name
            FROM waiting_list w
            JOIN customer_details c ON w.cust_id = c.cust_id
            WHERE w.flight_code = ? AND w.seat_class = ? AND w.travel_date = ?
            ORDER BY w.waiting_no
            LIMIT 1
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(waitingSql)) {
            ps.setInt(1, flightCode);
            ps.setString(2, seatClass);
            ps.setDate(3, Date.valueOf(travelDate));
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int customerId = rs.getInt("cust_id");
                    int waitId = rs.getInt("wait_id");
                    
                    int assignedSeat = assignSeat(conn, flightCode, seatClass, "Any");
                    
                    if (assignedSeat > 0) {
                        String pnr = generatePNR();
                        double baseFare = seatClass.equals("Economy") ? 850.0 : 2040.0;
                        
                        insertReservation(conn, customerId, flightCode, seatClass, assignedSeat, 
                                        pnr, "Confirmed", baseFare, travelDate);
                        
                        String deleteSql = "DELETE FROM waiting_list WHERE wait_id = ?";
                        try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                            deletePs.setInt(1, waitId);
                            deletePs.executeUpdate();
                        }
                        
                        LOGGER.info("Promoted waiting list customer to confirmed reservation: " + pnr);
                    }
                }
            }
        }
    }
    
    /**
     * Get all reservations with pagination
     */
    public List<Reservation> getAllReservations(int offset, int limit) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        
        String sql = """
            SELECT r.*, f.flight_name, c.cust_name
            FROM reservations r
            JOIN flights f ON r.flight_code = f.flight_code
            JOIN customer_details c ON r.cust_id = c.cust_id
            ORDER BY r.reservation_id DESC
            LIMIT ? OFFSET ?
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation reservation = new Reservation();
                    reservation.setReservationId(rs.getInt("reservation_id"));
                    reservation.setPnr(rs.getString("pnr"));
                    reservation.setCustomerId(rs.getInt("cust_id"));
                    reservation.setFlightCode(rs.getInt("flight_code"));
                    reservation.setSeatClass(rs.getString("seat_class"));
                    reservation.setSeatNumber(rs.getInt("seat_number"));
                    reservation.setStatus(rs.getString("status"));
                    reservation.setFare(rs.getDouble("fare"));
                    reservation.setTravelDate(rs.getDate("travel_date").toLocalDate());
                    reservation.setFlightName(rs.getString("flight_name"));
                    reservation.setCustomerName(rs.getString("cust_name"));
                    
                    reservations.add(reservation);
                }
            }
        }
        
        return reservations;
    }
    
    // Result classes
    public static class ReservationResult {
        private final boolean confirmed;
        private final String pnr;
        private final int seatNumber;
        private final double fare;
        private final int waitingNumber;
        
        public ReservationResult(boolean confirmed, String pnr, int seatNumber, double fare, int waitingNumber) {
            this.confirmed = confirmed;
            this.pnr = pnr;
            this.seatNumber = seatNumber;
            this.fare = fare;
            this.waitingNumber = waitingNumber;
        }
        
        public boolean isConfirmed() { return confirmed; }
        public String getPnr() { return pnr; }
        public int getSeatNumber() { return seatNumber; }
        public double getFare() { return fare; }
        public int getWaitingNumber() { return waitingNumber; }
    }
    
    public static class CancellationResult {
        private final boolean success;
        private final double refundAmount;
        private final double cancellationFee;
        
        public CancellationResult(boolean success, double refundAmount, double cancellationFee) {
            this.success = success;
            this.refundAmount = refundAmount;
            this.cancellationFee = cancellationFee;
        }
        
        public boolean isSuccess() { return success; }
        public double getRefundAmount() { return refundAmount; }
        public double getCancellationFee() { return cancellationFee; }
    }
    
    private static class RefundCalculation {
        private final double refundAmount;
        private final double cancellationFee;
        
        public RefundCalculation(double refundAmount, double cancellationFee) {
            this.refundAmount = refundAmount;
            this.cancellationFee = cancellationFee;
        }
        
        public double getRefundAmount() { return refundAmount; }
        public double getCancellationFee() { return cancellationFee; }
    }
}