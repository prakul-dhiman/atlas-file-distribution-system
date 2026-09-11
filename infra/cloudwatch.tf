# CloudWatch Monitoring, Alarms, and Safety Billing Alarm

resource "aws_cloudwatch_log_group" "atlas_logs" {
  name              = "/ecs/atlas-backend"
  retention_in_days = 30
}

# Safety Net: Billing Alarm triggers if estimated charges exceed $50
resource "aws_cloudwatch_metric_alarm" "billing_alarm" {
  alarm_name          = "atlas-monthly-billing-alarm"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "EstimatedCharges"
  namespace           = "AWS/Billing"
  period              = "21600" # 6 hours
  statistic           = "Maximum"
  threshold           = "50"
  alarm_description   = "Billing alarm to prevent unexpected AWS cost overruns (> $50/month)"

  dimensions = {
    Currency = "USD"
  }
}
