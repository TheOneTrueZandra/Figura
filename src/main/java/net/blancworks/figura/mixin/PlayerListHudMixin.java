package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.api.nameplate.NamePlateAPI;
import net.blancworks.figura.lua.api.nameplate.NamePlateCustomization;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(at = @At("RETURN"), method = "getPlayerName", cancellable = true)
    private void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text text = cir.getReturnValue();

        if ((boolean) Config.entries.get("listMods").value) {
            UUID uuid = entry.getProfile().getId();
            String playerName = entry.getProfile().getName();

            PlayerData currentData = PlayerDataManager.getDataForPlayer(uuid);
            if (currentData != null && !playerName.equals("")) {
                NamePlateCustomization nameplateData = currentData.script == null ? null : currentData.script.nameplateCustomizations.get(NamePlateAPI.TABLIST);

                try {
                    if (text instanceof TranslatableText) {
                        Object[] args = ((TranslatableText) text).getArgs();

                        for (Object arg : args) {
                            if (NamePlateAPI.applyFormattingRecursive((LiteralText) arg, uuid, playerName, nameplateData, currentData))
                                break;
                        }
                    } else if (text instanceof LiteralText) {
                        NamePlateAPI.applyFormattingRecursive((LiteralText) text, uuid, playerName, nameplateData, currentData);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        cir.setReturnValue(text);
    }
}
