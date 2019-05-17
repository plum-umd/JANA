#!/bin/bash

javac crypt/util/CryptConfig.java
javac crypt/main/Main.java
jar cfe crypt.jar crypt.main.Main crypt/main/Main.class crypt/util/CryptConfig.class

if [ ! -d monday-dist ]; then
    mkdir monday-dist
fi

cp crypt.jar monday-dist
cp -r example monday-dist
cp challenge.txt monday-dist
cp description.txt monday-dist

tar -cvf monday-dist.tar monday-dist/*
