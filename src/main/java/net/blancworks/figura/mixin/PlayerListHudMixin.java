package net.blancworks.figura.mixin;

import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(at = @At("RETURN"), method = "getPlayerName", cancellable = true)
    private void getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!Config.nameTagMark.value)
            return;
        
        BaseText text = (BaseText) cir.getReturnValue();
        text.append(PlayerDataManager.getDataForPlayer(entry.getProfile().getId()).getNameDecorations());
        cir.setReturnValue(text);
    }
}
