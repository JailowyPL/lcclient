package com.lightcraft.client.screen;

import com.lightcraft.client.LightCraftClientInit;
import com.lightcraft.client.anim.AnimationController;
import com.lightcraft.client.config.LightCraftConfig;
import com.lightcraft.client.render.GradientRenderer;
import com.lightcraft.client.render.ParticleSystem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;

import java.util.function.Consumer;

/**
 * Replacement for vanilla {@link LoadingOverlay}.
 * Phase 1 (0.0 -> 0.35): fade from black, gradient reveals, logo blur-in
 * Phase 2 (0.35 -> 0.85): resource reload progress with shimmer bar + circuit lines
 * Phase 3 (0.85 -> 1.0): hand-off to {@link IntroScreen}
 */
public final class LightCraftLoadingOverlay extends LoadingOverlay {
    private static final ResourceLocation LOGO =
            ResourceLocation.fromNamespaceAndPath("lightcraft", "textures/gui/logo.png");

    private final Minecraft client;
    private final ReloadInstance reload;
    private final Consumer<Throwable> exceptionHandler;
    private final boolean reloading;

    private final long startMillis = System.currentTimeMillis();
    private long lastFrameMillis = startMillis;
    private float reloadProgress = 0f;
    private float fadeOut = 0f;
    private boolean introStarted = false;

    private final ParticleSystem particles;
    private final int[] gradientStops;

    public LightCraftLoadingOverlay(Minecraft client, ReloadInstance reload,
                                    Consumer<Throwable> handler, boolean reloading) {
        super(client, reload, handler, reloading);
        this.client = client;
        this.reload = reload;
        this.exceptionHandler = handler;
        this.reloading = reloading;

        LightCraftConfig cfg = LightCraftClientInit.config();
        this.particles = new ParticleSystem(cfg.particleCount);
        this.gradientStops = new int[cfg.gradientColors.length];
        for (int i = 0; i < cfg.gradientColors.length; i++) {
            gradientStops[i] = GradientRenderer.hex(cfg.gradientColors[i]);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float tickDelta) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        long now = System.currentTimeMillis();
        float dt = (now - lastFrameMillis) / 1000f;
        lastFrameMillis = now;

        LightCraftConfig cfg = LightCraftClientInit.config();
        float elapsed = (now - startMillis) / 1000f * (1f / Math.max(0.05f, cfg.animationSpeed));

        // Track reload progress.
        reloadProgress = AnimationController.lerp(reloadProgress,
                reload.getActualProgress(), Math.min(1f, dt * 4f));

        // Intro enter — 0.8s fade in of the whole scene.
        float intro = AnimationController.easeOutCubic(Math.min(1f, elapsed / 0.8f));

        // --- Layer 0: black base ---
        g.fill(0, 0, w, h, 0xFF000000);

        // --- Layer 1: gradient (fade-in) ---
        int[] stops = new int[gradientStops.length];
        for (int i = 0; i < stops.length; i++) {
            stops[i] = GradientRenderer.withAlpha(gradientStops[i], intro * 0.95f);
        }
        GradientRenderer.fillMultiStop(g, 0, 0, w, h, stops);

        // --- Layer 2: starfield particles ---
        particles.resize(w, h);
        particles.update(dt);
        particles.render(g, intro);

        // --- Layer 3: circuit lines (animated stroke) ---
        drawCircuitLines(g, w, h, elapsed, intro);

        // --- Layer 4: logo with blur-in + subtle zoom ---
        float logoT = AnimationController.easeOutCubic(Math.min(1f, (elapsed - 0.2f) / 1.2f));
        float scale = AnimationController.lerp(1.25f, 1.0f, logoT);
        float alpha = logoT;
        drawLogo(g, w, h, scale, alpha, elapsed);

        // --- Layer 5: shimmer progress bar ---
        drawShimmerBar(g, w, h, reloadProgress, elapsed, intro);

        // --- Layer 6: subtitle / status ---
        drawStatus(g, w, h, elapsed, intro);

        // --- Hand-off to IntroScreen ---
        if (reload.isDone() && !introStarted) {
            fadeOut += dt * 1.25f;
            if (fadeOut >= 1f) {
                introStarted = true;
                try {
                    reload.checkExceptions();
                    exceptionHandler.accept(null);
                } catch (Throwable t) {
                    exceptionHandler.accept(t);
                }
                if (cfg.enableIntro) {
                    client.setScreen(new IntroScreen());
                } else {
                    client.setOverlay(null);
                }
            }
            // Black fade-to-transition
            g.fill(0, 0, w, h, GradientRenderer.withAlpha(0xFF000000,
                    AnimationController.easeInQuad(Math.min(1f, fadeOut))));
        }

        RenderSystem.enableBlend();
    }

