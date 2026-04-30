package com.lightcraft.client.anim;

/**
 * Easing / timing utilities for Light Craft's cinematic animations.
 * All functions operate on a normalised [0..1] progress value.
 * Inspired by CSS cubic-bezier timing functions.
 */
public final class AnimationController {
    private AnimationController() {}

    public static float clamp01(float x) {
        return x < 0f ? 0f : (x > 1f ? 1f : x);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp01(t);
    }

    /** Smooth in/out (S-curve). */
    public static float easeInOut(float t) {
        t = clamp01(t);
        return t * t * (3f - 2f * t);
    }

    /** Strong ease-out, good for entrance animations. */
    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    /** Soft start, useful for blur / fade. */
    public static float easeInQuad(float t) {
        t = clamp01(t);
        return t * t;
    }

    /** Pulse between 0 and 1 with sinusoidal curve. */
    public static float pulse(float seconds, float frequencyHz) {
        return 0.5f + 0.5f * (float) Math.sin(seconds * frequencyHz * Math.PI * 2.0);
    }

    /** Shimmer gradient offset in the [0..1] range. */
    public static float shimmer(float seconds, float speed) {
        float v = (seconds * speed) % 1f;
        return v < 0f ? v + 1f : v;
    }
}
