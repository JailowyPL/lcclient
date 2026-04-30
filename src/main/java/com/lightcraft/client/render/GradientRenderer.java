package com.lightcraft.client.render;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Low-level gradient + glow helpers built on GuiGraphics (Blaze3D-backed in 26.1+).
 * These do not rely on legacy raw OpenGL calls and are Vulkan-ready.
 */
public final class GradientRenderer {
    private GradientRenderer() {}

    /** Parses #RRGGBB or #AARRGGBB into an ARGB int. */
    public static int hex(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s;
        long v = Long.parseLong(h, 16);
        if (h.length() == 6) v |= 0xFF000000L;
        return (int) v;
    }

    /** Linear interpolation between two ARGB colors. */
    public static int lerpColor(int a, int b, float t) {
        t = t < 0f ? 0f : (t > 1f ? 1f : t);
        int aA = (a >>> 24) & 0xFF, aR = (a >>> 16) & 0xFF, aG = (a >>> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >>> 16) & 0xFF, bG = (b >>> 8) & 0xFF, bB = b & 0xFF;
        int rA = (int) (aA + (bA - aA) * t);
        int rR = (int) (aR + (bR - aR) * t);
        int rG = (int) (aG + (bG - aG) * t);
        int rB = (int) (aB + (bB - aB) * t);
        return (rA << 24) | (rR << 16) | (rG << 8) | rB;
    }

    /** Applies an alpha multiplier to an ARGB color. */
    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Multi-stop diagonal gradient fill.
     * Stops are evenly distributed. Drawn as horizontal slices to approximate
     * a diagonal using per-row interpolation (cheap, GPU-friendly).
     */
    public static void fillMultiStop(GuiGraphics g, int x, int y, int w, int h, int[] stops) {
        if (stops.length == 0) return;
        if (stops.length == 1) {
            g.fill(x, y, x + w, y + h, stops[0]);
            return;
        }
        for (int i = 0; i < h; i++) {
            float t = (float) i / Math.max(1, h - 1);
            float seg = t * (stops.length - 1);
            int idx = (int) Math.floor(seg);
            int next = Math.min(stops.length - 1, idx + 1);
            int c = lerpColor(stops[idx], stops[next], seg - idx);
            g.fill(x, y + i, x + w, y + i + 1, c);
        }
    }

    /** Soft glow rectangle — layered translucent fills that simulate a blur halo. */
    public static void glowRect(GuiGraphics g, int cx, int cy, int radius, int argb) {
        for (int i = radius; i > 0; i -= 2) {
            float a = (float) (radius - i) / radius;
            int col = withAlpha(argb, a * 0.12f);
            g.fill(cx - i, cy - i, cx + i, cy + i, col);
        }
    }

    /** Horizontal shimmer bar — progress bar alternative. */
    public static void shimmerBar(GuiGraphics g, int x, int y, int w, int h, float phase, int colA, int colB) {
        int segments = Math.max(8, w / 4);
        int segW = w / segments;
        for (int i = 0; i < segments; i++) {
            float t = (i / (float) segments + phase) % 1f;
            float wave = (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2.0));
            int c = lerpColor(colA, colB, wave);
            g.fill(x + i * segW, y, x + (i + 1) * segW, y + h, c);
        }
    }
}
