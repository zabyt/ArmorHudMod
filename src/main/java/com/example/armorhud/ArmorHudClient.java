package com.example.armorhud;

import java.util.ArrayList;
import java.util.List;

import com.warwa.elytraslot.IElytraSlotPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArmorHudClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("armorhud");

    private static final Identifier ARMOR_HUD_ID = Identifier.fromNamespaceAndPath("armorhud", "armor_display");
    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");

    public static final int BAR_WIDTH = 182;
    public static final int BAR_HEIGHT = 22;
    public static final int SLOT_PITCH = 20;
    public static final int OFFHAND_WIDTH = 29;

    public static boolean dragPreviewActive = false;
    public static boolean fpsDragPreviewActive = false;

    private static long fpsLastTime = System.nanoTime();
    private static int fpsFrameCount = 0;
    private static int fpsDisplay = 0;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                ARMOR_HUD_ID, ArmorHudClient::renderArmor);
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("armorhud", "fps_counter"),
                (graphics, tickCounter) -> {
                    try {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc == null || mc.player == null || mc.level == null) return;

                        fpsFrameCount++;
                        long now = System.nanoTime();
                        if (now - fpsLastTime >= 1_000_000_000L) {
                            fpsDisplay = fpsFrameCount;
                            fpsFrameCount = 0;
                            fpsLastTime = now;
                        }

                        ArmorHudConfig cfg = ArmorHudConfig.get();
                        if (!cfg.showFps) return;
                        if (fpsDragPreviewActive) return;
                        if (mc.gui.hud.isHidden()) return;

                        int limit = mc.options.framerateLimit().get();
                        String fpsText = fpsDisplay + " : " + (limit >= 260 ? "∞" : limit);
                        int tw = mc.font.width(fpsText);
                        int px = graphics.guiWidth() / 2 + BAR_WIDTH / 2 + cfg.fpsXOffset;
                        int py = 4 + cfg.fpsYOffset;
                        graphics.fill(px, py, px + tw + 6, py + 12, 0xA0000000);
                        graphics.text(mc.font, fpsText, px + 3, py + 2, 0xFFFFFFFF, true);
                    } catch (Exception e) {
                        LOGGER.error("FPS HUD render error", e);
                    }
                });

        LOGGER.info("Registered all HUD elements");
        HudElementRegistry.attachElementAfter(VanillaHudElements.MOB_EFFECTS,
                Identifier.fromNamespaceAndPath("armorhud", "effect_timers"),
                (graphics, tickCounter) -> EffectTimersHud.render(graphics, Minecraft.getInstance(), tickCounter));
        LOGGER.info("Armor HUD registered");
    }

    @SuppressWarnings("unchecked")
    private static void renderArmor(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ArmorHudConfig cfg = ArmorHudConfig.get();
        if (!cfg.enabled || dragPreviewActive || player == null || minecraft.level == null || player.isSpectator()) {
            return;
        }
        if (minecraft.gui.hud.isHidden()) {
            return;
        }

        Object[] info = collectWorn(player);
        List<ItemStack> worn = (List<ItemStack>) info[0];
        int chestIdx = (int) info[1];
        if (worn.isEmpty()) {
            return;
        }

        ItemStack elytra = ItemStack.EMPTY;
        if (chestIdx >= 0 && FabricLoader.getInstance().isModLoaded("elytraslot")) {
            elytra = getElytraStack(player);
        }

        int[] anchor = computeAnchor(graphics.guiWidth(), graphics.guiHeight(), player, cfg, worn.size());
        drawPanel(graphics, minecraft, worn, anchor[0] + cfg.xOffset, anchor[1] + cfg.yOffset,
                cfg.showDurability, chestIdx, elytra);
    }


    public static Object[] collectWorn(LocalPlayer player) {
        List<ItemStack> worn = new ArrayList<>(ARMOR_SLOTS.length);
        int chestIdx = -1;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (slot == EquipmentSlot.CHEST) {
                chestIdx = worn.size();
            }
            worn.add(player.getItemBySlot(slot));
        }
        return new Object[]{worn, chestIdx};
    }

    @SuppressWarnings("unchecked")
    private static ItemStack getElytraStack(LocalPlayer player) {
        try {
            if (player instanceof IElytraSlotPlayer holder) {
                return holder.elytraslot_getElytraStack();
            }
        } catch (NoClassDefFoundError ignored) {
        }
        return ItemStack.EMPTY;
    }

    public static int[] computeAnchor(int guiWidth, int guiHeight, LocalPlayer player, ArmorHudConfig cfg, int count) {
        int totalWidth = count * SLOT_PITCH + 2;
        String anchor = cfg.anchor == null ? "hotbar" : cfg.anchor;
        switch (anchor) {
            case "above_hotbar":
                return new int[]{guiWidth / 2 - totalWidth / 2, guiHeight - 29 - BAR_HEIGHT};
            case "top_left":
                return new int[]{4, 4};
            case "top_right":
                return new int[]{guiWidth - totalWidth - 4, 4};
            case "bottom_left":
                return new int[]{4, guiHeight - BAR_HEIGHT - 4};
            case "bottom_right":
                return new int[]{guiWidth - totalWidth - 4, guiHeight - BAR_HEIGHT - 4};
            default: {
                boolean right = cfg.rightSide;
                int panelX = right ? guiWidth / 2 + BAR_WIDTH / 2 : guiWidth / 2 - BAR_WIDTH / 2 - totalWidth;
                HumanoidArm offhandArm = player.getMainArm().getOpposite();
                boolean offhandOnThisSide = cfg.connectToOffhand
                        && !player.getOffhandItem().isEmpty()
                        && ((right && offhandArm == HumanoidArm.RIGHT) || (!right && offhandArm == HumanoidArm.LEFT));
                if (offhandOnThisSide) {
                    panelX += right ? OFFHAND_WIDTH : -OFFHAND_WIDTH;
                }
                return new int[]{panelX, guiHeight - BAR_HEIGHT};
            }
        }
    }

    public static void drawPanel(GuiGraphicsExtractor graphics, Minecraft minecraft, List<ItemStack> worn,
                          int panelX, int barY, boolean durability, int chestIdx, ItemStack elytra) {
        int slotsWidth = worn.size() * SLOT_PITCH;
        int textureWidth = slotsWidth + 2;


        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, BAR_WIDTH, BAR_HEIGHT,
                0, 0, panelX, barY, textureWidth - 3, BAR_HEIGHT);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, BAR_WIDTH, BAR_HEIGHT,
                BAR_WIDTH - 3, 0, panelX + textureWidth - 3, barY, 3, BAR_HEIGHT);

        for (int k = 0; k < worn.size(); k++) {
            ItemStack stack = worn.get(k);
            int itemX = panelX + 3 + k * SLOT_PITCH;
            int itemY = barY + 3;
            if (!stack.isEmpty()) {
                graphics.item(stack, itemX, itemY);
                if (durability) {
                    graphics.itemDecorations(minecraft.font, stack, itemX, itemY);
                }
            }
        }


        if (!elytra.isEmpty() && chestIdx >= 0) {
            int ex = panelX + 1 + chestIdx * SLOT_PITCH;
            int ey = barY - 21;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, BAR_WIDTH, BAR_HEIGHT,
                    0, 0, ex, ey, 1, BAR_HEIGHT);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, BAR_WIDTH, BAR_HEIGHT,
                    1, 0, ex + 1, ey, SLOT_PITCH, BAR_HEIGHT);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, BAR_WIDTH, BAR_HEIGHT,
                    BAR_WIDTH - 1, 0, ex + SLOT_PITCH + 1, ey, 1, BAR_HEIGHT);
            graphics.item(elytra, ex + 3, ey + 3);
            if (durability) {
                graphics.itemDecorations(minecraft.font, elytra, ex + 3, ey + 3);
            }
        }
    }
}
