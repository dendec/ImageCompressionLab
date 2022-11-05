package edu.onu.ddechev.codecs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static edu.onu.ddechev.codecs.Huffman.*;

public class HuffmanTest {

    @Test
    public void endToEndTest1() throws IOException {
        byte[] data = "ABCDEABCDEABCDEABCDEABCDEACDEACDEACDEADEADEDDD".getBytes(StandardCharsets.UTF_8);
        Huffman huffman = new Huffman();
        Map<Byte, Huffman.CodeValue> dict = new HashMap<>();
        Map<Byte, Integer> table = getFrequencyTable(data);
        System.out.println(table.entrySet().stream()
                .map(e -> String.format("%s: %d", new String(new byte[]{e.getKey()}), e.getValue()))
                .collect(Collectors.joining("\n")));
        Node<TreeValue> tree = buildTree(table);
        System.out.println(tree);
        buildDict(tree, dict, new boolean[]{});
        System.out.println(dict.entrySet().stream()
                .map(e -> String.format("%s: %s", new String(new byte[]{e.getKey()}), e.getValue()))
                .collect(Collectors.joining("\n")));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        huffman.compress(data, stream, dict);

        ByteBuffer buffer = ByteBuffer.wrap(stream.toByteArray());
        Map<CodeValue, Byte> dictRestored = readDict(buffer);
        System.out.println(dictRestored.entrySet().stream()
                .map(e -> String.format("%s: %s", e.getKey(), new String(new byte[]{e.getValue()})))
                .collect(Collectors.joining("\n")));
        byte[] restored = huffman.restore(buffer, dictRestored, data.length);
        System.out.println(new String(restored));
        Assertions.assertArrayEquals(data, restored);
    }

