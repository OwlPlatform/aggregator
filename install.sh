#!/bin/bash

OWL_USER=owl
VERSION="1.0.0"

SRC_JAR_DIR="target"
SRC_JAR_FILE="owl-aggregator-$VERSION-SNAPSHOT-jar-with-dependencies.jar"
SRC_SCRIPT_DIR="scripts"
SRC_CONTROL_SCRIPT="owl-aggregator"
SRC_INIT_SCRIPT="owl-aggregator.init"

LOG_DIR="/var/log/owl"
INSTALL_DIR="/usr/local/bin/owl"
INIT_DIR="/etc/init.d"
DST_JAR_FILE="owl-aggregator.jar"
DST_CONTROL_SCRIPT="owl-aggregator"
DST_INIT_SCRIPT="owl-aggregator"

# Create user if it doesn't exist
useradd -c "Owl Platform" -M -s /usr/sbin/nologin -U $OWL_USER 

# Create directories needed to run
install -d -o $OWL_USER $INSTALL_DIR
install -d -o $OWL_USER $LOG_DIR

# Executables
install -o $OWL_USER  $SRC_JAR_DIR/$SRC_JAR_FILE $INSTALL_DIR/$DST_JAR_FILE
install -o $OWL_USER $SRC_SCRIPT_DIR/$SRC_CONTROL_SCRIPT $INSTALL_DIR/$DST_CONTROL_SCRIPT

# Init service
install $SRC_SCRIPT_DIR/$SRC_INIT_SCRIPT $INIT_DIR/$DST_INIT_SCRIPT

update-rc.d owl-aggregator defaults
