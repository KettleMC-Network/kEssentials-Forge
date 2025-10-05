# kEssentials (Forge 1.18.2)

Gradle project (Java 17) for Forge 1.18.2 with Essentials-like features, robust Vanish, Invsee Pro, RTP safe-scan, multi-language chat, and a Discord bridge (JDA) incl. whitelist linking.

## Features

### Chat, tablist & presentation

* **Animated tab header/footer & scoreboard** with configurable titles, lines and LuckPerms-aware placeholders; updates every second and reacts to vanish/name display changes.
* **Adventure MiniMessage MOTD** applied on start and via `/kchat` reload; automatically refreshed on schedule.
* **Chat formatting pipeline** keeps mute support, injects placeholders (LuckPerms prefix/suffix, statistics, world info, animations) and can mirror messages to Discord.
* **Name display & tab-order integration** sorts players by LuckPerms group weight/meta and exposes prefixes above heads (optional hide when vanished).

### Teleportation & player utilities

* Essentials-style **homes, warps, spawn, back** with configurable warmups/cooldowns, biome-aware RTP scanning, previous-location tracking and per-player home limits derived from permissions or meta.
* **TPA queue** with `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tplist`, `/tptoggle`, movement-cancelling warmups and 60s expiry.
* Quick-access utilities: `/top`, `/jump`, `/hat`, `/material`, workbench/anvil/enchant interfaces, ender chest/see menus, heal/feed/repair, speed/fly/gamemode/god toggles, extinguisher etc.

### Moderation & staff tooling

* **Vanish** toggles invisibility, silence, no-clip, fake join/leave broadcasts and hides vanished players from newcomers, while updating tab/name displays.
* **Freeze/mute, social spy & god mode** leverage a persistent state service; frozen players cannot move, interact or break/place blocks, god cancels damage, spies mirror private messages.
* **Inventory inspection**: `/invsee`, `/invsee <player> true`, `/armorsee`, `/endersee`, `/enderchest` (self or others) with optional modify permission and user feedback.
* **Chat administration** with `/chatclear`, `/f3d`, `/broadcast`, `/msg`, `/reply`, `/socialspy`, `/near`, `/seen`, `/clear`, `/repair`, `/ext` and `/link` for Discord whitelisting.

### Discord bridge & account linking

* **Two-way chat relay** (optionally formatted), join/quit announcements, ClearLagg command relays and presence updates configurable via `discord.properties`.
* **Bot commands**: Discord text commands `!online`, `!say`, `!kick`, `!link <code>` plus slash commands `/online`, optional `/stop`, `/link` with whitelist integration via the link service and `/link` in-game.

### Automation & configuration

* **Warmups, cooldowns & cancel-on-move** logic shared across teleports; per-command timing configurable in `config.json` (defaults provided).
* **Scheduled restart announcer**, per-command disable/override messages, custom join actions and command-triggered console/player actions.
* **Custom join/leave messages** with vanish awareness and ability to fake messages when toggling vanish.

### Internationalisation

* Locale service with default + per-player overrides, bundled `messages_en.properties`/`messages_de.properties` and hot-reload via `/kessentials reload` or `/kchat reload`. Server overrides can live in `config/kessentials/lang/`.

## Build

```bash
./gradlew build
```

Jar: `build/libs/kessentials-forge-0.3.1.jar`

## Setup

* Forge 1.18.2-40.x
* Install LuckPerms-Forge (permissions & meta).
* Configure `config/kessentials/discord.properties` (`token=`, `channelId=`).

## Configuration highlights

`config/kessentials/config.json` is created with sensible defaults and supports:

* `defaultMaxHomes` (int)
* `cooldownsSeconds` / `warmupsSeconds` — map of command → seconds, e.g. `{ "home": 3, "warp": 3, "spawn": 3, "rtp": 30 }`
* `rtpBiomeBlacklist` — disallowed biomes for random teleport
* `vanishFakeMessages` — toggles fake join/leave broadcasts when vanishing
* `customJoinLeaveMessages` — disable if you prefer vanilla messages
* `disabledCommands` — map of root literal → optional override message (cancels execution)
* `onJoinActions` — server commands (supports `player:`, `server:`, `source:` prefixes)
* `onCommandActions` — execute commands when specific (or `"*"`) commands run
* `restartTimes` — list of `HH:mm` restarts, announced at 60/30/10… seconds
* `permissionLayout`, `permissionLayoutOther`, `permissionHomeLayout` — templated nodes for LuckPerms integration

