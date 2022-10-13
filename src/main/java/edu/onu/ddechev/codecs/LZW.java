package edu.onu.ddechev.codecs;

import javafx.scene.paint.Color;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
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
        codes.add(END_CODE);
        return writeCodes(codes);
    }

    @Override
    public List<Color> restoreSerializedImage(byte[] compressed, Integer length) {
        throw new UnsupportedOperationException();
    }

    private void compressChannel(List<Color> serializedImage, ToIntFunction<Color> channelExtractor, Table table, List<Integer> codes) {
        List<Byte> curStr = new ArrayList<>();
        serializedImage.stream()
                .map(c -> Integer.valueOf(channelExtractor.applyAsInt(c)).byteValue())
                .forEach(b -> {
                    if (!table.has(curStr, b)) {
                        Integer code = table.get(curStr);
                        codes.add(code);
                        table.add(curStr, b);
                        curStr.clear();
                    }
                    curStr.add(b);
                });
        codes.add(table.get(curStr));
    }

    private byte[] writeCodes(List<Integer> codes) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int currentByte;
        int nextByte = 0;
        boolean hasNext = false;
        for (Integer code : codes) {
            if (!hasNext) {
                currentByte = code >> (CODE_LEN - Byte.SIZE);
                nextByte = code & Double.valueOf(Math.pow(2, CODE_LEN - Byte.SIZE) - 1).intValue();
                hasNext = true;
                stream.write(currentByte);
            } else {
                currentByte = nextByte << (CODE_LEN - Byte.SIZE) | code >> Byte.SIZE;
                nextByte = code & Double.valueOf(Math.pow(2, Byte.SIZE) - 1).intValue();
                stream.write(currentByte);
                stream.write(nextByte);
                hasNext = false;
                nextByte = 0;
            }
        }
        return stream.toByteArray();
    }

    private static class Table {
        private final Map<String, Integer> table = new HashMap<>();
        private Integer codeCounter = 258;

        public Table() {
            IntStream.range(0, 256).forEach(code -> table.put(String.valueOf(Character.valueOf((char) code)), code));
        }

        public Integer get(List<Byte> bytesList) {
            byte[] bytes = new byte[bytesList.size()];
            IntStream.range(0, bytesList.size()).forEach(i -> bytes[i] = bytesList.get(i));
            return table.get(new String(bytes, StandardCharsets.ISO_8859_1));
        }

        public Boolean has(List<Byte> bytesList, Byte newByte) {
            byte[] bytes = new byte[bytesList.size() + 1];
            IntStream.range(0, bytesList.size()).forEach(i -> bytes[i] = bytesList.get(i));
            bytes[bytesList.size()] = newByte;
            return table.containsKey(new String(bytes, StandardCharsets.ISO_8859_1));
        }

        public void add(List<Byte> bytesList, Byte newByte) {
            byte[] bytes = new byte[bytesList.size() + 1];
            IntStream.range(0, bytesList.size()).forEach(i -> bytes[i] = bytesList.get(i));
            bytes[bytesList.size()] = newByte;
            table.put(new String(bytes, StandardCharsets.ISO_8859_1), codeCounter++);
        }
    }
}
