package edu.onu.ddechev.codecs;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

public interface Codec {

    Integer HEADER_SIZE = 4;

    default byte[] compress(Image image) {
        Integer w = Double.valueOf(image.getWidth()).intValue();
        Integer h = Double.valueOf(image.getHeight()).intValue();
        return compress(w, h, image.getPixelReader());
    }

    byte[] compress(Integer w, Integer h, PixelReader reader);

    Image restore(byte[] compressed);
}
