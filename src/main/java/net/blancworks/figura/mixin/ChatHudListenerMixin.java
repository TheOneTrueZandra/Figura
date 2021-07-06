package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudListener;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ChatHudListener.class)
public class ChatHudListenerMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onChatMessage(MessageType type, Text message, UUID senderUuid, CallbackInfo ci) {
        if (!(boolean) Config.entries.get("chatMods").value)
            return;

        String playerName = "";
        UUID uuid = senderUuid;

        //get player profile
        PlayerListEntry playerEntry = this.client.player.networkHandler.getPlayerListEntry(senderUuid);

        if (playerEntry == null) {
            String textString = message.getString();
            for (String part : textString.split("(§.)|[^\\w]")) {
                if (part.isEmpty())
                    continue;

                PlayerListEntry entry = this.client.player.networkHandler.getPlayerListEntry(part);
                if (entry != null) {
                    playerName = entry.getProfile().getName();
                    uuid = entry.getProfile().getId();
                    break;
                }
            }
        } else {
            playerName = playerEntry.getProfile().getName();
        }

        //get player data
        PlayerData currentData = PlayerDataManager.getDataForPlayer(uuid);

        //player not found or no data
        if (playerName.equals("") || currentData == null)
            return;

        //apply formatting
        NamePlateCustomization nameplateData = currentData.script == null ? null : currentData.script.nameplateCustomizations.get(NamePlateAPI.CHAT);

        try {
            if (message instanceof TranslatableText) {
                Object[] args = ((TranslatableText) message).getArgs();

                for (Object arg : args) {
                    if (NamePlateAPI.applyFormattingRecursive((LiteralText) arg, uuid, playerName, nameplateData, currentData))
                        break;
                }
            } else if (message instanceof LiteralText) {
                NamePlateAPI.applyFormattingRecursive((LiteralText) message, uuid, playerName, nameplateData, currentData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
