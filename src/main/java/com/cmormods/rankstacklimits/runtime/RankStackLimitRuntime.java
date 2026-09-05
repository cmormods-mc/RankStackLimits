package com.cmormods.rankstacklimits.runtime;

import com.cmormods.rankstacklimits.RankStackLimits;
import com.cmormods.rankstacklimits.inventory.PlayerInventoryReconciler;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RankStackLimitRuntime {
    private static final int LIMIT_REFRESH_INTERVAL_TICKS = 20;
    private static final Map<UUID, Integer> CACHED_LIMITS = new HashMap<>();

    private static boolean registered;
    private static int tickCounter;
    private static PlayerInventoryReconciler reconciler;

    private RankStackLimitRuntime() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        reconciler = new PlayerInventoryReconciler(RankStackLimits.config());

        ServerPlayerEvents.JOIN.register(player -> {
            int limit = refreshLimit(player);
            reconciler.reconcile(player, limit);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            CACHED_LIMITS.remove(oldPlayer.getUUID());
            int limit = refreshLimit(newPlayer);
            reconciler.reconcile(newPlayer, limit);
        });

        ServerPlayerEvents.LEAVE.register(player -> CACHED_LIMITS.remove(player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(RankStackLimitRuntime::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        tickCounter++;
        boolean refreshThisTick = tickCounter >= LIMIT_REFRESH_INTERVAL_TICKS;
        if (refreshThisTick) {
            tickCounter = 0;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int limit;
            if (refreshThisTick) {
                limit = refreshLimit(player);
            } else {
                limit = CACHED_LIMITS.computeIfAbsent(
                        player.getUUID(),
                        ignored -> RankStackLimits.resolver().resolve(player)
                );
            }
            reconciler.reconcile(player, limit);
        }
    }

    private static int refreshLimit(ServerPlayer player) {
        int limit = RankStackLimits.resolver().resolve(player);
        CACHED_LIMITS.put(player.getUUID(), limit);
        return limit;
    }
}
