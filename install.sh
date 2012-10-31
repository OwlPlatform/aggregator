#!/bin/bash

OWL_USER=owl
VERSION="1.0.0"

SRC_JAR_DIR="target"
SRC_JAR_FILE="owl-aggregator-$VERSION-SNAPSHOT-jar-with-dependencies.jar"
SRC_SCRIPT_DIR="scripts"
SRC_CONTROL_SCRIPT="owl-aggregator"
SRC_INIT_SCRIPT="owl-aggregator.init"
LOG4J_DEV_FILE="src/main/resources/log4j.xml"
LOG4J_FILE="src/main/resources/log4j-install.xml"

LOG_DIR="/var/log/owl"
INSTALL_DIR="/usr/local/bin/owl"
INIT_DIR="/etc/init.d"
DST_JAR_FILE="owl-aggregator.jar"
DST_CONTROL_SCRIPT="owl-aggregator"
DST_INIT_SCRIPT="owl-aggregator"

if [ ! -e $SRC_JAR_DIR/$SRC_JAR_FILE ]; then
  echo "Compiling aggregator using Apache Maven"
  mv "$LOG4J_DEV_FILE" "$LOG4J_DEV_FILE.bkp" && \
  cp "$LOG4J_FILE" "$LOG4J_DEV_FILE" && \
  mvn clean package >/dev/null && \
  mv "$LOG4J_DEV_FILE.bkp" "$LOG4J_DEV_FILE" || 
  echo "Unable to compile the aggregator."
fi

# Create user if it doesn't exist
sudo /usr/sbin/useradd -c "Owl Platform" -M -s /usr/sbin/nologin -U $OWL_USER 

# Create directories needed to run
sudo install -d -o $OWL_USER $INSTALL_DIR
sudo install -d -o $OWL_USER $LOG_DIR

# Executables
sudo install -o $OWL_USER  $SRC_JAR_DIR/$SRC_JAR_FILE $INSTALL_DIR/$DST_JAR_FILE
sudo install -o $OWL_USER $SRC_SCRIPT_DIR/$SRC_CONTROL_SCRIPT $INSTALL_DIR/$DST_CONTROL_SCRIPT

# Init service
sudo install $SRC_SCRIPT_DIR/$SRC_INIT_SCRIPT $INIT_DIR/$DST_INIT_SCRIPT

sudo update-rc.d owl-aggregator defaults

sudo service owl-aggregator restart

echo "Installation complete."
