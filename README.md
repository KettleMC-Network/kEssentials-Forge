# kEssentials (Forge 1.18.2)

## 🇩🇪 Vollständige Beschreibung (Deutsch)

Gradle‑Projekt (Java 17) für Forge 1.18.2 mit Essentials‑ähnlichen Funktionen: starkes Vanish, Invsee Pro, sicheres RTP‑Scan, mehrsprachiger Chat und Discord‑Bridge (JDA) inkl. Whitelist‑Verknüpfung.

### Features

#### Chat, Tablist & Präsentation

* **Animierter Tab‑Header/-Footer & Scoreboard** mit konfigurierbaren Titeln/Zeilen und LuckPerms‑Platzhaltern; Aktualisierung jede Sekunde, reagiert auf Vanish/Name‑Änderungen.
* **Adventure MiniMessage‑MOTD** wird beim Serverstart gesetzt, periodisch aktualisiert und per `/kchat reload` neu angewendet.
* **Chat‑Formatierungspipeline** behält Mute‑Support, injiziert Platzhalter (LuckPerms‑Prefix/Suffix, Statistiken, Welt‑Info, Animationen) und kann Nachrichten zu Discord spiegeln.
* **Namensanzeige & Tab‑Sortierung** sortiert Spieler nach LuckPerms‑Gruppengewicht/Meta und zeigt Präfixe über Köpfen (optional verstecken bei Vanish).

#### Teleportation & Spieler‑Utilities

* Essentials‑artige **Homes, Warps, Spawn, Back** mit Warmups/Cooldowns, biome‑bewusstem RTP‑Scan, Positions‑Historie und Home‑Limits pro Spieler (aus Permissions/Meta).
* **TPA‑Warteschlange**: `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tplist`, `/tptoggle`, Warmup wird bei Bewegung abgebrochen; Ablauf nach 60 s.
* **Schnell‑Kommandos**: `/top`, `/jump`, `/hat`, `/material`, virtuelle Werkbank/Amboss/Verzauberung, Ender‑Kiste/‑Einsicht, Heal/Feed/Repair, Speed/Fly/Gamemode/God‑Toggles, Löschen von Feuer u. v. m.

#### Moderation & Team‑Tools

* **Vanish** schaltet Unsichtbarkeit, Stille, No‑Clip, Fake Join/Leave und versteckt Vanished‑Spieler vor Neulingen; Tab/Name werden live aktualisiert.
* **Freeze/Mute, Social‑Spy & God‑Mode** über persistenten Zustands‑Service; Gefrorene können sich nicht bewegen/interagieren, God verhindert Schaden, Social‑Spy spiegelt private Nachrichten.
* **Inventar‑Einsicht**: `/invsee`, `/armorsee`, `/endersee`, `/enderchest` (eigene oder fremde) mit optionaler Schreibberechtigung inkl. Rückmeldungen an Nutzer.
* **Chat‑Administration**: `/chatclear`, `/f3d`, `/broadcast`, `/msg`, `/reply`, `/socialspy`, `/near`, `/seen`, `/clear`, `/repair`, `/ext` und `/link` (Discord‑Whitelist).

#### Discord‑Bridge & Account‑Verknüpfung

* **Zwei‑Wege‑Chat** (optional formatiert), Join/Quit‑Ankündigungen, ClearLagg‑Relays, Presence‑Updates; konfigurierbar in `discord.properties`.
* **Bot‑Befehle**: Text `!online`, `!say`, `!kick`, `!link <code>`; Slash `/online`, optional `/stop`, `/link` (mit Whitelist‑Integration) sowie `/link` im Spiel.

#### Automatisierung & Konfiguration

* **Warmups, Cooldowns & Abbruch‑bei‑Bewegung** zentral für Teleporte; Zeiten pro Befehl in `config.json`.
* **Geplante Neustart‑Ankündigungen**, pro‑Befehl Deaktivierungen/Überschreibungstexte, benutzerdefinierte Join‑Aktionen sowie Befehls‑getriggerte Aktionen (Konsole/Spieler/Quelle).
* **Benutzerdefinierte Join/Leave‑Nachrichten** mit Vanish‑Bewusstsein und Fake‑Meldungen beim Umschalten.

#### Internationalisierung

* Locale‑Service mit Default + pro‑Spieler‑Overrides, mitgelieferte `messages_en.properties`/`messages_de.properties`, Hot‑Reload via `/kessentials reload` oder `/kchat reload`. Server‑Overrides unter `config/kessentials/lang/`.

---

## Build

```bash
./gradlew build
```

Jar: `build/libs/kessentials-forge-0.3.1.jar`

## Einrichtung

* Forge 1.18.2‑40.x
* LuckPerms‑Forge installieren (Permissions & Meta).
* `config/kessentials/discord.properties` konfigurieren (`token=`, `channelId=`).

## Konfigurations‑Highlights

`config/kessentials/config.json` wird mit sinnvollen Defaults erstellt und unterstützt:

