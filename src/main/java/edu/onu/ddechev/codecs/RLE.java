package edu.onu.ddechev.codecs;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.*;

public class RLE implements Codec {

    private static final Integer MAX_ENCODE_LENGTH = 1 << 7;
    private final List<Integer> sameCounts = new ArrayList<>();
    private final List<Integer> diffCounts = new ArrayList<>();

    @Override
    public void compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        stream.write(compress(serializedImage.getR()));
        stream.write(compress(serializedImage.getG()));
        stream.write(compress(serializedImage.getB()));
    }

    @Override
    public byte[] compress(byte[] data) {
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
    public Image restoreImage(byte[] compressed) {
        ByteBuffer buffer = ByteBuffer.wrap(compressed);
        int width = Short.valueOf(buffer.getShort()).intValue();
        int height = Short.valueOf(buffer.getShort()).intValue();
        WritableImage image = new WritableImage(width, height);
        buffer = buffer.slice();
        byte[] compressedData = new byte[buffer.limit()];
        buffer.get(compressedData);
        try {
            byte[] data = restore(compressedData);
            int channelLength = data.length / 3;
            byte[] r = Arrays.copyOfRange(data, 0, channelLength);
            byte[] g = Arrays.copyOfRange(data, channelLength, 2*channelLength);
            byte[] b = Arrays.copyOfRange(data, 2*channelLength, 3*channelLength);
            SerializedImage serializedImage = new SerializedImage(width, height, r, g, b);
            image.getPixelWriter().setPixels(0, 0, serializedImage.getWidth(), serializedImage.getHeight(), WritablePixelFormat.getByteRgbInstance(), serializedImage.get(), 0, serializedImage.getWidth() * 3);
            return image;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Restoration error %s", e));
        }
    }

    @Override
    public byte[] restore(byte[] compressed) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(compressed);
        byte[] result = new byte[getLength(buffer)];
        int index = 0;
        do {
            int current = Byte.toUnsignedInt(buffer.get());
            int flag = current >> 7;
            int count = current & 0B01111111;
            if (flag == 1) { // repeat
                byte value = buffer.get();
                Arrays.fill(result, index, index + count, value);
                sameCounts.add(count);
            } else { // copy
                buffer.get(result, index, count);
                diffCounts.add(count);
            }
            index += count;
        } while (buffer.hasRemaining());
        return result;
    }

    private int getLength(ByteBuffer compressed) {
        int length = 0;
        do {
            int current = Byte.toUnsignedInt(compressed.get());
            int flag = current >> 7;
            int count = current & 0B01111111;
            if (flag == 1) { // repeat
                compressed.position(compressed.position()+1);
            } else { // copy
                compressed.position(compressed.position()+count);
            }
            length += count;
        } while (compressed.hasRemaining());
        compressed.position(0);
        return length;
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
