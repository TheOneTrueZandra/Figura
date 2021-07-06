package net.blancworks.figura.lua.api.world.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.function.Supplier;

public class LivingEntityAPI {
    
    
    public static class LivingEntityAPITable<T extends LivingEntity> extends EntityAPI.EntityLuaAPITable<T> {

        public LivingEntityAPITable(Supplier<T> targetEntity) {
            super(targetEntity);
        }

        @Override
        public LuaTable getTable() {
            LuaTable superTable = super.getTable();

            superTable.set("getBodyYaw", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().bodyYaw);
                }
            });
            
            superTable.set("getHealth", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getHealth());
                }
            });

            superTable.set("getMaxHealth", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getMaxHealth());
                }
            });

            superTable.set("getHealthPercentage", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getHealth() / targetEntity.get().getMaxHealth());
                }
            });

            superTable.set("getArmor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getArmor());
                }
            });

            superTable.set("getDeathTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().deathTime);
                }
            });

            superTable.set("getStatusEffectTypes", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    LuaTable effects = new LuaTable();

                    int i = 1;
                    for (StatusEffectInstance inst : targetEntity.get().getStatusEffects()) {
                        effects.set(i, LuaString.valueOf(Registry.STATUS_EFFECT.getId(inst.getEffectType()).toString()));
                        i++;
                    }

                    return effects;
                }
            });

            superTable.set("getStatusEffect", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Identifier effectId = Identifier.tryParse(arg.checkjstring());
                    if(effectId == null)
                        return NIL;

                    StatusEffect statusEffect = Registry.STATUS_EFFECT.get(effectId);

                    if (!targetEntity.get().hasStatusEffect(statusEffect))
                        return NIL;

                    LuaTable effect = new LuaTable();
                    StatusEffectInstance instance = targetEntity.get().getStatusEffect(statusEffect);
                    effect.set("duration", instance.getDuration());
                    effect.set("amplifier", instance.getAmplifier());

                    return effect;
                }
            });

            superTable.set("isSneaky", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().isSneaky());
                }
            });

            superTable.set("getStuckArrowCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getStuckArrowCount());
                }
            });

            superTable.set("getStingerCount", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(targetEntity.get().getStingerCount());
                }
            });

            superTable.set("isLeftHanded", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetEntity.get().getMainArm() == Arm.LEFT);
                }
            });

            return superTable;
        }
    }
}
