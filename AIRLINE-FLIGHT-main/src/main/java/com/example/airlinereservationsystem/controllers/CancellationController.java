package com.example.airlinereservationsystem.controllers;

import com.example.airlinereservationsystem.DatabaseConnection;
import com.example.airlinereservationsystem.models.Reservation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for Flight Cancellation Module
 * Handles PNR search, booking cancellation, and refund calculation
 */
public class CancellationController {
    private static final Logger LOGGER = Logger.getLogger(CancellationController.class.getName());
    
    // Search Section
    @FXML private TextField pnrField;
    @FXML private Button searchBtn;
    @FXML private ProgressIndicator searchProgress;
    
    // Booking Details Section
    @FXML private VBox bookingDetailsSection;
    @FXML private Label pnrLabel;
    @FXML private Label passengerNameLabel;
    @FXML private Label flightLabel;
    @FXML private Label travelDateLabel;
    @FXML private Label classLabel;
    @FXML private Label seatLabel;
    @FXML private Label farePaidLabel;
    @FXML private Label statusLabel;
    
    // Refund Section
    @FXML private VBox refundSection;
    @FXML private Label originalFareLabel;
    @FXML private Label cancellationFeeLabel;
    @FXML private Label refundAmountLabel;
    
    // Action Buttons
    @FXML private HBox actionButtonsSection;
    @FXML private Button cancelBookingBtn;
    @FXML private ProgressBar cancellationProgress;
    
    // All Bookings Table
    @FXML private TableView<Reservation> bookingsTable;
    @FXML private TableColumn<Reservation, String> pnrCol;
    @FXML private TableColumn<Reservation, String> nameCol;
    @FXML private TableColumn<Reservation, String> flightCol;
    @FXML private TableColumn<Reservation, String> dateCol;
    @FXML private TableColumn<Reservation, String> classCol;
    @FXML private TableColumn<Reservation, String> seatCol;
    @FXML private TableColumn<Reservation, Double> fareCol;
    @FXML private TableColumn<Reservation, String> bookingStatusCol;
    
    // Pagination
    @FXML private Button firstPageBtn;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Button lastPageBtn;
    @FXML private Label pageLabel;
    
    @FXML private Label messageLabel;
    
    private ObservableList<Reservation> allBookings = FXCollections.observableArrayList();
    private Reservation selectedBooking;
    private int currentPage = 1;
    private final int itemsPerPage = 10;
    private int totalPages = 1;
    
    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            loadAllBookings();
            hideDetailsSection();
            
