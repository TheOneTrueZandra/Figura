package net.blancworks.figura.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import net.blancworks.figura.Config;
import net.blancworks.figura.PlayerData;
import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.actionWheel.ActionWheelCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.text.*;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaError;

public class ActionWheel extends DrawableHelper {

    private final MinecraftClient client;

    public static final Identifier ACTION_WHEEL = new Identifier("figura", "textures/gui/action_wheel.png");
    public static final Identifier ACTION_WHEEL_SELECTED = new Identifier("figura", "textures/gui/action_wheel_selected.png");
    public static final Vector3f ERROR_COLOR = new Vector3f(1.0f, 0.28f, 0.28f);

    public static int selectedSlot = -1;
    public static boolean enabled = true;

    public ActionWheel(MinecraftClient client) {
        this.client = client;
    }

    public void render(MatrixStack matrices) {
        //screen
        Vec2f screenSize = new Vec2f(this.client.getWindow().getWidth() / 2.0f, this.client.getWindow().getHeight() / 2.0f);
        float screenScale = (float) this.client.getWindow().getScaleFactor();

        //mouse
        Vec2f mousePos = new Vec2f((float) this.client.mouse.getX() - screenSize.x, (float) this.client.mouse.getY() - screenSize.y);
        float angle = getAngle(mousePos.x, mousePos.y);
        float distance = MathHelper.sqrt(mousePos.x * mousePos.x + mousePos.y * mousePos.y);

        //wheel
        Vec2f wheelPos = new Vec2f(screenSize.x / screenScale, screenSize.y / screenScale);
        int wheelSize = 192;

        //item
        Vec2f itemOffset = new Vec2f((wheelPos.x * 2.0f / 3.0f) - 8, (wheelPos.y * 2.0f / 3.0f) - 8);
        int itemRadius = 42;

        //script data
        PlayerData data = PlayerDataManager.localPlayer;

        //render
        RenderSystem.enableBlend();

        if (data != null && data.script != null) {
            int leftSegments = data.script.actionWheelLeftSize;
            int rightSegments = data.script.actionWheelRightSize;

            //set selected slot
            if (distance > 30 * screenScale) {
                if (angle < 180) {
                    selectedSlot = MathHelper.floor((rightSegments / 180.0) * angle);
                } else {
                    selectedSlot = MathHelper.floor((leftSegments / 180.0) * (angle - 180)) + 4;
                }
            } else {
                selectedSlot = -1;
            }

            //render wheel
            renderWheel(matrices, wheelPos, wheelSize, leftSegments, rightSegments);

            //render overlay
            for (int i = 0; i < leftSegments + rightSegments; i++) {
                int index = i < rightSegments ? i : i - rightSegments + 4;
                renderOverlay(matrices, wheelPos, wheelSize, leftSegments, rightSegments, data, index);
            }

            //render text
            if (selectedSlot != -1) {
                renderText(matrices, wheelPos, wheelSize, screenScale, data);
            }

            //render items
            renderItems(leftSegments, rightSegments, itemOffset, itemRadius, data);
        }
        else {
            //draw default wheel
            renderWheel(matrices, wheelPos, wheelSize, 4, 4);

            //draw warning texts
            drawCenteredText(
                    matrices, MinecraftClient.getInstance().textRenderer,
                    new TranslatableText("gui.figura.actionwheel.warning").formatted(Formatting.UNDERLINE),
                    (int) wheelPos.x, (int) wheelPos.y - 4,
                    16733525
            );
            drawCenteredText(
                    matrices, MinecraftClient.getInstance().textRenderer,
                    new TranslatableText("gui.figura.actionwheel.warninginfo"),
                    (int) wheelPos.x, (int) Math.max(wheelPos.y - wheelSize / 2.0 - 10, 4),
                    16733525
            );
        }

        RenderSystem.disableBlend();
    }

    public float getAngle(float x, float y) {
        float ang = (float) Math.toDegrees(MathHelper.atan2(x, -y));
        return ang < 0 ? 360 + ang : ang;
    }

