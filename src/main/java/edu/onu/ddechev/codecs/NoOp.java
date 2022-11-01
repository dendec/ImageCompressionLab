package edu.onu.ddechev.codecs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class NoOp implements Codec {

    @Override
    public void compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException {
        stream.write(serializedImage.getR());
        stream.write(serializedImage.getG());
        stream.write(serializedImage.getB());
    }

    @Override
    public SerializedImage restore(ByteBuffer compressed, Integer width, Integer height) {
        int length = width * height;
        byte[] r = new byte[length];
        byte[] g = new byte[length];
        byte[] b = new byte[length];
        compressed
                .get(r, 0, length)
                .get(g, 0, length)
                .get(b, 0, length);
        return new SerializedImage(width, height, r, g, b);
    }

    @Override
    public Map<String, Object> getLastCompressionProperties() {
        return Map.of();
    }
}
