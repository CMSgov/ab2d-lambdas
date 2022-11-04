

resource "aws_lambda_function" "metrics" {
  depends_on       = [aws_iam_role.iam_for_everything]
  filename         = "/tmp/setup/metrics-lambda/build/distributions/metrics-lambda.zip"
  function_name    = "CloudwatchEventHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.metrics.CloudwatchEventHandler"
  source_code_hash = filebase64sha256("/tmp/setup/metrics-lambda/build/distributions/metrics-lambda.zip")
  runtime          = "java11"
  environment {
    variables = { "com.amazonaws.sdk.disableCertChecking" = true, IS_LOCALSTACK = true }
  }
  tags = {
    "key" = "lam"
  }
}


resource "aws_lambda_function" "audit" {
  depends_on       = [aws_iam_role.iam_for_everything]
  filename         = "/tmp/setup/audit/build/distributions/audit-lambda.zip"
  function_name    = "AuditEventHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.metrics.AuditEventHandler"
  source_code_hash = filebase64sha256("/tmp/setup/audit/build/distributions/audit-lambda.zip")
  runtime          = "java11"
  environment {
    variables = { "com.amazonaws.sdk.disableCertChecking" = true, IS_LOCALSTACK = true }
  }
  tags = {
    "key" = "lam"
  }
}

#    awslocal lambda create-function --function-name CloudwatchEventHandler --zip-file fileb:///tmp/setup/metrics-lambda/build/distributions/metrics-lambda.zip --handler gov.cms.ab2d.metrics.CloudwatchEventHandler --environment="Variables={com.amazonaws.sdk.disableCertChecking=true,IS_LOCALSTACK=true}" --runtime java11 --timeout 900 --role=""
#    awslocal sqs create-queue --queue-name local-events-sqs

#    awslocal lambda create-function --function-name AuditEventHandler --zip-file fileb:///tmp/setup/audit/build/distributions/audit-lambda.zip --handler gov.cms.ab2d.metrics.AuditEventHandler --environment="Variables={com.amazonaws.sdk.disableCertChecking=true,IS_LOCALSTACK=true}" --runtime java11 --timeout 900 --role=""

