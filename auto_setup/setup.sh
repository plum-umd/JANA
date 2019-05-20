#!/bin/bash

# Clone the JANA repository here
git clone https://github.com/plum-umd/JANA.git


# Import zip with all of the directories
cp /vagrant/resources.tar.gz .
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
cp ~/zip/Makefile.config.apron ~/tools/apron/Makefile.config

# Perform symlink
cd /usr/lib/jvm/java-8-openjdk-amd64/include
sudo ln -s linux/jni_md.h .
cd ~/tools/apron

# Make and make install
make && sudo make install

# Install ELINA
cd ~/tools
git clone https://github.com/eth-sri/ELINA.git
cd ELINA
git checkout ae3404f84c0ec4e746bdaa31eca70ca580029165

# Copy the makefile
cp ~/zip/Makefile.config.elina Makefile.config
cp ~/zip/Makefile.elina ./java_interface/Makefile
cp ~/zip/Test.java.fixed ./java_interface/elina/Test.java

# Make and make install
make && sudo make install

# Go back to JANA
cd ~/JANA/externs
cp ~/zip/build.sh.fixed ./build.sh
./build.sh # This will fail but it's okay

# Replace the pom.xml file and the build.xml file
cp ~/zip/test.data.build.xml WALA/com.ibm.wala.cast.js.test.data/build.xml
cp ~/zip/WALA.pom.xml WALA/pom.xml

cd ../scala-tools

# Copy in JARS
cp ~/tools/apron/japron/apron.jar lib/
cp ~/tools/apron/japron/gmp.jar lib/
cp ~/tools/ELINA/java_interface/elina.jar lib/

# Copy in top level makefile
cd ..
cp ~/zip/Makefile.top ./Makefile

# Make
make

# Decompress benchmark
jar xf ~/zip/dacapo-2006-10-MR2.jar
cd scala-tools
mkdir -p files/xalan
touch files/xalan/xalan.txt

export LD_LIBRARY_PATH=/usr/local/lib

# Test ainterp
java -Djava.library.path=/usr/java/packages/lib/amd64:/usr/lib/x86_64-linux-gnu/jni:/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu:/usr/lib/jni:/lib:/usr/lib:/usr/local:/usr/local/lib -jar AInterp.jar -a inter-bottomup --domain BOX --context 0-CFA --obj_rep ALLOCATION --out xalan --index 0 -D/home/vagrant/JANA/dacapo -Sdacapo.xalan.Main.* "dacapo.xalan.Main.main([Ljava/lang/String;)V"
