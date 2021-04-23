package net.blancworks.figura.gui.widgets;

import com.google.common.collect.ImmutableList;
import net.blancworks.figura.Config;
import net.blancworks.figura.Config.ConfigEntry;
import net.blancworks.figura.gui.FiguraConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigListWidget extends ElementListWidget<ConfigListWidget.Entry> {

    private final FiguraConfigScreen parent;

    public ConfigListWidget(FiguraConfigScreen parent, MinecraftClient client) {
        super(client, parent.width + 45, parent.height, 43, parent.height - 32, 20);
        this.parent = parent;

        //category title
        this.addEntry(new ConfigListWidget.CategoryEntry(new TranslatableText("gui.figura.config.nametag")));

        //entries
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.previewnametag"), new TranslatableText("gui.figura.config.tooltip.previewnametag"), Config.previewNameTag));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.nametagmark"), new TranslatableText("gui.figura.config.tooltip.nametagmark"), Config.nameTagMark));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.nametagmarkcolored"), new TranslatableText("gui.figura.config.tooltip.nametagmarkcolored"), Config.nameTagMarkColored));

        //category title
        this.addEntry(new ConfigListWidget.CategoryEntry(new TranslatableText("gui.figura.config.misc")));

        //entries
        List<Text> buttonLocationEntries = Arrays.asList(
                new TranslatableText("gui.figura.config.buttonlocation.topleft"),
                new TranslatableText("gui.figura.config.buttonlocation.topright"),
                new TranslatableText("gui.figura.config.buttonlocation.bottomleft"),
                new TranslatableText("gui.figura.config.buttonlocation.bottomright")
        );
        this.addEntry(new MultiStateEntry(new TranslatableText("gui.figura.config.buttonlocation"), new TranslatableText("gui.figura.config.tooltip.buttonlocation"), Config.buttonLocation, buttonLocationEntries));
        
        
        //category title
        this.addEntry(new ConfigListWidget.CategoryEntry(new TranslatableText("gui.figura.config.dev").formatted(Formatting.RED)));

        //entries
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.usenewnetwork"), new TranslatableText("gui.figura.config.tooltip.usenewnetwork"), Config.useNewNetwork));
        this.addEntry(new ConfigListWidget.BooleanEntry(new TranslatableText("gui.figura.config.uselocalserver"), new TranslatableText("gui.figura.config.tooltip.uselocalserver"), Config.useLocalServer));
    }

    @Override
    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 15;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 150;
    }

    public class CategoryEntry extends ConfigListWidget.Entry {
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
            ConfigListWidget.this.client.textRenderer.getClass();
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

    public class BooleanEntry extends ConfigListWidget.Entry {
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
            this.toggle = new ButtonWidget(0, 0, 75, 20, this.display, (button) -> {
                config.value = !config.value;
                Config.saveConfig();
            });

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> {
                config.value = config.defaultValue;
                Config.saveConfig();
            });
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            ConfigListWidget.this.client.textRenderer.getClass();
            textRenderer.draw(matrices, this.display, (float) x, (float) (posY - 9 / 2), 16777215);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = this.config.value != this.config.defaultValue;
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 165;
            this.toggle.y = y;
            this.toggle.setMessage(new TranslatableText("gui." + (this.config.value ? "yes" : "no")));

            //if setting is changed
            if (this.config.value != this.initValue)
                this.toggle.setMessage(this.toggle.getMessage().shallowCopy().formatted(Formatting.UNDERLINE));

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
            return ImmutableList.of(this.toggle, this.reset);
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

    public class MultiStateEntry extends ConfigListWidget.Entry {
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

        public MultiStateEntry(Text display, Text tooltip, ConfigEntry<Integer> config, List<Text> states) {
            this.display = display;
            this.tooltip = tooltip;
            this.config = config;
            this.initValue = config.value % states.size();
            this.states = states;

            //toggle button
            this.toggle = new ButtonWidget(0, 0, 75, 20, this.display, (button) -> {
                config.value = (config.value + 1) % states.size();
                Config.saveConfig();
            });

            //reset button
            this.reset = new ButtonWidget(0, 0, 50, 20, new TranslatableText("controls.reset"), (button) -> {
                config.value = config.defaultValue;
                Config.saveConfig();
            });
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            //text
            TextRenderer textRenderer = ConfigListWidget.this.client.textRenderer;
            int posY = y + entryHeight / 2;
            ConfigListWidget.this.client.textRenderer.getClass();
            textRenderer.draw(matrices, this.display, (float) x, (float)(posY - 9 / 2), 16777215);

            //reset button
            this.reset.x = x + 250;
            this.reset.y = y;
            this.reset.active = !this.config.value.equals(this.config.defaultValue);
            this.reset.render(matrices, mouseX, mouseY, tickDelta);

            //toggle button
            this.toggle.x = x + 165;
            this.toggle.y = y;
            this.toggle.setMessage(states.get(this.config.value));

            //if setting is changed
            if (this.config.value != this.initValue)
                this.toggle.setMessage(this.toggle.getMessage().shallowCopy().formatted(Formatting.UNDERLINE));

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
            return ImmutableList.of(this.toggle, this.reset);
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

    public abstract static class Entry extends ElementListWidget.Entry<ConfigListWidget.Entry> {
        public Entry() {}
    }
}
