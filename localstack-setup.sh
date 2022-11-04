#!/bin/bash

#./gradlew buildZip

function setup(){
  echo 'a'
#  until grep -q '^Ready.' /tmp/localstack_infra.log >/dev/null 2>&1 ; do
 #     echo "Waiting for all LocalStack services to be ready"
  #    sleep 7
   # done


    # docker-compose -f ./docker-compose.yml run --rm terraform init -upgrade
#     docker-compose -f ./docker-compose.yml run --rm terraform apply -auto-approve

#    awslocal lambda create-function --function-name CloudwatchEventHandler --zip-file fileb:///tmp/setup/metrics-lambda/build/distributions/metrics-lambda.zip --handler gov.cms.ab2d.metrics.CloudwatchEventHandler --environment="Variables={com.amazonaws.sdk.disableCertChecking=true,IS_LOCALSTACK=true}" --runtime java11 --timeout 900 --role=""
#    awslocal sqs create-queue --queue-name local-events-sqs

#    awslocal lambda create-function --function-name AuditEventHandler --zip-file fileb:///tmp/setup/audit/build/distributions/audit-lambda.zip --handler gov.cms.ab2d.metrics.AuditEventHandler --environment="Variables={com.amazonaws.sdk.disableCertChecking=true,IS_LOCALSTACK=true}" --runtime java11 --timeout 900 --role=""

}

setup &

$@