package net.blancworks.figura.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.gui.FiguraTrustScreen;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayerListWidget extends CustomListWidget<PlayerListEntry, PlayerListWidget.PlayerListWidgetEntry> {

    public static final Identifier lockTextureID = new Identifier("figura", "textures/gui/lock.png");

    public PlayerListWidget(MinecraftClient client, int width, int height, int y1, int y2, int entryHeight, TextFieldWidget searchBox, CustomListWidget list, Screen parent, CustomListWidgetState state) {
        super(client, width, height, y1, y2, entryHeight, searchBox, list, parent, state);
    }

    @Override
    protected void doFiltering(String searchTerm) {
        super.doFiltering(searchTerm);
        FiguraTrustScreen realScreen = (FiguraTrustScreen) getParent();

        HashMap<Identifier, ArrayList<PlayerListEntry>> sortedEntries = new HashMap<Identifier, ArrayList<PlayerListEntry>>();
        ArrayList<Identifier> sortedEntriesOrdered = new ArrayList<Identifier>();

        for (Identifier preset : PlayerTrustManager.defaultGroups) {
            sortedEntries.put(preset, new ArrayList<>());
            sortedEntriesOrdered.add(preset);
        }

        //Foreach player
        for (PlayerListEntry listEntry : client.getNetworkHandler().getPlayerList()) {
            if (listEntry.getProfile().getName().toLowerCase().contains(searchTerm.toLowerCase()) && listEntry.getProfile().getId() != realScreen.draggedId) {

                //Get trust container for that player
                TrustContainer container = PlayerTrustManager.getContainer(new Identifier("players", listEntry.getProfile().getId().toString()));
                Identifier groupName = container.getParentIdentifier();
                
                if(container.isHidden)
                    continue;

                //Create sorting group if need be
                if (!sortedEntries.containsKey(groupName)) {
                    sortedEntries.put(groupName, new ArrayList<PlayerListEntry>());
                    sortedEntriesOrdered.add(groupName);
                }

                //Add to sorting group
                ArrayList<PlayerListEntry> list = sortedEntries.get(groupName);
                list.add(listEntry);
            }
        }

        //For all the sorted entries
        for (Identifier id : sortedEntriesOrdered) {
            TrustContainer tc = PlayerTrustManager.getContainer(id);
            
            if(tc.isHidden)
                continue;
            ArrayList<PlayerListEntry> list = sortedEntries.get(id);

            if (tc.displayChildren) {
                addEntry(new GroupListWidgetEntry(id, this) {{
                    identifier = id.toString();
                    if (PlayerTrustManager.defaultGroups.contains(id)) {
                        Text text = new TranslatableText("gui.figura." + id.getPath());
                        displayText = new LiteralText("- ").setStyle(Style.EMPTY.withColor(TextColor.parse("gray"))).append(text);
                    } else {
                        displayText = new LiteralText(id.getPath()).setStyle(Style.EMPTY.withColor(TextColor.parse("gray")));
                    }
                }});

                for (PlayerListEntry playerListEntry : list) {
                    addEntry(new PlayerListWidgetEntry(playerListEntry, this));
                }
            } else {
                addEntry(new GroupListWidgetEntry(id, this) {{
                    identifier = id.toString();

                    if (PlayerTrustManager.defaultGroups.contains(id)) {
                        Text text = new TranslatableText("gui.figura." + id.getPath());
                        displayText = new LiteralText("+ ").setStyle(Style.EMPTY.withColor(TextColor.parse("dark_gray"))).append(text);
                    } else {
                        displayText = new LiteralText(id.getPath()).setStyle(Style.EMPTY.withColor(TextColor.parse("dark_gray")));
                    }
                }});
            }
        }

    }

    @Override
    public void select(PlayerListWidgetEntry entry) {

        if (entry instanceof GroupListWidgetEntry) {
            if (state.selected == entry.entryValue) {
                TrustContainer tc = PlayerTrustManager.getContainer((Identifier) state.selected);

                tc.displayChildren = !tc.displayChildren;

                reloadFilters();
                return;
            }
        }

        super.select(entry);

        ((FiguraTrustScreen) getParent()).permissionList.rebuild();
    }

    public class PlayerListWidgetEntry extends CustomListEntry {

        public ToggleButtonWidget toggleButton;

        public PlayerListWidgetEntry(Object obj, CustomListWidget list) {
            super(obj, list);

            Identifier id;

            if (obj instanceof PlayerListEntry)
                id = new Identifier("players", ((PlayerListEntry) obj).getProfile().getId().toString());
            else 
                id = (Identifier) obj;

            TrustContainer tc = PlayerTrustManager.getContainer(id);

            toggleButton = new ToggleButtonWidget(0, 0, 16, 16, !tc.isLocked) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    tc.isLocked = !tc.isLocked;
                    toggled = !tc.isLocked;

                    FiguraTrustScreen trustScreen = (FiguraTrustScreen) list.getParent();
                    trustScreen.permissionList.rebuild();
                }

                @Override
                public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                    MinecraftClient minecraftClient = MinecraftClient.getInstance();
                    minecraftClient.getTextureManager().bindTexture(this.texture);
                    RenderSystem.disableDepthTest();
                    int i = this.u;
                    int j = this.v;
                    if (this.toggled) {
                        i += this.pressedUOffset;
                    }

                    if (this.isHovered()) {
                        j += this.hoverVOffset;
                    }

                    matrices.push();
                    this.drawTexture(matrices, this.x, this.y, i, j, this.width, this.height, 32, 32);
                    matrices.pop();
                    RenderSystem.enableDepthTest();
                }
            };

            toggleButton.setTextureUV(0, 0, 16, 16, lockTextureID);
        }

        @Override
        public String getIdentifier() {
            PlayerListEntry entry = (PlayerListEntry) getEntryObject();
            return entry.getProfile().getId().toString();
        }

        @Override
        public Text getDisplayText() {
            PlayerListEntry entry = (PlayerListEntry) getEntryObject();
            return new LiteralText("  " + entry.getProfile().getName());
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            toggleButton.mouseMoved(mouseX, mouseY);
            super.mouseMoved(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (toggleButton.isMouseOver(mouseX, mouseY))
                return toggleButton.mouseClicked(mouseX, mouseY, button);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
            super.render(matrices, index, y, x, rowWidth, rowHeight, mouseX, mouseY, isSelected, delta);

            toggleButton.x = (x + rowWidth) - 16;
            toggleButton.y = y;
            toggleButton.render(matrices, mouseX, mouseY, delta);
        }
    }

    public class GroupListWidgetEntry extends PlayerListWidgetEntry {
        public String identifier;
        public Text displayText;

        public GroupListWidgetEntry(Object obj, CustomListWidget list) {
            super(obj, list);
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public Text getDisplayText() {
            return displayText;
        }
    }
}
