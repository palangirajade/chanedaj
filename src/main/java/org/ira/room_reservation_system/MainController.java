package org.ira.room_reservation_system;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class MainController {
    @FXML
    private TextField roomNameField;
    @FXML
    private TextField roomCapacityField;
    @FXML
    private FlowPane roomListPane;
    @FXML
    private HBox buttonBar;

    private final List<Room> rooms = new ArrayList<>();
    private Room selectedRoom = null;

    public static class Room {
        private String name;
        private int capacity;
        private String status; // vacant or occupied
        public Room(String name, int capacity, String status) {
            this.name = name;
            this.capacity = capacity;
            this.status = status;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public void initialize() {
        LoginController.loadBookings();
        refreshRoomList();
        updateRoomStatusesByBooking();
        if (LoginController.currentRole != null && LoginController.currentRole.equalsIgnoreCase("staff")) {
            disableAdminButtons();
        }
    }

    private void disableAdminButtons() {
        for (javafx.scene.Node node : buttonBar.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                String text = btn.getText().toLowerCase();
                if (text.contains("add") || text.contains("update") || text.contains("delete")) {
                    btn.setDisable(true);
                }
            }
        }
    }

    private void updateRoomStatusesByBooking() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        for (Room room : rooms) {
            boolean occupied = false;
            for (LoginController.Booking booking : LoginController.bookings) {
                if (booking.roomName.equals(room.getName()) &&
                    booking.dateTime.isEqual(now)) {
                    occupied = true;
                    break;
                }
            }
            room.setStatus(occupied ? "Occupied" : room.getStatus());
        }
    }

    @FXML
    private void onAddRoom() {
        try {
            RoomDialog dialog = new RoomDialog(null);
            dialog.setTitle("Add Room");
            dialog.showAndWait().ifPresent(room -> {
                if (room == null) {
                    showAlert("Invalid input. Please enter valid room details.");
                    return;
                }
                // Check for duplicate room name
                for (Room r : rooms) {
                    if (r.getName().equalsIgnoreCase(room.getName())) {
                        showAlert("A room with this name already exists.");
                        return;
                    }
                }
                rooms.add(room);
                refreshRoomList();
            });
        } catch (Exception e) {
            showAlert("Error adding room: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateRoom() {
        if (selectedRoom == null) {
            showAlert("Please select a room to update.");
            return;
        }
        try {
            RoomDialog dialog = new RoomDialog(selectedRoom);
            dialog.setTitle("Update Room");
            dialog.showAndWait().ifPresent(updatedRoom -> {
                if (updatedRoom == null) {
                    showAlert("Invalid input. Please enter valid room details.");
                    return;
                }
                // Check for duplicate room name (excluding the currently selected room)
                for (Room r : rooms) {
                    if (r != selectedRoom && r.getName().equalsIgnoreCase(updatedRoom.getName())) {
                        showAlert("A room with this name already exists.");
                        return;
                    }
                }
                selectedRoom.setName(updatedRoom.getName());
                selectedRoom.setCapacity(updatedRoom.getCapacity());
                selectedRoom.setStatus(updatedRoom.getStatus());
                refreshRoomList();
            });
        } catch (Exception e) {
            showAlert("Error updating room: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteRoom() {
        if (selectedRoom == null) {
            showAlert("Please select a room to delete.");
            return;
        }
        try {
            rooms.remove(selectedRoom);
            clearFields();
            selectedRoom = null;
            refreshRoomList();
        } catch (Exception e) {
            showAlert("Error deleting room: " + e.getMessage());
        }
    }

    @FXML
    private void onExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Rooms to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Room Name,Capacity,Status\n");
                for (Room room : rooms) {
                    writer.write(room.getName() + "," + room.getCapacity() + "," + room.getStatus() + "\n");
                }
            } catch (IOException e) {
                showAlert("Failed to export CSV: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onLoadTestData() {
        try {
            InputStream is = getClass().getResourceAsStream("/org/ira/room_reservation_system/test_rooms.csv");
            if (is == null) {
                showAlert("Test data CSV not found.");
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            rooms.clear();
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; } // skip header
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    try {
                        String name = parts[0].trim();
                        int capacity = Integer.parseInt(parts[1].trim());
                        String status = parts[2].trim();
                        rooms.add(new Room(name, capacity, status));
                    } catch (NumberFormatException nfe) {
                        // skip invalid row
                    }
                }
            }
            refreshRoomList();
        } catch (Exception e) {
            showAlert("Failed to load test data: " + e.getMessage());
        }
    }

    @FXML
    private void onSignOut() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            Stage stage = (Stage) roomListPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
        } catch (Exception e) {
            showAlert("Failed to load login screen: " + e.getMessage());
        }
    }

    private void refreshRoomList() {
        roomListPane.getChildren().clear();
        for (Room room : rooms) {
            VBox card = createRoomCard(room);
            roomListPane.getChildren().add(card);
        }
    }

    private VBox createRoomCard(Room room) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(18));
        card.setSpacing(8);
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.12, 0, 2); -fx-cursor: hand; -fx-border-color: transparent;");

        Label nameLabel = new Label(room.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-family: 'Segoe UI', 'Arial', sans-serif; -fx-font-weight: 600; -fx-text-fill: #0a192f;");
        
        Label capLabel = new Label("Capacity: " + room.getCapacity());
        capLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a5568; -fx-padding: 2 0;");
        
        Label statusLabel = new Label(room.getStatus());
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: normal; -fx-background-radius: 4; -fx-padding: 4 10;" +
            (room.getStatus().equalsIgnoreCase("Vacant")
                ? "-fx-background-color: #e6f6ff; -fx-text-fill: #2b6cb0;"
                : "-fx-background-color: #fff3e0; -fx-text-fill: #e65100;"));

        // Booking indicator
        boolean hasBooking = false;
        for (LoginController.Booking booking : LoginController.bookings) {
            if (booking.roomName.equals(room.getName())) {
                hasBooking = true;
                break;
            }
        }
        Label bookingIndicator = null;
        if (hasBooking) {
            bookingIndicator = new Label("Booked");
            bookingIndicator.setStyle("-fx-background-color: #1a365d; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: normal; -fx-background-radius: 4; -fx-padding: 4 10; -fx-alignment: center;");
        }

        Region spacer = new Region();
        spacer.setMinHeight(15);

        Button bookBtn = new Button("Book Room");
        bookBtn.setPrefWidth(150);
        bookBtn.setStyle("-fx-background-color: #1a365d; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 0; -fx-min-width: 100;");
        bookBtn.setOnAction(e -> showBookingDialog(room));
        if (room.getStatus().equalsIgnoreCase("Occupied")) {
            bookBtn.setDisable(true);
            bookBtn.setStyle("-fx-background-color: #90a4ae; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: default; -fx-padding: 8 0; -fx-min-width: 100;");
        }

        // Create status container
        HBox statusContainer = new HBox(8);
        statusContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        statusContainer.getChildren().add(statusLabel);
        if (bookingIndicator != null) {
            statusContainer.getChildren().add(bookingIndicator);
        }
        
        card.getChildren().addAll(nameLabel, capLabel, statusContainer, spacer, bookBtn);
        
        card.setOnMouseClicked((MouseEvent e) -> {
            selectedRoom = room;
            highlightSelectedCard(card);
        });
        return card;
    }

    private void showBookingDialog(Room room) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Book Room: " + room.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Style dialog buttons with navy blue
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle("-fx-background-color: #1a365d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle("-fx-text-fill: #4a5568; -fx-padding: 8 20;");
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 15;");
        
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-padding: 4;");
        datePicker.setPrefWidth(200);
        
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, LocalDateTime.now().getHour());
        hourSpinner.setEditable(true);
        hourSpinner.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #cbd5e0; -fx-border-radius: 4;");
        hourSpinner.setPrefWidth(200);
        
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, LocalDateTime.now().getMinute());
        minuteSpinner.setEditable(true);
        minuteSpinner.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #cbd5e0; -fx-border-radius: 4;");
        minuteSpinner.setPrefWidth(200);
        
        Label titleLabel = new Label("UNIVERSITY OF CEBU");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0a192f; -fx-padding: 0 0 5 0;");
        
        Label subtitleLabel = new Label("Room Booking");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1a365d; -fx-padding: 0 0 15 0;");
        
        Label roomInfoLabel = new Label("Room: " + room.getName() + " (Capacity: " + room.getCapacity() + ")");
        roomInfoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2d3748; -fx-font-weight: bold;");
        
        // Labels for fields with consistent navy blue styling
        Label dateLabel = new Label("Select Date:");
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2d3748;");
        
        Label hourLabel = new Label("Hour (0-23):");
        hourLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2d3748;");
        
        Label minuteLabel = new Label("Minute (0-59):");
        minuteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2d3748;");
        
        VBox vbox = new VBox(8, titleLabel, subtitleLabel, roomInfoLabel, 
                            new Separator(), 
                            dateLabel, datePicker, 
                            hourLabel, hourSpinner, 
                            minuteLabel, minuteSpinner);
        vbox.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(vbox);
        // Set minimum dialog width for better appearance
        dialog.getDialogPane().setPrefWidth(320);
        dialog.getDialogPane().setMinHeight(400);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                LocalDate date = datePicker.getValue();
                int hour = hourSpinner.getValue();
                int minute = minuteSpinner.getValue();
                if (date != null) {
                    LocalTime time = LocalTime.of(hour, minute);
                    LocalDateTime bookingTime = LocalDateTime.of(date, time);
                    LoginController.addBooking(room.getName(), bookingTime);
                    updateRoomStatusesByBooking();
                    refreshRoomList();
                    
                    // Show success confirmation
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Booking Confirmed");
                    success.setHeaderText(null);
                    success.setContentText("Room " + room.getName() + " has been successfully booked.");
                    ((Stage) success.getDialogPane().getScene().getWindow()).getIcons().add(null);
                    success.getDialogPane().setStyle("-fx-background-color: white;");
                    success.showAndWait();
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void highlightSelectedCard(VBox selectedCard) {
        // Reset all cards to default style
        for (javafx.scene.Node node : roomListPane.getChildren()) {
            node.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0.12, 0, 2); -fx-cursor: hand; -fx-border-color: transparent;");
        }
        
        // Apply selected style with subtle gradient and border - navy blue accent
        selectedCard.setStyle("-fx-background-color: linear-gradient(to bottom, white, #f0f5fa); -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(10,25,47,0.3), 12, 0.2, 0, 3); -fx-cursor: hand; -fx-border-color: #1a365d; -fx-border-width: 2; -fx-border-radius: 8;");
    }

    private void clearFields() {
        // No-op: text fields removed from UI
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Error");
        alert.getDialogPane().setStyle("-fx-background-color: white;");
        ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setStyle(
            "-fx-background-color: #1a365d; -fx-text-fill: white; -fx-font-weight: bold;");
        alert.showAndWait();
    }

    // RoomDialog inner class for add/update dialogs
    private static class RoomDialog extends Dialog<Room> {
        private final TextField nameField = new TextField();
        private final TextField capacityField = new TextField();
        private final ComboBox<String> statusBox = new ComboBox<>();

        public RoomDialog(Room room) {
            setTitle(room == null ? "Add Room" : "Update Room");
            setHeaderText(null);
            ButtonType okButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
            
            // Style dialog buttons with navy blue
            getDialogPane().lookupButton(okButtonType).setStyle("-fx-background-color: #1a365d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
            getDialogPane().lookupButton(ButtonType.CANCEL).setStyle("-fx-text-fill: #4a5568; -fx-padding: 8 20;");
            getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 15;");
            
            // Style the controls with navy blue accents
            nameField.setPromptText("Enter room name");
            nameField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-padding: 8;");
            nameField.setPrefWidth(200);
            
            capacityField.setPromptText("Enter capacity");
            capacityField.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-padding: 8;");
            capacityField.setPrefWidth(200);
            
            statusBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #cbd5e0; -fx-border-radius: 4; -fx-padding: 4;");
            statusBox.setPrefWidth(200);

            statusBox.getItems().addAll("Vacant", "Occupied");
            if (room != null) {
                nameField.setText(room.getName());
                capacityField.setText(String.valueOf(room.getCapacity()));
                statusBox.setValue(room.getStatus());
            } else {
                statusBox.setValue("Vacant");
            }

            VBox content = new VBox(12);
            content.setPadding(new Insets(15));
            
            // Title and header with navy blue colors
            Label titleLabel = new Label("UNIVERSITY OF CEBU");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0a192f; -fx-padding: 0 0 5 0;");
            
            Label subtitleLabel = new Label(room == null ? "Add New Room" : "Update Room Details");
            subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1a365d; -fx-padding: 0 0 15 0;");
            
            content.getChildren().addAll(titleLabel, subtitleLabel, new Separator());
            
            // Form fields with navy blue text
            Label nameLabel = new Label("Room Name:");
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2d3748;");
            content.getChildren().addAll(nameLabel, nameField);
            
            Label capacityLabel = new Label("Capacity:");
            capacityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2d3748;");
            content.getChildren().addAll(capacityLabel, capacityField);
            
            Label statusLabel = new Label("Status:");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2d3748;");
            content.getChildren().addAll(statusLabel, statusBox);
            
            getDialogPane().setPrefWidth(320);
            getDialogPane().setContent(content);

            setResultConverter(dialogButton -> {
                if (dialogButton == okButtonType) {
                    String name = nameField.getText().trim();
                    String capStr = capacityField.getText().trim();
                    String status = statusBox.getValue();
                    
                    // Validate inputs
                    if (name.isEmpty()) {
                        showValidationError("Room name is required.");
                        return null;
                    }
                    
                    if (capStr.isEmpty()) {
                        showValidationError("Capacity is required.");
                        return null;
                    }
                    
                    if (status == null) {
                        showValidationError("Status selection is required.");
                        return null;
                    }
                    
                    try {
                        int capacity = Integer.parseInt(capStr);
                        if (capacity <= 0) {
                            showValidationError("Capacity must be a positive number.");
                            return null;
                        }
                        return new Room(name, capacity, status);
                    } catch (NumberFormatException e) {
                        showValidationError("Capacity must be a valid number.");
                        return null;
                    }
                }
                return null;
            });
        }
        
        // Validation error helper
        private void showValidationError(String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.getDialogPane().setStyle("-fx-background-color: white;");
            alert.showAndWait();
        }
    }
}