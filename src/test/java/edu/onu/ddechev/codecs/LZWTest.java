package edu.onu.ddechev.codecs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static edu.onu.ddechev.codecs.Huffman.*;
import static edu.onu.ddechev.codecs.Huffman.readDict;

public class LZWTest {

    @Test
    public void endToEndTest1() throws IOException {
        byte[] data = "ABCDEABCDEABCDEABCDEABCDEACDEACDEACDEADEADEDDD".getBytes(StandardCharsets.UTF_8);
        LZW lzw = new LZW();
        byte[] compressed = lzw.compress(data);
        byte[] restored = lzw.restore(compressed);
        Assertions.assertArrayEquals(data, restored);
    }

    @Test
    public void endToEndTest2() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("test.csv");
        assert is != null;
        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        List<Byte> bytesList = Arrays.stream(reader.readLine().split(",")).map(s->Byte.valueOf(s.trim())).collect(Collectors.toList());
        byte[] data = new byte[bytesList.size()];
        for (int i = 0; i < bytesList.size(); i++) {
            data[i]=bytesList.get(i);
        }
        LZW lzw = new LZW();
        byte[] compressed = lzw.compress(data);
        byte[] restored = lzw.restore(compressed);
        System.out.println(Arrays.toString(data));
        System.out.println(Arrays.toString(restored));
        Assertions.assertArrayEquals(data, restored);
    }

    @Test
    public void endToEndTest() throws IOException {
        LZW lzw = new LZW();
        for (int length = 10000; length <= 20000; length++) {
            byte[] data = new byte[length];
            new Random().nextBytes(data);
            if (length % 1000 == 0) {
                System.out.println(length);
            }
            byte[] compressed = lzw.compress(data);
            byte[] restored = lzw.restore(compressed);
            if (data.length != restored.length) {
                System.out.println(Arrays.toString(data));
            }
            Assertions.assertArrayEquals(data, restored);
        }
    }
}
