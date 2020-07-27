#!/bin/sh

rm -f plugins/*.jar
cp -r plugins/FarLands/cache/* plugins/
java -Djline.terminal=jline.UnsupportedTerminal -Xmx$1 -Xms$1 -jar ./spigot.jar