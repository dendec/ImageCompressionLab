package edu.onu.ddechev.controllers;

import edu.onu.ddechev.App;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;


public class PrimaryController {

    @FXML
    public ImageView originalImage;

    @FXML
    public TableView imagePropertiesTable;

    @FXML
    private void openFile() throws FileNotFoundException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.png", "*.bmp"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp")
        );
        File file = fileChooser.showOpenDialog(App.getScene().getWindow());
        if (file != null) {
            Image image = new Image(new FileInputStream(file));
            originalImage.setFitWidth(image.getWidth());
            originalImage.setFitHeight(image.getHeight());
            originalImage.setImage(image);
            populateTable(image);
        }
    }

    private void populateTable(Image image) {
        if (imagePropertiesTable.getColumns().isEmpty()) {
            TableColumn<Map, String> column1 = new TableColumn<>("Property");
            column1.setCellValueFactory(new MapValueFactory<>("property"));

            TableColumn<Map, String> column2 = new TableColumn<>("Value");
            column2.setCellValueFactory(new MapValueFactory<>("value"));
            imagePropertiesTable.getColumns().addAll(column1, column2);
        }
        imagePropertiesTable.getItems().clear();
        imagePropertiesTable.getItems().addAll(Map.of("property", "width", "value", String.valueOf(image.getWidth())));
        imagePropertiesTable.getItems().addAll(Map.of("property", "height", "value", String.valueOf(image.getHeight())));
    }

    @FXML
    private void close() {
        Platform.exit();
        System.exit(0);
    }

}
