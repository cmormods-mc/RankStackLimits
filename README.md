# RankStackLimits

Fabric 1.21.1 server-side mod for LuckPerms-driven per-player stack limits.

## Current v1 scope

- Minecraft 1.21.1 / Fabric
- Java 21
- LuckPerms meta-based limits using `stack-limit`
- Supported player limit range: 64–99
- Vanilla max-stack-1 items protected by default
- Safe downgrade redistribution policy
- No Item Components or owo-lib dependency

Example LuckPerms setup:

```text
/lp group default meta set stack-limit 64
/lp group vip meta set stack-limit 80
/lp group vipplus meta set stack-limit 96
/lp group legend meta set stack-limit 99
```

The generated server config is `config/rankstacklimits.json`.

Inventory enforcement hooks are the next implementation phase; the current repository contains the validated configuration, LuckPerms resolution, eligibility, and redistribution policy layers.
