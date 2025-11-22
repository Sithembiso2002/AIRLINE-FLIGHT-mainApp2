package com.example.airlinereservationsystem.controllers;

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

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StaffController {
    private static final Logger LOGGER = Logger.getLogger(StaffController.class.getName());
    
    private ReservationService reservationService = new ReservationService();
    private User currentUser;
    
    @FXML private TextField customerNameField;
    @FXML private TextField fatherNameField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private DatePicker dobPicker;
    @FXML private TextArea addressArea;
    @FXML private TextField phoneField;
    @FXML private TextField professionField;
    
    @FXML private DatePicker travelDatePicker;
    @FXML private ComboBox<String> classCombo;
    @FXML private ComboBox<String> seatPreferenceCombo;
    @FXML private ComboBox<String> concessionCombo;
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
    
    @FXML private Button makeReservationBtn;
    @FXML private ProgressBar reservationProgress;
    
    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, String> pnrCol;
    @FXML private TableColumn<Reservation, String> passengerCol;
    @FXML private TableColumn<Reservation, String> flightCol;
    @FXML private TableColumn<Reservation, String> seatCol;
    @FXML private TableColumn<Reservation, String> statusCol;
    @FXML private TableColumn<Reservation, Double> fareAmountCol;
    
    @FXML private Button firstPageBtn;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Button lastPageBtn;
    @FXML private Label pageLabel;
    @FXML private Label recordCountLabel;
    
    @FXML private TextField pnrSearchField;
    @FXML private Button searchPnrBtn;
    @FXML private VBox reservationDetailsBox;
    @FXML private Label reservationInfoLabel;
    @FXML private Button cancelReservationBtn;
    
    @FXML private Button fadeEffectBtn;
    @FXML private Label statusLabel;
    @FXML private TextArea reportsArea;
    
    private ObservableList<Flight> availableFlights = FXCollections.observableArrayList();
    private ObservableList<Reservation> allReservations = FXCollections.observableArrayList();
    
    private int currentPage = 1;
    private final int itemsPerPage = 10;
    private int totalPages = 1;
    
    @FXML
    public void initialize() {
        try {
            setupTables();
            setupComboBoxes();
            setupVisualEffects();
            loadReservations();
            
            statusLabel.setText("Staff Dashboard Ready - Assist customers with reservations");
            LOGGER.info("StaffController initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize StaffController", e);
            statusLabel.setText("Initialization failed");
        }
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        statusLabel.setText("Staff: " + user.getFullName() + " - Ready to assist customers");
    }
    
    private void setupTables() {
        flightNameCol.setCellValueFactory(new PropertyValueFactory<>("flightName"));
        routeCol.setCellValueFactory(new PropertyValueFactory<>("route"));
        departureCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        availableSeatsCol.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));
        fareCol.setCellValueFactory(new PropertyValueFactory<>("baseFare"));
        flightsTable.setItems(availableFlights);
        
        pnrCol.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        passengerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        flightCol.setCellValueFactory(new PropertyValueFactory<>("flightName"));
        seatCol.setCellValueFactory(cellData -> {
            Reservation res = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                res.getSeatClass() + " - Seat " + res.getSeatNumber());
        });
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        fareAmountCol.setCellValueFactory(new PropertyValueFactory<>("fare"));
        reservationsTable.setItems(allReservations);
        
        flightsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                calculateFare(newSelection.getBaseFare());
            }
        });
        
        reservationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayReservationDetails(newSelection);
            }
        });
    }
    
    private void setupComboBoxes() {
        genderCombo.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        genderCombo.setValue("Male");
        
        classCombo.setItems(FXCollections.observableArrayList("Economy", "Business"));
        classCombo.setValue("Economy");
        
        seatPreferenceCombo.setItems(FXCollections.observableArrayList("Any", "Window", "Aisle"));
        seatPreferenceCombo.setValue("Any");
        
        concessionCombo.setItems(FXCollections.observableArrayList(
            "None", 
            "Student (25% discount)", 
            "Senior Citizen (13% discount)", 
            "Cancer Patient (56.9% discount)"
        ));
        concessionCombo.setValue("None");
        
        concessionCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (flightsTable.getSelectionModel().getSelectedItem() != null) {
                calculateFare(flightsTable.getSelectionModel().getSelectedItem().getBaseFare());
            }
        });
    }
    
    private void setupVisualEffects() {
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(2.0), fadeEffectBtn);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.5);
        fadeTransition.setCycleCount(Timeline.INDEFINITE);
        fadeTransition.setAutoReverse(true);
        fadeTransition.play();
    }
    
    @FXML
    protected void onSearchFlights(ActionEvent event) {
        try {
            if (travelDatePicker.getValue() == null) {
                showAlert("Validation Error", "Please select travel date");
                return;
            }
            
            searchProgress.setVisible(true);
            searchFlightsBtn.setDisable(true);
            statusLabel.setText("Searching available flights...");
            
            Task<List<Flight>> searchTask = new Task<List<Flight>>() {
                @Override
                protected List<Flight> call() throws Exception {
                    return reservationService.searchFlights(
                        travelDatePicker.getValue(),
                        classCombo.getValue(),
                        "Any"
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
                        statusLabel.setText("No flights available for selected date and class");
                    } else {
                        statusLabel.setText("Found " + flights.size() + " available flights");
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
            if (!validateReservationData()) {
                return;
            }
            
            reservationProgress.setVisible(true);
            makeReservationBtn.setDisable(true);
            statusLabel.setText("Processing customer reservation...");
            
            Task<ReservationService.ReservationResult> reservationTask = new Task<ReservationService.ReservationResult>() {
                @Override
                protected ReservationService.ReservationResult call() throws Exception {
                    Customer customer = createCustomerFromForm();
                    Flight selectedFlight = flightsTable.getSelectionModel().getSelectedItem();
                    
                    return reservationService.makeReservation(
                        customer,
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
                        statusLabel.setText("Reservation confirmed for customer!");
                        showAlert("Reservation Confirmed", 
                            "Customer reservation successful!\n" +
                            "PNR: " + result.getPnr() + "\n" +
                            "Seat: " + result.getSeatNumber() + "\n" +
                            "Fare: M" + String.format("%.2f", result.getFare()) + " LSL");
                        clearCustomerForm();
                        loadReservations();
                    } else {
                        statusLabel.setText("Customer added to waiting list");
                        showAlert("Waiting List", 
                            "No seats available. Customer added to waiting list.\n" +
                            "Waiting Number: " + result.getWaitingNumber());
                    }
                });
            });
            
            reservationTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    reservationProgress.setVisible(false);
                    makeReservationBtn.setDisable(false);
                    statusLabel.setText("Reservation failed");
                    showAlert("Reservation Error", "Failed to process reservation: " + reservationTask.getException().getMessage());
                });
            });
            
            new Thread(reservationTask).start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to make reservation", e);
            statusLabel.setText("Reservation failed");
        }
    }
    
    @FXML
    protected void onSearchPNR(ActionEvent event) {
        String pnr = pnrSearchField.getText().trim();
        if (pnr.isEmpty()) {
            showAlert("Validation Error", "Please enter PNR number");
            return;
        }
        
        for (Reservation reservation : allReservations) {
            if (reservation.getPnr().equals(pnr)) {
                reservationsTable.getSelectionModel().select(reservation);
                reservationsTable.scrollTo(reservation);
                displayReservationDetails(reservation);
                statusLabel.setText("Reservation found: " + pnr);
                return;
            }
        }
        
        showAlert("Not Found", "No reservation found with PNR: " + pnr);
        statusLabel.setText("Reservation not found: " + pnr);
    }
    
    @FXML
    protected void onCancelReservation(ActionEvent event) {
        Reservation selectedReservation = reservationsTable.getSelectionModel().getSelectedItem();
        if (selectedReservation == null) {
            showAlert("Selection Error", "Please select a reservation to cancel");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Cancellation");
        confirmAlert.setHeaderText("Cancel Reservation");
        confirmAlert.setContentText("Are you sure you want to cancel reservation " + selectedReservation.getPnr() + "?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                processCancellation(selectedReservation.getPnr());
            }
        });
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
                    statusLabel.setText("Reservation cancelled successfully");
                    showAlert("Cancellation Successful", 
                        "Reservation cancelled!\n" +
                        "Refund Amount: M" + String.format("%.2f", result.getRefundAmount()) + " LSL\n" +
                        "Cancellation Fee: M" + String.format("%.2f", result.getCancellationFee()) + " LSL");
                    loadReservations();
                    reservationDetailsBox.setVisible(false);
                } else {
                    statusLabel.setText("Cancellation failed");
                    showAlert("Cancellation Error", "Failed to cancel reservation");
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
    
    @FXML
    protected void onGenerateReport(ActionEvent event) {
        Task<String> reportTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                StringBuilder report = new StringBuilder();
                report.append("=== STAFF SUMMARY REPORT ===\n");
                report.append("Generated by: ").append(currentUser.getFullName()).append("\n");
                report.append("Date: ").append(java.time.LocalDateTime.now()).append("\n\n");
                
                long confirmedReservations = allReservations.stream()
                    .filter(r -> "Confirmed".equals(r.getStatus()))
                    .count();
                
                long cancelledReservations = allReservations.stream()
                    .filter(r -> "Cancelled".equals(r.getStatus()))
                    .count();
                
                double totalRevenue = allReservations.stream()
                    .filter(r -> "Confirmed".equals(r.getStatus()))
                    .mapToDouble(Reservation::getFare)
                    .sum();
                
                report.append("RESERVATION STATISTICS:\n");
                report.append("- Total Reservations: ").append(allReservations.size()).append("\n");
                report.append("- Confirmed: ").append(confirmedReservations).append("\n");
                report.append("- Cancelled: ").append(cancelledReservations).append("\n");
                report.append("- Total Revenue: M").append(String.format("%.2f", totalRevenue)).append(" LSL\n\n");
                
                long economySeats = allReservations.stream()
                    .filter(r -> "Economy".equals(r.getSeatClass()) && "Confirmed".equals(r.getStatus()))
                    .count();
                
                long businessSeats = allReservations.stream()
                    .filter(r -> "Business".equals(r.getSeatClass()) && "Confirmed".equals(r.getStatus()))
                    .count();
                
                report.append("SEAT ALLOCATION:\n");
                report.append("- Economy Class: ").append(economySeats).append(" seats\n");
                report.append("- Business Class: ").append(businessSeats).append(" seats\n\n");
                
                report.append("RECENT RESERVATIONS (Last 10):\n");
                allReservations.stream()
                    .limit(10)
                    .forEach(r -> {
                        report.append("- ").append(r.getPnr())
                              .append(" | ").append(r.getCustomerName())
                              .append(" | ").append(r.getFlightName())
                              .append(" | ").append(r.getStatus())
                              .append("\n");
                    });
                
                return report.toString();
            }
        };
        
        reportTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                reportsArea.setText(reportTask.getValue());
                statusLabel.setText("Summary report generated");
            });
        });
        
        new Thread(reportTask).start();
    }
    
    @FXML
    protected void onFirstPage(ActionEvent event) {
        currentPage = 1;
        loadReservations();
    }
    
    @FXML
    protected void onPreviousPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            loadReservations();
        }
    }
    
    @FXML
    protected void onNextPage(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            loadReservations();
        }
    }
    
    @FXML
    protected void onLastPage(ActionEvent event) {
        currentPage = totalPages;
        loadReservations();
    }
    
    private void loadReservations() {
        Task<List<Reservation>> loadTask = new Task<List<Reservation>>() {
            @Override
            protected List<Reservation> call() throws Exception {
                return reservationService.getAllReservations(100, 0);
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            List<Reservation> reservations = loadTask.getValue();
            Platform.runLater(() -> {
                allReservations.clear();
                allReservations.addAll(reservations);
                
                totalPages = Math.max(1, (int) Math.ceil((double) reservations.size() / itemsPerPage));
                updatePaginationControls();
                
                statusLabel.setText("Loaded " + reservations.size() + " reservations");
            });
        });
        
        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("Failed to load reservations");
                LOGGER.log(Level.SEVERE, "Failed to load reservations", loadTask.getException());
            });
        });
        
        new Thread(loadTask).start();
    }
    
    private void updatePaginationControls() {
        firstPageBtn.setDisable(currentPage == 1);
        prevPageBtn.setDisable(currentPage == 1);
        nextPageBtn.setDisable(currentPage == totalPages);
        lastPageBtn.setDisable(currentPage == totalPages);
        
        pageLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
        
        int startRecord = (currentPage - 1) * itemsPerPage + 1;
        int endRecord = Math.min(currentPage * itemsPerPage, allReservations.size());
        recordCountLabel.setText(String.format("Showing %d-%d of %d records", 
            startRecord, endRecord, allReservations.size()));
    }
    
    private void calculateFare(double baseFareAmount) {
        try {
            String concessionType = extractConcessionType(concessionCombo.getValue());
            
            double discount = com.example.airlinereservationsystem.utils.ConcessionCalculator.calculateDiscount(baseFareAmount, concessionType);
            double finalFare = baseFareAmount - discount;
            
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
    
    private Customer createCustomerFromForm() {
        Customer customer = new Customer();
        customer.setName(customerNameField.getText().trim());
        customer.setFatherName(fatherNameField.getText().trim());
        customer.setGender(genderCombo.getValue());
        customer.setDateOfBirth(dobPicker.getValue());
        customer.setAddress(addressArea.getText().trim());
        customer.setPhoneNumber(phoneField.getText().trim());
        customer.setProfession(professionField.getText().trim());
        customer.setConcession(extractConcessionType(concessionCombo.getValue()));
        customer.setTravelDate(travelDatePicker.getValue());
        
        return customer;
    }
    
    private boolean validateReservationData() {
        if (customerNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter customer name");
            return false;
        }
        
        if (phoneField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter customer phone number");
            return false;
        }
        
        if (travelDatePicker.getValue() == null) {
            showAlert("Validation Error", "Please select travel date");
            return false;
        }
        
        if (travelDatePicker.getValue().isBefore(LocalDate.now())) {
            showAlert("Validation Error", "Travel date cannot be in the past");
            return false;
        }
        
        if (flightsTable.getSelectionModel().getSelectedItem() == null) {
            showAlert("Validation Error", "Please select a flight");
            return false;
        }
        
        return true;
    }
    
    private void displayReservationDetails(Reservation reservation) {
        reservationDetailsBox.setVisible(true);
        reservationInfoLabel.setText(
            "PNR: " + reservation.getPnr() + "\n" +
            "Passenger: " + reservation.getCustomerName() + "\n" +
            "Flight: " + reservation.getFlightName() + "\n" +
            "Seat: " + reservation.getSeatClass() + " - " + reservation.getSeatNumber() + "\n" +
            "Fare: M" + String.format("%.2f", reservation.getFare()) + " LSL\n" +
            "Travel Date: " + reservation.getTravelDate() + "\n" +
            "Status: " + reservation.getStatus()
        );
    }
    
    private void clearCustomerForm() {
        customerNameField.clear();
        fatherNameField.clear();
        genderCombo.setValue("Male");
        dobPicker.setValue(LocalDate.now().minusYears(18));
        addressArea.clear();
        phoneField.clear();
        professionField.clear();
        concessionCombo.setValue("None");
        
        baseFareLabel.setText("M0.00 LSL");
        discountLabel.setText("M0.00 LSL");
        finalFareLabel.setText("M0.00 LSL");
        
        flightsTable.getSelectionModel().clearSelection();
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