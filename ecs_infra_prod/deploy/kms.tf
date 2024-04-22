resource "aws_kms_key" "docdbkms" {
  description             = "docdb KMS Key"
  enable_key_rotation     = true
  key_usage               = "ENCRYPT_DECRYPT"
  deletion_window_in_days = 10

}
resource "aws_kms_alias" "docdbkmsalias" {
  name          = "alias/kms-docdb-${lower(terraform.workspace)}"
  target_key_id = aws_kms_key.docdbkms.key_id
}

resource "aws_kms_key" "awsebs" {
  description             = "ebs KMS Key"
  enable_key_rotation     = true
  key_usage               = "ENCRYPT_DECRYPT"
  deletion_window_in_days = 10

}
resource "aws_kms_alias" "awsebskmsalias" {
  name          = "alias/ebs-${lower(terraform.workspace)}"
  target_key_id = aws_kms_key.awsebs.key_id
}

resource "aws_kms_key" "rds" {
  description             = "ebs KMS Key"
  enable_key_rotation     = true
  key_usage               = "ENCRYPT_DECRYPT"
  deletion_window_in_days = 10

}
resource "aws_kms_alias" "rdskmsalias" {
  name          = "alias/rds-${lower(terraform.workspace)}"
  target_key_id = aws_kms_key.rds.key_id
}