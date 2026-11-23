package com.fittrack.storage;

import com.fittrack.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Decodes uploaded images (never trusting the declared content type), downsizes them and
 * re-encodes as JPEG. Re-encoding strips all metadata, including EXIF GPS location.
 */
@Component
public class ImageProcessor {

    private static final int MAX_SOURCE_PIXELS = 40_000_000;

    public byte[] toJpeg(byte[] input, int maxDimension) {
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(input));
        } catch (IOException e) {
            source = null;
        }
        if (source == null) {
            throw new BadRequestException("Unsupported image. Please upload a JPEG or PNG file.");
        }
        if ((long) source.getWidth() * source.getHeight() > MAX_SOURCE_PIXELS) {
            throw new BadRequestException("Image dimensions are too large");
        }

        double scale = Math.min(1.0, (double) maxDimension / Math.max(source.getWidth(), source.getHeight()));
        int w = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE); // flatten PNG transparency
            g.fillRect(0, 0, w, h);
            g.drawImage(source, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f);
            writer.write(null, new IIOImage(out, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode image", e);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
