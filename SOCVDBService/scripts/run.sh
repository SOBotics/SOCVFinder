#!/bin/bash

# This will start up the bot.
# Requires `java` command to be linked to JDK 1.8 or higher.
# If you get a `Permission Denied` error, make sure the
# script is executable by running `chmod +x run.sh`

if [ "${PWD##*/}" == "scripts" ]; then
    cd ..
    java -cp "SOCVDBService.jar:lib/*" jdd.so.bot.ChatBot
else
    echo "Please run this from within scripts/"
fi

