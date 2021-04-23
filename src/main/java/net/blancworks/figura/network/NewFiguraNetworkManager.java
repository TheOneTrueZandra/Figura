package net.blancworks.figura.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NewFiguraNetworkManager implements IFiguraNetwork {
    @Override
    public void tickNetwork() {
        
    }

    @Override
    public CompletableFuture<CompoundTag> getAvatarData(UUID id) {
        return null;
    }

    @Override
    public CompletableFuture<UUID> postAvatar() {
        return null;
    }

    @Override
    public CompletableFuture setAvatarCurr(UUID avatarID) {
        return null;
    }

    @Override
    public CompletableFuture deleteAvatar() {
        return null;
    }

    @Override
    public CompletableFuture<String> asyncGetAvatarHash(UUID avatarID) {
        return null;
    }

    @Override
    public void parseKickAuthMessage(Text reason) {
        
    }
}
