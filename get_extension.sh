#!/bin/bash
set -e

JAVAC=`which javac`
READLINK="readlink"
PLATFORM=`uname`
if [[ "$PLATFORM" == 'Linux' ]]; then
    JNI_EXT="so"
    JAVA_HOME=$(dirname $(dirname `$READLINK -f $JAVAC`))
    OSNAME="linux_amd64"
elif [[ "$PLATFORM" == 'FreeBSD' ]]; then
    JNI_EXT="so"
    JAVA_HOME=$(dirname $(dirname `$READLINK -f $JAVAC`))
    OSNAME="freebsd_amd64"
elif [[ "$PLATFORM" == 'Darwin' ]]; then
    READLINK="greadlink"
    #JAVA_HOME=$(dirname $(dirname $(dirname `$READLINK -f $JAVAC`)))
    JAVA_HOME=`/usr/libexec/java_home`
    OSV=`uname -r | awk -F'.' '{ print $1 }'`
    # The extension changed for El Capitan
    if [ "$OSV" == "15" ]; then
      JNI_EXT="jnilib"
    else
      JNI_EXT="dylib"
    fi  
    OSNAME="mac_os_x_x86_64"
fi

echo $JNI_EXT
