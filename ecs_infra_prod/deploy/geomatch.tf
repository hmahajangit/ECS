resource "aws_wafv2_web_acl" "waf" {
  name  = "${local.prefix}_geo_match_statement_allow"
  scope = "REGIONAL"

  default_action {
    block {}
  }

  rule {
    name     = "Countries_allowed_rule"
    priority = 1

    action {
      allow {}
    }

    statement {
      geo_match_statement {
        country_codes = [
          //Europe
          "DE", "GB", "SE", "NO", "DK", "FI", "ES", "PT", "CZ", "AT", "FR", "NL", "CH", "IT", "BE", "PL", "GR",
          //Asia
          "SG", "IN", "CN", "AE", "TR", "TH", "ZA", "TW",
          //America
        "US", "CA", "MX", "BR"]

      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.prefix}-rule-metric-name"
      sampled_requests_enabled   = false
    }

  }

  tags = local.common_tags

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${local.prefix}-metric-name"
    sampled_requests_enabled   = false
  }
}

resource "aws_cloudwatch_log_group" "waf" {
  name = "aws-waf-logs-${local.common_tags.Environment}"
}

resource "aws_wafv2_web_acl_logging_configuration" "WafWebAclLogging" {
  log_destination_configs = [aws_cloudwatch_log_group.waf.arn]
  resource_arn            = aws_wafv2_web_acl.waf.arn
}

resource "aws_cloudwatch_log_resource_policy" "waf" {
  policy_document = data.aws_iam_policy_document.waf.json
  policy_name     = "${local.prefix}-webacl-policy"
}

data "aws_iam_policy_document" "waf" {
  version = "2012-10-17"
  statement {
    effect = "Allow"
    principals {
      identifiers = ["delivery.logs.amazonaws.com"]
      type        = "Service"
    }
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.waf.arn}:*"]
    condition {
      test     = "ArnLike"
      values   = ["arn:aws:logs:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:*"]
      variable = "aws:SourceArn"
    }
    condition {
      test     = "StringEquals"
      values   = [tostring(data.aws_caller_identity.current.account_id)]
      variable = "aws:SourceAccount"
    }
  }
}

resource "aws_cloudwatch_log_metric_filter" "metric_filter" {
  name           = "waf-service"
  pattern        = "ERROR"
  log_group_name = "aws-waf-logs-${local.common_tags.Environment}"

  metric_transformation {
    name      = "EventCount"
    namespace = "custommetrics"
    value     = "1"
  }
}