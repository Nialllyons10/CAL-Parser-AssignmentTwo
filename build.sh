#!/usr/bin/env bash
jjtree assignment2.jjt;
javacc -debug_parser assignment2.jj;
javac *.java
