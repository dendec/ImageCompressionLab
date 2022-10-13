package edu.onu.ddechev.codecs;

import javafx.scene.paint.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RLE implements Codec {

    private static final Integer MAX_ENCODE_LENGTH = 1 << 7;

    @Override
    public byte[] compress(List<Color> serializedImage, ByteArrayOutputStream stream) throws IOException {
        List<byte[]> compressedChannels = List.<ToIntFunction<Color>>of(this::red, this::green, this::blue).parallelStream()
                .map(channelExtractor -> compressChannel(serializedImage, channelExtractor))
                .collect(Collectors.toList());
        for (byte[] bytes : compressedChannels) {
            stream.write(bytes);
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
                for (int i = index; i < index + differentPixelsCount; i++) {
                    stream.write(channelExtractor.applyAsInt(serializedImage.get(i)));
                }
                index += differentPixelsCount;
            }
        } while (index < serializedImage.size());
        return stream.toByteArray();
    }

    @Override
    public List<Color> restoreSerializedImage(byte[] compressed, Integer length) {
        ByteBuffer compressedBuffer = ByteBuffer.wrap(compressed);
        List<Double> red = restoreChannel(compressedBuffer, length);
        List<Double> green = restoreChannel(compressedBuffer, length);
        List<Double> blue = restoreChannel(compressedBuffer, length);
        if (List.of(red, green, blue).stream().mapToInt(List::size).allMatch(z -> z == red.size())) {
            return IntStream.range(0, red.size()).mapToObj(index ->
                    Color.color(red.get(index), green.get(index), blue.get(index))
            ).collect(Collectors.toList());
        }
        throw new IllegalStateException("Restored different length of channels");
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
            result++;
            index++;
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
