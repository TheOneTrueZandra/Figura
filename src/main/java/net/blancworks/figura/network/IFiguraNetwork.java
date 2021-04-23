package net.blancworks.figura.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IFiguraNetwork {

    //Ticks the network.
    void tickNetwork();
    
    //Returns a CompoundTag contianing all the data this network reports for the avatar of a given UUID.
    CompletableFuture<CompoundTag> getAvatarData(UUID id);
    
    //Posts an avatar to the server.
    CompletableFuture<UUID> postAvatar();
    
    //Sets a player's avatar to the provided UUID.
    CompletableFuture setAvatarCurr(UUID avatarID);
    
    //Deletes the avatar from the server for this player.
    CompletableFuture deleteAvatar();
    
    //Gets the hash for a given avatar from the network.
    CompletableFuture<String> asyncGetAvatarHash(UUID avatarID);
    
    void parseKickAuthMessage(Text reason);
}
