package edu.onu.ddechev.controllers;

import edu.onu.ddechev.App;
import edu.onu.ddechev.codecs.Codec;
import edu.onu.ddechev.codecs.NoOp;
import edu.onu.ddechev.utils.ImageAnalyzer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;


public class PrimaryController {

    @FXML
    private ImageView originalImageView;

    @FXML
    private ImageView compressedImageView;

    @FXML
    private TableView imagePropertiesTable;

    private Codec codec = new NoOp();

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
            originalImageView.setFitWidth(image.getWidth());
            originalImageView.setFitHeight(image.getHeight());
            originalImageView.setImage(image);
            populateTable(ImageAnalyzer.analyzeImage(image));
            byte[] compressed = codec.compress(image);
            Image compressedImage = codec.restore(compressed);
            compressedImageView.setFitWidth(compressedImage.getWidth());
            compressedImageView.setFitHeight(compressedImage.getHeight());
            compressedImageView.setImage(compressedImage);
        }
    }

    private void populateTable(List<Map<String, String>> data) {
        if (imagePropertiesTable.getColumns().isEmpty()) {
            TableColumn<Map, Object> column1 = new TableColumn<>("Property");
            column1.setCellValueFactory(new MapValueFactory<>("property"));
            column1.prefWidthProperty().bind(imagePropertiesTable.widthProperty().divide(2).add(-2));
            TableColumn<Map, Object> column2 = new TableColumn<>("Value");
            column2.setCellValueFactory(new MapValueFactory<>("value"));
            column2.prefWidthProperty().bind(imagePropertiesTable.widthProperty().divide(2).add(-2));
            imagePropertiesTable.getColumns().addAll(column1, column2);
        }
        imagePropertiesTable.getItems().clear();
        data.forEach(item -> imagePropertiesTable.getItems().add(item));
    }

    @FXML
    private void close() {
        Platform.exit();
        System.exit(0);
    }

}
