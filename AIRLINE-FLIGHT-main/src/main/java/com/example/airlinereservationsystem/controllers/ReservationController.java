package com.example.airlinereservationsystem.controllers;

import com.example.airlinereservationsystem.DatabaseConnection;
import com.example.airlinereservationsystem.models.Customer;
import com.example.airlinereservationsystem.models.Flight;
import com.example.airlinereservationsystem.models.Reservation;
import com.example.airlinereservationsystem.utils.ConcessionCalculator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for Flight Reservation Module
 * Handles customer registration, flight search, seat assignment, and fare calculation
 */
public class ReservationController {
    private static final Logger LOGGER = Logger.getLogger(ReservationController.class.getName());
    
    // Customer Details
    @FXML private TextField customerNameField;
    @FXML private TextField fatherNameField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private DatePicker dobPicker;
    @FXML private TextArea addressArea;
    @FXML private TextField phoneField;
    @FXML private TextField professionField;
    
    // Travel Details
    @FXML private DatePicker travelDatePicker;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> seatPreferenceCombo;
    @FXML private ComboBox<String> concessionCombo;
    
    // Flight Search
    @FXML private Button searchFlightsBtn;
    @FXML private ProgressIndicator searchProgress;
    @FXML private TableView<Flight> flightsTable;
    @FXML private TableColumn<Flight, String> flightNameCol;
    @FXML private TableColumn<Flight, String> routeCol;
    @FXML private TableColumn<Flight, String> departureCol;
    @FXML private TableColumn<Flight, String> arrivalCol;
    @FXML private TableColumn<Flight, Integer> availableSeatsCol;
    @FXML private TableColumn<Flight, Double> fareCol;
    
    // Fare Calculation
    @FXML private Label baseFareLabel;
    @FXML private Label discountLabel;
    @FXML private Label finalFareLabel;
    
    // Actions
    @FXML private Button reserveBtn;
    @FXML private ProgressBar reservationProgress;
    @FXML private Label statusLabel;
    
    private ObservableList<Flight> availableFlights = FXCollections.observableArrayList();
    private double baseFare = 0.0;
    private double finalFare = 0.0;
    
    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            setupComboBoxes();
            setupEventHandlers();
            
