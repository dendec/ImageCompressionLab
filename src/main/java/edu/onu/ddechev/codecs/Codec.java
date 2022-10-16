package edu.onu.ddechev.codecs;

import javafx.scene.image.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public interface Codec {

    List<Class<? extends Codec>> IMPLEMENTATIONS = List.of(NoOp.class, RLE.class, LZW.class);

    Integer HEADER_SIZE = 4;

    default byte[] compress(Image image) {
        short width = Double.valueOf(image.getWidth()).shortValue();
        short height = Double.valueOf(image.getHeight()).shortValue();
        PixelReader reader = image.getPixelReader();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(ByteBuffer.allocate(HEADER_SIZE).putShort(width).putShort(height).array());
            SerializedImage serializedImage = new SerializedImage(width, height);
            int[] buffer = new int[width*height];
            reader.getPixels(0,0, width, height, WritablePixelFormat.getIntArgbInstance(), buffer, 0, width);
            serializedImage.add(buffer);
            return compress(serializedImage, stream);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Compression error %s", e));
        }
    }

    byte[] compress(SerializedImage serializedImage, ByteArrayOutputStream stream) throws IOException;

    default Image restore(byte[] compressed) {
        ByteBuffer buffer = ByteBuffer.wrap(compressed);
        int width = Short.valueOf(buffer.getShort()).intValue();
        int height = Short.valueOf(buffer.getShort()).intValue();
        WritableImage image = new WritableImage(width, height);
        try {
            SerializedImage serializedImage = restore(buffer, width, height);
            image.getPixelWriter().setPixels(0, 0, serializedImage.getWidth(), serializedImage.getHeight(), WritablePixelFormat.getByteRgbInstance(), serializedImage.get(), 0, serializedImage.getWidth() * 3);
            return image;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Restoration error %s", e));
        }
    }

    SerializedImage restore(ByteBuffer compressed, Integer width, Integer height) throws IOException;

//
//    default int red(Color color) {
//        return channelToInt(color.getRed());
//    }
//
//    default int green(Color color) {
//        return channelToInt(color.getGreen());
//    }
//
//    default int blue(Color color) {
//        return channelToInt(color.getBlue());
//    }
//
//    default int channelToInt(double channel) {
//        return Long.valueOf(Math.round(channel * 255)).intValue();
//    }
//
//    default double intToChannel(int color) {
//        return color / 255.;
//    }
//
//    default double byteToChannel(byte color) {
//        return intToChannel(Byte.toUnsignedInt(color));
//    }
}