    public void renderWheel(MatrixStack matrices, Vec2f pos, int size, int leftSegments, int rightSegments) {
        //texture
        this.client.getTextureManager().bindTexture(ACTION_WHEEL);

        //draw right side
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(pos.y - size / 2.0d), 0.0d);
        drawTexture(matrices, 0, 0, size / 2, size, 8.0f * (rightSegments - 1), 0.0f, 8, 16, 32, 16);

        matrices.pop();

        //draw left side
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(pos.y + size / 2.0d), 0.0d);
        Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(180);
        matrices.multiply(quaternion);

        drawTexture(matrices, 0, 0, size / 2, size, 8.0f * (leftSegments - 1), 0.0f, 8, 16, 32, 16);

        matrices.pop();
    }

    public void renderOverlay(MatrixStack matrices, Vec2f pos, int size, int leftSegments, int rightSegments, PlayerData data, int slot) {
        ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (slot + 1));

        //property variables
        boolean hasFunction = false;
        boolean hasColor = false;
        boolean hasHoverColor = false;
        boolean isSelected = selectedSlot == slot;
        Vector3f overlayColor = new Vector3f(1.0f, 1.0f, 1.0f);

        if (customization != null) {
            hasFunction = customization.function != null;
            hasColor = customization.color != null;
            hasHoverColor = customization.hoverColor != null;
        }

        //set default color
        if (hasColor)
            overlayColor = customization.color;

        //if is selected, but has no function, set to error color
        //if it has function and has an hover color, set to the hover color
        if (isSelected) {
            if (!hasFunction) {
                overlayColor = ERROR_COLOR;
            } else if (hasHoverColor) {
                overlayColor = customization.hoverColor;
            }
        } else if (!hasColor) {
            return;
        }

        //modifiable variables
        int segments;
        int selected;

        if (slot < 4) {
            segments = rightSegments;
            selected = slot;
        } else {
            segments = leftSegments;
            selected = slot - 4 + leftSegments;
        }

        double y = pos.y;
        float angle = 0.0f;
        int height = size / 2;
        float u = 0.0f;
        float v = 0.0f;
        int regionHeight = 8;

        switch (segments) {
            case 1: {
                y = selected % 2 == 1 ? pos.y + size / 2.0d : pos.y - size / 2.0d;
                angle = 180f * selected;
                height = size;
                regionHeight = 16;
                break;
            }
            case 2: {
                angle = 90f * (selected - 1f);
                u = 8.0f;
                break;
            }
            case 3: {
                if (selected % 3 != 2) {
                    y += (selected < 3 ? -1 : 1) * size / 2.0d;

                    if (selected % 3 == 1) {
                        y += (selected < 3 ? 1 : -1) * size / 4.0d;
                        v = 8.0f;
                    }

                    u = 16.0f;
                }
                else {
                    u = 8.0f;
                    v = 8.0f;
                }

                angle = 180f * MathHelper.floor(selected / 3.0d);
                break;
            }
            case 4: {
                angle = 90f * (MathHelper.floor(selected / 2.0d) + 3f);
                u = 24.0f;
                v = selected % 2 == 1 ? 8.0f : 0.0f;
                break;
            }
        }

        //texture
        this.client.getTextureManager().bindTexture(ACTION_WHEEL_SELECTED);

        //draw
        matrices.push();

        matrices.translate(Math.round(pos.x), Math.round(y), 0.0d);
        Quaternion quaternion = Vector3f.POSITIVE_Z.getDegreesQuaternion(angle);
        matrices.multiply(quaternion);

        RenderSystem.color3f(overlayColor.getX(), overlayColor.getY(), overlayColor.getZ());
        drawTexture(matrices, 0, 0, size / 2, height, u, v, 8, regionHeight, 32, 16);

        matrices.pop();
    }

    public void renderText(MatrixStack matrices, Vec2f pos, int size, float scale, PlayerData data) {
        //customization
        ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));

        Text text = new TranslatableText("gui.figura.actionwheel.nofunction");
        int textColor = Formatting.RED.getColorValue();

        if (customization != null && customization.function != null) {
            if (customization.title == null)
                return;

            try {
                text = Text.Serializer.fromJson(new StringReader(customization.title));
            } catch (Exception ignored) {
                text = new LiteralText(customization.title);
            }

            textColor = Formatting.WHITE.getColorValue();
        }

        //text pos
        Vec2f textPos;
        int titleLen = this.client.textRenderer.getWidth(text) / 2;

        switch ((int) Config.entries.get("actionWheelPos").value) {
            //top
            case 1: textPos = new Vec2f(pos.x - titleLen, (float) Math.max(pos.y - size / 2.0 - 10, 4)); break;
            //bottom
            case 2: textPos = new Vec2f(pos.x - titleLen, (float) Math.min(pos.y + size / 2.0 + 4, this.client.getWindow().getHeight() - 12)); break;
            //center
            case 3: textPos = new Vec2f(pos.x - titleLen, pos.y - 4); break;
            //default mouse
            default: textPos = new Vec2f((float) this.client.mouse.getX() / scale, (float) this.client.mouse.getY() / scale - 10); break;
        }

        //draw
        matrices.push();
        matrices.translate(0, 0, 599);
        drawTextWithShadow(matrices, this.client.textRenderer, text, (int) textPos.x, (int) textPos.y, textColor);
        matrices.pop();
    }

    public void renderItems(int leftSegments, int rightSegments, Vec2f offset, int radius, PlayerData data) {
        for (int i = 0; i < leftSegments + rightSegments; i++) {

            int index;
            float angle;
            if (i < rightSegments) {
                index = i;
                angle = (float) Math.toRadians(180.0 / rightSegments * (index - ((rightSegments - 1) * 0.5)));
            } else {
                index = i - rightSegments + 4;
                angle = (float) Math.toRadians(180.0 / leftSegments * (index - 4 - ((leftSegments - 1) * 0.5) + leftSegments));
            }

            //get item - defaults to air
            ItemStack item = Registry.ITEM.get(Identifier.tryParse("minecraft:air")).getDefaultStack();

            ActionWheelCustomization customization = data.script.getActionWheelCustomization("SLOT_" + (index + 1));

            if (customization != null) {
                if (selectedSlot == index && customization.hoverItem != null) {
                    item = customization.hoverItem;
                } else if (customization.item != null) {
                    item = customization.item;
                }
            }

            //radius * cos/sin angle in rads + offset
            Vec2f pos = new Vec2f(radius * MathHelper.cos(angle) + offset.x, radius * MathHelper.sin(angle) + offset.y);

            //render
            RenderSystem.pushMatrix();
            RenderSystem.scalef(1.5f, 1.5f, 1.5f);

            this.client.getItemRenderer().renderGuiItemIcon(item, (int) pos.x, (int) pos.y);

            RenderSystem.popMatrix();
        }
    }

    public static void play() {
        if (selectedSlot != -1) {
            PlayerData currentData = PlayerDataManager.localPlayer;

            if (currentData != null && currentData.script != null) {
                ActionWheelCustomization customization = currentData.script.getActionWheelCustomization("SLOT_" + (selectedSlot + 1));

                if (customization != null && customization.function != null) {
                    try {
                        customization.function.call();
                    } catch (Exception error) {
                        if (error instanceof LuaError) {
                            String msg = error.getMessage();
                            msg = msg.replace("\t", "   ");
                            String[] messageParts = msg.split("\n");

                            for (String part : messageParts) {
                                CustomScript.sendChatMessage(new LiteralText(part).setStyle(Style.EMPTY.withColor(TextColor.parse("red"))));
                            }
                        }

                        error.printStackTrace();
                    }
                }
            }

            enabled = false;
            selectedSlot = -1;
        }
    }
}
