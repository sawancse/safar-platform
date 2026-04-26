import { useEffect, useMemo, useState } from 'react';
import {
  Table, Tag, Typography, Button, Modal, Form, Input, Select, message, Space,
  Tooltip, Statistic, Row, Col, Card, Empty, Popconfirm,
} from 'antd';
import { PlusOutlined, CopyOutlined, WhatsAppOutlined, MessageOutlined, StopOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Paragraph, Text } = Typography;

type Invite = {
  id: string;
  inviteToken: string;
  phone: string;
  businessName?: string;
  serviceType: string;
  notes?: string;
  sentAt: string;
  openedAt?: string;
  onboardingStartedAt?: string;
  submittedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  expiredAt?: string;
  expiresAt: string;
  sentVia: string;
  serviceListingId?: string;
};

type CreateResponse = {
  invite: Invite;
  deepLink: string;
  whatsAppMessage: string;
};

const SERVICE_TYPES = [
  { value: 'CAKE_DESIGNER',  label: '🎂 Cake Designer' },
  { value: 'SINGER',         label: '🎤 Singer' },
  { value: 'PANDIT',         label: '🪔 Pandit' },
  { value: 'DECORATOR',      label: '🌸 Decorator' },
  { value: 'STAFF_HIRE',     label: '🧑‍🍳 Staff Hire' },
  { value: 'COOK',           label: '👨‍🍳 Cook' },
  { value: 'APPLIANCE_RENTAL', label: '🍳 Appliance' },
  { value: 'PHOTOGRAPHER',   label: '📷 Photographer' },
  { value: 'DJ',             label: '🎧 DJ' },
  { value: 'MEHENDI',        label: '🌿 Mehendi' },
  { value: 'MAKEUP_ARTIST',  label: '💄 Makeup Artist' },
];

function statusOf(i: Invite): { label: string; color: string } {
  if (i.cancelledAt) return { label: 'Cancelled', color: 'default' };
  if (new Date(i.expiresAt) < new Date()) return { label: 'Expired', color: 'default' };
  if (i.completedAt) return { label: 'Approved ✓', color: 'green' };
  if (i.submittedAt) return { label: 'Submitted', color: 'blue' };
  if (i.onboardingStartedAt) return { label: 'In progress', color: 'cyan' };
  if (i.openedAt) return { label: 'Opened', color: 'orange' };
  return { label: 'Sent', color: 'gray' };
}

