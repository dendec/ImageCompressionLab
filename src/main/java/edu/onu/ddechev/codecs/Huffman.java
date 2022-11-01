package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class Huffman implements Codec {
    @Override
    public void compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        byte[] data = serializedImage.data();
        Map<Byte, Integer> frequencyTable = getFrequencyTable(data);
        Node<TreeValue> node = buildTree(frequencyTable);
        Map<Byte, DictValue> dict = new HashMap<>();
        buildDict(node, dict, new boolean[]{});
        compress(data, stream, dict);
    }

    private void compress(byte[] data, ByteArrayOutputStream stream, Map<Byte, DictValue> dict) throws IOException {
        writeTable(stream, dict);
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        int bitsWritten = 0;
        for (Byte b: data) {
            bitsWritten += dict.get(b).write(buffer, bitsWritten);
        }
        stream.write(buffer.array());
    }

    private void writeTable(ByteArrayOutputStream streamNode, Map<Byte, DictValue> dict) {
        for (Map.Entry<Byte, DictValue> entry: dict.entrySet()) {
            streamNode.write(entry.getKey());
            streamNode.write(entry.getValue().length);
            streamNode.write(entry.getValue().value);
        }
    }

    private Map<Byte, Integer> getFrequencyTable(byte[] data) {
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

    private void buildDict(Node<TreeValue> node, Map<Byte, DictValue> dict, boolean[] path) {
        if ((node.left == null) && (node.right == null)) {
            DictValue dictValue = new DictValue(path);
            if (node.value.values.length > 1) {
                throw new IllegalStateException("Terminal node has more than one value");
            }
            dict.put(node.value.values[0], dictValue);
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

    private Node<TreeValue> buildTree(Map<Byte, Integer> frequencyTable) {
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
        return null;
    }

    @Override
    public Map<String, Object> getLastCompressionProperties() {
        return Map.of();
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
    }

    static class DictValue {
        private final int value;
        private final byte length;

        public DictValue(boolean[] treePath) {
            this.length = Integer.valueOf(treePath.length).byteValue();
            int v = 0;
            for (int i = 0; i < length; i++) {
                if (treePath[i]) {
                    v += Math.pow(2, i);
                }
            }
            this.value = v;
        }

        public int write(ByteBuffer buffer, int bitsWritten) {
            buffer.mark();
            byte current = buffer.get();
            buffer.reset();
            int bitsAvailable = 8 - bitsWritten % 8;
            int written = 0;
            int newValue;
            if (bitsAvailable <= length) {
                newValue = current | (value >> (length - bitsAvailable));
                written += bitsAvailable;
                buffer.put(Integer.valueOf(newValue).byteValue());
            } else {
                newValue = current | (value << (bitsAvailable - length));
                written += length;
                buffer.put(Integer.valueOf(newValue).byteValue());
                buffer.reset();
            }
            while (written < length) {
                int toWrite = length - written;
                if (toWrite > 8) {
                    newValue = 0xFF & (value >> (length - written - 8));
                    written += 8;
                    buffer.put(Integer.valueOf(newValue).byteValue());
                } else {
                    newValue = 0xFF & (value << (8 - toWrite));
                    written += toWrite;
                    buffer.put(Integer.valueOf(newValue).byteValue());
                    buffer.reset();
                }
            }
            return written;
        }
    }

}