    @Test
    public void endToEndTest2() throws IOException {
        byte[] data = new byte[]{-92, 116, 75, 67, -51, -45, 114, -27, 47, 113, 101, -26, -38, -43, -50, -101, -36, -12, -44, 6, 82, 14, 31, -23, -103, 123, -62, -36, -51, 44, 2, 99, -8, 88, -11, -103, 70, -107, -49, -117, -77, 42, 67, -99, 100, 10, 66, 60, 89, 25, -21, -38, -123, 91, -38, -112, 42, 24, -106, -127, -19, 38, 65, 42, 97, -112, -37, 69, -50, 122, 113, -46, -125, -79, 40, -74, 1, 4, 91, -45, 107, 79, 111, -100, -90, -105, 118, -4, 43, -104, 23, -128, 9, -51, -82, 41, -5, 78, -86, 16, 124, 31, -58, -115, -52, 110, 60, -27, 3, 60, -23, -128, 54, 110, 26, -52, 13, -59, 117, 68, -31, -89, -70, 74, -122, -122, 65, 90, -108, -11, 41, -1, 9, -33, 83, 91, -116, 41, 114, -118, 3, -105, -23, 100, -45, 16, 11, -89, -10, -116, -85, 99, -90, -38, -41, -98, 77, 22, 114, -98, 46, 116, -116, 118, 85, -58, 43, -40, 24, 109, 33, -12, 90, 9, -89, -105, -49, -96, 118, -58, 2, -72, 13, -13, 104, -7, -78, -34, 75, 7, 114, 31, -127, 124, 20, -40, -32, 19, 121, -84, -106, 63, -110, -82, -87, 75, -101, -78, -87, 48, -127, -81, -76, 54, -21, -60, 41, -95, -116, 111, -15, 40, 19, 77, 118, -124, -128, 54, 81, 72, 80, 60, -124, 40, -6, 10, 82, -97, 1, -100, 87, 38, 70, -125, -11, -71, -53, -49, 125, 49, -39, 51, -76, 27, 114, 31, -83, -104, -29, 11, -13, -58, 31, -16, 47, -121, -93, 29, -4, -94, 77, -45, -63, -75, 91, 54, -111, -37, 121, -33, 15, 11, -9, 98, -114, -19, -75, 73, -8, -2, -43, 65, 51, 6, 100, 49, 61, 57, 57, -16, 18, 55, -8, 80, 69, 43, 91, -106, -72, -42, -7, 16, 28, 30, -2, 16, 123, -58, 118, -27, -34, -88, 39, 113, 35, 80, -127, 106, 36, -111, 112, -118, 69, 114, 84, 40, 50, 6, 56, -21, 126, -48, 110, 52, -10, -56, -126, -78, -3, -92, -99, -66, 25, 118, 126, -7, -6, 18, -27, -110, -80, -100, -117, -64, 6, -90, 94, 126, 77, -29, 111, 49, 91, -80, 48, -50, -126, 6, 102, -55, -103, 42, -49, -49, 24, 71, -118, 116, -110, -63, -48, 18, -127, -69, 109, 108, 13, 115, -73, 20, 79, 55, 93, -63, -85, -120, -108, -106, -70, -20, 12, -79, 88, 120, -77, -81, 126, -33, -72, 19, -23, 67, 112, 20, -118, -123, 60, -45, -43, -19, 60, -100, 100, -39, 54, -73, 46, -113, 41, 6, 45, 123, -103, 49, 71, 27, -83, -89, -95, -104, -109, -86, -96, -9, 29, -86, -109, 1, -57, 126, 29, -45, -13, 62, -83, 93, -122, -106, -118, -91, 108, -78, 122, -71, -2, 26, -9, 8, -47, -20, 12, -28, 48, -125, 127, 64, -10, -117, -72, 43, 45, -65, 104, -52, 50, 85, -65, 63, 31, 115, -60, 113, 52, -3, -40, 37, 10, -86, -3, -85, 49, -4, -3, -99, 99, -99, -17, -88, 28, -111, -23, -53, -127, 94, 126, -60, 65, 6, 34, -35, -32, 96, -53, 6, 88, -33, -111, 115, -68, -30, 115, 0, -49, 107, -25, 47, -110, -92, 126, -110, 84, 115, 104, -100, 96, -51, -23, 75, -33, -88, -44, 63, -51, 10, -19, 28, 82, 101, -125, -103, -117, 92, 117, -64, -67, -103, -14, -126, 95, -69, 95, 118, -49, -35, 78, -64, -78, 52, 121, 13, 94, 20, -61, -72, 62, 25, 96, -38, -30, -54, -98, 48, 104, 54, -91, -96, 87, -125, 118, -62, -113, 55, -25, -75, 82, 66, 5, -118, 27, 102, -63, 82, 76, -73, 86, 74, 70, 126, -69, 43, -14, 0, 104, 34, -96, 9, -88, -100, -122, -74, -124, -18, -46, -117, 17, -5, 22, 114, -49, -111, -57, -103, -105, -104, -41, 52, -27, -30, 124, -42, 122, -84, 52, -49, 124, -89, -8, -69, -89, 83, 125, -65, 93, 62, 95, -24, -5, 40, 3, -126, 62, 28, -104, 69, -105, 51, 94, 126, 60, 103, -47, -53, 125, 31, 85, -79, 6, -106, -103, 2, -49, 116, 110, -83, -8, 82, -123, -114, 103, -36, 36, 20, -70, -48, -28, 30, -95, 4, -31, -38, 29, -115, 29, -49, -119, 119, -92, 5, 73, 0, -92, 64, -47, 59, 21, 73, -128, 57, -88, 110, 59, 124, -120, -70, -1, -65, 77, -123, 87, 61, 71, 18, -70, -123, -117, -116, -59, 111, -110, 33, 25, 35, -114, 45, 99, -3, 32, 96, -44, 61, 67, 107, 1, -38, -46, -55, -101, -70, 87, 42, -96, 52, -90, -2, 63, 120, 22, 54, 63, -76, 112, 102, 97, -92, -98, 88, -68, -61, -43, 24, 6, -115, -25, -122, 104, 98, 62, -100, 116, -9, -52, -58, 40, 75, -1, 25, 70, -52, -81, 30, 101, -102, 66, 127, -97, 33, 33, 48, 28, -5, 115, -103, -35, -89, 91, 86, -63, -35, -35, -9, 65, 10, 28, 20, -35, -33, 37, 59, 96, 68, -45, 39, -116, 105, -101, -122, 99, 14, 46, -84, 66, 46, 57, 111, 83, -91, 70, 75, -125, -64, 70, 5, 12, 42, 47, 13, 115, 84, -65, -104, -109, -24, -109, 50, 109, 51, -51, -15, -44, -30, 31, 30, -12, -46, 11, 9, 22, 69, 17, -49, -57, 11, 5, 6, -74, -57, 117, -81, -31, 33, -113, 85, -41, -64, 107, 28, -93, 60, -22, -60, 42, -102, 15, -17, 123, 38, 36, 74, 11, -46, -26, -80, -52, -67, 122, -60, 115, 116, 26, 18, -30, -4, -128, 77, -104, 95, 88, -49, 46, -11, 90, -63, 53, -127, -14, 72, 36, 26, 68, -79, -57, -44, -1, 28, 116, -46, 12, -119, 74, 24, -97, -82, 118, 10, 35, -105, 55, 60, 14, -108, -14, -128, 10, -9, -43, 84, 51, -65, -64, 79, 80, -47, 20, -106, -96, 46, -59, -45, -65, 58, -106, 120, 89, 35, 94, 36, -126, 49, -119, -35, 76, 36, 57, -48, 107, 94, 127, -80, -32, 12, 36, 38, 92, -32, 115, 113, 117, 49, 106, -6, -91, 51, -120};
        Huffman huffman = new Huffman();
        Map<Byte, Huffman.CodeValue> dict = new HashMap<>();
        Map<Byte, Integer> table = getFrequencyTable(data);
        Node<TreeValue> tree = buildTree(table);
        buildDict(tree, dict, new boolean[]{});
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        huffman.compress(data, stream, dict);

        ByteBuffer buffer = ByteBuffer.wrap(stream.toByteArray());
        Map<CodeValue, Byte> dictRestored = readDict(buffer);
        byte[] restored = huffman.restore(buffer, dictRestored, data.length);
        System.out.println(new String(restored));
        Assertions.assertArrayEquals(data, restored);
    }

