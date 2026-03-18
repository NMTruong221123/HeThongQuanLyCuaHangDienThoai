package com.phonestore.util.qr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal offline QR code generator for this app.
 *
 * Supports:
 * - Byte mode encoding (UTF-8 bytes)
 * - Versions 1..10
 * - Error correction level L
 *
 * This is intentionally small to avoid external dependencies.
 */
public final class OfflineQr {

    private OfflineQr() {}

    public static BufferedImage render(String text, int targetSizePx) {
        if (targetSizePx <= 0) targetSizePx = 300;
        QrMatrix m = encode(text == null ? "" : text);
        return renderMatrix(m.modules, targetSizePx);
    }

    private static BufferedImage renderMatrix(boolean[][] modules, int targetSizePx) {
        int n = modules.length;
        int quiet = 4;
        int total = n + quiet * 2;
        int scale = Math.max(1, targetSizePx / total);
        int imgSize = total * scale;

        BufferedImage img = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgSize, imgSize);

            g.setColor(Color.BLACK);
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    if (modules[y][x]) {
                        int px = (x + quiet) * scale;
                        int py = (y + quiet) * scale;
                        g.fillRect(px, py, scale, scale);
                    }
                }
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    private record QrMatrix(boolean[][] modules, boolean[][] isFunction, int version) {}

    private static QrMatrix encode(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        int version = chooseVersion(data);
        Spec spec = Spec.forVersion(version);

        byte[] dataCodewords = makeDataCodewords(data, version, spec.totalDataCodewords);
        byte[] finalCodewords = addEccAndInterleave(dataCodewords, spec);
        boolean[] finalBits = codewordsToBits(finalCodewords, remainderBits(version));

        QrMatrix base = buildBaseMatrix(version);
        boolean[][] modules = deepCopy(base.modules);
        boolean[][] isFunction = base.isFunction;

        placeDataBits(modules, isFunction, finalBits);

        // Choose the best mask for scan reliability.
        int bestMask = 0;
        int bestPenalty = Integer.MAX_VALUE;
        boolean[][] best = null;
        for (int mask = 0; mask < 8; mask++) {
            boolean[][] trial = deepCopy(modules);
            applyMask(trial, isFunction, mask);
            drawFormatBits(trial, isFunction, mask);
            if (version >= 7) {
                drawVersionBits(trial, isFunction, version);
            }
            int penalty = calcPenaltyScore(trial);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                bestMask = mask;
                best = trial;
            }
        }

        if (best != null) {
            modules = best;
        } else {
            applyMask(modules, isFunction, bestMask);
            drawFormatBits(modules, isFunction, bestMask);
            if (version >= 7) {
                drawVersionBits(modules, isFunction, version);
            }
        }

        return new QrMatrix(modules, isFunction, version);
    }

    private static boolean[][] deepCopy(boolean[][] src) {
        boolean[][] dst = new boolean[src.length][];
        for (int i = 0; i < src.length; i++) dst[i] = Arrays.copyOf(src[i], src[i].length);
        return dst;
    }

    // ------------------------- Version / ECC specs -------------------------

    private record Spec(
            int version,
            int totalDataCodewords,
            int eccCodewordsPerBlock,
            int group1Blocks,
            int group1DataCodewords,
            int group2Blocks,
            int group2DataCodewords
    ) {
        static Spec forVersion(int v) {
            // Values are for Error Correction Level L.
            // Source: common QR spec tables.
            return switch (v) {
                case 1 -> new Spec(1, 19, 7, 1, 19, 0, 0);
                case 2 -> new Spec(2, 34, 10, 1, 34, 0, 0);
                case 3 -> new Spec(3, 55, 15, 1, 55, 0, 0);
                case 4 -> new Spec(4, 80, 20, 1, 80, 0, 0);
                case 5 -> new Spec(5, 108, 26, 1, 108, 0, 0);
                case 6 -> new Spec(6, 136, 18, 2, 68, 0, 0);
                case 7 -> new Spec(7, 156, 20, 2, 78, 0, 0);
                case 8 -> new Spec(8, 194, 24, 2, 97, 0, 0);
                case 9 -> new Spec(9, 232, 30, 2, 116, 0, 0);
                case 10 -> new Spec(10, 274, 18, 2, 68, 2, 69);
                default -> throw new IllegalArgumentException("Unsupported QR version: " + v);
            };
        }
    }

    private static int chooseVersion(byte[] data) {
        // Byte mode overhead:
        // - mode: 4 bits
        // - count: 8 bits (v1-9) or 16 bits (v10)
        // - data: 8*len bits
        int len = data == null ? 0 : data.length;
        for (int v = 1; v <= 10; v++) {
            Spec s = Spec.forVersion(v);
            int countBits = (v <= 9) ? 8 : 16;
            int neededBits = 4 + countBits + (len * 8);
            int capBits = s.totalDataCodewords * 8;
            if (neededBits <= capBits) return v;
        }
        throw new IllegalArgumentException("Dữ liệu QR quá dài (hỗ trợ tối đa version 10)");
    }

    private static int remainderBits(int version) {
        return switch (version) {
            case 1 -> 0;
            case 2, 3, 4, 5, 6 -> 7;
            case 7, 8, 9, 10 -> 0;
            default -> 0;
        };
    }

    // ------------------------- Data encoding -------------------------

    private static byte[] makeDataCodewords(byte[] dataBytes, int version, int totalDataCodewords) {
        BitBuffer bb = new BitBuffer(totalDataCodewords * 8);
        int len = dataBytes == null ? 0 : dataBytes.length;

        // Mode indicator: byte mode
        bb.appendBits(0b0100, 4);

        int countBits = (version <= 9) ? 8 : 16;
        bb.appendBits(len, countBits);

        for (int i = 0; i < len; i++) {
            bb.appendBits(dataBytes[i] & 0xFF, 8);
        }

        int capBits = totalDataCodewords * 8;
        int remaining = capBits - bb.size();
        if (remaining < 0) {
            throw new IllegalArgumentException("Dữ liệu QR quá dài cho version " + version);
        }

        // Terminator (up to 4 zeros)
        int terminator = Math.min(4, remaining);
        bb.appendBits(0, terminator);

        // Pad to byte
        while ((bb.size() % 8) != 0) bb.appendBits(0, 1);

        // Pad bytes
        int padByte0 = 0xEC;
        int padByte1 = 0x11;
        boolean toggle = false;
        while ((bb.size() / 8) < totalDataCodewords) {
            bb.appendBits(toggle ? padByte1 : padByte0, 8);
            toggle = !toggle;
        }

        byte[] out = new byte[totalDataCodewords];
        for (int i = 0; i < totalDataCodewords; i++) {
            out[i] = (byte) bb.getByte(i);
        }
        return out;
    }

    private static boolean[] codewordsToBits(byte[] codewords, int remainderBits) {
        int cwLen = codewords == null ? 0 : codewords.length;
        boolean[] bits = new boolean[cwLen * 8 + remainderBits];
        int k = 0;
        for (int i = 0; i < cwLen; i++) {
            int b = codewords[i] & 0xFF;
            for (int j = 7; j >= 0; j--) {
                bits[k++] = ((b >>> j) & 1) != 0;
            }
        }
        // remainder bits are zeros by default
        return bits;
    }

    private static final class BitBuffer {
        private final List<Boolean> bits = new ArrayList<>();
        private final int cap;

        BitBuffer(int capBits) {
            this.cap = Math.max(0, capBits);
        }

        void appendBits(int value, int length) {
            if (length <= 0) return;
            for (int i = length - 1; i >= 0; i--) {
                if (bits.size() >= cap) break;
                bits.add(((value >>> i) & 1) != 0);
            }
        }

        int size() {
            return bits.size();
        }

        int getByte(int byteIndex) {
            int start = byteIndex * 8;
            int v = 0;
            for (int i = 0; i < 8; i++) {
                v <<= 1;
                if (start + i < bits.size() && bits.get(start + i)) v |= 1;
            }
            return v;
        }
    }

    // ------------------------- ECC / Reed-Solomon -------------------------

    private static byte[] addEccAndInterleave(byte[] dataCodewords, Spec spec) {
        List<byte[]> dataBlocks = new ArrayList<>();
        int off = 0;

        for (int i = 0; i < spec.group1Blocks; i++) {
            byte[] blk = Arrays.copyOfRange(dataCodewords, off, off + spec.group1DataCodewords);
            dataBlocks.add(blk);
            off += spec.group1DataCodewords;
        }
        for (int i = 0; i < spec.group2Blocks; i++) {
            byte[] blk = Arrays.copyOfRange(dataCodewords, off, off + spec.group2DataCodewords);
            dataBlocks.add(blk);
            off += spec.group2DataCodewords;
        }

        List<byte[]> eccBlocks = new ArrayList<>();
        for (byte[] blk : dataBlocks) {
            eccBlocks.add(rsComputeRemainder(blk, spec.eccCodewordsPerBlock));
        }

        int maxDataLen = 0;
        for (byte[] blk : dataBlocks) maxDataLen = Math.max(maxDataLen, blk.length);

        int totalBlocks = dataBlocks.size();
        int totalEcc = totalBlocks * spec.eccCodewordsPerBlock;
        int total = spec.totalDataCodewords + totalEcc;
        byte[] out = new byte[total];
        int p = 0;

        // Interleave data
        for (int i = 0; i < maxDataLen; i++) {
            for (byte[] blk : dataBlocks) {
                if (i < blk.length) out[p++] = blk[i];
            }
        }

        // Interleave ECC
        for (int i = 0; i < spec.eccCodewordsPerBlock; i++) {
            for (byte[] eb : eccBlocks) {
                out[p++] = eb[i];
            }
        }

        return out;
    }

    private static final class Gf256 {
        static final int PRIMITIVE = 0x11D;
        static final int[] EXP = new int[512];
        static final int[] LOG = new int[256];

        static {
            int x = 1;
            for (int i = 0; i < 255; i++) {
                EXP[i] = x;
                LOG[x] = i;
                x <<= 1;
                if ((x & 0x100) != 0) x ^= PRIMITIVE;
            }
            for (int i = 255; i < 512; i++) EXP[i] = EXP[i - 255];
        }

        static int mul(int a, int b) {
            if (a == 0 || b == 0) return 0;
            return EXP[LOG[a] + LOG[b]];
        }
    }

    private static byte[] rsComputeRemainder(byte[] data, int eccLen) {
        if (eccLen <= 0) return new byte[0];
        int[] gen = rsGeneratorPoly(eccLen); // length eccLen+1, gen[0]=1
        int[] ecc = new int[eccLen];

        for (byte b : data) {
            int factor = (b & 0xFF) ^ ecc[0];
            System.arraycopy(ecc, 1, ecc, 0, eccLen - 1);
            ecc[eccLen - 1] = 0;
            for (int j = 0; j < eccLen; j++) {
                ecc[j] ^= Gf256.mul(gen[j + 1], factor);
            }
        }

        byte[] out = new byte[eccLen];
        for (int i = 0; i < eccLen; i++) out[i] = (byte) ecc[i];
        return out;
    }

    private static int[] rsGeneratorPoly(int degree) {
        int[] poly = new int[] {1};
        for (int i = 0; i < degree; i++) {
            int[] next = new int[poly.length + 1];
            for (int j = 0; j < poly.length; j++) {
                next[j] ^= poly[j];
                next[j + 1] ^= Gf256.mul(poly[j], Gf256.EXP[i]);
            }
            poly = next;
        }
        return poly;
    }

    // ------------------------- Matrix construction -------------------------

    private static QrMatrix buildBaseMatrix(int version) {
        int size = 17 + 4 * version;
        boolean[][] modules = new boolean[size][size];
        boolean[][] isFunction = new boolean[size][size];

        // Finder patterns
        drawFinder(modules, isFunction, 0, 0);
        drawFinder(modules, isFunction, size - 7, 0);
        drawFinder(modules, isFunction, 0, size - 7);

        // Separators
        drawSeparator(isFunction, 0, 0);
        drawSeparator(isFunction, size - 7, 0);
        drawSeparator(isFunction, 0, size - 7);

        // Timing patterns
        for (int i = 8; i < size - 8; i++) {
            boolean dark = (i % 2) == 0;
            setFunction(modules, isFunction, 6, i, dark);
            setFunction(modules, isFunction, i, 6, dark);
        }

        // Alignment patterns
        int[] align = alignmentCenters(version);
        if (align.length > 0) {
            for (int y : align) {
                for (int x : align) {
                    // Skip if overlaps finder
                    if ((x == 6 && y == 6) || (x == 6 && y == size - 7) || (x == size - 7 && y == 6)) continue;
                    drawAlignment(modules, isFunction, x, y);
                }
            }
        }

        // Reserve format info areas
        for (int i = 0; i < 9; i++) {
            if (i != 6) {
                isFunction[8][i] = true;
                isFunction[i][8] = true;
            }
        }
        for (int i = size - 8; i < size; i++) {
            isFunction[8][i] = true;
            isFunction[i][8] = true;
        }
        isFunction[8][8] = true;

        // Dark module
        setFunction(modules, isFunction, 8, 4 * version + 9, true);

        // Version info areas reserved (values drawn later)
        if (version >= 7) {
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 3; j++) {
                    isFunction[size - 11 + j][i] = true;
                    isFunction[i][size - 11 + j] = true;
                }
            }
        }

        return new QrMatrix(modules, isFunction, version);
    }

    private static void setFunction(boolean[][] modules, boolean[][] isFunction, int x, int y, boolean dark) {
        modules[y][x] = dark;
        isFunction[y][x] = true;
    }

    private static void drawFinder(boolean[][] modules, boolean[][] isFunction, int x, int y) {
        for (int dy = -1; dy <= 7; dy++) {
            for (int dx = -1; dx <= 7; dx++) {
                int xx = x + dx;
                int yy = y + dy;
                if (xx < 0 || yy < 0 || xx >= modules.length || yy >= modules.length) continue;
                boolean dark = (dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6)
                        && (dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4));
                modules[yy][xx] = dark;
                isFunction[yy][xx] = true;
            }
        }
    }

    private static void drawSeparator(boolean[][] isFunction, int x, int y) {
        int size = isFunction.length;
        for (int i = -1; i <= 7; i++) {
            mark(isFunction, x - 1, y + i);
            mark(isFunction, x + 7, y + i);
            mark(isFunction, x + i, y - 1);
            mark(isFunction, x + i, y + 7);
        }
        // keep inside bounds
        for (int i = 0; i < size; i++) {
            // noop; marking does bounds check
        }
    }

    private static void mark(boolean[][] isFunction, int x, int y) {
        if (x < 0 || y < 0 || x >= isFunction.length || y >= isFunction.length) return;
        isFunction[y][x] = true;
    }

    private static void drawAlignment(boolean[][] modules, boolean[][] isFunction, int cx, int cy) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int x = cx + dx;
                int y = cy + dy;
                boolean dark = Math.max(Math.abs(dx), Math.abs(dy)) != 1;
                setFunction(modules, isFunction, x, y, dark);
            }
        }
    }

    private static int[] alignmentCenters(int version) {
        return switch (version) {
            case 1 -> new int[0];
            case 2 -> new int[] {6, 18};
            case 3 -> new int[] {6, 22};
            case 4 -> new int[] {6, 26};
            case 5 -> new int[] {6, 30};
            case 6 -> new int[] {6, 34};
            case 7 -> new int[] {6, 22, 38};
            case 8 -> new int[] {6, 24, 42};
            case 9 -> new int[] {6, 26, 46};
            case 10 -> new int[] {6, 28, 50};
            default -> new int[0];
        };
    }

    private static void placeDataBits(boolean[][] modules, boolean[][] isFunction, boolean[] dataBits) {
        int size = modules.length;
        int bitIndex = 0;
        int dir = -1;
        for (int x = size - 1; x >= 1; x -= 2) {
            if (x == 6) x--; // skip timing column
            for (int y = (dir == -1 ? size - 1 : 0); (dir == -1 ? y >= 0 : y < size); y += dir) {
                for (int dx = 0; dx < 2; dx++) {
                    int xx = x - dx;
                    if (isFunction[y][xx]) continue;
                    boolean bit = bitIndex < dataBits.length && dataBits[bitIndex];
                    modules[y][xx] = bit;
                    bitIndex++;
                }
            }
            dir = -dir;
        }
    }

    private static void applyMask(boolean[][] modules, boolean[][] isFunction, int mask) {
        int size = modules.length;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (isFunction[y][x]) continue;
                boolean invert = switch (mask) {
                    case 0 -> ((x + y) % 2) == 0;
                    case 1 -> (y % 2) == 0;
                    case 2 -> (x % 3) == 0;
                    case 3 -> ((x + y) % 3) == 0;
                    case 4 -> (((y / 2) + (x / 3)) % 2) == 0;
                    case 5 -> ((x * y) % 2 + (x * y) % 3) == 0;
                    case 6 -> ((((x * y) % 2) + ((x * y) % 3)) % 2) == 0;
                    case 7 -> ((((x + y) % 2) + ((x * y) % 3)) % 2) == 0;
                    default -> ((x + y) % 2) == 0;
                };
                if (invert) modules[y][x] = !modules[y][x];
            }
        }
    }

    private static int calcPenaltyScore(boolean[][] m) {
        int size = m.length;
        int penalty = 0;

        // N1: Adjacent modules in row/column in same color
        for (int y = 0; y < size; y++) {
            int runColor = m[y][0] ? 1 : 0;
            int runLen = 1;
            for (int x = 1; x < size; x++) {
                int c = m[y][x] ? 1 : 0;
                if (c == runColor) runLen++;
                else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5);
                    runColor = c;
                    runLen = 1;
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5);
        }
        for (int x = 0; x < size; x++) {
            int runColor = m[0][x] ? 1 : 0;
            int runLen = 1;
            for (int y = 1; y < size; y++) {
                int c = m[y][x] ? 1 : 0;
                if (c == runColor) runLen++;
                else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5);
                    runColor = c;
                    runLen = 1;
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5);
        }

        // N2: 2x2 blocks of same color
        for (int y = 0; y < size - 1; y++) {
            for (int x = 0; x < size - 1; x++) {
                boolean c = m[y][x];
                if (m[y][x + 1] == c && m[y + 1][x] == c && m[y + 1][x + 1] == c) penalty += 3;
            }
        }

        // N3: Finder-like patterns in rows/cols
        int[] pat1 = {1,0,1,1,1,0,1,0,0,0,0};
        int[] pat2 = {0,0,0,0,1,0,1,1,1,0,1};
        for (int y = 0; y < size; y++) {
            for (int x = 0; x <= size - 11; x++) {
                if (matchesPatternRow(m, y, x, pat1) || matchesPatternRow(m, y, x, pat2)) penalty += 40;
            }
        }
        for (int x = 0; x < size; x++) {
            for (int y = 0; y <= size - 11; y++) {
                if (matchesPatternCol(m, x, y, pat1) || matchesPatternCol(m, x, y, pat2)) penalty += 40;
            }
        }

        // N4: Balance of dark modules
        int dark = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (m[y][x]) dark++;
            }
        }
        int total = size * size;
        int percent = (dark * 100) / total;
        int k = Math.abs(percent - 50) / 5;
        penalty += k * 10;

        return penalty;
    }

    private static boolean matchesPatternRow(boolean[][] m, int row, int startCol, int[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            boolean bit = pattern[i] == 1;
            if (m[row][startCol + i] != bit) return false;
        }
        return true;
    }

    private static boolean matchesPatternCol(boolean[][] m, int col, int startRow, int[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            boolean bit = pattern[i] == 1;
            if (m[startRow + i][col] != bit) return false;
        }
        return true;
    }

    // ------------------------- Format / version bits -------------------------

    private static void drawFormatBits(boolean[][] modules, boolean[][] isFunction, int mask) {
        int format = calcFormatBits(mask);
        int size = modules.length;

        // Coordinates for bits 14..0 (MSB..LSB)
        int[][] a = {
                {8, 0}, {8, 1}, {8, 2}, {8, 3}, {8, 4}, {8, 5},
                {8, 7}, {8, 8}, {7, 8}, {5, 8}, {4, 8}, {3, 8}, {2, 8}, {1, 8}, {0, 8}
        };
        int[][] b = {
                {size - 1, 8}, {size - 2, 8}, {size - 3, 8}, {size - 4, 8}, {size - 5, 8}, {size - 6, 8},
                {size - 7, 8},
                {8, size - 8}, {8, size - 7}, {8, size - 6}, {8, size - 5}, {8, size - 4}, {8, size - 3}, {8, size - 2}, {8, size - 1}
        };

        for (int i = 0; i < 15; i++) {
            boolean bit = ((format >>> (14 - i)) & 1) != 0;
            int ax = a[i][0], ay = a[i][1];
            modules[ay][ax] = bit;
            isFunction[ay][ax] = true;

            int bx = b[i][0], by = b[i][1];
            modules[by][bx] = bit;
            isFunction[by][bx] = true;
        }
    }

    private static int calcFormatBits(int mask) {
        // EC level L => 01
        int ecBits = 0b01;
        int data = (ecBits << 3) | (mask & 0b111); // 5 bits
        int rem = bchRemainder(data << 10, 0b10100110111);
        int format = ((data << 10) | rem) ^ 0b101010000010010;
        return format & 0x7FFF;
    }

    private static void drawVersionBits(boolean[][] modules, boolean[][] isFunction, int version) {
        int size = modules.length;
        int bits = calcVersionBits(version);

        // Place bits (0 is LSB)
        for (int i = 0; i < 18; i++) {
            boolean bit = ((bits >>> i) & 1) != 0;
            int aRow = size - 11 + (i % 3);
            int aCol = i / 3;
            modules[aRow][aCol] = bit;
            isFunction[aRow][aCol] = true;

            int bRow = i / 3;
            int bCol = size - 11 + (i % 3);
            modules[bRow][bCol] = bit;
            isFunction[bRow][bCol] = true;
        }
    }

    private static int calcVersionBits(int version) {
        int data = version << 12;
        int rem = bchRemainder(data, 0b1111100100101);
        return (version << 12) | rem;
    }

    private static int bchRemainder(int value, int poly) {
        int msbPoly = 31 - Integer.numberOfLeadingZeros(poly);
        while (Integer.numberOfLeadingZeros(value) <= 31 - msbPoly) {
            int shift = (31 - Integer.numberOfLeadingZeros(value)) - msbPoly;
            value ^= (poly << shift);
        }
        return value;
    }
}
