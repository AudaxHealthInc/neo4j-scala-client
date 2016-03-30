#!/usr/bin/env bash

# Setup variables
if [[ "$DOCKER_ENV" == "" ]]; then
    echo "Using default docker-machine environment 'default'"
    export DOCKER_ENV="default"
else
    echo "Using custom docker-machine environment '$DOCKER_ENV'"
fi

# Try to initialize docker-machine (if needed)
if [[ "$(docker-machine ls | grep "$DOCKER_ENV")" != "" ]]; then
    if [[ "$(docker-machine status ${DOCKER_ENV})" == "Running" ]]; then
        # Set the environment
        eval "$(docker-machine env ${DOCKER_ENV})"
        # Only stop if running
        DPID="$(docker ps | grep neo4j | sed 's/\([^ ]*\).*$/\1/')"
        if [[ "$DPID" != "" ]]; then
            echo "Killing neo4j container..."
            docker kill ${DPID}
        else
            echo "neo4j docker container already stopped"
        fi
    else
        echo "docker-machine $DOCKER_ENV already stopped"
    fi
fi

