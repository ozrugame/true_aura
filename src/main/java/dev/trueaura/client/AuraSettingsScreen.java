package dev.trueaura.client;

import dev.trueaura.aura.AuraSettings;
import dev.trueaura.aura.AuraShape;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/** Owns an isolated draft; only Save publishes it to the config and server. */
public final class AuraSettingsScreen extends Screen {
    private static final String[] COLOR_KEYS = {"main", "highlight", "base"};
    private final Screen parent;
    private AuraSettings draft;
    private int selectedColor;
    private int page;
    private boolean choosingShape;
    private int shapePage;
    private final AuraEffectEditor effects;
    private CameraType previousCamera;
    private Button saveButton;
    private int panelX;
    private int panelY;
    private int panelWidth;

    public double previewFov() { return 70; }
    public AuraPreviewFraming.Orbit previewOrbit(double heading) {
        return AuraPreviewFraming.orbit(heading, width, height, panelX, previewFov(), Math.max(4, draft.scale() * (draft.custom() == null ? 4 : Math.max(5, draft.custom().maxHeight(1.8) * 1.5))));
    }
    public double previewCenterHeight(float playerHeight) { return (draft.custom() == null ? playerHeight + .6 : draft.custom().maxHeight(playerHeight)) * draft.scale() / 2; }

