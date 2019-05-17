#!/bin/bash

javac match/util/StringUtil.java
javac match/main/Main.java
jar cfe match.jar match.main.Main match/main/Main.class match/util/StringUtil.class

if [ ! -d sunday-dist ]; then
    mkdir sunday-dist
fi

cp match.jar sunday-dist
cp -r example sunday-dist
cp challenge.txt sunday-dist
cp description.txt sunday-dist

tar -cvf sunday-dist.tar sunday-dist/*
