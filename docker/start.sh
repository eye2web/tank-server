#!/bin/sh

# Make sure the script exits if a command returns non-zero exit code
set -e

findDockerExposedPort() {
  export DOCKER_CONTAINER_ID=$(cat /proc/self/cgroup  | grep "cpu:/" | sed 's/\([0-9]\):cpu:.*\/docker\///g')
  export NETWORK_BINDING=$(curl --retry 5 --connect-timeout 3 -X GET "http://${DOCKER_HOST_IP}:5555/container/${DOCKER_CONTAINER_ID}/portbinding?port=8080&protocol=tcp")
  export DOCKER_EXPOSED_PORT=$(echo ${NETWORK_BINDING} | jq -c '.[0].HostPort' | sed -e 's/^"//'  -e 's/"$//')
  if [ -z ${DOCKER_EXPOSED_PORT} ]; then echo DOCKER_EXPOSED_PORT not set, port not found. && exit 1; fi
}

if echo ${PROFILE} | grep -iqF aws; then
  echo AWS profile enabled

  export DOCKER_HOST_IP=$(curl --retry 5 --connect-timeout 3 -s 169.254.169.254/latest/meta-data/local-ipv4)
  findDockerExposedPort
fi

if echo ${PROFILE} | grep -iqF docker-dev; then
  echo Docker dev profile enabled
  if [ -z ${DOCKER_HOST_IP} ]; then echo DOCKER_HOST_IP variable is not set. && exit 1; fi

  findDockerExposedPort
fi

if echo ${PROFILE} | grep -iqF hipaa; then
  echo Docker hipaa-dev profile enabled
  source /app/retrieve_keystore.sh
fi


exec java -server ${JAVA_OPTS} -Dspring.profiles.active=${PROFILE} -jar /app/service.jar
