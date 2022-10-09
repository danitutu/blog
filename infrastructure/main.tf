terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.16"
    }
  }

  required_version = ">= 1.2.0"
}

provider "aws" {
  region = "eu-central-1"
}

resource "aws_instance" "blog" {
  ami           = "ami-0931770438affc90c"
  instance_type = "t2.micro"
  key_name      = "blog-key-pair"
  security_groups = [
    "default",
    "ssh-access",
    "HTTP80",
    "HTTP8080",
    "HTTPS443"
  ]
  associate_public_ip_address = true

  tags = {
    Name = "blog"
  }
}

data "aws_eip" "lb" {
  id = "eipalloc-03556710c1002f08f"
}

resource "aws_eip_association" "eip_assoc" {
  instance_id   = aws_instance.blog.id
  allocation_id = data.aws_eip.lb.id
}