* `defaultMaxHomes` (int)
* `cooldownsSeconds` / `warmupsSeconds` — Map *Befehl → Sekunden*, z. B. `{ "home": 3, "warp": 3, "spawn": 3, "rtp": 30 }`
* `rtpBiomeBlacklist` — verbotene Biome für Random‑Teleport
* `vanishFakeMessages` — Fake Join/Leave bei Vanish umschalten
* `customJoinLeaveMessages` — deaktivieren, wenn Vanilla‑Meldungen bevorzugt
* `disabledCommands` — Map *Root‑Literal → optionaler Ersatztext* (bricht Ausführung ab)
* `onJoinActions` — Server‑Kommandos (Präfixe `player:`, `server:`, `source:`)
* `onCommandActions` — Aktionen ausführen, wenn bestimmte (oder `"*"`) Befehle genutzt werden
* `restartTimes` — Liste `HH:mm`‑Neustarts, angekündigt bei 60/30/10… s
* `permissionLayout`, `permissionLayoutOther`, `permissionHomeLayout` — Templates für LuckPerms‑Nodes

Weitere wichtige Dateien:

* `config/kessentials/chat.json` — RTP‑Radius‑Min/Max, Discord‑Fallback‑Channel, speichert letzten `/msg`‑Partner.
* `config/kessentials/lang/` — Locale‑Overrides + `locales.json` (UUID → Locale‑Tag).
* `config/kessentials/links.json` — persistente Discord↔Minecraft‑Verknüpfungen.
* `config/kessentials/kchat.json` — MOTD, Tab, Scoreboard, Animationen, Namensanzeige‑Format.
* `config/kessentials/motd.txt` — Legacy‑MOTD‑Fallback des `MotdService` nach Serverstart.

## Befehle

*Standard‑Permissions folgen dem Layout `system.%command%` (Selbst) und `system.%command%.other(s)` (andere). Befehle ohne Check sind für alle offen und können über `disabledCommands` oder LuckPerms‑Regeln begrenzt werden.*

### Spieler‑Pflege & Inventar

| Befehl                                           | Aliasse                                              | Berechtigung(en)                                                      | Hinweise                                                                                   |
| ------------------------------------------------ | ---------------------------------------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| `/heal [player]`                                 | `/heilen`                                            | `system.heal`, `system.heal.other`                                    | Stellt Leben, Hunger, Sättigung her; entfernt Effekte & Feuer; meldet Heilung bei anderen. |
| `/feed [player]`                                 | `/essen`                                             | `system.feed`, `system.feed.other`                                    | Füllt Hunger & Sättigung.                                                                  |
| `/hat`                                           | —                                                    | `system.hat`                                                          | Tauscht gehaltenes Item mit Helm‑Slot.                                                     |
| `/workbench [player]`                            | `/werkbank`, `/craftingtable`, `/craft`, `/crafting` | `system.workbench`, `system.workbench.other`                          | Öffnet virtuelle Werkbank (selbst/anderer) mit Feedback.                                   |
| `/anvil`                                         | `/amboss`                                            | `system.anvil`                                                        | Öffnet virtuellen Amboss.                                                                  |
| `/enchantingtable <lvl1> <lvl2> <lvl3> [player]` | `/verzauberungstisch`, `/enchantmenttable`           | `system.enchantingtable`, `system.enchantingtable.other`              | Öffnet Verzauberungstisch mit benutzerdef. Slot‑Kosten; bleibt pro Spieler erhalten.       |
| `/enderchest [player]`                           | `/ec`, `/enderkiste`                                 | `system.enderchest`, `system.enderchest.other` oder `system.endersee` | Öffnet eigene/fremde Ender‑Kiste; benachrichtigt Betroffene.                               |
| `/endersee <player>`                             | —                                                    | `system.endersee`                                                     | Abkürzung zum Einsehen fremder Ender‑Kisten.                                               |
| `/invsee <player> [modify]`                      | `/inventorysee`, `/inv`, `/inventory`, `/inventar`   | `system.invsee`, `system.invsee.modify`                               | Fremdinventar öffnen; optional mit Schreibrecht (`.modify`).                               |
| `/armorsee <player> [modify]`                    | `/armor`, `/armsee`, `/ruestung`                     | `system.armorsee`, `system.armorsee.modify`                           | Rüstungs-/Offhand‑Slots anzeigen; optional ändern.                                         |
| `/repair [player]`                               | `/reparieren`                                        | `system.repair`, `system.repair.other`                                | Repariert Main‑Hand‑Item (falls beschädigbar); lokalisierte Fehlermeldungen.               |
| `/clear [player]`                                | —                                                    | `system.clear`, `system.clear.other`                                  | Leert Inventar (selbst/anderer).                                                           |
| `/ext`                                           | —                                                    | `system.extinguish`                                                   | Löscht Feuer auf dem Spieler.                                                              |
| `/material`                                      | `/mat`                                               | –                                                                     | Zeigt Registry‑ID des gehaltenen Items (Debug).                                            |
| `/link`                                          | —                                                    | –                                                                     | Generiert 6‑stelligen Code für Discord‑Verknüpfung (`!link`/`/link`).                      |

### Bewegung & Teleport

