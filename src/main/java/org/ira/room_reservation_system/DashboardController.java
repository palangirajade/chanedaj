package org.ira.room_reservation_system;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardController {    
    @FXML
    private Label totalRoomsLabel;
    @FXML
    private Label vacantRoomsLabel;
    @FXML
    private Label occupiedRoomsLabel;
    @FXML
    private Label totalBookingsLabel;
    @FXML
    private Label userWelcomeLabel;
    @FXML
    private VBox recentBookingsContainer;
    private Map<String, MainController.Room> roomsMap = new HashMap<>();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private Timer dashboardUpdateTimer;

    public void initialize() {
        loadRoomData();
        updateSummaryLabels();
        populateRecentBookings();
        
        // Set welcome message with current user
        if (LoginController.currentUser != null) {
            userWelcomeLabel.setText("Welcome, " + LoginController.currentUser);
        } else {
            userWelcomeLabel.setText("Welcome, Guest");
        }
        
        // Start a timer to check for bookings and update statuses periodically
        startDashboardUpdateTimer();
    }
    
    private void startDashboardUpdateTimer() {
        dashboardUpdateTimer = new Timer(true); // Set as daemon timer
        dashboardUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Run on JavaFX thread since we're updating UI
                Platform.runLater(() -> {
                    checkForBookingUpdates();
                    updateSummaryLabels();
                });
            }
        }, 60000, 60000); // Update every minute (60,000 ms)
    }
    
    private void stopDashboardUpdateTimer() {
        if (dashboardUpdateTimer != null) {
            dashboardUpdateTimer.cancel();
            dashboardUpdateTimer = null;
        }
    }    private void checkForBookingUpdates() {
        // Re-load room data to get fresh status
        loadRoomData();
        
        // Check if any bookings match the current time using the utility method
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        boolean roomStatusChanged = MainController.checkAndUpdateRoomStatuses(roomsMap, now);
        
        if (roomStatusChanged) {
            // Save the updated room statuses using the static method
            MainController.saveRoomsToCSV(roomsMap);
            // Also refresh the dashboard UI
            updateSummaryLabels();
        }
    }
    
    private void loadRoomData() {
        // Get room data from MainController
        MainController mainController = new MainController();
        mainController.loadRoomsFromCSV();
        roomsMap = mainController.getRoomsMap();
    }
    
    private void updateSummaryLabels() {
        int totalRooms = roomsMap.size();
        int vacantRooms = 0;
        int occupiedRooms = 0;
        
        for (MainController.Room room : roomsMap.values()) {
            if (room.getStatus().equalsIgnoreCase("Vacant")) {
                vacantRooms++;
            } else {
                occupiedRooms++;
            }
        }
        
        totalRoomsLabel.setText(String.valueOf(totalRooms));
        vacantRoomsLabel.setText(String.valueOf(vacantRooms));
        occupiedRoomsLabel.setText(String.valueOf(occupiedRooms));
        totalBookingsLabel.setText(String.valueOf(LoginController.bookings.size()));
    }
    
    private void populateRecentBookings() {
        recentBookingsContainer.getChildren().clear();
        
        // Sort bookings by date/time (most recent first)
        LoginController.bookings.sort((b1, b2) -> b2.dateTime.compareTo(b1.dateTime));
        
        // Add at most 5 most recent bookings
        int count = 0;
        for (LoginController.Booking booking : LoginController.bookings) {
            if (count >= 5) break;
            
            HBox bookingEntry = createBookingEntry(booking);
            recentBookingsContainer.getChildren().add(bookingEntry);
            count++;
        }
        
        // If no bookings, show a message
        if (recentBookingsContainer.getChildren().isEmpty()) {
            Label noBookingsLabel = new Label("No recent bookings found");
            noBookingsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568; -fx-padding: 10;");
            recentBookingsContainer.getChildren().add(noBookingsLabel);
        }
    }
    
    private HBox createBookingEntry(LoginController.Booking booking) {
        HBox entry = new HBox(10);
        entry.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6; -fx-padding: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 6;");
        
        VBox details = new VBox(5);
        
        Label roomLabel = new Label("Room: " + booking.roomName);
        roomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a365d;");
        
        Label dateLabel = new Label("Date: " + booking.dateTime.format(DATE_FORMATTER));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4a5568;");
                
        Label timeLabel = new Label("Time: " + booking.dateTime.format(TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4a5568;");
        
        Label userLabel = new Label("User: " + LoginController.currentUser);
        userLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4a5568;");
        
        details.getChildren().addAll(roomLabel, dateLabel, timeLabel, userLabel);
        entry.getChildren().add(details);
        
        return entry;
    }

    @FXML
    private void onExportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Dashboard Data");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Dashboard Report - " + java.time.LocalDate.now() + "\n\n");
                writer.write("SUMMARY\n");
                writer.write("Total Rooms," + totalRoomsLabel.getText() + "\n");
                writer.write("Vacant Rooms," + vacantRoomsLabel.getText() + "\n");
                writer.write("Occupied Rooms," + occupiedRoomsLabel.getText() + "\n");
                writer.write("Total Bookings," + totalBookingsLabel.getText() + "\n\n");
                
                writer.write("ROOM DETAILS\n");
                writer.write("Room Name,Capacity,Status\n");
                for (MainController.Room room : roomsMap.values()) {
                    writer.write(room.getName() + "," + room.getCapacity() + "," + room.getStatus() + "\n");
                }
                
                writer.write("\nRECENT BOOKINGS\n");
                writer.write("Room,Date and Time,User\n");
                
                // Add only the 5 most recent bookings (already sorted)
                int count = 0;
                for (LoginController.Booking booking : LoginController.bookings) {
                    if (count >= 5) break;
                    writer.write(booking.roomName + "," + booking.dateTime.format(DATE_TIME_FORMATTER) 
                        + "," + LoginController.currentUser + "\n");
                    count++;
                }
                
                showAlert("Dashboard data exported successfully to " + file.getName());
            } catch (IOException e) {
                showAlert("Failed to export data: " + e.getMessage());
            }
        }
    }    @FXML
    private void onBackToRooms() {
        try {
            // Stop the dashboard update timer
            stopDashboardUpdateTimer();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
            Scene scene = new Scene(loader.load(), 1221, 761);
            Stage stage = (Stage) totalRoomsLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setTitle("Room Reservation System");
        } catch (Exception e) {
            showAlert("Failed to navigate back: " + e.getMessage());
        }
    }
    
    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: white;");
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK)).setStyle(
            "-fx-background-color: #1a365d; -fx-text-fill: white; -fx-font-weight: bold;");
        alert.showAndWait();
    }
}
