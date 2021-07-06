package net.blancworks.figura.lua.api.sound;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

public class SoundAPI {

    public static HashMap<String, SoundEvent> soundEvents = new HashMap<String, SoundEvent>() {{
        for (Identifier id : Registry.SOUND_EVENT.getIds()) {
            SoundEvent type = Registry.SOUND_EVENT.get(id);
            put(id.getPath(), type);
            put(id.toString(), type);
        }
    }};


    public static Identifier getID() {
        return new Identifier("default", "sound");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("playSound", new TwoArgFunction() {
                // DEPRECATED
                @Deprecated
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    // INCREDIBLY DEPRECATED
                    if(script.soundSpawnCount > script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_SOUND_EFFECTS_ID))
                        return NIL;
                    script.soundSpawnCount++;
                    
                    SoundEvent targetEvent = soundEvents.get(arg1.checkjstring());
                    if (targetEvent == null)
                        return NIL;

                    FloatArrayList floats = LuaUtils.getFloatsFromTable(arg2.checktable());

                    if (floats.size() != 5)
                        return NIL;

                    World w = MinecraftClient.getInstance().world;

                    if (!MinecraftClient.getInstance().isPaused() && w != null) {
                        w.playSound(
                                floats.getFloat(0), floats.getFloat(1), floats.getFloat(2),
                                targetEvent, SoundCategory.PLAYERS,
                                floats.getFloat(3), floats.getFloat(4), true
                        );
                    }

                    return NIL;
                }

                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                    if(script.soundSpawnCount > script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_SOUND_EFFECTS_ID))
                        return NIL;
                    script.soundSpawnCount++;

                    SoundEvent targetEvent = soundEvents.get(arg1.checkjstring());
                    if (targetEvent == null)
                        return NIL;

                    LuaVector pos = LuaVector.checkOrNew(arg2);
                    LuaVector pitchVol = LuaVector.checkOrNew(arg3);

                    World w = MinecraftClient.getInstance().world;

                    if (!MinecraftClient.getInstance().isPaused() && w != null) {
                        w.playSound(
                                pos.x(), pos.y(), pos.z(),
                                targetEvent, SoundCategory.PLAYERS,
                                pitchVol.x(), pitchVol.y(), true
                        );
                    }

                    return NIL;
                }
            });
        }});
    }

}
