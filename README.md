# Owl Platform Aggregator #
Version: 1.0.0<br />
Author: Robert S. Moore

## Description ##
A Java-based sensor sample aggregator for the Owl Platform. Provides a
centralized point for sensors to forward data and for solvers to subscribe to
that data.

## Automatic Installation ##
The provided installation script `install.sh` can be used to automatically
compile and install the aggregator.  It will create a user account named
'owl', create log and binary directories (/var/log/owl and /usr/local/bin/owl)
if they don't exist, and install the init.d startup script
(/etc/init.d/owl-aggregator).  The `sudo` utility is used to copy the files.

    ./install.sh

The default logging configuration can be modified by editing the
log4j-install.xml file provided in `src/main/resources/`.  More information on
Log4J can be found [at the project
homepage](http://logging.apache.org/log4j/1.2/ "Apache Log4J").

## Manual Compilation and Installation ##
The aggregator can be built by hand using [Apache
Maven](http://maven.apache.org "Apache Maven Homepage") or via the provided
install script.  To build the aggregator, invoke the `package` target to
produce the executable JAR file:

    mvn clean package

The output will be in the `target` subdirectory.  You can execute the JAR in
the standard way:

    java -jar target/owl-aggregator-1.0.0-SNAPSHOT-jar-with-dependencies.jar

