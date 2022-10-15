package edu.onu.ddechev.codecs;

import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LZW implements Codec {
    private static final int CLEAR_CODE = 256;
    private static final int END_CODE = 257;
    private static final int CODE_LEN = 12;

    @Override
    public byte[] compress(List<Color> serializedImage, ByteArrayOutputStream stream) {
        Table table = new Table();
        List<Integer> codes = new ArrayList<>();
        codes.add(CLEAR_CODE);
        compressChannel(serializedImage, this::red, table, codes);
        compressChannel(serializedImage, this::green, table, codes);
        compressChannel(serializedImage, this::blue, table, codes);
        codes.add(END_CODE);
        return writeCodes(codes, stream);
    }

    @Override
    public List<Color> restoreSerializedImage(byte[] compressed, Integer length) throws IOException {
        Table table = null;
        Iterator<Integer> codes = readCodes(compressed).iterator();
        ByteBuffer accumulator = ByteBuffer.allocate(length * 3); // 3 channels
        Integer code = codes.next();
        Integer prevCode = null;
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
        }
        accumulator.position(0);
        byte[] red = new byte[length];
        accumulator.get(red, 0, length);
        byte[] green = new byte[length];
        accumulator.get(green, 0, length);
        byte[] blue = new byte[length];
        accumulator.get(blue, 0, length);
        return IntStream.range(0, length)
                .mapToObj(i -> Color.color(
                        byteToChannel(red[i]),
                        byteToChannel(green[i]),
                        byteToChannel(blue[i])))
                .collect(Collectors.toList());
    }

    private void compressChannel(List<Color> serializedImage, ToIntFunction<Color> channelExtractor, Table table, List<Integer> codes) {
        List<Byte> curStr = new ArrayList<>();
        serializedImage.stream()
                .map(c -> Integer.valueOf(channelExtractor.applyAsInt(c)).byteValue())
                .forEach(b -> {
                    List<Byte> newStr = new ArrayList<>(curStr);
                    newStr.add(b);
                    if (!table.has(newStr)) {
                        Integer code = table.get(curStr);
                        codes.add(code);
                        table.add(newStr);
                        curStr.clear();
                    }
                    curStr.add(b);
                });
        codes.add(table.get(curStr));
    }

    private byte[] writeCodes(List<Integer> codes,  ByteArrayOutputStream stream) {
        int currentByte;
        int nextByte = 0;
        boolean hasNext = false;
        for (Integer code : codes) {
            if (!hasNext) {
                /*currentByte = code >> (CODE_LEN - Byte.SIZE);
                nextByte = code & Double.valueOf(Math.pow(2, CODE_LEN - Byte.SIZE) - 1).intValue();*/
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
        return stream.toByteArray();
    }

    private List<Integer> readCodes(byte[] compressed) throws IOException {
        InputStream stream = new ByteArrayInputStream(compressed);
        List<Integer> codes = new ArrayList<>();
        while (stream.available() > 0) {
            byte[] chunks = stream.readNBytes(3);
            codes.add((Byte.toUnsignedInt(chunks[0]) << 4) + ((Byte.toUnsignedInt(chunks[1]) & 0b11110000) >> 4));
            if (chunks.length > 2) {
                codes.add(((Byte.toUnsignedInt(chunks[1]) & 0b00001111) << 8) + Byte.toUnsignedInt(chunks[2]));
            }
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
                //tableByBytes.put(String.valueOf(Character.valueOf((char) code)), code);
            });
        }

        public Integer get(List<Byte> bytes) {
            return tableByBytes.get(toBytesStr(bytes));
        }

        public byte[] get(Integer code) {
            return tableByCode.get(code).getBytes(StandardCharsets.ISO_8859_1);
        }

        public Boolean has(List<Byte> bytesList) {
            return tableByBytes.containsKey(toBytesStr(bytesList));
        }

        public Boolean has(Integer code) {
            return tableByCode.containsKey(code);
        }

        public void add(List<Byte> bytesList) {
            String bytes = toBytesStr(bytesList);
            Integer code = codeCounter++;
            tableByBytes.put(bytes, code);
            tableByCode.put(code, bytes);
        }

        public void add(byte[] bytes) {
            String bytesStr = new String(bytes, StandardCharsets.ISO_8859_1);
            Integer code = codeCounter++;
            tableByBytes.put(bytesStr, code);
            tableByCode.put(code, bytesStr);
        }

        private String toBytesStr(List<Byte> bytesList) {
            byte[] bytes = new byte[bytesList.size()];
            int i = 0;
            for (Byte b: bytesList) {
                bytes[i++] = b;
            }
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }

    }
}
