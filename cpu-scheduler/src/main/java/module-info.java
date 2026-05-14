module com.cpusched {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.cpusched to javafx.fxml;
    exports com.cpusched;
}