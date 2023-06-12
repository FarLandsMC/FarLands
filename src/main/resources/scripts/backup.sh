#!/bin/sh
# Usage: ./backup.sh <screen> <homeDir> <serverDir> <backupDir> <dedicatedMemory>

sleep 12
# Temp disabled so we don't backup anarchy
#screen -S $1 -p 0 -X stuff "cd $2\n"
#screen -S $1 -p 0 -X stuff "time rsync -a --exclude=database.db $3/ $4\n"
#screen -S $1 -p 0 -X stuff "cd $3\n"
screen -S $1 -p 0 -X stuff "./server.sh $5\n"
