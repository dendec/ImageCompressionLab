package edu.onu.ddechev.codecs;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface Codec {

    List<Class<? extends Codec>> IMPLEMENTATIONS = List.of(NoOp.class, RLE.class, LZW12.class, Huffman.class);

    Integer HEADER_SIZE = 4;

    default byte[] compress(Image image) {
        short width = Double.valueOf(image.getWidth()).shortValue();
        short height = Double.valueOf(image.getHeight()).shortValue();
        PixelReader reader = image.getPixelReader();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(ByteBuffer.allocate(HEADER_SIZE).putShort(width).putShort(height).array());
            SerializedImage serializedImage = new SerializedImage(width, height);
            int[] buffer = new int[width*height];
            reader.getPixels(0,0, width, height, WritablePixelFormat.getIntArgbInstance(), buffer, 0, width);
            serializedImage.add(buffer);
            compress(serializedImage, stream);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Compression error %s", e));
        }
    }

    default void compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        stream.write(compress(serializedImage.data()));
    }

    byte[] compress(byte[] data) throws IOException;

    default Image restoreImage(byte[] compressed) {
        ByteBuffer buffer = ByteBuffer.wrap(compressed);
        int width = Short.valueOf(buffer.getShort()).intValue();
        int height = Short.valueOf(buffer.getShort()).intValue();
        WritableImage image = new WritableImage(width, height);
        buffer = buffer.slice();
        byte[] compressedData = new byte[buffer.limit()];
        buffer.get(compressedData);
        try {
            SerializedImage serializedImage = new SerializedImage(width, height, restore(compressedData));//(buffer, width, height);
            image.getPixelWriter().setPixels(0, 0, serializedImage.getWidth(), serializedImage.getHeight(), WritablePixelFormat.getByteRgbInstance(), serializedImage.get(), 0, serializedImage.getWidth() * 3);
            return image;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Restoration error %s", e));
        }
    }

    byte[] restore(byte[] compressed) throws IOException;

    Map<String, Object> getLastCompressionProperties();

}
