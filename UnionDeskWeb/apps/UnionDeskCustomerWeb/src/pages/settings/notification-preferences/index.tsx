import { BellOutlined, ClockCircleOutlined, MailOutlined, ReloadOutlined, SaveOutlined, SafetyOutlined } from "@ant-design/icons";
import { Alert, Button, Card, Checkbox, Col, Form, Input, Row, Space, Switch, Tag, Typography, message } from "antd";
import { useEffect } from "react";

const STORAGE_KEY = "uniondesk.customer.notification-preferences";

type NotificationPreferenceFormValues = {
  inAppEnabled: boolean;
  emailEnabled: boolean;
  categories: string[];
  quietStart: string;
  quietEnd: string;
};

const defaultValues: NotificationPreferenceFormValues = {
  inAppEnabled: true,
  emailEnabled: true,
  categories: ["ticket_progress", "system_announcement", "security_alert"],
  quietStart: "22:00",
  quietEnd: "08:00"
};

function loadPreferences(): NotificationPreferenceFormValues {
  if (typeof window === "undefined") {
    return defaultValues;
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return defaultValues;
    }
    const parsed = JSON.parse(raw) as Partial<NotificationPreferenceFormValues>;
    return {
      ...defaultValues,
      ...parsed,
      categories: Array.isArray(parsed.categories) ? parsed.categories : defaultValues.categories
    };
  } catch {
    return defaultValues;
  }
}

export default function NotificationPreferencesPage() {
  const [form] = Form.useForm<NotificationPreferenceFormValues>();

  useEffect(() => {
    form.setFieldsValue(loadPreferences());
  }, [form]);

  const handleSave = (values: NotificationPreferenceFormValues) => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(values));
    message.success("通知偏好已保存到本机");
  };

  const handleReset = () => {
    form.setFieldsValue(defaultValues);
    window.localStorage.removeItem(STORAGE_KEY);
    message.success("已恢复默认通知偏好");
  };

  return (
    <div className="customer-page customer-settings">
      <section className="customer-page__hero">
          <Space wrap size={8}>
            <Tag color="cyan">通知偏好</Tag>
            <Tag color="blue">本机暂存</Tag>
            <Tag color="green">偏好中心</Tag>
          </Space>
        <Typography.Title level={2} className="customer-page__hero-title">
          通知偏好设置
        </Typography.Title>
        <Typography.Paragraph className="customer-page__hero-copy">
          管理站内信、邮件与免打扰时段，让工单进度、系统公告和安全提醒按你的习惯送达。
        </Typography.Paragraph>
        <Alert
          type="info"
          showIcon
          message="当前为前端暂存版"
          description="后端通知偏好接口尚未接入时，保存内容会先写入浏览器本地；接口就绪后可直接切换为真实保存。"
        />
      </section>

      <Form<NotificationPreferenceFormValues>
        form={form}
        layout="vertical"
        onFinish={handleSave}
        initialValues={defaultValues}
      >
        <div className="customer-page__grid customer-page__grid--2">
          <Card bordered={false} className="customer-page__surface" title="通知渠道" extra={<BellOutlined />}>
            <Space direction="vertical" size={12} style={{ width: "100%" }}>
              <Form.Item
                name="inAppEnabled"
                valuePropName="checked"
                label="站内信通知"
                tooltip="工单进度、系统公告和安全提醒将优先送达站内信"
              >
                <Switch aria-label="站内信通知" />
              </Form.Item>
              <Form.Item
                name="emailEnabled"
                valuePropName="checked"
                label="邮件通知"
                tooltip="适合补充接收不在站内时的关键消息"
              >
                <Switch aria-label="邮件通知" />
              </Form.Item>
            </Space>
          </Card>

          <Card bordered={false} className="customer-page__surface" title="通知类型" extra={<SafetyOutlined />}>
            <Form.Item
              name="categories"
              label="选择希望接收的通知类型"
              rules={[{ required: true, message: "请至少选择一种通知类型" }]}
            >
              <Checkbox.Group
                style={{ width: "100%" }}
                options={[
                  { label: "工单进度", value: "ticket_progress" },
                  { label: "系统公告", value: "system_announcement" },
                  { label: "安全提醒", value: "security_alert" }
                ]}
              />
            </Form.Item>
          </Card>
        </div>

        <Card bordered={false} className="customer-page__surface" title="免打扰时段" extra={<ClockCircleOutlined />}>
          <Row gutter={[16, 16]}>
            <Col xs={24} md={12}>
              <Form.Item name="quietStart" label="开始时间" rules={[{ required: true, message: "请选择免打扰开始时间" }]}>
                <Input type="time" aria-label="免打扰开始时间" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="quietEnd" label="结束时间" rules={[{ required: true, message: "请选择免打扰结束时间" }]}>
                <Input type="time" aria-label="免打扰结束时间" />
              </Form.Item>
            </Col>
          </Row>
          <Typography.Text className="customer-page__muted">
            免打扰时间段内仍保留站内信列表，只是不主动打断你。
          </Typography.Text>
        </Card>

        <div className="customer-page__toolbar">
          <Typography.Text className="customer-page__muted">保存后会自动记住你的偏好，下次登录仍然生效。</Typography.Text>
          <Space wrap>
            <Button icon={<ReloadOutlined aria-hidden />} onClick={handleReset}>
              恢复默认
            </Button>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined aria-hidden />}>
              保存偏好
            </Button>
          </Space>
        </div>
      </Form>
    </div>
  );
}
