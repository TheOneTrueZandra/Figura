package net.blancworks.figura;


import com.google.common.io.CharStreams;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.models.parsers.BlockbenchModelDeserializer;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.*;
import java.sql.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is the specific class used for the LOCAL player.
 * This is in place to allow users to freely modify their model based on files loaded from disk,
 * and allow for easier editing.
 */
public class LocalPlayerData extends PlayerData {
    public String loadedName;
    private final Map<String, WatchKey> watchKeys = new Object2ObjectOpenHashMap<>();
    private final Set<String> watchedFiles = new HashSet<>();
    public static WatchService ws;

    static {
        try {
            ws = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LocalPlayerData(UUID playerId) {
        super(playerId);
    }
    
    @Override
    public void tick() {

        this.lastHashCheckTime = new Date(Long.MAX_VALUE);
        if (this.loadedName != null)
            this.lastHash = "";
        super.tick();

        this.tickFileWatchers();
    }

    public static Path getContentDirectory() {
        return FiguraMod.getModContentDirectory().resolve("model_files");
    }

    /**
     * Loads a model file at a specific directory.
     *
     * @param fileName
     */
    public void loadModelFile(String fileName) {
        watchedFiles.clear();

        //create root directory
        Path contentDirectory = getContentDirectory();

        try {
            Files.createDirectories(contentDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //check file type
        boolean isZip = fileName.endsWith(".zip");
        boolean isDirectory = !isZip && !fileName.endsWith("*");

        //reset paths
        Path jsonPath = null;
        Path jsonPlayerPath = null;
        Path texturePath = null;
        Path scriptPath = null;
        Path metadataPath;

        //dummy file - must be initialized
        File file = null;

        //folder data
        if (isDirectory) {
            file = new File(contentDirectory.resolve(fileName).toString());

            //set paths
            jsonPath = file.toPath().resolve("model.bbmodel");
            jsonPlayerPath = file.toPath().resolve("player_model.bbmodel");
            texturePath = file.toPath().resolve("texture.png");
            scriptPath = file.toPath().resolve("script.lua");
            metadataPath = file.toPath().resolve("metadata.nbt");

            //add watchedfiles
            watchedFiles.add(jsonPath.toString());
            watchedFiles.add(jsonPlayerPath.toString());
            watchedFiles.add(texturePath.toString());
            watchedFiles.add(scriptPath.toString());
            watchedFiles.add(metadataPath.toString());

            //set root directory
            contentDirectory = file.toPath();
        }
        //zip data
        else if (isZip) {
            //add zip to watched files, even if you cant edit opened zip files, you might be able to
            file = new File(contentDirectory.resolve(fileName).toString());
            watchedFiles.add(file.toString());
        }
        //then must be a .bbmodel *
        else {
            //remove invalid * from name
            fileName = fileName.substring(0, fileName.length() - 1);

            //set paths
            jsonPath = contentDirectory.resolve(fileName + ".bbmodel");
            jsonPlayerPath = contentDirectory.resolve("player_" + fileName + ".bbmodel");
            texturePath = contentDirectory.resolve(fileName + ".png");
            scriptPath = contentDirectory.resolve(fileName + ".lua");
            metadataPath = contentDirectory.resolve(fileName + ".nbt");

            //add watched files
            watchedFiles.add(jsonPath.toString());
            watchedFiles.add(texturePath.toString());
            watchedFiles.add(scriptPath.toString());
            watchedFiles.add(metadataPath.toString());

            //add * back
            fileName += "*";
        }

        //check if files exists
        boolean cantLoad = !isZip && ((!Files.exists(jsonPath) && !Files.exists(jsonPlayerPath)) || !Files.exists(texturePath));

        //check for zip files
        if (isZip) {
            try {
                ZipFile zipFile = new ZipFile(file.getPath());

                boolean hasModel = zipFile.getEntry("model.bbmodel") != null;
                boolean hasPlayerModel = zipFile.getEntry("player_model.bbmodel") != null;
                boolean hasTexture = zipFile.getEntry("texture.png") != null;

                cantLoad = (!hasModel && !hasPlayerModel) || !hasTexture;
            } catch (Exception e) {
                FiguraMod.LOGGER.debug(e.toString());
                cantLoad = true;
            }
        }

        //log and clear player model
        if (cantLoad) {
            FiguraMod.LOGGER.error("Failed to load model " + fileName);
            PlayerDataManager.clearLocalPlayer();
            return;
        }

        if (!watchKeys.containsKey(contentDirectory.toString())) {
            try {
                watchKeys.put(contentDirectory.toString(), contentDirectory.register(ws, StandardWatchEventKinds.ENTRY_MODIFY));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //set loaded name
        this.loadedName = fileName;


        //Set up ZIP file.
        ZipFile modelZip = null;

        try {
            if (isZip)
                modelZip = new ZipFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        InputStream inputStream = null;

        //load JSON model.
        try {
            //Clear current model
            this.model = null;
            //Set up string for later
            String modelJsonText = null;
            

            //Get input stream, either from file, or from zip.
            if (isZip) {
                ZipEntry modelEntry = modelZip.getEntry("model.bbmodel");

                if (modelEntry == null){
                    modelEntry = modelZip.getEntry("player_model.bbmodel");
                    BlockbenchModelDeserializer.overrideAsPlayerModel = true;
                }

                inputStream = modelZip.getInputStream(modelEntry);
            } else {
                if (Files.exists(jsonPath)) {
                    inputStream = new FileInputStream(jsonPath.toFile());
                } else {
                    inputStream = new FileInputStream(jsonPlayerPath.toFile());
                    BlockbenchModelDeserializer.overrideAsPlayerModel = true;
                }
            }

            //Try to read from input stream
            try (final Reader reader = new InputStreamReader(inputStream)) {
                modelJsonText = CharStreams.toString(reader);
            }

            //Finalize string for lambda
            String finalModelJsonText = modelJsonText;
            //Load model from GSON in off-thread.
            FiguraMod.doTask(() -> {
                this.model = FiguraMod.GSON.fromJson(finalModelJsonText, CustomModel.class);
                this.model.owner = this;
                this.model.isDone = true;
                FiguraMod.LOGGER.warn("Model Loading Finished");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Close previously used stream, regardless of what it was (zip or filestream)
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        //Load texture.
        try {
            //Clear current texture.
            this.texture = null;

            //Generate Identifier for texture.
            Identifier id = new Identifier("figura", playerId.toString());

            //Create new texture, and register it.
            this.texture = new FiguraTexture();
            this.texture.id = id;
            getTextureManager().registerTexture(id, texture);

            //Get input stream, either from file, or from zip.
            if (isZip) {
                inputStream = modelZip.getInputStream(modelZip.getEntry("texture.png"));
            } else {
                inputStream = new FileInputStream(texturePath.toFile());
            }

            //Load texture (tasks are managed by the texture itself)
            texture.loadFromStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //We don't have to close the input stream for textures, they do that for us.
        //We'll still set it to null, tho.
        inputStream = null;


        //Load script.
        try {
            //Clear previous script.
            this.script = null;
            //Set up string for later
            String scriptSource = null;

            //Get input stream, either from file, or from zip.
            if (isZip) {
                //Get entry
                ZipEntry fileEntry = modelZip.getEntry("script.lua");
                //If there is an script entry
                if (fileEntry != null)
                    inputStream = modelZip.getInputStream(fileEntry);
            } else if (Files.exists(scriptPath)) {
                inputStream = new FileInputStream(scriptPath.toFile());
            }

            //If there is a script, try to load it
            if (inputStream != null) {
                //Try to read from input stream
                try (final Reader reader = new InputStreamReader(inputStream)) {
                    scriptSource = CharStreams.toString(reader);
                }

                //Create script.
                this.script = new CustomScript();

                //Finalize script source for lambda.
                String finalScriptSource = scriptSource;
                //Load script on off-thread.
                FiguraMod.doTask(() -> {
                    this.script.load(this, finalScriptSource);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Close previously used stream, regardless of what it was (zip or filestream)
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Load extra textures
        try {
            extraTextures.clear();

            for (FiguraTexture.TextureType textureType : FiguraTexture.EXTRA_TEXTURE_TO_RENDER_LAYER.keySet()) {
                Path location = null;

                //If this is a zip file
                if (isZip) {
                    //Get entry
                    ZipEntry fileEntry = modelZip.getEntry("texture" + textureType.toString() + ".png");
                    //If there is an entry that matches this texture
                    if (fileEntry != null)
                        inputStream = modelZip.getInputStream(fileEntry);
                } else { //If this is not a zip file 

                    //Check for directory first
                    if (isDirectory) {
                        location = file.toPath().resolve("texture" + textureType.toString() + ".png");
                    } else { //Legacy support.
                        location = contentDirectory.resolve(fileName.substring(0, fileName.length() - 1) + textureType.toString() + ".png");
                    }

                    //If file exists at that location, make a stream for it, and set it to be watched.
                    if (Files.exists(location)) {
                        inputStream = new FileInputStream(location.toFile());
                        watchedFiles.add(location.toString());
                    }
                }

                //If there IS a stream for this extra texture
                if (inputStream != null) {
                    FiguraTexture extraTexture = new FiguraTexture();
                    extraTexture.id = new Identifier("figura", playerId.toString() + textureType.toString());
                    extraTexture.filePath = location;
                    getTextureManager().registerTexture(extraTexture.id, extraTexture);
                    extraTexture.type = textureType;

                    extraTextures.add(extraTexture);

                    extraTexture.loadFromStream(inputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //We don't need to close the input stream here, because if it exists, it's an extra-texture stream.
        //We keep those open until texture loading is finished.

        //Close ZIP stream.
        try {
            if (isZip)
                modelZip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tickFileWatchers() {

        boolean doReload = false;

        for (Map.Entry<String, WatchKey> entry : watchKeys.entrySet()) {
            WatchKey key = watchKeys.get(entry.getKey());

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // This key is registered only
                // for ENTRY_CREATE events,
                // but an OVERFLOW event can
                // occur regardless if events
                // are lost or discarded.
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                // The filename is the
                // context of the event.
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();

                Path parentPath = FileSystems.getDefault().getPath(entry.getKey());
                Path child = parentPath.resolve(filename);
                String realName = child.getFileName().toString();

                try {

                    if (watchedFiles.contains(child.toString()))
                        doReload = true;

                    if (realName.equals(loadedName) && !doReload)
                        doReload = true;

                } catch (Exception e) {
                    System.err.println(e);
                }
            }
        }

        if (doReload) {
            watchKeys.clear();

            PlayerDataManager.lastLoadedFileName = loadedName;
            loadModelFile(loadedName);
        }
    }
}
