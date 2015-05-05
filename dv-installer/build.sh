#!/bin/bash

# Messaging
scriptname=$(basename $0)
fatalmsg () {
    echo "$scriptname error: $@"
    exit
}
infomsg () {
    echo "$scriptname: $@"
}

#[ "$#" -le 2 ] || fatalmsg "Need at most 2 arguments"

actiondesc=""

infomsg "Settings MAVEN_OPTS to repo root"
export MAVEN_OPTS="$MAVEN_OPTS -Duser.home=`pwd`"
echo $MAVEN_OPTS


# Argument is clean mode
if [ "$1" = "clean" ]; then
    infomsg "Cleaning"
    actiondesc="Clean installer"
    infomsg "Destroying .m2 directory"
    rm -rf .m2
    #these are all removed per build anyways.
    #infomsg "Removing soa/installer/parsed"
    #rm -rf soa/installer/parsed
    #infomsg "Removing brms/installer/parsed"
    #rm -rf brms/installer/parsed
    #infomsg "Removing bpms/installer/parsed"
    #rm -rf bpms/installer/parsed
    infomsg "Clearing runtime dependencies folder"
    rm -rf common-files/runtime-dependencies/*.jar
    infomsg "Clearing izpack compiler"
    rm -f standalone-compiler/*
    infomsg "Clearing eap-dist"
    rm -rf eap/eap-dist/*
    infomsg "Clearing soa-dist"
    rm -rf soa/soa-dist/*
    infomsg "Clearing brms-dist"
    rm -rf brms/brms-dist/*
    infomsg "Clearing bpms-dist"
    rm -rf bpms/bpms-dist/*
    infomsg "Clearing sramp-dist"
    rm -rf sramp/sramp-dist/*
#    infomsg "Clearing ds-dist"
#    rm -rf ds/ds-dist/*

    infomsg "Running maven clean goal for installer project"
    mvn clean

    # Clean izpack project if it is given
    if [ ! -z "$2" ] && [ -f "$2/pom.xml" ]; then
        infomsg "Running maven clean goal for izpack project"
        actiondesc="$actiondesc and izpack"

        pushd "$2"
        mvn clean
        popd
    else
        infomsg "No izpack project given to clean, skipping"
    fi

# Argument is the path to izpack reporoot, containing the izpack project
elif [ ! -z "$1" ] && [ -f "$1/pom.xml" ]; then
    mkdir target
    infomsg "Creating .m2 directory in .m2/"
    mkdir -p .m2/

    infomsg "Copying ./${3}settings.xml to .m2/"
    cp pom.${3} pom.xml
    cp ${3}settings.xml .m2/settings.xml
    infomsg "Building izpack then installer"
    actiondesc="Build izpack then installer"

    # Create a space delimited list of the rest of the arguments
    restOfArgs=${@:2:$(($#))}

    # Setup izpack to build with mvn install
    #export IZPACK_ANT_HOME=$(which ant | sed s.bin/ant..)

    # Build the izpack project
    pushd "$1"
    mvn install || fatalmsg "izpack build failed"
    popd

    # Build the installer project with the remaining arguments
    mvn install $restOfArgs || fatalmsg "installer build failed"

# Something else
else
    infomsg "Building installer only"
    actiondesc="Build only installer"
    infomsg "Creating .m2 directory in .m2/"
    mkdir -p .m2/

    infomsg "Copying ./${2}settings.xml to .m2/"
    cp pom.${2} pom.xml
    cp ${2}settings.xml .m2/settings.xml

    # pass all args to maven
    mvn install $@ || fatalmsg "installer build failed"
fi

infomsg "Completed action ($actiondesc) successfully"
