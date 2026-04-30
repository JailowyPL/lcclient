# Light Craft Client — Minecraft 26.1.2 Fabric Mod

A premium, cinematic loading & intro overlay for Minecraft Java Edition **26.1.2** (Fabric).

Inspired by modern AAA launchers and premium clients (Lunar / Badlion) — but with an artistic, minimalist, neon-futuristic identity: violet → pink → blue → cyan gradient, glass glow, animated circuit lines, starfield particles, shimmer progress bar, and a cosmic half-orbit intro for **LIGHT CRAFT CLIENT**.

## ✨ Features

- Fully replaces the vanilla loading overlay (with safe fallback to vanilla)
- Cinematic multi-phase loading animation (gradient reveal → logo blur-in → circuit lines → shimmer progress)
- Cosmic intro screen: starfield, half-orbit moon arc, letter-by-letter title reveal, pulsing subtitle
- 60 fps feel — async particle updates, Blaze3D-friendly, Vulkan-ready (no raw OpenGL calls)
- JSON configuration: `config/lightcraft.json` — toggle intro, animation speed, colors, title, subtitle, particle count
- Modular architecture — animation / rendering / config / mixin are cleanly separated

## 📦 Project Structure

```
lightcraft-mod/
├── build.gradle            # Loom 1.15, Java 25, MC 26.1.2, official Mojang mappings
├── settings.gradle
├── gradle.properties
└── src/main/
    ├── java/com/lightcraft/
    │   ├── LightCraftClient.java              # API marker
    │   ├── client/
    │   │   ├── LightCraftClientInit.java      # Fabric client entrypoint
    │   │   ├── anim/AnimationController.java  # easing / pulse / shimmer
    │   │   ├── config/LightCraftConfig.java   # JSON config
    │   │   ├── render/GradientRenderer.java   # multi-stop gradient + glow + shimmer bar
    │   │   ├── render/ParticleSystem.java     # starfield particles
    │   │   └── screen/
    │   │       ├── LightCraftLoadingOverlay.java  # replaces vanilla LoadingOverlay
    │   │       └── IntroScreen.java               # cinematic intro
    │   └── mixin/MinecraftClientMixin.java    # hook to swap vanilla overlay
    └── resources/
        ├── fabric.mod.json
        ├── lightcraft.mixins.json
        └── assets/lightcraft/
            ├── icon/icon.png
            └── textures/gui/
                ├── logo.png         # loading logo (feather / crystal)
                └── moonplay.png     # intro half-orbit background
```

## 🛠 Installation (for players)

1. Install **Fabric Loader 0.18.4+** for Minecraft **26.1.2**.
2. Install **Fabric API 0.146.1+26.1.2** into your `mods/` folder.
3. Drop `lightcraft-client-1.0.0.jar` (produced by `./gradlew build`) into your `mods/` folder.
4. Launch Minecraft — the custom loading screen + intro will play automatically.

## 🧑‍💻 Building from source

Requires:
- JDK **25** (Eclipse Temurin 25 recommended)
- Gradle **9.4+** (wrapper is generated on first run)
- IntelliJ IDEA **2025.3+** for mixin tooling (optional)

```bash
cd lightcraft-mod
./gradlew build
```

The compiled jar will be at `build/libs/lightcraft-client-1.0.0.jar`.

> Note: 26.1 is the first version **not obfuscated** and uses **official Mojang mappings** only (Yarn is no longer supported). `build.gradle` already sets `loom.officialMojangMappings()`.

## ⚙️ Configuration

On first run, `config/lightcraft.json` is created:

```json
{
  "enableIntro": true,
  "enableLoadingOverlay": true,
  "animationSpeed": 1.0,
  "title": "LIGHT CRAFT CLIENT",
  "subtitle": "Powered by Light Craft",
  "gradientColors": ["#2B0B4E", "#8A2BE2", "#FF3EA5", "#1EA7FF", "#6FF7FF"],
  "particleCount": 120
}
```

- `animationSpeed` — `1.0` default, `2.0` = twice as fast, `0.5` = half speed
- Disable the whole mod visually by setting `enableIntro` and `enableLoadingOverlay` to `false`

## 🧩 Replacing assets

Drop your own PNGs into `src/main/resources/assets/lightcraft/textures/gui/`:
- `logo.png` — loading-screen icon (square, 512×512 recommended)
- `moonplay.png` — intro background (square)

## 🛡 Fallback

If anything fails during init (missing assets, config parse error, reflection error), the mod **silently falls back to the vanilla loading screen**, so your game is never broken.

## 📜 License

MIT © Light Craft / Moon Play Team
