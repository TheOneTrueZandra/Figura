package net.blancworks.figura;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {
    public static ConfigEntry<Boolean> previewNameTag;
    public static ConfigEntry<Boolean> nameTagMark;
    public static ConfigEntry<Boolean> nameTagMarkColored;
    public static ConfigEntry<Integer> buttonLocation;

    private static final File file = new File(FabricLoader.getInstance().getConfigDir().resolve("figura.properties").toString());

    public static void initialize() {
        setDefaults();
        loadConfig();
        saveConfig();
    }

    public static void loadConfig() {
        try {
            if(file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line = br.readLine();

                while (line != null) {
                    String[] content = line.split("=");

                    if (content.length >= 2 && line.charAt(0) != '#') {
                        switch (content[0]) {
                            case "previewNameTag":
                                previewNameTag = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "nameTagMark":
                                nameTagMark = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "nameTagMarkColored":
                                nameTagMarkColored = new ConfigEntry<>(Boolean.parseBoolean(content[1]), true);
                                break;
                            case "buttonLocation":
                                int i = Integer.parseInt(content[1]) % 4;
                                if (i < 0) i += 4;
                                buttonLocation = new ConfigEntry<>(i, 3);
                                break;
                        }
                    }
                    line = br.readLine();
                }

                br.close();
            }
        }
        catch (Exception e) {
            FiguraMod.LOGGER.warn("Failed to load config file! Generating a new one...");
            e.printStackTrace();
            setDefaults();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try {
            FileWriter writer = new FileWriter(file);

            writer.write("### Displays your NameTag on the model preview screen ### - default true\n");
            writer.write("previewNameTag=" + previewNameTag.value + "\n\n");

            writer.write("### Adds The Mark △ to the NameTag of players using Figura ### - default true\n");
            writer.write("nameTagMark=" + nameTagMark.value + "\n\n");

            writer.write("### Paints The Mark △ blue when the player's name doesn't have a color ### - default true\n");
            writer.write("nameTagMarkColored=" + nameTagMarkColored.value + "\n\n");

            writer.write("### Location where the Figura settings button should be ###\n");
            writer.write("### 0 - top left | 1 - top right | 2 - bottom left | 3 - bottom right ### - default 3\n");
            writer.write("buttonLocation=" + buttonLocation.value);

            writer.close();
        }
        catch (Exception e) {
            FiguraMod.LOGGER.error("Failed to save config file!");
            e.printStackTrace();
        }
    }

    public static void setDefaults() {
        previewNameTag = new ConfigEntry<>(true, true);
        nameTagMark = new ConfigEntry<>(true, true);
        nameTagMarkColored = new ConfigEntry<>(true, true);
        buttonLocation = new ConfigEntry<>(3, 3);
    }

    public static class ConfigEntry<T> {
        public T value;
        public T defaultValue;

        public ConfigEntry(T value, T defaultValue) {
            this.value = value;
            this.defaultValue = defaultValue;
        }
    }
}