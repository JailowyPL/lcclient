package com.lightcraft.mixin;

import com.lightcraft.client.LightCraftClientInit;
import com.lightcraft.client.screen.LightCraftLoadingOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

/**
 * Swaps the vanilla {@link LoadingOverlay} for our Light Craft overlay
 * whenever Minecraft calls {@code setOverlay(new LoadingOverlay(...))}.
 *
 * Falls back to vanilla if the custom overlay is disabled in config or if
 * any construction error occurs (we catch and return the original overlay).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"),
        index = 0,
        require = 0
    )
    private net.minecraft.client.gui.screens.Overlay lightcraft$wrapInitialLoading(
            net.minecraft.client.gui.screens.Overlay original) {
        return replaceIfLoading(original);
    }

    private net.minecraft.client.gui.screens.Overlay replaceIfLoading(
            net.minecraft.client.gui.screens.Overlay original) {
        try {
            if (!LightCraftClientInit.config().enableLoadingOverlay) return original;
            if (!(original instanceof LoadingOverlay lo)) return original;

            // Reflectively read the private fields of the vanilla LoadingOverlay so we
            // can forward the same reload + exception handler to our replacement.
            var cls = LoadingOverlay.class;
            var reloadField = cls.getDeclaredField("reload");
            var handlerField = cls.getDeclaredField("onFinish");
            var reloadingField = cls.getDeclaredField("fadeIn");
            reloadField.setAccessible(true);
            handlerField.setAccessible(true);
            reloadingField.setAccessible(true);

            ReloadInstance reload = (ReloadInstance) reloadField.get(lo);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> handler = (Consumer<Throwable>) handlerField.get(lo);
            boolean reloading = reloadingField.getBoolean(lo);

            Minecraft mc = (Minecraft) (Object) this;
            return new LightCraftLoadingOverlay(mc, reload, handler, reloading);
        } catch (Throwable t) {
            LightCraftClientInit.LOGGER.warn(
                    "[LightCraft] Could not wrap LoadingOverlay, falling back to vanilla.", t);
            return original;
        }
    }
}
