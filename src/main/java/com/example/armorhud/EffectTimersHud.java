package com.example.armorhud;

import com.google.common.collect.Ordering;
import com.zabyt.lib.hud.HudColors;
import com.zabyt.lib.hud.HudTimes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class EffectTimersHud {

    public static final int CELL_STEP = 25;
    static final int CELL_SIZE = 24;

    private EffectTimersHud() {
    }

    record Marker(int cellX, int cellY, String text, int textColor, Integer overlay) {
    }

    static void render(GuiGraphicsExtractor graphics, Minecraft minecraft, DeltaTracker tickCounter) {
        ArmorHudConfig cfg = ArmorHudConfig.get();
        if (!guardsPass(minecraft, cfg)) {
            return;
        }
        for (Marker m : computeMarkers(minecraft, cfg)) {
            if (m.overlay != null) {
                graphics.fill(m.cellX, m.cellY, m.cellX + CELL_SIZE, m.cellY + CELL_SIZE, m.overlay);
            }
            int tx = m.cellX + CELL_SIZE / 2 - minecraft.font.width(m.text) / 2;
            graphics.text(minecraft.font, m.text, tx, m.cellY + CELL_SIZE, m.textColor, true);
        }
    }

    private static boolean guardsPass(Minecraft minecraft, ArmorHudConfig cfg) {
        LocalPlayer player = minecraft.player;
        if (!cfg.showEffectTimers || player == null || minecraft.level == null || player.isSpectator()) {
            return false;
        }
        if (minecraft.gui.hud.isHidden()) {
            return false;
        }
        if (minecraft.gui.screen() != null && minecraft.gui.screen().showsActiveEffects()) {
            return false;
        }
        return !player.getActiveEffects().isEmpty();
    }

    static List<Marker> computeMarkers(Minecraft minecraft, ArmorHudConfig cfg) {
        LocalPlayer player = minecraft.player;
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        List<MobEffectInstance> sorted = Ordering.natural().reverse().sortedCopy(effects);

        List<Marker> markers = new ArrayList<>(sorted.size());
        int beneficial = 0;
        int harmful = 0;

        for (MobEffectInstance inst : sorted) {
            if (!inst.showIcon()) {
                continue;
            }
            int cellX = minecraft.getWindow().getGuiScaledWidth() + cfg.effectXOffset;
            int cellY = 1 + cfg.effectYOffset;
            if (inst.getEffect().value().isBeneficial()) {
                cellX -= CELL_STEP * ++beneficial;
            } else {
                cellX -= CELL_STEP * ++harmful;
                cellY += 26;
            }

            boolean infinite = inst.isInfiniteDuration();
            String text;
            int textColor;
            Integer overlay = null;
            if (infinite) {
                text = "*";
                textColor = HudColors.WHITE;
            } else {
                int secs = (int) Math.ceil(inst.getDuration() / 20.0);
                text = HudTimes.mmss(secs);
                textColor = secs <= cfg.timerRedSeconds ? HudColors.RED : HudColors.WHITE;
                overlay = overlayFor(secs, cfg);
            }
            markers.add(new Marker(cellX, cellY, text, textColor, overlay));
        }
        return markers;
    }

    private static Integer overlayFor(int secs, ArmorHudConfig cfg) {
        if (!cfg.tintEffectIcons) {
            return null;
        }
        if (secs <= cfg.timerPulseSeconds) {
            return HudColors.pulsingRed(System.currentTimeMillis(), 600);
        }
        if (secs <= cfg.timerRedSeconds) {
            return HudColors.steadyRedTint();
        }
        return null;
    }
}