    public AuraSettingsScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
        this.draft = AuraClient.settings();
        CustomAuraLibrary.reload();
        this.effects = new AuraEffectEditor(() -> draft, this::updateDraft, this::buildControls);
    }

    public void applyServerSettings(AuraSettings settings) {
        draft = settings;
        effects.resetText();
        AuraClient.preview(draft);
        buildControls();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(218, Math.max(148, width / 2 - 24));
        panelX = width - panelWidth - 8;
        panelY = Math.max(4, (height - 232) / 2);
        if (minecraft.level != null && previousCamera == null) {
            previousCamera = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
        AuraClient.preview(draft);
        buildControls();
    }

    private void buildControls() {
        setFocused(null);
        setDragging(false);
        clearWidgets();
        int x = panelX + 10;
        int contentWidth = panelWidth - 20;
        int tabWidth = (contentWidth - 8) / 3;
        String[] tabs = {"appearance", "colors", "effects.title"};
        for (int i = 0; i < tabs.length; i++) {
            int target = i;
            Button tab = addRenderableWidget(Button.builder(text(tabs[i]), button -> changePage(target))
                .bounds(x + i * (tabWidth + 4), panelY + 36, i == 2 ? contentWidth - 2 * (tabWidth + 4) : tabWidth, 20).build());
            tab.active = page != i;
        }

        if (page == 2) {
            effects.build(font, x, panelY, contentWidth, widget -> addRenderableWidget(widget));
        } else if (page == 1) {
            addColors(x, contentWidth);
        } else {
            addAppearance(x, contentWidth);
        }

        int buttonWidth = (contentWidth - 8) / 3;
        saveButton = addRenderableWidget(Button.builder(text("save"), button -> {
            if (effects.valid()) {
                AuraClient.saveSettings(draft);
                onClose();
            }
        }).bounds(x, panelY + 208, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(text("cancel"), button -> onClose())
                .bounds(x + buttonWidth + 4, panelY + 208, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(text("reset"), button -> {
            draft = AuraClient.limits().apply(AuraSettings.DEFAULT);
            effects.resetText();
            AuraClient.preview(draft);
            buildControls();
        }).bounds(x + 2 * (buttonWidth + 4), panelY + 208, contentWidth - 2 * (buttonWidth + 4), 20)
                .tooltip(Tooltip.create(text("reset_hint"))).build());
        saveButton.active = effects.valid();
    }

    private void addAppearance(int x, int contentWidth) {
        if (choosingShape) {
            addShapeList(x, contentWidth);
            return;
        }
        addRenderableWidget(new SettingSlider(x, panelY + 64, contentWidth, "intensity", .1, AuraClient.limits().maxIntensity(), draft.intensity(),
                value -> updateDraft(draft.withAppearance(value, draft.scale(), draft.speed(), draft.shape()))));
        addRenderableWidget(new SettingSlider(x, panelY + 90, contentWidth, "scale", .5, AuraClient.limits().maxScale(), draft.scale(),
                value -> updateDraft(draft.withAppearance(draft.intensity(), value, draft.speed(), draft.shape()))));
        addRenderableWidget(new SettingSlider(x, panelY + 116, contentWidth, "speed", .1, 3, draft.speed(),
                value -> updateDraft(draft.withAppearance(draft.intensity(), draft.scale(), value, draft.shape()))));
        addRenderableWidget(Button.builder(shapeMessage(), button -> {
            choosingShape = true;
            buildControls();
        }).bounds(x, panelY + 142, contentWidth, 20).tooltip(Tooltip.create(text("shape_hint"))).build());
    }

    private record ShapeOption(Component name, AuraShape shape, dev.trueaura.aura.CustomAura custom) { }
    private java.util.List<ShapeOption> shapeOptions() {
        var result = new java.util.ArrayList<ShapeOption>();
        for (var shape : AuraShape.values()) result.add(new ShapeOption(shapeName(shape), shape, null));
        for (var custom : CustomAuraLibrary.current().entries()) result.add(new ShapeOption(Component.literal(custom.name()), custom.pattern(), custom));
        if (draft.custom() != null && result.stream().noneMatch(o -> draft.custom().equals(o.custom())))
            result.add(new ShapeOption(Component.literal(draft.custom().name()), draft.shape(), draft.custom()));
        return result;
    }
    private void reloadShapes() {
        var report = CustomAuraLibrary.reload();
        if (draft.custom() != null) report.entries().stream().filter(d -> d.id().equals(draft.custom().id())).findFirst()
            .ifPresent(d -> updateDraft(draft.withCustom(d)));
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable("screen.trueaura.import_report", report.entries().size(), report.errors().size()), false);
            report.errors().stream().limit(8).forEach(error -> minecraft.player.displayClientMessage(Component.literal(error).withStyle(net.minecraft.ChatFormatting.RED), false));
        }
        buildControls();
    }
    private void addShapeList(int x, int contentWidth) {
        var options = shapeOptions();
        int pages = (options.size() + 7) / 8;
        shapePage = Math.max(0, Math.min(shapePage, pages - 1));
        int columnWidth = (contentWidth - 4) / 2;
        addRenderableWidget(Button.builder(text("import_folder"), button -> net.minecraft.Util.getPlatform().openFile(CustomAuraLibrary.folder().toFile()))
            .bounds(x, panelY + 64, columnWidth, 18).tooltip(Tooltip.create(Component.literal(CustomAuraLibrary.folder().toString()))).build());
        var report = CustomAuraLibrary.current();
        Component diagnostic = Component.translatable("screen.trueaura.import_report", report.entries().size(), report.errors().size());
        for (String error : report.errors().stream().limit(8).toList()) diagnostic = diagnostic.copy().append("\n").append(error);
        addRenderableWidget(Button.builder(text("import_reload"), button -> reloadShapes())
            .bounds(x + columnWidth + 4, panelY + 64, contentWidth - columnWidth - 4, 18).tooltip(Tooltip.create(diagnostic)).build());
        for (int i = 0; i < 8 && shapePage * 8 + i < options.size(); i++) {
            var option = options.get(shapePage * 8 + i);
            boolean selected = java.util.Objects.equals(draft.custom(), option.custom()) && draft.shape() == option.shape();
            Component label = selected ? Component.literal("✓ ").append(option.name()).withStyle(net.minecraft.ChatFormatting.GOLD) : option.name();
            Component tip = option.custom() == null ? option.name() : option.name().copy().append("\n" + option.custom().id());
            addRenderableWidget(Button.builder(label, button -> {
                updateDraft(draft.withShape(option.shape()).withCustom(option.custom()));
                choosingShape = false; buildControls();
            }).bounds(x + (i % 2) * (columnWidth + 4), panelY + 86 + (i / 2) * 23,
                i % 2 == 0 ? columnWidth : contentWidth - columnWidth - 4, 20).tooltip(Tooltip.create(tip)).build());
        }
        var previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> { shapePage--; buildControls(); })
            .bounds(x, panelY + 184, 22, 18).build()); previous.active = shapePage > 0;
        var next = addRenderableWidget(Button.builder(Component.literal(">"), b -> { shapePage++; buildControls(); })
            .bounds(x + contentWidth - 22, panelY + 184, 22, 18).build()); next.active = shapePage + 1 < pages;
        addRenderableWidget(Button.builder(text("shape_back").copy().append(" " + (shapePage + 1) + "/" + pages), button -> {
            choosingShape = false; buildControls();
        }).bounds(x + 26, panelY + 184, contentWidth - 52, 18).build());
    }

    private void addColors(int x, int contentWidth) {
        int third = (contentWidth - 8) / 3;
        for (int i = 0; i < 3; i++) {
            int target = i;
            Button button = addRenderableWidget(Button.builder(text("color." + COLOR_KEYS[i]), control -> {
                selectedColor = target;
                buildControls();
            }).bounds(x + i * (third + 4), panelY + 64, i == 2 ? contentWidth - 2 * (third + 4) : third, 18).build());
            button.active = selectedColor != i;
        }
        new AuraColorPicker(colorAt(selectedColor), text("color." + COLOR_KEYS[selectedColor]), color -> updateDraft(draft.withColors(
            selectedColor == 0 ? color : draft.mainColor(), selectedColor == 1 ? color : draft.highlightColor(),
            selectedColor == 2 ? color : draft.baseColor())))
            .build(x, panelY + 88, contentWidth, widget -> addRenderableWidget(widget));
        addRenderableWidget(Button.builder(text("preset.gold"), button -> preset(0xFFB30F, 0xFFF5A0, 0xB080FF))
                .bounds(x, panelY + 184, third, 18).build());
        addRenderableWidget(Button.builder(text("preset.ice"), button -> preset(0x37CFFF, 0xE2FAFF, 0x526EFF))
                .bounds(x + third + 4, panelY + 184, third, 18).build());
        addRenderableWidget(Button.builder(text("preset.rose"), button -> preset(0xFF477F, 0xFFE8F1, 0xB249FF))
                .bounds(x + 2 * (third + 4), panelY + 184, contentWidth - 2 * (third + 4), 18).build());
    }

    private int colorAt(int index) {
        return index == 0 ? draft.mainColor() : index == 1 ? draft.highlightColor() : draft.baseColor();
    }

    private void preset(int main, int highlight, int base) {
        updateDraft(draft.withColors(main, highlight, base));
        buildControls();
    }

    private void updateDraft(AuraSettings settings) {
        draft = settings;
        AuraClient.preview(draft);
    }

    private void changePage(int nextPage) {
        page = nextPage;
        choosingShape = false;
        buildControls();
    }

    private Component shapeMessage() {
        return Component.translatable("screen.trueaura.shape", draft.custom() == null ? shapeName(draft.shape()) : Component.literal(draft.custom().name()));
    }

    private static Component shapeName(AuraShape shape) {
        return text("shape." + shape.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && choosingShape) {
            choosingShape = false;
            buildControls();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void tick() {
        if (page == 2) effects.tick();
        if (saveButton != null) saveButton.active = effects.valid();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page == 2 && mouseX >= panelX && mouseY >= panelY + 110 && mouseY < panelY + 183) return effects.scroll(delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft.level == null) renderBackground(graphics);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 232, 0xDC111522);
        graphics.renderOutline(panelX, panelY, panelWidth, 232, 0xFF70619E);
        graphics.drawCenteredString(font, title, panelX + panelWidth / 2, panelY + 7, 0xFFE5B3);
        graphics.drawCenteredString(font, text(AuraClient.isShared() ? "shared" : "local"),
                panelX + panelWidth / 2, panelY + 22, 0xB5C5DF);
        int x = panelX + 10;
        if (page == 2) {
            effects.render(graphics);
        } else if (page == 1) {
            int contentWidth = panelWidth - 20;
            int third = (contentWidth - 8) / 3;
            for (int i = 0; i < 3; i++) {
                int start = x + i * (third + 4);
                graphics.fill(start, panelY + 83, start + (i == 2 ? contentWidth - 2 * (third + 4) : third),
                    panelY + 86, 0xFF000000 | colorAt(i));
            }
            graphics.drawString(font, text("picker.selected"), x, panelY + 163, 0xB5C5DF);
            graphics.fill(x + 65, panelY + 160, x + contentWidth, panelY + 173, 0xFF000000 | colorAt(selectedColor));
            graphics.renderOutline(x + 65, panelY + 160, contentWidth - 65, 13, 0xFFBBBBBB);
            graphics.drawString(font, text("picker.drag"), x, panelY + 175, 0xB5C5DF);
        } else if (!choosingShape) {
            graphics.drawWordWrap(font, text("draft_hint"), x, panelY + 170, panelWidth - 20, 0xB5C5DF);
        }
        int helpWidth = Math.max(60, panelX - 16);
        graphics.drawWordWrap(font, text(minecraft.level == null ? "world_required" : "preview_hint"),
                8, 12, helpWidth, 0xFFFFFF);
        graphics.drawWordWrap(font, Component.translatable("screen.trueaura.enabled_after",
                text(AuraClient.isAuraEnabled() ? "enabled" : "disabled")), 8, height - 44, helpWidth, 0xD7DBE8);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        AuraClient.clearPreview();
        if (previousCamera != null) {
            minecraft.options.setCameraType(previousCamera);
            previousCamera = null;
        }
        super.removed();
    }

    private static Component text(String key) {
        return Component.translatable("screen.trueaura." + key);
    }

    private static final class SettingSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer changed;

        private SettingSlider(int x, int y, int width, String label, double min, double max,
                              double initial, DoubleConsumer changed) {
            super(x, y, width, 20, Component.empty(), (max == min ? 0 : (initial - min) / (max - min)));
            this.label = label;
            this.min = min;
            this.max = max;
            this.changed = changed;
            updateMessage();
        }

        private double settingValue() {
            return min + value * (max - min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.trueaura.value", text(label),
                    String.format(Locale.ROOT, "%.2f", settingValue())));
        }

        @Override
        protected void applyValue() {
            changed.accept(settingValue());
        }
    }
}
