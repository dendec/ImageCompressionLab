package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LZW16 extends LZW {

    @Override
    protected Integer getCodeLength() {
        return 16;
    }

    @Override
    protected List<Integer> readCodes(ByteBuffer compressed) {
        List<Integer> codes = new ArrayList<>();
        byte[] chunks = new byte[2];
        while (compressed.hasRemaining()) {
            int remaining = compressed.remaining();
            if (remaining >= 2) {
                compressed.get(chunks);
            } else {
                Arrays.fill(chunks, (byte)0);
                compressed.get(chunks, 0, remaining);
            }
            codes.add((Byte.toUnsignedInt(chunks[0]) << 8) + Byte.toUnsignedInt(chunks[1]));
        }
        return codes;
    }

    @Override
    protected void writeCodes(List<Integer> codes, ByteArrayOutputStream stream) {
        int currentByte, nextByte;
        for (Integer code : codes) {
            currentByte = (code & 0xFF00) >> 8;
            nextByte = code & 0x00FF;
            stream.write(currentByte);
            stream.write(nextByte);
        }
    }


}
