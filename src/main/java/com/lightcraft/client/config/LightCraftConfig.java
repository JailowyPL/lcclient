package com.lightcraft.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON configuration. Located at: config/lightcraft.json
 * Allows the user to tweak intro speed, colors, texts and toggle the intro.
 */
public final class LightCraftConfig {
    public boolean enableIntro = true;
    public boolean enableLoadingOverlay = true;

    /** Animation time multiplier. 1.0 = default. 0.5 = twice as fast. */
    public float animationSpeed = 1.0f;

    public String title = "LIGHT CRAFT CLIENT";
    public String subtitle = "Powered by Light Craft";

    /** Hex colors for the gradient background (top-left -> bottom-right). */
    public String[] gradientColors = new String[] {
            "#2B0B4E", // deep violet
            "#8A2BE2", // violet
            "#FF3EA5", // pink
            "#1EA7FF", // blue
            "#6FF7FF"  // cyan
    };

    public int particleCount = 120;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static LightCraftConfig fallback() {
        LightCraftConfig c = new LightCraftConfig();
        c.enableIntro = false;
        c.enableLoadingOverlay = false;
        return c;
    }

    public static LightCraftConfig loadOrCreate() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("lightcraft.json");
        try {
            if (Files.exists(file)) {
                try (var r = Files.newBufferedReader(file)) {
                    LightCraftConfig cfg = GSON.fromJson(r, LightCraftConfig.class);
                    return cfg == null ? new LightCraftConfig() : cfg;
                }
            }
            LightCraftConfig defaults = new LightCraftConfig();
            save(defaults, file);
            return defaults;
        } catch (IOException e) {
            return new LightCraftConfig();
        }
    }

    private static void save(LightCraftConfig cfg, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(cfg));
    }
}
