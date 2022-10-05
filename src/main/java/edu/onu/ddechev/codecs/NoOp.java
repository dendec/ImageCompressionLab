package edu.onu.ddechev.codecs;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class NoOp implements Codec {

    @Override
    public byte[] compress(Integer width, Integer height, PixelReader reader) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(ByteBuffer.allocate(HEADER_SIZE).putShort(width.shortValue()).putShort(height.shortValue()).array());
            IntStream.range(0, height).forEach(y ->
                IntStream.range(0, width).forEach(x -> {
                    Color c = reader.getColor(x, y);
                    DoubleStream.of(c.getRed(), c.getGreen(), c.getBlue())
                            .mapToInt(z -> Double.valueOf(z*255).intValue())
                            .forEach(stream::write);
                })
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Override
    public Image restore(byte[] compressed) {
        ByteBuffer header = ByteBuffer.wrap(Arrays.copyOfRange(compressed, 0, HEADER_SIZE));
        short width = header.getShort();
        short height = header.position(2).getShort();
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        IntStream.range(0, height).forEach(y ->
            IntStream.range(0, width).forEach(x -> {
                int index = 3 * (y * width + x) + HEADER_SIZE;
                double r = Byte.toUnsignedInt(compressed[index]) / 255.;
                double g = Byte.toUnsignedInt(compressed[index+1]) / 255.;
                double b = Byte.toUnsignedInt(compressed[index+2]) / 255.;
                Color color = Color.color(r, g, b);
                writer.setColor(x, y, color);
            })
        );
        return image;
    }
}
