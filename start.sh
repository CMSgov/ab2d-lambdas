#!/bin/bash

export TF_LOG="TRACE"

./gradlew clean buildZip

docker-compose -f ./docker-compose.yml run --rm terraform init -upgrade
docker-compose -f ./docker-compose.yml run --rm terraform apply --auto-approve