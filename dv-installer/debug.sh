#!/bin/sh

if [ "$#" -lt 2 ]
then
    echo "Usage: ./debug.sh (eap|fsw|dv|bpms|brms) PORT [-console]";
    exit 1
fi

installer=$(ls -t target/*.jar | grep $1-installer | head -1)

if [ -z $installer ]
then
    echo "No $1 installer found in target."
    exit 1
fi

echo "[ Running latest $1 installer jar debug mode ]"
java -jar -DTRACE=true -Xdebug \
    -Xrunjdwp:server=y,transport=dt_socket,address=$2,suspend=y \
    $installer $3;

