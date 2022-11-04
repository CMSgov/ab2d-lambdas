#!/bin/bash

docker-compose -f ./docker-compose.yml run --rm terraform destroy  --auto-approve
