package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.network.MessageType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "addChatMessage", at = @At("HEAD"))
    private void addChatMessage(MessageType type, Text message, UUID senderUuid, CallbackInfo ci) {
        if (!Config.nameTagMark.value)
            return;
        
        if (senderUuid != Util.NIL_UUID && message instanceof TranslatableText && ((TranslatableText) message).getKey().equals("chat.type.text")) {
            Object[] args = ((TranslatableText) message).getArgs();
            if (args.length > 0 && args[0] instanceof MutableText) {
                MutableText playerName = ((MutableText) args[0]);
                playerName.append(PlayerDataManager.getDataForPlayer(senderUuid).getNameDecorations());
            }
        }
    }
}
