package com.lightcraft.client;

import com.lightcraft.client.config.LightCraftConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint for Light Craft Client.
 * Loads config and prepares the custom loading / intro sequence.
 */
public final class LightCraftClientInit implements ClientModInitializer {
    public static final String MOD_ID = "lightcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger("LightCraft");

    private static LightCraftConfig CONFIG;

    @Override
    public void onInitializeClient() {
        try {
            CONFIG = LightCraftConfig.loadOrCreate();
            LOGGER.info("[LightCraft] Initialized. Intro enabled: {}", CONFIG.enableIntro);
        } catch (Throwable t) {
            // Fallback: disable custom intro if anything goes wrong.
            LOGGER.error("[LightCraft] Failed to initialize, falling back to vanilla loading.", t);
            CONFIG = LightCraftConfig.fallback();
        }
    }

    public static LightCraftConfig config() {
        return CONFIG == null ? LightCraftConfig.fallback() : CONFIG;
    }
}
