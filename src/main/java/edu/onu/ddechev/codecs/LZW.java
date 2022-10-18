package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

public abstract class LZW implements Codec {
    private static final int CLEAR_CODE = 256;
    private static final int END_CODE = 257;

    @Override
    public byte[] compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        List<Integer> codes = new ArrayList<>();
        byte[] data = ByteBuffer.allocate(serializedImage.size() * 3)
                .put(serializedImage.getR())
                .put(serializedImage.getG())
                .put(serializedImage.getB()).array();
        codes.add(CLEAR_CODE);
        compressChannel(data, new Table(getCodeLength()), codes);
        codes.add(END_CODE);
        writeCodes(codes, stream);
        return stream.toByteArray();
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
        System.out.printf("Table size %d\n", table.size());
        int capacity = 1 << getCodeLength();
        if (table.size() > capacity) {
            throw new IllegalStateException("Table overflow");
        }
    }

    @Override
    public SerializedImage restore(ByteBuffer compressed, Integer width, Integer height) {
        Table table = null;
        List<Integer> codesList = readCodes(compressed);
        Iterator<Integer> codes = codesList.iterator();
        int length = width * height;
        ByteBuffer accumulator = ByteBuffer.allocate(length * 3); // 3 channels
        Integer code = codes.next();
        Integer prevCode = null;
        while (code != END_CODE) {
            if (code == CLEAR_CODE) {
                table = new Table(getCodeLength());
                code = codes.next();
                if (code == END_CODE) {
                    break;
                }
                accumulator.put(table.get(code));
                prevCode = code;
            } else {
                assert table != null;
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

    protected abstract List<Integer> readCodes(ByteBuffer compressed);

    protected abstract void writeCodes(List<Integer> codes, ByteArrayOutputStream stream) throws IOException;

    protected abstract Integer getCodeLength();

    private static class Table {
        private final Map<String, Integer> tableByBytes;
        private final Map<Integer, String> tableByCode;
        private Integer codeCounter = 258;

        public Table(Integer codeLength) {
            if (codeLength == null) {
                tableByBytes = new HashMap<>();
                tableByCode = new HashMap<>();
            } else {
                if (codeLength > 24) {
                    throw new IllegalStateException("Code length too long");
                }
                tableByBytes = new HashMap<>(1 << codeLength);
                tableByCode = new HashMap<>(1 << codeLength);
            }
            IntStream.range(0, 256).forEach(code -> {
                String bytesStr = toBytesStr(new byte[]{Integer.valueOf(code).byteValue()});
                tableByBytes.put(bytesStr, code);
                tableByCode.put(code, bytesStr);
            });
        }

        public Table() {
            this(null);
        }

        public Integer get(byte[] bytes) {
            return tableByBytes.get(toBytesStr(bytes));
        }

        public byte[] get(Integer code) {
            return Base64.getDecoder().decode(tableByCode.get(code));
        }

        public Boolean has(byte[] bytes) {
            return tableByBytes.containsKey(toBytesStr(bytes));
        }

        public Boolean has(Integer code) {
            return tableByCode.containsKey(code);
        }

        public void add(byte[] bytes) {
            String bytesStr = toBytesStr(bytes);
            Integer code = codeCounter++;
            tableByBytes.put(bytesStr, code);
            tableByCode.put(code, bytesStr);
        }

        private String toBytesStr(byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }

        public Integer size() {
            return tableByBytes.size();
        }
    }
}
