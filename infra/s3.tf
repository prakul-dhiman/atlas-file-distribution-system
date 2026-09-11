# Amazon S3 Buckets acting as Distributed Storage Nodes

resource "aws_s3_bucket" "storage_node_1" {
  bucket        = "atlas-storage-node-1-${var.environment}"
  force_destroy = false
}

resource "aws_s3_bucket" "storage_node_2" {
  bucket        = "atlas-storage-node-2-${var.environment}"
  force_destroy = false
}

resource "aws_s3_bucket" "storage_node_3" {
  bucket        = "atlas-storage-node-3-${var.environment}"
  force_destroy = false
}

# Object Versioning for S3 buckets
resource "aws_s3_bucket_versioning" "v1" {
  bucket = aws_s3_bucket.storage_node_1.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_versioning" "v2" {
  bucket = aws_s3_bucket.storage_node_2.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_versioning" "v3" {
  bucket = aws_s3_bucket.storage_node_3.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Lifecycle rules: 30-day auto-purge for trash prefix
resource "aws_s3_bucket_lifecycle_configuration" "trash_purge_1" {
  bucket = aws_s3_bucket.storage_node_1.id

  rule {
    id     = "trash-30day-expiration"
    status = "Enabled"
    filter {
      prefix = "trash/"
    }
    expiration {
      days = 30
    }
  }
}
