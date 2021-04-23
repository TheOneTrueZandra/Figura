package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLoginNetworkHandler.class)
public class ClientLoginNetworkHandlerMixin {
    @Inject(at = @At("HEAD"), method = "onDisconnected(Lnet/minecraft/text/Text;)V")
    public void onDisconnected(Text reason, CallbackInfo ci) {
        FiguraMod.networkManager.parseKickAuthMessage(reason);
    }
}
