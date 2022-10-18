package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LZW12 extends LZW {

    @Override
    protected Integer getCodeLength() {
        return 12;
    }

    @Override
    protected List<Integer> readCodes(ByteBuffer compressed) {
        List<Integer> codes = new ArrayList<>();
        byte[] chunks = new byte[3];
        while (compressed.hasRemaining()) {
            int remaining = compressed.remaining();
            if (remaining >= 3) {
                compressed.get(chunks);
            } else {
                Arrays.fill(chunks, (byte)0);
                compressed.get(chunks, 0, remaining);
            }
            codes.add((Byte.toUnsignedInt(chunks[0]) << 4) + ((Byte.toUnsignedInt(chunks[1]) & 0b11110000) >> 4));
            codes.add(((Byte.toUnsignedInt(chunks[1]) & 0b00001111) << 8) + Byte.toUnsignedInt(chunks[2]));
        }
        return codes;
    }

    @Override
    protected void writeCodes(List<Integer> codes, ByteArrayOutputStream stream) {
        int currentByte;
        int nextByte = 0;
        boolean hasNext = false;
        for (Integer code : codes) {
            if (!hasNext) {
                currentByte = code >> 4;
                nextByte = (code & 0b00001111) << 4;
                hasNext = true;
                stream.write(currentByte);
            } else {
                currentByte = nextByte | code >> 8;
                nextByte = code & 0b11111111;
                stream.write(currentByte);
                stream.write(nextByte);
                hasNext = false;
                nextByte = 0;
            }
        }
        if (hasNext) {
            stream.write(nextByte);
        }
    }
}
