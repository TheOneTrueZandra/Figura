package net.blancworks.figura;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.blancworks.figura.models.FiguraTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PlayerDataManager {
    public static boolean didInitLocalPlayer = false;
    public static final Map<UUID, PlayerData> LOADED_PLAYER_DATA = new Object2ObjectOpenHashMap<>();

    //Players that we're currently queued up to grab data for.
    private static final Set<UUID> SERVER_REQUESTED_PLAYERS = new HashSet<>();
    private static final List<UUID> TO_CLEAR = new ArrayList<>();

    //Hash checking stuff
    public static final Queue<UUID> TO_REFRESH = new ArrayDeque<>();
    public static final Set<UUID> TO_REFRESH_SET = new HashSet<>();

    public static LocalPlayerData localPlayer;

    public static String lastLoadedFileName;

    public static boolean hasPlayerData(UUID id) {
        return LOADED_PLAYER_DATA.containsKey(id);
    }

    public static PlayerData getDataForPlayer(UUID id) {
        PlayerData getData;

        if (TO_CLEAR.contains(id)) {
            TO_CLEAR.remove(id);
            LOADED_PLAYER_DATA.remove(id);
        }

        if (!didInitLocalPlayer) {
            if (id == MinecraftClient.getInstance().player.getUuid()) {
                localPlayer = new LocalPlayerData(id);
                LOADED_PLAYER_DATA.put(id, localPlayer);
                didInitLocalPlayer = true;

                if (lastLoadedFileName != null) {
                    localPlayer.vanillaModel = ((PlayerEntityRenderer) MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(MinecraftClient.getInstance().player)).getModel();
                    localPlayer.loadModelFile(lastLoadedFileName);
                    localPlayer.getFileSize();
                    return localPlayer;
                }

                getPlayerAvatarFromServerOrCache(localPlayer.playerId, localPlayer);
                return localPlayer;
            }
        }

        if (id == MinecraftClient.getInstance().player.getUuid())
            return localPlayer;

        if (!LOADED_PLAYER_DATA.containsKey(id)) {
            getData = new PlayerData(id);

            getPlayerAvatarFromServerOrCache(id, getData);

            LOADED_PLAYER_DATA.put(id, getData);
        } else {
            getData = LOADED_PLAYER_DATA.get(id);
        }

        return getData;
    }

    //Attempts to get the data for a player from the server.
    public static void getPlayerAvatarFromServerOrCache(UUID id, PlayerData targetData) {
        //Prevent this from running more than once at a time per player.
        if (SERVER_REQUESTED_PLAYERS.contains(id))
            return;
        SERVER_REQUESTED_PLAYERS.add(id);

        FiguraMod.doTask(() -> {

            try {
                //Attempt to load from cache first.
                if (!attemptCacheLoad(id, targetData))
                    //If cache load fails or is invalid, load from server.
                    loadFromNetwork(id, targetData);
            } catch (Exception e){
                e.printStackTrace();
            }

            SERVER_REQUESTED_PLAYERS.remove(id);
        });
    }

    //Loads the model out of the local cache, if the file for that exists.
    //Returns true if loaded from cache.
    //Cache load only happens if the hash on the server matches the local hash, meaning the avatar has not changed.
    public static boolean attemptCacheLoad(UUID id, PlayerData targetData) {
        Path destinationPath = FiguraMod.getModContentDirectory().resolve("cache");

        String[] splitID = id.toString().split("-");

        for (int i = 0; i < splitID.length; i++) {
            if (i != splitID.length - 1)
                destinationPath = destinationPath.resolve(splitID[i]);
        }

        Path nbtFilePath = destinationPath.resolve(splitID[splitID.length - 1] + ".nbt");
        Path hashFilePath = destinationPath.resolve(splitID[splitID.length - 1] + ".hsh");

        try {
            if (Files.exists(nbtFilePath) && Files.exists(hashFilePath)) {
                String hash = Files.readAllLines(hashFilePath).get(0);
                String serverHash = hash;

                try {
                    serverHash = FiguraMod.networkManager.asyncGetAvatarHash(id).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (serverHash.length() == 0)
                    serverHash = hash;

                if (serverHash.equals(hash)) {
                    FileInputStream fis = new FileInputStream(nbtFilePath.toFile());
                    DataInputStream dis = new DataInputStream(fis);

                    targetData.loadFromNbt(dis);
                    targetData.lastHash = hash;
                    targetData.lastHashCheckTime = new Date(new Date().getTime() - (1000 * 1000));

                    FiguraMod.LOGGER.debug("Used cached model.");
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    //Loads the model off of the network.
    public static void loadFromNetwork(UUID id, PlayerData targetData) {
        FiguraMod.networkManager.getAvatarData(id).thenApply((tag) -> {
            try {
                
                //If no avatar.
                if(tag == null){
                    return null;
                }
                
                targetData.loadFromNbt(tag);
                targetData.lastHash = FiguraMod.networkManager.asyncGetAvatarHash(id).get();
                targetData.lastHashCheckTime = new Date(new Date().getTime() - (1000 * 1000));
                targetData.saveToCache(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return null;
        });
    }

    public static void clearPlayer(UUID id) {
        TO_CLEAR.add(id);

        if (localPlayer != null) {
            if (id == localPlayer.playerId) {
                localPlayer = null;
                didInitLocalPlayer = false;
            }
        }
    }

    public static void clearCache() {
        LOADED_PLAYER_DATA.clear();
        localPlayer = null;
        didInitLocalPlayer = false;
        lastLoadedFileName = null;
    }

    public static void clearLocalPlayer() {
        LOADED_PLAYER_DATA.remove(localPlayer.playerId);
        localPlayer = null;
        didInitLocalPlayer = false;
        lastLoadedFileName = null;
    }

    private static int hashCheckCooldown = 0;

    //Tick function for the client. Basically dispatches all the other functions in the mod.
    public static void tick() {
        if (MinecraftClient.getInstance().world == null)
            return;

        for (UUID uuid : TO_CLEAR) {
            LOADED_PLAYER_DATA.remove(uuid);
        }
        TO_CLEAR.clear();

        for (Map.Entry<UUID, PlayerData> entry : LOADED_PLAYER_DATA.entrySet()) {
            entry.getValue().tick();
        }

        if (hashCheckCooldown > 0)
            hashCheckCooldown--;

        if (hashCheckCooldown == 0 && TO_REFRESH.size() > 0) {
            UUID nextID = TO_REFRESH.remove();
            TO_REFRESH_SET.remove(nextID);

            checkPlayerDataHash(nextID);
            hashCheckCooldown += 4;
        }
    }

    public static void checkForPlayerDataRefresh(PlayerData data) {
        //Never check local player data for this.
        if (data == localPlayer)
            return;

        Date checkDate = new Date();
        if (checkDate.getTime() - data.lastHashCheckTime.getTime() > 1000 * 10) {
            if (!TO_REFRESH_SET.contains(data.playerId)) {
                TO_REFRESH_SET.add(data.playerId);
                TO_REFRESH.add(data.playerId);
            }
        }
    }

    public static void checkPlayerDataHash(UUID id) {
        PlayerData dat = getDataForPlayer(id);
        dat.lastHashCheckTime = new Date();

        if (dat instanceof LocalPlayerData) {
            if (((LocalPlayerData) dat).loadedName != null) {
                return;
            }
        }

        FiguraMod.doTask(() -> {
            try {
                String hash = FiguraMod.networkManager.asyncGetAvatarHash(id).get();

                if (!hash.equals(dat.lastHash) && hash.length() > 0) {
                    TO_CLEAR.add(id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    //Reloads all textures, used for asset reloads in vanilla.
    public static void reloadAllTextures() {
        for (Map.Entry<UUID, PlayerData> entry : LOADED_PLAYER_DATA.entrySet()) {
            PlayerData pDat = entry.getValue();
            if (pDat.texture != null) {
                pDat.texture.registerTexture();
                pDat.texture.uploadUsingData();
            }

            for (FiguraTexture extraTexture : pDat.extraTextures) {
                extraTexture.registerTexture();
                extraTexture.uploadUsingData();
            }
        }
    }
}
