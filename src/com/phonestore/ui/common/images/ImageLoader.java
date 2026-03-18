package com.phonestore.ui.common.images;

import com.phonestore.constant.PathConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageLoader {

    private static Icon defaultNoImage;

    private static final String[] ICON_BASE_PATHS = {
        PathConstants.RES_ICONS,
        "/com/phonestore/resources/images/icons"
    };

    private static final String[] DEFAULT_IMAGE_BASE_PATHS = {
        PathConstants.RES_DEFAULT,
        "/com/phonestore/resources/images/default"
    };

    private ImageLoader() {}

    /**
     * An ImageIcon that paints a higher-resolution backing image into a smaller logical size.
     * This avoids blurry icons on HiDPI screens when the UI scale factor is > 1.
     */
    private static final class HiDpiImageIcon extends ImageIcon {
        private final int logicalWidth;
        private final int logicalHeight;

        private HiDpiImageIcon(Image image, int logicalWidth, int logicalHeight) {
            super(image);
            this.logicalWidth = Math.max(1, logicalWidth);
            this.logicalHeight = Math.max(1, logicalHeight);
        }

        @Override
        public int getIconWidth() {
            return logicalWidth;
        }

        @Override
        public int getIconHeight() {
            return logicalHeight;
        }

        @Override
        public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
            Image img = getImage();
            if (img == null) {
                super.paintIcon(c, g, x, y);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            applyQualityHints(g2);
            g2.drawImage(img, x, y, logicalWidth, logicalHeight, c);
            g2.dispose();
        }
    }

    public static void preloadDefaults() {
        if (defaultNoImage == null) {
            defaultNoImage = loadFirstResourceIcon(DEFAULT_IMAGE_BASE_PATHS, "no-image.png", 120, 90);
            if (defaultNoImage == null) {
                defaultNoImage = createPlaceholder(120, 90);
            }
        }
    }

    public static Icon getDefaultNoImage(int width, int height) {
        preloadDefaults();
        return scale(defaultNoImage, width, height);
    }

    public static Icon loadIcon(String iconFileName, int width, int height) {
        Icon icon = loadFirstResourceIcon(ICON_BASE_PATHS, iconFileName, width, height);
        if (icon != null) return icon;
        return createPlaceholder(width, height);
    }

    public static Icon loadDefaultImage(String fileName, int width, int height) {
        Icon icon = loadFirstResourceIcon(DEFAULT_IMAGE_BASE_PATHS, fileName, width, height);
        if (icon != null) return icon;
        return getDefaultNoImage(width, height);
    }

    private static Icon loadFirstResourceIcon(String[] basePaths, String fileName, int width, int height) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        // If caller already passed an absolute classpath, try it first.
        if (fileName.startsWith("/")) {
            Icon abs = loadResourceIcon(fileName, width, height);
            if (abs != null) return abs;
        }

        for (String base : basePaths) {
            Icon icon = loadResourceIcon(base + "/" + fileName, width, height);
            if (icon != null) {
                return icon;
            }

            // If caller passed a .png but only .svg exists, try svg automatically.
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png")) {
                String svg = fileName.substring(0, fileName.length() - 4) + ".svg";
                Icon svgIcon = loadResourceIcon(base + "/" + svg, width, height);
                if (svgIcon != null) {
                    return svgIcon;
                }
            }
        }

        return null;
    }

    public static Icon loadImageFromDisk(String path, int width, int height) {
        if (path == null || path.isBlank()) {
            return getDefaultNoImage(width, height);
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return getDefaultNoImage(width, height);
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return getDefaultNoImage(width, height);
            }
            return toHiDpiIcon(image, width, height);
        } catch (IOException e) {
            return getDefaultNoImage(width, height);
        }
    }

    /**
     * Load a default image but scale to fit within the box while preserving aspect ratio.
     * Useful for large illustrations (e.g. login screen) to avoid stretching.
     */
    public static Icon loadDefaultImageFit(String fileName, int maxWidth, int maxHeight) {
        Icon icon = loadFirstResourceIconFit(DEFAULT_IMAGE_BASE_PATHS, fileName, maxWidth, maxHeight);
        if (icon != null) return icon;
        return getDefaultNoImage(Math.max(1, maxWidth), Math.max(1, maxHeight));
    }

    private static Icon loadResourceIcon(String classpath, int width, int height) {
        if (classpath == null || classpath.isBlank()) {
            return null;
        }

        String lower = classpath.toLowerCase();
        if (lower.endsWith(".svg")) {
            return loadSvgResourceIcon(classpath, width, height);
        }

        try (InputStream in = ImageLoader.class.getResourceAsStream(classpath)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    return toHiDpiIcon(image, width, height);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }

        // Fallback for dev runs where resources directory isn't on runtime classpath.
        return loadFromFileSystem(classpath, width, height);
    }

    /**
     * Optional SVG support via Apache Batik.
     * If Batik is not present on classpath, returns null (caller will fallback).
     */
    private static Icon loadSvgResourceIcon(String classpath, int width, int height) {
        // Prefer FlatLaf's SVG loader (if present) because it doesn't require Batik.
        Icon flat = loadSvgWithFlatLaf(classpath, width, height);
        if (flat != null) {
            return flat;
        }

        try (InputStream in = ImageLoader.class.getResourceAsStream(classpath)) {
            if (in != null) {
                Dimension px = scaledPixels(width, height);
                byte[] pngBytes = transcodeSvgToPngBytes(in, px.width, px.height);
                if (pngBytes != null && pngBytes.length > 0) {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
                    if (image != null) {
                        return wrapHiDpi(image, width, height);
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through
        }

        // Fallback for dev runs where resources directory isn't on runtime classpath.
        return loadFromFileSystem(classpath, width, height);
    }

    private static Icon loadFromFileSystem(String classpath, int width, int height) {
        File f = resolveDevFile(classpath);
        if (f == null || !f.exists() || !f.isFile()) {
            return null;
        }

        String lower = f.getName().toLowerCase();
        if (lower.endsWith(".svg")) {
            // Try FlatLaf SVG icon from file URL.
            Icon byUrl = loadSvgWithFlatLafUrl(f, width, height);
            if (byUrl != null) {
                return byUrl;
            }

            // Try Batik transcode from file stream if available.
            try (InputStream in = new FileInputStream(f)) {
                Dimension px = scaledPixels(width, height);
                byte[] pngBytes = transcodeSvgToPngBytes(in, px.width, px.height);
                if (pngBytes == null || pngBytes.length == 0) {
                    return null;
                }
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
                if (image == null) {
                    return null;
                }
                return wrapHiDpi(image, width, height);
            } catch (Exception ignored) {
                return null;
            }
        }

        // Raster images
        try {
            BufferedImage image = ImageIO.read(f);
            if (image == null) {
                return null;
            }
            return toHiDpiIcon(image, width, height);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Icon loadFirstResourceIconFit(String[] basePaths, String fileName, int maxWidth, int maxHeight) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        if (fileName.startsWith("/")) {
            Icon abs = loadResourceIconFit(fileName, maxWidth, maxHeight);
            if (abs != null) return abs;
        }

        for (String base : basePaths) {
            Icon icon = loadResourceIconFit(base + "/" + fileName, maxWidth, maxHeight);
            if (icon != null) {
                return icon;
            }

            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png")) {
                String svg = fileName.substring(0, fileName.length() - 4) + ".svg";
                Icon svgIcon = loadResourceIconFit(base + "/" + svg, maxWidth, maxHeight);
                if (svgIcon != null) {
                    return svgIcon;
                }
            }
        }

        return null;
    }

    private static Icon loadResourceIconFit(String classpath, int maxWidth, int maxHeight) {
        if (classpath == null || classpath.isBlank()) {
            return null;
        }

        String lower = classpath.toLowerCase();
        if (lower.endsWith(".svg")) {
            Icon svg = loadSvgResourceIconFit(classpath, maxWidth, maxHeight);
            if (svg != null) return svg;
        }

        try (InputStream in = ImageLoader.class.getResourceAsStream(classpath)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    Dimension d = fitSize(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    return toHiDpiIcon(image, d.width, d.height);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }

        File f = resolveDevFile(classpath);
        if (f == null || !f.exists() || !f.isFile()) {
            return null;
        }
        if (f.getName().toLowerCase().endsWith(".svg")) {
            Dimension intrinsic = null;
            try {
                intrinsic = readSvgIntrinsicSize(f.toURI().toURL());
            } catch (Exception ignored) {
                // ignore
            }
            Dimension d = intrinsic != null ? fitSize(intrinsic.width, intrinsic.height, maxWidth, maxHeight)
                    : new Dimension(Math.max(1, maxWidth), Math.max(1, maxHeight));

            Icon byUrl = loadSvgWithFlatLafUrlFit(f, d.width, d.height);
            if (byUrl != null) return byUrl;
            return loadFromFileSystem(classpath, d.width, d.height);
        }
        try {
            BufferedImage image = ImageIO.read(f);
            if (image == null) return null;
            Dimension d = fitSize(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
            return toHiDpiIcon(image, d.width, d.height);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Icon loadSvgResourceIconFit(String classpath, int maxWidth, int maxHeight) {
        Icon flat = loadSvgWithFlatLafFit(classpath, maxWidth, maxHeight);
        if (flat != null) return flat;

        // Fallback: preserve aspect ratio even when using Batik transcode.
        try {
            java.net.URL url = ImageLoader.class.getResource(classpath);
            Dimension intrinsic = readSvgIntrinsicSize(url);
            if (intrinsic != null) {
                Dimension d = fitSize(intrinsic.width, intrinsic.height, maxWidth, maxHeight);
                return loadSvgResourceIcon(classpath, d.width, d.height);
            }
        } catch (Exception ignored) {
            // ignore
        }

        return loadSvgResourceIcon(classpath, maxWidth, maxHeight);
    }

    private static File resolveDevFile(String classpath) {
        if (classpath == null || classpath.isBlank()) {
            return null;
        }

        String p = classpath.startsWith("/") ? classpath.substring(1) : classpath;

        // If classpath is "images/...", try "./resources/images/..." first.
        File f1 = new File("resources", p);
        if (f1.isFile()) return f1;

        // If classpath is "com/phonestore/resources/images/...", try "./src/com/phonestore/resources/images/...".
        File f2 = new File("src", p);
        if (f2.isFile()) return f2;

        // Also try when base path is omitted and caller passes just file name.
        File f3 = new File(p);
        if (f3.isFile()) return f3;

        return null;
    }

    private static Icon loadSvgWithFlatLafUrl(File file, int width, int height) {
        try {
            Class<?> flatSvgIconClass = Class.forName("com.formdev.flatlaf.extras.FlatSVGIcon");
            java.net.URL url = file.toURI().toURL();

            Object icon;
            try {
                icon = flatSvgIconClass.getConstructor(java.net.URL.class).newInstance(url);
            } catch (NoSuchMethodException ignored) {
                icon = flatSvgIconClass.getConstructor(String.class).newInstance(url.toString());
            }

            try {
                icon = flatSvgIconClass.getMethod("derive", int.class, int.class)
                        .invoke(icon, Math.max(1, width), Math.max(1, height));
            } catch (Exception ignored) {
                // ignore
            }

            if (icon instanceof Icon i) {
                return i;
            }

            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Icon loadSvgWithFlatLaf(String classpath, int width, int height) {
        try {
            Class<?> flatSvgIconClass = Class.forName("com.formdev.flatlaf.extras.FlatSVGIcon");
            java.net.URL url = ImageLoader.class.getResource(classpath);
            if (url == null) {
                return null;
            }

            Object icon;
            try {
                // FlatSVGIcon(URL)
                icon = flatSvgIconClass.getConstructor(java.net.URL.class).newInstance(url);
            } catch (NoSuchMethodException ignored) {
                // FlatSVGIcon(String)
                icon = flatSvgIconClass.getConstructor(String.class).newInstance(classpath);
            }

            // Use vector icon at the requested logical size.
            try {
                icon = flatSvgIconClass.getMethod("derive", int.class, int.class)
                        .invoke(icon, Math.max(1, width), Math.max(1, height));
            } catch (Exception ignored) {
                // Ignore; icon will be scaled by Swing later.
            }

            if (icon instanceof Icon i) {
                return i;
            }

            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] transcodeSvgToPngBytes(InputStream svgInput, int width, int height) {
        try {
            Class<?> pngTranscoderClass = Class.forName("org.apache.batik.transcoder.image.PNGTranscoder");
            Class<?> transcoderInputClass = Class.forName("org.apache.batik.transcoder.TranscoderInput");
            Class<?> transcoderOutputClass = Class.forName("org.apache.batik.transcoder.TranscoderOutput");
            Class<?> svgAbstractTranscoderClass = Class.forName("org.apache.batik.transcoder.SVGAbstractTranscoder");

            Object transcoder = pngTranscoderClass.getDeclaredConstructor().newInstance();

            Object keyWidth = svgAbstractTranscoderClass.getField("KEY_WIDTH").get(null);
            Object keyHeight = svgAbstractTranscoderClass.getField("KEY_HEIGHT").get(null);

            // addTranscodingHint(TranscodingHints.Key, Object)
            for (var m : pngTranscoderClass.getMethods()) {
                if ("addTranscodingHint".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(transcoder, keyWidth, (float) width);
                    m.invoke(transcoder, keyHeight, (float) height);
                    break;
                }
            }

            Object input = transcoderInputClass.getConstructor(InputStream.class).newInstance(svgInput);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Object output = transcoderOutputClass.getConstructor(java.io.OutputStream.class).newInstance(out);

            var transcode = pngTranscoderClass.getMethod("transcode", transcoderInputClass, transcoderOutputClass);
            transcode.invoke(transcoder, input, output);

            return out.toByteArray();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Icon scale(Icon icon, int width, int height) {
        if (icon == null) {
            return createPlaceholder(width, height);
        }

        if (icon instanceof ImageIcon ii && ii.getImage() != null) {
            BufferedImage src = toBufferedImage(ii.getImage());
            return toHiDpiIcon(src, width, height);
        }

        // If it's already a vector Icon (e.g. FlatSVGIcon), derive if possible.
        try {
            var m = icon.getClass().getMethod("derive", int.class, int.class);
            Object d = m.invoke(icon, Math.max(1, width), Math.max(1, height));
            if (d instanceof Icon derived) {
                return derived;
            }
        } catch (Exception ignored) {
            // ignore
        }

        return icon;
    }

    private static Icon loadSvgWithFlatLafFit(String classpath, int maxWidth, int maxHeight) {
        try {
            Class<?> flatSvgIconClass = Class.forName("com.formdev.flatlaf.extras.FlatSVGIcon");
            java.net.URL url = ImageLoader.class.getResource(classpath);
            if (url == null) {
                return null;
            }

            Object base;
            try {
                base = flatSvgIconClass.getConstructor(java.net.URL.class).newInstance(url);
            } catch (NoSuchMethodException ignored) {
                base = flatSvgIconClass.getConstructor(String.class).newInstance(classpath);
            }

            Dimension intrinsic = readSvgIntrinsicSize(url);
            int w0;
            int h0;
            if (intrinsic != null) {
                w0 = Math.max(1, intrinsic.width);
                h0 = Math.max(1, intrinsic.height);
            } else if (base instanceof Icon ii) {
                // Fallback: FlatSVGIcon default size may be 16x16, which can distort aspect-fit.
                w0 = Math.max(1, ii.getIconWidth());
                h0 = Math.max(1, ii.getIconHeight());
            } else {
                return null;
            }

            Dimension d = fitSize(w0, h0, maxWidth, maxHeight);
            Object derived = base;
            try {
                derived = flatSvgIconClass.getMethod("derive", int.class, int.class)
                        .invoke(base, d.width, d.height);
            } catch (Exception ignored) {
                // ignore
            }

            if (derived instanceof Icon i) {
                return i;
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Icon loadSvgWithFlatLafUrlFit(File file, int maxWidth, int maxHeight) {
        try {
            Class<?> flatSvgIconClass = Class.forName("com.formdev.flatlaf.extras.FlatSVGIcon");
            java.net.URL url = file.toURI().toURL();

            Object base;
            try {
                base = flatSvgIconClass.getConstructor(java.net.URL.class).newInstance(url);
            } catch (NoSuchMethodException ignored) {
                base = flatSvgIconClass.getConstructor(String.class).newInstance(url.toString());
            }

            Dimension intrinsic = readSvgIntrinsicSize(url);
            int w0;
            int h0;
            if (intrinsic != null) {
                w0 = Math.max(1, intrinsic.width);
                h0 = Math.max(1, intrinsic.height);
            } else if (base instanceof Icon ii) {
                w0 = Math.max(1, ii.getIconWidth());
                h0 = Math.max(1, ii.getIconHeight());
            } else {
                return null;
            }

            Dimension d = fitSize(w0, h0, maxWidth, maxHeight);
            Object derived = base;
            try {
                derived = flatSvgIconClass.getMethod("derive", int.class, int.class)
                        .invoke(base, d.width, d.height);
            } catch (Exception ignored) {
                // ignore
            }

            if (derived instanceof Icon i) {
                return i;
            }

            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static final Pattern SVG_VIEWBOX_PATTERN = Pattern.compile("viewBox\\s*=\\s*\"[^\\\"]*\\s([0-9]+(?:\\.[0-9]+)?)\\s+([0-9]+(?:\\.[0-9]+)?)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_WIDTH_PATTERN = Pattern.compile("width\\s*=\\s*\"([0-9]+(?:\\.[0-9]+)?)(px)?\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern SVG_HEIGHT_PATTERN = Pattern.compile("height\\s*=\\s*\"([0-9]+(?:\\.[0-9]+)?)(px)?\"", Pattern.CASE_INSENSITIVE);

    /**
     * Best-effort extraction of an SVG's intrinsic dimensions.
     * Prefer viewBox width/height, fall back to width/height attributes.
     */
    private static Dimension readSvgIntrinsicSize(java.net.URL url) {
        if (url == null) return null;
        try (InputStream in = url.openStream()) {
            byte[] buf = in.readNBytes(8192);
            if (buf == null || buf.length == 0) return null;
            String head = new String(buf, StandardCharsets.UTF_8);

            Matcher vb = SVG_VIEWBOX_PATTERN.matcher(head);
            if (vb.find()) {
                int w = (int) Math.round(Double.parseDouble(vb.group(1)));
                int h = (int) Math.round(Double.parseDouble(vb.group(2)));
                if (w > 0 && h > 0) return new Dimension(w, h);
            }

            Matcher mw = SVG_WIDTH_PATTERN.matcher(head);
            Matcher mh = SVG_HEIGHT_PATTERN.matcher(head);
            if (mw.find() && mh.find()) {
                int w = (int) Math.round(Double.parseDouble(mw.group(1)));
                int h = (int) Math.round(Double.parseDouble(mh.group(1)));
                if (w > 0 && h > 0) return new Dimension(w, h);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static Dimension fitSize(int srcW, int srcH, int maxW, int maxH) {
        int mw = Math.max(1, maxW);
        int mh = Math.max(1, maxH);
        int sw = Math.max(1, srcW);
        int sh = Math.max(1, srcH);

        double scale = Math.min((double) mw / sw, (double) mh / sh);
        int w = Math.max(1, (int) Math.round(sw * scale));
        int h = Math.max(1, (int) Math.round(sh * scale));
        return new Dimension(w, h);
    }

    private static void applyQualityHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static float getUiScaleFactor() {
        try {
            Class<?> uiScale = Class.forName("com.formdev.flatlaf.util.UIScale");
            try {
                Object v = uiScale.getMethod("getUserScaleFactor").invoke(null);
                if (v instanceof Number n) {
                    return Math.max(1f, n.floatValue());
                }
            } catch (NoSuchMethodException ignored) {
                // fall through
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return 1f;
    }

    private static Dimension scaledPixels(int logicalW, int logicalH) {
        float scale = getUiScaleFactor();
        int w = Math.max(1, Math.round(Math.max(1, logicalW) * scale));
        int h = Math.max(1, Math.round(Math.max(1, logicalH) * scale));
        return new Dimension(w, h);
    }

    private static ImageIcon wrapHiDpi(BufferedImage backing, int logicalW, int logicalH) {
        int lw = Math.max(1, logicalW);
        int lh = Math.max(1, logicalH);
        Dimension px = scaledPixels(lw, lh);

        BufferedImage img = backing;
        if (img.getWidth() != px.width || img.getHeight() != px.height) {
            img = scaleHighQuality(img, px.width, px.height);
        }

        if (px.width == lw && px.height == lh) {
            return new ImageIcon(img);
        }
        return new HiDpiImageIcon(img, lw, lh);
    }

    private static ImageIcon toHiDpiIcon(BufferedImage src, int logicalW, int logicalH) {
        int lw = Math.max(1, logicalW);
        int lh = Math.max(1, logicalH);
        Dimension px = scaledPixels(lw, lh);
        BufferedImage scaled = scaleHighQuality(src, px.width, px.height);
        return wrapHiDpi(scaled, lw, lh);
    }

    private static ImageIcon toHiDpiIcon(Icon icon, int logicalW, int logicalH) {
        int lw = Math.max(1, logicalW);
        int lh = Math.max(1, logicalH);
        Dimension px = scaledPixels(lw, lh);

        int iw = Math.max(1, icon.getIconWidth());
        int ih = Math.max(1, icon.getIconHeight());
        double sx = (double) px.width / iw;
        double sy = (double) px.height / ih;

        BufferedImage img = new BufferedImage(px.width, px.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        applyQualityHints(g);
        g.scale(sx, sy);
        icon.paintIcon(null, g, 0, 0);
        g.dispose();

        return wrapHiDpi(img, lw, lh);
    }

    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage bi) {
            return bi;
        }
        int w = Math.max(1, img.getWidth(null));
        int h = Math.max(1, img.getHeight(null));
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        applyQualityHints(g);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return b;
    }

    private static BufferedImage scaleHighQuality(BufferedImage src, int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (src.getWidth() == w && src.getHeight() == h) {
            return src;
        }
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        applyQualityHints(g);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    private static ImageIcon createPlaceholder(int width, int height) {
        BufferedImage img = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(230, 230, 230));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(new Color(160, 160, 160));
        g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        String text = "IMG";
        FontMetrics fm = g.getFontMetrics();
        int x = (img.getWidth() - fm.stringWidth(text)) / 2;
        int y = (img.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);
        g.dispose();
        return new ImageIcon(img);
    }
}
