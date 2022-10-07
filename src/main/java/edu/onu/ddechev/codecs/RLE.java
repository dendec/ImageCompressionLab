package edu.onu.ddechev.codecs;

import edu.onu.ddechev.utils.ProfilingUtil;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RLE implements Codec {

    private static final Integer MAX_ENCODE_LENGTH = 1 << 7;

    @Override
    public byte[] compress(Integer width, Integer height, PixelReader reader) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(getHeader(width, height));
            List<Color> serializedImage = new ArrayList<>();
            IntStream.range(0, height).forEach(y ->
                IntStream.range(0, width).forEach(x ->
                    serializedImage.add(reader.getColor(x, y))
                )
            );
            List<byte[]> compressedChannels =List.<ToIntFunction<Color>>of(this::red, this::green, this::blue).parallelStream()
                    .map(channelExtractor -> compressChannel(serializedImage, channelExtractor))
                    .collect(Collectors.toList());
            for (byte[] bytes: compressedChannels) {
                stream.write(bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Compression error %s", e));
        }
        return stream.toByteArray();
    }

    private byte[] compressChannel(List<Color> serializedImage, ToIntFunction<Color> channelExtractor) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int index = 0;
        do {
            Integer samePixelsCount = count(index, serializedImage, comparator(channelExtractor));
            if (samePixelsCount > 1) {
                stream.write(firstByte(true, samePixelsCount));
                stream.write(channelExtractor.applyAsInt(serializedImage.get(index)));
                index += samePixelsCount;
            } else {
                Integer differentPixelsCount = count(index, serializedImage, comparator(channelExtractor).negate());
                stream.write(firstByte(false, differentPixelsCount));
                for (int i = index; i < index+differentPixelsCount; i++) {
                    stream.write(channelExtractor.applyAsInt(serializedImage.get(i)));
                }
                index += differentPixelsCount;
            }
        } while (index < serializedImage.size());
        return stream.toByteArray();
    }

    @Override
    public void restore(byte[] compressed, Integer width, Integer height, PixelWriter writer) {
        ByteBuffer compressedBuffer = ByteBuffer.wrap(compressed);
        Integer length = width * height;
        List<Double> red = restoreChannel(compressedBuffer, length);
        List<Double> green = restoreChannel(compressedBuffer, length);
        List<Double> blue = restoreChannel(compressedBuffer, length);
        IntStream.range(0, height).forEach(y ->
                IntStream.range(0, width).forEach(x -> {
                    int index = y * width + x;
                    Color color = Color.color(red.get(index), green.get(index), blue.get(index));
                    writer.setColor(x, y, color);
                })
        );
    }

    public List<Double> restoreChannel(ByteBuffer compressed, Integer length) {
        List<Double> result = new ArrayList<>(length);
        int index = 0;
        do {
            int current = Byte.toUnsignedInt(compressed.get());
            int flag = current >> 7;
            int count = current & 0B01111111;
            if (flag == 1) { // repeat
                int value = Byte.toUnsignedInt(compressed.get());
                result.addAll(Collections.nCopies(count, intToChannel(value)));
            } else { // copy
                byte[] values = new byte[count];
                compressed.get(values);
                IntStream.range(0, values.length)
                        .map(i -> Byte.toUnsignedInt(values[i]))
                        .mapToObj(this::intToChannel)
                        .collect(Collectors.toCollection(() -> result));
            }
            index += count;
        } while (index < length);
        return result;
    }

    private int firstByte(Boolean flag, Integer count) {
        if (count > MAX_ENCODE_LENGTH) {
            throw new InvalidParameterException(String.format("Payload can not be longer than %d", MAX_ENCODE_LENGTH));
        }
        if (flag) {
            return 1 << 7 | count.byteValue();
        }
        return count.byteValue();
    }

    private Integer count(int index, List<Color> serializedImage, BiPredicate<Color, Color> comparator) {
        Color current, next = null;
        int result = 0;
        do {
            current = serializedImage.get(index);
            if (index < serializedImage.size() - 1) {
                next = serializedImage.get(index + 1);
            }
            result ++;
            index ++;
        } while (comparator.test(current, next) && index < serializedImage.size() && result < MAX_ENCODE_LENGTH - 1);
        return result;
    }

    private BiPredicate<Color, Color> comparator(ToIntFunction<Color> channelExtractor) {
        return (c1, c2) -> {
            if (Stream.of(c1, c2).anyMatch(Objects::isNull)) {
                return false;
            } else {
                return channelExtractor.applyAsInt(c1) == channelExtractor.applyAsInt(c2);
            }
        };
    }

}