Other noteworthy config files:

* `config/kessentials/chat.json` — RTP min/max radius, Discord channel fallback, stores last `/msg` partner.
* `config/kessentials/lang/` — locale overrides plus `locales.json` mapping UUIDs → locale tag.
* `config/kessentials/links.json` — persistent Discord → Minecraft link mappings.
* `config/kessentials/kchat.json` — MOTD, tab, scoreboard, animations and name display format (see below).
* `config/kessentials/motd.txt` — legacy MOTD fallback used by `MotdService` after server start.

## Commands

*Default permissions assume the shipped layout `system.%command%` (self) and `system.%command%.other(s)` for targeting others. Commands without a check are open to all players and can be gated via `disabledCommands` or permission layout tweaks.*

### Player care & inventory

| Command                                          | Aliases                                              | Permission(s)                                                       | Notes                                                                                            |
| ------------------------------------------------ | ---------------------------------------------------- | ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `/heal [player]`                                 | `/heilen`                                            | `system.heal`, `system.heal.other`                                  | Restores health, hunger, saturation, clears effects & fire; notifies source when healing others. |
| `/feed [player]`                                 | `/essen`                                             | `system.feed`, `system.feed.other`                                  | Restores hunger & saturation.                                                                    |
| `/hat`                                           | —                                                    | `system.hat`                                                        | Swaps held item with helmet slot.                                                                |
| `/workbench [player]`                            | `/werkbank`, `/craftingtable`, `/craft`, `/crafting` | `system.workbench`, `system.workbench.other`                        | Opens a crafting GUI for self or target and sends feedback.                                      |
| `/anvil`                                         | `/amboss`                                            | `system.anvil`                                                      | Opens a virtual anvil.                                                                           |
| `/enchantingtable <lvl1> <lvl2> <lvl3> [player]` | `/verzauberungstisch`, `/enchantmenttable`           | `system.enchantingtable`, `system.enchantingtable.other`            | Opens enchanting table with custom slot costs; persists per-player until cleared.                |
| `/enderchest [player]`                           | `/ec`, `/enderkiste`                                 | `system.enderchest`, `system.enderchest.other` or `system.endersee` | Opens own or others' ender chest, notifies both viewer and owner.                                |
| `/endersee <player>`                             | —                                                    | `system.endersee`                                                   | Shortcut to view another player's ender chest.                                                   |
| `/invsee <player> [modify]`                      | `/inventorysee`, `/inv`, `/inventory`, `/inventar`   | `system.invsee`, `system.invsee.modify`                             | Opens remote inventory with optional write access gated by `.modify`.                            |
| `/armorsee <player> [modify]`                    | `/armor`, `/armsee`, `/ruestung`                     | `system.armorsee`, `system.armorsee.modify`                         | Displays armor/offhand slots with optional modification permission.                              |
| `/repair [player]`                               | `/reparieren`                                        | `system.repair`, `system.repair.other`                              | Repairs the main-hand item if damageable; reports errors per locale.                             |
| `/clear [player]`                                | —                                                    | `system.clear`, `system.clear.other`                                | Clears inventory for self or target.                                                             |
| `/ext`                                           | —                                                    | `system.extinguish`                                                 | Extinguishes fire on the caller.                                                                 |
| `/material`                                      | `/mat`                                               | –                                                                   | Shows registry ID of the held item (debug utility).                                              |
| `/link`                                          | —                                                    | –                                                                   | Generates a 6-digit code for Discord linking via `!link` or `/link` on Discord.                  |

### Movement & teleportation

