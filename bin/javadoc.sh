#!/bin/sh
javadoc -notimestamp -encoding utf8 -charset utf8 -d javadoc/ -sourcepath src/java/ -subpackages zen
cp ~/etc/html/z-modified-jdk-api-javadoc-stylesheet.css javadoc/stylesheet.css