| Befehl                                   | Aliasse                                          | Berechtigung(en)                           | Hinweise                                                                                  |
| ---------------------------------------- | ------------------------------------------------ | ------------------------------------------ | ----------------------------------------------------------------------------------------- |
| `/sethome [name]`                        | `/newhome`, `/newhomes`, `/zuhausefestlegen`     | `system.home`                              | Speichert Home (Standardname `home`); Limits aus Permission/Meta.                         |
| `/home [name]`                           | `/homes`, `/zuhause`                             | `system.home`                              | Teleport zu Home mit Warmup/Cooldown.                                                     |
| `/delhome <name>`                        | `/deletehome`, `/removehome`, `/zuhauseloeschen` | `system.home`                              | Löscht Home.                                                                              |
| `/setwarp <name>`                        | `/newwarp`, `/addwarp`                           | `system.warp`                              | Speichert Warp.                                                                           |
| `/warp <name>`                           | `/warps`                                         | `system.warp`                              | Teleport zum Warp; Warmup/Cooldown; Biome‑Blacklist; `/warp list`.                        |
| `/delwarp <name>`                        | `/deletewarp`, `/removewarp`                     | `system.warp`                              | Entfernt Warp.                                                                            |
| `/spawn`                                 | —                                                | –                                          | Teleport zum konfigurierten Spawn (mit Warmup/Cooldown); `/back` kompatibel.              |
| `/setspawn`                              | —                                                | `system.setspawn`                          | Setzt globalen Spawn.                                                                     |
| `/rtp`                                   | —                                                | `system.rtp`                               | Sicherer Random‑Teleport innerhalb Radius & Biome‑Blacklist; respektiert Warmup/Cooldown. |
| `/back`                                  | —                                                | `system.back`                              | Zurück zur letzten Position (Tod/Teleport).                                               |
| `/tp <player>`                           | —                                                | `system.tp`                                | Teleportiert dich zu Spieler.                                                             |
| `/tp <from> <to>`                        | —                                                | `system.tp.other`                          | Versetzt einen Spieler zu einem anderen.                                                  |
| `/tphere <player>`                       | —                                                | `system.tp.other`                          | Holt Spieler zu dir.                                                                      |
| `/tppos <x> <y> <z>`                     | —                                                | `system.tp`                                | Teleport zu Koordinaten in aktueller Dimension.                                           |
| `/teleportplayer <from> <to>`            | `/tpp`                                           | `system.tp.other`                          | Alias für Spieler→Spieler.                                                                |
| `/tpa <player>`                          | `/tpanfrage`                                     | `system.tpa`                               | Sendet Teleport‑Anfrage (60 s Gültigkeit).                                                |
| `/tpahere <player>`                      | —                                                | `system.tpa`                               | Bittet Ziel, zu dir zu teleportieren.                                                     |
| `/tpaccept [player]`, `/tpdeny [player]` | `/tpakzeptieren`, `/tpablehnen`                  | –                                          | Nimmt TPA an/lehnen ab (kein Permission‑Check).                                           |
| `/tplist`                                | `/tpanfragen`                                    | –                                          | Listet offene Anfragen.                                                                   |
| `/tptoggle`                              | —                                                | –                                          | Blockiert/erlaubt eingehende Anfragen.                                                    |
| `/top`                                   | —                                                | –                                          | Teleportiert auf den höchsten sicheren Block bei aktuellem X/Z.                           |
| `/jump`                                  | —                                                | –                                          | Teleportiert zum anvisierten Block (max. 120 Blöcke).                                     |
| `/speed <0.1–5> [player]`                | `/geschwindigkeit`                               | `system.speed`, `system.speed.other`       | Passt Lauf/Flug‑Geschwindigkeit an.                                                       |
| `/fly [player]`                          | `/flight`, `/flug`, `/flugmodus`, `/fliegen`     | `system.fly`, `system.fly.other`           | Schaltet Fliegen um; deaktiviert beim Entzug.                                             |
| `/gamemode <mode> [player]`              | `/gm`                                            | `system.gamemode`, `system.gamemode.other` | Unterstützt numerische & Kurzformen; benachrichtigt Beteiligte.                           |
| `/god`                                   | —                                                | `system.god`                               | Schaltet Unverwundbarkeit um (Event‑basiert).                                             |
| `/vanish [player]`                       | `/verschwinden`, `/v`                            | `system.vanish`, `system.vanish.other`     | Vanish‑Status, Fake‑Meldungen, Tab/Name‑Verstecken.                                       |
| `/suicide`                               | `/suizid`, `/selbstmord`                         | `system.suicide`                           | Tötet den Spieler; lokalisiert.                                                           |

### Moderation & Chat

| Befehl                                         | Aliasse                             | Berechtigung(en)                       | Hinweise                                                            |
| ---------------------------------------------- | ----------------------------------- | -------------------------------------- | ------------------------------------------------------------------- |
| `/freeze [player]`                             | `/einfrieren`                       | `system.freeze`, `system.freeze.other` | Toggle Freeze; verhindert Bewegung/Interaktion/Abbau/Platzieren.    |
| `/mute <player>` / `/unmute <player>`          | —                                   | `system.mute`                          | Persistentes Mute; Chat‑Handler blockt.                             |
| `/socialspy`                                   | —                                   | `system.socialspy`                     | Spiegelt `/msg`‑Verkehr für Staff.                                  |
| `/broadcast <message>`                         | —                                   | `system.broadcast`                     | Sendet Rohtext an alle (ohne Formatierung).                         |
| `/chatclear`                                   | `/cc`, `/chatleeren`                | `system.chatclear`                     | Leert Chat global und bestätigt lokalisiert.                        |
| `/f3d`                                         | `/clearmychat`, `/meinenchatleeren` | –                                      | Leert eigenen Chat‑Puffer.                                          |
| `/msg <player> <message>` / `/reply <message>` | —                                   | `system.msg`                           | Private Nachrichten mit Reply‑Speicher, Social‑Spy, Offline‑Checks. |
| `/near [radius]`                               | —                                   | –                                      | Listet Spieler im Umkreis (Standard 100).                           |
| `/seen <player>`                               | —                                   | –                                      | Zeigt „zuletzt gesehen“ (über State‑Service).                       |

