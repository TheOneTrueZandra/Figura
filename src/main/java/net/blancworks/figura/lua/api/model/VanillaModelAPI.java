package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.function.Supplier;

public class VanillaModelAPI {

    //Main body accessors
    public static final String VANILLA_HEAD = "HEAD";
    public static final String VANILLA_TORSO = "TORSO";
    public static final String VANILLA_LEFT_ARM = "LEFT_ARM";
    public static final String VANILLA_RIGHT_ARM = "RIGHT_ARM";
    public static final String VANILLA_LEFT_LEG = "LEFT_LEG";
    public static final String VANILLA_RIGHT_LEG = "RIGHT_LEG";

    //Layered accessors
    public static final String VANILLA_HAT = "HAT";
    public static final String VANILLA_JACKET = "JACKET";
    public static final String VANILLA_LEFT_SLEEVE = "LEFT_SLEEVE";
    public static final String VANILLA_RIGHT_SLEEVE = "RIGHT_SLEEVE";
    public static final String VANILLA_LEFT_PANTS = "LEFT_PANTS_LEG";
    public static final String VANILLA_RIGHT_PANTS = "RIGHT_PANTS_LEG";


    public static Identifier getID() {
        return new Identifier("default", "vanilla_model");
    }

    public static Supplier<PlayerEntityModel<?>> getCurrModel = () -> FiguraMod.currentData.vanillaModel;

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            set(VANILLA_HEAD, getTableForPart(() -> getCurrModel.get().head, VANILLA_HEAD, script));
            set(VANILLA_TORSO, getTableForPart(() -> getCurrModel.get().torso, VANILLA_TORSO, script));

            set(VANILLA_LEFT_ARM, getTableForPart(() -> getCurrModel.get().leftArm, VANILLA_LEFT_ARM, script));
            set(VANILLA_RIGHT_ARM, getTableForPart(() -> getCurrModel.get().rightArm, VANILLA_RIGHT_ARM, script));

            set(VANILLA_LEFT_LEG, getTableForPart(() -> getCurrModel.get().leftLeg, VANILLA_LEFT_LEG, script));
            set(VANILLA_RIGHT_LEG, getTableForPart(() -> getCurrModel.get().rightLeg, VANILLA_RIGHT_LEG, script));

            set(VANILLA_HAT, getTableForPart(() -> getCurrModel.get().helmet, VANILLA_HAT, script));
            set(VANILLA_JACKET, getTableForPart(() -> getCurrModel.get().jacket, VANILLA_JACKET, script));

            set(VANILLA_LEFT_SLEEVE, getTableForPart(() -> getCurrModel.get().leftSleeve, VANILLA_LEFT_SLEEVE, script));
            set(VANILLA_RIGHT_SLEEVE, getTableForPart(() -> getCurrModel.get().rightSleeve, VANILLA_RIGHT_SLEEVE, script));

            set(VANILLA_LEFT_PANTS, getTableForPart(() -> getCurrModel.get().leftPantLeg, VANILLA_LEFT_PANTS, script));
            set(VANILLA_RIGHT_PANTS, getTableForPart(() -> getCurrModel.get().rightPantLeg, VANILLA_RIGHT_PANTS, script));
        }});
    }

    public static ReadOnlyLuaTable getTableForPart(Supplier<ModelPart> part, String accessor, CustomScript script) {
        return new ModelPartTable(part, accessor, script);
    }

    public static class ModelPartTable extends ScriptLocalAPITable {
        Supplier<ModelPart> targetPart;

        public float pivotX, pivotY, pivotZ;
        public float pitch, yaw, roll;
        public boolean visible;

        String accessor;

        public ModelPartTable(Supplier<ModelPart> part, String accessor, CustomScript script) {
            super(script);
            targetPart = part;
            this.accessor = accessor;
            super.setTable(getTable(script));

            script.vanillaModelPartTables.add(this);
        }

        public LuaTable getTable(CustomScript script) {
            LuaTable ret = new LuaTable();

            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakePartCustomization(accessor).pos);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    customization.pos = LuaVector.checkOrNew(arg1).asV3f();

                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakePartCustomization(accessor).rot);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    customization.rot = LuaVector.checkOrNew(arg1).asV3f();

                    return NIL;
                }
            });

            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);

                    if (customization != null && customization.visible != null)
                        return LuaBoolean.valueOf(customization.visible);

                    return NIL;
                }
            });

            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);

                    if (arg.isnil()) {
                        customization.visible = null;
                        return NIL;
                    }

                    customization.visible = arg.checkboolean();

                    return NIL;
                }
            });


            ret.set("getOriginPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return new LuaVector(pivotX, pivotY, pivotZ);
                }
            });

            ret.set("getOriginRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return new LuaVector(pitch, yaw, roll);
                }
            });

            ret.set("getOriginEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(visible);
                }
            });

            ret.set("isOptionEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    try {
                        return LuaBoolean.valueOf(script.playerData.lastEntity.isPartVisible(PlayerModelPart.valueOf(accessor)));
                    }
                    catch (Exception ignored) {
                        return NIL;
                    }
                }
            });

            return ret;
        }

        public void updateFromPart() {
            ModelPart part = targetPart.get();
            pivotX = part.pivotX;
            pivotY = part.pivotY;
            pivotZ = part.pivotZ;

            pitch = part.pitch;
            yaw = part.yaw;
            roll = part.roll;
            visible = part.visible;
        }
    }

}
