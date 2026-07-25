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
    public String gradient = "none";
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
        return Math.round(value * 100.0) / 100.0;
    }

    public Component displayName() {
        String value = name.isBlank() ? "Untitled bookmark" : name;
        if (!hasGradient()) return Component.literal(value);
        return gradientText(value, packedColor(gradientStart()), packedColor(gradientEnd()));
    }

    public static Component gradientText(String value, int startColor, int endColor) {
        MutableComponent result = Component.empty();
        for (int i = 0; i < value.length(); i++) {
            float progress = value.length() <= 1 ? 0.0F : (float) i / (value.length() - 1);
            int color = interpolateColor(startColor, endColor, progress);
            result = result.append(Component.literal(String.valueOf(value.charAt(i)))
                    .withStyle(style -> style.withColor(color)));
        }
        return result;
    }

    public boolean hasGradient() {
        return gradientStart() != null;
    }

    private String gradientKey() {
        return gradient == null ? "none" : gradient.trim().toLowerCase(Locale.ROOT);
    }

    public String gradientLabel() {
        return switch (gradientKey()) {
            case "black" -> "Black";
            case "red" -> "Red gradient";
            case "yellow" -> "Yellow gradient";
            case "blue" -> "Blue gradient";
            case "orange" -> "Orange gradient";
            case "purple" -> "Purple gradient";
            case "green" -> "Green gradient";
            case "pink" -> "Pink gradient";
            default -> "No gradient";
        };
    }

    public void cycleGradient() {
        String[] gradients = {"none", "red", "yellow", "blue", "orange", "purple", "black", "green", "pink"};
        for (int i = 0; i < gradients.length; i++) {
            if (gradients[i].equals(gradient)) {
                gradient = gradients[(i + 1) % gradients.length];
                return;
            }
        }
        gradient = gradients[0];
    }

    private int[] gradientStart() {
        return switch (gradientKey()) {
            case "black" -> new int[]{0, 0, 0};
            case "red" -> new int[]{255, 85, 85};
            case "yellow" -> new int[]{255, 255, 85};
            case "blue" -> new int[]{85, 255, 255};
            case "orange" -> new int[]{255, 170, 0};
            case "purple" -> new int[]{255, 85, 255};
            case "green" -> new int[]{85, 255, 85};
            case "pink" -> new int[]{255, 153, 204};
            default -> null;
        };
    }

    private int[] gradientEnd() {
        return switch (gradientKey()) {
            case "black" -> new int[]{0, 0, 0};
            case "red" -> new int[]{170, 0, 0};
            case "yellow" -> new int[]{255, 170, 0};
            case "blue" -> new int[]{85, 85, 255};
            case "orange" -> new int[]{170, 55, 0};
            case "purple" -> new int[]{170, 0, 170};
            case "green" -> new int[]{0, 170, 0};
            case "pink" -> new int[]{255, 51, 136};
            default -> null;
        };
    }

    private static int interpolate(int start, int end, float progress) {
        return Math.round(start + (end - start) * progress);
    }

    private static int packedColor(int[] color) {
        return (color[0] << 16) | (color[1] << 8) | color[2];
    }

    private static int interpolateColor(int start, int end, float progress) {
        int red = interpolate((start >> 16) & 0xFF, (end >> 16) & 0xFF, progress);
        int green = interpolate((start >> 8) & 0xFF, (end >> 8) & 0xFF, progress);
        int blue = interpolate(start & 0xFF, end & 0xFF, progress);
        return (red << 16) | (green << 8) | blue;
    }
}
