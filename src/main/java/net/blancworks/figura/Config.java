package net.blancworks.figura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class Config {
    public static final Map<String, ConfigEntry> entries = new HashMap<>();

    private static final File file = new File(FabricLoader.getInstance().getConfigDir().resolve("figura.json").toString());

    public static void initialize() {
        setDefaults();
        loadConfig();
        saveConfig();
    }

    public static void loadConfig() {
        try {
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                JsonObject json = new JsonParser().parse(br).getAsJsonObject();

                for (Map.Entry<String, ConfigEntry> entryMap : entries.entrySet()) {
                    ConfigEntry entry = entryMap.getValue();

                    try {
                        String jsonValue = json.getAsJsonPrimitive(entryMap.getKey()).getAsString();
                        entry.setValue(jsonValue);

                        if (entry.modValue != null) {
                            int value = Integer.parseInt(jsonValue) % (int) entry.modValue;
                            if (value < 0) value += (int) entry.modValue;
                            entry.setValue(String.valueOf(value));
                        }
                        else {
                            entry.setValue(jsonValue);
                        }
                    } catch (Exception e) {
                        entry.value = entry.defaultValue;
                    }
                }

                br.close();
            }
        } catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to load config file! Generating a new one...");
            e.printStackTrace();
            setDefaults();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            JsonObject config = new JsonObject();

            for (Map.Entry<String, ConfigEntry> entry : entries.entrySet()) {
                if (entry.getValue().value instanceof Number)
                    config.addProperty(entry.getKey(), (Number) entry.getValue().value);
                else if (entry.getValue().value instanceof Boolean)
                    config.addProperty(entry.getKey(), (boolean) entry.getValue().value);
                else
                    config.addProperty(entry.getKey(), String.valueOf(entry.getValue().value));
            }

            FileWriter fileWriter = new FileWriter(file);
            String jsonString = config.toString().replaceAll(":",": ").replaceAll(",",",\n  ").replaceAll("\\{","{\n  ").replaceAll("}","\n}");

            fileWriter.write(jsonString);
            fileWriter.close();
        } catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to save config file!");
            e.printStackTrace();
        }
    }

    public static void copyConfig() {
        entries.forEach((s, configEntry) -> configEntry.setValue(configEntry.configValue.toString()));
    }

    public static void discardConfig() {
        entries.forEach((s, configEntry) -> configEntry.configValue = configEntry.value);
    }

    public static void setDefaults() {
        entries.clear();
        entries.put("previewNameTag", new ConfigEntry<>(true));
        entries.put("buttonLocation", new ConfigEntry<>(4, 5));
        //entries.put("useNewNetwork", new ConfigEntry<>(true));
        entries.put("useLocalServer", new ConfigEntry<>(false));
        entries.put("scriptLog", new ConfigEntry<>(0, 3));
        entries.put("listMods", new ConfigEntry<>(true));
        entries.put("chatMods", new ConfigEntry<>(true));
        entries.put("nameTagMods", new ConfigEntry<>(true));
        entries.put("nameTagIcon", new ConfigEntry<>(true));
        entries.put("ownNameTag", new ConfigEntry<>(false));
        entries.put("logOthers", new ConfigEntry<>(false));
        entries.put("actionWheel", new ConfigEntry<>(GLFW.GLFW_KEY_B));
        entries.put("formatScript", new ConfigEntry<>(true));
        entries.put("actionWheelPos", new ConfigEntry<>(0, 4));
    }

    public static class ConfigEntry<T> {
        public T value;
        public T defaultValue;
        public T configValue;
        public T modValue;

        public ConfigEntry(T value) {
            this(value, null);
        }

        public ConfigEntry(T value, T modValue) {
            this.value = value;
            this.defaultValue = value;
            this.configValue = value;
            this.modValue = modValue;
        }

        @SuppressWarnings("unchecked")
        private void setValue(String text) {
            try {
                if (value instanceof String)
                    value = (T) text;
                else if (value instanceof Boolean)
                    value = (T) Boolean.valueOf(text);
                else if (value instanceof Integer)
                    value = (T) Integer.valueOf(text);
                else if (value instanceof Float)
                    value = (T) Float.valueOf(text);
                else if (value instanceof Long)
                    value = (T) Long.valueOf(text);
                else if (value instanceof Double)
                    value = (T) Double.valueOf(text);
                else if (value instanceof Byte)
                    value = (T) Byte.valueOf(text);
                else if (value instanceof Short)
                    value = (T) Short.valueOf(text);
            } catch (Exception e) {
                value = defaultValue;
            }

            configValue = value;
        }
    }

    //returns true if modmenu shifts other buttons on the game menu screen
    public static boolean modmenuButton() {
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            File file = new File(FabricLoader.getInstance().getConfigDir().resolve("modmenu.json").toString());

            try {
                if (file.exists()) {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    JsonObject json = new JsonParser().parse(br).getAsJsonObject();

                    String config = json.getAsJsonPrimitive("mods_button_style").getAsString();

                    br.close();

                    if (!config.equals("shrink") && !config.equals("icon")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }
}