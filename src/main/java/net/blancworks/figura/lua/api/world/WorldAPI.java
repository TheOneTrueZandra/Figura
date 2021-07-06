package net.blancworks.figura.lua.api.world;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.lua.api.world.block.BlockStateAPI;
import net.blancworks.figura.lua.api.world.entity.PlayerEntityAPI;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class WorldAPI {

    private static World getWorld(){
        return MinecraftClient.getInstance().world;
    }

    private static ReadOnlyLuaTable globalLuaTable;


    public static Identifier getID() {
        return new Identifier("default", "world");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        if(globalLuaTable == null)
            updateGlobalTable();
        return globalLuaTable;
    }

    public static void updateGlobalTable(){
        globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
            set("getBlockState", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    World w = getWorld();

                    if (w.getChunk(pos) == null) return NIL;

                    BlockState state = w.getBlockState(pos);

                    return BlockStateAPI.getTable(state);
                }
            });

            set("getBlockTags", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    World w = getWorld();

                    if (w.getChunk(pos) == null) return NIL;

                    BlockState state = w.getBlockState(pos);

                    LuaTable table = new LuaTable();
                    BlockTags.getTagGroup().getTagsFor(state.getBlock()).forEach(identifier -> table.insert(0, LuaValue.valueOf(String.valueOf(identifier))));

                    return new ReadOnlyLuaTable(table);
                }
            });

            set("getRedstonePower", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    World w = getWorld();

                    if (w.getChunk(pos) == null) return NIL;

                    return LuaNumber.valueOf(w.getReceivedRedstonePower(pos));
                }
            });

            set("getStrongRedstonePower", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaVector vec = LuaVector.checkOrNew(arg);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    World w = getWorld();

                    if (w.getChunk(pos) == null) return NIL;

                    return LuaNumber.valueOf(w.getReceivedStrongRedstonePower(pos));
                }
            });

            set("getTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(getWorld().getTime());
                }
            });

            set("getTimeOfDay", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(getWorld().getTimeOfDay());
                }
            });

            set("getMoonPhase", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(getWorld().getMoonPhase());
                }
            });

            //Looks like this just... Gets the time??? Confused on this one.
            set("getLunarTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(getWorld().getLunarTime());
                }
            });

            set("getRainGradient", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    return LuaNumber.valueOf(getWorld().getRainGradient((float)(a.checkdouble())));
                }
            });

            set("isLightning", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    return LuaBoolean.valueOf(getWorld().isThundering());
                }
            });

            set("getLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    getWorld().calculateAmbientDarkness();
                    int dark = getWorld().getAmbientDarkness();
                    int realLight = getWorld().getLightingProvider().getLight(pos, dark);

                    return LuaInteger.valueOf(realLight);
                }
            });

            set("getSkyLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    return LuaInteger.valueOf(getWorld().getLightLevel(LightType.SKY, pos));
                }
            });

            set("getBlockLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    return LuaInteger.valueOf(getWorld().getLightLevel(LightType.BLOCK, pos));
                }
            });

            set("getBiomeID", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {

                    LuaVector vec = LuaVector.checkOrNew(a);
                    BlockPos pos = new BlockPos(vec.asV3iFloored());

                    if (getWorld().getChunk(pos) == null) return NIL;

                    Biome b = getWorld().getBiome(pos);

                    if (b == null)
                        return NIL;

                    Identifier id = getWorld().getRegistryManager().get(Registry.BIOME_KEY).getId(b);

                    if (id == null)
                        return NIL;

                    return LuaString.valueOf(id.toString());
                }
            });

            set("getFiguraPlayers", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable playerList = new LuaTable();

                    getWorld().getPlayers().forEach(entity -> {
                        PlayerData data = PlayerDataManager.getDataForPlayer(entity.getUuid());

                        if (data != null && data.model != null)
                            playerList.insert(0, new PlayerEntityAPI.PlayerEntityLuaAPITable(() -> entity).getTable());
                    });

                    return playerList;
                }
            });
        }});
    }

}
