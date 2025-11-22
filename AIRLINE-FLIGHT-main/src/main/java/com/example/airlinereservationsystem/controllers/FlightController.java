package com.example.airlinereservationsystem.controllers;

import com.example.airlinereservationsystem.DatabaseConnection;
import com.example.airlinereservationsystem.models.Flight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlightController {
    private static final Logger LOGGER = Logger.getLogger(FlightController.class.getName());

    @FXML
    private TableView<Flight> flightTable;
    @FXML
    private TableColumn<Flight, String> flightNameColumn;
    @FXML
    private TableColumn<Flight, Integer> ecoSeatsColumn;
    @FXML
    private TableColumn<Flight, Integer> exeSeatsColumn;
    @FXML
    private TextField flightNameField;
    @FXML
    private TextField classCodeField;
    @FXML
    private TextField ecoSeatsField;
    @FXML
    private TextField exeSeatsField;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label statusLabel;

    private ObservableList<Flight> flightList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            flightNameColumn.setCellValueFactory(new PropertyValueFactory<>("flightName"));
            ecoSeatsColumn.setCellValueFactory(new PropertyValueFactory<>("totalEcoSeats"));
            exeSeatsColumn.setCellValueFactory(new PropertyValueFactory<>("totalExeSeats"));

            loadFlights();

            // Add a listener to populate the form when a row is selected
            flightTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> populateFormFromSelection());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize FlightController", e);
        }
    }

    private void loadFlights() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM flights")) {

            flightList.clear();
            while (rs.next()) {
                flightList.add(new Flight(
                        rs.getString("flight_name"),
                        rs.getInt("flight_code"),
                        rs.getString("class_code"),
                        rs.getInt("total_exe_seats"),
                        rs.getInt("total_eco_seats")
                ));
            }
            flightTable.setItems(flightList);
            LOGGER.info("Loaded " + flightList.size() + " flights");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load flights", e);
        }
    }

    @FXML
    private void handleAddFlight() {
        String sql = "INSERT INTO flights(flight_name, class_code, total_eco_seats, total_exe_seats) VALUES(?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, flightNameField.getText());
            pstmt.setString(2, classCodeField.getText());
            pstmt.setInt(3, Integer.parseInt(ecoSeatsField.getText()));
            pstmt.setInt(4, Integer.parseInt(exeSeatsField.getText()));
            pstmt.executeUpdate();
            statusLabel.setText("Flight added successfully!");
            loadFlights(); // Refresh the table
            clearFields();
        } catch (Exception e) {
            statusLabel.setText("Error adding flight: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Failed to add flight", e);
        }
    }

    @FXML
    private void handleUpdateFlight() {
        Flight selectedFlight = flightTable.getSelectionModel().getSelectedItem();
        if (selectedFlight == null) {
            statusLabel.setText("Please select a flight to update.");
            return;
        }

        String sql = "UPDATE flights SET flight_name = ?, class_code = ?, total_eco_seats = ?, total_exe_seats = ? WHERE flight_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, flightNameField.getText());
            pstmt.setString(2, classCodeField.getText());
            pstmt.setInt(3, Integer.parseInt(ecoSeatsField.getText()));
            pstmt.setInt(4, Integer.parseInt(exeSeatsField.getText()));
            pstmt.setInt(5, selectedFlight.getFlightCode());
            pstmt.executeUpdate();
            statusLabel.setText("Flight updated successfully!");
            loadFlights(); // Refresh the table
            clearFields();
        } catch (Exception e) {
            statusLabel.setText("Error updating flight: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Failed to update flight", e);
        }
    }

    @FXML
    private void handleDeleteFlight() {
        Flight selectedFlight = flightTable.getSelectionModel().getSelectedItem();
        if (selectedFlight == null) {
            statusLabel.setText("Please select a flight to delete.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // Delete related records first to avoid foreign key constraint violations
            try (PreparedStatement pstmt1 = conn.prepareStatement("DELETE FROM fare WHERE flight_code = ?");
                 PreparedStatement pstmt2 = conn.prepareStatement("DELETE FROM reservations WHERE flight_code = ?");
                 PreparedStatement pstmt3 = conn.prepareStatement("DELETE FROM waiting_list WHERE flight_code = ?");
                 PreparedStatement pstmt4 = conn.prepareStatement("DELETE FROM flights WHERE flight_code = ?")) {
                
                int flightCode = selectedFlight.getFlightCode();
                
                pstmt1.setInt(1, flightCode);
                pstmt1.executeUpdate();
                
                pstmt2.setInt(1, flightCode);
                pstmt2.executeUpdate();
                
                pstmt3.setInt(1, flightCode);
                pstmt3.executeUpdate();
                
                pstmt4.setInt(1, flightCode);
                pstmt4.executeUpdate();
                
                conn.commit();
                statusLabel.setText("Flight deleted successfully!");
                loadFlights();
            }
        } catch (Exception e) {
            statusLabel.setText("Error deleting flight: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Failed to delete flight", e);
        }
    }

    @FXML
    private void populateFormFromSelection() {
        Flight selectedFlight = flightTable.getSelectionModel().getSelectedItem();
        if (selectedFlight != null) {
            flightNameField.setText(selectedFlight.getFlightName());
            classCodeField.setText(selectedFlight.getClassCode());
            ecoSeatsField.setText(String.valueOf(selectedFlight.getTotalEcoSeats()));
            exeSeatsField.setText(String.valueOf(selectedFlight.getTotalExeSeats()));
        }
    }

    @FXML
    private void handleNew() {
        clearFields();
        flightTable.getSelectionModel().clearSelection();
        statusLabel.setText("Ready to add a new flight.");
    }

    private void clearFields() {
        flightNameField.clear();
        classCodeField.clear();
        ecoSeatsField.clear();
        exeSeatsField.clear();
    }

    @FXML
    private void navigateFirst() { flightTable.getSelectionModel().selectFirst(); populateFormFromSelection(); }
    @FXML
    private void navigatePrevious() { flightTable.getSelectionModel().selectPrevious(); populateFormFromSelection(); }
    @FXML
    private void navigateNext() { flightTable.getSelectionModel().selectNext(); populateFormFromSelection(); }
    @FXML
    private void navigateLast() { flightTable.getSelectionModel().selectLast(); populateFormFromSelection(); }
}
