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

    private static final String USERS_FILE = "src/main/resources/org/ira/room_reservation_system/users.csv";
    private static final Map<String, String> users = new HashMap<>();
    private static boolean usersLoaded = false;

    private static final String BOOKINGS_FILE = "src/main/resources/org/ira/room_reservation_system/bookings.csv";
    private static final DateTimeFormatter BOOKING_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    static final List<Booking> bookings = new ArrayList<>();

    private static final Map<String, String> roles = new HashMap<>();
    private static boolean rolesLoaded = false;
    public static String currentRole = null;

    private static final String ROLES_FILE = "src/main/resources/org/ira/room_reservation_system/roles.csv";

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
    }

    private void loadUsers() {
        if (usersLoaded) return;
        usersLoaded = true;
        users.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    users.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadRoles() {
        if (rolesLoaded) return;
        rolesLoaded = true;
        roles.clear();
        if (!Files.exists(Paths.get(ROLES_FILE))) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(ROLES_FILE))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    roles.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveUser(String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(username + "," + password + "\n");
        } catch (Exception ignored) {}
    }

    @FXML
    private ComboBox<String> roleComboBox;

    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().setAll("admin", "staff");
        }
    }

    @FXML
    private void onLogin() {
        loadUsers();
        loadRoles();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String selectedRole = roleComboBox.getValue();
        if (users.containsKey(username) && users.get(username).equals(password)) {
            String userRole = roles.getOrDefault(username, "staff");
            if (selectedRole == null || !selectedRole.equalsIgnoreCase(userRole)) {
                messageLabel.setText("Role does not match user");
                return;
            }
            currentRole = userRole;
            messageLabel.setText("");
            switchToMain();
        } else {
            messageLabel.setText("Invalid username or password");
        }
    }

    @FXML
    private void onSignUp() {
        loadUsers();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Username and password required");
            return;
        }
        if (users.containsKey(username)) {
            messageLabel.setText("Username already exists");
            return;
        }
        users.put(username, password);
        saveUser(username, password);
        messageLabel.setText("Sign up successful! Please log in.");
    }

    private void switchToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Room Reservation System");
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Failed to load main UI");
        }
    }
}
