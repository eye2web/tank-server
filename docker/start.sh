#!/bin/sh

set -e

exec java -server ${JAVA_OPTS}  -jar /app/service.jar
