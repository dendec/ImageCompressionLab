package edu.onu.ddechev.controllers;

import edu.onu.ddechev.App;
import edu.onu.ddechev.codecs.Codec;
import edu.onu.ddechev.codecs.NoOp;
import edu.onu.ddechev.utils.AnalysisResult;
import edu.onu.ddechev.utils.ImageAnalyzer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;


public class PrimaryController {

    @FXML
    private ImageView originalImageView;

    @FXML
    private ImageView restoredImageView;

    @FXML
    private TableView<AnalysisResult.Property> compressionPropertiesTable;

    @FXML
    private TableView<AnalysisResult.Property> imagePropertiesTable;

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
            showImage(originalImageView, image);
            AnalysisResult analysisResult = ImageAnalyzer.analyzeCompression(image, codec);
            showImage(restoredImageView, analysisResult.getRestoredImage());
            populateTable(imagePropertiesTable, analysisResult.getImageProperties());
            populateTable(compressionPropertiesTable, analysisResult.getCompressionProperties());
        }
    }

    private void showImage(ImageView imageView, Image image) {
        imageView.setFitWidth(image.getWidth());
        imageView.setFitHeight(image.getHeight());
        imageView.setImage(image);
    }

    private void populateTable(TableView<AnalysisResult.Property> table, List<AnalysisResult.Property> data) {
        if (table.getColumns().isEmpty()) {
            TableColumn<AnalysisResult.Property, String> column1 = new TableColumn<>("Property");
            column1.setCellValueFactory(new PropertyValueFactory<>("name"));
            column1.prefWidthProperty().bind(table.widthProperty().divide(2).add(-2));
            TableColumn<AnalysisResult.Property, String> column2 = new TableColumn<>("Value");
            column2.setCellValueFactory(new PropertyValueFactory<>("value"));
            column2.prefWidthProperty().bind(table.widthProperty().divide(2).add(-2));
            table.getColumns().addAll(column1, column2);
        }
        table.getItems().clear();
        data.forEach(item -> table.getItems().add(item));
    }

    @FXML
    private void close() {
        Platform.exit();
        System.exit(0);
    }

}
