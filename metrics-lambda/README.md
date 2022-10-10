A simple lambda that converts Cloudwatch events to messages the event service can accept. 

## Build

AWS lambdas need to be zipped. The follow command will build the code and zip the resulting jar.
```
gradle buildZip
```

## Deploy

For the time being this process is manual. Log into AWS, find the metric lambda, and upload the zip from the build process. 