A simple lambda that converts Cloudwatch events to messages the event service can accept.

## Build

AWS lambdas need to be zipped. The follow command will build the code and zip the resulting jar.

```
gradle buildZip
```

## Run

In the root directory run
./start.sh

This script will

- builds all lambdas with the buildZip task
- init terraform using the terraform docker image
- Sets up a lockstack docker image
- Applies the terraform that facilitates running lambdas

## Stop

In the root directory run
./start.sh

This script will

- destroy localstack infrastructure
- stop the locakstack docker image
- delete terraform lock files

## Deploy

Use this jenkins job to deploy
https://jenkins.ab2d.cms.gov/job/AB2D-Ops/job/Terraform/job/Deploy%20Lambda/

