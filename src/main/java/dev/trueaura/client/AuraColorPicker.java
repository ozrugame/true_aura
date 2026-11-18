package dev.trueaura.client;

import dev.trueaura.aura.AuraColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Shared HSV state keeps the chosen hue while dragging through white or black. */
final class AuraColorPicker {
    private double hue, saturation, value;
    private final IntConsumer changed;
    private final Component channel;
    AuraColorPicker(int rgb, Component channel, IntConsumer changed) {
        AuraColor.Hsv initial = AuraColor.hsv(rgb);
        hue = initial.hue(); saturation = initial.saturation(); value = initial.value();
        this.changed = changed;
        this.channel = channel;
    }
    void build(int x, int y, int width, Consumer<AbstractWidget> add) {
        add.accept(new PickerWidget(x, y, width - 22, 68, false));
        add.accept(new PickerWidget(x + width - 14, y, 14, 68, true));
    }
    private void publish() { changed.accept(AuraColor.rgb(hue, saturation, value)); }
    private static Component text(String key) { return Component.translatable("screen.trueaura.picker." + key); }

    private final class PickerWidget extends AbstractWidget {
        private final boolean hueOnly;
        PickerWidget(int x, int y, int width, int height, boolean hueOnly) {
            super(x, y, width, height, text(hueOnly ? "hue" : "palette"));
            this.hueOnly = hueOnly;
            setTooltip(Tooltip.create(text(hueOnly ? "hue_hint" : "palette_hint")));
        }
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (hueOnly) {
                for (int part = 0; part < 6; part++) {
                    int top = getY() + part * height / 6, bottom = getY() + (part + 1) * height / 6;
                    graphics.fillGradient(getX(), top, getX() + width, bottom,
                        0xFF000000 | AuraColor.rgb(part / 6.0, 1, 1), 0xFF000000 | AuraColor.rgb((part + 1) / 6.0, 1, 1));
                }
                int mark = getY() + (int) Math.round(hue * (height - 1));
                graphics.fill(getX() - 2, mark - 1, getX() + width + 2, mark + 2, 0xFF111111);
                graphics.fill(getX() - 1, mark, getX() + width + 1, mark + 1, 0xFFFFFFFF);
            } else {
                // One vertical gradient per GUI column, not a texture upload or per-pixel mesh.
                for (int column = 0; column < width; column++) {
                    int top = 0xFF000000 | AuraColor.rgb(hue, column / (double) (width - 1), 1);
                    graphics.fillGradient(getX() + column, getY(), getX() + column + 1, getY() + height, top, 0xFF000000);
                }
                int px = getX() + (int) Math.round(saturation * (width - 1));
                int py = getY() + (int) Math.round((1 - value) * (height - 1));
                graphics.renderOutline(px - 3, py - 3, 7, 7, 0xFF111111);
                graphics.renderOutline(px - 2, py - 2, 5, 5, 0xFFFFFFFF);
            }
            if (isFocused()) graphics.renderOutline(getX() - 2, getY() - 2, width + 4, height + 4, 0xFFFFE5B3);
        }
        private void pick(double mouseX, double mouseY) {
            double vertical = AuraColor.clamp((mouseY - getY()) / (height - 1));
            if (hueOnly) hue = vertical;
            else { saturation = AuraColor.clamp((mouseX - getX()) / (width - 1)); value = 1 - vertical; }
            publish();
        }
        @Override public void onClick(double mouseX, double mouseY) { pick(mouseX, mouseY); }
        @Override protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) { pick(mouseX, mouseY); }
        @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
            if (!isFocused()) return false;
            double step = .02;
            if (key != GLFW.GLFW_KEY_LEFT && key != GLFW.GLFW_KEY_RIGHT && key != GLFW.GLFW_KEY_UP && key != GLFW.GLFW_KEY_DOWN) return false;
            if (hueOnly) {
                hue = AuraColor.clamp(hue + (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_LEFT ? -1 : 1) / 100.0);
            } else if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
                saturation = AuraColor.clamp(saturation + (key == GLFW.GLFW_KEY_LEFT ? -step : step));
            } else value = AuraColor.clamp(value + (key == GLFW.GLFW_KEY_UP ? step : -step));
            publish(); return true;
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.translatable("screen.trueaura.picker.narration",
                channel, getMessage(), Math.round(hue * 360), Math.round(saturation * 100), Math.round(value * 100)));
            output.add(NarratedElementType.USAGE, text("keyboard"));
        }
    }
}
