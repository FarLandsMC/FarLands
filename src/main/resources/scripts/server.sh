#!/bin/sh

if [ -f plugins/FarLands/cache/paper.jar ]; then
    mv plugins/FarLands/cache/paper.jar ./
fi

rm -f plugins/*.jar
cp -r plugins/FarLands/cache/*.jar plugins/
java -Djline.terminal=jline.UnsupportedTerminal -Xmx$1 -Xms$1 -jar ./paper.jar