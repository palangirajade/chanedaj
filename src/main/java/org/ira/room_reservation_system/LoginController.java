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

    private void saveUser(String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(username + "," + password + "\n");
        } catch (Exception ignored) {}
    }

    @FXML
    private void onLogin() {
        loadUsers();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (users.containsKey(username) && users.get(username).equals(password)) {
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
            messageLabel.setText("Failed to load main UI");
        }
    }
}
