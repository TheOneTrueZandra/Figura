package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.blancworks.figura.Config;
import net.blancworks.figura.FiguraMod;
import net.blancworks.figura.LocalPlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.gui.widgets.CustomListWidgetState;
import net.blancworks.figura.gui.widgets.ModelFileListWidget;
import net.blancworks.figura.gui.widgets.TexturedButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Quaternion;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FiguraGuiScreen extends Screen {

    public Screen parentScreen;

    public Identifier uploadTexture = new Identifier("figura", "textures/gui/upload.png");
    public Identifier reloadTexture = new Identifier("figura", "textures/gui/reload.png");
    public Identifier deleteTexture = new Identifier("figura", "textures/gui/delete.png");
    public Identifier expandTexture = new Identifier("figura", "textures/gui/expand.png");
    public Identifier keybindsTexture = new Identifier("figura", "textures/gui/keybinds.png");
    public Identifier playerBackgroundTexture = new Identifier("figura", "textures/gui/player_background.png");
    public Identifier scalableBoxTexture = new Identifier("figura", "textures/gui/scalable_box.png");

    public static final List<Text> deleteTooltip = new ArrayList<Text>(){{
        add(new TranslatableText("gui.figura.button.tooltip.deleteavatar").setStyle(Style.EMPTY.withColor(TextColor.parse("red"))));
        add(new TranslatableText("gui.figura.button.tooltip.deleteavatartwo").setStyle(Style.EMPTY.withColor(TextColor.parse("red"))));
    }};

    public static final TranslatableText uploadTooltip = new TranslatableText("gui.figura.button.tooltip.upload");
    public static final TranslatableText reloadTooltip = new TranslatableText("gui.figura.button.tooltip.reloadavatar");
    public static final TranslatableText keybindTooltip = new TranslatableText("gui.figura.button.tooltip.keybinds");

    public TexturedButtonWidget uploadButton;
    public TexturedButtonWidget reloadButton;
    public TexturedButtonWidget deleteButton;
    public TexturedButtonWidget expandButton;
    public TexturedButtonWidget keybindsButton;

    public MutableText nameText;
    public MutableText rawNameText;
    public MutableText fileSizeText;
    public MutableText modelComplexityText;
    public MutableText scriptText;

    private TextFieldWidget searchBox;
    private int paneY;
    private int paneWidth;
    private int searchBoxX;

    private boolean isHoldingShift = false;

    //gui sizes
    private static int guiScale, modelBgSize, modelSize;
    private static float screenScale;

    //model properties
    private boolean canRotate;
    private boolean canDrag;
    private static boolean expand;

    private float anchorX, anchorY;
    private float anchorAngleX, anchorAngleY;
    private float angleX, angleY;

    private float scaledValue;
    private final float SCALE_FACTOR = 1.1F;

    private int modelX, modelY;
    private float dragDeltaX, dragDeltaY;
    private float dragAnchorX, dragAnchorY;

    //model nameplate
    public static boolean showOwnNametag = false;

    public FiguraTrustScreen trustScreen = new FiguraTrustScreen(this);
    public FiguraConfigScreen configScreen = new FiguraConfigScreen(this);
    public FiguraKeyBindsScreen keyBindsScreen = new FiguraKeyBindsScreen(this);

    public CustomListWidgetState modelFileListState = new CustomListWidgetState();
    public ModelFileListWidget modelFileList;

    public FiguraGuiScreen(Screen parentScreen) {
        super(new TranslatableText("gui.figura.menutitle"));
        this.parentScreen = parentScreen;

        //reset model settings
        canRotate = false;
        canDrag = false;
        expand = false;
        resetModelPos();
    }

    @Override
    protected void init() {
        super.init();

        //screen size
        guiScale = (int) this.client.getWindow().getScaleFactor();
        screenScale = (float) (Math.min(this.width, this.height) / 1018.0);

        //model size
        modelBgSize = Math.min((int) ((512 / guiScale) * (screenScale * guiScale)), 258);
        modelSize = Math.min((int) ((192 / guiScale) * (screenScale * guiScale)), 96);

        //search box and model list
        paneY = 48;
        paneWidth = this.width / 3 - 8;

        int searchBoxWidth = paneWidth - 5;
        searchBoxX = 7;
        this.searchBox = new TextFieldWidget(this.textRenderer, searchBoxX, 22, searchBoxWidth, 20, this.searchBox, new TranslatableText("gui.figura.button.search"));
        this.searchBox.setChangedListener((string_1) -> modelFileList.filter(string_1, false));
        this.modelFileList = new ModelFileListWidget(this.client, paneWidth, this.height, paneY + 19, this.height - 36, 20, this.searchBox, this.modelFileList, this, modelFileListState);
        this.modelFileList.setLeftPos(5);
        this.addChild(this.modelFileList);
        this.addChild(this.searchBox);

        int width = Math.min((this.width / 2) - 10 - 128, 128);

        //open folder
        this.addButton(new ButtonWidget(5, this.height - 20 - 5, 140, 20, new TranslatableText("gui.figura.button.openfolder"), (buttonWidgetx) -> {
            Path modelDir = LocalPlayerData.getContentDirectory();
            try {
                if (!Files.exists(modelDir))
                    Files.createDirectory(modelDir);
                Util.getOperatingSystem().open(modelDir.toUri());
            } catch (Exception e) {
                FiguraMod.LOGGER.error(e.toString());
            }
        }));

        //back button
        this.addButton(new ButtonWidget(this.width - width - 5, this.height - 20 - 5, width, 20, new TranslatableText("gui.figura.button.back"), (buttonWidgetx) -> this.client.openScreen(parentScreen)));

        //trust button
        this.addButton(new ButtonWidget(this.width - 140 - 5, 15, 140, 20, new TranslatableText("gui.figura.button.trustmenu"), (buttonWidgetx) -> this.client.openScreen(trustScreen)));

        //config button
        this.addButton(new ButtonWidget(this.width - 140 - 5, 40, 140, 20, new TranslatableText("gui.figura.button.configmenu"), (buttonWidgetx) -> this.client.openScreen(configScreen)));

        //help button
        this.addButton(new ButtonWidget(this.width - 140 - 5, 65, 140, 20, new TranslatableText("gui.figura.button.help"), (buttonWidgetx) -> this.client.openScreen(new ConfirmChatLinkScreen((bl) -> {
            if (bl) {
                Util.getOperatingSystem().open("https://github.com/TheOneTrueZandra/Figura/wiki/Figura-Panel");
            }
            this.client.openScreen(this);
        }, "https://github.com/TheOneTrueZandra/Figura/wiki/Figura-Panel", true))));

        //keybinds button
        keybindsButton = new TexturedButtonWidget(
                this.width - 140 - 5 - 25, 15,
                20, 20,
                0, 0, 20,
                keybindsTexture, 40, 40,
                (bx) -> this.client.openScreen(keyBindsScreen)
        );
        this.addButton(keybindsButton);
        keybindsButton.active = false;

        //delete button
        deleteButton = new TexturedButtonWidget(
                this.width / 2 + modelBgSize / 2 + 4, this.height / 2 - modelBgSize / 2,
                25, 25,
                0, 0, 25,
                deleteTexture, 50, 50,
                (bx) -> {
                    if (isHoldingShift)
                        FiguraMod.networkManager.deleteAvatar();
                }
        );
        this.addButton(deleteButton);
        deleteButton.active = false;

        //upload button
        uploadButton = new TexturedButtonWidget(
                this.width / 2 + modelBgSize / 2 + 4, this.height / 2 + modelBgSize / 2 - 25,
                25, 25,
                0, 0, 25,
                uploadTexture, 25, 50,
                (bx) -> {
                    FiguraMod.networkManager.postAvatar().thenRun(()->System.out.println("UPLOADED AVATAR"));
                }
        );
        this.addButton(uploadButton);

        //reload local button
        reloadButton = new TexturedButtonWidget(
                this.width / 2 + modelBgSize / 2 + 4, this.height / 2 + modelBgSize / 2 - 25 - 30,
                25, 25,
                0, 0, 25,
                reloadTexture, 25, 50,
                (bx) -> PlayerDataManager.clearLocalPlayer()
        );
        this.addButton(reloadButton);

        //expand button
        expandButton = new TexturedButtonWidget(
                this.width / 2 - modelBgSize / 2, this.height / 2 - modelBgSize / 2 - 15,
                15, 15,
                0, 0, 15,
                expandTexture, 15, 30,
                (bx) -> {
                    expand = !expand;
                    updateExpand();
                }
        );
        this.addButton(expandButton);

        //reload status
        if (PlayerDataManager.localPlayer != null && PlayerDataManager.localPlayer.model != null) {
            if (PlayerDataManager.lastLoadedFileName != null)
                nameText = new TranslatableText("gui.figura.name", PlayerDataManager.lastLoadedFileName.substring(0, Math.min(20, PlayerDataManager.lastLoadedFileName.length())));
            modelComplexityText = new TranslatableText("gui.figura.complexity", PlayerDataManager.localPlayer.model.getRenderComplexity());
            FiguraMod.doTask(() -> fileSizeText = getFileSizeText());
            scriptText = getScriptText();
        }

        updateExpand();
    }

    @Override
    public void onClose() {
        this.client.openScreen(parentScreen);
    }

    int tickCount = 0;

    @Override
    public void tick() {
        super.tick();

        tickCount++;

        if (tickCount > 20) {
            tickCount = 0;

            //reload model list
            modelFileList.reloadFilters();

            //reload data
            if (PlayerDataManager.localPlayer != null && PlayerDataManager.localPlayer.model != null) {
                if (PlayerDataManager.lastLoadedFileName == null)
                    nameText = null;
                modelComplexityText = new TranslatableText("gui.figura.complexity", PlayerDataManager.localPlayer.model.getRenderComplexity());
                FiguraMod.doTask(() -> fileSizeText = getFileSizeText());
                scriptText = getScriptText();
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);

        //draw player preview
        if (!expand) {
            MinecraftClient.getInstance().getTextureManager().bindTexture(playerBackgroundTexture);
            drawTexture(matrices, this.width / 2 - modelBgSize / 2, this.height / 2 - modelBgSize / 2, 0, 0, modelBgSize, modelBgSize, modelBgSize, modelBgSize);
        }
        else {

            MinecraftClient.getInstance().getTextureManager().bindTexture(scalableBoxTexture);
            drawTexture(matrices, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        }

        drawEntity(modelX, modelY, (int) (modelSize + scaledValue), angleX, angleY, MinecraftClient.getInstance().player);

        //draw search box and file list
        modelFileList.render(matrices, mouseX, mouseY, delta);
        searchBox.render(matrices, mouseX, mouseY, delta);

        //draw text
        if (!expand) {
            int currY = 82;

            if (nameText != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, nameText, this.width - this.textRenderer.getWidth(nameText) - 8, currY += 12, 16777215);
            if (fileSizeText != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, fileSizeText, this.width - this.textRenderer.getWidth(fileSizeText) - 8, currY += 12, 16777215);
            if (modelComplexityText != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, modelComplexityText, this.width - this.textRenderer.getWidth(modelComplexityText) - 8, currY += 12, 16777215);
            if (scriptText != null)
                drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, scriptText, this.width - this.textRenderer.getWidth(scriptText) - 8, currY + 12, 16777215);

            //deprecated warning
            if (rawNameText != null && rawNameText.getString().endsWith("*"))
                drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, new TranslatableText("gui.figura.deprecatedwarning"), this.width / 2, 4, TextColor.parse("red").getRgb());

            //mod version
            drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, new LiteralText("Figura " + FiguraMod.MOD_VERSION).setStyle(Style.EMPTY.withItalic(true)), this.width / 2, this.height - 12, TextColor.parse("dark_gray").getRgb());
        }

        //draw buttons
        super.render(matrices, mouseX, mouseY, delta);

        if (uploadButton.isMouseOver(mouseX, mouseY)){
            matrices.push();
            matrices.translate(0, 0, 599);
            renderTooltip(matrices, uploadTooltip, mouseX, mouseY);
            matrices.pop();
        }

        if (reloadButton.isMouseOver(mouseX, mouseY)){
            matrices.push();
            matrices.translate(0, 0, 599);
            renderTooltip(matrices, reloadTooltip, mouseX, mouseY);
            matrices.pop();
        }

        keybindsButton.active = PlayerDataManager.localPlayer != null && PlayerDataManager.localPlayer.script != null;

        boolean wasKeybindsActive = keybindsButton.active;
        keybindsButton.active = true;
        if (keybindsButton.isMouseOver(mouseX, mouseY)) {
            matrices.push();
            matrices.translate(0, 0, 599);
            renderTooltip(matrices, keybindTooltip, mouseX, mouseY);
            matrices.pop();
        }
        keybindsButton.active = wasKeybindsActive;

        if (!deleteButton.active) {
            deleteButton.active = true;
            boolean mouseOver = deleteButton.isMouseOver(mouseX, mouseY);
            deleteButton.active = false;

            if (mouseOver) {
                matrices.push();
                matrices.translate(0, 0, 599);
                renderTooltip(matrices, deleteTooltip, mouseX, mouseY);
                matrices.pop();
            }
        }
    }

    @Override
    public void renderBackground(MatrixStack matrices) {
        super.renderBackground(matrices);
        overlayBackground(0, 0, this.width, this.height, 64, 64, 64, 255, 255);
    }

    static void overlayBackground(int x1, int y1, int x2, int y2, int red, int green, int blue, int startAlpha, int endAlpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Objects.requireNonNull(MinecraftClient.getInstance()).getTextureManager().bindTexture(DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        buffer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(x1, y2, 0.0D).texture(x1 / 32.0F, y2 / 32.0F).color(red, green, blue, endAlpha).next();
        buffer.vertex(x2, y2, 0.0D).texture(x2 / 32.0F, y2 / 32.0F).color(red, green, blue, endAlpha).next();
        buffer.vertex(x2, y1, 0.0D).texture(x2 / 32.0F, y1 / 32.0F).color(red, green, blue, startAlpha).next();
        buffer.vertex(x1, y1, 0.0D).texture(x1 / 32.0F, y1 / 32.0F).color(red, green, blue, startAlpha).next();
        tessellator.draw();
    }

    private static final int FILESIZE_WARNING_THRESHOLD = 75000;
    private static final int FILESIZE_LARGE_THRESHOLD = 100000;

    public void clickButton(String fileName) {
        PlayerDataManager.lastLoadedFileName = fileName;
        PlayerDataManager.localPlayer.isLocalAvatar = true;
        PlayerDataManager.localPlayer.loadModelFile(fileName);

        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                if (PlayerDataManager.localPlayer.texture.isDone) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            nameText = new TranslatableText("gui.figura.name", fileName.substring(0, Math.min(20, fileName.length())));
            rawNameText = new LiteralText(fileName);
            modelComplexityText = new TranslatableText("gui.figura.complexity", PlayerDataManager.localPlayer.model.getRenderComplexity());
            FiguraMod.doTask(() -> fileSizeText = getFileSizeText());
            scriptText = getScriptText();

        }, Util.getMainWorkerExecutor());
    }

    public MutableText getScriptText() {
        MutableText fsText = new LiteralText("Script: ");

        if (PlayerDataManager.localPlayer.script != null) {
            TranslatableText text;

            //error loading script
            if (PlayerDataManager.localPlayer.script.loadError) {
                text = new TranslatableText("gui.script.error");
                text.setStyle(text.getStyle().withColor(TextColor.parse("red")));
            }
            //loading okei
            else {
                text = new TranslatableText("gui.script.ok");
                text.setStyle(text.getStyle().withColor(TextColor.parse("green")));
            }

            fsText.append(text);
        }
        //script not found
        else {
            TranslatableText text = new TranslatableText("gui.script.none");
            text.setStyle(text.getStyle().withColor(TextColor.parse("white")));
            fsText.append(text);
        }
        return fsText;
    }

    public MutableText getFileSizeText() {
        int fileSize = PlayerDataManager.localPlayer.getFileSize();

        //format file size
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        float size = Float.parseFloat(df.format(fileSize / 1000.0f));

        MutableText fsText = new TranslatableText("gui.figura.filesize", size);

        if (fileSize >= FILESIZE_LARGE_THRESHOLD)
            fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("red")));
        else if (fileSize >= FILESIZE_WARNING_THRESHOLD)
            fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("orange")));
        else
            fsText.setStyle(fsText.getStyle().withColor(TextColor.parse("white")));

        return fsText;
    }

    public void updateExpand() {
        if (expand) {
            this.buttons.forEach(button -> button.visible = false);

            expandButton.visible = true;
            expandButton.setPos(5, 5);

            searchBox.visible = false;
            modelFileList.updateSize(0, 0, this.height, 0);
        } else {
            this.buttons.forEach(button -> button.visible = true);

            expandButton.setPos(this.width / 2 - modelBgSize / 2, this.height / 2 - modelBgSize / 2 - 15);

            searchBox.visible = true;
            modelFileList.updateSize(paneWidth, this.height, paneY + 19, this.height - 36);
            modelFileList.setLeftPos(5);

            scaledValue  = 0.0F;
        }

        modelX = this.width / 2;
        modelY = this.height / 2;
    }

    public void resetModelPos() {
        anchorX = 0.0F;
        anchorY = 0.0F;
        anchorAngleX = 0.0F;
        anchorAngleY = 0.0F;
        angleX = -15.0F;
        angleY = 30.0F;
        scaledValue = 0.0F;
        modelX = this.width / 2;
        modelY = this.height / 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        switch (button) {
            //left click - rotate
            case 0:
                //set anchor rotation
                if ((mouseX >= this.width / 2.0 - modelBgSize / 2.0 && mouseX <= this.width / 2.0 + modelBgSize / 2.0 &&
                        mouseY >= this.height / 2.0 - modelBgSize / 2.0 && mouseY <= this.height / 2.0 + modelBgSize / 2.0) || expand) {
                    //get starter mouse pos
                    anchorX = (float) mouseX;
                    anchorY = (float) mouseY;

                    //get starter rotation angles
                    anchorAngleX = angleX;
                    anchorAngleY = angleY;

                    //enable rotate
                    canRotate = true;
                }
                break;

            //right click - move
            case 1:
                //get starter mouse pos
                dragDeltaX = (float) mouseX;
                dragDeltaY = (float) mouseY;

                //also get start node pos
                dragAnchorX = modelX;
                dragAnchorY = modelY;

                //enable dragging
                canDrag = true;
                break;

            //middle click - reset pos
            case 2:
                canRotate = false;
                canDrag = false;
                resetModelPos();
                break;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        canRotate = false;
        canDrag = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        //set rotations
        if (canRotate) {
            //get starter rotation angle then get hot much is moved and divided by a slow factor
            angleX = (float) (anchorAngleX + (anchorY - mouseY) / (3.0 / guiScale));
            angleY = (float) (anchorAngleY - (anchorX - mouseX) / (3.0 / guiScale));

            //prevent rating so much down and up
            if (angleX > 90) {
                anchorY = (float) mouseY;
                anchorAngleX = 90;
                angleX = 90;
            } else if (angleX < -90) {
                anchorY = (float) mouseY;
                anchorAngleX = -90;
                angleX = -90;
            }
            //cap to 360 so we don't get extremely high unnecessary rotation values
            if (angleY >= 360 || angleY <= -360) {
                anchorX = (float) mouseX;
                anchorAngleY = 0;
                angleY = 0;
            }
        }

        //right click - move
        else if (canDrag && expand) {
            //get how much it should move
            //get actual pos of the mouse, then subtract starter X,Y
            float x = (float) (mouseX - dragDeltaX);
            float y = (float) (mouseY - dragDeltaY);

            //move it
            if (modelX >= 0 && modelX <= this.width)
                modelX = (int) (dragAnchorX + x);
            if (modelY >= 0 && modelY <= this.height)
                modelY = (int) (dragAnchorY + y);

            //if out of range - move it back
            //cant be "elsed" because it needs to be checked after the move
            modelX = modelX < 0 ? 0 : Math.min(modelX, this.width);
            modelY = modelY < 0 ? 0 : Math.min(modelY, this.height);
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        boolean result = super.keyReleased(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
            isHoldingShift = false;
            deleteButton.active = false;
        }

        return result;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        //scroll - scale
        if (expand) {
            //set scale direction
            float scaledir = (amount > 0) ? SCALE_FACTOR : 1 / SCALE_FACTOR;

            //determine scale
            scaledValue = ((modelSize + scaledValue) * scaledir) - modelSize;

            //limit scale
            if (scaledValue <= -35) scaledValue = -35.0F;
            if (scaledValue >= 250) scaledValue = 250.0F;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && expand) {
            expand = false;
            updateExpand();
            return false;
        }

        boolean result = super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
            isHoldingShift = true;
            deleteButton.active = true;
        }

        return result;
    }

    @Override
    public void filesDragged(List<Path> paths) {
        super.filesDragged(paths);

        String string = paths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
        this.client.openScreen(new ConfirmScreen((bl) -> {
            Path destPath = LocalPlayerData.getContentDirectory();
            if (bl) {
                paths.forEach((path2) -> {
                    try {
                        Stream<Path> stream = Files.walk(path2);
                        try {
                            stream.forEach((path3) -> {
                                try {
                                    Util.relativeCopy(path2.getParent(), destPath, path3);
                                } catch (IOException e) {
                                    FiguraMod.LOGGER.error("Failed to copy model file from {} to {}", path3, destPath);
                                    e.printStackTrace();
                                }

                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        stream.close();
                    } catch (Exception e) {
                        FiguraMod.LOGGER.error("Failed to copy model file from {} to {}", path2, destPath);
                        e.printStackTrace();
                    }

                });
            }
            this.client.openScreen(this);
        }, new TranslatableText("gui.figura.dropconfirm"), new LiteralText(string)));
    }

    public static void drawEntity(int x, int y, int size, float rotationX, float rotationY, LivingEntity entity) {
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) x, (float) y, 1500.0F);
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(0.0D, 0.0D, 1000.0D);
        matrixStack.scale((float) size, (float) size, (float) size);
        Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(180.0F);
        Quaternion quaternion2 = Vector3f.POSITIVE_X.getDegreesQuaternion(rotationX);
        quaternion.hamiltonProduct(quaternion2);
        matrixStack.multiply(quaternion);
        float h = entity.bodyYaw;
        float i = entity.yaw;
        float j = entity.pitch;
        float k = entity.prevHeadYaw;
        float l = entity.headYaw;
        boolean invisible = entity.isInvisible();
        entity.bodyYaw = 180.0F - rotationY;
        entity.yaw = 180.0F - rotationY;
        entity.pitch = 0.0F;
        entity.headYaw = entity.yaw;
        entity.prevHeadYaw = entity.yaw;
        entity.setInvisible(false);
        showOwnNametag = (boolean) Config.entries.get("previewNameTag").value;
        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        quaternion2.conjugate();
        entityRenderDispatcher.setRotation(quaternion2);
        entityRenderDispatcher.setRenderShadows(false);
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        int box = modelBgSize * guiScale;
        if (!expand)
            RenderSystem.enableScissor(x * guiScale - box / 2, y * guiScale - box / 2, box, box);
        RenderSystem.runAsFancy(() -> entityRenderDispatcher.render(entity, 0.0D, -1.0D, 0.0D, 0.0F, 1.0F, matrixStack, immediate, 15728880));
        RenderSystem.disableScissor();
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        entity.bodyYaw = h;
        entity.yaw = i;
        entity.pitch = j;
        entity.prevHeadYaw = k;
        entity.headYaw = l;
        entity.setInvisible(invisible);
        showOwnNametag = false;
        RenderSystem.popMatrix();
    }
}