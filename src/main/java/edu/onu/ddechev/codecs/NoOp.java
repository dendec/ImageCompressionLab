package edu.onu.ddechev.codecs;

import java.util.Map;

public class NoOp implements Codec {

    @Override
    public byte[] compress(byte[] data) {
        return data;
    }

    @Override
    public byte[] restore(byte[] compressed) {
        return compressed;
    }

    @Override
    public Map<String, Object> getLastCompressionProperties() {
        return Map.of();
    }
}
