package edu.onu.ddechev.codecs;

import javafx.scene.paint.Color;

import java.security.InvalidParameterException;

public class SerializedImage {
    private final int w;
    private final int h;
    private final byte[] data;
    private final static int R_OFFSET = 0;
    private final static int G_OFFSET = 1;
    private final static int B_OFFSET = 2;

    public SerializedImage(int w, int h) {
        this.w = w;
        this.h = h;
        this.data = new byte[w*h*3];
    }

    public SerializedImage(int w, int h, byte[] r, byte[] g, byte[] b) {
        if (r.length != w*h || g.length != w*h || b.length != w*h) {
            throw new InvalidParameterException("invalid channel length");
        }
        this.w = w;
        this.h = h;
        this.data = new byte[w*h*3];
        for (int i = 0; i < w*h; i++) {
            data[i*3+R_OFFSET] = r[i];
            data[i*3+G_OFFSET] = g[i];
            data[i*3+B_OFFSET] = b[i];
        }
    }

    public SerializedImage(int w, int h, byte[] data) {
        if (data.length < w*h*3) {
            throw new InvalidParameterException(String.format("invalid data length %d: expected %d", data.length, w*h*3));
        }
        this.w = w;
        this.h = h;
        this.data = data;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public int size() {
        return w*h;
    }

    public byte[] getR() {
        return getColor(R_OFFSET);
    }

    public byte[] getG() {
        return getColor(G_OFFSET);
    }

    public byte[] getB() {
        return getColor(B_OFFSET);
    }

    private byte[] getColor(int offset) {
        int size = size();
        byte[] r = new byte[size];
        for (int i = 0; i < size; i++) {
            r[i] = data[i*3+offset];
        }
        return r;
    }

    public byte[] data() {
        return data;
    }

    public void add(int x, int y, Color c) {
        int i = (y*w + x)*3;
        data[i*3+R_OFFSET] = channelToByte(c.getRed());
        data[i*3+G_OFFSET] = channelToByte(c.getGreen());
        data[i*3+B_OFFSET] = channelToByte(c.getBlue());
    }

    public void add(int x, int y, int c) { //ARGB
        int i = (y*w + x)*3;
        add(i, c);
    }

    public void add(int[] colors) { //ARGB
        for (int i = 0; i<colors.length; i++) {
            add(i, colors[i]);
        }
    }

    private void add(int i, int c) { //ARGB
        data[i*3+R_OFFSET] = Integer.valueOf((c & 0x00FF0000) >> 16).byteValue();
        data[i*3+G_OFFSET] = Integer.valueOf((c & 0x0000FF00) >> 8).byteValue();
        data[i*3+B_OFFSET] = Integer.valueOf((c & 0x000000FF)).byteValue();
    }

    public Color get(int x, int y) {
        int i = (y*w + x)*3;
        return Color.color(byteToChannel(data[i+R_OFFSET]), byteToChannel(data[i+G_OFFSET]), byteToChannel(data[i+B_OFFSET]));
    }

    public byte[] get() {
        return data;
    }

    private byte channelToByte(double channel) {
        if (channel < 0.0 || channel > 1.0) {
            throw new InvalidParameterException("channel must be in range [0.0, 1.0]");
        }
        return Long.valueOf(Math.round(channel * 255)).byteValue();
    }

    private double intToChannel(int color) {
        return color / 255.;
    }

    private double byteToChannel(byte color) {
        return intToChannel(Byte.toUnsignedInt(color));
    }
}
