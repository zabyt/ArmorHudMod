package com.example.armorhud.drag;

import java.util.ArrayList;
import java.util.List;

import com.example.armorhud.ArmorHudClient;
import com.example.armorhud.ArmorHudConfig;
import com.example.armorhud.EffectTimersHud;
import com.zabyt.lib.hud.HudColors;
import com.zabyt.lib.screen.DraggableHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DragTargets {

    private static final int SNAP = 7;
    private static final Identifier EFFECT_BG = Identifier.withDefaultNamespace("hud/effect_background");

    private DragTargets() {
    }

    public static class ArmorDragTarget implements DraggableHud {

        private final List<ItemStack> preview = new ArrayList<>();
        private int chestIdx = -1;
        private boolean snapFlush;
        private boolean snapRow;
        private boolean snapCenter;

        public ArmorDragTarget() {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                Object[] info = ArmorHudClient.collectWorn(player);
                @SuppressWarnings("unchecked")
                List<ItemStack> worn = (List<ItemStack>) info[0];
                preview.addAll(worn);
                chestIdx = (int) info[1];
            }
            if (preview.isEmpty()) {
                preview.add(new ItemStack(Items.DIAMOND_HELMET));
                preview.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
                preview.add(new ItemStack(Items.DIAMOND_LEGGINGS));
                preview.add(new ItemStack(Items.DIAMOND_BOOTS));
            }
        }

        @Override
        public Component dragTitle() {
            return Component.literal("Position Armor HUD");
        }

        @Override
        public int xOffset() {
            return ArmorHudConfig.get().xOffset;
        }

        @Override
        public int yOffset() {
            return ArmorHudConfig.get().yOffset;
        }

        @Override
        public void setOffsets(int x, int y) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            cfg.xOffset = x;
            cfg.yOffset = y;
        }

        private int[] baseAnchor(int screenWidth, int screenHeight) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return new int[]{screenWidth / 2 + ArmorHudClient.BAR_WIDTH / 2,
                        screenHeight - ArmorHudClient.BAR_HEIGHT};
            }
            return ArmorHudClient.computeAnchor(screenWidth, screenHeight, player,
                    ArmorHudConfig.get(), preview.size());
        }

        @Override
        public void drawPreview(GuiGraphicsExtractor graphics, Minecraft minecraft, int w, int h) {
            int[] a = baseAnchor(w, h);
            ArmorHudConfig cfg = ArmorHudConfig.get();
            ArmorHudClient.drawPanel(graphics, minecraft, preview,
                    a[0] + cfg.xOffset, a[1] + cfg.yOffset, cfg.showDurability, chestIdx,
                    net.minecraft.world.item.ItemStack.EMPTY);
        }

        @Override
        public void applySnapping(int w, int h) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            int totalWidth = preview.size() * ArmorHudClient.SLOT_PITCH + 2;
            int baseX = baseAnchor(w, h)[0];

            snapFlush = Math.abs(cfg.xOffset) <= SNAP;
            if (snapFlush) {
                cfg.xOffset = 0;
            }
            snapRow = Math.abs(cfg.yOffset) <= SNAP;
            if (snapRow) {
                cfg.yOffset = 0;
            }
            int diff = baseX + cfg.xOffset + totalWidth / 2 - w / 2;
            snapCenter = Math.abs(diff) <= SNAP;
            if (snapCenter) {
                cfg.xOffset -= diff;
            }
        }

        @Override
        public void drawGuides(GuiGraphicsExtractor graphics, int w, int h) {
            int[] p = baseAnchor(w, h);
            ArmorHudConfig cfg = ArmorHudConfig.get();
            int px = p[0] + cfg.xOffset;
            int py = p[1] + cfg.yOffset;
            if (snapFlush) {
                graphics.fill(px, 0, px + 1, h, HudColors.GUIDE_LINE);
            }
            if (snapCenter) {
                graphics.fill(w / 2, 0, w / 2 + 1, h, HudColors.GUIDE_LINE);
            }
            if (snapRow) {
                int rowY = py + ArmorHudClient.BAR_HEIGHT / 2;
                graphics.fill(0, rowY, w, rowY + 1, HudColors.GUIDE_LINE);
            }
        }
    }

    public static class EffectsDragTarget implements DraggableHud {

        private boolean snapX;
        private boolean snapY;

        @Override
        public Component dragTitle() {
            return Component.literal("Position Effect Timers");
        }

        @Override
        public int xOffset() {
            return ArmorHudConfig.get().effectXOffset;
        }

        @Override
        public int yOffset() {
            return ArmorHudConfig.get().effectYOffset;
        }

        @Override
        public void setOffsets(int x, int y) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            cfg.effectXOffset = x;
            cfg.effectYOffset = y;
        }

        @Override
        public void drawPreview(GuiGraphicsExtractor graphics, Minecraft minecraft, int w, int h) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            int cellX = w - EffectTimersHud.CELL_STEP;
            int topY = 1;
            int bottomY = 27;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BG, cellX, topY, 24, 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BG, cellX, bottomY, 24, 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    net.minecraft.client.gui.Hud.getMobEffectSprite(MobEffects.SPEED),
                    cellX + 3, topY + 3, 18, 18);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    net.minecraft.client.gui.Hud.getMobEffectSprite(MobEffects.HASTE),
                    cellX + 3, bottomY + 3, 18, 18);

            String t1 = "1:05";
            String t2 = "0:03";
            long now = System.currentTimeMillis();
            graphics.text(minecraft.font, t1,
                    cellX + 12 - minecraft.font.width(t1) / 2 + cfg.effectXOffset,
                    topY + 24 + cfg.effectYOffset, HudColors.WHITE, true);
            graphics.text(minecraft.font, t2,
                    cellX + 12 - minecraft.font.width(t2) / 2 + cfg.effectXOffset,
                    bottomY + 24 + cfg.effectYOffset, HudColors.pulsingRed(now, 600), true);
        }

        @Override
        public void applySnapping(int w, int h) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            snapX = Math.abs(cfg.effectXOffset) <= SNAP;
            if (snapX) {
                cfg.effectXOffset = 0;
            }
            snapY = Math.abs(cfg.effectYOffset) <= SNAP;
            if (snapY) {
                cfg.effectYOffset = 0;
            }
        }

        @Override
        public void drawGuides(GuiGraphicsExtractor graphics, int w, int h) {
            if (snapX) {
                int x = w - EffectTimersHud.CELL_STEP + 12;
                graphics.fill(x, 0, x + 1, h, HudColors.GUIDE_LINE);
            }
            if (snapY) {
                graphics.fill(0, 25, w, 26, HudColors.GUIDE_LINE);
            }
        }
    }

    public static class FpsDragTarget implements DraggableHud {

        private boolean snapX;
        private boolean snapY;

        @Override
        public Component dragTitle() {
            return Component.literal("Position FPS Counter");
        }

        @Override
        public int xOffset() {
            return ArmorHudConfig.get().fpsXOffset;
        }

        @Override
        public int yOffset() {
            return ArmorHudConfig.get().fpsYOffset;
        }

        @Override
        public void setOffsets(int x, int y) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            cfg.fpsXOffset = x;
            cfg.fpsYOffset = y;
        }

        @Override
        public void drawPreview(GuiGraphicsExtractor graphics, Minecraft minecraft, int w, int h) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            int px = w / 2 + ArmorHudClient.BAR_WIDTH / 2 + cfg.fpsXOffset;
            int py = 4 + cfg.fpsYOffset;
            String text = "120 : 240";
            int tw = minecraft.font.width(text);
            graphics.fill(px, py, px + tw + 6, py + 12, 0x80000000);
            graphics.text(minecraft.font, text, px + 3, py + 2, 0xFF54E38A, true);
        }

        @Override
        public void applySnapping(int w, int h) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            int px = w / 2 + ArmorHudClient.BAR_WIDTH / 2 + cfg.fpsXOffset;
            int baseX = w / 2 + ArmorHudClient.BAR_WIDTH / 2;
            snapX = Math.abs(cfg.fpsXOffset) <= SNAP;
            if (snapX) {
                cfg.fpsXOffset = 0;
            }
            snapY = Math.abs(cfg.fpsYOffset) <= SNAP;
            if (snapY) {
                cfg.fpsYOffset = 0;
            }
            int diff = px - w + 30;
            if (Math.abs(diff) <= SNAP) {
                cfg.fpsXOffset -= diff;
            }
        }

        @Override
        public void drawGuides(GuiGraphicsExtractor graphics, int w, int h) {
            ArmorHudConfig cfg = ArmorHudConfig.get();
            if (snapX) {
                int x = w / 2 + ArmorHudClient.BAR_WIDTH / 2;
                graphics.fill(x, 0, x + 1, h, HudColors.GUIDE_LINE);
            }
            if (snapY) {
                graphics.fill(0, 6, w, 7, HudColors.GUIDE_LINE);
            }
        }
    }
}
