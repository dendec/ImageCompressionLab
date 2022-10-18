package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LZW20 extends LZW {

    @Override
    protected Integer getCodeLength() {
        return 20;
    }

    @Override
    protected List<Integer> readCodes(ByteBuffer compressed) {
        List<Integer> codes = new ArrayList<>();
        byte[] chunks = new byte[5];
        while (compressed.hasRemaining()) {
            int remaining = compressed.remaining();
            if (remaining >= 5) {
                compressed.get(chunks);
            } else {
                Arrays.fill(chunks, (byte)0);
                compressed.get(chunks, 0, remaining);
            }
            codes.add((Byte.toUnsignedInt(chunks[0]) << 12) + (Byte.toUnsignedInt(chunks[1]) << 4) + ((Byte.toUnsignedInt(chunks[2]) & 0xF0) >> 4));
            codes.add(((Byte.toUnsignedInt(chunks[2]) & 0x0F) << 16) + (Byte.toUnsignedInt(chunks[3]) << 8) + + Byte.toUnsignedInt(chunks[4]));
        }
        return codes;
    }

    @Override
    protected void writeCodes(List<Integer> codes, ByteArrayOutputStream stream) throws IOException {
        byte[] chunks = new byte[5];
        int code;
        for (int i = 0; i < codes.size(); i+=2) {
            code = codes.get(i);
            chunks[0] = Integer.valueOf((code & 0x000FF000) >> 12).byteValue();
            chunks[1] = Integer.valueOf((code & 0x00000FF0) >> 4).byteValue();
            chunks[2] = Integer.valueOf((code & 0x0000000F) << 4).byteValue();
            if (i < codes.size() - 1) {
                code = codes.get(i+1);
                chunks[2] = Integer.valueOf(chunks[2] | (Integer.valueOf((code & 0x000F0000) >> 16).byteValue())).byteValue();
                chunks[3] = Integer.valueOf((code & 0x0000FF00) >> 8).byteValue();
                chunks[4] = Integer.valueOf(code & 0x000000FF).byteValue();
                stream.write(chunks);
            } else {
                stream.write(chunks, 0, 3);
            }
        }
    }
}
