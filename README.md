# 1MB Placeholders

Little plugin for 1MoreBlock.com that adds global PlaceholderAPI values we can reuse across the server in holograms, menus, scoreboards, chat, and other plugins.

## Dependencies

- Paper `1.21.11` API
- PlaceholderAPI plugin
- LuckPerms, if you want to assign the `onemb.placeholders.*` or individual `onemb.placeholders.*` permissions for the `/_placeholders` admin commands
- Tested against `PlaceholderAPI-2.12.3-DEV-265.jar`
- PlaceholderAPI builds can be downloaded from <https://ci.extendedclip.com/job/PlaceholderAPI/>

## Compile

### macOS Java 25 setup

If you have both Java 25 and Java 26 installed, point your shell to Java 25 before running Gradle:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew clean build
```

This produces:

```text
build/libs/1MB-Placeholders-v1.2.2-534-j25-1.21.11.jar
```

### Rollback build for Java 21 and Paper 1.19.1

If you want to reproduce the older baseline before the upgrade work, use:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
./gradlew clean build \
  -PtargetJava=21 \
  -PminecraftVersion=1.19.1 \
  -PpaperApiVersion=1.19.1-R0.1-SNAPSHOT \
  -PapiVersion=1.19 \
  -PpluginVersion=1.0.0 \
  -PbuildNumber=519
```

That produces:

```text
build/libs/1MB-Placeholders-v1.0.0-j21-1.19.1.jar
```

## Installation

1. Install Paper `1.21.11`.
2. Install PlaceholderAPI.
3. Drop the compiled `1MB-Placeholders` jar into the server `plugins/` folder.
4. Start the server once so the plugin creates `plugins/1MB-Placeholders/config.yml`.
5. Edit the config if needed.
6. Run `/_placeholders reload` when you want saved changes to go live.

## Releases

Pre-compiled release jars can be downloaded from the GitHub Releases section in the repository sidebar on the right side of the GitHub page.

## Commands

- `/_placeholders` lists placeholders. Players get paginated output and console shows all matches.
- `/_placeholders <page>` lets players jump to another page.
- `/_placeholders list [category] [page]` lists placeholders, optionally filtered by category.
- `/_placeholders get <key>` shows stored and live details for one placeholder.
- `/_placeholders get <category> <key>` does the same with an explicit category check.
- `/_placeholders preview <key>` shows raw, configured, formatted, and plain previews.
- `/_placeholders preview <category> <key>` does the same with an explicit category check.
- `/_placeholders search <text> [category] [page]` searches keys, descriptions, and values.
- `/_placeholders add <category> <key> <value...>` saves a new static placeholder to config.yml.
- `/_placeholders add [category:]<key> <value...>` is still accepted for compatibility, but the spaced category form is the preferred syntax.
- `/_placeholders category <category> <true|false>` enables or disables a whole placeholder category in config.yml.
- `/_placeholders set <key> <value...>` updates an existing static placeholder in config.yml.
- `/_placeholders set <category> <key> <value...>` does the same with an explicit category check.
- `/_placeholders remove <key>` removes a placeholder from config.yml.
- `/_placeholders remove <category> <key>` does the same with an explicit category check.
- `/_placeholders reload` reloads placeholders from config.yml.
- `/_placeholders backup` creates a timestamped backup of config.yml.
- `/_placeholders debug` shows plugin diagnostics, runtime details, and available debug actions.
- `/_placeholders debug config` safely adds any missing bundled default config nodes to an existing config.yml without overwriting current values.
- `/_placeholders debug clear logs` clears log files while keeping a persistent purge history.
- `/_placeholders debug clear backups` clears backup files and audits who cleared them.

## Command Examples

```text
/_placeholders
/_placeholders 2
/_placeholders list system
/_placeholders search paper
/_placeholders get paper_version
/_placeholders preview branding header
/_placeholders add events event_name Summer Festival
/_placeholders set default 1mb_version 3.11.8
/_placeholders category events true
/_placeholders category rotating false
/_placeholders reload
/_placeholders debug
/_placeholders debug config
```

## Permissions

- `onemb.placeholders.admin` is the base permission required for every `/_placeholders` command.
- `onemb.placeholders.view` allows list, get, and preview commands.
- `onemb.placeholders.search` allows search commands.
- `onemb.placeholders.edit` allows add, set, remove, and category toggle commands.
- `onemb.placeholders.reload` allows reload commands.
- `onemb.placeholders.backup` allows backup commands.
- `onemb.placeholders.debug` allows debug and maintenance commands such as `/_placeholders debug config`.
- `onemb.placeholders.*` grants all of the above.
- Add `onemb.placeholders.*` to the `1mb_owner` LuckPerms group, or assign the individual permissions to any staff group or player that should manage placeholders.

