# RankStackLimits

Fabric 1.21.1 server-side mod for LuckPerms-driven per-player stack limits.

## Current v1 scope

- Minecraft 1.21.1 / Fabric
- Java 21
- LuckPerms meta-based limits using `stack-limit`
- Supported player limit range: 64–99
- Vanilla max-stack-1 and damageable items protected
- Safe rank-downgrade redistribution with no item deletion
- Live player-inventory and cursor enforcement
- LuckPerms limits refreshed every 20 server ticks
- No Item Components or owo-lib dependency

Example LuckPerms setup:

```text
/lp group default meta set stack-limit 64
/lp group vip meta set stack-limit 80
/lp group vipplus meta set stack-limit 96
/lp group legend meta set stack-limit 99
```

The generated server config is `config/rankstacklimits.json`.

## Phase 4 enforcement model

Minecraft 1.21.1's vanilla `max_stack_size` data component is used as a shared technical ceiling of 99 for eligible stacks. Every eligible stack uses the same ceiling so stacks remain component-compatible when they move between the world, inventories, and containers. A server-side reconciler then enforces each player's LuckPerms-resolved count limit in their personal inventory and cursor.

When a player's rank is reduced, oversized stacks are merged into other player inventory slots without exceeding the new limit. If there is not enough room, excess is dropped in safe chunks. If a drop cannot be created, the remainder is restored instead of being deleted.

### Current boundary

Phase 4 enforces the player's own inventory and cursor. External container storage (for example chests and machine inventories) still uses the shared technical ceiling and is not yet given per-player slot semantics. Container/shift-click hardening is the next phase; this limitation is kept explicit rather than hiding it behind unsafe global item mutations.
