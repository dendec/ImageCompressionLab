package edu.onu.ddechev.utils;

import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

public class AnalysisResult {

    public static class Property {
        private final String name;
        private final Object value;

        public Property(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }
    }

    private final List<Property> imageProperties = new ArrayList<>();
    private final List<Property> compressionProperties = new ArrayList<>();
    private Image restoredImage;
    private byte[] compressedData;

    public AnalysisResult() {
    }

    public void addImageProperty(String name, Object value) {
        imageProperties.add(new Property(name, value));
    }

    public void addCompressionProperty(String name, Object value) {
        compressionProperties.add(new Property(name, value));
    }

    public List<Property> getImageProperties() {
        return imageProperties;
    }

    public List<Property> getCompressionProperties() {
        return compressionProperties;
    }

    public Image getRestoredImage() {
        return restoredImage;
    }

    public byte[] getCompressedData() {
        return compressedData;
    }

    public void setRestoredImage(Image restoredImage) {
        this.restoredImage = restoredImage;
    }

    public void setCompressedData(byte[] compressedData) {
        this.compressedData = compressedData;
    }
}
