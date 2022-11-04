package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class Huffman implements Codec {

    Map<Byte, CodeValue> dict;

    @Override
    public void compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        byte[] data = serializedImage.data();
        Map<Byte, Integer> frequencyTable = getFrequencyTable(data);
        Node<TreeValue> node = buildTree(frequencyTable);
        dict = new HashMap<>();
        buildDict(node, dict, new boolean[]{});
        compress(data, stream, dict);
    }

    void compress(byte[] data, ByteArrayOutputStream stream, Map<Byte, CodeValue> dict) throws IOException {
        writeDict(stream, dict);
        ByteBuffer buffer = ByteBuffer.allocate(data.length*3);
        int bitsWritten = 0;
        for (Byte b: data) {
            bitsWritten += dict.get(b).write(buffer, bitsWritten);
        }
        int len = buffer.position();
        if (bitsWritten % 8 != 0) {
            len ++;
        }
        byte[] compressed = new byte[len];
        buffer.position(0);
        buffer.get(compressed, 0, compressed.length);
        stream.write(compressed);
    }

    private void writeDict(ByteArrayOutputStream stream, Map<Byte, CodeValue> dict) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(stream);
        dataOutputStream.writeByte(dict.size() - 1);
        for (Map.Entry<Byte, CodeValue> entry: dict.entrySet()) {
            dataOutputStream.writeByte(entry.getKey());
            dataOutputStream.writeByte(entry.getValue().length);
            dataOutputStream.writeShort(entry.getValue().value);
        }
    }

    static Map<Byte, Integer> getFrequencyTable(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Map<Byte, Integer> result = new HashMap<>();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (result.containsKey(b)) {
                result.put(b, result.get(b) + 1);
            } else {
                result.put(b, 1);
            }
        }
        return result;
    }

    static void buildDict(Node<TreeValue> node, Map<Byte, CodeValue> dict, boolean[] path) {
        if ((node.left == null) && (node.right == null)) {
            CodeValue codeValue = new CodeValue(path);
            if (node.value.values.length > 1) {
                throw new IllegalStateException("Terminal node has more than one value");
            }
            dict.put(node.value.values[0], codeValue);
        } else {
            if (node.left != null) {
                boolean[] nextPath = Arrays.copyOf(path, path.length + 1);
                nextPath[path.length] = false;
                buildDict(node.left, dict, nextPath);
            }
            if (node.right != null) {
                boolean[] nextPath = Arrays.copyOf(path, path.length + 1);
                nextPath[path.length] = true;
                buildDict(node.right, dict, nextPath);
            }
        }
    }

    static Node<TreeValue> buildTree(Map<Byte, Integer> frequencyTable) {
        PriorityQueue<Node<TreeValue>> queue = frequencyTable.entrySet().stream()
                .map(entry -> new Node<>(new TreeValue(entry.getValue(), new byte[]{entry.getKey()})))
                .collect(Collectors.toCollection(PriorityQueue::new));
        while (queue.size() > 1) {
            Node<TreeValue> node1 = queue.poll();
            Node<TreeValue> node2 = queue.poll();
            if (node2 != null) {
                queue.add(new Node<>(new TreeValue(node1.value, node2.value), node1, node2));
            }
        }
        return queue.poll();
    }

    @Override
    public SerializedImage restore(ByteBuffer compressed, Integer width, Integer height) {
        byte[] data = restore(compressed, readDict(compressed), width * height * 3);
        return new SerializedImage(width, height, data);
    }

    byte[] restore(ByteBuffer compressed, Map<CodeValue, Byte> dict, int len) {
        ByteBuffer restored = ByteBuffer.allocate(len);
        List<Integer> codeLengths = dict.keySet().stream().map(c -> c.length).distinct().sorted().collect(Collectors.toList());
        Map<Integer, List<Huffman.CodeValue>> codeByLength = dict.keySet().stream().collect(Collectors.groupingBy(c -> c.length));
        int bitsRead = 0;
        int count = 0;
        while (count < len) {
            for (int codeLength: codeLengths) {
                int pos = compressed.position();
                int value = CodeValue.read(compressed, bitsRead, codeLength);
                Huffman.CodeValue code = null;
                for (Huffman.CodeValue cv: codeByLength.get(codeLength)) {
                    if (cv.value == value) {
                        code = cv;
                        break;
                    }
                }
                if (code == null) {
                    compressed.position(pos);
                } else {
                    restored.put(dict.get(code));
                    bitsRead += codeLength;
                    count++;
                    break;
                }
            }
        }
        return restored.array();
    }

    static Map<CodeValue, Byte> readDict(ByteBuffer compressed) {
        int dictLength = Byte.toUnsignedInt(compressed.get()) + 1;
        Map<CodeValue, Byte> dict = new HashMap<>(dictLength);
        for (int i = 0; i < dictLength; i++) {
            byte b = compressed.get();
            int codeLength = compressed.get();
            int codeValue = compressed.getShort();
            dict.put(new CodeValue(codeValue, codeLength), b);
        }
        return dict;
    }

    @Override
    public Map<String, Object> getLastCompressionProperties() {
        return Map.of(
                "dict length", dict.size(),
                "dict size, bytes", 4*dict.size()+1,
                "shortest code", dict.values().stream().min(Comparator.comparingInt(cv -> cv.length)).map(cv -> cv.length).orElse(-1),
                "longest code", dict.values().stream().max(Comparator.comparingInt(cv -> cv.length)).map(cv -> cv.length).orElse(-1)
        );
    }

    static class TreeValue implements Comparable<TreeValue> {

        private final int freq;
        private final byte[] values;

        public TreeValue(int freq, byte[] values) {
            this.freq = freq;
            this.values = values;
        }

        public TreeValue(TreeValue v1, TreeValue v2) {
            int freq = v1.freq + v2.freq;
            int length = v1.values.length + v2.values.length;
            byte[] values = Arrays.copyOf(v1.values, length);
            System.arraycopy(v2.values, 0, values, v1.values.length, v2.values.length);
            this.freq = freq;
            this.values = values;
        }

        @Override
        public int compareTo(TreeValue treeValue) {
            return freq - treeValue.freq;
        }

        @Override
        public String toString() {
            return String.format("[%s|%d]", new String(values) ,freq);
        }
    }

    static class Node<T extends Comparable<T>> implements Comparable<Node<T>> {
        T value;
        Node<T> left;
        Node<T> right;

        Node(T value, Node<T> left, Node<T> right) {
            this.value = value;
            this.right = right;
            this.left = left;
        }

        Node(T value) {
            this.value = value;
            right = null;
            left = null;
        }

        @Override
        public int compareTo(Node<T> node) {
            return value.compareTo(node.value);
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("(");
            s.append(value);
            if (left != null) {
                s.append("|l=");
                s.append(left);
            }
            if (right != null) {
                s.append("|r=");
                s.append(right);
            }
            s.append(")");
            return s.toString();
        }
    }

    static class CodeValue {
        private final int value;
        private final int length;

        public CodeValue(int value, int length) {
            this.value = value;
            this.length = length;
        }

        public CodeValue(boolean[] treePath) {
            this.length = Integer.valueOf(treePath.length).byteValue();
            int v = 0;
            for (int i = 0; i < length; i++) {
                if (treePath[length-i-1]) {
                    v += Math.pow(2, i);
                }
            }
            this.value = v;
        }

        public int write(ByteBuffer buffer, int bitsWritten) {
            buffer.mark();
            int current = Byte.toUnsignedInt(buffer.get());
            buffer.reset();
            int bitsAvailable = 8 - bitsWritten % 8;
            int written = 0;
            int newValue;
            if (bitsAvailable <= length) {
                newValue = current | (value >> (length - bitsAvailable));
                written += bitsAvailable;
                buffer.put(Integer.valueOf(newValue).byteValue());
                buffer.mark();
            } else {
                newValue = current | (value << (bitsAvailable - length));
                written += length;
                buffer.put(Integer.valueOf(newValue).byteValue());
                buffer.reset();
            }
            while (written < length) {
                int toWrite = length - written;
                if (toWrite >= 8) {
                    newValue = 0xFF & (value >> (length - written - 8));
                    written += 8;
                    buffer.put(Integer.valueOf(newValue).byteValue());
                    buffer.mark();
                } else {
                    newValue = 0xFF & (value << (8 - toWrite));
                    written += toWrite;
                    buffer.put(Integer.valueOf(newValue).byteValue());
                    buffer.reset();
                }
            }
            return written;
        }

        static int read(ByteBuffer buffer, int bitsRead, int codeLength) {
            int bitsAvailable = 8 - bitsRead % 8;
            buffer.mark();
            int current = Byte.toUnsignedInt(buffer.get());
            int read = 0;
            if (bitsAvailable >= codeLength) {
                if (bitsAvailable > codeLength) {
                    buffer.reset();
                }
                return (current >> (bitsAvailable - codeLength)) & ((1 << codeLength) - 1);
            } else {
                int result = (current & ((1 << bitsAvailable) - 1)) << (codeLength - bitsAvailable);
                read += bitsAvailable;
                while (read < codeLength) {
                    buffer.mark();
                    current = Byte.toUnsignedInt(buffer.get());
                    if (codeLength - read < 8) {
                        result += (current >> (8 - codeLength + read)) & ((1 << (codeLength - read)) - 1);
                        read += codeLength - read;
                        buffer.reset();
                    } else {
                        result += current << (codeLength - read - 8);
                        read += 8;
                    }
                }
                return result;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CodeValue codeValue = (CodeValue) o;
            return value == codeValue.value && length == codeValue.length;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, length);
        }

        @Override
        public String toString() {
            String code = Integer.toBinaryString(value);
            String withLeadingZeros = String.format(String.format("%%%ds", length), code).replace(' ', '0');
            return withLeadingZeros.substring(withLeadingZeros.length() - length);
        }
    }

}