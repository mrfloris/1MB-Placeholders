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
build/libs/1MB-Placeholders-v1.2.0-j25-1.21.11.jar
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

## Permissions

- `onemb.placeholders.admin` is the base permission required for every `/_placeholders` command.
- `onemb.placeholders.view` allows list, get, and preview commands.
- `onemb.placeholders.search` allows search commands.
- `onemb.placeholders.edit` allows add, set, and remove commands.
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
- In-game add, set, and remove commands write directly to `config.yml`, but the changes do not go live until you run `/_placeholders reload`.
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

## Support

This is made for the 1MoreBlock.com Minecraft server, you're free to use this for your own server, but support is 'as is', open a new Issue on Github here and we will try our best to give support.

## Credits

Code created, and .jar made by PyroTempus. (v 1.0.0 build 519 for 1.19.x)
Code updated by mrfloris and OpenAI. (v 1.2.1 build 533 for 1.21.11)

## Placeholders

Use placeholders with this syntax:

```text
%onemb_[placeholder]%
```

Examples from the default config:

```text
%onemb_1mb_version%
%onemb_1mb_staff%
%onemb_plugin_version%
%onemb_plugin_build%
%onemb_mc_version%
%onemb_paper_version%
%onemb_server_engine%
%onemb_java_version%
%onemb_day_name%
%onemb_header%
%onemb_footer%
```
