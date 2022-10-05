module edu.onu.ddechev {
    requires javafx.controls;
    requires javafx.fxml;

    opens edu.onu.ddechev to javafx.fxml;
    exports edu.onu.ddechev;
    exports edu.onu.ddechev.controllers;
    opens edu.onu.ddechev.controllers to javafx.fxml;
}