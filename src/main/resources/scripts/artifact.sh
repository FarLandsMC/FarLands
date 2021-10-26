#!/bin/sh
# Usage: ./artifact.sh <screen> <paperDownload> <dedicatedMemory> <downloadPaper>

sleep 12
if [ "$4" = "true" ]; then
    screen -S $1 -p 0 -X stuff "wget -O spigot.jar $2\n"
fi
screen -S $1 -p 0 -X stuff "./server.sh $3\n"