package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;

public class NewFiguraGuiScreen extends Screen {

    public Screen parentScreen;
    private boolean hudHidden;

    private final Identifier BOOK_TEXTURE = new Identifier("figura", "textures/gui/book.png");

    public NewFiguraGuiScreen(Screen parentScreen) {
        super(new TranslatableText("gui.figura.menutitle"));
        this.parentScreen = parentScreen;
    }

    @Override
    public void init() {
        super.init();

        //hide hud
        this.hudHidden = this.client.options.hudHidden;
        this.client.options.hudHidden = true;
    }

    @Override
    public void onClose() {
        this.client.options.hudHidden = this.hudHidden;
        this.client.openScreen(parentScreen);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        //variables
        Vec2f screen = new Vec2f(this.width, this.height);
        Vec2f offset = new Vec2f(Math.max(30f, 5f), screen.y / 2.0f - 90);

        //texture
        MinecraftClient.getInstance().getTextureManager().bindTexture(BOOK_TEXTURE);

        //middle
        matrices.push();
        matrices.translate(screen.x / 2.0 - 11.5 - offset.x, offset.y, 0f);
        drawTexture(matrices, 0, 0, 23, 180, 8.0f, 0.0f, 4, 16, 32, 16);
        matrices.pop();

        //left side
        matrices.push();
        matrices.translate(10, offset.y, 0f);
        drawTexture(matrices, 0, 0, 23, 180, 0.0f, 0.0f, 4, 16, 32, 16);
        matrices.pop();

        //right side
        matrices.push();
        matrices.translate(screen.x - 10 - 23 - 11.5 - offset.x, offset.y, 0f);
        drawTexture(matrices, 0, 0, 23, 180, 12.0f, 0.0f, 4, 16, 32, 16);
        matrices.pop();

        super.render(matrices, mouseX, mouseY, delta);
    }
}
