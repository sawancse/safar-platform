import { useEffect, useMemo, useState } from 'react';
import {
  Table, Tag, Typography, Button, Modal, Form, InputNumber, Input, message, Space,
  Tabs, Empty,
} from 'antd';
import { EditOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Paragraph, Text } = Typography;

type RateRow = {
  id: string;
  serviceType: string;
  tier: 'STARTER' | 'PRO' | 'COMMERCIAL';
  commissionPct: number;
  promotionThreshold: number;
  notes?: string;
  updatedAt?: string;
  updatedBy?: string;
};

const SERVICE_TYPES = [
  { key: 'CAKE_DESIGNER',    label: 'Cake Designer',  emoji: '🎂' },
  { key: 'SINGER',           label: 'Singer',         emoji: '🎤' },
  { key: 'PANDIT',           label: 'Pandit',         emoji: '🪔' },
  { key: 'DECORATOR',        label: 'Decorator',      emoji: '🌸' },
  { key: 'STAFF_HIRE',       label: 'Staff Hire',     emoji: '🧑‍🍳' },
  { key: 'COOK',             label: 'Cook',           emoji: '👨‍🍳' },
  { key: 'APPLIANCE_RENTAL', label: 'Appliance',      emoji: '🍳' },
  { key: 'PHOTOGRAPHER',     label: 'Photographer',   emoji: '📷' },
  { key: 'DJ',               label: 'DJ',             emoji: '🎧' },
  { key: 'MEHENDI',          label: 'Mehendi',        emoji: '🌿' },
  { key: 'MAKEUP_ARTIST',    label: 'Makeup Artist',  emoji: '💄' },
];

const TIER_COLOR: Record<string, string> = {
  STARTER: 'default',
  PRO: 'blue',
  COMMERCIAL: 'gold',
};

export default function CommissionRatesPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [tab, setTab] = useState('all');
  const [rows, setRows] = useState<RateRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<RateRow | null>(null);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    try {
      const serviceType = tab === 'all' ? undefined : tab;
      const data = await adminApi.listCommissionRates(token, serviceType);
      setRows((data || []).map((r: any) => ({
        ...r,
        commissionPct: Number(r.commissionPct),
      })));
    } catch (err: any) {
      message.error(err?.response?.data?.detail || 'Failed to load rates');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [tab]);

  function openEdit(row: RateRow) {
    setEditing(row);
    form.setFieldsValue({
      commissionPct: row.commissionPct,
      promotionThreshold: row.promotionThreshold,
      notes: row.notes ?? '',
    });
  }

  async function save() {
    if (!editing) return;
    try {
      const values = await form.validateFields();
      setSaving(true);
      await adminApi.updateCommissionRate(editing.serviceType, editing.tier, {
        commissionPct: values.commissionPct,
        promotionThreshold: values.promotionThreshold,
        notes: values.notes || undefined,
      }, token);
      message.success(`Updated ${editing.serviceType} ${editing.tier}`);
      setEditing(null);
      load();
    } catch (err: any) {
      message.error(err?.response?.data?.detail || err?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  const columns: ColumnsType<RateRow> = useMemo(() => [
    {
      title: 'Service Type', dataIndex: 'serviceType', key: 'serviceType',
      render: (t: string) => {
        const def = SERVICE_TYPES.find(s => s.key === t);
        return <Space><span style={{ fontSize: 18 }}>{def?.emoji ?? '•'}</span><span>{def?.label ?? t}</span></Space>;
      },
      filters: SERVICE_TYPES.map(s => ({ text: s.label, value: s.key })),
      onFilter: (v, row) => row.serviceType === v,
    },
    {
      title: 'Tier', dataIndex: 'tier', key: 'tier',
      render: (t: string) => <Tag color={TIER_COLOR[t]}>{t}</Tag>,
      sorter: (a, b) => a.tier.localeCompare(b.tier),
    },
    {
      title: 'Commission %', dataIndex: 'commissionPct', key: 'commissionPct',
      render: (v: number) => <span style={{ fontWeight: 600 }}>{v.toFixed(2)}%</span>,
    },
    {
      title: 'Promotion threshold', dataIndex: 'promotionThreshold', key: 'promotionThreshold',
      render: (v: number) => v === 0 ? <Text type="secondary">entry tier</Text> : `${v}+ bookings`,
    },
    { title: 'Notes', dataIndex: 'notes', key: 'notes', render: (n?: string) => n ?? '—' },
    {
      title: 'Actions', key: 'actions',
      render: (_, row) => (
        <Button type="link" icon={<EditOutlined />} onClick={() => openEdit(row)}>Edit</Button>
      ),
    },
  ], []);

  return (
    <div>
      <Title level={3}>Commission Rates</Title>
      <Paragraph type="secondary">
        Per-(service type × tier) commission percentages and the booking-count thresholds that
        promote vendors to the next tier. Vendors auto-promote nightly via{' '}
        <Text code>TrustTierScheduler</Text>; never auto-demote.{' '}
        <strong>Vendors join at STARTER and never pay a monthly fee — only commission.</strong>{' '}
        Per-vendor overrides are set on individual listings via the Service Listings page.
      </Paragraph>

      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          { key: 'all', label: 'All types' },
          ...SERVICE_TYPES.map(s => ({
            key: s.key,
            label: <span>{s.emoji} {s.label}</span>,
          })),
        ]}
      />

      <Table
        rowKey="id"
        loading={loading}
        dataSource={rows}
        columns={columns}
        size="middle"
        pagination={false}
        locale={{ emptyText: <Empty description="No commission rates configured" /> }}
      />

      <Modal
        title={editing ? `Edit ${editing.serviceType} · ${editing.tier}` : ''}
        open={!!editing}
        onCancel={() => setEditing(null)}
        onOk={save}
        confirmLoading={saving}
        okText="Save (effective on next nightly cycle)"
      >
        <Paragraph type="secondary" style={{ fontSize: 12 }}>
          Changes take effect on the next vendor-promotion cycle (nightly @ 02:00 IST). The
          rate cache refreshes within 5 minutes for new bookings.
        </Paragraph>
        <Form form={form} layout="vertical">
          <Form.Item
            name="commissionPct"
            label="Commission percentage"
            rules={[
              { required: true, message: 'Required' },
              { type: 'number', min: 0, max: 50, message: 'Must be 0-50' },
            ]}
          >
            <InputNumber
              min={0}
              max={50}
              step={0.5}
              precision={2}
              style={{ width: '100%' }}
              addonAfter="%"
            />
          </Form.Item>
          <Form.Item
            name="promotionThreshold"
            label="Promotion threshold (completed bookings to enter this tier)"
            rules={[{ type: 'number', min: 0, message: 'Must be ≥ 0' }]}
          >
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="notes" label="Notes (optional, for ops audit trail)">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
