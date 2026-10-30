package com.example.armorhud;

import com.example.armorhud.drag.DragTargets;
import com.example.armorhud.drag.DragTargets.ArmorDragTarget;
import com.zabyt.lib.screen.HudDragScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ArmorHudConfigScreen extends Screen {

    private static final int RANGE = 200;
    private static final String[] ANCHORS = {
            "hotbar", "above_hotbar", "top_left", "top_right", "bottom_left", "bottom_right"
    };
    private static final String[] ANCHOR_NAMES = {
            "Hotbar", "Above hotbar", "Top left", "Top right", "Bottom left", "Bottom right"
    };

    private final Screen parent;
    private AbstractSliderButton xSlider;
    private AbstractSliderButton ySlider;
    private EditBox xBox;
    private EditBox yBox;

    protected ArmorHudConfigScreen(Screen parent) {
        super(Component.literal("zabyt's hud stuff"));
        this.parent = parent;
    }

    private static boolean isCustom(int v) {
        return Math.abs(v) > RANGE;
    }

    @Override
    protected void init() {
        ArmorHudConfig cfg = ArmorHudConfig.get();
        int centerX = this.width / 2;

        this.addRenderableWidget(Checkbox.builder(Component.literal("Enabled"), this.font)
                .pos(centerX - 100, 36)
                .selected(cfg.enabled)
                .onValueChange((cb, checked) -> cfg.enabled = checked)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Effect timers..."),
                        btn -> this.minecraft.gui.setScreen(new EffectTimersConfigScreen(this)))
                .bounds(centerX + 8, 36, 92, 18)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("Side: " + (cfg.rightSide ? "Right" : "Left")),
                        btn -> {
                            cfg.rightSide = !cfg.rightSide;
                            btn.setMessage(Component.literal("Side: " + (cfg.rightSide ? "Right" : "Left")));
                        })
                .bounds(centerX - 100, 60, 200, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("Anchor: " + anchorName(cfg.anchor)),
                        btn -> {
                            cfg.anchor = nextAnchor(cfg.anchor);
                            btn.setMessage(Component.literal("Anchor: " + anchorName(cfg.anchor)));
                        })
                .bounds(centerX - 100, 84, 200, 20)
                .build());

        xSlider = createOffsetSlider(centerX - 100, 112, true, cfg);
        this.addRenderableWidget(xSlider);

        ySlider = createOffsetSlider(centerX - 100, 152, false, cfg);
        this.addRenderableWidget(ySlider);

        xBox = new EditBox(this.font, centerX - 100, 112, 200, 20, Component.literal("X offset"));
        configureBox(xBox, cfg, true);
        this.addRenderableWidget(xBox);

        yBox = new EditBox(this.font, centerX - 100, 152, 200, 20, Component.literal("Y offset"));
        configureBox(yBox, cfg, false);
        this.addRenderableWidget(yBox);

        this.addRenderableWidget(Checkbox.builder(Component.literal("Connect to offhand slot"), this.font)
                .pos(centerX - 100, 184)
                .selected(cfg.connectToOffhand)
                .onValueChange((cb, checked) -> cfg.connectToOffhand = checked)
                .build());

        this.addRenderableWidget(Checkbox.builder(Component.literal("Durability bars"), this.font)
                .pos(centerX - 100, 208)
                .selected(cfg.showDurability)
                .onValueChange((cb, checked) -> cfg.showDurability = checked)
                .build());

        this.addRenderableWidget(Checkbox.builder(Component.literal("FPS counter"), this.font)
                .pos(centerX - 100, 236)
                .selected(cfg.showFps)
                .onValueChange((cb, checked) -> cfg.showFps = checked)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Drag FPS position..."),
                        btn -> this.minecraft.gui.setScreen(new HudDragScreen(this, new DragTargets.FpsDragTarget()) {
                            @Override
                            protected void onActiveChanged(boolean active) {
                                ArmorHudClient.fpsDragPreviewActive = active;
                            }
                        }))
                .bounds(centerX - 100, 258, 200, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Drag armor to position..."),
                        btn -> this.minecraft.gui.setScreen(new HudDragScreen(this, new ArmorDragTarget()) {
                            @Override
                            protected void onActiveChanged(boolean active) {
                                ArmorHudClient.dragPreviewActive = active;
                            }
                        }))
                .bounds(centerX - 100, this.height - 52, 200, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> onClose())
                .bounds(centerX - 100, this.height - 27, 200, 20)
                .build());

        refresh();
    }

    private static String anchorName(String anchor) {
        for (int i = 0; i < ANCHORS.length; i++) {
            if (ANCHORS[i].equals(anchor)) {
                return ANCHOR_NAMES[i];
            }
        }
        return ANCHOR_NAMES[0];
    }

    private static String nextAnchor(String anchor) {
        for (int i = 0; i < ANCHORS.length; i++) {
            if (ANCHORS[i].equals(anchor)) {
                return ANCHORS[(i + 1) % ANCHORS.length];
            }
        }
        return ANCHORS[0];
    }

    private AbstractSliderButton createOffsetSlider(int x, int y, boolean xAxis, ArmorHudConfig cfg) {
        int value = xAxis ? cfg.xOffset : cfg.yOffset;
        double t = (value + RANGE) / (double) (RANGE * 2);
        t = Math.max(0.0, Math.min(1.0, t));
        AbstractSliderButton slider = new AbstractSliderButton(x, y, 200, 20,
                Component.empty(), t) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(offsetLabel(xAxis, cfg)));
            }

            @Override
            protected void applyValue() {
                int v = (int) Math.round(this.value * RANGE * 2) - RANGE;
                if (xAxis) {
                    cfg.xOffset = v;
                } else {
                    cfg.yOffset = v;
                }
            }
        };
        slider.setMessage(Component.literal(offsetLabel(xAxis, cfg)));
        return slider;
    }

    private void configureBox(EditBox box, ArmorHudConfig cfg, boolean xAxis) {
        box.setMaxLength(7);
        box.setValue(String.valueOf(xAxis ? cfg.xOffset : cfg.yOffset));
        box.setResponder(s -> {
            try {
                int v = Integer.parseInt(s.trim());
                if (xAxis) {
                    cfg.xOffset = v;
                } else {
                    cfg.yOffset = v;
                }
            } catch (NumberFormatException ignored) {
            }
            refresh();
        });
        box.visible = false;
    }

    private void refresh() {
        ArmorHudConfig cfg = ArmorHudConfig.get();
        boolean xc = isCustom(cfg.xOffset);
        boolean yc = isCustom(cfg.yOffset);
        if (xSlider != null) {
            xSlider.visible = !xc;
        }
        if (ySlider != null) {
            ySlider.visible = !yc;
        }
        if (xBox != null) {
            xBox.visible = xc;
        }
        if (yBox != null) {
            yBox.visible = yc;
        }
    }

    private static String offsetLabel(boolean xAxis, ArmorHudConfig cfg) {
        int v = xAxis ? cfg.xOffset : cfg.yOffset;
        String axis = xAxis ? "X" : "Y";
        if (isCustom(v)) {
            return axis + " offset: Custom";
        }
        return axis + " offset: " + signed(v);
    }

    private static String signed(int v) {
        return v >= 0 ? "+" + v : String.valueOf(v);
    }

    @Override
    public void tick() {
        refresh();
    }

    @Override
    public void onClose() {
        ArmorHudConfig.get().save();
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        if (isCustom(ArmorHudConfig.get().xOffset)) {
            graphics.centeredText(this.font, Component.literal("X offset: Custom - type exact value"), this.width / 2, 133, 0xFFFF55);
        }
        if (isCustom(ArmorHudConfig.get().yOffset)) {
            graphics.centeredText(this.font, Component.literal("Y offset: Custom - type exact value"), this.width / 2, 173, 0xFFFF55);
        }
    }
}