## Config Notes

- Placeholders are grouped by category.
- Each category has `enabled: true/false`, so you can prepare event placeholders ahead of time and enable them later.
- Every placeholder can have a `description`.
- Built-in categories such as `system` and `date` are generated automatically by the plugin.
- New bundled config defaults are not auto-inserted into an already existing live `config.yml`.
- `/_placeholders debug config` can merge any missing bundled default nodes into the existing file without replacing your own edits or custom placeholders.
- `/_placeholders debug` shows a support-friendly overview with plugin/build/runtime details, category counts, placeholder counts, and bundled config validation status.
- `/_placeholders debug` also shows config paths, log/backup counts, formatting/listing settings, and up to five current validation issues.
- `Listing.show-category`, `Listing.only-show-enabled-placeholders`, `Listing.show-category-description`, `Listing.show-type`, and `Listing.show-category-count` control how `/_placeholders` and `/_placeholders list` render output.
- Commands that target placeholders accept bare keys such as `1mb_version`, full placeholder references such as `%onemb_1mb_version%`, and category-prefixed references such as `system:mc_version`.
- Rotating placeholders are supported through `type: rotating` and a `values:` list.
- In-game add, set, remove, and category toggle commands write directly to `config.yml`, but the changes do not go live until you run `/_placeholders reload`.
- Backups are written to `plugins/1MB-Placeholders/backups/`.
- Admin actions such as add, set, remove, reload, backup, and debug config are written to `plugins/1MB-Placeholders/logs/actions.log`.
- Log and backup purges keep a durable audit record in `plugins/1MB-Placeholders/logs/purge-history.log`.
- The plugin validates placeholder structure on load and reload, and `/_placeholders debug` surfaces current validation issues.

## Placeholder Types

- `static` stores a fixed value directly in `config.yml`.
- `builtin` is generated automatically by the plugin from server, plugin, Java, or date information.
- `rotating` cycles through a configured `values:` list using `interval-seconds` while the placeholder and category are enabled.

## Formatting Notes

- `Formatting.parse-mini-message` enables direct MiniMessage parsing for values such as `<gold>Hello</gold>`.
- `Formatting.convert-legacy-ampersand-codes` enables conversion for values such as `&6Hello`.
- `Formatting.strip-formatting` returns plain text with color and formatting removed.
- The default bundled config enables MiniMessage parsing and legacy ampersand conversion.
- `/_placeholders preview <key>` helps compare the stored value, configured output, formatted preview, and plain preview before you switch formatting on globally.

## FAQ

- Can I use these in ajLeaderboard and CMI Holograms?
  Yes.
- If I add a new placeholder called `banana_example` then what do I use in-game?
  `%onemb_banana_example%`
- Can I use MiniMessage `<gold>..</gold>` and CMI `{#hex}` colors?
  Yes, by default these are enabled and supported, you can turn this off in `config.yml`.
- Do I need PlaceholderAPI installed?
  Yes. This plugin depends on PlaceholderAPI and will not work without it.
- Do changes from `/_placeholders add`, `set`, `remove`, or `category` go live instantly?
  No. They are written to `config.yml` immediately, and then you run `/_placeholders reload` when you want them to become active.
- Can I disable a whole event or seasonal set of placeholders at once?
  Yes. Use categories and toggle them with `/_placeholders category <category> <true|false>`, for example `/_placeholders category events true`.
- Can I edit built-in placeholders such as `paper_version` or `java_version`?
  No. Built-in placeholders are generated by the plugin. Use your own `static` placeholders when you want custom values.
- Where are backups and admin logs stored?
  Backups are stored in `plugins/1MB-Placeholders/backups/` and admin action logs are stored in `plugins/1MB-Placeholders/logs/`.

## Support

This is made for the 1MoreBlock.com Minecraft server, you're free to use this for your own server, but support is 'as is', open a new Issue on Github here and we will try our best to give support.

## Credits

Code created, and .jar made by PyroTempus. (v 1.0.0 build 519 for 1.19.x)
Code updated by mrfloris and OpenAI. (v 1.2.2 build 534 for 1.21.11)

## Placeholders

Use placeholders with this syntax:

```text
%onemb_[placeholder]%
```

Default `config.yml` on first install:

