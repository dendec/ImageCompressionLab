package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RLE implements Codec {

    private static final Integer MAX_ENCODE_LENGTH = 1 << 7;
    private final List<Integer> sameCounts = new ArrayList<>();
    private final List<Integer> diffCounts = new ArrayList<>();

    @Override
    public byte[] compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        stream.write(compressChannel(serializedImage.getR()));
        stream.write(compressChannel(serializedImage.getG()));
        stream.write(compressChannel(serializedImage.getB()));
        return stream.toByteArray();
    }

    private byte[] compressChannel(byte[] data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int index = 0;
        do {
            Integer samePixelsCount = count(index, data, true);
            if (samePixelsCount > 1) {
                stream.write(firstByte(true, samePixelsCount));
                stream.write(data, index, 1);
                index += samePixelsCount;
            } else {
                Integer differentPixelsCount = count(index, data, false);
                stream.write(firstByte(false, differentPixelsCount));
                stream.write(data, index, differentPixelsCount);
                index += differentPixelsCount;
            }
        } while (index < data.length);
        return stream.toByteArray();
    }

    @Override
    public SerializedImage restore(ByteBuffer compressed, Integer width, Integer height) throws IOException {
        Integer length = width * height;
        sameCounts.clear();
        diffCounts.clear();
        byte[] r = restoreChannel(compressed, length);
        byte[] g = restoreChannel(compressed, length);
        byte[] b = restoreChannel(compressed, length);
        return new SerializedImage(width, height, r, g, b);
    }

    public byte[] restoreChannel(ByteBuffer compressed, Integer length) {
        byte[] result = new byte[length];
        int index = 0;
        do {
            int current = Byte.toUnsignedInt(compressed.get());
            int flag = current >> 7;
            int count = current & 0B01111111;
            if (flag == 1) { // repeat
                byte value = compressed.get();
                Arrays.fill(result, index, index + count, value);
                sameCounts.add(count);
            } else { // copy
                compressed.get(result, index, count);
                diffCounts.add(count);
            }
            index += count;
        } while (index < length);
        return result;
    }

    private Integer count(int index, byte[] data, boolean isEquals) {
        byte current, next;
        int result = 0;
        boolean accepted;
        do {
            current = data[index];
            if (index < data.length - 1) {
                next = data[index + 1];
                accepted = isEquals == (current == next);
            } else {
                accepted = !isEquals;
            }
            result++;
            index++;
        } while (accepted && index < data.length && result < MAX_ENCODE_LENGTH - 1);
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

    @Override
    public Map<String, Object> getLastCompressionProperties() {
        return Map.of(
                "same count", sameCounts.size(),
                "same max", sameCounts.stream().mapToInt(Integer::intValue).max().orElse(0),
                "same average", sameCounts.stream().mapToInt(Integer::intValue).average().orElse(0.),
                "diff count", diffCounts.size(),
                "diff max", diffCounts.stream().mapToInt(Integer::intValue).max().orElse(0),
                "diff average", diffCounts.stream().mapToInt(Integer::intValue).average().orElse(0.)
        );
    }

}
