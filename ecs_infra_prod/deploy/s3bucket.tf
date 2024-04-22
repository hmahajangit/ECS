resource "aws_s3_bucket" "appdata" {
  bucket = "${local.prefix}-appdata1"

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-appdata" },
  )
}


resource "aws_s3_bucket_versioning" "appdata" {
  bucket = aws_s3_bucket.appdata.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_ownership_controls" "appdata" {
  bucket = aws_s3_bucket.appdata.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "appdata_bucket" {
  depends_on = [aws_s3_bucket_ownership_controls.appdata]

  bucket = aws_s3_bucket.appdata.id
  acl    = "private"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "appdata_bucket" {
  bucket = aws_s3_bucket.appdata.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_accelerate_configuration" "appdata_bucket" {
  bucket = aws_s3_bucket.appdata.id
  status = "Enabled"
}

resource "aws_s3_bucket_lifecycle_configuration" "appdata_bucket" {
  bucket = aws_s3_bucket.appdata.id

  rule {
    id     = "appdata-expiration"
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

resource "aws_s3_bucket_logging" "appdata" {
  bucket = aws_s3_bucket.appdata.id

  target_bucket = aws_s3_bucket.logging.id
  target_prefix = "s3-logging/logs/appdata/"
}

resource "aws_s3_bucket" "master-appdata" {
  bucket = "${local.prefix}-master-appdata"

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-master-appdata" },
  )
}

resource "aws_s3_bucket_versioning" "master_appdata" {
  bucket = aws_s3_bucket.master-appdata.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_ownership_controls" "master_appdata" {
  bucket = aws_s3_bucket.master-appdata.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "master_appdata" {
  depends_on = [aws_s3_bucket_ownership_controls.master_appdata]

  bucket = aws_s3_bucket.master-appdata.id
  acl    = "private"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "master_appdata_bucket" {
  bucket = aws_s3_bucket.master-appdata.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_accelerate_configuration" "master_appdata_bucket" {
  bucket = aws_s3_bucket.master-appdata.id
  status = "Enabled"
}

resource "aws_s3_bucket_lifecycle_configuration" "master_appdata_bucket" {
  bucket = aws_s3_bucket.master-appdata.id

  rule {
    id     = "master-appdata-expiration"
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

resource "aws_s3_bucket_logging" "master-appdata" {
  bucket = aws_s3_bucket.master-appdata.id

  target_bucket = aws_s3_bucket.logging.id
  target_prefix = "s3-logging/logs/master-appdata/"
}



output "appdata_s3_bucket_id" {
  value = aws_s3_bucket.appdata.id
}
output "appdata_s3_bucket_arn" {
  value = aws_s3_bucket.appdata.arn
}
output "master_appdata_s3_bucket_id" {
  value = aws_s3_bucket.master-appdata.id
}
output "master_appdata_s3_bucket_arn" {
  value = aws_s3_bucket.master-appdata.arn
}
