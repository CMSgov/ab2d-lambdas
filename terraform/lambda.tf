

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
  handler          = "gov.cms.ab2d.audit.AuditEventHandler"
  source_code_hash = filebase64sha256("/tmp/setup/audit/build/distributions/audit-lambda.zip")
  runtime          = "java11"
  environment {
    variables = { "com.amazonaws.sdk.disableCertChecking" = true, IS_LOCALSTACK = true }
  }
  tags = {
    "key" = "lam"
  }
}
