import { PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import { Button, Card, Col, Form, Input, List, Row, Select, Space, Statistic, Tag, Typography, message } from "antd";
import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCustomerPortal } from "@uniondesk/shared";

import TicketStatusTag from "../../components/TicketStatusTag";
import { formatDateTime } from "../../utils/date";

type TicketFormValues = {
  typeId: string;
  title: string;
  description: string;
};

export default function HomePage() {
  const portal = useCustomerPortal();
  const navigate = useNavigate();
  const [form] = Form.useForm<TicketFormValues>();
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const unreadPreview = useMemo(() => portal.inboxMessages.slice(0, 4), [portal.inboxMessages]);

  if (!portal.activeDomain) {
    return (
      <Card bordered={false} style={{ borderRadius: 24 }}>
        <Space direction="vertical" size={12}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            请先选择业务域
          </Typography.Title>
          <Typography.Text type="secondary">进入工作台前，需要先确定当前客户业务域。</Typography.Text>
          <Button type="primary" onClick={() => navigate("/domains")}>
            去选择业务域
          </Button>
        </Space>
      </Card>
    );
  }
  const activeDomain = portal.activeDomain;

  const handleSubmit = async (values: TicketFormValues) => {
    setSubmitting(true);
    try {
      const attachmentIds: number[] = [];
      for (const file of pendingFiles) {
        const snapshot = await portal.uploadAttachment(file, activeDomain.id);
        const latestAttachment = snapshot.attachments[0];
        if (latestAttachment) {
          attachmentIds.push(latestAttachment.id);
        }
      }
      const next = portal.createTicket({
        typeId: values.typeId,
        title: values.title.trim(),
        description: values.description.trim(),
        attachmentIds
      });
      message.success("工单已提交");
      form.resetFields();
      setPendingFiles([]);
      const createdTicket = next.currentDomainTickets[0];
      if (createdTicket) {
        navigate(`/tickets/${createdTicket.id}`);
      }
    } catch (error) {
      message.error(error instanceof Error ? error.message : "提交失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Space direction="vertical" size={20} style={{ width: "100%" }}>
      <Card bordered={false} style={{ borderRadius: 28 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} lg={14}>
            <Space direction="vertical" size={10} style={{ width: "100%" }}>
              <Tag color="blue" style={{ width: "fit-content" }}>
                当前业务域
              </Tag>
              <Typography.Title level={2} style={{ margin: 0 }}>
                {activeDomain.name}
              </Typography.Title>
              <Typography.Text type="secondary">{activeDomain.description}</Typography.Text>
            </Space>
          </Col>
          <Col xs={24} lg={10}>
            <Row gutter={12}>
              <Col span={12}>
                <Statistic title="待处理工单" value={portal.currentDomainTickets.filter((ticket) => ticket.status === "open").length} />
              </Col>
              <Col span={12}>
                <Statistic title="未读通知" value={portal.unreadCount} />
              </Col>
            </Row>
          </Col>
        </Row>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={14}>
          <Card
            title="提交工单"
            bordered={false}
            extra={
              <Button icon={<ReloadOutlined />} onClick={() => form.resetFields()}>
                重置
              </Button>
            }
            style={{ borderRadius: 24 }}
          >
            <Form<TicketFormValues> form={form} layout="vertical" onFinish={handleSubmit}>
              <Form.Item label="工单类型" name="typeId" rules={[{ required: true, message: "请选择工单类型" }]}>
                <Select
                  placeholder="请选择工单类型"
                  options={portal.ticketTypes.map((type) => ({
                    value: type.id,
                    label: type.name
                  }))}
                  onChange={(typeId: string) => {
                    const selected = portal.ticketTypes.find((type) => type.id === typeId);
                    const currentDescription = form.getFieldValue("description");
                    if (
                      selected?.has_description_slot === true
                      && selected?.description_template_md
                      && !String(currentDescription ?? "").trim()
                    ) {
                      form.setFieldsValue({ description: selected.description_template_md });
                    }
                  }}
                />
              </Form.Item>
              <Form.Item label="标题" name="title" rules={[{ required: true, message: "请输入标题" }]}>
                <Input placeholder="简要描述你的问题" maxLength={80} />
              </Form.Item>
              <Form.Item label="详细说明" name="description" rules={[{ required: true, message: "请输入详细说明" }]}>
                <Input.TextArea rows={5} placeholder="请尽量写清楚业务背景、问题现象和期望结果" />
              </Form.Item>
              <Form.Item label="附件">
                <Input
                  type="file"
                  multiple
                  onChange={(event) => {
                    setPendingFiles(Array.from(event.target.files ?? []));
                  }}
                />
                <Typography.Text type="secondary">当前已选择 {pendingFiles.length} 个附件。</Typography.Text>
              </Form.Item>
              <Button type="primary" icon={<PlusOutlined />} htmlType="submit" loading={submitting}>
                提交工单
              </Button>
            </Form>
          </Card>
        </Col>

        <Col xs={24} xl={10}>
          <Card title="我的工单" bordered={false} style={{ borderRadius: 24, marginBottom: 16 }}>
            <List
              dataSource={portal.currentDomainTickets}
              locale={{ emptyText: "当前业务域暂无工单" }}
              renderItem={(ticket) => (
                <List.Item
                  actions={[
                    <Link key="detail" to={`/tickets/${ticket.id}`}>
                      查看
                    </Link>,
                    ticket.status === "open" ? (
                      <Button
                        key="withdraw"
                        type="link"
                        onClick={() => {
                          try {
                            portal.withdrawTicket(ticket.id);
                            message.success("工单已撤回");
                          } catch (error) {
                            message.error(error instanceof Error ? error.message : "撤回失败");
                          }
                        }}
                      >
                        撤回
                      </Button>
                    ) : (
                      <span key="withdraw-placeholder" />
                    )
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space wrap>
                        <Typography.Text strong>{ticket.ticketNo}</Typography.Text>
                        <TicketStatusTag status={ticket.status} />
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size={4}>
                        <Typography.Text>{ticket.title}</Typography.Text>
                        <Typography.Text type="secondary">{formatDateTime(ticket.updatedAt)}</Typography.Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>

          <Card title="最新通知" bordered={false} style={{ borderRadius: 24 }}>
            <List
              dataSource={unreadPreview}
              locale={{ emptyText: "暂无通知" }}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    <Button
                      key="read"
                      type="link"
                      onClick={() => {
                        portal.markInboxRead(item.id);
                        navigate("/inbox");
                      }}
                    >
                      查看
                    </Button>
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <span>{item.title}</span>
                        {item.isRead ? <Tag>已读</Tag> : <Tag color="red">未读</Tag>}
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size={4}>
                        <Typography.Text>{item.content}</Typography.Text>
                        <Typography.Text type="secondary">{formatDateTime(item.createdAt)}</Typography.Text>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
