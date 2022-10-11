#!/bin/bash

function setup(){
  until grep -q '^Ready.' /tmp/localstack_infra.log >/dev/null 2>&1 ; do
      echo "Waiting for all LocalStack services to be ready"
      sleep 7
    done

    awslocal lambda create-function --function-name CloudwatchEventHandler --zip-file fileb:///tmp/setup/build/distributions/ab2d-metrics.zip --handler gov.cms.ab2d.metrics.CloudwatchEventHandler --environment="Variables={com.amazonaws.sdk.disableCertChecking=true,IS_LOCALSTACK=true}" --runtime java11 --timeout 900 --role=""
    awslocal sqs create-queue --queue-name ab2d-events
}

setup &

$@