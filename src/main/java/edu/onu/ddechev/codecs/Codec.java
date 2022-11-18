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
            int[] buffer = new int[width*height];
            reader.getPixels(0,0, width, height, WritablePixelFormat.getIntArgbInstance(), buffer, 0, width);
            byte[] data = new byte[buffer.length*3];
            for (int i = 0; i<buffer.length; i++) {
                data[i*3] = Integer.valueOf((buffer[i] & 0x00FF0000) >> 16).byteValue();
                data[i*3+1] = Integer.valueOf((buffer[i] & 0x0000FF00) >> 8).byteValue();
                data[i*3+2] = Integer.valueOf((buffer[i] & 0x000000FF)).byteValue();
            }
            stream.write(compress(data));
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Compression error %s", e));
        }
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
            byte[] data = restore(compressedData);
            image.getPixelWriter().setPixels(0, 0,
                    width, height,
                    WritablePixelFormat.getByteRgbInstance(), data,
                    0, width * 3);
            return image;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Restoration error %s", e));
        }
    }

    byte[] restore(byte[] compressed) throws IOException;

    Map<String, Object> getLastCompressionProperties();

}
