module org.ira.room_reservation_system {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.ira.room_reservation_system to javafx.fxml;
    exports org.ira.room_reservation_system;
}