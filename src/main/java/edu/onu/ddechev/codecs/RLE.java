package edu.onu.ddechev.codecs;

import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

public class RLE implements Codec {


    @Override
    public byte[] compress(Integer w, Integer h, PixelReader reader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WritableImage restore(byte[] compressed) {
        throw new UnsupportedOperationException();
    }
}
