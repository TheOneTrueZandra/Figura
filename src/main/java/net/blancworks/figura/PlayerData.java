package net.blancworks.figura;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.models.CustomModel;
import net.blancworks.figura.models.FiguraTexture;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.blancworks.figura.trust.TrustContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


/**
 * Responsible for storing all the data associated with the player on this client.
 */
public class PlayerData {
    private static TextureManager textureManager;

    //ID of the player
    public UUID playerId;

    //The custom model associated with the player
    public CustomModel model;
    //The custom texture for the custom model
    public FiguraTexture texture;
    //The custom script for the model.
    public CustomScript script;
    //Vanilla model for the player, in case we need it for something.
    public PlayerEntityModel vanillaModel;

    //Extra textures for the model (like emission)
    public final List<FiguraTexture> extraTextures = new ArrayList<>();

    public PlayerEntity lastEntity;

    //The last time we checked for a hash
    public Date lastHashCheckTime = new Date();
    //The last hash code of the avatar.
    public String lastHash = "";
    //True if the model needs to be re-loaded due to a hash mismatch.
    public boolean isInvalidated = false;

    private Identifier trustIdentifier;

    //Name tag and player list mark
    private MutableText nameDecorations;

    public Identifier getTrustIdentifier() {
        if (trustIdentifier == null)
            trustIdentifier = new Identifier("players", playerId.toString());
        return trustIdentifier;
    }

