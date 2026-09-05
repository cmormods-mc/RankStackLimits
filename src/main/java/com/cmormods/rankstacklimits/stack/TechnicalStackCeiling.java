package com.cmormods.rankstacklimits.stack;

import com.cmormods.rankstacklimits.policy.StackEligibilityPolicy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class TechnicalStackCeiling {
    public static final int TECHNICAL_MAX = 99;

    private TechnicalStackCeiling() {
    }

    public static int intrinsicLimit(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Integer intrinsic = stack.getDefaultComponents().get(DataComponents.MAX_STACK_SIZE);
        return intrinsic == null ? 1 : intrinsic;
    }

    public static boolean apply(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        int intrinsic = intrinsicLimit(stack);
        if (intrinsic <= 1 || stack.get(DataComponents.MAX_DAMAGE) != null) {
            return false;
        }

        int current = stack.getMaxStackSize();
        if (current == TECHNICAL_MAX) {
            return false;
        }

        // Respect another mod or custom stack that deliberately supplied a different ceiling.
        if (current != intrinsic) {
            return false;
        }

        stack.set(DataComponents.MAX_STACK_SIZE, TECHNICAL_MAX);
        return true;
    }

    public static int effectivePlayerLimit(ItemStack stack, int resolvedPlayerLimit, boolean preserveVanillaUnstackables) {
        if (stack.isEmpty()) {
            return 0;
        }

        int intrinsic = intrinsicLimit(stack);
        int current = stack.getMaxStackSize();

        // V1 hard safety: damageable and intrinsic max-1 stacks are never expanded.
        if (intrinsic <= 1 || stack.get(DataComponents.MAX_DAMAGE) != null) {
            return Math.min(current, 1);
        }

        // If a different per-stack ceiling is present, do not override that mod's policy.
        if (current != intrinsic && current != TECHNICAL_MAX) {
            return current;
        }

        return Math.min(TECHNICAL_MAX,
                StackEligibilityPolicy.effectiveLimit(intrinsic, resolvedPlayerLimit, preserveVanillaUnstackables));
    }
}
