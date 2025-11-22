package com.example.airlinereservationsystem.controllers;

import com.example.airlinereservationsystem.DatabaseConnection;
import com.example.airlinereservationsystem.models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin Controller for comprehensive system management
 * Handles all admin verification requirements for BIOP2210
 */
public class AdminController {
    private static final Logger LOGGER = Logger.getLogger(AdminController.class.getName());
    
    // User Management
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> fullNameColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> createdColumn;
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<String> roleCombo;
    
    // System Statistics
    @FXML private Label totalUsersLabel;
    @FXML private Label totalFlightsLabel;
    @FXML private Label totalReservationsLabel;
    @FXML private Label totalCancellationsLabel;
    @FXML private Label systemStatusLabel;
    
    // Database Operations
    @FXML private TextArea sqlQueryArea;
    @FXML private TextArea queryResultArea;
    @FXML private Button executeQueryBtn;
    
    // Reservation Management
    @FXML private TableView<ReservationInfo> reservationsTable;
    @FXML private TableColumn<ReservationInfo, String> pnrColumn;
    @FXML private TableColumn<ReservationInfo, String> passengerColumn;
    @FXML private TableColumn<ReservationInfo, String> flightColumn;
    @FXML private TableColumn<ReservationInfo, String> seatColumn;
    @FXML private TableColumn<ReservationInfo, String> statusColumn;
    
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private ObservableList<ReservationInfo> reservationsList = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        try {
            setupTables();
            setupComboBoxes();
            loadSystemData();
            updateSystemStatistics();
            
            LOGGER.info("AdminController initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize AdminController", e);
            showError("Initialization Error", "Failed to initialize admin panel: " + e.getMessage());
        }
    }
    
    private void setupTables() {
        // Users table
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        createdColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        usersTable.setItems(usersList);
        
        // Reservations table
        pnrColumn.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        passengerColumn.setCellValueFactory(new PropertyValueFactory<>("passengerName"));
        flightColumn.setCellValueFactory(new PropertyValueFactory<>("flightInfo"));
        seatColumn.setCellValueFactory(new PropertyValueFactory<>("seatInfo"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        reservationsTable.setItems(reservationsList);
        
        // Selection listeners
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateUserForm(newSelection);
            }
        });
    }
    
    private void setupComboBoxes() {
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Staff", "Customer"));
        roleCombo.setValue("Customer");
    }
    
    private void loadSystemData() {
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                loadUsers();
                loadReservations();
                return null;
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                systemStatusLabel.setText("✅ System data loaded successfully");
            });
        });
        
        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                systemStatusLabel.setText("❌ Failed to load system data");
                showError("Data Loading Error", "Failed to load system data: " + loadTask.getException().getMessage());
            });
        });
        
        new Thread(loadTask).start();
    }
    
    private void loadUsers() throws SQLException {
        String sql = "SELECT user_id, username, full_name, role, created_at FROM users ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            ObservableList<User> users = FXCollections.observableArrayList();
            while (rs.next()) {
                User user = new User(
                    rs.getString("username"),
                    "", // Don't load password for security
                    rs.getString("full_name"),
                    rs.getString("role")
                );
                user.setUserId(rs.getInt("user_id"));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                users.add(user);
            }
            
            Platform.runLater(() -> {
                usersList.clear();
                usersList.addAll(users);
            });
        }
    }
    
    private void loadReservations() throws SQLException {
        String sql = """
            SELECT r.reservation_id, r.pnr, c.cust_name, f.flight_name, 
                   r.seat_class, r.seat_number, r.status, r.travel_date
            FROM reservations r
            JOIN customer_details c ON r.cust_id = c.cust_id
            JOIN flights f ON r.flight_code = f.flight_code
            ORDER BY r.reservation_id DESC
            LIMIT 100
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            ObservableList<ReservationInfo> reservations = FXCollections.observableArrayList();
            while (rs.next()) {
                ReservationInfo info = new ReservationInfo(
                    rs.getString("pnr"),
                    rs.getString("cust_name"),
                    rs.getString("flight_name"),
                    rs.getString("seat_class") + " - Seat " + rs.getInt("seat_number"),
                    rs.getString("status")
                );
                reservations.add(info);
            }
            
            Platform.runLater(() -> {
                reservationsList.clear();
                reservationsList.addAll(reservations);
            });
        }
    }
    
    private void updateSystemStatistics() {
        Task<SystemStats> statsTask = new Task<SystemStats>() {
            @Override
            protected SystemStats call() throws Exception {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    SystemStats stats = new SystemStats();
                    
                    // Count users
                    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) stats.totalUsers = rs.getInt(1);
                    }
                    
                    // Count flights
                    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM flights");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) stats.totalFlights = rs.getInt(1);
                    }
                    
                    // Count reservations
                    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM reservations");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) stats.totalReservations = rs.getInt(1);
                    }
                    
                    // Count cancellations
                    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM cancellations");
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) stats.totalCancellations = rs.getInt(1);
                    }
                    
                    return stats;
                }
            }
        };
        
        statsTask.setOnSucceeded(e -> {
            SystemStats stats = statsTask.getValue();
            Platform.runLater(() -> {
                totalUsersLabel.setText(String.valueOf(stats.totalUsers));
                totalFlightsLabel.setText(String.valueOf(stats.totalFlights));
                totalReservationsLabel.setText(String.valueOf(stats.totalReservations));
                totalCancellationsLabel.setText(String.valueOf(stats.totalCancellations));
            });
        });
        
        new Thread(statsTask).start();
    }
    
    @FXML
    protected void onAddUser(ActionEvent event) {
        try {
            if (!validateUserForm()) return;
            
            String sql = "INSERT INTO users (username, password, full_name, role, created_at) VALUES (?, ?, ?, ?, ?)";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setString(1, usernameField.getText().trim());
                ps.setString(2, passwordField.getText()); // In production, hash this
                ps.setString(3, fullNameField.getText().trim());
                ps.setString(4, roleCombo.getValue());
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                
                ps.executeUpdate();
                
                showInfo("Success", "User added successfully!");
                clearUserForm();
                loadUsers();
                updateSystemStatistics();
                
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add user", e);
            showError("Database Error", "Failed to add user: " + e.getMessage());
        }
    }
    
    @FXML
    protected void onUpdateUser(ActionEvent event) {
        try {
            User selectedUser = usersTable.getSelectionModel().getSelectedItem();
            if (selectedUser == null) {
                showError("Selection Error", "Please select a user to update.");
                return;
            }
            
            if (!validateUserForm()) return;
            
            String sql = "UPDATE users SET username = ?, full_name = ?, role = ? WHERE user_id = ?";
            
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setString(1, usernameField.getText().trim());
                ps.setString(2, fullNameField.getText().trim());
                ps.setString(3, roleCombo.getValue());
                ps.setInt(4, selectedUser.getUserId());
                
                ps.executeUpdate();
                
                showInfo("Success", "User updated successfully!");
                clearUserForm();
                loadUsers();
                
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update user", e);
            showError("Database Error", "Failed to update user: " + e.getMessage());
        }
    }
    
    @FXML
    protected void onDeleteUser(ActionEvent event) {
        try {
            User selectedUser = usersTable.getSelectionModel().getSelectedItem();
            if (selectedUser == null) {
                showError("Selection Error", "Please select a user to delete.");
                return;
            }
            
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Deletion");
            alert.setHeaderText("Delete User");
            alert.setContentText("Are you sure you want to delete user: " + selectedUser.getUsername() + "?");
            
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        String sql = "DELETE FROM users WHERE user_id = ?";
                        
                        try (Connection conn = DatabaseConnection.getConnection();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            
                            ps.setInt(1, selectedUser.getUserId());
                            ps.executeUpdate();
                            
                            showInfo("Success", "User deleted successfully!");
                            clearUserForm();
                            loadUsers();
                            updateSystemStatistics();
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Failed to delete user", e);
                        showError("Database Error", "Failed to delete user: " + e.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete user", e);
            showError("Error", "Failed to delete user: " + e.getMessage());
        }
    }
    
    @FXML
    protected void onExecuteQuery(ActionEvent event) {
        try {
            String query = sqlQueryArea.getText().trim();
            if (query.isEmpty()) {
                showError("Query Error", "Please enter a SQL query to execute.");
                return;
            }
            
            executeQueryBtn.setDisable(true);
            queryResultArea.setText("Executing query...");
            
            Task<String> queryTask = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    try (Connection conn = DatabaseConnection.getConnection();
                         Statement stmt = conn.createStatement()) {
                        
                        if (query.toLowerCase().startsWith("select")) {
                            try (ResultSet rs = stmt.executeQuery(query)) {
                                StringBuilder result = new StringBuilder();
                                ResultSetMetaData metaData = rs.getMetaData();
                                int columnCount = metaData.getColumnCount();
                                
                                // Add column headers
                                for (int i = 1; i <= columnCount; i++) {
                                    result.append(metaData.getColumnName(i)).append("\t");
                                }
                                result.append("\n");
                                
                                // Add data rows
                                while (rs.next()) {
                                    for (int i = 1; i <= columnCount; i++) {
                                        result.append(rs.getString(i)).append("\t");
                                    }
                                    result.append("\n");
                                }
                                
                                return result.toString();
                            }
                        } else {
                            int rowsAffected = stmt.executeUpdate(query);
                            return "Query executed successfully. Rows affected: " + rowsAffected;
                        }
                    }
                }
            };
            
            queryTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    queryResultArea.setText(queryTask.getValue());
                    executeQueryBtn.setDisable(false);
                });
            });
            
            queryTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    queryResultArea.setText("Error: " + queryTask.getException().getMessage());
                    executeQueryBtn.setDisable(false);
                });
            });
            
            new Thread(queryTask).start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to execute query", e);
            showError("Query Error", "Failed to execute query: " + e.getMessage());
            executeQueryBtn.setDisable(false);
        }
    }
    
    @FXML
    protected void onRefreshData(ActionEvent event) {
        loadSystemData();
        updateSystemStatistics();
        systemStatusLabel.setText("🔄 Data refreshed");
    }
    
    @FXML
    protected void onReassignSeat(ActionEvent event) {
        ReservationInfo selectedReservation = reservationsTable.getSelectionModel().getSelectedItem();
        if (selectedReservation == null) {
            showError("Selection Error", "Please select a reservation to reassign seat.");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reassign Seat");
        dialog.setHeaderText("Reassign seat for: " + selectedReservation.getPassengerName());
        dialog.setContentText("Enter new seat number:");
        
        dialog.showAndWait().ifPresent(newSeat -> {
            try {
                String sql = "UPDATE reservations SET seat_number = ? WHERE pnr = ?";
                
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    
                    ps.setInt(1, Integer.parseInt(newSeat));
                    ps.setString(2, selectedReservation.getPnr());
                    
                    ps.executeUpdate();
                    
                    showInfo("Success", "Seat reassigned successfully!");
                    loadReservations();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to reassign seat", e);
                showError("Database Error", "Failed to reassign seat: " + e.getMessage());
            }
        });
    }
    
    @FXML
    protected void onClearQuery(ActionEvent event) {
        sqlQueryArea.clear();
        queryResultArea.clear();
    }
    
    @FXML
    protected void onQueryAllUsers(ActionEvent event) {
        sqlQueryArea.setText("SELECT user_id, username, full_name, role, created_at FROM users ORDER BY created_at DESC;");
    }
    
    @FXML
    protected void onQueryAllFlights(ActionEvent event) {
        sqlQueryArea.setText("SELECT flight_code, flight_name, class_code, total_eco_seats, total_exe_seats FROM flights ORDER BY flight_name;");
    }
    
    @FXML
    protected void onQueryAllReservations(ActionEvent event) {
        sqlQueryArea.setText("SELECT r.reservation_id, r.pnr, c.cust_name, f.flight_name, r.seat_class, r.seat_number, r.status, r.fare, r.travel_date FROM reservations r JOIN customer_details c ON r.cust_id = c.cust_id JOIN flights f ON r.flight_code = f.flight_code ORDER BY r.reservation_id DESC;");
    }
    
    @FXML
    protected void onQueryAllCancellations(ActionEvent event) {
        sqlQueryArea.setText("SELECT c.cancel_id, c.reservation_id, c.cancel_date, c.refund_amount, c.cancellation_fee FROM cancellations c ORDER BY c.cancel_date DESC;");
    }
    
    @FXML
    protected void onTestConnection(ActionEvent event) {
        Task<Boolean> testTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    return conn != null && !conn.isClosed();
                }
            }
        };
        
        testTask.setOnSucceeded(e -> {
            Boolean connected = testTask.getValue();
            Platform.runLater(() -> {
                if (connected) {
                    systemStatusLabel.setText("✅ Database connection successful");
                    showInfo("Connection Test", "Database connection is working properly!");
                } else {
                    systemStatusLabel.setText("❌ Database connection failed");
                    showError("Connection Test", "Failed to connect to database!");
                }
            });
        });
        
        testTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                systemStatusLabel.setText("❌ Database connection error");
                showError("Connection Test", "Database connection error: " + testTask.getException().getMessage());
            });
        });
        
        new Thread(testTask).start();
    }
    
    private void populateUserForm(User user) {
        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName());
        roleCombo.setValue(user.getRole());
        passwordField.clear(); // Don't populate password for security
    }
    
    private void clearUserForm() {
        usernameField.clear();
        passwordField.clear();
        fullNameField.clear();
        roleCombo.setValue("Customer");
        usersTable.getSelectionModel().clearSelection();
    }
    
    private boolean validateUserForm() {
        if (usernameField.getText().trim().isEmpty()) {
            showError("Validation Error", "Username is required.");
            return false;
        }
        
        if (passwordField.getText().trim().isEmpty()) {
            showError("Validation Error", "Password is required.");
            return false;
        }
        
        if (fullNameField.getText().trim().isEmpty()) {
            showError("Validation Error", "Full name is required.");
            return false;
        }
        
        return true;
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // Helper classes
    private static class SystemStats {
        int totalUsers = 0;
        int totalFlights = 0;
        int totalReservations = 0;
        int totalCancellations = 0;
    }
    
    public static class ReservationInfo {
        private String pnr;
        private String passengerName;
        private String flightInfo;
        private String seatInfo;
        private String status;
        
        public ReservationInfo(String pnr, String passengerName, String flightInfo, String seatInfo, String status) {
            this.pnr = pnr;
            this.passengerName = passengerName;
            this.flightInfo = flightInfo;
            this.seatInfo = seatInfo;
            this.status = status;
        }
        
        // Getters
        public String getPnr() { return pnr; }
        public String getPassengerName() { return passengerName; }
        public String getFlightInfo() { return flightInfo; }
        public String getSeatInfo() { return seatInfo; }
        public String getStatus() { return status; }
        
        // Setters for JavaFX property binding
        public void setPnr(String pnr) { this.pnr = pnr; }
        public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
        public void setFlightInfo(String flightInfo) { this.flightInfo = flightInfo; }
        public void setSeatInfo(String seatInfo) { this.seatInfo = seatInfo; }
        public void setStatus(String status) { this.status = status; }
    }
}