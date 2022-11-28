resource "aws_cloudwatch_event_rule" "profile_generator_lambda_event_rule" {
  depends_on = [docker_image.localstack]
  name = "profile-generator-lambda-event-rule"
  description = "retry scheduled every 2 min"
  schedule_expression = "rate(2 minutes)"
}

resource "aws_cloudwatch_event_target" "profile_generator_lambda_target" {
  depends_on = [docker_image.localstack]
  arn = aws_lambda_function.audit.arn
  rule = aws_cloudwatch_event_rule.profile_generator_lambda_event_rule.name
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_rw_fallout_retry_step_deletion_lambda" {
  depends_on = [docker_image.localstack]
  statement_id = "AllowExecutionFromCloudWatch"
  action = "lambda:InvokeFunction"
  function_name = aws_lambda_function.audit.arn
  principal = "events.amazonaws.com"
  source_arn = aws_cloudwatch_event_rule.profile_generator_lambda_event_rule.arn
}