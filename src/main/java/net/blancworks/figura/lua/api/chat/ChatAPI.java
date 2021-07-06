package net.blancworks.figura.lua.api.chat;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.ChatHudAccess;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

import java.util.List;

public class ChatAPI {

    public static Identifier getID() {
        return new Identifier("default", "chat");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{

            set("sendMessage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    //only send messages for local player
                    if (script.playerData == PlayerDataManager.localPlayer) {
                        ClientPlayerEntity player = MinecraftClient.getInstance().player;
                        if (player != null) player.sendChatMessage(arg.tojstring());
                    }

                    return NIL;
                }
            });

            set("getMessage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    //get message list
                    List<ChatHudLine<Text>> chat = ((ChatHudAccess) MinecraftClient.getInstance().inGameHud.getChatHud()).getMessages();

                    //access message
                    try {
                        //index - 1 to keep it with lua syntax
                        return LuaValue.valueOf(chat.get(arg.checkint() - 1).getText().getString());
                    } catch (Exception ignored) {
                        return NIL;
                    }
                }
            });

        }});
    }
}