```yaml
# 1MB Placeholders configuration
#
# Placeholder syntax:
# Use placeholders in-game as %onemb_[placeholder]%
# Example:
#   %onemb_1mb_version%
#   %onemb_mc_version%
#   %onemb_day_name%
# Admin commands accept bare keys like 1mb_version,
# full references like %onemb_1mb_version%,
# and category:key references like system:mc_version.
#
# Placeholder types:
# - static: a fixed value stored in this config.yml file.
# - builtin: a value generated by the plugin from server, plugin, or date data.
# - rotating: cycles through a configured values: list using interval-seconds while enabled.
#
# In-game edits from /_placeholders add|set|remove are written to this file immediately.
# They do not go live until you run /_placeholders reload.

Formatting:
  # Best for modern MiniMessage values such as <gold>Hello</gold>.
  parse-mini-message: true
  # Best for legacy values such as &6Hello.
  convert-legacy-ampersand-codes: true
  # When true, the placeholder output is returned as plain text with formatting removed.
  strip-formatting: false

Listing:
  # When true, list output groups placeholders by category.
  show-category: true
  # When true, disabled categories and inactive rotating placeholders stay hidden in list output.
  only-show-enabled-placeholders: false
  # When true and categories are shown, include the category description in list output.
  show-category-description: true
  # When true, include placeholder types such as static, builtin, or rotating.
  show-type: true
  # When true and categories are shown, include the visible placeholder count for that category.
  show-category-count: true

Placeholders:
  default:
    enabled: true
    description: Manual placeholders that stay available during normal server use.
    placeholders:
      1mb_version:
        type: static
        description: The current 1MoreBlock server version string.
        value: '3.11.7.5'
      1mb_staff:
        type: static
        description: Staff credit line shown around the server.
        value: 'mrfloris, tidala, and others'
      server_status:
        type: static
        description: Manual server status text.
        value: 'Live'

  branding:
    enabled: true
    description: Branding, lines, and reusable visual placeholders.
    placeholders:
      brand_color:
        type: static
        description: Shared brand color token.
        value: '{#orange}'
      header:
        type: static
        description: Header line used in holograms and menus.
        value: '{#orange}--------------------------------'
      footer:
        type: static
        description: Footer line used in holograms and menus.
        value: '{#orange}--------------------------------'

  system:
    enabled: true
    description: Special built-in placeholders generated automatically by the plugin.
    placeholders:
      plugin_version:
        type: builtin
        builtin: plugin_version
        description: 1MB-Placeholders plugin version.
      plugin_build:
        type: builtin
        builtin: plugin_build
        description: 1MB-Placeholders build number.
      mc_version:
        type: builtin
        builtin: minecraft_version
        description: Minecraft server version.
      java_version:
        type: builtin
        builtin: java_version
        description: Active Java version.
      paper_version:
        type: builtin
        builtin: paper_version
        description: Paper Minecraft version, for example 1.21.11.
      server_engine:
        type: builtin
        builtin: server_engine
        description: "Full server engine string, for example 1.21.11-130-c5a2736 (MC: 1.21.11)."
      online_players:
        type: builtin
        builtin: online_players
        description: Current number of online players.
      max_players:
        type: builtin
        builtin: max_players
        description: Maximum player slots.

  date:
    enabled: true
    description: Built-in date placeholders from the server clock.
    placeholders:
      day_name:
        type: builtin
        builtin: day_name
        description: Full weekday name, for example Monday.
      day_of_week:
        type: builtin
        builtin: day_of_week
        description: Weekday number from 1 to 7.
      day_of_month:
        type: builtin
        builtin: day_of_month
        description: Day number in the current month.
      month_name:
        type: builtin
        builtin: month_name
        description: Full month name, for example March.
      month_number:
        type: builtin
        builtin: month_number
        description: Two-digit month number.
      year:
        type: builtin
        builtin: year
        description: Four-digit year.
      date_iso:
        type: builtin
        builtin: date_iso
        description: ISO date in YYYY-MM-DD format.
      time_24h:
        type: builtin
        builtin: time_24h
        description: 24-hour time in HH:mm format.

  events:
    enabled: false
    description: Pre-stage event placeholders here, then enable this category when the event goes live.
    placeholders:
      event_name:
        type: static
        description: Name of the current event.
        value: 'Winter Event'
      event_status:
        type: static
        description: Status text for the current event.
        value: 'Starting soon'

  rotating:
    enabled: false
    description: Rotating placeholders that change value over time when their category is enabled.
    placeholders:
      rotating_greeting:
        type: rotating
        description: Greeting that rotates every 60 seconds.
        rotating-enabled: true
        interval-seconds: 60
        values:
          - hey
          - hi
          - hello
          - nice to see you
          - oh there you are
          - hello there
          - hi hi
```
