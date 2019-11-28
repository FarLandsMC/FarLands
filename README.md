# FarLands

Private bukkit plugin for FarLands

## Docker Instructions

```sh
# Change directory to the docker folder.
cd docker

# Build the buildtools image. Run when you need a new Spigot executable.
./build.py build-no-cache bt

# Build the plugin image.
./build.py build pl

# Launch yourself into a new VM with the server files.
./build.py run pl

# Extract server files to host.
./build.py extract pl
```
