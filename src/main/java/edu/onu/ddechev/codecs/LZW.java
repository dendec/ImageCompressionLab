package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LZW implements Codec {
    private static final int CLEAR_CODE = 256;
    private static final int END_CODE = 257;
    private static final int CODE_LENGTH = 12;

    private Integer tablesCount;
    private Integer knownCodeCount;
    private Integer unknownCodeCount;

    @Override
    public byte[] compress(byte[] data) throws IOException {
        List<Integer> codes = new ArrayList<>();
        codes.add(CLEAR_CODE);
        compress(data, codes);
        codes.add(END_CODE);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writeCodes(codes, stream);
        return stream.toByteArray();
    }

    void compress(byte[] data, List<Integer> codes) throws IOException {
        Table table = new Table(CODE_LENGTH);
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
    public byte[] restore(byte[] compressed) throws IOException {
        ByteBuffer compressedBuffer = ByteBuffer.wrap(compressed);
        Table table = new Table(CODE_LENGTH);
        List<Integer> codesList = readCodes(compressedBuffer);
        Iterator<Integer> codes = codesList.iterator();
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
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
                arrayOutputStream.write(table.get(code));
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
                arrayOutputStream.write(output);
                table.add(newChain);
            }
            prevCode = code;
            code = codes.next();
        }
        return arrayOutputStream.toByteArray();
    }

    @Override
    public Map<String, Object> getLastCompressionProperties() {
        return Map.of(
                "tables count", tablesCount,
                "known code count", knownCodeCount,
                "unknown code count", unknownCodeCount
        );
    }

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
            return tableByCode.get(code).getBytes(StandardCharsets.ISO_8859_1);
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
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }

        public Integer size() {
            return tableByBytes.size();
        }

        public Boolean hasCapacity() {
            return (capacity == null) || (codeCounter < capacity);
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