| Command                                  | Aliases                                          | Permission(s)                              | Notes                                                                               |
| ---------------------------------------- | ------------------------------------------------ | ------------------------------------------ | ----------------------------------------------------------------------------------- |
| `/sethome [name]`                        | `/newhome`, `/newhomes`, `/zuhausefestlegen`     | `system.home`                              | Saves a home (default name `home`); home limits read from permissions/meta.         |
| `/home [name]`                           | `/homes`, `/zuhause`                             | `system.home`                              | Teleports to stored home with warmup/cooldown support.                              |
| `/delhome <name>`                        | `/deletehome`, `/removehome`, `/zuhauseloeschen` | `system.home`                              | Deletes a saved home.                                                               |
| `/setwarp <name>`                        | `/newwarp`, `/addwarp`                           | `system.warp`                              | Saves current location as warp.                                                     |
| `/warp <name>`                           | `/warps`                                         | `system.warp`                              | Teleports to warp with warmup/cooldown, biome blacklist and `/warp list`.           |
| `/delwarp <name>`                        | `/deletewarp`, `/removewarp`                     | `system.warp`                              | Removes a warp entry.                                                               |
| `/spawn`                                 | —                                                | –                                          | Teleports to configured spawn with warmup/cooldown; uses `/back`.                   |
| `/setspawn`                              | —                                                | `system.setspawn`                          | Saves the current position as global spawn.                                         |
| `/rtp`                                   | —                                                | `system.rtp`                               | Safe random teleport using RTP radius & biome blacklist; honours warmups/cooldowns. |
| `/back`                                  | —                                                | `system.back`                              | Returns to last stored location (death/teleport).                                   |
| `/tp <player>`                           | —                                                | `system.tp`                                | Teleports self to target.                                                           |
| `/tp <from> <to>`                        | —                                                | `system.tp.other`                          | Moves one player to another.                                                        |
| `/tphere <player>`                       | —                                                | `system.tp.other`                          | Summons a player to you.                                                            |
| `/tppos <x> <y> <z>`                     | —                                                | `system.tp`                                | Teleports to coordinates in current dimension.                                      |
| `/teleportplayer <from> <to>`            | `/tpp`                                           | `system.tp.other`                          | Alias for moving one player to another.                                             |
| `/tpa <player>`                          | `/tpanfrage`                                     | `system.tpa`                               | Sends a teleport request (60s TTL).                                                 |
| `/tpahere <player>`                      | —                                                | `system.tpa`                               | Asks player to teleport to you.                                                     |
| `/tpaccept [player]`, `/tpdeny [player]` | `/tpakzeptieren`, `/tpablehnen`                  | –                                          | Accept/deny pending TPA (no permission check).                                      |
| `/tplist`                                | `/tpanfragen`                                    | –                                          | Lists pending requests.                                                             |
| `/tptoggle`                              | —                                                | –                                          | Blocks/unblocks incoming requests.                                                  |
| `/top`                                   | —                                                | –                                          | Teleports to highest safe block at current X/Z.                                     |
| `/jump`                                  | —                                                | –                                          | Teleports to block you are looking at (max 120 blocks).                             |
| `/speed <0.1–5> [player]`                | `/geschwindigkeit`                               | `system.speed`, `system.speed.other`       | Adjusts walk/fly speed with feedback.                                               |
| `/fly [player]`                          | `/flight`, `/flug`, `/flugmodus`, `/fliegen`     | `system.fly`, `system.fly.other`           | Toggles flight; disables flying when revoked.                                       |
| `/gamemode <mode> [player]`              | `/gm`                                            | `system.gamemode`, `system.gamemode.other` | Supports numeric & shorthand modes; notifies both players.                          |
| `/god`                                   | —                                                | `system.god`                               | Toggles damage immunity, enforced via event listener.                               |
| `/vanish [player]`                       | `/verschwinden`, `/v`                            | `system.vanish`, `system.vanish.other`     | Toggles vanish state, fake join/leave messages and tab/name hiding.                 |
| `/suicide`                               | `/suizid`, `/selbstmord`                         | `system.suicide`                           | Kills the player and shows localized message.                                       |

### Moderation & chat

