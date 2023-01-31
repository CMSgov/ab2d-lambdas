Receives coverage counts from other services through SNS messages. 

## Build

AWS lambdas need to be zipped. The follow command will build the code and zip the resulting jar.
```
gradle buildZip
```

## Deploy

https://jenkins.ab2d.cms.gov/job/AB2D-Ops/job/Terraform/job/Deploy%20Lambda/