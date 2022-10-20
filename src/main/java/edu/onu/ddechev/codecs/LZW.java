package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class LZW implements Codec {
    private static final int CLEAR_CODE = 256;
    private static final int END_CODE = 257;

    private Integer tablesCount;
    private Integer knownCodeCount;
    private Integer unknownCodeCount;

    @Override
    public byte[] compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        List<Integer> codes = new ArrayList<>();
        byte[] data = ByteBuffer.allocate(serializedImage.size() * 3)
                .put(serializedImage.getR())
                .put(serializedImage.getG())
                .put(serializedImage.getB()).array();
        codes.add(CLEAR_CODE);
        compress(data, codes);
        codes.add(END_CODE);
        writeCodes(codes, stream);
        return stream.toByteArray();
    }

    private void compress(byte[] data, List<Integer> codes) throws IOException {
        Table table = new Table(getCodeLength());
        ByteArrayOutputStream curStr = new ByteArrayOutputStream();
        for (byte b : data) {
            ByteArrayOutputStream newStr = new ByteArrayOutputStream();
            curStr.writeTo(newStr);
            newStr.write(b);
            byte[] newStrBytes = newStr.toByteArray();
            if (!table.has(newStrBytes)) {
                Integer code = table.get(curStr.toByteArray());
                codes.add(code);
                table.add(newStrBytes);
                if (!table.hasCapacity()) {
                    table.init();
                    codes.add(CLEAR_CODE);
                }
                curStr.reset();
            }
            curStr.write(b);
        }
        codes.add(table.get(curStr.toByteArray()));
    }

    @Override
    public SerializedImage restore(ByteBuffer compressed, Integer width, Integer height) throws IOException {
        Table table = new Table(getCodeLength());
        List<Integer> codesList = readCodes(compressed);
        Iterator<Integer> codes = codesList.iterator();
        int length = width * height;
        ByteBuffer accumulator = ByteBuffer.allocate(length * 3); // 3 channels
        Integer code = codes.next();
        Integer prevCode = null;
        tablesCount = 0;
        knownCodeCount = 0;
        unknownCodeCount = 0;
        byte[] newChain, output, prevStr;
        while (code != END_CODE) {
            if (code == CLEAR_CODE) {
                table.init();
                tablesCount++;
                code = codes.next();
                if (code == END_CODE) {
                    break;
                }
                accumulator.put(table.get(code));
            } else {
                prevStr = table.get(prevCode);
                if (table.has(code)) {
                    knownCodeCount++;
                    output = table.get(code);
                    newChain = ByteBuffer.allocate(prevStr.length + 1).put(prevStr).put(output[0]).array();
                } else {
                    unknownCodeCount++;
                    newChain = ByteBuffer.allocate(prevStr.length + 1).put(prevStr).put(prevStr[0]).array();
                    output = newChain;
                }
                accumulator.put(output);
                table.add(newChain);
            }
            prevCode = code;
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

    @Override
    public Map<String, Object> getLastCompressionProperties() {
        return Map.of(
                "tables count", tablesCount,
                "known code count", knownCodeCount,
                "unknown code count", unknownCodeCount
        );
    }

    protected abstract Integer getCodeLength();

    protected abstract List<Integer> readCodes(ByteBuffer compressed);

    protected abstract void writeCodes(List<Integer> codes, ByteArrayOutputStream stream) throws IOException;

    private static class Table {
        private final Map<String, Integer> tableByBytes;
        private final Map<Integer, String> tableByCode;
        private Integer codeCounter;
        private final Integer capacity;

        public Table(Integer codeLength) {
            if (codeLength == null) {
                tableByBytes = new HashMap<>();
                tableByCode = new HashMap<>();
                this.capacity = null;
            } else {
                this.capacity = 1 << codeLength;
                if (codeLength > 24) {
                    throw new IllegalStateException("Code length too long");
                }
                tableByBytes = new HashMap<>(1 << codeLength);
                tableByCode = new HashMap<>(1 << codeLength);
            }
            init();
        }

        public Table() {
            this(null);
        }

        public void init() {
            tableByBytes.clear();
            tableByCode.clear();
            IntStream.range(0, 256).forEach(code -> {
                String bytesStr = toBytesStr(new byte[]{Integer.valueOf(code).byteValue()});
                tableByBytes.put(bytesStr, code);
                tableByCode.put(code, bytesStr);
            });
            codeCounter = 258;
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
            if (size().equals(capacity)) {
                throw new IllegalStateException("Table overflow");
            }
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

        public Boolean hasCapacity() {
            return (capacity == null) || (size() < capacity);
        }

        @Override
        public String toString() {
            return tableByCode.entrySet().stream()
                    .filter(e -> e.getKey() >= 258)
                    .sorted(Comparator.comparingInt(Map.Entry::getKey))
                    .map(e -> String.format("%d: %s", e.getKey(), e.getValue()))
                    .collect(Collectors.joining("\n"));
        }
    }
}