export default function VendorInvitesPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [invites, setInvites] = useState<Invite[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [created, setCreated] = useState<CreateResponse | null>(null);
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  async function load() {
    setLoading(true);
    try {
      const data = await adminApi.listVendorInvites(token);
      setInvites(data || []);
    } catch (err: any) {
      message.error(err?.response?.data?.detail || 'Failed to load invites');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  const stats = useMemo(() => {
    const total = invites.length;
    const opened = invites.filter(i => i.openedAt).length;
    const submitted = invites.filter(i => i.submittedAt).length;
    const approved = invites.filter(i => i.completedAt).length;
    return {
      total, opened, submitted, approved,
      openRate:  total > 0 ? Math.round((opened / total) * 100) : 0,
      submitRate: opened > 0 ? Math.round((submitted / opened) * 100) : 0,
      approveRate: submitted > 0 ? Math.round((approved / submitted) * 100) : 0,
    };
  }, [invites]);

  async function submit() {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const res: CreateResponse = await adminApi.createVendorInvite({
        phone: values.phone,
        serviceType: values.serviceType,
        businessName: values.businessName,
        notes: values.notes,
        sentVia: 'MANUAL',
      }, token);
      setCreated(res);
      setCreateOpen(false);
      form.resetFields();
      load();
    } catch (err: any) {
      if (err?.errorFields) return;
      message.error(err?.response?.data?.detail || 'Could not create invite');
    } finally {
      setSubmitting(false);
    }
  }

  async function cancel(id: string) {
    try {
      await adminApi.cancelVendorInvite(id, token);
      message.success('Invite cancelled');
      load();
    } catch (err: any) {
      message.error(err?.response?.data?.detail || 'Cancel failed');
    }
  }

  function copy(text: string, what: string) {
    navigator.clipboard.writeText(text).then(() => message.success(`${what} copied`));
  }

  function whatsAppLink(invite: Invite, msg: string) {
    const phoneNoPlus = invite.phone.replace(/\D/g, '');
    return `https://wa.me/${phoneNoPlus}?text=${encodeURIComponent(msg)}`;
  }

  const columns: ColumnsType<Invite> = useMemo(() => [
    {
      title: 'Vendor', key: 'vendor',
      render: (_, i) => (
        <div>
          <div style={{ fontWeight: 500 }}>{i.businessName || <Text type="secondary">(unknown)</Text>}</div>
          <Text type="secondary" style={{ fontSize: 11 }}>{i.phone}</Text>
        </div>
      ),
    },
    {
      title: 'Type', dataIndex: 'serviceType',
      render: (t: string) => {
        const def = SERVICE_TYPES.find(s => s.value === t);
        return <Tag>{def?.label ?? t}</Tag>;
      },
      filters: SERVICE_TYPES.map(s => ({ text: s.label, value: s.value })),
      onFilter: (v, row) => row.serviceType === v,
    },
    {
      title: 'Status', key: 'status',
      render: (_, i) => {
        const s = statusOf(i);
        return <Tag color={s.color}>{s.label}</Tag>;
      },
    },
    {
      title: 'Sent', dataIndex: 'sentAt',
      render: (iso: string) => new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }),
      sorter: (a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime(),
    },
    {
      title: 'Funnel', key: 'funnel',
      render: (_, i) => (
        <Space size={4}>
          <Tooltip title={i.openedAt ? `Opened ${new Date(i.openedAt).toLocaleString('en-IN')}` : 'Not opened'}>
            <span style={{ color: i.openedAt ? '#52c41a' : '#ccc', fontSize: 16 }}>👁</span>
          </Tooltip>
          <Tooltip title={i.submittedAt ? `Submitted ${new Date(i.submittedAt).toLocaleString('en-IN')}` : 'Not submitted'}>
            <span style={{ color: i.submittedAt ? '#52c41a' : '#ccc', fontSize: 16 }}>📨</span>
          </Tooltip>
          <Tooltip title={i.completedAt ? `Approved ${new Date(i.completedAt).toLocaleString('en-IN')}` : 'Not approved'}>
            <span style={{ color: i.completedAt ? '#52c41a' : '#ccc', fontSize: 16 }}>✅</span>
          </Tooltip>
        </Space>
      ),
    },
    {
      title: 'Actions', key: 'actions', width: 220,
      render: (_, i) => {
        const s = statusOf(i);
        const canCancel = !i.openedAt && !i.cancelledAt;
        const isActive = s.label !== 'Cancelled' && s.label !== 'Expired';
        const deepLink = `${window.location.origin.replace('admin.', '')}/vendor/onboard/${i.serviceType.toLowerCase().replace('_designer','').replace('_hire','-hire').replace('decorator','decor')}?invite=${i.inviteToken}`;
        const msg = `Hi ${i.businessName || 'there'} — start selling on Safar in 10 minutes: ${deepLink}\n\nYour phone is pre-filled. You'll need ID + (for food) FSSAI to publish.`;
        return (
          <Space size={4}>
            {isActive && (
              <>
                <Tooltip title="Copy WhatsApp message">
                  <Button size="small" icon={<CopyOutlined />} onClick={() => copy(msg, 'Message')} />
                </Tooltip>
                <Tooltip title="Open in WhatsApp">
                  <Button size="small" icon={<WhatsAppOutlined />} type="primary" ghost
                    href={whatsAppLink(i, msg)} target="_blank" rel="noreferrer" />
                </Tooltip>
              </>
            )}
            {canCancel && (
              <Popconfirm title="Cancel invite?" onConfirm={() => cancel(i.id)}>
                <Button size="small" danger icon={<StopOutlined />} />
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ], []);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>Vendor Invites</Title>
          <Paragraph type="secondary">
            BD outreach via WhatsApp (Pattern E). Generate a token-gated link, paste it into your WhatsApp.
            Vendor's phone pre-fills automatically. Invites expire in 30 days.
          </Paragraph>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          New Invite
        </Button>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}><Card><Statistic title="Sent" value={stats.total} /></Card></Col>
        <Col span={6}><Card><Statistic title="Opened" value={stats.opened} suffix={`(${stats.openRate}%)`} /></Card></Col>
        <Col span={6}><Card><Statistic title="Submitted" value={stats.submitted} suffix={stats.opened ? `(${stats.submitRate}% of opens)` : ''} /></Card></Col>
        <Col span={6}><Card><Statistic title="Approved" value={stats.approved} valueStyle={{ color: '#52c41a' }} suffix={stats.submitted ? `(${stats.approveRate}%)` : ''} /></Card></Col>
      </Row>

      <Table
        rowKey="id"
        loading={loading}
        dataSource={invites}
        columns={columns}
        size="middle"
        locale={{ emptyText: <Empty description="No invites yet — click New Invite to get started" /> }}
      />

      {/* Create modal */}
      <Modal
        title="Send vendor invite"
        open={createOpen}
        onCancel={() => { setCreateOpen(false); form.resetFields(); }}
        onOk={submit}
        confirmLoading={submitting}
        okText="Generate invite"
      >
        <Paragraph type="secondary" style={{ fontSize: 12 }}>
          Generates a token-gated deep-link. Copy the message and paste into your personal WhatsApp.
          Once vendor taps the link, their phone pre-fills automatically — they don't need to enter OTP.
        </Paragraph>
        <Form form={form} layout="vertical" requiredMark="optional">
          <Form.Item name="phone" label="Phone (E.164, e.g. +919876543210)" rules={[{ required: true, message: 'Required' }]}>
            <Input placeholder="+919876543210" />
          </Form.Item>
          <Form.Item name="serviceType" label="Service type" rules={[{ required: true, message: 'Required' }]}>
            <Select placeholder="Select service type" options={SERVICE_TYPES} />
          </Form.Item>
          <Form.Item name="businessName" label="Business name (your guess — vendor can edit)">
            <Input placeholder="e.g. Sweet Symphony Bakery" />
          </Form.Item>
          <Form.Item name="notes" label="Notes (for ops audit)">
            <Input.TextArea rows={2} placeholder="e.g. referred by Bakery X, spotted at wedding expo Mumbai" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Created modal — copy WhatsApp message + deep link */}
      <Modal
        title="✓ Invite generated"
        open={!!created}
        onCancel={() => setCreated(null)}
        footer={[
          <Button key="close" onClick={() => setCreated(null)}>Close</Button>,
        ]}
      >
        {created && (() => {
          const phoneNoPlus = created.invite.phone.replace(/\D/g, '');
          return (
            <div>
              <Paragraph>Copy + send this WhatsApp message to <Text strong>{created.invite.phone}</Text>:</Paragraph>
              <Card size="small" style={{ background: '#fafafa', marginBottom: 16 }}>
                <Paragraph style={{ marginBottom: 8, whiteSpace: 'pre-wrap' }}>{created.whatsAppMessage}</Paragraph>
              </Card>
              <Space>
                <Button icon={<CopyOutlined />} onClick={() => copy(created.whatsAppMessage, 'Message')}>
                  Copy message
                </Button>
                <Button type="primary" icon={<WhatsAppOutlined />}
                  href={`https://wa.me/${phoneNoPlus}?text=${encodeURIComponent(created.whatsAppMessage)}`}
                  target="_blank" rel="noreferrer">
                  Open WhatsApp
                </Button>
                <Button icon={<MessageOutlined />} onClick={() => copy(created.deepLink, 'Deep link')}>
                  Copy link only
                </Button>
              </Space>
            </div>
          );
        })()}
      </Modal>
    </div>
  );
}