### Welt & Umgebung

| Befehl                                          | Aliasse                                          | Berechtigung(en)      | Hinweise                                                       |   |                                        |
| ----------------------------------------------- | ------------------------------------------------ | --------------------- | -------------------------------------------------------------- | - | -------------------------------------- |
| `/time set <ticks> [dimension]`                 | —                                                | `system.time`         | Setzt Tageszeit; Dimension‑Vorschläge vorhanden.               |   |                                        |
| `/time add <ticks> [dimension]`                 | —                                                | `system.time`         | Fügt Ticks zur Tageszeit hinzu.                                |   |                                        |
| `/weather <clear                                | rain                                             | thunder> [dimension]` | —                                                              | – | Ändert Wetter sofort (offener Befehl). |
| `/morning` `/day` `/midday` `/evening` `/night` | `/morgen`, `/tag`, `/mittag`, `/abend`, `/nacht` | –                     | Zeit‑Presets ohne Permission‑Check (per Config einschränkbar). |   |                                        |

### System & Integration

| Befehl                | Aliasse                      | Berechtigung(en) | Hinweise                                                                     |
| --------------------- | ---------------------------- | ---------------- | ---------------------------------------------------------------------------- |
| `/kchat [reload]`     | —                            | `system.kchat`   | Ohne Argumente MOTD/Tab/Scoreboard neu anwenden; `reload` lädt `kchat.json`. |
| `/kessentials`        | `/essentials`, `/kessential` | –                | Zeigt Plugin‑Version.                                                        |
| `/kessentials reload` | —                            | `system.reload`  | Lädt Config/Chat/Locales neu und setzt MOTD/Tab/Scoreboard.                  |

## Berechtigungen (LuckPerms)

Mit dem Default‑Layout (`system.%command%`) sind u. a. wichtig:

* **Teleport & Reisen** — `system.home`, `system.home.other(s)` (standardmäßig ungenutzt), `system.home.limit.<n>`, `system.warp`, `system.spawn`, `system.setspawn`, `system.rtp`, `system.back`, `system.tp`, `system.tp.other`, `system.tpa`.
* **Utilities** — `system.heal`, `system.feed`, `system.fly`, `system.speed`, `system.gamemode`, `system.god`, `system.vanish`, `system.suicide`, `system.hat`, `system.extinguish`, `system.workbench`, `system.anvil`, `system.enchantingtable`, `system.enderchest`, `system.enderchest.other`, `system.endersee`, `system.invsee`, `system.invsee.modify`, `system.armorsee`, `system.armorsee.modify`, `system.material` (offen).
* **Moderation** — `system.freeze`, `system.freeze.other`, `system.mute`, `system.socialspy`, `system.broadcast`, `system.chatclear`, `system.clear`, `system.clear.other`, `system.repair`, `system.repair.other`, `system.msg`, `system.near` (offen) usw.
* **Umgebung** — `system.time`, (Zeit/Wetter‑Presets offen, falls nicht per Config begrenzt), `system.kchat`, `system.reload`.

`permissionLayoutOther` setzt `.other` **oder** `.others`, sodass z. B. `system.vanish.other` und `system.fly.others` beide gelten. Home‑Limits nutzen `permissionHomeLayout` (`system.home.limit.%limit%`). Ältere LuckPerms‑Meta `kessentials.maxhomes` wird weiterhin als Fallback beachtet.

### Migration (≤ 0.3.1 → neue Layouts)

1. `config/kessentials/config.json` um `permissionLayout`, `permissionLayoutOther`, `permissionHomeLayout` ergänzen (oder Datei löschen, um Defaults neu zu erzeugen).
2. LuckPerms‑Rechte anpassen – entweder Defaults `system.*` verwenden oder das Layout auf `kessentials.%command%` zurückstellen.
3. Home‑Limits können direkt via Permissions vergeben werden (`system.home.limit.<n>`). Vorhandene `kessentials.maxhomes`‑Meta bleiben gültig.

## Discord (JDA)

* Bridge MC↔Discord; Presence zeigt Spielerzahl und respektiert manuelle Status‑Overrides. Slash/Text‑Befehle benötigen Kanal‑Konfiguration in `discord.properties`.
* Text: `!say <msg>`, `!online`, `!kick <player> [reason]`, `!link <code>`; optional `!stop` via Slash, wenn aktiviert.
* Slash: `/online`, `/stop` (falls `enableStopCommand`), `/link <code>` (falls `enableLinkCommand`).
* Verknüpfen: `/link` im Spiel → `/link <code>` (Discord) fügt Whitelist hinzu und speichert Zuordnung.

## Locales

