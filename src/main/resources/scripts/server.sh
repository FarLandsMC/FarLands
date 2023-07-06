#!/bin/sh

cached_jar="plugins/FarLands/cache/paper.jar"
if [ -f $cached_jar ]; then
    mv paper.jar paper-old.jar
    mv $cached_jar ./
fi

rm -f plugins/*.jar
cp -r plugins/FarLands/cache/*.jar plugins/
java -Djline.terminal=jline.UnsupportedTerminal -Xmx$1 -Xms$1 -jar ./paper.jar