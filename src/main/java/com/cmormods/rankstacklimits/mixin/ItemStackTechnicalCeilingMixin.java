package com.cmormods.rankstacklimits.mixin;

import com.cmormods.rankstacklimits.stack.TechnicalStackCeiling;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackTechnicalCeilingMixin {
    @Inject(
            method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/core/component/PatchedDataComponentMap;)V",
            at = @At("TAIL")
    )
    private void rankstacklimits$applyTechnicalCeiling(
            ItemLike item,
            int count,
            PatchedDataComponentMap components,
            CallbackInfo ci
    ) {
        TechnicalStackCeiling.apply((ItemStack) (Object) this);
    }
}
