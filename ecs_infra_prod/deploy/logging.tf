## This tf file is responsible for creating s3 bucket which is dedicated to capture logging 
## for all other s3 buckets.
## Also, this is creating one bucket for storing jks certs

resource "aws_s3_bucket" "logging" {
  bucket = "${local.prefix}-logging"

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-logging" },
  )
}


resource "aws_s3_bucket_versioning" "logging" {
  bucket = aws_s3_bucket.logging.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_ownership_controls" "logging" {
  bucket = aws_s3_bucket.logging.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "logging_bucket" {
  depends_on = [aws_s3_bucket_ownership_controls.logging]

  bucket = aws_s3_bucket.logging.id
  acl    = "private"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "logging_bucket" {
  bucket = aws_s3_bucket.logging.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_accelerate_configuration" "logging_bucket" {
  bucket = aws_s3_bucket.logging.id
  status = "Enabled"
}

resource "aws_s3_bucket_lifecycle_configuration" "logging_bucket" {
  bucket = aws_s3_bucket.logging.id

  rule {
    id     = "logging-expiration"
    status = "Enabled"

    transition {
      days          = var.infrequent_transition_days
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = var.glacier_transition_days
      storage_class = "GLACIER"
    }

    expiration {
      days = var.expiration_days
    }
  }
}

resource "aws_s3_object" "s3_logging" {
  bucket = aws_s3_bucket.logging.id
  key    = "s3-logging/"
  source = "/dev/null" # You can use an empty file or any other source you prefer
}

resource "aws_s3_bucket_logging" "logging" {
  bucket = aws_s3_bucket.logging.id

  target_bucket = aws_s3_bucket.logging.id
  target_prefix = "s3-logging/logs/logging/"
}

resource "aws_s3_bucket" "keystore-jks" {
  bucket = "${local.prefix}-keystore-jks"

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-keystore-jks" },
  )
}


resource "aws_s3_bucket_versioning" "keystore-jks" {
  bucket = aws_s3_bucket.keystore-jks.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_ownership_controls" "keystore-jks" {
  bucket = aws_s3_bucket.keystore-jks.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "keystore-jks-bucket" {
  depends_on = [aws_s3_bucket_ownership_controls.keystore-jks]

  bucket = aws_s3_bucket.keystore-jks.id
  acl    = "private"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "keystore-jks-bucket" {
  bucket = aws_s3_bucket.keystore-jks.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_accelerate_configuration" "keystore-jks-bucket" {
  bucket = aws_s3_bucket.keystore-jks.id
  status = "Enabled"
}

resource "aws_s3_bucket_lifecycle_configuration" "keystore-jks-bucket" {
  bucket = aws_s3_bucket.keystore-jks.id

  rule {
    id     = "keystore-jks-expiration"
    status = "Enabled"

    transition {
      days          = var.infrequent_transition_days
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = var.glacier_transition_days
      storage_class = "GLACIER"
    }

    expiration {
      days = var.expiration_days
    }
  }
}

resource "aws_s3_bucket_logging" "keystore-jks" {
  bucket = aws_s3_bucket.keystore-jks.id

  target_bucket = aws_s3_bucket.logging.id
  target_prefix = "s3-logging/logs/keystore-jks/"
}

resource "aws_s3_object" "object" {
  bucket = aws_s3_bucket.keystore-jks.id
  key    = "${lower(terraform.workspace)}/"
  source = "/dev/null" # You can use an empty file or any other source you prefer
}

output "logging_s3_bucket_id" {
  value = aws_s3_bucket.logging.id
}
output "logging_s3_bucket_arn" {
  value = aws_s3_bucket.logging.arn
}
output "keystore_jks_s3_bucket_id" {
  value = aws_s3_bucket.keystore-jks.id
}
output "keystore_jks_s3_bucket_arn" {
  value = aws_s3_bucket.keystore-jks.arn
}