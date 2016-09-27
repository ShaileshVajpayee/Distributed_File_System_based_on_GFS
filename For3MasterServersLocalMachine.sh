#!/bin/bash

javac MasterServer.java
java MasterServer ‘Port’ ‘IP' & javac MasterServer.java
java MasterServer ‘Port’ ‘IP' & javac MasterServer.java
java MasterServer ‘Port’ ‘IP'