# FarLands

PaperMC plugin for the FarLands Minecraft server (FarLandsMC.net)

## Setup

```shell
git clone git@github.com:FarLandsMC/FarLands.git # Clone Repo
mvn install # Download Dependencies
```

If you run into an Auth error while trying to download ChestShops or RegionProtection, please read [this](https://github.com/FarLandsMC/FarLands/wiki/Authentication-Failed-error-when-downloading-maven-dependencies).

## Build

```shell
mvn package
# or
mvn package -Dmaven.javadoc.skip=true # Don't update docs
```

## Plugins

The plugins that we use on the server.  
Some are required for the plugin to work, these are marked with `*`

- [ChestShops](https://github.com/FarLandsMC/ChestShops)*
- [RegionProtection](https://github.com/FarLandsMC/RegionProtection)*
- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)*
- [CoreProtect](https://www.spigotmc.org/resources/coreprotect.8631/)*
- [NuVotifier](https://www.spigotmc.org/resources/nuvotifier.13449/)*
- [HeadDatabase](https://www.spigotmc.org/resources/head-database.14280/)
- [OpenInv](https://dev.bukkit.org/projects/openinv)
- [WorldEdit](https://dev.bukkit.org/projects/worldedit)
- [BuycraftX](https://www.spigotmc.org/resources/buycraftx-bungeecord.25191/)

## Layout

- Commands - *All* commands that can be run
  - Discord
    - Commands that are intended for discord use/interaction
  - Player
    - Commands that are specific for players - Staff subcommands are here, too
  - Staff
    - Commands for staff members to use in moderation/building/etc
- Data - Handling of the server data
- Discord - Manage everything to do with Discord interaction 
- Gui - GUI classes and handler
- Mechanic - Various small tweaks and anti-cheat mechanics
- Scheduling - Scheduling utilities
- Util - Miscellaneous utility classes
