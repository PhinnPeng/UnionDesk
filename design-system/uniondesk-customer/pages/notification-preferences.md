# Notification Preferences Override

## Page Intent

- Customer settings page for notification channels, notification categories, and quiet hours.
- This page currently uses a frontend local-storage fallback because the backend preference API is not yet available.

## Required Structure

- Channel switches:
  - 站内信
  - 邮件
- Category checkboxes:
  - 工单进度
  - 系统公告
  - 安全提醒
- Quiet hours:
  - 开始时间
  - 结束时间
- Save / reset actions

## Fallback Rules

- Persist changes locally when backend support is missing.
- Keep a visible empty / fallback state that explains the current implementation boundary.
- Do not assume a server API exists until Epsilon confirms it.

## Visual Rules

- Keep the settings layout calm, friendly, and tool-like.
- Use short Chinese labels and avoid any marketing copy.
