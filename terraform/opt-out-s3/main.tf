# Temporary S3 bucket for opt-out

data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
}

resource "aws_s3_bucket" "opt_out" {
  bucket = "${local.account_id}-ab2d-opt-out-temp"
  tags = {
    business    = "oeda"
    application = "ab2d"
    Environment = "dev"
  }
}

resource "aws_s3_bucket_versioning" "opt_out" {
  bucket = aws_s3_bucket.opt_out.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_logging" "opt_out" {
  bucket        = aws_s3_bucket.opt_out.id
  target_bucket = "cms-cloud-${local.account_id}-us-east-1"
  target_prefix = "AWSLogs/${local.account_id}/s3/${local.account_id}-ab2d-opt-out-temp-access-logs/"
}

resource "aws_s3_bucket_policy" "opt_out" {
  bucket = aws_s3_bucket.opt_out.id
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Sid" : "AllowSSLRequestsOnly",
        "Effect" : "Deny",
        "Principal" : "*",
        "Action" : "s3:*",
        "Resource" : [
          "arn:aws:s3:::${local.account_id}-ab2d-opt-out-temp",
          "arn:aws:s3:::${local.account_id}-ab2d-opt-out-temp/*"
        ],
        "Condition" : {
          "Bool" : {
            "aws:SecureTransport" : "false"
          }
        }
      }
    ]
  })
}
