package com.lightcraft.client.render;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * Tiny, self-contained CPU particle system for the cosmic starfield
 * used in the loading + intro screens. Frame-rate independent.
 */
public final class ParticleSystem {
    private final float[] x, y, z, twinkle, speed;
    private final int count;
    private int width, height;
    private final Random rng = new Random(0xC0FFEE);

    public ParticleSystem(int count) {
        this.count = count;
        this.x = new float[count];
        this.y = new float[count];
        this.z = new float[count];        // depth: 0 far, 1 near
        this.twinkle = new float[count];
        this.speed = new float[count];
        for (int i = 0; i < count; i++) respawn(i, true);
    }

    public void resize(int w, int h) {
        this.width = w;
        this.height = h;
    }

    private void respawn(int i, boolean anywhere) {
        x[i] = rng.nextFloat();
        y[i] = anywhere ? rng.nextFloat() : 0f;
        z[i] = 0.15f + rng.nextFloat() * 0.85f;
        twinkle[i] = rng.nextFloat() * (float) (Math.PI * 2.0);
        speed[i] = 0.01f + rng.nextFloat() * 0.04f;
    }

    public void update(float deltaSeconds) {
        for (int i = 0; i < count; i++) {
            y[i] += speed[i] * z[i] * deltaSeconds * 0.15f;
            twinkle[i] += deltaSeconds * (2.0f + z[i] * 3.0f);
            if (y[i] > 1f) respawn(i, false);
        }
    }

    public void render(GuiGraphics g, float globalAlpha) {
        if (width == 0 || height == 0) return;
        for (int i = 0; i < count; i++) {
            int px = (int) (x[i] * width);
            int py = (int) (y[i] * height);
            float tw = 0.5f + 0.5f * (float) Math.sin(twinkle[i]);
            float a = tw * z[i] * globalAlpha;
            if (a < 0.05f) continue;
            int size = z[i] > 0.75f ? 2 : 1;
            int col = GradientRenderer.withAlpha(0xFFFFFFFF, a);
            g.fill(px, py, px + size, py + size, col);
            if (z[i] > 0.88f) {
                int halo = GradientRenderer.withAlpha(0xFF6FF7FF, a * 0.35f);
                g.fill(px - 1, py - 1, px + size + 1, py + size + 1, halo);
            }
        }
    }
}
