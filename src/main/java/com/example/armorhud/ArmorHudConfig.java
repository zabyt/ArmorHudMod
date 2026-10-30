package com.example.armorhud;

import com.zabyt.lib.config.SimpleConfig;

public class ArmorHudConfig extends SimpleConfig {

    public static final String MOD_ID = "armorhud";

    private static ArmorHudConfig instance;

    public boolean enabled = true;
    public String anchor = "hotbar";
    public boolean rightSide = true;
    public int xOffset = 0;
    public int yOffset = 0;
    public boolean connectToOffhand = true;
    public boolean showDurability = true;
    public boolean showFps = false;
    public int fpsXOffset = 0;
    public int fpsYOffset = 0;
    public boolean showEffectTimers = true;
    public int timerRedSeconds = 15;
    public int timerPulseSeconds = 5;
    public boolean tintEffectIcons = true;
    public int effectXOffset = 0;
    public int effectYOffset = 0;

    protected ArmorHudConfig() {
        super(MOD_ID);
    }

    public static ArmorHudConfig get() {
        if (instance == null) {
            instance = load(MOD_ID, ArmorHudConfig.class);
        }
        return instance;
    }
}
