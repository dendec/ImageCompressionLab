package edu.onu.ddechev.utils;

import edu.onu.ddechev.codecs.Codec;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.io.File;
import java.util.*;
import java.util.stream.IntStream;

public class CompressionAnalyzer {

    private CompressionAnalyzer() {
    }

    public static AnalysisResult analyzeCompression(File file, Image image, Codec codec) {
        AnalysisResult result = new AnalysisResult();
        int size = getSize(image);
        result.addImageProperty("path", file.getAbsolutePath());
        result.addImageProperty("file size, bytes", file.length());
        result.addImageProperty("width", getWidth(image));
        result.addImageProperty("height", getHeight(image));
        result.addImageProperty("size, bytes", size);
        result.addImageProperty("colors", getColorsCount(image));
        if (codec != null) {
            ProfilingUtil.ProfilingResult<byte[]> compressionResult = ProfilingUtil.executionTime(() -> codec.compress(image));
            byte[] compressed = compressionResult.getResult();
            ProfilingUtil.ProfilingResult<Image> restoreResult = ProfilingUtil.executionTime(() -> codec.restore(compressed));
            int compressedSize = compressed.length - Codec.HEADER_SIZE;
            result.addCompressionProperty("compression", codec.getClass().getSimpleName());
            result.addCompressionProperty("compression time, ms", compressionResult.getExecutionTime());
            result.addCompressionProperty("restore time, ms", restoreResult.getExecutionTime());
            result.addCompressionProperty("size, bytes", compressedSize);
            result.addCompressionProperty("ratio", Integer.valueOf(size).doubleValue() / compressedSize);
            result.setRestoredImage(restoreResult.getResult());
            result.setCompressedData(compressed);
        }
        return result;
    }

    private static int getWidth(Image image) {
        return Double.valueOf(image.getWidth()).intValue();
    }

    private static int getHeight(Image image) {
        return Double.valueOf(image.getHeight()).intValue();
    }

    private static int getSize(Image image) {
        return getWidth(image) * getHeight(image) * 3;
    }

    private static int getColorsCount(Image image) {
        PixelReader pixelReader = image.getPixelReader();
        Set<Integer> colors = new HashSet<>();
        IntStream.range(0, getHeight(image)).forEach(y ->
                IntStream.range(0, getWidth(image)).forEach(x -> {
                    colors.add(pixelReader.getArgb(x, y));
                })
        );
        return colors.size();
    }
}
