resource "docker_container" "localstack" {

  name  = "localstack"
  image = docker_image.localstack.latest
  depends_on = [docker_image.localstack]
  ports {
    internal = 443
    external = 443
  }
  ports {
    internal = 4566
    external = 4566
  }
  ports {
    internal = 4571
    external = 4571
  }
}

resource "docker_image" "localstack" {
  name = "localstack/localstack:1.2.0"

}