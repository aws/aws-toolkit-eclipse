#!/bin/sh

## Install third-party dependencies to your local maven repository
mvn -Pinstall-third-party-bundles clean install

## Build development environment to be used as the target platform
mvn -Pbuild-eclipse-devide package
