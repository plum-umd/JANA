#!/bin/bash
# export PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH
set -e
git submodule init
git submodule update
cd WALA 
mvn clean install -DskipTests=true 
cd ..
cd ppl
autoreconf
JAVA_HOME=`../../get_java_home.sh`
OSNAME=`../../get_os.sh`
JNI_EXT=`../../get_extension.sh`
./configure --enable-interfaces=Java --with-java=$JAVA_HOME
make
mkdir -p ../../scala-tools/lib/native/$OSNAME
cp interfaces/Java/jni/.libs/libppl_java.$JNI_EXT ../../scala-tools/lib/native/$OSNAME/libppl_java.$JNI_EXT
cp interfaces/Java/ppl_java.jar ../../scala-tools/lib/
mvn install:install-file -Dfile=./interfaces/Java/ppl_java.jar -DgroupId=com.bugseng.ppl -DartifactId=ppl -Dpackaging=jar -Dversion=1.3
cd ..
