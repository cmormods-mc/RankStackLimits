#!/usr/bin/env python3
from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

build = (ROOT / 'build.gradle').read_text()
mod = json.loads((ROOT / 'src/main/resources/fabric.mod.json').read_text())
mixin_config = json.loads((ROOT / 'src/main/resources/rankstacklimits.mixins.json').read_text())
technical = (ROOT / 'src/main/java/com/cmormods/rankstacklimits/stack/TechnicalStackCeiling.java').read_text()
mixin = (ROOT / 'src/main/java/com/cmormods/rankstacklimits/mixin/ItemStackTechnicalCeilingMixin.java').read_text()
reconciler = (ROOT / 'src/main/java/com/cmormods/rankstacklimits/inventory/PlayerInventoryReconciler.java').read_text()
runtime = (ROOT / 'src/main/java/com/cmormods/rankstacklimits/runtime/RankStackLimitRuntime.java').read_text()
main = (ROOT / 'src/main/java/com/cmormods/rankstacklimits/RankStackLimits.java').read_text()

checks = []

def require(ok, label):
    checks.append((ok, label))
    if not ok:
        print(f'FAIL: {label}')
        sys.exit(1)
    print(f'PASS: {label}')

version_match = re.search(r"version = '(\d+)\.(\d+)\.(\d+)(?:-[^']+)?'", build)
version_tuple = tuple(map(int, version_match.groups())) if version_match else (0, 0, 0)
require(version_tuple >= (0, 4, 0), 'Build is Phase 4 or newer')
require(mod.get('mixins') == ['rankstacklimits.mixins.json'], 'Fabric metadata loads the Phase 4 mixin config')
require(mixin_config.get('required') is True and 'ItemStackTechnicalCeilingMixin' in mixin_config.get('mixins', []),
        'ItemStack technical-ceiling mixin is required')
require('TECHNICAL_MAX = 99' in technical, 'Technical ceiling remains inside the vanilla 1.21.1 limit')
require('DataComponents.MAX_STACK_SIZE' in technical and 'stack.set(DataComponents.MAX_STACK_SIZE, TECHNICAL_MAX)' in technical,
        'Technical ceiling uses the vanilla max_stack_size component')
require('stack.getDefaultComponents()' in technical, 'Intrinsic item limit comes from vanilla/default item components')
require('DataComponents.MAX_DAMAGE' in technical and 'intrinsic <= 1' in technical,
        'Damageable and vanilla max-1 stacks have hard safety guards')
require('PatchedDataComponentMap' in mixin and '<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/core/component/PatchedDataComponentMap;)V' in mixin,
        'Mixin targets the canonical 1.21.1 ItemStack constructor')
require('TechnicalStackCeiling.apply((ItemStack) (Object) this)' in mixin,
        'Constructor hook delegates to the technical-ceiling policy')
require('inventory.getContainerSize()' in reconciler and 'Inventory.INVENTORY_SIZE' in reconciler,
        'Reconciler scans the full player inventory but redistributes only into main inventory slots')
require('ItemStack.isSameItemSameComponents' in reconciler,
        'Overflow merging preserves item/component identity')
require('targetLimit - existing.getCount()' in reconciler and 'Math.min(targetLimit, remaining)' in reconciler,
        'Overflow insertion cannot exceed the resolved player limit')
require('player.drop(template.copyWithCount(chunk), false)' in reconciler and 'if (remaining > 0)' in reconciler,
        'Overflow has a no-destruction drop fallback')
require('getCarried()' in reconciler and 'setCarried(carried)' in reconciler,
        'Cursor stack is reconciled as part of live enforcement')
require('broadcastChanges()' in reconciler and 'inventory.setChanged()' in reconciler,
        'Inventory changes are marked and synchronized')
require('LIMIT_REFRESH_INTERVAL_TICKS = 20' in runtime,
        'LuckPerms limits refresh once per second rather than every tick')
require('ServerPlayerEvents.JOIN.register' in runtime and 'ServerPlayerEvents.AFTER_RESPAWN.register' in runtime
        and 'ServerPlayerEvents.LEAVE.register' in runtime and 'ServerTickEvents.END_SERVER_TICK.register' in runtime,
        'Runtime covers join, respawn, leave, and continuous server-tick enforcement')
require('RankStackLimits.resolver().resolve(player)' in runtime and 'CACHED_LIMITS' in runtime,
        'Runtime uses the validated LuckPerms resolver with a player cache')
require('RankStackLimitRuntime.register()' in main, 'Phase 4 runtime is registered by the mod entrypoint')
combined = '\n'.join([build, technical, mixin, reconciler, runtime, main]).lower()
require('item-components' not in combined and 'itemcomponents' not in combined and 'owo' not in combined,
        'Phase 4 still has no Item Components or owo dependency/reference')

print(f'Phase 4 validation complete: {len(checks)} gates passed.')
