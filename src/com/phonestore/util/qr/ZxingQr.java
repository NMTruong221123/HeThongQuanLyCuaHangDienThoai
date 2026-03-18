package com.phonestore.util.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

public final class ZxingQr {

    private ZxingQr() {}

    public static BufferedImage render(String text, int sizePx) {
        if (sizePx <= 0) sizePx = 300;
        String payload = text == null ? "" : text;

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix;
        try {
            matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
        } catch (WriterException e) {
            throw new IllegalStateException("Không tạo được QR", e);
        }

        return toImage(matrix);
    }

    private static BufferedImage toImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int on = Color.BLACK.getRGB();
        int off = Color.WHITE.getRGB();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, matrix.get(x, y) ? on : off);
            }
        }
        return img;
    }
}
