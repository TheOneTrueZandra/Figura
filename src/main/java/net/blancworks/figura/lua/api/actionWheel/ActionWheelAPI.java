package net.blancworks.figura.lua.api.actionWheel;

import net.blancworks.figura.gui.ActionWheel;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ActionWheelAPI {
    public static final String SLOT_1 = "SLOT_1";
    public static final String SLOT_2 = "SLOT_2";
    public static final String SLOT_3 = "SLOT_3";
    public static final String SLOT_4 = "SLOT_4";
    public static final String SLOT_5 = "SLOT_5";
    public static final String SLOT_6 = "SLOT_6";
    public static final String SLOT_7 = "SLOT_7";
    public static final String SLOT_8 = "SLOT_8";

    public static Identifier getID() {
        return new Identifier("default", "action_wheel");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            set(SLOT_1, getTableForPart(SLOT_1, script));
            set(SLOT_2, getTableForPart(SLOT_2, script));
            set(SLOT_3, getTableForPart(SLOT_3, script));
            set(SLOT_4, getTableForPart(SLOT_4, script));
            set(SLOT_5, getTableForPart(SLOT_5, script));
            set(SLOT_6, getTableForPart(SLOT_6, script));
            set(SLOT_7, getTableForPart(SLOT_7, script));
            set(SLOT_8, getTableForPart(SLOT_8, script));

            set("setLeftSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int size = arg.checkint();
                    script.actionWheelLeftSize = MathHelper.clamp(size, 1, 4);
                    return NIL;
                }
            });

            set("setRightSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int size = arg.checkint();
                    script.actionWheelRightSize = MathHelper.clamp(size, 1, 4);
                    return NIL;
                }
            });

            set("getLeftSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.actionWheelLeftSize);
                }
            });

            set("getRightSize", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(script.actionWheelRightSize);
                }
            });

            set("getSelectedSlot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    int selected = ActionWheel.selectedSlot;
                    return selected == -1 ? NIL : LuaValue.valueOf(selected + 1);
                }
            });

        }});
    }

    public static ReadOnlyLuaTable getTableForPart(String accessor, CustomScript script) {
        return new ActionWheelTable(accessor, script);
    }

    private static class ActionWheelTable extends ScriptLocalAPITable {
        String accessor;

        public ActionWheelTable(String accessor, CustomScript script) {
            super(script);
            this.accessor = accessor;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();
            ret.set("getFunction", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return targetScript.getOrMakeActionWheelCustomization(accessor).function;
                }
            });

            ret.set("setFunction", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).function = arg1.isnil() ? null : arg1.checkfunction();
                    return NIL;
                }
            });

            ret.set("getItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return ItemStackAPI.getTable(targetScript.getOrMakeActionWheelCustomization(accessor).item);
                }
            });

            ret.set("setItem", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    if (arg1.isnil()) {
                        targetScript.getOrMakeActionWheelCustomization(accessor).item = null;
                        return NIL;
                    }

                    ItemStack item = (ItemStack) arg1.get("stack").touserdata(ItemStack.class);
                    if (item == null)
                        throw new LuaError("Not a ItemStack table!");

                    targetScript.getOrMakeActionWheelCustomization(accessor).item = item;
                    return NIL;
                }
            });

            ret.set("getHoverItem", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return ItemStackAPI.getTable(targetScript.getOrMakeActionWheelCustomization(accessor).hoverItem);
                }
            });

            ret.set("setHoverItem", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (arg.isnil()) {
                        targetScript.getOrMakeActionWheelCustomization(accessor).hoverItem = null;
                        return NIL;
                    }

                    ItemStack hoverItem = (ItemStack) arg.get("stack").touserdata(ItemStack.class);
                    if (hoverItem == null)
                        throw new LuaError("Not a ItemStack table!");

                    targetScript.getOrMakeActionWheelCustomization(accessor).hoverItem = hoverItem;
                    return NIL;
                }
            });

            ret.set("getColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeActionWheelCustomization(accessor).color);
                }
            });

            ret.set("setColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).color = LuaVector.checkOrNew(arg).asV3f();
                    return NIL;
                }
            });

            ret.set("getHoverColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeActionWheelCustomization(accessor).hoverColor);
                }
            });

            ret.set("setHoverColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).hoverColor = LuaVector.checkOrNew(arg).asV3f();
                    return NIL;
                }
            });

            ret.set("getTitle", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(targetScript.getOrMakeActionWheelCustomization(accessor).title);
                }
            });

            ret.set("setTitle", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeActionWheelCustomization(accessor).title = arg1.isnil() ? null : arg1.checkjstring();
                    return NIL;
                }
            });

            ret.set("clear", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    targetScript.getOrMakeActionWheelCustomization(accessor).function = null;
                    targetScript.getOrMakeActionWheelCustomization(accessor).item = null;
                    targetScript.getOrMakeActionWheelCustomization(accessor).hoverItem = null;
                    targetScript.getOrMakeActionWheelCustomization(accessor).color = null;
                    targetScript.getOrMakeActionWheelCustomization(accessor).hoverColor = null;
                    targetScript.getOrMakeActionWheelCustomization(accessor).title = null;
                    return NIL;
                }
            });

            return ret;
        }
    }
}