    private void drawCircuitLines(GuiGraphics g, int w, int h, float t, float intro) {
        int cx = w / 2, cy = h / 2;
        float phase = AnimationController.shimmer(t, 0.25f);
        int lines = 6;
        for (int i = 0; i < lines; i++) {
            float a = (i / (float) lines + phase) % 1f;
            int len = (int) (Math.min(w, h) * 0.45f * a);
            int alpha = (int) (140 * intro * (1f - a));
            int col = (alpha << 24) | 0x6FF7FF;
            // horizontal strokes left / right
            g.fill(cx - len, cy - 64 + i * 8, cx - len + 40, cy - 63 + i * 8, col);
            g.fill(cx + len - 40, cy + 48 - i * 8, cx + len, cy + 49 - i * 8, col);
        }
    }

    private void drawLogo(GuiGraphics g, int w, int h, float scale, float alpha, float t) {
        int logoSize = 96;
        int s = (int) (logoSize * scale);
        int x = (w - s) / 2;
        int y = (h - s) / 2 - 40;

        // Glow halo — soft pulse
        float pulse = 0.6f + 0.4f * AnimationController.pulse(t, 0.45f);
        int glow = GradientRenderer.withAlpha(0xFFB967FF, alpha * 0.35f * pulse);
        GradientRenderer.glowRect(g, x + s / 2, y + s / 2, (int) (s * 0.9f), glow);

        // Logo texture
        RenderSystem.enableBlend();
        int a = (int) (alpha * 255) & 0xFF;
        int tint = (a << 24) | 0xFFFFFF;
        g.blit(LOGO, x, y, 0, 0, s, s, s, s);
        // overlay a translucent quad to modulate alpha without touching shader tint
        if (a < 255) {
            g.fill(x, y, x + s, y + s, GradientRenderer.withAlpha(0xFF000000, 1f - alpha));
        }
    }

    private void drawShimmerBar(GuiGraphics g, int w, int h, float progress, float t, float intro) {
        int barW = (int) (w * 0.42f);
        int barH = 3;
        int x = (w - barW) / 2;
        int y = h - 80;

        // Backing track
        g.fill(x, y, x + barW, y + barH, GradientRenderer.withAlpha(0xFFFFFFFF, 0.08f * intro));

        // Progress fill
        int pw = (int) (barW * Math.max(0.05f, progress));
        float phase = AnimationController.shimmer(t, 0.4f);
        GradientRenderer.shimmerBar(g, x, y, pw, barH, phase,
                GradientRenderer.withAlpha(0xFFB967FF, intro),
                GradientRenderer.withAlpha(0xFF6FF7FF, intro));

        // End glow
        int gx = x + pw;
        GradientRenderer.glowRect(g, gx, y + barH / 2, 14,
                GradientRenderer.withAlpha(0xFF6FF7FF, intro * 0.9f));
    }

    private void drawStatus(GuiGraphics g, int w, int h, float t, float intro) {
        String title = "LIGHT CRAFT";
        String sub = "loading experience";
        float letterSpacing = 4f + 2f * AnimationController.pulse(t, 0.2f);

        int alpha = (int) (intro * 220) & 0xFF;
        int color = (alpha << 24) | 0xFFFFFF;
        int subColor = (alpha << 24) | 0xB8E6FF;

        int titleW = client.font.width(title) + (int) (letterSpacing * title.length());
        int tx = (w - titleW) / 2;
        int ty = h - 56;
        int cx = tx;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            g.drawString(client.font, ch, cx, ty, color, false);
            cx += client.font.width(ch) + (int) letterSpacing;
        }
        g.drawCenteredString(client.font, sub, w / 2, h - 42, subColor);
    }
}
