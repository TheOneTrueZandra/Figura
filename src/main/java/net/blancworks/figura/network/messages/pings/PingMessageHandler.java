package net.blancworks.figura.network.messages.pings;

import com.google.common.io.LittleEndianDataInputStream;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.api.network.LuaNetworkReadWriter;
import net.blancworks.figura.network.messages.pubsub.ChannelMessageHandler;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;

public class PingMessageHandler extends ChannelMessageHandler {
    
    @Override
    public void handleMessage(LittleEndianDataInputStream stream) throws Exception {
        super.handleMessage(stream);
        
        short count = (short) Math.max(Math.min(stream.readShort(), 32), 0);

        //System.out.println("Got " + count + " pings");

        PlayerData data = PlayerDataManager.getDataForPlayer(senderID);
        
        for(int i = 0; i < count; i++){
            short id = stream.readShort();
            LuaValue val = LuaNetworkReadWriter.readLuaValue(stream);
            data.script.handlePing(id, val);
        }
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:ping_handle";
    }
}
