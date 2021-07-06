package net.blancworks.figura.lua.api.world.entity;

import net.blancworks.figura.PlayerDataManager;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.function.Supplier;

public class PlayerEntityAPI {

    public static Identifier getID() {
        return new Identifier("default", "player");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {

        //Global table will get the local player.
        return new PlayerEntityLuaAPITable(() -> script.playerData.lastEntity);
    }

    public static ReadOnlyLuaTable get(PlayerEntity entity) {
        return new PlayerEntityLuaAPITable(() -> entity);
    }

    public static class PlayerEntityLuaAPITable extends LivingEntityAPI.LivingEntityAPITable<PlayerEntity> {

        public PlayerEntityLuaAPITable(Supplier<PlayerEntity> entitySupplier) {
            super(entitySupplier);
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable superTable = super.getTable();

            superTable.set("getHeldItem", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {

                    int hand = arg.checkint();

                    ItemStack targetStack;

                    if (hand == 1)
                        targetStack = targetEntity.get().getMainHandStack();
                    else if (hand == 2)
                        targetStack = targetEntity.get().getOffHandStack();
                    else
                        return NIL;

                    if (targetStack.equals(ItemStack.EMPTY))
                        return NIL;

                    return ItemStackAPI.getTable(targetStack);
                }
            });

            superTable.set("getFood", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getHungerManager().getFoodLevel());
                }
            });

            superTable.set("getSaturation", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getHungerManager().getSaturationLevel());
                }
            });

            superTable.set("getExperienceProgress", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().experienceProgress);
                }
            });

            superTable.set("getExperienceLevel", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().experienceLevel);
                }
            });

            superTable.set("getTargetedEntity", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (targetEntity.get() instanceof ClientPlayerEntity) {
                        Entity lookingAt = MinecraftClient.getInstance().targetedEntity;

                        if (lookingAt != null && !lookingAt.isInvisibleTo(targetEntity.get())) {
                            if (lookingAt instanceof PlayerEntity) {
                                return new PlayerEntityLuaAPITable(() -> (PlayerEntity) lookingAt).getTable();
                            }
                            else if (lookingAt instanceof LivingEntity) {
                                return new LivingEntityAPI.LivingEntityAPITable<>(() -> (LivingEntity) lookingAt).getTable();
                            }
                            else {
                                return new EntityAPI.EntityLuaAPITable<>(() -> lookingAt).getTable();
                            }
                        }
                    }

                    return NIL;
                }
            });

            superTable.set("lastDamageSource", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    DamageSource ds = PlayerDataManager.getDataForPlayer(targetEntity.get().getUuid()).script.lastDamageSource;
                    return ds == null ? NIL : LuaValue.valueOf(ds.name);
                }
            });

            return superTable;
        }

        @Override
        public LuaValue rawget(LuaValue key) {
            if (targetEntity.get() == null)
                throw new LuaError("Player Entity does not exist yet! Do NOT try to access the player in init! Do it in player_init instead!");
            return super.rawget(key);
        }
    }

}
