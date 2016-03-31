#!/usr/bin/env bash

# Setup variables
if [[ "$DOCKER_ENV" == "" ]]; then
    export DOCKER_ENV="default"
fi

echo "Stopping docker-machine '$DOCKER_ENV'"
docker-machine stop ${DOCKER_ENV}
