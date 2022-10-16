package edu.onu.ddechev.codecs;

import javafx.scene.paint.Color;

import java.security.InvalidParameterException;

public class SerializedImage {
    private final int w;
    private final int h;
    private final byte[] r;
    private final byte[] g;
    private final byte[] b;

    public SerializedImage(int w, int h) {
        this.w = w;
        this.h = h;
        this.r = new byte[w*h];
        this.g = new byte[w*h];
        this.b = new byte[w*h];
    }

    public SerializedImage(int w, int h, byte[] r, byte[] g, byte[] b) {
        if (r.length != w*h || g.length != w*h || b.length != w*h) {
            throw new InvalidParameterException("invalid channel length");
        }
        this.w = w;
        this.h = h;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public byte[] getR() {
        return r;
    }

    public byte[] getG() {
        return g;
    }

    public byte[] getB() {
        return b;
    }

    public void add(int x, int y, Color c) {
        int i = y*w + x;
        r[i] = channelToByte(c.getRed());
        g[i] = channelToByte(c.getGreen());
        b[i] = channelToByte(c.getBlue());
    }

    public void add(int x, int y, int c) { //ARGB
        int i = y*w + x;
        add(i, c);
    }

    public void add(int[] colors) { //ARGB
        for (int i = 0; i<colors.length; i++) {
            add(i, colors[i]);
        }
    }

    private void add(int i, int c) { //ARGB
        r[i] = Integer.valueOf((c & 0x00FF0000) >> 16).byteValue();
        g[i] = Integer.valueOf((c & 0x0000FF00) >> 8).byteValue();
        b[i] = Integer.valueOf((c & 0x000000FF)).byteValue();
    }


    public Color get(int x, int y) {
        int i = y*w + x;
        return Color.color(byteToChannel(r[i]), byteToChannel(g[i]), byteToChannel(b[i]));
    }

    public byte[] get() {
        byte[] result = new byte[w*h*3];
        for (int i = 0; i < w*h; i++) {
            result[3*i] = r[i];
            result[3*i+1] = g[i];
            result[3*i+2] = b[i];
        }
        return result;
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
