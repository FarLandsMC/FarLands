#!/bin/sh
# Usage: ./restart.sh <screen> <dedicatedMemory>

sleep 12
screen -S $1 -p 0 -X stuff "./server.sh $2\n"