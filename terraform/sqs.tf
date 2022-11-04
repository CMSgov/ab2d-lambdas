
resource "aws_sqs_queue" "local-events-sqs" {
  depends_on = [aws_iam_role.iam_for_everything, docker_container.localstack]
  name       = "local-events-sqs"
}