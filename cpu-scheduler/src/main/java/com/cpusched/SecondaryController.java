package com.cpusched;

import java.io.IOException;

import javafx.fxml.FXML;

// Controller for secondary screen
public class SecondaryController {

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}