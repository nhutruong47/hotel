package com.hsf.hotel.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Lightweight image sanity-checks performed at upload time. No external
 * dependencies — uses the JDK's {@link ImageIO} only.
 *
 * <p>Goals:
 * <ul>
 *   <li>Cap absolute dimensions to bound the file size even when an attacker
 *       uploads a 50 000 × 50 000 PNG. (The 5 MB body-size cap is the first
 *       line of defence; this is the second.)</li>
 *   <li>Decode the image once to verify it isn't a malicious file renamed as
 *       an image.</li>
 *   <li>Re-encode everything as a JPEG (or PNG for transparency) so the bytes
 *       served to the browser cannot carry an EXIF / metadata side-channel
 *       embedded in the original.</li>
 * </ul>
 *
 * <p>Trade-off: the first request that needs a smaller variant should still
 * resize on demand; this helper exists to keep the upload step safe and to
 * keep the storage footprint bounded.
 */
public final class ImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);

    /** Width × height cap. Anything larger is rejected at upload time. */
    public static final int MAX_DIMENSION = 4096;

    private static final Set<String> LOSSY_FORMATS = Set.of("jpg", "jpeg");
    private static final Set<String> LOSSLESS_FORMATS = Set.of("png", "webp", "gif", "avif");

    private ImageProcessor() {}

    /** Returns true if the extension denotes an image we know how to re-encode. */
    public static boolean isSupported(String extension) {
        if (extension == null) return false;
        String e = extension.toLowerCase(Locale.ROOT);
        if (e.startsWith(".")) {
            e = e.substring(1);
        }
        return LOSSY_FORMATS.contains(e) || LOSSLESS_FORMATS.contains(e);
    }

    /**
     * Decode the image and re-encode it. Throws if the bytes aren't a real
     * image, or if the dimensions exceed {@link #MAX_DIMENSION}.
     */
    public static byte[] sanitize(InputStream in, String originalExtension) throws IOException {
        if (in == null) {
            throw new IOException("Input stream is null");
        }
        BufferedImage src;
        try {
            src = ImageIO.read(in);
        } catch (IOException ex) {
            throw new IOException("Cannot decode image", ex);
        }
        if (src == null) {
            throw new IOException("Unsupported image format");
        }
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) {
            throw new IOException("Empty image");
        }
        if (w > MAX_DIMENSION || h > MAX_DIMENSION) {
            throw new IOException("Image dimensions exceed " + MAX_DIMENSION
                    + "x" + MAX_DIMENSION + " (got " + w + "x" + h + ")");
        }
        String fmt = pickFormat(originalExtension, src);
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        boolean wrote = ImageIO.write(src, fmt, out);
        if (!wrote) {
            // Fallback to PNG — ImageIO always has a PNG encoder.
            out.reset();
            wrote = ImageIO.write(src, "png", out);
            fmt = "png";
        }
        if (!wrote) {
            throw new IOException("Failed to re-encode image");
        }
        log.debug("Re-encoded upload as {} ({}x{})", fmt, w, h);
        return out.toByteArray();
    }

    /**
     * Produce a smaller variant if needed. Returns the original bytes when no
     * scaling is required.
     */
    public static byte[] downscale(byte[] bytes, String extension, int maxEdge) throws IOException {
        if (bytes == null || bytes.length == 0) return bytes;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) return bytes;
            int w = src.getWidth();
            int h = src.getHeight();
            int longest = Math.max(w, h);
            if (longest <= maxEdge) {
                return bytes;
            }
            double scale = (double) maxEdge / longest;
            int targetW = (int) Math.round(w * scale);
            int targetH = (int) Math.round(h * scale);
            BufferedImage dst = new BufferedImage(targetW, targetH,
                    src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB
                                                  : BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dst.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(src, 0, 0, targetW, targetH, null);
            } finally {
                g.dispose();
            }
            String fmt = pickFormat(extension, src);
            ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
            ImageIO.write(dst, fmt, out);
            return out.toByteArray();
        }
    }

    private static String pickFormat(String originalExtension, BufferedImage src) {
        String e = originalExtension == null ? "" : originalExtension.toLowerCase(Locale.ROOT);
        if (e.startsWith(".")) {
            e = e.substring(1);
        }
        if (LOSSY_FORMATS.contains(e)) return "jpg";
        if (LOSSLESS_FORMATS.contains(e)) {
            // PNG keeps transparency. Webp/gif/avif all re-encode fine, but
            // JPEG/PNG have the most predictable support across browsers.
            return src.getColorModel().hasAlpha() ? "png" : "jpg";
        }
        return "jpg";
    }
}
