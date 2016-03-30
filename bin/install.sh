#!/usr/bin/env bash

# Verify that docker-machine is installed
hash docker-machine &> /dev/null || {
    echo "docker-machine is required. Use the link below to download Docker Toolbox."
    echo "https://www.docker.com/docker-toolbox"
    exit 1
}

OPTS=$@

DEFAULT_DOCKER_DRIVER="virtualbox"
DEFAULT_DOCKER_ENV="default"
DEFAULT_NEO4J_VERSION="latest"

function printUsage() {
    echo "Usage: $0 [-dnv | --driver DOCKER_DRIVER --env DOCKER_ENV --version NEO4J_VERSION]"
    echo ""
    echo "    -d, --driver (default: \$DOCKER_DRIVER ($DOCKER_DRIVER) or '$DEFAULT_DOCKER_DRIVER'):"
    echo "        The docker-machine driver to use to create the docker-machine environment"
    echo "    -n, --env (default: \$DOCKER_ENV ($DOCKER_ENV) or '$DEFAULT_DOCKER_DRIVER'):"
    echo "        The docker-machine environment name to create or reuse"
    echo "    -v, --version (default: \$DOCKER_NEO4J_VERSION ($DOCKER_NEO4J_VERSION) or '$DEFAULT_NEO4J_VERSION'):"
    echo "        The version of the neo4j image to pull"
    echo ""
}

while [[ $# > 0 ]]
do
key="$1"

case $key in

    -d|--driver)
        if [[ "$2" != "" ]]; then
            DOCKER_DRIVER="$2"
        fi
        shift
    ;;

    -n|--env)
        if [[ "$2" != "" ]]; then
            DOCKER_ENV="$2"
        fi
        shift
    ;;

    -v|--version)
        if [[ "$2" != "" ]]; then
            NEO4J_VERSION="$2"
        fi
        shift
    ;;

    *)
        echo "Unknown option: $OPTS"
        echo ""
        printUsage
        exit 1
    ;;

esac
shift
done

# Set the defaults
if [[ "$DOCKER_DRIVER" == "" ]]; then
    DOCKER_DRIVER=${DEFAULT_DOCKER_DRIVER}
fi
if [[ "$DOCKER_ENV" == "" ]]; then
    DOCKER_ENV=${DEFAULT_DOCKER_ENV}
fi
if [[ "$NEO4J_VERSION" == "" ]]; then
    NEO4J_VERSION=${DEFAULT_NEO4J_VERSION}
fi

# Log the operations
echo "Use docker-machine environment '$DOCKER_ENV' to install Neo4j $NEO4J_VERSION."

# Create the docker machine environment if necessary
if [[ "$(docker-machine ls | grep ${DOCKER_ENV})" == "" ]]; then
    docker-machine create -d ${DOCKER_DRIVER} ${DOCKER_ENV}
fi

# Start machine if it isn't running
if [[ "$(docker-machine status ${DOCKER_ENV})" != "Running" ]]; then
    docker-machine start ${DOCKER_ENV}
fi

# Load the required environment variables
eval "$(docker-machine env ${DOCKER_ENV})"

# Download the docker image for Neo4j
if [[ "$(docker images | grep neo4j | grep ${NEO4J_VERSION})" ]]; then
    echo "docker-machine env '$DOCKER_ENV' already has the neo4j $NEO4J_VERSION image available."
else
    docker pull neo4j:${NEO4J_VERSION}
fi
echo "Run docker/start.sh to start this Neo4j image."
