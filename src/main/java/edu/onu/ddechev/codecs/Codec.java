package edu.onu.ddechev.codecs;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public interface Codec {

    List<Class<? extends Codec>> IMPLEMENTATIONS = List.of(NoOp.class, RLE.class, LZW.class);

    Integer HEADER_SIZE = 4;

    default byte[] compress(Image image) {
        Integer w = Double.valueOf(image.getWidth()).intValue();
        Integer h = Double.valueOf(image.getHeight()).intValue();
        return compress(w, h, image.getPixelReader());
    }

    default byte[] getHeader(Integer width, Integer height) {
        return ByteBuffer.allocate(HEADER_SIZE).putShort(width.shortValue()).putShort(height.shortValue()).array();
    }

    default byte[] compress(Integer width, Integer height, PixelReader reader) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(getHeader(width, height));
            List<Color> serializedImage = new ArrayList<>();
            IntStream.range(0, height).forEach(y ->
                    IntStream.range(0, width).forEach(x ->
                            serializedImage.add(reader.getColor(x, y))
                    )
            );
            return compress(serializedImage, stream);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Compression error %s", e));
        }
    }

    byte[] compress(List<Color> serializedImage, ByteArrayOutputStream stream) throws IOException;

    default Image restore(byte[] compressed) {
        ByteBuffer header = ByteBuffer.wrap(Arrays.copyOfRange(compressed, 0, HEADER_SIZE));
        int width = Short.valueOf(header.getShort()).intValue();
        int height = Short.valueOf(header.position(Short.BYTES).getShort()).intValue();
        int length = width * height;
        WritableImage image = new WritableImage(width, height);
        try {
            List<Color> serializedImage = restoreSerializedImage(Arrays.copyOfRange(compressed, HEADER_SIZE, compressed.length), length);
            restore(width, height, serializedImage, image.getPixelWriter());
            return image;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Restoration error %s", e));
        }
    }

    List<Color> restoreSerializedImage(byte[] compressed, Integer length) throws IOException;

    default void restore(Integer width, Integer height, List<Color> serializedImage, PixelWriter writer) {
        IntStream.range(0, height).forEach(y ->
                IntStream.range(0, width).forEach(x ->
                        writer.setColor(x, y, serializedImage.get(y * width + x))
                )
        );
    }

    default int red(Color color) {
        return channelToInt(color.getRed());
    }

    default int green(Color color) {
        return channelToInt(color.getGreen());
    }

    default int blue(Color color) {
        return channelToInt(color.getBlue());
    }

    default int channelToInt(double channel) {
        return Long.valueOf(Math.round(channel * 255)).intValue();
    }

    default double intToChannel(int color) {
        return color / 255.;
    }

    default double byteToChannel(byte color) {
        return intToChannel(Byte.toUnsignedInt(color));
    }
}
