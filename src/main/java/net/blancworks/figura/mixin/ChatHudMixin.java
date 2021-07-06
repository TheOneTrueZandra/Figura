package net.blancworks.figura.mixin;

import net.blancworks.figura.access.ChatHudAccess;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ChatHud.class)
public class ChatHudMixin implements ChatHudAccess {

    @Shadow @Final private List<ChatHudLine<Text>> messages;

    @Override
    public List<ChatHudLine<Text>> getMessages() {
        return this.messages;
    }
}
