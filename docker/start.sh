#!/usr/bin/env bash

# Setup variables
BASEDIR=$(cd $(dirname $0)/..; pwd)
if [[ "$DOCKER_ENV" == "" ]]; then
    export DOCKER_ENV="default"
fi

echo "Using docker-machine environment '$DOCKER_ENV'"

# Try to initialize docker-machine (if needed)
if [[ "$(docker-machine ls | grep "$DOCKER_ENV")" != "" ]]; then
    if [[ "$(docker-machine status ${DOCKER_ENV})" != "Running" ]]; then
        docker-machine start ${DOCKER_ENV}
    fi
    # Set the environment
    eval "$(docker-machine env ${DOCKER_ENV})"
    # Set the host
    export NEO4J_HOST="$(docker-machine ip ${DOCKER_ENV})"
    # Only start things if nothing is running
    if [[ "$(docker ps | grep "neo4j")" != "" ]]; then
        echo "Neo4j already started."
    else
        # Run the necessary container
        docker run \
            --detach \
            --publish=7474:7474 \
            --volume=${BASEDIR}/tmp/neo4j/data:/data \
            neo4j
    fi
fi

# Show container info
docker ps | grep "neo4j"
echo ""

# Show docker-machine ip
export NEO4J_HOST="$(docker-machine ip ${DOCKER_ENV})"
NEW_URL="http://$NEO4J_HOST:7474"
if [[ "$NEO4J_URL" == "$NEW_URL" ]]; then
    echo "NEO4J_URL is correctly set to $NEW_URL"
else
    echo "Copy the following environment variables into this shell to run:"
    echo ""
    echo "export NEO4J_URL=\"$NEW_URL\""
    export NEO4J_URL="$NEW_URL"
fi
