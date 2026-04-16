# 1MB Placeholders

Little plugin for 1MoreBlock.com that adds global PlaceholderAPI values we can reuse across the server in holograms, menus, scoreboards, chat, and other plugins.

## Dependencies

- Paper `1.21.11` API
- PlaceholderAPI plugin
- LuckPerms, if you want to assign the `onemb.placeholders.admin` permission for the `/_placeholders` in-game admin command
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
build/libs/1MB-Placeholders-v1.1.0-j25-1.21.11.jar
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
5. Stop the server, edit the config if needed, then start the server again.

## Releases

Pre-compiled release jars can be downloaded from the GitHub Releases section in the repository sidebar on the right side of the GitHub page.

## Commands

- `/_placeholders` lists all configured placeholders and their values.
- `/_placeholders help` shows the command help.
- `/_placeholders reload` reloads placeholders from `config.yml` without restarting the server.

## Permissions

- `onemb.placeholders.admin` allows use of all `/_placeholders` commands.
- Add this permission to the `1mb_owner` LuckPerms group, or to any group or player that should manage these placeholders.

## Support

This is made for the 1MoreBlock.com Minecraft server, you're free to use this for your own server, but support is 'as is', open a new Issue on Github here and we will try our best to give support.

## Credits

Code created, and .jar made by PyroTempus. (v 1.0.0 build 519 for 1.19.x)
Code updated by mrfloris and OpenAI. (v 1.1.0 build 521 for 1.21.11)

## Placeholders

Use placeholders with this syntax:

```text
%onemb_[placeholder]%
```

Examples from the default config:

```text
%onemb_1mb_version%
%onemb_mc_version%
%onemb_java_version%
%onemb_1mb_staff%
%onemb_header%
%onemb_footer%
```
