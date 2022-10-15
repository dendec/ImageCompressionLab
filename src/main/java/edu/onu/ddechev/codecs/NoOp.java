package edu.onu.ddechev.codecs;

import javafx.scene.paint.Color;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class NoOp implements Codec {

    @Override
    public byte[] compress(List<Color> serializedImage, ByteArrayOutputStream stream) {
        serializedImage.forEach(c ->
                DoubleStream.of(c.getRed(), c.getGreen(), c.getBlue())
                        .mapToInt(this::channelToInt)
                        .forEach(stream::write));
        return stream.toByteArray();
    }

    @Override
    public List<Color> restoreSerializedImage(byte[] compressed, Integer length) {
        return IntStream.range(0, length).mapToObj(i -> {
            int index = 3 * i;
            double r = byteToChannel(compressed[index]);
            double g = byteToChannel(compressed[index + 1]);
            double b = byteToChannel(compressed[index + 2]);
            return Color.color(r, g, b);
        }).collect(Collectors.toList());
    }
}
