#!/bin/bash
java -jar ../crypt.jar 23 show
echo
OUT=$(java -jar ../crypt.jar 23 encrypt 'hello, world!')
echo $OUT
echo
java -jar ../crypt.jar 23 decrypt $OUT