            statusLabel.setText("✅ Ready to make reservation");
            LOGGER.info("ReservationController initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize ReservationController", e);
            statusLabel.setText("❌ Initialization failed");
        }
    }
    
    private void setupTableColumns() {
        flightNameCol.setCellValueFactory(new PropertyValueFactory<>("flightName"));
        routeCol.setCellValueFactory(new PropertyValueFactory<>("route"));
        departureCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        availableSeatsCol.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));
        fareCol.setCellValueFactory(new PropertyValueFactory<>("baseFare"));
        
        flightsTable.setItems(availableFlights);
        
        // Selection listener for fare calculation
        flightsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                calculateFare(newSelection.getBaseFare());
            }
        });
    }
    
    private void setupComboBoxes() {
        // Gender options
        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        genderCombo.setValue("Male");
        
        // Class options
        classCombo.setItems(FXCollections.observableArrayList("Economy", "Business"));
        classCombo.setValue("Economy");
        
        // Seat preference options
        seatPreferenceCombo.setItems(FXCollections.observableArrayList("Any", "Window", "Aisle"));
        seatPreferenceCombo.setValue("Any");
        
        // Concession categories with discounts
        concessionCombo.setItems(FXCollections.observableArrayList(
            "None", 
            "Student (25% discount)", 
            "Senior Citizen (13% discount)", 
            "Cancer Patient (56.9% discount)"
        ));
        concessionCombo.setValue("None");
        
        // Add listener for concession changes
        concessionCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (flightsTable.getSelectionModel().getSelectedItem() != null) {
                calculateFare(flightsTable.getSelectionModel().getSelectedItem().getBaseFare());
            }
        });
    }
    
    private void setupEventHandlers() {
        // Set minimum dates
        dobPicker.setValue(LocalDate.now().minusYears(18));
        travelDatePicker.setValue(LocalDate.now().plusDays(1));
        
        // Disable past dates for travel
        travelDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
            }
        });
    }
    
    @FXML
    protected void onSearchFlights(ActionEvent event) {
        try {
            if (!validateTravelDetails()) {
                return;
            }
            
            searchProgress.setVisible(true);
            searchFlightsBtn.setDisable(true);
            statusLabel.setText("🔍 Searching available flights...");
            
            Task<ObservableList<Flight>> searchTask = new Task<ObservableList<Flight>>() {
                @Override
                protected ObservableList<Flight> call() throws Exception {
                    return searchAvailableFlights();
                }
            };
            
            searchTask.setOnSucceeded(e -> {
                availableFlights.clear();
                availableFlights.addAll(searchTask.getValue());
                
                Platform.runLater(() -> {
                    searchProgress.setVisible(false);
                    searchFlightsBtn.setDisable(false);
                    
                    if (availableFlights.isEmpty()) {
                        statusLabel.setText("❌ No flights available for selected date and class");
                    } else {
                        statusLabel.setText("✅ Found " + availableFlights.size() + " available flights");
                    }
                });
            });
            
            searchTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    searchProgress.setVisible(false);
                    searchFlightsBtn.setDisable(false);
                    statusLabel.setText("❌ Error searching flights");
                    showAlert("Search Error", "Failed to search flights: " + searchTask.getException().getMessage());
                });
            });
            
            new Thread(searchTask).start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to search flights", e);
            statusLabel.setText("❌ Search failed");
        }
    }
    
    private ObservableList<Flight> searchAvailableFlights() throws SQLException {
        ObservableList<Flight> flights = FXCollections.observableArrayList();
        
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
            ORDER BY f.flight_name
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String selectedClass = classCombo.getValue();
            LocalDate travelDate = travelDatePicker.getValue();
            
            ps.setString(1, selectedClass);
            ps.setString(2, selectedClass);
            ps.setDate(3, Date.valueOf(travelDate));
            ps.setString(4, selectedClass);
            ps.setString(5, selectedClass);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Flight flight = new Flight(
                        rs.getString("flight_name"),
                        rs.getInt("flight_code"),
                        rs.getString("class_code"),
                        rs.getInt("total_exe_seats"),
                        rs.getInt("total_eco_seats")
                    );
                    
                    // Set properties from database
                    flight.setAvailableSeats(rs.getInt("available_seats"));
                    
                    // Get fare from database or use default
                    double dbFare = rs.getDouble("base_fare");
                    flight.setBaseFare(dbFare > 0 ? dbFare : (selectedClass.equals("Economy") ? 850.0 : 2040.0));
                    
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
    
    private void calculateFare(double baseFareAmount) {
        try {
            this.baseFare = baseFareAmount;
            String concessionType = concessionCombo.getValue();
            
            // Extract concession type from display text
            String actualConcessionType = extractConcessionType(concessionType);
            
            double discount = ConcessionCalculator.calculateDiscount(baseFareAmount, actualConcessionType);
            this.finalFare = baseFareAmount - discount;
            
            Platform.runLater(() -> {
                baseFareLabel.setText(String.format("M%.2f LSL", baseFareAmount));
                discountLabel.setText(String.format("M%.2f LSL (%.1f%%)", discount, (discount/baseFareAmount)*100));
                finalFareLabel.setText(String.format("M%.2f LSL", finalFare));
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to calculate fare", e);
        }
    }
    
    private String extractConcessionType(String displayText) {
        if (displayText == null || displayText.equals("None")) {
            return "None";
        }
        if (displayText.startsWith("Student")) {
            return "Student";
        }
        if (displayText.startsWith("Senior Citizen")) {
            return "Senior Citizen";
        }
        if (displayText.startsWith("Cancer Patient")) {
            return "Cancer Patient";
        }
        return "None";
    }
    
    @FXML
    protected void onMakeReservation(ActionEvent event) {
        try {
            if (!validateReservationData()) {
                return;
            }
            
            reservationProgress.setVisible(true);
            reserveBtn.setDisable(true);
            statusLabel.setText("🎫 Processing reservation...");
            
            Task<String> reservationTask = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    return processReservation();
                }
            };
            
            reservationTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    reservationProgress.setVisible(false);
                    reserveBtn.setDisable(false);
                    
                    String result = reservationTask.getValue();
                    if (result.startsWith("SUCCESS")) {
                        statusLabel.setText("✅ Reservation confirmed!");
                        showAlert("Reservation Confirmed", result);
                        onResetForm(null);
                    } else {
                        statusLabel.setText("⏳ Added to waiting list");
                        showAlert("Waiting List", result);
                    }
                });
            });
            
            reservationTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    reservationProgress.setVisible(false);
                    reserveBtn.setDisable(false);
                    statusLabel.setText("❌ Reservation failed");
                    showAlert("Reservation Error", "Failed to process reservation: " + reservationTask.getException().getMessage());
                });
            });
            
            new Thread(reservationTask).start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to make reservation", e);
            statusLabel.setText("❌ Reservation failed");
        }
    }
    
    private String processReservation() throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Insert customer
            int customerId = insertCustomer(conn);
            
            // 2. Check seat availability and assign
            Flight selectedFlight = flightsTable.getSelectionModel().getSelectedItem();
            String selectedClass = classCombo.getValue();
            
            int assignedSeat = assignSeat(conn, selectedFlight.getFlightCode(), selectedClass);
            
            if (assignedSeat > 0) {
                // 3. Create confirmed reservation
                String pnr = generatePNR();
                insertReservation(conn, customerId, selectedFlight.getFlightCode(), selectedClass, assignedSeat, pnr, "Confirmed");
                
                conn.commit();
                return "SUCCESS: Reservation confirmed!\nPNR: " + pnr + "\nSeat: " + assignedSeat + "\nFare: M" + String.format("%.2f", finalFare) + " LSL";
                
            } else {
                // 4. Add to waiting list
                int waitingNumber = addToWaitingList(conn, customerId, selectedFlight.getFlightCode(), selectedClass);
                
                conn.commit();
                return "WAITING: No seats available. Added to waiting list.\nWaiting Number: " + waitingNumber;
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
    
    private int insertCustomer(Connection conn) throws SQLException {
        String sql = """
            INSERT INTO customer_details (cust_name, father_name, gender, dob, address, tel_no, profession, concession, travel_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customerNameField.getText().trim());
            ps.setString(2, fatherNameField.getText().trim());
            ps.setString(3, genderCombo.getValue());
            ps.setDate(4, Date.valueOf(dobPicker.getValue()));
            ps.setString(5, addressArea.getText().trim());
            ps.setString(6, phoneField.getText().trim());
            ps.setString(7, professionField.getText().trim());
            ps.setString(8, extractConcessionType(concessionCombo.getValue()));
            ps.setDate(9, Date.valueOf(travelDatePicker.getValue()));
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        throw new SQLException("Failed to insert customer");
    }
    
    private int assignSeat(Connection conn, int flightCode, String seatClass) throws SQLException {
        // Check available seats
        String checkSql = """
            SELECT 
                CASE WHEN ? = 'Economy' THEN f.total_eco_seats ELSE f.total_exe_seats END as total_seats,
                COALESCE(COUNT(r.seat_number), 0) as reserved_seats
            FROM flights f
            LEFT JOIN reservations r ON f.flight_code = r.flight_code AND r.seat_class = ? AND r.status = 'Confirmed'
            WHERE f.flight_code = ?
            GROUP BY f.flight_code, f.total_eco_seats, f.total_exe_seats
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, seatClass);
            ps.setString(2, seatClass);
            ps.setInt(3, flightCode);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int totalSeats = rs.getInt("total_seats");
                    int reservedSeats = rs.getInt("reserved_seats");
                    
                    if (reservedSeats < totalSeats) {
                        // Find next available seat
                        return findNextAvailableSeat(conn, flightCode, seatClass, totalSeats);
                    }
                }
            }
        }
        
        return 0; // No seats available
    }
    
    private int findNextAvailableSeat(Connection conn, int flightCode, String seatClass, int totalSeats) throws SQLException {
        String seatPreference = seatPreferenceCombo.getValue();
        
        // Get reserved seats
        String reservedSql = "SELECT seat_number FROM reservations WHERE flight_code = ? AND seat_class = ? AND status = 'Confirmed'";
        
        try (PreparedStatement ps = conn.prepareStatement(reservedSql)) {
            ps.setInt(1, flightCode);
            ps.setString(2, seatClass);
            
            try (ResultSet rs = ps.executeQuery()) {
                boolean[] reserved = new boolean[totalSeats + 1];
                while (rs.next()) {
                    reserved[rs.getInt("seat_number")] = true;
                }
                
                // Assign seat based on preference
                if ("Window".equals(seatPreference)) {
                    // Window seats: 1, 6, 7, 12, etc. (assuming 6 seats per row)
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
            }
        }
        
        return 0;
    }
    
    private boolean isWindowSeat(int seatNumber) {
        // Assuming 6 seats per row (A-F), window seats are A(1) and F(6)
        int seatInRow = ((seatNumber - 1) % 6) + 1;
        return seatInRow == 1 || seatInRow == 6;
    }
    
    private void insertReservation(Connection conn, int customerId, int flightCode, String seatClass, int seatNumber, String pnr, String status) throws SQLException {
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
            ps.setDouble(6, finalFare);
            ps.setDate(7, Date.valueOf(travelDatePicker.getValue()));
            ps.setString(8, pnr);
            
            ps.executeUpdate();
        }
    }
    
    private int addToWaitingList(Connection conn, int customerId, int flightCode, String seatClass) throws SQLException {
        // Get next waiting number
        String countSql = "SELECT COALESCE(MAX(waiting_no), 0) + 1 as next_waiting FROM waiting_list WHERE flight_code = ? AND seat_class = ?";
        
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
        
        // Insert into waiting list
        String insertSql = """
            INSERT INTO waiting_list (flight_code, cust_id, seat_class, waiting_no, travel_date)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, flightCode);
            ps.setInt(2, customerId);
            ps.setString(3, seatClass);
            ps.setInt(4, waitingNumber);
            ps.setDate(5, Date.valueOf(travelDatePicker.getValue()));
            
            ps.executeUpdate();
        }
        
        return waitingNumber;
    }
    
    private String generatePNR() {
        return "PNR" + System.currentTimeMillis() % 1000000;
    }
    
    private boolean validateTravelDetails() {
        if (travelDatePicker.getValue() == null) {
            showAlert("Validation Error", "Please select travel date");
            return false;
        }
        
        if (travelDatePicker.getValue().isBefore(LocalDate.now())) {
            showAlert("Validation Error", "Travel date cannot be in the past");
            return false;
        }
        
        return true;
    }
    
    private boolean validateReservationData() {
        if (customerNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter customer name");
            return false;
        }
        
        if (phoneField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter phone number");
            return false;
        }
        
        if (flightsTable.getSelectionModel().getSelectedItem() == null) {
            showAlert("Validation Error", "Please select a flight");
            return false;
        }
        
        return validateTravelDetails();
    }
    
    @FXML
    protected void onResetForm(ActionEvent event) {
        // Clear customer details
        customerNameField.clear();
        fatherNameField.clear();
        genderCombo.setValue("Male");
        dobPicker.setValue(LocalDate.now().minusYears(18));
        addressArea.clear();
        phoneField.clear();
        professionField.clear();
        
        // Clear travel details
        travelDatePicker.setValue(LocalDate.now().plusDays(1));
        classCombo.setValue("Economy");
        seatPreferenceCombo.setValue("Any");
        concessionCombo.setValue("None");
        
        // Clear flights and fare
        availableFlights.clear();
        baseFareLabel.setText("M0.00 LSL");
        discountLabel.setText("M0.00 LSL");
        finalFareLabel.setText("M0.00 LSL");
        
        statusLabel.setText("✅ Form reset - Ready for new reservation");
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