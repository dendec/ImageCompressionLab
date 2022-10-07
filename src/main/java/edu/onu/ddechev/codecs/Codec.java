package edu.onu.ddechev.codecs;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public interface Codec {

    List<Class<? extends Codec>> IMPLEMENTATIONS = List.of(NoOp.class, RLE.class);

    Integer HEADER_SIZE = 4;

    default byte[] compress(Image image) {
        Integer w = Double.valueOf(image.getWidth()).intValue();
        Integer h = Double.valueOf(image.getHeight()).intValue();
        return compress(w, h, image.getPixelReader());
    }

    default byte[] getHeader(Integer width, Integer height) {
        return ByteBuffer.allocate(HEADER_SIZE).putShort(width.shortValue()).putShort(height.shortValue()).array();
    }

    byte[] compress(Integer width, Integer height, PixelReader reader);

    default Image restore(byte[] compressed) {
        ByteBuffer header = ByteBuffer.wrap(Arrays.copyOfRange(compressed, 0, HEADER_SIZE));
        int width = Short.valueOf(header.getShort()).intValue();
        int height = Short.valueOf(header.position(Short.BYTES).getShort()).intValue();
        WritableImage image = new WritableImage(width, height);
        restore(Arrays.copyOfRange(compressed, HEADER_SIZE, compressed.length), width, height, image.getPixelWriter());
        return image;
    }

    void restore(byte[] compressed, Integer width, Integer height, PixelWriter writer);

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
}
