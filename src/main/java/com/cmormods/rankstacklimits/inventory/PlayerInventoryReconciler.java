package com.cmormods.rankstacklimits.inventory;

import com.cmormods.rankstacklimits.config.RankStackLimitsConfig;
import com.cmormods.rankstacklimits.stack.TechnicalStackCeiling;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class PlayerInventoryReconciler {
    private final RankStackLimitsConfig config;

    public PlayerInventoryReconciler(RankStackLimitsConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public boolean reconcile(ServerPlayer player, int resolvedPlayerLimit) {
        Inventory inventory = player.getInventory();
        boolean changed = false;

        // Normalize the player's own stacks to the shared technical ceiling first so
        // component equality remains stable before any overflow is redistributed.
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            changed |= TechnicalStackCeiling.apply(inventory.getItem(slot));
        }

        if (player.containerMenu != null) {
            changed |= TechnicalStackCeiling.apply(player.containerMenu.getCarried());
        }

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            int targetLimit = TechnicalStackCeiling.effectivePlayerLimit(
                    stack,
                    resolvedPlayerLimit,
                    config.preserveVanillaUnstackables()
            );
            if (targetLimit < 1 || stack.getCount() <= targetLimit) {
                continue;
            }

            changed = true;
            int overflow = stack.getCount() - targetLimit;
            stack.setCount(targetLimit);

            ItemStack template = stack.copy();
            int remaining = insertIntoMainInventory(inventory, template, overflow, targetLimit, slot);
            remaining = dropOverflow(player, template, remaining, targetLimit);

            // Never destroy items. If dropping was prevented, leave the unsent remainder
            // on the source stack and try again on a later tick.
            if (remaining > 0) {
                stack.setCount(stack.getCount() + remaining);
            }
        }

        if (player.containerMenu != null) {
            ItemStack carried = player.containerMenu.getCarried();
            if (!carried.isEmpty()) {
                int targetLimit = TechnicalStackCeiling.effectivePlayerLimit(
                        carried,
                        resolvedPlayerLimit,
                        config.preserveVanillaUnstackables()
                );
                if (targetLimit > 0 && carried.getCount() > targetLimit) {
                    changed = true;
                    int overflow = carried.getCount() - targetLimit;
                    carried.setCount(targetLimit);

                    ItemStack template = carried.copy();
                    int remaining = insertIntoMainInventory(inventory, template, overflow, targetLimit, -1);
                    remaining = dropOverflow(player, template, remaining, targetLimit);
                    if (remaining > 0) {
                        carried.setCount(carried.getCount() + remaining);
                    }
                    player.containerMenu.setCarried(carried);
                }
            }
        }

        if (changed) {
            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
        }

        return changed;
    }

    private static int insertIntoMainInventory(
            Inventory inventory,
            ItemStack template,
            int amount,
            int targetLimit,
            int excludedSlot
    ) {
        int remaining = amount;

        // Merge first, but never beyond this player's resolved limit.
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE && remaining > 0; slot++) {
            if (slot == excludedSlot) {
                continue;
            }
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, template)) {
                continue;
            }

            int space = targetLimit - existing.getCount();
            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining);
            existing.setCount(existing.getCount() + moved);
            remaining -= moved;
        }

        // Then use empty main-inventory slots, again capped by the player's limit.
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE && remaining > 0; slot++) {
            if (slot == excludedSlot || !inventory.getItem(slot).isEmpty()) {
                continue;
            }

            int moved = Math.min(targetLimit, remaining);
            inventory.setItem(slot, template.copyWithCount(moved));
            remaining -= moved;
        }

        return remaining;
    }

    private static int dropOverflow(ServerPlayer player, ItemStack template, int amount, int targetLimit) {
        int remaining = amount;
        while (remaining > 0) {
            int chunk = Math.min(targetLimit, remaining);
            if (player.drop(template.copyWithCount(chunk), false) == null) {
                return remaining;
            }
            remaining -= chunk;
        }
        return 0;
    }
}
