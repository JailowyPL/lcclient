package com.lightcraft.client.screen;

import com.lightcraft.client.LightCraftClientInit;
import com.lightcraft.client.anim.AnimationController;
import com.lightcraft.client.config.LightCraftConfig;
import com.lightcraft.client.render.GradientRenderer;
import com.lightcraft.client.render.ParticleSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Cinematic intro:
 * - cosmos / starfield background
 * - half-orbit arc grows over the title
 * - letters of LIGHT CRAFT CLIENT appear one by one with tracking animation
 * - subtitle fades in + glow pulses
 * - at the end, transitions to the vanilla TitleScreen
 */
public final class IntroScreen extends Screen {
    private static final ResourceLocation MOON =
            ResourceLocation.fromNamespaceAndPath("lightcraft", "textures/gui/moonplay.png");

    private final long startMillis = System.currentTimeMillis();
    private long lastFrame = startMillis;
    private ParticleSystem particles;
    private float outro = 0f;

    // Sequence timings (seconds)
    private static final float T_BG_IN       = 0.6f;
    private static final float T_MOON_IN     = 1.0f;
    private static final float T_LETTERS     = 1.8f;
    private static final float T_SUB_IN      = 0.6f;
    private static final float T_HOLD        = 1.4f;
    private static final float T_FADE_OUT    = 0.9f;

    public IntroScreen() {
        super(Component.literal("Light Craft Intro"));
    }

    @Override
    protected void init() {
        LightCraftConfig cfg = LightCraftClientInit.config();
        particles = new ParticleSystem(Math.max(60, cfg.particleCount));
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float tickDelta) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        long now = System.currentTimeMillis();
        float dt = (now - lastFrame) / 1000f;
        lastFrame = now;

        LightCraftConfig cfg = LightCraftClientInit.config();
        float scale = 1f / Math.max(0.05f, cfg.animationSpeed);
        float t = (now - startMillis) / 1000f * scale;

        // Stage offsets
        float s1 = AnimationController.easeOutCubic(Math.min(1f, t / T_BG_IN));
        float s2 = AnimationController.easeOutCubic(Math.min(1f, (t - T_BG_IN) / T_MOON_IN));
        float s3 = Math.max(0f, Math.min(1f, (t - T_BG_IN - T_MOON_IN) / T_LETTERS));
        float s4 = AnimationController.easeOutCubic(
                Math.min(1f, (t - T_BG_IN - T_MOON_IN - T_LETTERS) / T_SUB_IN));

        float totalBeforeOutro = T_BG_IN + T_MOON_IN + T_LETTERS + T_SUB_IN + T_HOLD;
        if (t >= totalBeforeOutro) {
            outro = Math.min(1f, (t - totalBeforeOutro) / T_FADE_OUT);
        }

        // 1. Deep-space background (black -> cosmic gradient)
        g.fill(0, 0, w, h, 0xFF000000);
        int[] cosmos = new int[]{
                GradientRenderer.withAlpha(0xFF0A0420, s1),
                GradientRenderer.withAlpha(0xFF1C0A3E, s1),
                GradientRenderer.withAlpha(0xFF0B1B3F, s1),
                GradientRenderer.withAlpha(0xFF050510, s1)
        };
        GradientRenderer.fillMultiStop(g, 0, 0, w, h, cosmos);

        // 2. Starfield
        particles.resize(w, h);
        particles.update(dt);
        particles.render(g, s1);

        // 3. Half-orbit arc (grows + soft glow)
        drawOrbit(g, w, h, s2, t);

        // 4. Title — letter-by-letter reveal with tracking spacing
        drawTitle(g, w, h, s3, t);

        // 5. Subtitle
        drawSubtitle(g, w, h, s4, t);

        // 6. Outro fade to black, then TitleScreen
        if (outro > 0f) {
            g.fill(0, 0, w, h,
                    GradientRenderer.withAlpha(0xFF000000, AnimationController.easeInQuad(outro)));
            if (outro >= 1f && minecraft != null) {
                minecraft.setScreen(new TitleScreen());
            }
        }
    }

    private void drawOrbit(GuiGraphics g, int w, int h, float s, float t) {
        if (s <= 0f) return;
        int cx = w / 2;
        int cy = (int) (h * 0.42f);
        int radius = (int) (Math.min(w, h) * 0.18f * s);
        if (radius < 4) return;

        // Moon image faintly behind (scaled)
        int ms = radius * 2;
        int mx = cx - ms / 2;
        int my = cy - ms / 2;
        g.blit(MOON, mx, my, 0, 0, ms, ms, ms, ms);
        // Soft pulse glow on top of the arc
        float pulse = 0.5f + 0.5f * AnimationController.pulse(t, 0.3f);
        int glow = GradientRenderer.withAlpha(0xFFFFFFFF, s * 0.25f * pulse);
        GradientRenderer.glowRect(g, cx, cy - radius / 2, radius, glow);

        // Thin arc line (simulated by stepping a semi-circle in pixels)
        int steps = 64;
        for (int i = 0; i <= steps; i++) {
            float a = (float) Math.PI * (1f - i / (float) steps);
            int px = cx + (int) (Math.cos(a) * radius);
            int py = cy - (int) (Math.sin(a) * radius);
            int col = GradientRenderer.withAlpha(0xFFFFFFFF, s * 0.85f);
            g.fill(px, py, px + 1, py + 1, col);
        }
    }

    private void drawTitle(GuiGraphics g, int w, int h, float progress, float t) {
        if (progress <= 0f || minecraft == null) return;
        String title = LightCraftClientInit.config().title;
        int base = minecraft.font.width(title);
        // Tracking animation: wide -> tight
        float tracking = AnimationController.lerp(14f, 4f, AnimationController.easeOutCubic(progress));
        int totalW = base + (int) (tracking * (title.length() - 1));
        int x = (w - totalW) / 2;
        int y = (int) (h * 0.58f);

        float perLetter = 1f / title.length();
        for (int i = 0; i < title.length(); i++) {
            float local = Math.min(1f, Math.max(0f, (progress - i * perLetter) / perLetter));
            local = AnimationController.easeOutCubic(local);
            int alpha = (int) (local * 255) & 0xFF;
            if (alpha < 5) break;
            int col = (alpha << 24) | 0xFFFFFFFF & 0x00FFFFFF | 0xFFFFFF;
            // glow behind letter
            String ch = String.valueOf(title.charAt(i));
            int cw = minecraft.font.width(ch);
            int lx = x + (int) (tracking * i) + sumLetters(title, i);
            int glow = GradientRenderer.withAlpha(0xFFB967FF, local * 0.45f);
            GradientRenderer.glowRect(g, lx + cw / 2, y + 4, 14, glow);
            g.drawString(minecraft.font, ch, lx, y, col, false);
        }
    }

    private int sumLetters(String s, int upTo) {
        int acc = 0;
        if (minecraft == null) return 0;
        for (int i = 0; i < upTo; i++) acc += minecraft.font.width(String.valueOf(s.charAt(i)));
        return acc;
    }

    private void drawSubtitle(GuiGraphics g, int w, int h, float s, float t) {
        if (s <= 0f || minecraft == null) return;
        String sub = LightCraftClientInit.config().subtitle;
        int alpha = (int) (s * 200) & 0xFF;
        int col = (alpha << 24) | 0xCFEBFF;
        g.drawCenteredString(minecraft.font, sub, w / 2, (int) (h * 0.66f), col);
    }
}
