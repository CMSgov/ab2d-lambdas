resource "aws_lambda_function" "metrics" {
  depends_on       = [aws_iam_role.iam_for_everything]
  filename         = "/tmp/setup/metrics-lambda/build/distributions/metrics-lambda.zip"
  function_name    = "CloudwatchEventHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.metrics.CloudwatchEventHandler"
  source_code_hash = filebase64sha256("/tmp/setup/metrics-lambda/build/distributions/metrics-lambda.zip")
  runtime          = "java11"
  environment {
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
    }
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
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
      environment                             = "local"
      JAVA_TOOL_OPTIONS                       = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      AB2D_EFS_MOUNT                          = "/tmp/jobdownloads/"
      audit_files_ttl_hours                   = 1
    }
  }
  tags = {
    "key" = "lam"
  }
}

resource "aws_lambda_function" "coverage_count" {
  depends_on       = [
    aws_iam_role.iam_for_everything, aws_lambda_function.database_management,
    data.aws_lambda_invocation.update_database_schema
  ]
  filename         = "/tmp/setup/coverage-counts/build/distributions/coverage-count.zip"
  function_name    = "CoverageCountsHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.coveragecounts.CoverageCountsHandler"
  source_code_hash = filebase64sha256("/tmp/setup/coverage-counts/build/distributions/coverage-count.zip")
  runtime          = "java11"
  environment {
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
      environment                             = "local"
      JAVA_TOOL_OPTIONS                       = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      DB_URL                                  = "jdbc:postgresql://host.docker.internal:5432/ab2d"
      DB_USERNAME                             = "ab2d"
      DB_PASSWORD                             = "ab2d"
    }
  }
  tags = {
    "key" = "lam"
  }
}

resource "aws_sns_topic" "coverage_count_sns" {
  name = "local-coverage-count"
}

resource "aws_sns_topic_subscription" "user_updates_lampda_target" {
  topic_arn = aws_sns_topic.coverage_count_sns.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.coverage_count.arn
}

resource "aws_lambda_function" "database_management" {
  depends_on       = [aws_iam_role.iam_for_everything]
  filename         = "/tmp/setup/database-management/build/distributions/database-management.zip"
  function_name    = "DatabaseManagementHandler"
  role             = aws_iam_role.iam_for_everything.arn
  handler          = "gov.cms.ab2d.databasemanagement.DatabaseManagementHandler"
  source_code_hash = filebase64sha256("/tmp/setup/database-management/build/distributions/database-management.zip")
  runtime          = "java11"
  environment {
    variables = {
      "com.amazonaws.sdk.disableCertChecking" = true
      IS_LOCALSTACK                           = true
      environment                             = "local"
      JAVA_TOOL_OPTIONS                       = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      DB_URL                                  = "jdbc:postgresql://host.docker.internal:5432/ab2d"
      DB_USERNAME                             = "ab2d"
      DB_PASSWORD                             = "ab2d"
      LIQUIBASE_DUPLICATE_FILE_MODE           = "WARN"
      liquibaseSchemaName                     = "lambda"
    }
  }
  tags = {
    "key" = "lam"
  }
}

data aws_lambda_invocation "update_database_schema" {
  depends_on    = [aws_lambda_function.database_management]
  function_name = aws_lambda_function.database_management.function_name
  input         = <<JSON
  {
  }
  JSON
}

output "result_entry" {
  value = jsondecode(data.aws_lambda_invocation.update_database_schema.result)
}