| Command                                        | Aliases                             | Permission(s)                          | Notes                                                                                |
| ---------------------------------------------- | ----------------------------------- | -------------------------------------- | ------------------------------------------------------------------------------------ |
| `/freeze [player]`                             | `/einfrieren`                       | `system.freeze`, `system.freeze.other` | Toggles freeze; prevents movement/interaction/break/place via events.                |
| `/mute <player>` / `/unmute <player>`          | —                                   | `system.mute`                          | Toggles persistent mute; muted players blocked in chat handler.                      |
| `/socialspy`                                   | —                                   | `system.socialspy`                     | Mirrors `/msg` traffic for staff.                                                    |
| `/broadcast <message>`                         | —                                   | `system.broadcast`                     | Sends raw message to all players (no formatting).                                    |
| `/chatclear`                                   | `/cc`, `/chatleeren`                | `system.chatclear`                     | Clears chat for everyone and posts localized confirmation.                           |
| `/f3d`                                         | `/clearmychat`, `/meinenchatleeren` | –                                      | Clears personal chat buffer (no permission check).                                   |
| `/msg <player> <message>` / `/reply <message>` | —                                   | `system.msg`                           | Private messaging with last-reply memory, social spy integration and offline checks. |
| `/near [radius]`                               | —                                   | –                                      | Lists players within radius (default 100).                                           |
| `/seen <player>`                               | —                                   | –                                      | Shows last seen timestamp tracked via state service.                                 |

### World & environment

| Command                                         | Aliases                                          | Permission(s)         | Notes                                                               |   |                                           |
| ----------------------------------------------- | ------------------------------------------------ | --------------------- | ------------------------------------------------------------------- | - | ----------------------------------------- |
| `/time set <ticks> [dimension]`                 | —                                                | `system.time`         | Sets day time for specified level; dimension suggestions supported. |   |                                           |
| `/time add <ticks> [dimension]`                 | —                                                | `system.time`         | Adds ticks to day time.                                             |   |                                           |
| `/weather <clear                                | rain                                             | thunder> [dimension]` | —                                                                   | – | Adjusts weather instantly (open command). |
| `/morning` `/day` `/midday` `/evening` `/night` | `/morgen`, `/tag`, `/mittag`, `/abend`, `/nacht` | –                     | Quick time presets (no built-in permission check).                  |   |                                           |

### System & integration

| Command               | Aliases                      | Permission(s)   | Notes                                                                                   |
| --------------------- | ---------------------------- | --------------- | --------------------------------------------------------------------------------------- |
| `/kchat [reload]`     | —                            | `system.kchat`  | Without args reapplies MOTD/tab/scoreboard; `/kchat reload` reloads `kchat.json`.       |
| `/kessentials`        | `/essentials`, `/kessential` | –               | Shows plugin version.                                                                   |
| `/kessentials reload` | —                            | `system.reload` | Reloads config, chat, locales and reapplies MOTD/tab/scoreboard with failure reporting. |

## Permissions (LuckPerms)

With the default layout (`system.%command%`) the important nodes are:

* **Teleport & travel** — `system.home`, `system.home.other(s)` (not used by default), `system.home.limit.<n>`, `system.warp`, `system.spawn`, `system.setspawn`, `system.rtp`, `system.back`, `system.tp`, `system.tp.other`, `system.tpa`.
* **Utilities** — `system.heal`, `system.feed`, `system.fly`, `system.speed`, `system.gamemode`, `system.god`, `system.vanish`, `system.suicide`, `system.hat`, `system.extinguish`, `system.workbench`, `system.anvil`, `system.enchantingtable`, `system.enderchest`, `system.enderchest.other`, `system.endersee`, `system.invsee`, `system.invsee.modify`, `system.armorsee`, `system.armorsee.modify`, `system.material` (open by default).
* **Moderation** — `system.freeze`, `system.freeze.other`, `system.mute`, `system.socialspy`, `system.broadcast`, `system.chatclear`, `system.clear`, `system.clear.other`, `system.repair`, `system.repair.other`, `system.msg`, `system.near` (open), etc.
* **Environment** — `system.time`, (weather/time preset commands are open unless gated via config), `system.kchat`, `system.reload`.

`permissionLayoutOther` inserts either `.other` or `.others`, so LuckPerms assignments such as `system.vanish.other` and `system.fly.others` will both satisfy the check. Home limits use `permissionHomeLayout` (`system.home.limit.%limit%` by default). Legacy LuckPerms meta `kessentials.maxhomes` is still honoured as fallback.

Adjust `permissionLayout*` in `config.json` to retain old `kessentials.*` nodes or custom schemes. Commands without explicit checks can be wrapped by LuckPerms command rules or `disabledCommands`.

### Migration (≤ 0.3.1 → neue Layouts)