* Mitgelieferte Sprachdateien unter `src/main/resources/lang/` (z. B. `messages_de.properties`, `messages_en.properties`).
* Server‑Overrides in `config/kessentials/lang/` (gleicher Dateiname). Overrides stechen gebündelte Schlüssel.
* Standard‑ und Spieler‑Locales in `config/kessentials/lang/locales.json`:

```json
{
  "defaultLocale": "de",
  "playerLocales": {
    "11111111-2222-3333-4444-555555555555": "en",
    "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee": "de-DE"
  }
}
```

Nach dem Bearbeiten `/kessentials reload` ausführen, um ohne Neustart zu übernehmen.

## Platzhalter

Verfügbar in Tablist, Scoreboard, Chat und MOTD (wo sinnvoll):

* `%player%`, `%online%`, `%server_max%`, `%ping%`
* `%world%`, `%world_name_<dimensionId>%`
* `%luckperms-prefix%`, `%luckperms-suffix%`, `%rank%`
* `%lp-meta:<key>%` (beliebige LuckPerms‑Meta‑Keys)
* `%statistic_<namespace:path>%` (z. B. `minecraft:deaths`, `minecraft:mob_kills`)
* `%animation:Name%` (Frames/Animationen aus `kchat.json`)

### Erweiterte Animationen

In `config/kessentials/kchat.json` können Animationen mit **eigenem Intervall** definiert werden:

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

Verwendbar z. B. als `%animation:MyAnimation1%` in Tab/Scoreboard/Chat.

## MOTD, Tablist & Scoreboard (`config/kessentials/kchat.json`)