            messageLabel.setText("✅ Ready to search bookings");
            LOGGER.info("CancellationController initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize CancellationController", e);
            messageLabel.setText("❌ Initialization failed");
        }
    }
    
    private void setupTableColumns() {
        pnrCol.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("passengerName"));
        flightCol.setCellValueFactory(new PropertyValueFactory<>("flightName"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("travelDate"));
        classCol.setCellValueFactory(new PropertyValueFactory<>("seatClass"));
        seatCol.setCellValueFactory(new PropertyValueFactory<>("seatNumber"));
        fareCol.setCellValueFactory(new PropertyValueFactory<>("fare"));
        bookingStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Double-click to select booking
        bookingsTable.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Reservation reservation = row.getItem();
                    pnrField.setText(reservation.getPnr());
                    onSearchBooking(null);
                }
            });
            return row;
        });
    }
    
    @FXML
    protected void onSearchBooking(ActionEvent event) {
        try {
            String pnr = pnrField.getText().trim();
            if (pnr.isEmpty()) {
                showAlert("Validation Error", "Please enter PNR or Reservation ID");
                return;
            }
            
            searchProgress.setVisible(true);
            searchBtn.setDisable(true);
            messageLabel.setText("🔍 Searching booking...");
            
            Task<Reservation> searchTask = new Task<Reservation>() {
                @Override
                protected Reservation call() throws Exception {
                    return searchBookingByPNR(pnr);
                }
            };
            
            searchTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    searchProgress.setVisible(false);
                    searchBtn.setDisable(false);
                    
                    Reservation booking = searchTask.getValue();
                    if (booking != null) {
                        selectedBooking = booking;
                        displayBookingDetails(booking);
                        calculateRefund(booking);
                        showDetailsSection();
                        messageLabel.setText("✅ Booking found");
                    } else {
                        hideDetailsSection();
                        messageLabel.setText("❌ No booking found with PNR: " + pnr);
                        showAlert("Not Found", "No booking found with PNR: " + pnr);
                    }
                });
            });
            
            searchTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    searchProgress.setVisible(false);
                    searchBtn.setDisable(false);
                    hideDetailsSection();
                    messageLabel.setText("❌ Search failed");
                    showAlert("Search Error", "Failed to search booking: " + searchTask.getException().getMessage());
                });
            });
            
            new Thread(searchTask).start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to search booking", e);
            messageLabel.setText("❌ Search failed");
        }
    }
    
    private Reservation searchBookingByPNR(String pnr) throws SQLException {
        String sql = """
            SELECT r.*, c.cust_name, f.flight_name
            FROM reservations r
            JOIN customer_details c ON r.cust_id = c.cust_id
            JOIN flights f ON r.flight_code = f.flight_code
            WHERE r.pnr = ? OR CAST(r.reservation_id AS VARCHAR) = ?
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, pnr);
            ps.setString(2, pnr);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reservation reservation = new Reservation();
                    reservation.setReservationId(rs.getInt("reservation_id"));
                    reservation.setPnr(rs.getString("pnr"));
                    reservation.setPassengerName(rs.getString("cust_name"));
                    reservation.setFlightName(rs.getString("flight_name"));
                    reservation.setTravelDate(rs.getDate("travel_date").toLocalDate());
                    reservation.setSeatClass(rs.getString("seat_class"));
                    reservation.setSeatNumber(rs.getInt("seat_number"));
                    reservation.setFare(rs.getDouble("fare"));
                    reservation.setStatus(rs.getString("status"));
                    reservation.setFlightCode(rs.getInt("flight_code"));
                    reservation.setCustId(rs.getInt("cust_id"));
                    
                    return reservation;
                }
            }
        }
        
        return null;
    }
    
    private void displayBookingDetails(Reservation booking) {
        pnrLabel.setText(booking.getPnr());
        passengerNameLabel.setText(booking.getPassengerName());
        flightLabel.setText(booking.getFlightName());
        travelDateLabel.setText(booking.getTravelDate().toString());
        classLabel.setText(booking.getSeatClass());
        seatLabel.setText(String.valueOf(booking.getSeatNumber()));
        farePaidLabel.setText(String.format("M%.2f LSL", booking.getFare()));
        statusLabel.setText(booking.getStatus());
        
        // Set status color
        if ("Confirmed".equals(booking.getStatus())) {
            statusLabel.setStyle("-fx-text-fill: #27ae60;");
        } else if ("Cancelled".equals(booking.getStatus())) {
            statusLabel.setStyle("-fx-text-fill: #e74c3c;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #f39c12;");
        }
    }
    
    private void calculateRefund(Reservation booking) {
        double originalFare = booking.getFare();
        double cancellationFee = 0.0;
        double refundAmount = 0.0;
        
        if ("Cancelled".equals(booking.getStatus())) {
            // Already cancelled
            cancellationFee = originalFare;
            refundAmount = 0.0;
        } else {
            // Calculate cancellation fee based on time until departure
            LocalDate travelDate = booking.getTravelDate();
            LocalDate today = LocalDate.now();
            long daysUntilTravel = ChronoUnit.DAYS.between(today, travelDate);
            
            if (daysUntilTravel > 1) {
                // More than 24 hours: 10% fee
                cancellationFee = originalFare * 0.10;
            } else if (daysUntilTravel >= 0) {
                // Within 24 hours: 25% fee
                cancellationFee = originalFare * 0.25;
            } else {
                // Past travel date: No refund
                cancellationFee = originalFare;
            }
            
            refundAmount = originalFare - cancellationFee;
        }
        
        originalFareLabel.setText(String.format("M%.2f LSL", originalFare));
        cancellationFeeLabel.setText(String.format("M%.2f LSL", cancellationFee));
        refundAmountLabel.setText(String.format("M%.2f LSL", refundAmount));
        
        // Enable/disable cancel button
        cancelBookingBtn.setDisable("Cancelled".equals(booking.getStatus()));
    }
    
    @FXML
    protected void onCancelBooking(ActionEvent event) {
        try {
            if (selectedBooking == null) {
                showAlert("Error", "No booking selected");
                return;
            }
            
            // Confirmation dialog
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Cancellation");
            confirm.setHeaderText("Cancel Booking");
            confirm.setContentText("Are you sure you want to cancel this booking?\n\n" +
                    "PNR: " + selectedBooking.getPnr() + "\n" +
                    "Passenger: " + selectedBooking.getPassengerName() + "\n" +
                    "Refund: " + refundAmountLabel.getText());
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    processCancellation();
                }
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to cancel booking", e);
            messageLabel.setText("❌ Cancellation failed");
        }
    }
    
    private void processCancellation() {
        cancellationProgress.setVisible(true);
        cancelBookingBtn.setDisable(true);
        messageLabel.setText("❌ Processing cancellation...");
        
        Task<Boolean> cancellationTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return cancelBookingInDatabase();
            }
        };
        
        cancellationTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                cancellationProgress.setVisible(false);
                cancelBookingBtn.setDisable(false);
                
                if (cancellationTask.getValue()) {
                    messageLabel.setText("✅ Booking cancelled successfully");
                    showAlert("Cancellation Successful", 
                            "Booking cancelled successfully!\n\n" +
                            "Refund amount: " + refundAmountLabel.getText() + "\n" +
                            "Refund will be processed within 5-7 business days.");
                    
                    // Refresh the booking details
                    onSearchBooking(null);
                    loadAllBookings();
                } else {
                    messageLabel.setText("❌ Cancellation failed");
                    showAlert("Cancellation Failed", "Failed to cancel booking. Please try again.");
                }
            });
        });
        
        cancellationTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                cancellationProgress.setVisible(false);
                cancelBookingBtn.setDisable(false);
                messageLabel.setText("❌ Cancellation failed");
                showAlert("Cancellation Error", "Failed to cancel booking: " + cancellationTask.getException().getMessage());
            });
        });
        
        new Thread(cancellationTask).start();
    }
    
    private boolean cancelBookingInDatabase() throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Update reservation status
            String updateReservationSql = "UPDATE reservations SET status = 'Cancelled' WHERE reservation_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateReservationSql)) {
                ps.setInt(1, selectedBooking.getReservationId());
                ps.executeUpdate();
            }
            
            // 2. Insert cancellation record
            String insertCancellationSql = """
                INSERT INTO cancellations (reservation_id, cancel_date, refund_amount, cancellation_fee)
                VALUES (?, CURRENT_DATE, ?, ?)
            """;
            
            double refundAmount = Double.parseDouble(refundAmountLabel.getText().replace("M", "").replace(" LSL", ""));
            double cancellationFee = Double.parseDouble(cancellationFeeLabel.getText().replace("M", "").replace(" LSL", ""));
            
            try (PreparedStatement ps = conn.prepareStatement(insertCancellationSql)) {
                ps.setInt(1, selectedBooking.getReservationId());
                ps.setDouble(2, refundAmount);
                ps.setDouble(3, cancellationFee);
                ps.executeUpdate();
            }
            
            // 3. Check waiting list and promote if available
            promoteFromWaitingList(conn, selectedBooking.getFlightCode(), selectedBooking.getSeatClass());
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback cancellation", rollbackEx);
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
    
    private void promoteFromWaitingList(Connection conn, int flightCode, String seatClass) throws SQLException {
        // Get first person from waiting list
        String waitingSql = """
            SELECT w.*, c.cust_name
            FROM waiting_list w
            JOIN customer_details c ON w.cust_id = c.cust_id
            WHERE w.flight_code = ? AND w.seat_class = ?
            ORDER BY w.waiting_no
            LIMIT 1
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(waitingSql)) {
            ps.setInt(1, flightCode);
            ps.setString(2, seatClass);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int custId = rs.getInt("cust_id");
                    LocalDate travelDate = rs.getDate("travel_date").toLocalDate();
                    
                    // Create confirmed reservation
                    String insertReservationSql = """
                        INSERT INTO reservations (cust_id, flight_code, seat_class, seat_number, status, fare, travel_date, pnr)
                        VALUES (?, ?, ?, ?, 'Confirmed', ?, ?, ?)
                    """;
                    
                    try (PreparedStatement insertPs = conn.prepareStatement(insertReservationSql)) {
                        insertPs.setInt(1, custId);
                        insertPs.setInt(2, flightCode);
                        insertPs.setString(3, seatClass);
                        insertPs.setInt(4, selectedBooking.getSeatNumber()); // Assign the cancelled seat
                        insertPs.setDouble(5, selectedBooking.getFare());
                        insertPs.setDate(6, Date.valueOf(travelDate));
                        insertPs.setString(7, "PNR" + System.currentTimeMillis() % 1000000);
                        
                        insertPs.executeUpdate();
                    }
                    
                    // Remove from waiting list
                    String deleteWaitingSql = "DELETE FROM waiting_list WHERE flight_code = ? AND cust_id = ? AND seat_class = ?";
                    try (PreparedStatement deletePs = conn.prepareStatement(deleteWaitingSql)) {
                        deletePs.setInt(1, flightCode);
                        deletePs.setInt(2, custId);
                        deletePs.setString(3, seatClass);
                        deletePs.executeUpdate();
                    }
                    
                    LOGGER.info("Promoted customer from waiting list: " + rs.getString("cust_name"));
                }
            }
        }
    }
    
    @FXML
    protected void onSearchAnother(ActionEvent event) {
        pnrField.clear();
        hideDetailsSection();
        messageLabel.setText("✅ Ready to search another booking");
    }
    
    @FXML
    protected void onRefreshBookings(ActionEvent event) {
        loadAllBookings();
        messageLabel.setText("✅ Bookings refreshed");
    }
    
    private void loadAllBookings() {
        Task<ObservableList<Reservation>> loadTask = new Task<ObservableList<Reservation>>() {
            @Override
            protected ObservableList<Reservation> call() throws Exception {
                return loadBookingsFromDatabase();
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            allBookings.clear();
            allBookings.addAll(loadTask.getValue());
            
            totalPages = (int) Math.ceil((double) allBookings.size() / itemsPerPage);
            currentPage = 1;
            updatePagination();
        });
        
        loadTask.setOnFailed(e -> {
            LOGGER.log(Level.SEVERE, "Failed to load bookings", loadTask.getException());
        });
        
        new Thread(loadTask).start();
    }
    
    private ObservableList<Reservation> loadBookingsFromDatabase() throws SQLException {
        ObservableList<Reservation> bookings = FXCollections.observableArrayList();
        
        String sql = """
            SELECT r.*, c.cust_name, f.flight_name
            FROM reservations r
            JOIN customer_details c ON r.cust_id = c.cust_id
            JOIN flights f ON r.flight_code = f.flight_code
            ORDER BY r.reservation_id DESC
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Reservation reservation = new Reservation();
                reservation.setReservationId(rs.getInt("reservation_id"));
                reservation.setPnr(rs.getString("pnr"));
                reservation.setPassengerName(rs.getString("cust_name"));
                reservation.setFlightName(rs.getString("flight_name"));
                reservation.setTravelDate(rs.getDate("travel_date").toLocalDate());
                reservation.setSeatClass(rs.getString("seat_class"));
                reservation.setSeatNumber(rs.getInt("seat_number"));
                reservation.setFare(rs.getDouble("fare"));
                reservation.setStatus(rs.getString("status"));
                
                bookings.add(reservation);
            }
        }
        
        return bookings;
    }
    
    // Pagination methods
    @FXML
    protected void onFirstPage(ActionEvent event) {
        currentPage = 1;
        updatePagination();
    }
    
    @FXML
    protected void onPreviousPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }
    
    @FXML
    protected void onNextPage(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }
    
    @FXML
    protected void onLastPage(ActionEvent event) {
        currentPage = totalPages;
        updatePagination();
    }
    
    private void updatePagination() {
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allBookings.size());
        
        ObservableList<Reservation> pageData = FXCollections.observableArrayList();
        for (int i = startIndex; i < endIndex; i++) {
            pageData.add(allBookings.get(i));
        }
        
        bookingsTable.setItems(pageData);
        
        pageLabel.setText(String.format("Page %d of %d", currentPage, Math.max(1, totalPages)));
        
        firstPageBtn.setDisable(currentPage == 1);
        prevPageBtn.setDisable(currentPage == 1);
        nextPageBtn.setDisable(currentPage == totalPages || totalPages == 0);
        lastPageBtn.setDisable(currentPage == totalPages || totalPages == 0);
    }
    
    private void showDetailsSection() {
        if (bookingDetailsSection != null) {
            bookingDetailsSection.setVisible(true);
            bookingDetailsSection.setManaged(true);
        }
        if (refundSection != null) {
            refundSection.setVisible(true);
            refundSection.setManaged(true);
        }
        if (actionButtonsSection != null) {
            actionButtonsSection.setVisible(true);
            actionButtonsSection.setManaged(true);
        }
    }
    
    private void hideDetailsSection() {
        bookingDetailsSection.setVisible(false);
        bookingDetailsSection.setManaged(false);
        refundSection.setVisible(false);
        refundSection.setManaged(false);
        actionButtonsSection.setVisible(false);
        actionButtonsSection.setManaged(false);
    }
    
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}