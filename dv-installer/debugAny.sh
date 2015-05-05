#!/bin/sh

if test $# -lt 1;
then
    echo "Usage: ./debug.sh PORT [-console]";
    exit;
else
    java -jar -DTRACE=true -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=$1,suspend=y $2 $3;
fi