    @Test
    public void endToEndTest() throws IOException {
        Huffman huffman = new Huffman();
        for (int length = 1000; length <= 10000; length++) {
            byte[] data = new byte[length];
            new Random().nextBytes(data);
            System.out.println(length);
            System.out.println(Arrays.toString(data));
            Map<Byte, Huffman.CodeValue> dict = new HashMap<>();
            Map<Byte, Integer> table = getFrequencyTable(data);
            Node<TreeValue> tree = buildTree(table);
            buildDict(tree, dict, new boolean[]{});
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            huffman.compress(data, stream, dict);
            ByteBuffer buffer = ByteBuffer.wrap(stream.toByteArray());
            Map<CodeValue, Byte> dictRestored = readDict(buffer);
            byte[] restored = huffman.restore(buffer, dictRestored, data.length);
            System.out.println(Arrays.toString(restored));
            Assertions.assertArrayEquals(data, restored);
        }
    }

    @Test
    public void readTest1() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{(byte) 0b10101010, (byte) 0b10101010, (byte) 0b10101010});
        int value = Huffman.CodeValue.read(buffer, 7, 11);
        Assertions.assertEquals(0b01010101010, value);
    }

    @Test
    public void readWriteTest1() {
        int length = 11;
        int offset = 7;
        int value = 1422;
        System.out.printf("value %d length %d offset %d\n", value, length, offset);
        Huffman.CodeValue code = new Huffman.CodeValue(value, length);
        ByteBuffer buffer = ByteBuffer.allocate(4);

        int written = code.write(buffer, offset);
        Assertions.assertEquals(length, written);
        Assertions.assertArrayEquals(new byte[]{1, 0b0110_0011, (byte)0b1000_0000, 0}, buffer.array());
        buffer.position(0);
        int valueRestored = Huffman.CodeValue.read(buffer, offset, length);
        Assertions.assertEquals(value, valueRestored);
    }

    @Test
    public void readWriteTest() {
        Random r = new Random();
        for (int length = 1; length <= 31; length++) {
            for (int offset = 0; offset < 8; offset++) {
                int value = r.nextInt() & ((1 << length) - 1);
                System.out.printf("value %d length %d offset %d\n", value, length, offset);
                Huffman.CodeValue code = new Huffman.CodeValue(value, length);
                ByteBuffer buffer = ByteBuffer.allocate(6);

                int written = code.write(buffer, offset);
                Assertions.assertEquals(length, written);

                buffer.position(0);
                int valueRestored = Huffman.CodeValue.read(buffer, offset, length);
                Assertions.assertEquals(value, valueRestored);
            }
        }
    }
}
