package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.*;

public class RLE implements Codec {

    private static final Integer MAX_ENCODE_LENGTH = 1 << 7;
    private final List<Integer> sameCounts = new ArrayList<>();
    private final List<Integer> diffCounts = new ArrayList<>();

    @Override
    public byte[] compress(byte[] data) {
        byte[] compressedR = compressChannel(getChannel(0, data));
        byte[] compressedG = compressChannel(getChannel(1, data));
        byte[] compressedB = compressChannel(getChannel(2, data));
        return ByteBuffer
                .allocate(compressedR.length + compressedG.length + compressedB.length)
                .put(compressedR).put(compressedG).put(compressedB)
                .array();
    }

    private byte[] getChannel(int offset, byte[] data) {
        int size = data.length / 3;
        byte[] channel = new byte[size];
        for (int i = 0; i < size; i++) {
            channel[i] = data[i*3+offset];
        }
        return channel;
    }

    public byte[] compressChannel(byte[] data) {
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
    public byte[] restore(byte[] compressed) {
        ByteBuffer buffer = ByteBuffer.wrap(compressed);
        byte[] restored = new byte[getLength(buffer)];
        int index = 0;
        do {
            int current = Byte.toUnsignedInt(buffer.get());
            int flag = current >> 7;
            int count = current & 0B01111111;
            if (flag == 1) { // repeat
                byte value = buffer.get();
                Arrays.fill(restored, index, index + count, value);
                sameCounts.add(count);
            } else { // copy
                buffer.get(restored, index, count);
                diffCounts.add(count);
            }
            index += count;
        } while (buffer.hasRemaining());

        byte[] result = new byte[restored.length];
        int channelLength = restored.length / 3;
        byte[] r = Arrays.copyOfRange(restored, 0, channelLength);
        byte[] g = Arrays.copyOfRange(restored, channelLength, 2*channelLength);
        byte[] b = Arrays.copyOfRange(restored, 2*channelLength, 3*channelLength);
        for (int i = 0; i < r.length; i++) {
            result[i*3] = r[i];
            result[i*3+1] = g[i];
            result[i*3+2] = b[i];
        }
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
