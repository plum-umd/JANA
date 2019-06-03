#!/bin/bash

# Clone the JANA repository here
tar xf resources.tar.gz

# Install all dependencies
sudo apt update
yes | sudo apt upgrade
yes | sudo apt install default-jdk libgmp-dev libmpfr-dev subversion make gcc autoconf python g++
yes | sudo apt install software-properties-common
sudo apt-add-repository universe
sudo apt update
yes | sudo apt install maven
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt update
yes | sudo apt install sbt

# Add .sbt script

yes exit | sbt
mkdir -p ~/.sbt/1.0/plugins # WILL BREAK IF SBT VERSION CHANGES
cp zip/plugins.sbt ~/.sbt/1.0/plugins

# Install tool dependencies
mkdir tools
cd tools
svn co svn://scm.gforge.inria.fr/svnroot/apron/apron/trunk apron
cp ../zip/Makefile.config.apron apron/Makefile.config

# Perform symlink
JAVAC=`which javac`
READLINK="readlink"
JAVA_HOME=$(dirname $(dirname `$READLINK -f $JAVAC`))
sudo ln -s $JAVA_HOME/include/linux/jni_md.h $JAVA_HOME/include
cd apron

# Make and make install
make && sudo make install

# Install ELINA
cd ..
git clone https://github.com/eth-sri/ELINA.git
cd ELINA
git checkout ae3404f84c0ec4e746bdaa31eca70ca580029165

# Copy the makefile
cp ../../zip/Makefile.config.elina.inplace Makefile.config
cp ../../zip/Makefile.elina ./java_interface/Makefile
cp ../../zip/Test.java.fixed ./java_interface/elina/Test.java
cp ../apron/japron/apron/japron.h ./java_interface/elina
cp ../apron/japron/gmp/jgmp.h ./java_interface/elina
# Make and make install
make && sudo make install

# Go back to JANA
cd ../../../externs
#cp ../auto_setup/zip/build.sh.fixed ./build.sh ##
git submodule init
git submodule update

# Replace the pom.xml file and the build.xml file
cp ../auto_setup/zip/test.data.build.xml WALA/com.ibm.wala.cast.js.test.data/build.xml
cp ../auto_setup/zip/WALA.pom.xml WALA/pom.xml

cd ../scala-tools

# Copy in JARS
cp ../auto_setup/tools/apron/japron/apron.jar lib/
cp ../auto_setup/tools/apron/japron/gmp.jar lib/
cp ../auto_setup/tools/ELINA/java_interface/elina.jar lib/

# Copy in top level makefile
cd ..
#cp ~/zip/Makefile.top ./Makefile ##REPLACED

# Make
make

# Decompress benchmark
jar xf auto_setup/zip/dacapo.jar
mv dacapo-2006-10-MR2/ dacapo
cd scala-tools
mkdir -p files/xalan
touch files/xalan/xalan.txt

export LD_LIBRARY_PATH=/usr/local/lib

# Test ainterp
java -Djava.library.path=/usr/java/packages/lib/amd64:/usr/lib/x86_64-linux-gnu/jni:/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu:/usr/lib/jni:/lib:/usr/lib:/usr/local:/usr/local/lib -jar AInterp.jar -a inter-bottomup --domain BOX --context 0-CFA --obj_rep ALLOCATION --out xalan --index 0 -D/home/vagrant/JANA/dacapo -Sdacapo.xalan.Main.* "dacapo.xalan.Main.main([Ljava/lang/String;)V"