1. `config/kessentials/config.json` um die neuen Felder `permissionLayout`, `permissionLayoutOther` und `permissionHomeLayout` ergänzen (oder Datei löschen, damit sie mit Defaults neu erzeugt wird).
2. LuckPerms-Berechtigungen an das neue Layout anpassen – entweder die Defaults `system.*` verwenden oder das Layout auf `kessentials.%command%` zurücksetzen.
3. Home-Limits können jetzt direkt über Permissions vergeben werden (Default `system.home.limit.<n>`). Vorhandene `kessentials.maxhomes`-Meta-Werte bleiben gültig.

## Discord (JDA)

* Bridge MC ↔ Discord; presence shows playercount and honours manual status overrides. Slash/text commands require channel configuration in `discord.properties`.
* Text commands: `!say <msg>`, `!online`, `!kick <player> [reason]`, `!link <code>`; optional `!stop` through slash command if enabled.
* Slash commands: `/online`, `/stop` (if `enableStopCommand`), `/link <code>` (if `enableLinkCommand`).
* Linking: `/link` in-game → `/link <code>` (Discord) adds player to whitelist and persists mapping.

## Locales

* Built-in language files live under `src/main/resources/lang/` (e.g. `messages_de.properties`, `messages_en.properties`).
* Server-specific overrides can be placed in `config/kessentials/lang/` (same filename pattern). These files override bundled keys.
* Default and per-player locales are configured via `config/kessentials/lang/locales.json`:

```json
{
  "defaultLocale": "de",
  "playerLocales": {
    "11111111-2222-3333-4444-555555555555": "en",
    "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee": "de-DE"
  }
}
```

Set `defaultLocale` to the desired fallback language tag and optionally map individual player UUIDs to their preferred locale. Players without an explicit entry use the default. After editing locale files, run `/kessentials reload` to apply changes without restarting the server.

## Placeholders

Supported in Tablist, Scoreboard, Chat und MOTD (soweit sinnvoll):

* `%player%`, `%online%`, `%server_max%`, `%ping%`
* `%world%`, `%world_name_<dimensionId>%`
* `%luckperms-prefix%`, `%luckperms-suffix%`, `%rank%`
* `%lp-meta:<key>%` (beliebige LuckPerms-Meta-Keys)
* `%statistic_<namespace:path>%` (z. B. `minecraft:deaths`, `minecraft:mob_kills`)
* `%animation:Name%` (Frames/Animationen aus `kchat.json`)

### Advanced animations

In `config/kessentials/kchat.json` you can define animations with a custom **change interval**:

```json
"animationsAdv": {
  "MyAnimation1": {
    "changeInterval": 100,
    "texts": [
      "&d-&3--------------",
      "&3-&d-&3-------------"
    ]
  }
}
```

You can reference it via `%animation:MyAnimation1%` in tab/scoreboard/chat.

## MOTD, Tablist & Scoreboard (`config/kessentials/kchat.json`)

```jsonc
{
  "motdEnabled": true,
  "motdMiniMessage": "<gradient:gold:yellow><bold>KettleMC</bold></gradient> <gray>StoneBlock 3</gray>\n<dark_gray>➜</dark_gray> <white>Join now!</white>",
  "tabHeader": [
    "&eWillkommen, %player%!",
    "&7Online: &a%online%&7/&a%server_max%"
  ],
  "tabFooter": [
    "&7Ping: &a%ping% ms",
    "%animation:InfoBar%"
  ],
  "scoreboard": {
    "enabled": true,
    "title": "&6&lKettleMC",
    "lines": [
      "&7Rank: &f%luckperms-prefix%",
      "&7World: &f%world_name_overworld%",
      "&7Kills: &f%statistic_minecraft:mob_kills%"
    ]
  },
  "animations": {
    "InfoBar": ["&eHave fun!", "&aVote heute!"]
  }
}
```

* MOTD nutzt vollständige **MiniMessage**-Syntax (Kyori Adventure) und aktualisiert sich beim Serverstart, periodisch sowie via `/kchat reload`.
* Tablist und Scoreboard werden bei Spielerbeitritt und anschließend sekündlich synchronisiert; Animationen wechseln nach ihrer Intervall-Konfiguration.
* `/kchat reload` lädt `kchat.json` neu, aktualisiert Tablist/Scoreboard aller Spieler und setzt die MOTD neu.
