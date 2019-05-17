#!/bin/bash
# export PATH=/usr/lib/jvm/java-8-openjdk-amd64/bin:$PATH
set -e
git submodule init
git submodule update
cd joana2/contrib
git submodule init
git submodule update
mkdir -p wala/com.ibm.wala.dalvik.test/lib
mkdir -p wala/com.ibm.wala.dalvik.test/data
cp ../../troff2html.cup wala/com.ibm.wala.dalvik.test/data/
cp ../../dx.jar wala/com.ibm.wala.dalvik.test/lib/
cd wala
mvn clean verify -DskipTests=true 
cd ../..
ant 
cd ..
mkdir -p WALA/com.ibm.wala.dalvik.test/lib
mkdir -p WALA/com.ibm.wala.dalvik.test/data
cp troff2html.cup WALA/com.ibm.wala.dalvik.test/data/
cp dx.jar WALA/com.ibm.wala.dalvik.test/lib/
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
