#!/bin/sh

rm -f plugins/*.jar
cp -r plugins/FarLands/cache/* plugins/
/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java -Djline.terminal=jline.UnsupportedTerminal -Xmx$1 -Xms$1 -jar ./spigot.jar