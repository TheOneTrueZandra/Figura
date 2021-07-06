package net.blancworks.figura.network.messages.pings;

import com.google.common.io.LittleEndianDataOutputStream;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.network.LuaNetworkReadWriter;
import net.blancworks.figura.network.messages.MessageSender;
import org.luaj.vm2.LuaValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class PingMessageSender extends MessageSender {

    public Queue<CustomScript.LuaPing> pingSet = new LinkedList<>();
    
    public PingMessageSender(Queue<CustomScript.LuaPing> pings){
        pingSet.addAll(pings);
        pings.clear();
    }

    @Override
    protected void write(LittleEndianDataOutputStream stream) throws IOException {
        super.write(stream);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        LittleEndianDataOutputStream outputStream = new LittleEndianDataOutputStream(bos);

        outputStream.writeShort(pingSet.size());
        
        //System.out.println("Wrote " + pingSet.size() + " pings");
        
        for(int i = 0; i < pingSet.size(); i++){
            CustomScript.LuaPing p = pingSet.poll();
            outputStream.writeShort(p.functionID);
            try {


                LuaNetworkReadWriter.writeLuaValue(p.args, outputStream);

                byte[] data = bos.toByteArray();

                outputStream.writeInt(data.length);
                outputStream.write(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        byte[] dataArray = bos.toByteArray();
        
        stream.writeInt(dataArray.length);
        stream.write(dataArray);
    }

    @Override
    public String getProtocolName() {
        return "figura_v1:ping";
    }
}
