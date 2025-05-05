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

public class MainController {
    @FXML
    private TextField roomNameField;
    @FXML
    private TextField roomCapacityField;
    @FXML
    private FlowPane roomListPane;

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

    @FXML
    public void initialize() {
        refreshRoomList();
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
        card.setPadding(new Insets(16));
        card.setSpacing(6);
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, #b0b4c1, 6, 0.10, 0, 2); -fx-cursor: hand; -fx-border-color: transparent;");

        Label nameLabel = new Label(room.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-family: 'Segoe UI', 'Arial', sans-serif; -fx-font-weight: bold; -fx-text-fill: #2d3559;");
        Label capLabel = new Label("Capacity: " + room.getCapacity());
        capLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #4e54c8;");
        Label statusLabel = new Label(room.getStatus());
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 2 8;" +
            (room.getStatus().equalsIgnoreCase("Vacant")
                ? "-fx-background-color: #eafaf1; -fx-text-fill: #27ae60;"
                : "-fx-background-color: #fff4e6; -fx-text-fill: #e67e22;"));

        card.getChildren().addAll(nameLabel, capLabel, statusLabel);
        card.setOnMouseClicked((MouseEvent e) -> {
            selectedRoom = room;
            highlightSelectedCard(card);
        });
        return card;
    }

    private void highlightSelectedCard(VBox selectedCard) {
        for (javafx.scene.Node node : roomListPane.getChildren()) {
            node.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, #b0b4c1, 6, 0.10, 0, 2); -fx-cursor: hand; -fx-border-color: transparent;");
        }
        selectedCard.setStyle("-fx-background-color: linear-gradient(to right, #e0e7ff, #f4f6fb); -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, #4e54c8, 8, 0.18, 0, 2); -fx-cursor: hand; -fx-border-color: #4e54c8; -fx-border-width: 2; -fx-border-radius: 10;");
    }

    private void clearFields() {
        // No-op: text fields removed from UI
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
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
            ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

            statusBox.getItems().addAll("Vacant", "Occupied");
            if (room != null) {
                nameField.setText(room.getName());
                capacityField.setText(String.valueOf(room.getCapacity()));
                statusBox.setValue(room.getStatus());
            } else {
                statusBox.setValue("Vacant");
            }

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            grid.add(new Label("Room Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Capacity:"), 0, 1);
            grid.add(capacityField, 1, 1);
            grid.add(new Label("Status:"), 0, 2);
            grid.add(statusBox, 1, 2);
            getDialogPane().setContent(grid);

            setResultConverter(dialogButton -> {
                if (dialogButton == okButtonType) {
                    String name = nameField.getText().trim();
                    String capStr = capacityField.getText().trim();
                    String status = statusBox.getValue();
                    if (name.isEmpty() || capStr.isEmpty() || status == null) return null;
                    try {
                        int capacity = Integer.parseInt(capStr);
                        return new Room(name, capacity, status);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return null;
            });
        }
    }
}