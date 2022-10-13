package edu.onu.ddechev.controllers;

import edu.onu.ddechev.App;
import edu.onu.ddechev.codecs.Codec;
import edu.onu.ddechev.utils.AnalysisResult;
import edu.onu.ddechev.utils.ImageAnalyzer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class PrimaryController implements Initializable {

    @FXML
    private ImageView originalImageView;

    @FXML
    private ImageView restoredImageView;

    @FXML
    private TableView<AnalysisResult.Property> compressionPropertiesTable;

    @FXML
    private TableView<AnalysisResult.Property> imagePropertiesTable;

    @FXML
    private MenuBar menuBar;

    private Codec codec;
    private File file;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable(imagePropertiesTable);
        initializeTable(compressionPropertiesTable);
        initializeMenu();
    }

    private void initializeMenu() {
        ToggleGroup toggleGroup = new ToggleGroup();
        menuBar.getMenus().stream()
            .filter(item -> item.getText().equals("Codec"))
            .forEach(item -> Codec.IMPLEMENTATIONS.stream()
                    .map(codec -> {
                        RadioMenuItem menuItem = new RadioMenuItem(codec.getSimpleName());
                        menuItem.setToggleGroup(toggleGroup);
                        menuItem.setOnAction(this::changeCodec);
                        return menuItem;
                    })
                    .collect(Collectors.toCollection(item::getItems))
            );
    }

    private void initializeTable(TableView<AnalysisResult.Property> table) {
        TableColumn<AnalysisResult.Property, String> column1 = new TableColumn<>("Property");
        column1.setCellValueFactory(new PropertyValueFactory<>("name"));
        column1.prefWidthProperty().bind(table.widthProperty().divide(2).add(-2));
        TableColumn<AnalysisResult.Property, String> column2 = new TableColumn<>("Value");
        column2.setCellValueFactory(new PropertyValueFactory<>("value"));
        column2.prefWidthProperty().bind(table.widthProperty().divide(2).add(-2));
        table.getColumns().addAll(column1, column2);
    }

    @FXML
    private void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.jpg", "*.png", "*.bmp"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp")
        );
        file = fileChooser.showOpenDialog(App.getScene().getWindow());
        updateForm();
    }

    private void showImage(ImageView imageView, Image image) {
        if (image != null) {
            imageView.setFitWidth(image.getWidth());
            imageView.setFitHeight(image.getHeight());
            imageView.setImage(image);
        }
    }

    private void populateTable(TableView<AnalysisResult.Property> table, List<AnalysisResult.Property> data) {
        if (table.getColumns().isEmpty()) {
            initializeTable(table);
        }
        table.getItems().clear();
        data.forEach(item -> table.getItems().add(item));
    }

    private void updateForm() {
        if (file != null) {
            Image image;
            try {
                image = new Image(new FileInputStream(file));
                showImage(originalImageView, image);
                AnalysisResult analysisResult = ImageAnalyzer.analyzeCompression(image, codec);
                showImage(restoredImageView, analysisResult.getRestoredImage());
                populateTable(imagePropertiesTable, analysisResult.getImageProperties());
                populateTable(compressionPropertiesTable, analysisResult.getCompressionProperties());
            } catch (FileNotFoundException e) {
                showError(String.format("File %s not found", file.getAbsolutePath()));
            } catch (RuntimeException e) {
                showError(String.format("Error %s", e));
            }
        }
    }

    @FXML
    private void close() {
        Platform.exit();
        System.exit(0);
    }

    private void changeCodec(ActionEvent event) {
        if (event.getSource() instanceof MenuItem) {
            MenuItem menuItem = (MenuItem) event.getSource();
            Optional<Class<? extends Codec>> maybeCodecClass = Codec.IMPLEMENTATIONS.stream()
                    .filter(codec -> codec.getSimpleName().equals(menuItem.getText()))
                    .findFirst();
            if (maybeCodecClass.isPresent()) {
                try {
                    codec = maybeCodecClass.get().getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    showError(e);
                }
            }
            updateForm();
        }
    }

    private void showError(Exception e) {
        e.printStackTrace();
        showError(e.getMessage());
    }

    private void showError(String s) {
        Alert alert = new Alert(Alert.AlertType.ERROR, s, ButtonType.OK);
        alert.show();
    }

}
