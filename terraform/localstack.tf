resource "aws_iam_role" "iam_for_everything" {
  name               = "iam"
  depends_on         = [docker_container.localstack, docker_image.localstack]
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Stmt1572416334166",
      "Action": "*",
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
EOF
}


