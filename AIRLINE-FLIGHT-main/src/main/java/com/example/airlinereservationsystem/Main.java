package com.example.airlinereservationsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {
    static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    @Override
    public void start(Stage stage) {
        try {
            // Setup database (with fallback support)
            try {
                DatabaseConnection.setupDatabase();
                LOGGER.info("Database setup completed successfully");
            } catch (Exception dbError) {
                LOGGER.log(Level.WARNING, "Database setup failed, continuing in demo mode", dbError);
            }

            // Load the login screen first
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/airlinereservationsystem/login.fxml"));
            Scene scene = new Scene(loader.load(), 500, 700);
            
            stage.setTitle("✈️ Airline System - Login");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();
            
            LOGGER.info("Application started successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            showErrorDialog("Application Error", "Failed to start the application: " + e.getMessage());
        }
    }
    
    private void showErrorDialog(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("Application Startup Error");
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show error dialog", e);
            System.err.println("Critical Error: " + message);
        }
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Application launch failed", e);
            System.exit(1);
        }
    }
}
