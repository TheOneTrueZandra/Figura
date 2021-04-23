package net.blancworks.figura.mixin;

import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntityModel.class)
public class PlayerModelRendererMixin<T extends LivingEntity> extends BipedEntityModel<T> {
    public PlayerModelRendererMixin(float scale) {
        super(scale);
    }
    
    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        super.render(matrices, vertices, light, overlay, red, green, blue, alpha);

        PlayerData playerData = FiguraMod.currentData;

        if (playerData != null) {
            PlayerDataManager.checkForPlayerDataRefresh(playerData);

            if (playerData.model != null) {
                if (playerData.texture == null || !playerData.texture.isDone)
                    return;

                matrices.push();

                try {
                    playerData.model.render((PlayerEntityModel<T>) (Object) this, matrices, FiguraMod.vertexConsumerProvider, light, overlay, 1, 1, 1, alpha);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                matrices.pop();
            }
        }
    }
}
