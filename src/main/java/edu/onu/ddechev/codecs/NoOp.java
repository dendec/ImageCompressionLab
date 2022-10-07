package edu.onu.ddechev.codecs;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class NoOp implements Codec {

    @Override
    public byte[] compress(Integer width, Integer height, PixelReader reader) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(getHeader(width, height));
            IntStream.range(0, height).forEach(y ->
                IntStream.range(0, width).forEach(x -> {
                    Color c = reader.getColor(x, y);
                    DoubleStream.of(c.getRed(), c.getGreen(), c.getBlue())
                            .mapToInt(this::channelToInt)
                            .forEach(stream::write);
                })
            );
        } catch (IOException e) {
            throw new RuntimeException(String.format("Compression error %s", e));
        }
        return stream.toByteArray();
    }

    @Override
    public void restore(byte[] compressed, Integer width, Integer height, PixelWriter writer) {
        IntStream.range(0, height).forEach(y ->
            IntStream.range(0, width).forEach(x -> {
                int index = 3 * (y * width + x);
                double r = Byte.toUnsignedInt(compressed[index]) / 255.;
                double g = Byte.toUnsignedInt(compressed[index+1]) / 255.;
                double b = Byte.toUnsignedInt(compressed[index+2]) / 255.;
                Color color = Color.color(r, g, b);
                writer.setColor(x, y, color);
            })
        );
    }

}
