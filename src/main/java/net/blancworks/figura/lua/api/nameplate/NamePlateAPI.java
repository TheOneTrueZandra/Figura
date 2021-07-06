package net.blancworks.figura.lua.api.nameplate;

import com.mojang.brigadier.StringReader;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.access.FiguraTextAccess;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.blancworks.figura.lua.api.math.VectorAPI;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

public class NamePlateAPI {
    public static final String ENTITY = "ENTITY";
    public static final String CHAT = "CHAT";
    public static final String TABLIST = "LIST";

    public static Identifier getID() {
        return new Identifier("default", "nameplate");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ScriptLocalAPITable(script, new LuaTable() {{
            set(ENTITY, getTableForPart(ENTITY, script));
            set(CHAT, getTableForPart(CHAT, script));
            set(TABLIST, getTableForPart(TABLIST, script));
        }});
    }

    public static ReadOnlyLuaTable getTableForPart(String accessor, CustomScript script) {
        return new NamePlateTable(accessor, script);
    }

    private static class NamePlateTable extends ScriptLocalAPITable {
        String accessor;

        public NamePlateTable(String accessor, CustomScript script) {
            super(script);
            this.accessor = accessor;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();
            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeNameplateCustomization(accessor).position);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeNameplateCustomization(accessor).position = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetScript.getOrMakeNameplateCustomization(accessor).enabled);
                }
            });

            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    if (arg.isnil()) {
                        customization.enabled = false;
                        return NIL;
                    }

                    customization.enabled = arg.checkboolean();
                    return NIL;
                }
            });

            ret.set("getScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaVector.of(targetScript.getOrMakeNameplateCustomization(accessor).scale);
                }
            });

            ret.set("setScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetScript.getOrMakeNameplateCustomization(accessor).scale = LuaVector.checkOrNew(arg1).asV3f();
                    return NIL;
                }
            });

            ret.set("setText", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetScript.getOrMakeNameplateCustomization(accessor).text = arg.isnil() ? null : arg.checkjstring().replaceAll("[\n\r]", "").replaceAll("figura:default", "minecraft:default");
                    return NIL;
                }
            });

            ret.set("getText", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetScript.getOrMakeNameplateCustomization(accessor).text);
                }
            });

            ret.set("setColor", new OneArgFunction() {
                @Override
                @Deprecated
                public LuaValue call(LuaValue arg) {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    if (arg.isnil()) {
                        customization.color = null;
                        return NIL;
                    }

                    Vector3f color = LuaVector.checkOrNew(arg).asV3f();
                    customization.color = ((Math.round(color.getX() * 255) & 0xFF) << 16) | ((Math.round(color.getY() * 255) & 0xFF) << 8) | (Math.round(color.getZ() * 255) & 0xFF);
                    return NIL;
                }
            });

            ret.set("getColor", new ZeroArgFunction() {
                @Override
                @Deprecated
                public LuaValue call() {
                    return VectorAPI.RGBfromInt(targetScript.getOrMakeNameplateCustomization(accessor).color);
                }
            });

            ret.set("setFormatting", new OneArgFunction() {
                @Override
                @Deprecated
                public LuaValue call(LuaValue arg) {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    customization.bold = null;
                    customization.italic = null;
                    customization.underline = null;
                    customization.obfuscated = null;
                    customization.strikethrough = null;

                    if (arg.isnil())
                        return NIL;

                    LuaTable formatting = arg.checktable();

                    for (int i = 1; i <= formatting.length() && i < 6; i++) {
                        String argument = formatting.get(i).checkjstring();

                        switch (argument) {
                            case "BOLD": customization.bold = true; break;
                            case "ITALIC": customization.italic = true; break;
                            case "UNDERLINE": customization.underline = true; break;
                            case "OBFUSCATED": customization.obfuscated = true; break;
                            case "STRIKETHROUGH": customization.strikethrough = true; break;
                        }
                    }

                    return NIL;
                }
            });

            ret.set("getFormatting", new ZeroArgFunction() {
                @Override
                @Deprecated
                public LuaValue call() {
                    NamePlateCustomization customization = targetScript.getOrMakeNameplateCustomization(accessor);

                    LuaTable formatting = new LuaTable();

                    if (customization.bold != null)
                        formatting.insert(0, LuaValue.valueOf("BOLD"));
                    if (customization.italic != null)
                        formatting.insert(0, LuaValue.valueOf("ITALIC"));
                    if (customization.underline != null)
                        formatting.insert(0, LuaValue.valueOf("UNDERLINE"));
                    if (customization.obfuscated != null)
                        formatting.insert(0, LuaValue.valueOf("OBFUSCATED"));
                    if (customization.strikethrough != null)
                        formatting.insert(0, LuaValue.valueOf("STRIKETHROUGH"));

                    return formatting;
                }
            });

            return ret;
        }
    }

    public static boolean applyFormattingRecursive(LiteralText text, UUID uuid, String playerName, NamePlateCustomization nameplateData, PlayerData currentData) {
        //save siblings
        ArrayList<Text> siblings = new ArrayList<>(text.getSiblings());

        //transform already transformed text
        if (((FiguraTextAccess) text).figura$getFigura()) {
            //transform the text
            Text transformed = applyNameplateFormatting(text, uuid, nameplateData, currentData);

            //set text as transformed
            ((FiguraTextAccess) text).figura$setText(((LiteralText) transformed).getRawString());

            //flag as figura text
            ((FiguraTextAccess) text).figura$setFigura(true);

            //set text style
            text.setStyle(transformed.getStyle());

            //add sibling texts
            transformed.getSiblings().forEach(((LiteralText) text)::append);

            return true;
        }
        //otherwise transform when playername
        else if (text.getRawString().contains(playerName)) {
            //save original style
            Style style = text.getStyle();

            //split the text
            String[] textSplit = text.getRawString().split(Pattern.quote(playerName), 2);

            Text playerNameSplitted = new LiteralText(playerName).setStyle(style);

            //transform the text
            Text transformed = applyNameplateFormatting(playerNameSplitted, uuid, nameplateData, currentData);

            //return the text
            if (!textSplit[0].equals("")) {
                //set pre text
                ((FiguraTextAccess) text).figura$setText(textSplit[0]);

                //set style
                text.setStyle(style);

                //append new text
                text.append(transformed);
            }
            else {
                //set text as transformed
                ((FiguraTextAccess) text).figura$setText(((LiteralText) transformed).getRawString());

                //flag as figura text
                ((FiguraTextAccess) text).figura$setFigura(true);

                //set text style
                text.setStyle(transformed.getStyle());

                //add sibling texts
                transformed.getSiblings().forEach(((LiteralText) text)::append);
            }

            //add post text
            if (textSplit.length > 1 && !textSplit[1].equals("")) {
                text.append(textSplit[1]).setStyle(style);
            }

            //append siblings back if not from figura
            for (Text sibling : siblings) {
                if (!((FiguraTextAccess) sibling).figura$getFigura())
                    text.append(sibling);
            }

            return true;
        }
        else {
            //then iterate through children
            for (Text sibling : siblings) {
                //split args when translatable text
                if (sibling instanceof TranslatableText) {
                    Object[] args = ((TranslatableText) sibling).getArgs();

                    for (Object arg : args) {
                        if (NamePlateAPI.applyFormattingRecursive((LiteralText) arg, uuid, playerName, nameplateData, currentData)) {
                            return true;
                        }
                    }
                }
                //else check and format literal text
                else if (sibling instanceof LiteralText && applyFormattingRecursive((LiteralText) sibling, uuid, playerName, nameplateData, currentData)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Text applyNameplateFormatting(Text text, UUID uuid, NamePlateCustomization nameplateData, PlayerData currentData) {
        //dummy playername text
        MutableText formattedText = new LiteralText(((LiteralText) text).getRawString());

        //original style
        Style originalStyle = text.getStyle();
        formattedText.setStyle(originalStyle);

        //mark text as figura text
        ((FiguraTextAccess) formattedText).figura$setFigura(true);

        if (currentData != null) {
            //apply nameplate formatting
            if (nameplateData != null && currentData.getTrustContainer().getBoolSetting(PlayerTrustManager.ALLOW_NAMEPLATE_MOD_ID)) {

                //try to parse the string as json text
                //otherwise use the deprecated customization method
                try {
                    if (nameplateData.text == null)
                        throw new Exception("No text data found - using deprecated method");

                    MutableText jsonText = Text.Serializer.fromJson(new StringReader(nameplateData.text));

                    if (jsonText == null)
                        throw new Exception("Error parsing JSON string - using deprecated method");

                    ((FiguraTextAccess) formattedText).figura$setText("");
                    formattedText.append(jsonText);
                }
                catch (Exception ignored) { //deprecated
                    //set color and properties
                    if (nameplateData.color != null)
                        originalStyle = originalStyle.withColor(TextColor.fromRgb(nameplateData.color));

                    if (nameplateData.bold != null)
                        originalStyle = originalStyle.withBold(nameplateData.bold);

                    if (nameplateData.italic != null)
                        originalStyle = originalStyle.withItalic(nameplateData.italic);

                    if (nameplateData.underline != null)
                        originalStyle = originalStyle.withUnderline(nameplateData.underline);

                    if (nameplateData.strikethrough != null && nameplateData.strikethrough)
                        originalStyle = originalStyle.withFormatting(Formatting.STRIKETHROUGH);

                    if (nameplateData.obfuscated != null && nameplateData.obfuscated)
                        originalStyle = originalStyle.withFormatting(Formatting.OBFUSCATED);

                    //set text, if not null
                    if (nameplateData.text != null)
                        ((FiguraTextAccess) formattedText).figura$setText(nameplateData.text);

                    //apply new style
                    formattedText.setStyle(originalStyle);
                }
            }
        }

        //add badges
        //font
        Identifier font = (boolean) Config.entries.get("nameTagIcon").value ? FiguraMod.FIGURA_FONT : Style.DEFAULT_FONT_ID;
        String badges = " ";

        //the mark
        if (currentData != null && currentData.model != null) {
            badges += FiguraMod.IS_CHEESE ? "\uD83E\uDDC0" : PlayerDataManager.getDataForPlayer(uuid).model.getRenderComplexity() < currentData.getTrustContainer().getFloatSetting(PlayerTrustManager.MAX_COMPLEXITY_ID) ? "△" : "▲";
        }

        //special badges
        if (FiguraMod.special.contains(uuid))
            badges += "✭";

        //append badges
        if (!badges.equals(" ")) {
            //create badges text
            LiteralText badgesText = new LiteralText(badges);

            //set formatting
            badgesText.setStyle(Style.EMPTY.withExclusiveFormatting(Formatting.WHITE).withFont(font));

            //flag as figura text
            ((FiguraTextAccess) badgesText).figura$setFigura(true);

            //append
            formattedText.append(badgesText);
        }

        return formattedText;
    }
}
