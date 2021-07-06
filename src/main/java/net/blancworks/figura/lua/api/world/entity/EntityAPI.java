package net.blancworks.figura.lua.api.world.entity;

import net.blancworks.figura.lua.api.NBTAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.item.ItemStackAPI;
import net.blancworks.figura.lua.api.math.LuaVector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Iterator;
import java.util.function.Supplier;

public class EntityAPI {

    public static class EntityLuaAPITable<T extends Entity> extends ReadOnlyLuaTable {

        public Supplier<T> targetEntity;
        
        public EntityLuaAPITable(Supplier<T> targetEntity) {
            this.targetEntity = targetEntity;
        }


        public LuaTable getTable() {

            return new LuaTable() {{

                set("getPos", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        Entity e = targetEntity.get();
                        return LuaVector.of(e.getPos());
                    }
                });

                set("getRot", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return new LuaVector(targetEntity.get().pitch, targetEntity.get().yaw);
                    }
                });

                set("getType", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaString.valueOf(Registry.ENTITY_TYPE.getId(targetEntity.get().getType()).toString());
                    }
                });

                set("getVelocity", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaVector.of(targetEntity.get().getVelocity());
                    }
                });

                set("getLookDir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaVector.of(targetEntity.get().getRotationVector());
                    }
                });

                set("getUUID", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaString.valueOf(targetEntity.get().getUuid().toString());
                    }
                });

                set("getFireTicks", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.get().getFireTicks());
                    }
                });

                set("getFrozenTicks", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(0);
                    }
                });

                set("getAir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.get().getAir());
                    }
                });

                set("getMaxAir", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.get().getMaxAir());
                    }
                });

                set("getAirPercentage", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(((float)targetEntity.get().getAir()) / targetEntity.get().getMaxAir());
                    }
                });

                set("getWorldName", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        if (targetEntity == null) return NIL;
                        World w = targetEntity.get().world;

                        return LuaString.valueOf(w.getRegistryKey().getValue().toString());
                    }
                });

                set("getEquipmentItem", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        int index = arg.checkint() - 1;
                        ItemStack stack = retrieveItemByIndex(targetEntity.get().getItemsEquipped(), index);
                        return ItemStackAPI.getTable(stack);
                    }
                });

                set("getAnimation", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        if (targetEntity == null) return NIL;

                        EntityPose p = targetEntity.get().getPose();

                        if (p == null)
                            return NIL;

                        return LuaString.valueOf(p.name());
                    }
                });

                set("getVehicle", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        if (targetEntity.get().getVehicle() == null) return NIL;

                        Entity vehicle = targetEntity.get().getVehicle();

                        if (vehicle instanceof PlayerEntity) {
                            return new PlayerEntityAPI.PlayerEntityLuaAPITable(() -> (PlayerEntity) vehicle).getTable();
                        }
                        else if (vehicle instanceof LivingEntity) {
                            return new LivingEntityAPI.LivingEntityAPITable<>(() -> (LivingEntity) vehicle).getTable();
                        }
                        else {
                            return new EntityLuaAPITable<>(() -> vehicle).getTable();
                        }
                    }
                });

                set("isGrounded", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaBoolean.valueOf(targetEntity.get().isOnGround());
                    }
                });

                set("getEyeHeight", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaNumber.valueOf(targetEntity.get().getEyeHeight(targetEntity.get().getPose()));
                    }
                });

                set("getBoundingBox", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        Entity ent = targetEntity.get();
                        EntityDimensions dims = ent.getDimensions(ent.getPose());
                        float x = dims.width;
                        float y = dims.height;
                        float z = dims.width;

                        return new LuaVector(x, y, z);
                    }
                });

                set("getName", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        verifyEntityExists();
                        
                        Entity ent = targetEntity.get();
                        if (ent.hasCustomName() && ent.getCustomName() != null)
                            return LuaValue.valueOf(ent.getCustomName().getString());
                        else
                            return LuaValue.valueOf(ent.getName().getString());
                    }
                });

                set("getTargetedBlockPos", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        HitResult result = targetEntity.get().raycast(20.0D, 0.0F, arg.checkboolean());
                        if (result.getType() == HitResult.Type.BLOCK) {
                            return LuaVector.of(((BlockHitResult) result).getBlockPos());
                        } else {
                            return NIL;
                        }
                    }
                });

                set("getNbtValue", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        String pathArg = arg.checkjstring();

                        String[] path = pathArg.split("\\.");

                        CompoundTag tag = new CompoundTag();
                        targetEntity.get().toTag(tag);

                        Tag current = tag;
                        for (String key : path) {
                            if (current == null)
                                current = tag.get(key);
                            else if (current instanceof CompoundTag)
                                current = ((CompoundTag)current).get(key);
                            else current = null;
                        }

                        if (current == null) return NIL;

                        return NBTAPI.fromTag(current);
                    }
                });

            }};
        }

        @Override
        public LuaValue rawget(LuaValue key) {
            verifyEntityExists();
            return super.rawget(key);
        }
        

        private static <T> T retrieveItemByIndex(Iterable<T> iterable, int index) {

            if (iterable == null || index < 0) {

                return null;
            }

            int cursor = 0;

            Iterator<T> iterator = iterable.iterator();

            while (cursor < index && iterator.hasNext()) {

                iterator.next();
                cursor++;
            }

            return cursor == index && iterator.hasNext() ? iterator.next() : null;
        }

        protected void verifyEntityExists(){
            if(targetEntity.get() == null)
                throw new LuaError("Entity does not exist!");
        }
        
    }
}
