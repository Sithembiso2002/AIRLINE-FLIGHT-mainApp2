module com.example.airlinereservationsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.logging;
    requires java.desktop;
    requires org.postgresql.jdbc;

    opens com.example.airlinereservationsystem to javafx.fxml;
    opens com.example.airlinereservationsystem.controllers to javafx.fxml;
    opens com.example.airlinereservationsystem.models to javafx.base;

    exports com.example.airlinereservationsystem;
    exports com.example.airlinereservationsystem.controllers;
    exports com.example.airlinereservationsystem.models;
}
