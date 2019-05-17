#!/bin/bash
apt-get update
apt-get install -y build-essential git libgmp-dev openjdk-8-jdk openjdk-7-jdk maven ant subversion scala m4 autoconf automake apt-transport-https
echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
apt-get update
apt-get install -y sbt
update-java-alternatives -s java-1.7.0-openjdk-amd64
