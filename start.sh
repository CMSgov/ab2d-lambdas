#!/bin/bash
timeoutPortable() {

    expect
    mac=$?

    if [ $mac -eq 127 ] ; then
      echo "looks like you're using linux"
      timeout $1 bash -c -- "$2"
    else
      #atempt at making this work on mac but don't have one to test on
      time=$1
      command="/bin/sh -c \"$2\""
      echo "looks like you're using a mac"
      expect -c "set echo \"-noecho\"; set timeout $time; spawn -noecho $command; expect timeout { exit 1 } eof { exit 0 }"
      if [ $? = 1 ] ; then
          echo "Timeout after ${time} seconds"
      fi
    fi

}

export TF_LOG="TRACE"

if [ "$1" = "clean" ]; then
  ./gradlew clean
fi

# run docker-compose in detach mode to prevent flooding the console. If you have problems check a specific container's logs with docker logs --follow <container>
docker-compose up db localstack -d &
timeoutPortable 30 "while docker container inspect -f '{{.State.Running}}' postgres localstack | sed -e '1h;2,\$H;\$!d;g' -e '/^true\ntrue/p;q' >> /dev/null ; do sleep  5 && echo "checking if docker containers are up"; done"

./gradlew clean buildZip

if [ ! -f ./terraform/terraform.tfstate ]; then
  docker-compose -f ./docker-compose.yml run --rm terraform init -upgrade
fi

docker-compose -f ./docker-compose.yml run --rm terraform apply --auto-approve -var branch_name="${name}"
