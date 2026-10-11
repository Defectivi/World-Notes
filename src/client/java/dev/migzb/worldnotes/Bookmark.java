package dev.migzb.worldnotes;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.UUID;

public final class Bookmark {
    public String id = UUID.randomUUID().toString();
    public String profile = "";
    public String name = "Untitled bookmark";
    public String note = "";
    public String dimension = "minecraft:overworld";
    public String gradient = Gradient.NONE.key;
    public boolean hidden;
    public double x;
    public double y;
    public double z;

    public Bookmark() { }

    public Bookmark(String profile, String name, String note, String dimension, double x, double y, double z) {
        this.profile = profile;
        this.name = name;
        this.note = note;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static double roundToHundredth(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Coordinates must be finite");
        return java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    public Bookmark copy() {
        Bookmark copy = new Bookmark(profile, name, note, dimension, x, y, z);
        copy.id = id;
        copy.gradient = gradient;
        copy.hidden = hidden;
        return copy;
    }

    public boolean validCoordinates() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Math.abs(x) < 30_000_000 && Math.abs(z) < 30_000_000 && Math.abs(y) < 20_000_000;
    }

    static boolean validDimension(String value) {
        return value != null && value.matches("[a-z0-9_.-]+:[a-z0-9/._-]+");
    }

    public Component displayName() {
        String value = name.isBlank() ? "Untitled bookmark" : name;
        Gradient style = currentGradient();
        return style == Gradient.NONE
                ? Component.literal(value)
                : gradientText(value, style.start, style.end);
    }

    public static Component gradientText(String value, int startColor, int endColor) {
        MutableComponent result = Component.empty();
        int[] codePoints = value.codePoints().toArray();
        for (int i = 0; i < codePoints.length; i++) {
            float progress = codePoints.length <= 1 ? 0.0F : (float) i / (codePoints.length - 1);
            int color = interpolateColor(startColor, endColor, progress);
            result = result.append(Component.literal(new String(Character.toChars(codePoints[i])))
                    .withStyle(style -> style.withColor(color)));
        }
        return result;
    }

    public boolean hasGradient() {
        return currentGradient() != Gradient.NONE;
    }

    public String gradientLabel() {
        return currentGradient().label;
    }

    public void cycleGradient() {
        Gradient[] values = Gradient.values();
        gradient = values[(currentGradient().ordinal() + 1) % values.length].key;
    }

    private Gradient currentGradient() {
        return Gradient.fromKey(gradient);
    }

    private static int interpolate(int start, int end, float progress) {
        return Math.round(start + (end - start) * progress);
    }

    private static int interpolateColor(int start, int end, float progress) {
        int red = interpolate((start >> 16) & 0xFF, (end >> 16) & 0xFF, progress);
        int green = interpolate((start >> 8) & 0xFF, (end >> 8) & 0xFF, progress);
        int blue = interpolate(start & 0xFF, end & 0xFF, progress);
        return (red << 16) | (green << 8) | blue;
    }

    private enum Gradient {
        NONE("none", "No gradient", -1, -1),
        RED("red", "Red gradient", 0xFF5555, 0xAA0000),
        YELLOW("yellow", "Yellow gradient", 0xFFFF55, 0xFFAA00),
        BLUE("blue", "Blue gradient", 0x55FFFF, 0x5555FF),
        ORANGE("orange", "Orange gradient", 0xFFAA00, 0xAA3700),
        PURPLE("purple", "Purple gradient", 0xFF55FF, 0xAA00AA),
        BLACK("black", "Black", 0x000000, 0x000000),
        GREEN("green", "Green gradient", 0x55FF55, 0x00AA00),
        PINK("pink", "Pink gradient", 0xFF99CC, 0xFF3388);

        final String key;
        final String label;
        final int start;
        final int end;

        Gradient(String key, String label, int start, int end) {
            this.key = key;
            this.label = label;
            this.start = start;
            this.end = end;
        }

        static Gradient fromKey(String raw) {
            String normalized = raw == null ? NONE.key : raw.trim().toLowerCase(Locale.ROOT);
            for (Gradient candidate : values()) {
                if (candidate.key.equals(normalized)) return candidate;
            }
            return NONE;
        }
    }
}