```jsonc
{
  "motdEnabled": true,
  "motdMiniMessage": "<gradient:gold:yellow><bold>KettleMC</bold></gradient> <gray>StoneBlock 3</gray>
<dark_gray>➜</dark_gray> <white>Join now!</white>",
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

* MOTD nutzt **MiniMessage** (Kyori Adventure) und aktualisiert beim Start, periodisch und via `/kchat reload`.
* Tablist/Scoreboard werden beim Beitritt und dann sekündlich synchronisiert; Animationen wechseln gemäß Intervall.
* `/kchat reload` lädt `kchat.json`, aktualisiert Tab/Scoreboard für alle und setzt die MOTD neu.

---

## 🇬🇧 Complete Mirror (English)

Gradle project (Java 17) for Forge 1.18.2 with Essentials‑like features: robust vanish, Invsee Pro, safe RTP scan, multi‑language chat, and a Discord bridge (JDA) incl. whitelist linking.

### Features

#### Chat, Tablist & Presentation

* **Animated tab header/footer & scoreboard** with configurable titles/lines and LuckPerms‑aware placeholders; updates every second and reacts to vanish/name changes.
* **Adventure MiniMessage MOTD** applied at server start, refreshed on a schedule, and reloaded via `/kchat reload`.
* **Chat formatting pipeline** keeps mute support, injects placeholders (LuckPerms prefix/suffix, statistics, world info, animations) and can mirror messages to Discord.
* **Name display & tab sorting** sorts players by LuckPerms group weight/meta and exposes prefixes above heads (optionally hidden when vanished).

#### Teleportation & Player Utilities

* Essentials‑style **homes, warps, spawn, back** with configurable warmups/cooldowns, biome‑aware RTP scanning, previous‑location tracking and per‑player home limits (from permissions/meta).
* **TPA queue**: `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tplist`, `/tptoggle`; movement cancels warmups; requests expire after 60 s.
* **Quick utilities**: `/top`, `/jump`, `/hat`, `/material`, virtual workbench/anvil/enchanting, ender chest/see, heal/feed/repair, speed/fly/gamemode/god toggles, extinguisher, etc.

#### Moderation & Staff Tooling

* **Vanish** toggles invisibility, silence, no‑clip, fake join/leave broadcasts and hides vanished players from newcomers; updates tab/name displays.
* **Freeze/mute, social spy & god mode** via persistent state service; frozen players cannot move/interact/place/break; god cancels damage; spies mirror private messages.
* **Inventory inspection**: `/invsee`, `/armorsee`, `/endersee`, `/enderchest` (self or others) with optional modify permission and user feedback.
* **Chat administration**: `/chatclear`, `/f3d`, `/broadcast`, `/msg`, `/reply`, `/socialspy`, `/near`, `/seen`, `/clear`, `/repair`, `/ext` and `/link` for Discord whitelisting.

#### Discord Bridge & Account Linking

* **Two‑way chat relay** (optionally formatted), join/quit announcements, ClearLagg relays, presence updates; configured via `discord.properties`.
* **Bot commands**: Text `!online`, `!say`, `!kick`, `!link <code>`; slash `/online`, optional `/stop`, `/link` with whitelist integration and `/link` in‑game.

#### Automation & Configuration

* **Warmups, cooldowns & cancel‑on‑move** shared across teleports; per‑command timing in `config.json`.
* **Scheduled restart announcer**, per‑command disable/override messages, custom join actions and command‑triggered console/player actions.
* **Custom join/leave messages** with vanish awareness and the ability to fake messages when toggling vanish.

#### Internationalisation

* Locale service with default + per‑player overrides, bundled `messages_en.properties`/`messages_de.properties`, hot‑reload via `/kessentials reload` or `/kchat reload`. Server overrides live in `config/kessentials/lang/`.

---

## Build

```bash
./gradlew build
```

Jar: `build/libs/kessentials-forge-0.3.1.jar`

## Setup

* Forge 1.18.2‑40.x
* Install LuckPerms‑Forge (permissions & meta).
* Configure `config/kessentials/discord.properties` (`token=`, `channelId=`).

## Configuration Highlights

`config/kessentials/config.json` is created with sensible defaults and supports:

* `defaultMaxHomes` (int)
* `cooldownsSeconds` / `warmupsSeconds` — map *command → seconds*, e.g. `{ "home": 3, "warp": 3, "spawn": 3, "rtp": 30 }`
* `rtpBiomeBlacklist` — disallowed biomes for random teleport
* `vanishFakeMessages` — toggles fake join/leave broadcasts when vanishing
* `customJoinLeaveMessages` — disable if you prefer vanilla messages
* `disabledCommands` — map *root literal → optional override message* (cancels execution)
* `onJoinActions` — server commands (supports `player:`, `server:`, `source:` prefixes)
* `onCommandActions` — execute commands when specific (or `"*"`) commands run
* `restartTimes` — list of `HH:mm` restarts, announced at 60/30/10… seconds
* `permissionLayout`, `permissionLayoutOther`, `permissionHomeLayout` — templated nodes for LuckPerms integration

Other noteworthy files:

* `config/kessentials/chat.json` — RTP min/max radius, Discord channel fallback, stores last `/msg` partner.
* `config/kessentials/lang/` — locale overrides plus `locales.json` mapping UUIDs → locale tag.
* `config/kessentials/links.json` — persistent Discord ↔ Minecraft link mappings.
* `config/kessentials/kchat.json` — MOTD, tab, scoreboard, animations and name display format.
* `config/kessentials/motd.txt` — legacy MOTD fallback used by `MotdService` after server start.

## Commands

*Default permissions follow `system.%command%` (self) and `system.%command%.other(s)` for targeting others. Commands without a check are open to all players and can be gated via `disabledCommands` or permission layout tweaks.*

### Player Care & Inventory

| Command                                          | Aliases                                              | Permission(s)                                                       | Notes                                                                                   |
| ------------------------------------------------ | ---------------------------------------------------- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `/heal [player]`                                 | `/heilen`                                            | `system.heal`, `system.heal.other`                                  | Restores health/hunger/saturation; clears effects & fire; notifies when healing others. |
| `/feed [player]`                                 | `/essen`                                             | `system.feed`, `system.feed.other`                                  | Restores hunger & saturation.                                                           |
| `/hat`                                           | —                                                    | `system.hat`                                                        | Swaps held item with helmet slot.                                                       |
| `/workbench [player]`                            | `/werkbank`, `/craftingtable`, `/craft`, `/crafting` | `system.workbench`, `system.workbench.other`                        | Opens a crafting GUI for self or target with feedback.                                  |
| `/anvil`                                         | `/amboss`                                            | `system.anvil`                                                      | Opens a virtual anvil.                                                                  |
| `/enchantingtable <lvl1> <lvl2> <lvl3> [player]` | `/verzauberungstisch`, `/enchantmenttable`           | `system.enchantingtable`, `system.enchantingtable.other`            | Opens enchanting table with custom slot costs; persists per‑player.                     |
| `/enderchest [player]`                           | `/ec`, `/enderkiste`                                 | `system.enderchest`, `system.enderchest.other` or `system.endersee` | Opens own or others' ender chest; notifies both.                                        |
| `/endersee <player>`                             | —                                                    | `system.endersee`                                                   | Shortcut to view another player's ender chest.                                          |
| `/invsee <player> [modify]`                      | `/inventorysee`, `/inv`, `/inventory`, `/inventar`   | `system.invsee`, `system.invsee.modify`                             | Opens remote inventory; optional write access via `.modify`.                            |
| `/armorsee <player> [modify]`                    | `/armor`, `/armsee`, `/ruestung`                     | `system.armorsee`, `system.armorsee.modify`                         | Shows armor/offhand slots; optional modification.                                       |
| `/repair [player]`                               | `/reparieren`                                        | `system.repair`, `system.repair.other`                              | Repairs main‑hand item if damageable; localized errors.                                 |
| `/clear [player]`                                | —                                                    | `system.clear`, `system.clear.other`                                | Clears inventory for self or target.                                                    |
| `/ext`                                           | —                                                    | `system.extinguish`                                                 | Extinguishes fire on the caller.                                                        |
| `/material`                                      | `/mat`                                               | –                                                                   | Shows registry ID of held item (debug).                                                 |
| `/link`                                          | —                                                    | –                                                                   | Generates a 6‑digit code for Discord linking via `!link` or `/link`.                    |

### Movement & Teleportation

| Command                                  | Aliases                                          | Permission(s)                              | Notes                                                                                |
| ---------------------------------------- | ------------------------------------------------ | ------------------------------------------ | ------------------------------------------------------------------------------------ |
| `/sethome [name]`                        | `/newhome`, `/newhomes`, `/zuhausefestlegen`     | `system.home`                              | Saves a home (default `home`); limits from permissions/meta.                         |
| `/home [name]`                           | `/homes`, `/zuhause`                             | `system.home`                              | Teleports to home with warmup/cooldown.                                              |
| `/delhome <name>`                        | `/deletehome`, `/removehome`, `/zuhauseloeschen` | `system.home`                              | Deletes a saved home.                                                                |
| `/setwarp <name>`                        | `/newwarp`, `/addwarp`                           | `system.warp`                              | Saves current location as warp.                                                      |
| `/warp <name>`                           | `/warps`                                         | `system.warp`                              | Teleports to warp with warmup/cooldown, biome blacklist and `/warp list`.            |
| `/delwarp <name>`                        | `/deletewarp`, `/removewarp`                     | `system.warp`                              | Removes a warp entry.                                                                |
| `/spawn`                                 | —                                                | –                                          | Teleports to configured spawn; works with `/back`.                                   |
| `/setspawn`                              | —                                                | `system.setspawn`                          | Saves the current position as global spawn.                                          |
| `/rtp`                                   | —                                                | `system.rtp`                               | Safe random teleport using RTP radius & biome blacklist; respects warmups/cooldowns. |
| `/back`                                  | —                                                | `system.back`                              | Returns to last stored location (death/teleport).                                    |
| `/tp <player>`                           | —                                                | `system.tp`                                | Teleports self to target.                                                            |
| `/tp <from> <to>`                        | —                                                | `system.tp.other`                          | Moves one player to another.                                                         |
| `/tphere <player>`                       | —                                                | `system.tp.other`                          | Summons a player to you.                                                             |
| `/tppos <x> <y> <z>`                     | —                                                | `system.tp`                                | Teleports to coordinates in current dimension.                                       |
| `/teleportplayer <from> <to>`            | `/tpp`                                           | `system.tp.other`                          | Alias for moving one player to another.                                              |
| `/tpa <player>`                          | `/tpanfrage`                                     | `system.tpa`                               | Sends a teleport request (60 s TTL).                                                 |
| `/tpahere <player>`                      | —                                                | `system.tpa`                               | Asks player to teleport to you.                                                      |
| `/tpaccept [player]`, `/tpdeny [player]` | `/tpakzeptieren`, `/tpablehnen`                  | –                                          | Accept/deny pending TPA (no permission check).                                       |
| `/tplist`                                | `/tpanfragen`                                    | –                                          | Lists pending requests.                                                              |
| `/tptoggle`                              | —                                                | –                                          | Blocks/unblocks incoming requests.                                                   |
| `/top`                                   | —                                                | –                                          | Teleports to highest safe block at current X/Z.                                      |
| `/jump`                                  | —                                                | –                                          | Teleports to block you are looking at (max 120 blocks).                              |
| `/speed <0.1–5> [player]`                | `/geschwindigkeit`                               | `system.speed`, `system.speed.other`       | Adjusts walk/fly speed with feedback.                                                |
| `/fly [player]`                          | `/flight`, `/flug`, `/flugmodus`, `/fliegen`     | `system.fly`, `system.fly.other`           | Toggles flight; disables flying when revoked.                                        |
| `/gamemode <mode> [player]`              | `/gm`                                            | `system.gamemode`, `system.gamemode.other` | Supports numeric & shorthand modes; notifies both players.                           |
| `/god`                                   | —                                                | `system.god`                               | Toggles damage immunity via event listener.                                          |
| `/vanish [player]`                       | `/verschwinden`, `/v`                            | `system.vanish`, `system.vanish.other`     | Toggles vanish, fake join/leave messages and tab/name hiding.                        |
| `/suicide`                               | `/suizid`, `/selbstmord`                         | `system.suicide`                           | Kills the player and shows a localized message.                                      |

### Moderation & Chat

| Command                                        | Aliases                             | Permission(s)                          | Notes                                                                    |
| ---------------------------------------------- | ----------------------------------- | -------------------------------------- | ------------------------------------------------------------------------ |
| `/freeze [player]`                             | `/einfrieren`                       | `system.freeze`, `system.freeze.other` | Toggles freeze; prevents move/interact/break/place.                      |
| `/mute <player>` / `/unmute <player>`          | —                                   | `system.mute`                          | Persistent mute; chat handler blocks messages.                           |
| `/socialspy`                                   | —                                   | `system.socialspy`                     | Mirrors `/msg` traffic for staff.                                        |
| `/broadcast <message>`                         | —                                   | `system.broadcast`                     | Sends raw message to all players (no formatting).                        |
| `/chatclear`                                   | `/cc`, `/chatleeren`                | `system.chatclear`                     | Clears chat for everyone and posts localized confirmation.               |
| `/f3d`                                         | `/clearmychat`, `/meinenchatleeren` | –                                      | Clears personal chat buffer.                                             |
| `/msg <player> <message>` / `/reply <message>` | —                                   | `system.msg`                           | Private messaging with last‑reply memory, social spy and offline checks. |
| `/near [radius]`                               | —                                   | –                                      | Lists players within radius (default 100).                               |
| `/seen <player>`                               | —                                   | –                                      | Shows last‑seen timestamp tracked via state service.                     |

### World & Environment

| Command                                         | Aliases                                          | Permission(s)         | Notes                                                               |   |                                           |
| ----------------------------------------------- | ------------------------------------------------ | --------------------- | ------------------------------------------------------------------- | - | ----------------------------------------- |
| `/time set <ticks> [dimension]`                 | —                                                | `system.time`         | Sets day time for specified level; dimension suggestions supported. |   |                                           |
| `/time add <ticks> [dimension]`                 | —                                                | `system.time`         | Adds ticks to day time.                                             |   |                                           |
| `/weather <clear                                | rain                                             | thunder> [dimension]` | —                                                                   | – | Adjusts weather instantly (open command). |
| `/morning` `/day` `/midday` `/evening` `/night` | `/morgen`, `/tag`, `/mittag`, `/abend`, `/nacht` | –                     | Quick time presets (no built‑in permission check).                  |   |                                           |

### System & Integration

| Command               | Aliases                      | Permission(s)   | Notes                                                                             |
| --------------------- | ---------------------------- | --------------- | --------------------------------------------------------------------------------- |
| `/kchat [reload]`     | —                            | `system.kchat`  | Without args reapplies MOTD/tab/scoreboard; `/kchat reload` reloads `kchat.json`. |
| `/kessentials`        | `/essentials`, `/kessential` | –               | Shows plugin version.                                                             |
| `/kessentials reload` | —                            | `system.reload` | Reloads config, chat, locales and reapplies MOTD/tab/scoreboard.                  |

## Permissions (LuckPerms)

With the default layout (`system.%command%`) the important nodes include:

* **Teleport & travel** — `system.home`, `system.home.other(s)` (not used by default), `system.home.limit.<n>`, `system.warp`, `system.spawn`, `system.setspawn`, `system.rtp`, `system.back`, `system.tp`, `system.tp.other`, `system.tpa`.
* **Utilities** — `system.heal`, `system.feed`, `system.fly`, `system.speed`, `system.gamemode`, `system.god`, `system.vanish`, `system.suicide`, `system.hat`, `system.extinguish`, `system.workbench`, `system.anvil`, `system.enchantingtable`, `system.enderchest`, `system.enderchest.other`, `system.endersee`, `system.invsee`, `system.invsee.modify`, `system.armorsee`, `system.armorsee.modify`, `system.material` (open by default).
* **Moderation** — `system.freeze`, `system.freeze.other`, `system.mute`, `system.socialspy`, `system.broadcast`, `system.chatclear`, `system.clear`, `system.clear.other`, `system.repair`, `system.repair.other`, `system.msg`, `system.near` (open), etc.
* **Environment** — `system.time`, (weather/time preset commands are open unless gated via config), `system.kchat`, `system.reload`.

`permissionLayoutOther` inserts either `.other` or `.others`, so LuckPerms assignments such as `system.vanish.other` and `system.fly.others` both satisfy the check. Home limits use `permissionHomeLayout` (`system.home.limit.%limit%` by default). Legacy LuckPerms meta `kessentials.maxhomes` is still honoured as fallback.

### Migration (≤ 0.3.1 → new layouts)

1. Add `permissionLayout`, `permissionLayoutOther`, and `permissionHomeLayout` to `config/kessentials/config.json` (or delete the file to regenerate with defaults).
2. Adapt LuckPerms permissions — either use the default `system.*` scheme or switch the layout back to `kessentials.%command%`.
3. Home limits can now be granted directly via permissions (`system.home.limit.<n>`). Existing `kessentials.maxhomes` meta values remain valid.

## Discord (JDA)

* Bridge MC ↔ Discord; presence shows player count and honours manual status overrides. Slash/text commands require channel configuration in `discord.properties`.
* Text commands: `!say <msg>`, `!online`, `!kick <player> [reason]`, `!link <code>`; optional `!stop` via slash command if enabled.
* Slash commands: `/online`, `/stop` (if `enableStopCommand`), `/link <code>` (if `enableLinkCommand`).
* Linking: `/link` in‑game → `/link <code>` (Discord) adds player to whitelist and persists mapping.

## Locales

* Built‑in language files live under `src/main/resources/lang/` (e.g. `messages_de.properties`, `messages_en.properties`).
* Server‑specific overrides can be placed in `config/kessentials/lang/` (same filename pattern). These files override bundled keys.
* Default and per‑player locales are configured via `config/kessentials/lang/locales.json` (see example above). After editing, run `/kessentials reload` to apply changes without restarting the server.

## Placeholders

Supported in Tablist, Scoreboard, Chat and MOTD (where it makes sense):

* `%player%`, `%online%`, `%server_max%`, `%ping%`
* `%world%`, `%world_name_<dimensionId>%`
* `%luckperms-prefix%`, `%luckperms-suffix%`, `%rank%`
* `%lp-meta:<key>%` (arbitrary LuckPerms meta keys)
* `%statistic_<namespace:path>%` (e.g. `minecraft:deaths`, `minecraft:mob_kills`)
* `%animation:Name%` (frames/animations from `kchat.json`)

### Advanced Animations

Define animations with a custom **change interval** in `config/kessentials/kchat.json` (see example above). Reference via `%animation:MyAnimation1%` in tab/scoreboard/chat.

## MOTD, Tablist & Scoreboard (`config/kessentials/kchat.json`)

See the JSON example above. The MOTD uses full **MiniMessage** syntax; tablist and scoreboard are synced on join and every second; `/kchat reload` refreshes everything for all players and reapplies the MOTD.
