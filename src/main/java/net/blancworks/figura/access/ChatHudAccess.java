package net.blancworks.figura.access;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;

import java.util.List;

public interface ChatHudAccess {
    List<ChatHudLine<Text>> getMessages();
}
