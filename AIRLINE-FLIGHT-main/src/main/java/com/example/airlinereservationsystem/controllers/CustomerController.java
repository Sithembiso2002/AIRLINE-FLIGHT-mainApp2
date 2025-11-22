package com.example.airlinereservationsystem.controllers;

import com.example.airlinereservationsystem.DatabaseConnection;
import com.example.airlinereservationsystem.models.*;
import com.example.airlinereservationsystem.services.ReservationService;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerController {
    private static final Logger LOGGER = Logger.getLogger(CustomerController.class.getName());
    
    private ReservationService reservationService = new ReservationService();
    private User currentUser;
    private Customer currentCustomer;
    
    @FXML private TextField customerNameField;
    @FXML private TextField fatherNameField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private DatePicker dobPicker;
    @FXML private TextArea addressArea;
    @FXML private TextField phoneField;
    @FXML private TextField professionField;
    @FXML private TextField securityInfoField;
    @FXML private ComboBox<String> concessionCombo;
    @FXML private Button registerBtn;
    @FXML private Label registrationStatusLabel;
    
    @FXML private DatePicker travelDatePicker;
    @FXML private ComboBox<String> routeCombo;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> seatPreferenceCombo;
    @FXML private Button searchFlightsBtn;
    @FXML private ProgressIndicator searchProgress;
    
    @FXML private TableView<Flight> flightsTable;
    @FXML private TableColumn<Flight, String> flightNameCol;
    @FXML private TableColumn<Flight, String> routeCol;
    @FXML private TableColumn<Flight, String> departureCol;
    @FXML private TableColumn<Flight, String> arrivalCol;
    @FXML private TableColumn<Flight, Integer> availableSeatsCol;
    @FXML private TableColumn<Flight, Double> fareCol;
    
    @FXML private Label baseFareLabel;
    @FXML private Label discountLabel;
    @FXML private Label finalFareLabel;
    @FXML private Label concessionInfoLabel;
    
    @FXML private Button makeReservationBtn;
    @FXML private ProgressBar reservationProgress;
    
    @FXML private TableView<Reservation> myBookingsTable;
    @FXML private TableColumn<Reservation, String> myPnrCol;
    @FXML private TableColumn<Reservation, String> myFlightCol;
    @FXML private TableColumn<Reservation, String> mySeatCol;
    @FXML private TableColumn<Reservation, String> myStatusCol;
    @FXML private TableColumn<Reservation, Double> myFareCol;
    @FXML private TableColumn<Reservation, String> myTravelDateCol;
    
    @FXML private TextField cancelPnrField;
    @FXML private Button cancelBookingBtn;
    @FXML private VBox bookingDetailsBox;
    @FXML private Label bookingInfoLabel;
    @FXML private Label refundInfoLabel;
    @FXML private Button confirmCancelBtn;
    
    @FXML private Button fadeEffectBtn;
    @FXML private Label statusLabel;
    
    private ObservableList<Flight> availableFlights = FXCollections.observableArrayList();
    private ObservableList<Reservation> myReservations = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        try {
            setupTables();
            setupComboBoxes();
            setupVisualEffects();
            setupEventHandlers();
            prePopulateForm();
            
            statusLabel.setText("Customer Portal Ready - Register and book your flights");
            LOGGER.info("CustomerController initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize CustomerController", e);
            statusLabel.setText("Initialization failed");
        }
    }
    
    private void prePopulateForm() {
        // Pre-populate name from logged-in user if available
        if (currentUser != null) {
            customerNameField.setText(currentUser.getFullName());
        }
        
        // Set default values
        genderCombo.setValue("Male");
        concessionCombo.setValue("None - No discount");
        dobPicker.setValue(LocalDate.of(1990, 5, 15));
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        statusLabel.setText("Welcome " + user.getFullName() + " - Book your perfect flight");
        loadCustomerProfile();
        prePopulateForm();
    }
    
    private void setupTables() {
        flightNameCol.setCellValueFactory(new PropertyValueFactory<>("flightName"));
        routeCol.setCellValueFactory(new PropertyValueFactory<>("route"));
        departureCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        availableSeatsCol.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));
        fareCol.setCellValueFactory(new PropertyValueFactory<>("baseFare"));
        flightsTable.setItems(availableFlights);
        
        myPnrCol.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        myFlightCol.setCellValueFactory(new PropertyValueFactory<>("flightName"));
        mySeatCol.setCellValueFactory(cellData -> {
            Reservation res = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                res.getSeatClass() + " - Seat " + res.getSeatNumber());
        });
        myStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        myFareCol.setCellValueFactory(new PropertyValueFactory<>("fare"));
        myTravelDateCol.setCellValueFactory(cellData -> {
            Reservation res = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(res.getTravelDate().toString());
        });
        myBookingsTable.setItems(myReservations);
        
        flightsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                calculatePersonalizedFare(newSelection.getBaseFare());
            }
        });
        
        myBookingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayBookingForCancellation(newSelection);
            }
        });
    }
    
    private void setupComboBoxes() {
        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        genderCombo.setValue("Male");
        
        routeCombo.setItems(FXCollections.observableArrayList(
            "Any Route", "Maseru → Johannesburg", "Maseru → Cape Town", "Maseru → Durban",
            "Maseru → Bloemfontein", "Johannesburg → Maseru", "Cape Town → Maseru", "Durban → Maseru"
        ));
        routeCombo.setValue("Any Route");
        
        classCombo.setItems(FXCollections.observableArrayList("Economy", "Business"));
        classCombo.setValue("Economy");
        
        seatPreferenceCombo.setItems(FXCollections.observableArrayList("Any", "Window", "Aisle"));
        seatPreferenceCombo.setValue("Any");
        
        concessionCombo.setItems(FXCollections.observableArrayList(
            "None - No discount", 
            "Student - 25% discount (ID required)", 
            "Senior Citizen - 13% discount (Age 60+)", 
            "Cancer Patient - 56.9% discount (Medical certificate required)"
        ));
        concessionCombo.setValue("None - No discount");
        
        concessionCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateConcessionInfo(newVal);
            if (flightsTable.getSelectionModel().getSelectedItem() != null) {
                calculatePersonalizedFare(flightsTable.getSelectionModel().getSelectedItem().getBaseFare());
            }
        });
    }
    
    private void setupVisualEffects() {
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(3.0), fadeEffectBtn);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.3);
        fadeTransition.setCycleCount(Timeline.INDEFINITE);
        fadeTransition.setAutoReverse(true);
        fadeTransition.play();
        
        // Add click handler for special offers button
        fadeEffectBtn.setOnAction(e -> {
            showAlert("Special Offers", 
                "🎉 Current Special Offers:\n\n" +
                "✈️ Student Discount: 25% off all flights\n" +
                "👴 Senior Citizen: 13% off (Age 60+)\n" +
                "🏥 Medical Discount: 56.9% off with certificate\n\n" +
                "Book now and save on your next journey from Maseru!");
        });
    }
    
    private void setupEventHandlers() {
        dobPicker.setValue(LocalDate.now().minusYears(18));
        travelDatePicker.setValue(LocalDate.now().plusDays(1));
        
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
        
        // Enable search button when all required fields are filled
        travelDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateSearchButtonState());
        routeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateSearchButtonState());
        classCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateSearchButtonState());
        
        // Enable search button initially if customer is registered
        updateSearchButtonState();
    }
    
    private void updateSearchButtonState() {
        boolean canSearch = travelDatePicker.getValue() != null && 
                           routeCombo.getValue() != null && 
                           classCombo.getValue() != null;
        
        if (currentCustomer != null) {
            searchFlightsBtn.setDisable(!canSearch);
        } else {
            searchFlightsBtn.setDisable(true);
        }
    }
    
    @FXML
    protected void onRegisterCustomer(ActionEvent event) {
        try {
            if (!validateRegistrationData()) return;
            
            registerBtn.setDisable(true);
            registrationStatusLabel.setText("Registering customer profile...");
            
            currentCustomer = createCustomerFromRegistrationForm();
            
            registrationStatusLabel.setText("✅ Registration successful! You can now search and book flights.");
            statusLabel.setText("Welcome " + currentCustomer.getName() + " - Profile registered successfully");
            
            enableBookingFeatures();
            updateSearchButtonState();
            registerBtn.setDisable(false);
            registerBtn.setText("🔄 Update Profile");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register customer", e);
            registrationStatusLabel.setText("❌ Registration failed: " + e.getMessage());
            registerBtn.setDisable(false);
        }
    }
    
    @FXML
    protected void onSearchFlights(ActionEvent event) {
        try {
            if (currentCustomer == null) {
                showAlert("Registration Required", "Please complete your registration before searching flights.");
                return;
            }
            
            if (travelDatePicker.getValue() == null) {
                showAlert("Validation Error", "Please select travel date");
                return;
            }
            
            searchProgress.setVisible(true);
            searchFlightsBtn.setDisable(true);
            statusLabel.setText("Searching flights for your travel...");
            
            Task<List<Flight>> searchTask = new Task<List<Flight>>() {
                @Override
                protected List<Flight> call() throws Exception {
                    return reservationService.searchFlights(
                        travelDatePicker.getValue(),
                        classCombo.getValue(),
                        routeCombo.getValue()
                    );
                }
            };
            
            searchTask.setOnSucceeded(e -> {
                List<Flight> flights = searchTask.getValue();
                Platform.runLater(() -> {
                    availableFlights.clear();
                    availableFlights.addAll(flights);
                    
                    searchProgress.setVisible(false);
                    searchFlightsBtn.setDisable(false);
                    
                    if (flights.isEmpty()) {
                        statusLabel.setText("No flights available for your selected criteria");
                        showAlert("No Flights", "No flights found for the selected date and class. Please try different criteria.");
                    } else {
                        statusLabel.setText("Found " + flights.size() + " flights matching your criteria");
                    }
                });
            });
            
            searchTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    searchProgress.setVisible(false);
                    searchFlightsBtn.setDisable(false);
                    statusLabel.setText("Error searching flights");
                    showAlert("Search Error", "Failed to search flights: " + searchTask.getException().getMessage());
                });
            });
            
            new Thread(searchTask).start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to search flights", e);
            statusLabel.setText("Search failed");
        }
    }
    
    @FXML
    protected void onMakeReservation(ActionEvent event) {
        try {
            if (currentCustomer == null) {
                showAlert("Registration Required", "Please complete your registration before making reservations.");
                return;
            }
            
            Flight selectedFlight = flightsTable.getSelectionModel().getSelectedItem();
            if (selectedFlight == null) {
                showAlert("Selection Required", "Please select a flight to book.");
                return;
            }
            
            reservationProgress.setVisible(true);
            makeReservationBtn.setDisable(true);
            statusLabel.setText("Processing your reservation...");
            
            Task<ReservationService.ReservationResult> reservationTask = new Task<ReservationService.ReservationResult>() {
                @Override
                protected ReservationService.ReservationResult call() throws Exception {
                    currentCustomer.setTravelDate(travelDatePicker.getValue());
                    
                    return reservationService.makeReservation(
                        currentCustomer,
                        selectedFlight.getFlightCode(),
                        classCombo.getValue(),
                        seatPreferenceCombo.getValue(),
                        travelDatePicker.getValue()
                    );
                }
            };
            
            reservationTask.setOnSucceeded(e -> {
                ReservationService.ReservationResult result = reservationTask.getValue();
                Platform.runLater(() -> {
                    reservationProgress.setVisible(false);
                    makeReservationBtn.setDisable(false);
                    
                    if (result.isConfirmed()) {
                        statusLabel.setText("Reservation confirmed! Enjoy your flight.");
                        showAlert("Booking Confirmed", 
                            "Your flight is booked!\n\n" +
                            "PNR: " + result.getPnr() + "\n" +
                            "Seat: " + classCombo.getValue() + " - " + result.getSeatNumber() + "\n" +
                            "Fare: LSL" + String.format("%.2f", result.getFare()) + "\n\n" +
                            "Please save your PNR for future reference.");
                        
                        loadMyBookings();
                        flightsTable.getSelectionModel().clearSelection();
                        clearFareDisplay();
                        
                    } else {
                        statusLabel.setText("Added to waiting list - We'll notify you if seats become available");
                        showAlert("Waiting List", 
                            "Flight is fully booked. You have been added to the waiting list.\n\n" +
                            "Waiting Number: " + result.getWaitingNumber() + "\n\n" +
                            "We will contact you if seats become available.");
                    }
                });
            });
            
            reservationTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    reservationProgress.setVisible(false);
                    makeReservationBtn.setDisable(false);
                    statusLabel.setText("Reservation failed");
                    showAlert("Booking Error", "Failed to process your reservation: " + reservationTask.getException().getMessage());
                });
            });
            
            new Thread(reservationTask).start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to make reservation", e);
            statusLabel.setText("Reservation failed");
        }
    }
    
    @FXML
    protected void onLoadMyBookings(ActionEvent event) {
        loadMyBookings();
    }
    
    @FXML
    protected void onSearchBookingForCancel(ActionEvent event) {
        String pnr = cancelPnrField.getText().trim();
        if (pnr.isEmpty()) {
            showAlert("Validation Error", "Please enter your PNR number");
            return;
        }
        
        for (Reservation reservation : myReservations) {
            if (reservation.getPnr().equals(pnr)) {
                myBookingsTable.getSelectionModel().select(reservation);
                myBookingsTable.scrollTo(reservation);
                displayBookingForCancellation(reservation);
                statusLabel.setText("Booking found: " + pnr);
                return;
            }
        }
        
        showAlert("Not Found", "No booking found with PNR: " + pnr + "\nPlease check your PNR and try again.");
        statusLabel.setText("Booking not found: " + pnr);
    }
    
    @FXML
    protected void onConfirmCancellation(ActionEvent event) {
        Reservation selectedReservation = myBookingsTable.getSelectionModel().getSelectedItem();
        if (selectedReservation == null) {
            showAlert("Selection Error", "Please select a booking to cancel");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Cancellation");
        confirmAlert.setHeaderText("Cancel Your Booking");
        confirmAlert.setContentText("Are you sure you want to cancel booking " + selectedReservation.getPnr() + "?\n\nThis action cannot be undone.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                processCancellation(selectedReservation.getPnr());
            }
        });
    }
    
    private void loadCustomerProfile() {
        statusLabel.setText(currentUser.getFullName() + " - Please complete your profile to start booking");
    }
    
    private void loadMyBookings() {
        if (currentCustomer == null) {
            showAlert("Registration Required", "Please complete your registration to view bookings.");
            return;
        }
        
        Task<List<Reservation>> loadTask = new Task<List<Reservation>>() {
            @Override
            protected List<Reservation> call() throws Exception {
                return getCustomerReservations(currentCustomer.getPhoneNumber());
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            List<Reservation> reservations = loadTask.getValue();
            Platform.runLater(() -> {
                myReservations.clear();
                myReservations.addAll(reservations);
                statusLabel.setText("Loaded " + reservations.size() + " of your bookings");
            });
        });
        
        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("Failed to load bookings");
                showAlert("Error", "Failed to load your bookings: " + loadTask.getException().getMessage());
            });
        });
        
        new Thread(loadTask).start();
    }
    
    private List<Reservation> getCustomerReservations(String phoneNumber) throws Exception {
        List<Reservation> reservations = new ArrayList<>();
        
        String sql = """
            SELECT r.*, f.flight_name, c.cust_name
            FROM reservations r
            JOIN flights f ON r.flight_code = f.flight_code
            JOIN customer_details c ON r.cust_id = c.cust_id
            WHERE c.tel_no = ?
            ORDER BY r.travel_date DESC, r.reservation_id DESC
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, phoneNumber);
            
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
    
    private void enableBookingFeatures() {
        searchFlightsBtn.setDisable(false);
        statusLabel.setText("Profile registered! You can now search and book flights.");
    }
    
    private Customer createCustomerFromRegistrationForm() {
        Customer customer = new Customer();
        customer.setName(customerNameField.getText().trim());
        customer.setFatherName(fatherNameField.getText().trim());
        customer.setGender(genderCombo.getValue());
        customer.setDateOfBirth(dobPicker.getValue());
        customer.setAddress(addressArea.getText().trim());
        customer.setPhoneNumber(phoneField.getText().trim());
        customer.setProfession(professionField.getText().trim());
        customer.setSecurityInfo(securityInfoField.getText().trim());
        customer.setConcession(extractConcessionType(concessionCombo.getValue()));
        
        return customer;
    }
    
    private boolean validateRegistrationData() {
        if (customerNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter your full name");
            return false;
        }
        
        if (phoneField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter your phone number");
            return false;
        }
        
        if (dobPicker.getValue() == null) {
            showAlert("Validation Error", "Please select your date of birth");
            return false;
        }
        
        if (dobPicker.getValue().isAfter(LocalDate.now().minusYears(12))) {
            showAlert("Validation Error", "You must be at least 12 years old to book flights");
            return false;
        }
        
        return true;
    }
    
    private void calculatePersonalizedFare(double baseFareAmount) {
        try {
            String concessionType = extractConcessionType(concessionCombo.getValue());
            
            double discount = com.example.airlinereservationsystem.utils.ConcessionCalculator.calculateDiscount(baseFareAmount, concessionType);
            double finalFare = baseFareAmount - discount;
            
            Platform.runLater(() -> {
                baseFareLabel.setText(String.format("LSL%.2f", baseFareAmount));
                discountLabel.setText(String.format("LSL%.2f (%.1f%%)", discount, (discount/baseFareAmount)*100));
                finalFareLabel.setText(String.format("LSL%.2f", finalFare));
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to calculate fare", e);
        }
    }
    
    private void updateConcessionInfo(String concessionText) {
        String info = "";
        if (concessionText.contains("Student")) {
            info = "Student Discount: 25% off base fare. Valid student ID required at check-in.";
        } else if (concessionText.contains("Senior Citizen")) {
            info = "Senior Citizen Discount: 13% off base fare. Age proof required (60+ years).";
        } else if (concessionText.contains("Cancer Patient")) {
            info = "Medical Discount: 56.9% off base fare. Medical certificate required.";
        } else {
            info = "Regular fare applies. No additional discounts.";
        }
        
        concessionInfoLabel.setText(info);
    }
    
    private String extractConcessionType(String displayText) {
        if (displayText == null || displayText.contains("None")) {
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
    
    private void displayBookingForCancellation(Reservation reservation) {
        bookingDetailsBox.setVisible(true);
        
        LocalDate travelDate = reservation.getTravelDate();
        LocalDate today = LocalDate.now();
        double fare = reservation.getFare();
        
        long daysUntilTravel = java.time.temporal.ChronoUnit.DAYS.between(today, travelDate);
        
        double cancellationFee;
        String refundPolicy;
        
        if (daysUntilTravel < 0) {
            cancellationFee = fare;
            refundPolicy = "No refund (past travel date)";
        } else if (daysUntilTravel < 1) {
            cancellationFee = fare * 0.25;
            refundPolicy = "25% cancellation fee (within 24 hours)";
        } else {
            cancellationFee = fare * 0.10;
            refundPolicy = "10% cancellation fee (more than 24 hours)";
        }
        
        double refundAmount = fare - cancellationFee;
        
        bookingInfoLabel.setText(
            "PNR: " + reservation.getPnr() + "\n" +
            "Flight: " + reservation.getFlightName() + "\n" +
            "Seat: " + reservation.getSeatClass() + " - " + reservation.getSeatNumber() + "\n" +
            "Travel Date: " + reservation.getTravelDate() + "\n" +
            "Status: " + reservation.getStatus()
        );
        
        refundInfoLabel.setText(
            "Original Fare: LSL" + String.format("%.2f", fare) + "\n" +
            "Cancellation Policy: " + refundPolicy + "\n" +
            "Cancellation Fee: LSL" + String.format("%.2f", cancellationFee) + "\n" +
            "Refund Amount: LSL" + String.format("%.2f", refundAmount)
        );
    }
    
    private void processCancellation(String pnr) {
        Task<ReservationService.CancellationResult> cancellationTask = new Task<ReservationService.CancellationResult>() {
            @Override
            protected ReservationService.CancellationResult call() throws Exception {
                return reservationService.cancelReservation(pnr);
            }
        };
        
        cancellationTask.setOnSucceeded(e -> {
            ReservationService.CancellationResult result = cancellationTask.getValue();
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    statusLabel.setText("Booking cancelled successfully");
                    showAlert("Cancellation Successful", 
                        "Your booking has been cancelled.\n\n" +
                        "Refund Amount: LSL" + String.format("%.2f", result.getRefundAmount()) + "\n" +
                        "Cancellation Fee: LSL" + String.format("%.2f", result.getCancellationFee()) + "\n\n" +
                        "Refund will be processed within 5-7 business days.");
                    
                    loadMyBookings();
                    bookingDetailsBox.setVisible(false);
                    cancelPnrField.clear();
                } else {
                    statusLabel.setText("Cancellation failed");
                    showAlert("Cancellation Error", "Failed to cancel your booking. Please try again or contact support.");
                }
            });
        });
        
        cancellationTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("Cancellation error");
                showAlert("Cancellation Error", "Error: " + cancellationTask.getException().getMessage());
            });
        });
        
        new Thread(cancellationTask).start();
    }
    
    private void clearFareDisplay() {
        baseFareLabel.setText("LSL0.00");
        discountLabel.setText("LSL0.00");
        finalFareLabel.setText("LSL0.00");
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