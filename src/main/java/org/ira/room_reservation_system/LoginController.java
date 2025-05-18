package org.ira.room_reservation_system;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    @FXML
    private ComboBox<String> roleComboBox;

    private static final String USERS_FILE = "src/main/resources/org/ira/room_reservation_system/users.csv";
    private static final Map<String, String[]> userInfo = new HashMap<>();
    private static boolean usersLoaded = false;    private static final String BOOKINGS_FILE = "src/main/resources/org/ira/room_reservation_system/bookings.csv";
    static final List<Booking> bookings = new ArrayList<>();

    public static String currentRole = null;
    public static String currentUser = null;
    public static class Booking {
        public String roomName;
        public LocalDateTime dateTime;
        public Booking(String roomName, LocalDateTime dateTime) {
            this.roomName = roomName;
            this.dateTime = dateTime;
        }
    }

    public static void addBooking(String roomName, LocalDateTime dateTime) {
        bookings.add(new Booking(roomName, dateTime));
        saveBookings();
    }

    public static void saveBookings() {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(BOOKINGS_FILE, false))) {
            pw.println("room,datetime");
            for (Booking b : bookings) {
                pw.println(b.roomName + "," + b.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")));
            }
        } catch (Exception ignored) {}
    }

    public static void loadBookings() {
        bookings.clear();
        if (!Files.exists(Paths.get(BOOKINGS_FILE))) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    LocalDateTime dt = LocalDateTime.parse(parts[1], DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm"));
                    bookings.add(new Booking(parts[0], dt));
                }
            }
        } catch (Exception ignored) {}
    }    private void loadUsers() {
        if (usersLoaded) return;
        usersLoaded = true;
        userInfo.clear();
        
        // Create users file with default admin if it doesn't exist
        if (!Files.exists(Paths.get(USERS_FILE))) {
            try {
                Files.createDirectories(Paths.get(USERS_FILE).getParent());
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
                    writer.write("username,password,role\n");
                    writer.write("admin,admin,admin\n"); // Default admin user
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    String role = parts[2].trim();
                    userInfo.put(username, new String[]{password, role});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveAllUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, false))) {
            writer.write("username,password,role\n");
            for (Map.Entry<String, String[]> entry : userInfo.entrySet()) {
                writer.write(String.format("%s,%s,%s\n", 
                    entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveUser(String username, String password, String role) {
        userInfo.put(username, new String[]{password, role});
        saveAllUsers();
    }

    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().setAll("admin", "staff");
        }
    }

    @FXML
    private void onLogin() {
        loadUsers();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String selectedRole = roleComboBox.getValue();
        
        if (userInfo.containsKey(username)) {
            String[] info = userInfo.get(username);
            String storedPassword = info[0];
            String userRole = info[1];
              if (storedPassword.equals(password)) {
                if (selectedRole == null || !selectedRole.equalsIgnoreCase(userRole)) {
                    messageLabel.setText("Role does not match user");
                    return;
                }
                currentRole = userRole;
                currentUser = username;  // Set the current user
                messageLabel.setText("");
                switchToMain();
            } else {
                messageLabel.setText("Invalid username or password");
            }
        } else {
            messageLabel.setText("Invalid username or password");
        }
    }

    @FXML
    private void onSignUp() {
        loadUsers();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String selectedRole = roleComboBox.getValue();
        
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Username and password required");
            return;
        }
        
        if (selectedRole == null) {
            messageLabel.setText("Please select a role");
            return;
        }
        
        if (userInfo.containsKey(username)) {
            messageLabel.setText("Username already exists");
            return;
        }
        
        saveUser(username, password, selectedRole);
        messageLabel.setText("Sign up successful! Please log in.");
    }    private void switchToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
            Scene scene = new Scene(loader.load(), 1121, 761);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.centerOnScreen();
            stage.setScene(scene);
            stage.setTitle("Room Reservation System");
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Failed to load main UI");
        }
    }
}
