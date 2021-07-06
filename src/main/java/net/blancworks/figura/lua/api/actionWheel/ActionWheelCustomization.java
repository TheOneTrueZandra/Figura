package net.blancworks.figura.lua.api.actionWheel;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import org.luaj.vm2.LuaFunction;

public class ActionWheelCustomization {
    public LuaFunction function;
    public ItemStack item;
    public ItemStack hoverItem;
    public Vector3f color;
    public Vector3f hoverColor;
    public String title;
}
