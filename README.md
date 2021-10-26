# FarLands

PaperMC plugin for the FarLands Minecraft server (FarLandsMC.net)

## Setup

```shell
git clone git@github.com:FarLandsMC/FarLands.git # Clone Repo
mvn install # Download Dependencies
```

## Build

```shell
mvn package
# or
mvn package -Dmaven.javadoc.skip=true # Don't update docs
```

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
