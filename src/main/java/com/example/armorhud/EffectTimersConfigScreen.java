package com.example.armorhud;

import com.example.armorhud.drag.DragTargets.EffectsDragTarget;
import com.zabyt.lib.screen.HudDragScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EffectTimersConfigScreen extends Screen {

    private static final int RANGE = 200;

    private final Screen parent;
    private AbstractSliderButton xSlider;
    private AbstractSliderButton ySlider;
    private AbstractSliderButton redSlider;
    private AbstractSliderButton pulseSlider;
    private EditBox xBox;
    private EditBox yBox;

    protected EffectTimersConfigScreen(Screen parent) {
        super(Component.literal("Effect Timers"));
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
                .selected(cfg.showEffectTimers)
                .onValueChange((cb, checked) -> cfg.showEffectTimers = checked)
                .build());

        this.addRenderableWidget(Checkbox.builder(Component.literal("Tint icons red"), this.font)
                .pos(centerX + 8, 36)
                .selected(cfg.tintEffectIcons)
                .onValueChange((cb, checked) -> cfg.tintEffectIcons = checked)
                .build());

        redSlider = createThresholdSlider(centerX - 100, 64, true, cfg);
        this.addRenderableWidget(redSlider);

        pulseSlider = createThresholdSlider(centerX - 100, 92, false, cfg);
        this.addRenderableWidget(pulseSlider);

        xSlider = createOffsetSlider(centerX - 100, 124, true, cfg);
        this.addRenderableWidget(xSlider);

        ySlider = createOffsetSlider(centerX - 100, 164, false, cfg);
        this.addRenderableWidget(ySlider);

        xBox = new EditBox(this.font, centerX - 100, 124, 200, 20, Component.literal("X offset"));
        configureOffsetBox(xBox, cfg, true);
        this.addRenderableWidget(xBox);

        yBox = new EditBox(this.font, centerX - 100, 164, 200, 20, Component.literal("Y offset"));
        configureOffsetBox(yBox, cfg, false);
        this.addRenderableWidget(yBox);

        this.addRenderableWidget(Button.builder(Component.literal("Drag to position..."),
                        btn -> this.minecraft.gui.setScreen(new HudDragScreen(this, new EffectsDragTarget()) {
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

    private AbstractSliderButton createThresholdSlider(int x, int y, boolean red, ArmorHudConfig cfg) {
        int min = red ? 5 : 1;
        int max = red ? 60 : 20;
        int value = Math.max(min, Math.min(max, red ? cfg.timerRedSeconds : cfg.timerPulseSeconds));
        double t = (value - min) / (double) (max - min);
        String label = thresholdLabel(red, cfg, min, max);
        AbstractSliderButton slider = new AbstractSliderButton(x, y, 200, 20,
                Component.empty(), t) {
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(thresholdLabel(red, cfg, min, max)));
            }

            @Override
            protected void applyValue() {
                int v = min + (int) Math.round(this.value * (max - min));
                if (red) {
                    cfg.timerRedSeconds = v;
                } else {
                    cfg.timerPulseSeconds = v;
                }
            }
        };
        slider.setMessage(Component.literal(label));
        return slider;
    }

    private static String thresholdLabel(boolean red, ArmorHudConfig cfg, int min, int max) {
        int v = Math.max(min, Math.min(max, red ? cfg.timerRedSeconds : cfg.timerPulseSeconds));
        return red ? ("Red below: " + v + "s") : ("Pulse below: " + v + "s");
    }

    private AbstractSliderButton createOffsetSlider(int x, int y, boolean xAxis, ArmorHudConfig cfg) {
        int value = xAxis ? cfg.effectXOffset : cfg.effectYOffset;
        double t = Math.max(0.0, Math.min(1.0, (value + RANGE) / (double) (RANGE * 2)));
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
                    cfg.effectXOffset = v;
                } else {
                    cfg.effectYOffset = v;
                }
            }
        };
        slider.setMessage(Component.literal(offsetLabel(xAxis, cfg)));
        return slider;
    }

    private void configureOffsetBox(EditBox box, ArmorHudConfig cfg, boolean xAxis) {
        box.setMaxLength(7);
        box.setValue(String.valueOf(xAxis ? cfg.effectXOffset : cfg.effectYOffset));
        box.setResponder(s -> {
            try {
                int v = Integer.parseInt(s.trim());
                if (xAxis) {
                    cfg.effectXOffset = v;
                } else {
                    cfg.effectYOffset = v;
                }
            } catch (NumberFormatException ignored) {
            }
            refresh();
        });
        box.visible = false;
    }

    private void refresh() {
        ArmorHudConfig cfg = ArmorHudConfig.get();
        boolean xc = isCustom(cfg.effectXOffset);
        boolean yc = isCustom(cfg.effectYOffset);
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
        int v = xAxis ? cfg.effectXOffset : cfg.effectYOffset;
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
        ArmorHudConfig cfg = ArmorHudConfig.get();
        if (isCustom(cfg.effectXOffset)) {
            graphics.centeredText(this.font, Component.literal("X offset: Custom - type exact value"), this.width / 2, 145, 0xFFFF55);
        }
        if (isCustom(cfg.effectYOffset)) {
            graphics.centeredText(this.font, Component.literal("Y offset: Custom - type exact value"), this.width / 2, 185, 0xFFFF55);
        }
    }
}
