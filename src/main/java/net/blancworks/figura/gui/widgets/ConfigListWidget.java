package net.blancworks.figura.gui.widgets;

import net.blancworks.figura.Config;
import net.blancworks.figura.Config.ConfigEntry;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.gui.FiguraConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ConfigListWidget extends ElementListWidget<ConfigListWidget.Entry> {

    //screen
    private final FiguraConfigScreen parent;

    //focused binding
    private KeyBinding focusedBinding;

    //text types
    public static final Predicate<String> ANY = s -> true;
    public static final Predicate<String> INT = s -> s.matches("^[\\-+]?[0-9]*$");
    public static final Predicate<String> FLOAT = s -> s.matches("[\\-+]?[0-9]*(\\.[0-9]+)?") || s.endsWith(".") || s.isEmpty();

    public ConfigListWidget(FiguraConfigScreen parent, MinecraftClient client) {
        super(client, parent.width + 45, parent.height, 43, parent.height - 32, 20);
        this.parent = parent;

        //category title
        this.addEntry(new ConfigListWidget.CategoryEntry(new TranslatableText("gui.figura.config.nametag")));

        //entries
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.previewnametag"), new TranslatableText("gui.figura.config.tooltip.previewnametag"), Config.entries.get("previewNameTag")));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.nametagmods"), new TranslatableText("gui.figura.config.tooltip.nametagmods"), Config.entries.get("nameTagMods")));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.chatmods"), new TranslatableText("gui.figura.config.tooltip.chatmods"), Config.entries.get("chatMods")));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.listmods"), new TranslatableText("gui.figura.config.tooltip.listmods"), Config.entries.get("listMods")));
        this.addEntry(new ConfigListWidget.BooleanEntry(
                new TranslatableText("gui.figura.config.nametagicon"),
                new TranslatableText("gui.figura.config.tooltip.nametagicon", new LiteralText("△").setStyle(Style.EMPTY.withFont(FiguraMod.FIGURA_FONT))),
                Config.entries.get("nameTagIcon"))
        );

        //category title
        this.addEntry(new ConfigListWidget.CategoryEntry(new TranslatableText("gui.figura.config.misc")));

        //entries
        List<Text> buttonLocationEntries = Arrays.asList(
                new TranslatableText("gui.figura.config.buttonlocation.topleft"),
                new TranslatableText("gui.figura.config.buttonlocation.topright"),
                new TranslatableText("gui.figura.config.buttonlocation.bottomleft"),
                new TranslatableText("gui.figura.config.buttonlocation.bottomright"),
                new TranslatableText("gui.figura.config.buttonlocation.icon")
        );
        this.addEntry(new EnumEntry(new TranslatableText("gui.figura.config.buttonlocation"), new TranslatableText("gui.figura.config.tooltip.buttonlocation"), Config.entries.get("buttonLocation"), buttonLocationEntries));

        List<Text> scriptLogEntries = Arrays.asList(
                new TranslatableText("gui.figura.config.scriptlog.console_chat"),
                new TranslatableText("gui.figura.config.scriptlog.console"),
                new TranslatableText("gui.figura.config.scriptlog.chat")
        );
        this.addEntry(new EnumEntry(new TranslatableText("gui.figura.config.scriptlog"), new TranslatableText("gui.figura.config.tooltip.scriptlog"), Config.entries.get("scriptLog"), scriptLogEntries));

        //category title
        this.addEntry(new ConfigListWidget.CategoryEntry(new TranslatableText("gui.figura.config.actionwheel")));

        //entries
        this.addEntry(new KeyBindEntry(new TranslatableText("key.figura.actionwheel"), new TranslatableText("key.figura.tooltip.actionwheel"), Config.entries.get("actionWheel"), FiguraMod.actionWheel));

        List<Text> actionWheelEntries = Arrays.asList(
                new TranslatableText("gui.figura.config.actionwheelpos.mouse"),
                new TranslatableText("gui.figura.config.actionwheelpos.top"),
                new TranslatableText("gui.figura.config.actionwheelpos.bottom"),
                new TranslatableText("gui.figura.config.actionwheelpos.center")
        );
        this.addEntry(new EnumEntry(new TranslatableText("gui.figura.config.actionwheelpos"), new TranslatableText("gui.figura.config.tooltip.actionwheelpos"), Config.entries.get("actionWheelPos"), actionWheelEntries));

        //category title
        this.addEntry(new ConfigListWidget.CategoryEntry(new TranslatableText("gui.figura.config.dev").formatted(Formatting.RED)));

        //entries
        //this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.usenewnetwork"), new TranslatableText("gui.figura.config.tooltip.usenewnetwork"), Config.entries.get("useNewNetwork")));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.uselocalserver"), new TranslatableText("gui.figura.config.tooltip.uselocalserver"), Config.entries.get("useLocalServer")));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.formatscript"), new TranslatableText("gui.figura.config.tooltip.formatscript"), Config.entries.get("formatScript")));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.logothers"), new TranslatableText("gui.figura.config.tooltip.logothers"), Config.entries.get("logOthers")));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.ownnametag"), new TranslatableText("gui.figura.config.tooltip.ownnametag"), Config.entries.get("ownNameTag")));
    }

    @Override
    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 15;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 150;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.children().forEach(entry -> {
            if (entry instanceof InputEntry) {
                ((InputEntry) entry).field.setTextFieldFocused(((InputEntry) entry).field.isMouseOver(mouseX, mouseY));
            }
        });
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.focusedBinding != null) {
            this.focusedBinding.setBoundKey(keyCode == 256 ? InputUtil.UNKNOWN_KEY: InputUtil.fromKeyCode(keyCode, scanCode));
            this.focusedBinding = null;

            KeyBinding.updateKeysByCode();

            return true;
        }
        else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public class CategoryEntry extends Entry {
        //properties
        private final Text text;

        public CategoryEntry(Text text) {
            this.text = text;
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //render text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            Text text = this.text;
            int textWidth = ConfigListWidget.this.client.textRenderer.getWidth(this.text);
            float xPos = (float)(ConfigListWidget.this.client.currentScreen.width / 2 - textWidth / 2);
            int yPos = y + entryHeight;
            textRenderer.draw(matrices, text, xPos, (float)(yPos - 9 - 1), 16777215);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return false;
        }

        public List<? extends Element> children() {
            return Collections.emptyList();
        }
    }

    public class BooleanEntry extends Entry {
        //entry
        private final ConfigEntry<Boolean> config;

        //values
        private final Text display;
        private final Text tooltip;
        private final boolean initValue;

        //buttons
        private final ButtonWidget toggle;
        private final ButtonWidget reset;

        public BooleanEntry(Text display, Text tooltip, ConfigEntry<Boolean> config) {
            this.display = display;
            this.tooltip = tooltip;
            this.config = config;
            this.initValue = config.value;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 75, 20, this.display, (button) -> config.configValue = !config.configValue);

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> config.configValue = config.defaultValue);
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.display, (float) x, (float) (posY - 9 / 2), 16777215);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = this.config.configValue != this.config.defaultValue;
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 165;
            this.toggle.y = y;
            this.toggle.setMessage(new TranslatableText("gui." + (this.config.configValue ? "yes" : "no")));

            //if setting is changed
            if (this.config.configValue != this.initValue)
                this.toggle.setMessage(this.toggle.getMessage().shallowCopy().formatted(Formatting.AQUA));

            this.toggle.render(matrices, mouseX, mouseY, tickDelta);

            //overlay text
            if (isMouseOver(mouseX, mouseY) && mouseX < x + 165) {
                matrices.push();
                matrices.translate(0, 0, 599);
                parent.renderTooltip(matrices, this.tooltip, mouseX, mouseY);
                matrices.pop();
            }
        }

        public List<? extends Element> children() {
            return Arrays.asList(this.toggle, this.reset);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.toggle.mouseClicked(mouseX, mouseY, button) || this.reset.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.toggle.mouseReleased(mouseX, mouseY, button) || this.reset.mouseReleased(mouseX, mouseY, button);
        }
    }

    public class EnumEntry extends Entry {
        //entry
        private final ConfigEntry<Integer> config;

        //values
        private final Text display;
        private final Text tooltip;
        private final int initValue;
        private final List<Text> states;

        //buttons
        private final ButtonWidget toggle;
        private final ButtonWidget reset;

        public EnumEntry(Text display, Text tooltip, ConfigEntry<Integer> config, List<Text> states) {
            this.display = display;
            this.tooltip = tooltip;
            this.config = config;
            this.initValue = config.value;
            this.states = states;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 75, 20, this.display, (button) -> config.configValue = (config.configValue + 1) % states.size());

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> config.configValue = config.defaultValue);
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.display, (float) x, (float)(posY - 9 / 2), 16777215);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = !this.config.configValue.equals(this.config.defaultValue);
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 165;
            this.toggle.y = y;
            this.toggle.setMessage(states.get(this.config.configValue));

            //if setting is changed
            if (this.config.configValue != this.initValue)
                this.toggle.setMessage(this.toggle.getMessage().shallowCopy().formatted(Formatting.AQUA));

            this.toggle.render(matrices, mouseX, mouseY, tickDelta);

            //overlay text
            if (isMouseOver(mouseX, mouseY) && mouseX < x + 165) {
                matrices.push();
                matrices.translate(0, 0, 599);
                parent.renderTooltip(matrices, this.tooltip, mouseX, mouseY);
                matrices.pop();
            }
        }

        public List<? extends Element> children() {
            return Arrays.asList(this.toggle, this.reset);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.toggle.mouseClicked(mouseX, mouseY, button) || this.reset.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.toggle.mouseReleased(mouseX, mouseY, button) || this.reset.mouseReleased(mouseX, mouseY, button);
        }
    }

    public class InputEntry extends Entry {
        //entry
        private final ConfigEntry config;

        //values
        private final Text display;
        private final Text tooltip;
        private final Object initValue;

        //buttons
        private final TextFieldWidget field;
        private final ButtonWidget reset;

        public InputEntry(Text display, Text tooltip, ConfigEntry config, Predicate<String> validator) {
            this.display = display;
            this.tooltip = tooltip;
            this.config = config;
            this.initValue = config.value;

            //field
            this.field = new TextFieldWidget(ConfigListWidget.this.client.textRenderer, 0, 0, 70, 16, new LiteralText(config.configValue + ""));
            this.field.setChangedListener((fieldText) -> config.configValue = fieldText);
            this.field.setText(config.configValue + "");
            this.field.setTextPredicate(validator);

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> {
                config.configValue = config.defaultValue;
                this.field.setText(config.configValue + "");
            });
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.display, (float) x, (float)(posY - 9 / 2), 16777215);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = !this.config.configValue.equals(this.config.defaultValue + "");
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //text field
            this.field.x = x + 167;
            this.field.y = y + 2;

            //if setting is changed
            if (!this.config.configValue.equals(this.initValue + ""))
                try {
                    this.config.defaultValue.getClass().getConstructor(new Class[] {String.class}).newInstance(this.config.configValue);
                    this.field.setEditableColor(Formatting.AQUA.getColorValue());
                } catch (Exception e) {
                    this.field.setEditableColor(Formatting.RED.getColorValue());
                }
            else
                this.field.setEditableColor(Formatting.WHITE.getColorValue());

            this.field.render(matrices, mouseX, mouseY, tickDelta);

            //overlay text
            if (isMouseOver(mouseX, mouseY) && mouseX < x + 165) {
                matrices.push();
                matrices.translate(0, 0, 599);
                parent.renderTooltip(matrices, this.tooltip, mouseX, mouseY);
                matrices.pop();
            }
        }

        public List<? extends Element> children() {
            return Arrays.asList(this.field, this.reset);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.field.mouseClicked(mouseX, mouseY, button) || this.reset.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.field.mouseReleased(mouseX, mouseY, button) || this.reset.mouseReleased(mouseX, mouseY, button);
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return super.keyPressed(keyCode, scanCode, modifiers) || this.field.keyPressed(keyCode, scanCode, modifiers);
        }

        public boolean charTyped(char chr, int modifiers) {
            return this.field.charTyped(chr, modifiers);
        }
    }

    public class KeyBindEntry extends Entry {
        //entry
        private final ConfigEntry<Integer> config;

        //values
        private final Text display;
        private final Text tooltip;
        private final KeyBinding binding;

        //buttons
        private final ButtonWidget toggle;
        private final ButtonWidget reset;

        public KeyBindEntry(Text display, Text tooltip, ConfigEntry<Integer> config, KeyBinding binding) {
            this.display = display;
            this.tooltip = tooltip;
            this.config = config;
            this.binding = binding;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 75, 20, this.display, (button) -> focusedBinding = binding);

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> {
                binding.setBoundKey(binding.getDefaultKey());
                KeyBinding.updateKeysByCode();
            });
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            textRenderer.draw(matrices, this.display, (float) x, (float) (posY - 9 / 2), 16777215);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = !this.binding.isDefault();
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 165;
            this.toggle.y = y;
            this.toggle.setMessage(this.binding.getBoundKeyLocalizedText());

            if (focusedBinding == this.binding) {
                this.toggle.setMessage((new LiteralText("> ")).append(this.toggle.getMessage().shallowCopy().formatted(Formatting.AQUA)).append(" <").formatted(Formatting.AQUA));
            }
            else if (!this.binding.isUnbound()) {
                for (KeyBinding key : MinecraftClient.getInstance().options.keysAll) {
                    if (key != this.binding && this.binding.equals(key)) {
                        this.toggle.setMessage(this.toggle.getMessage().shallowCopy().formatted(Formatting.RED));
                        break;
                    }
                }
            }

            this.toggle.render(matrices, mouseX, mouseY, tickDelta);

            //overlay text
            if (isMouseOver(mouseX, mouseY) && mouseX < x + 165) {
                matrices.push();
                matrices.translate(0, 0, 599);
                parent.renderTooltip(matrices, this.tooltip, mouseX, mouseY);
                matrices.pop();
            }
        }

        public List<? extends Element> children() {
            return Arrays.asList(this.toggle, this.reset);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return this.toggle.mouseClicked(mouseX, mouseY, button) || this.reset.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return this.toggle.mouseReleased(mouseX, mouseY, button) || this.reset.mouseReleased(mouseX, mouseY, button);
        }
    }

    public abstract static class Entry extends ElementListWidget.Entry<Entry> {
        public Entry() {}
    }
}