    public static TextureManager getTextureManager() {
        if (textureManager == null)
            textureManager = MinecraftClient.getInstance().getTextureManager();
        return textureManager;
    }

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        if (FiguraMod.special.contains(this.playerId)) {
            this.nameDecorations = new LiteralText(" ").append(new TranslatableText("figura.star"));
        } else {
            this.nameDecorations = new LiteralText("");
        }
    }
    
    /**
     * Writes to the NBT this player data.
     *
     * @param nbt the nbt to write to
     * @return {@code true} if the player data was written into the NBT, otherwise {@code false}
     */
    public boolean writeNbt(CompoundTag nbt) {
        //You cannot save a model that is incomplete.
        if (model == null || texture == null)
            return false;

        //Put ID.
        nbt.putUuid("id", playerId);

        //Put Model.
        CompoundTag modelNbt = new CompoundTag();
        model.writeNbt(modelNbt);
        nbt.put("model", modelNbt);

        //Put Texture.
        try {
            CompoundTag textureNbt = new CompoundTag();
            texture.writeNbt(textureNbt);
            nbt.put("texture", textureNbt);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (script != null) {
            //Put Script.
            CompoundTag scriptNbt = new CompoundTag();
            script.toNBT(scriptNbt);
            nbt.put("script", scriptNbt);
        }

        if (extraTextures.size() > 0) {
            ListTag texList = new ListTag();

            for (FiguraTexture extraTexture : extraTextures) {
                CompoundTag elytraTextureNbt = new CompoundTag();
                extraTexture.writeNbt(elytraTextureNbt);
                texList.add(elytraTextureNbt);
            }

            nbt.put("exTexs", texList);
        }

        return true;
    }

    /**
     * Reads a player data from the given NBT.
     *
     * @param nbt the nbt to read
     */
    public void readNbt(CompoundTag nbt) {
        playerId = nbt.getUuid("id");

        try {
            //Create model on main thread.
            CompoundTag modelNbt = (CompoundTag) nbt.get("model");
            model = new CustomModel();

            //Load model on off-thread.
            FiguraMod.doTask(() -> {
                try {
                    model.readNbt(modelNbt);
                    model.owner = this;
                    model.isDone = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            //Create texture on main thread
            CompoundTag textureNbt = (CompoundTag) nbt.get("texture");
            texture = new FiguraTexture();
            texture.id = new Identifier("figura", playerId.toString());
            getTextureManager().registerTexture(texture.id, texture);

            //Load texture on off-thread
            FiguraMod.doTask(() -> {
                texture.readNbt(textureNbt);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nbt.contains("script")) {
                CompoundTag scriptNbt = (CompoundTag) nbt.get("script");

                script = new CustomScript();

                FiguraMod.doTask(() -> {
                    script.fromNBT(this, scriptNbt);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (nbt.contains("exTexs")) {
                ListTag textureList = (ListTag) nbt.get("exTexs");

                for (Tag element : textureList) {
                    FiguraTexture newTexture = new FiguraTexture();
                    newTexture.id = new Identifier("figura", playerId.toString() + newTexture.type.toString());
                    getTextureManager().registerTexture(newTexture.id, newTexture);
                    extraTextures.add(newTexture);

                    FiguraMod.doTask(() -> {
                        newTexture.readNbt((CompoundTag) element);
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Return the mark (and star) for this player's name.
    public Text getNameDecorations() {
        return this.nameDecorations;
    }
    
    public void updateNameDecorations() {
        if (this.model != null) {
            this.nameDecorations = new LiteralText(" ").append(new TranslatableText("figura.mark"));
            if (FiguraMod.special.contains(this.playerId)) {
                this.nameDecorations.append(new LiteralText(" ")).append(new TranslatableText("figura.star"));
            }
        } else {
            if (FiguraMod.special.contains(this.playerId)) {
                this.nameDecorations = new LiteralText(" ").append(new TranslatableText("figura.star"));
            } else {
                this.nameDecorations = new LiteralText("");
            }
        }
    }

    //Returns the file size, in bytes.
    public int getFileSize() {
        CompoundTag writtenNbt = new CompoundTag();
        this.writeNbt(writtenNbt);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream w = new DataOutputStream(out);

            NbtIo.writeCompressed(writtenNbt, w);

            this.model.totalSize = w.size();
            return w.size();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    //Ticks from client.
    public void tick() {
        if (this.isInvalidated)
            PlayerDataManager.clearPlayer(playerId);
        
        
        vanillaModel = ((PlayerEntityRenderer) MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(MinecraftClient.getInstance().player)).getModel();
        PlayerEntity newEnt = MinecraftClient.getInstance().world.getPlayerByUuid(this.playerId);
        if (lastEntity != newEnt && script != null) {
            if(newEnt == null){
                CustomScript reloadedScript = new CustomScript();
                reloadedScript.source = script.source;
                script = reloadedScript;
            } else {
                CustomScript reloadedScript = new CustomScript();
                reloadedScript.load(this, script.source);
                script = reloadedScript;
            }
        }
        lastEntity = newEnt;

        if (lastEntity != null) {
            if (script != null) {
                try {
                    script.tick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (Config.nameTagMarkColored.value && this.lastEntity.getScoreboardTeam() == null) {
                this.nameDecorations.formatted(Formatting.AQUA);
            } else {
                this.nameDecorations.formatted(Formatting.RESET);
            }
        }
    }

    public void loadFromNbt(DataInputStream input) throws Exception {
        CompoundTag nbt = NbtIo.readCompressed(input);
        loadFromNbt(nbt);
    }

    public void loadFromNbt(CompoundTag tag) {
        this.readNbt(tag);
        getFileSize();
        
        this.updateNameDecorations();
    }

    public TrustContainer getTrustContainer() {
        return PlayerTrustManager.getContainer(getTrustIdentifier());
    }

    //Saves this playerdata to the cache.
    public void saveToCache(UUID id) {
        
        //We run this as a task to make sure all the previous load operations are done (since those are all also tasks)
        FiguraMod.doTask(() -> {
            Path destinationPath = FiguraMod.getModContentDirectory().resolve("cache");

            String[] splitID = id.toString().split("-");

            for (int i = 0; i < splitID.length; i++) {
                if (i != splitID.length - 1)
                    destinationPath = destinationPath.resolve(splitID[i]);
            }

            Path nbtFilePath = destinationPath.resolve(splitID[splitID.length - 1] + ".nbt");
            Path hashFilePath = destinationPath.resolve(splitID[splitID.length - 1] + ".hsh");

            try {
                CompoundTag targetTag = new CompoundTag();
                this.writeNbt(targetTag);

                Files.createDirectories(nbtFilePath.getParent());
                NbtIo.writeCompressed(targetTag, new FileOutputStream(nbtFilePath.toFile()));
                Files.write(hashFilePath, this.lastHash.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
