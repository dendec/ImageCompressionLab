package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class LZW implements Codec {
    private static final int CLEAR_CODE = 256;
    private static final int END_CODE = 257;
    private static final int CODE_LEN = 12;

    @Override
    public byte[] compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        Table table = new Table();
        List<Integer> codes = new ArrayList<>();
        codes.add(CLEAR_CODE);
        compressChannel(serializedImage.getR(), table, codes);
        compressChannel(serializedImage.getG(), table, codes);
        compressChannel(serializedImage.getB(), table, codes);
        codes.add(END_CODE);
        return writeCodes(codes, stream);
    }

    private void compressChannel(byte[] data, Table table, List<Integer> codes) throws IOException {
        ByteArrayOutputStream curStr = new ByteArrayOutputStream();
        for (byte b: data) {
            ByteArrayOutputStream newStr = new ByteArrayOutputStream();
            curStr.writeTo(newStr);
            newStr.write(b);
            byte[] newStrBytes = newStr.toByteArray();
            if (!table.has(newStrBytes)) {
                Integer code = table.get(curStr.toByteArray());
                codes.add(code);
                table.add(newStrBytes);
                curStr.reset();
            }
            curStr.write(b);
        }
        codes.add(table.get(curStr.toByteArray()));
    }

    @Override
    public SerializedImage restore(ByteBuffer compressed, Integer width, Integer height) throws IOException {
        Table table = null;
        List<Integer> codesList = readCodes(compressed);
        Iterator<Integer> codes = codesList.iterator();
        Integer length = width * height;
        ByteBuffer accumulator = ByteBuffer.allocate(length * 3); // 3 channels
        Integer code = codes.next();
        Integer prevCode = null;
        int i =0;
        while (code != END_CODE) {
            if (code == CLEAR_CODE) {
                table = new Table();
                code = codes.next();
                if (code == END_CODE) {
                    break;
                }
                accumulator.put(table.get(code));
                prevCode = code;
            } else {
                if (table.has(code)) {
                    byte[] str = table.get(code);
                    accumulator.put(str);
                    byte[] prevStr = table.get(prevCode);
                    byte[] newStr = ByteBuffer.allocate(prevStr.length + 1).put(prevStr).put(str[0]).array();
                    table.add(newStr);
                    prevCode = code;
                } else {
                    byte[] prevStr = table.get(prevCode);
                    byte[] newStr = ByteBuffer.allocate(prevStr.length + 1).put(prevStr).put(prevStr[0]).array();
                    accumulator.put(newStr);
                    table.add(newStr);
                    prevCode = code;
                }
            }
            code = codes.next();
            i++;
        }
        accumulator.position(0);
        byte[] r = new byte[length];
        accumulator.get(r, 0, length);
        byte[] g = new byte[length];
        accumulator.get(g, 0, length);
        byte[] b = new byte[length];
        accumulator.get(b, 0, length);
        return new SerializedImage(width, height, r, g, b);
    }

    private byte[] writeCodes(List<Integer> codes,  ByteArrayOutputStream stream) {
        int currentByte;
        int nextByte = 0;
        boolean hasNext = false;
        for (Integer code : codes) {
            if (!hasNext) {
                currentByte = code >> (CODE_LEN - Byte.SIZE);
                nextByte = (code & 0b00001111) << (CODE_LEN - Byte.SIZE);
                hasNext = true;
                stream.write(currentByte);
            } else {
                currentByte = nextByte | code >> Byte.SIZE;
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
        return stream.toByteArray();
    }

    private List<Integer> readCodes(ByteBuffer compressed) {
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

    private static class Table {
        private final Map<String, Integer> tableByBytes = new HashMap<>();
        private final Map<Integer, String> tableByCode = new HashMap<>();
        private Integer codeCounter = 258;

        public Table() {
            IntStream.range(0, 256).forEach(code -> {
                String bytesStr = new String(new byte[]{Integer.valueOf(code).byteValue()}, StandardCharsets.ISO_8859_1);
                tableByBytes.put(bytesStr, code);
                tableByCode.put(code, bytesStr);
            });
        }

        public Integer get(byte[] bytes) {
            return tableByBytes.get(toBytesStr(bytes));
        }

        public byte[] get(Integer code) {
            return tableByCode.get(code).getBytes(StandardCharsets.ISO_8859_1);
        }

        public Boolean has(byte[] bytes) {
            return tableByBytes.containsKey(toBytesStr(bytes));
        }

        public Boolean has(Integer code) {
            return tableByCode.containsKey(code);
        }

        public void add(byte[] bytes) {
            String bytesStr = new String(bytes, StandardCharsets.ISO_8859_1);
            Integer code = codeCounter++;
            tableByBytes.put(bytesStr, code);
            tableByCode.put(code, bytesStr);
        }

        private String toBytesStr(byte[] bytes) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }

    }
}
