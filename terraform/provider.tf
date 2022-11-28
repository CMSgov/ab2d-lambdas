terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "2.23.0"
    }
    aws = {
      source = "hashicorp/aws"
      version = "4.38.0"
    }
  }
}


provider "docker" {
}

provider "aws" {
  access_key                  = "mock_access_key"
  region                      = "us-east-1"
  s3_force_path_style         = true
  secret_key                  = "mock_secret_key"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    sqs    = "http://host.docker.internal:4566"
    lambda = "http://host.docker.internal:4566"
    iam    = "http://host.docker.internal:4566"
    kinesis = "http://host.docker.internal:4566"
    cloudwatchevents = "http://host.docker.internal:4566"
  }
}