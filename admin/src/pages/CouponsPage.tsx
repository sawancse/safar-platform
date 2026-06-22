import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Button, Modal, Form, Input, InputNumber, Select, DatePicker, message, Space, Switch,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Paragraph } = Typography;

type Coupon = {
  id: string; code: string; scope: string; discountType: string;
  percentOff?: number; maxDiscountPaise?: number; flatOffPaise?: number;
  minBookingPaise?: number; validFrom?: string; validUntil?: string;
  usageLimit?: number; usedCount?: number; perUserLimit?: number;
  active?: boolean; description?: string;
};

const inr = (p?: number) => p != null ? `₹${(p / 100).toLocaleString('en-IN')}` : '—';

export default function CouponsPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [rows, setRows] = useState<Coupon[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();
  const discountType = Form.useWatch('discountType', form);

  async function load() {
    setLoading(true);
    try { setRows(await adminApi.listCoupons(token) || []); }
    catch { message.error('Failed to load coupons'); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); /* eslint-disable-next-line */ }, []);

  async function submit() {
    const v = await form.validateFields();
    const body: any = {
      code: (v.code || '').trim().toUpperCase(),
      scope: 'PLATFORM',
      discountType: v.discountType,
      percentOff: v.discountType === 'PERCENT' ? v.percentOff : undefined,
      maxDiscountPaise: v.discountType === 'PERCENT' && v.maxDiscountRupees ? Math.round(v.maxDiscountRupees * 100) : undefined,
      flatOffPaise: v.discountType === 'FLAT' && v.flatOffRupees ? Math.round(v.flatOffRupees * 100) : undefined,
      minBookingPaise: v.minBookingRupees ? Math.round(v.minBookingRupees * 100) : 0,
      validFrom: v.validFrom ? v.validFrom.format('YYYY-MM-DD') : undefined,
      validUntil: v.validUntil ? v.validUntil.format('YYYY-MM-DD') : undefined,
      usageLimit: v.usageLimit ?? undefined,
      perUserLimit: v.perUserLimit ?? undefined,
      description: v.description || undefined,
    };
    try {
      await adminApi.createCoupon(body, token);
      message.success('Coupon created');
      setOpen(false); form.resetFields(); load();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || e?.response?.data?.message || 'Create failed');
    }
  }

  async function toggle(c: Coupon) {
    try { await adminApi.setCouponActive(c.id, !c.active, token); load(); }
    catch { message.error('Failed to update'); }
  }

  const columns: ColumnsType<Coupon> = [
    { title: 'Code', dataIndex: 'code', render: (c) => <b>{c}</b> },
    { title: 'Discount', render: (_, r) => r.discountType === 'PERCENT'
        ? `${r.percentOff}%${r.maxDiscountPaise ? ` (max ${inr(r.maxDiscountPaise)})` : ''}`
        : `${inr(r.flatOffPaise)} off` },
    { title: 'Min booking', dataIndex: 'minBookingPaise', render: inr },
    { title: 'Validity', render: (_, r) => `${r.validFrom ?? '—'} → ${r.validUntil ?? '—'}` },
    { title: 'Used', render: (_, r) => `${r.usedCount ?? 0}${r.usageLimit ? ` / ${r.usageLimit}` : ''}` },
    { title: 'Per-user', dataIndex: 'perUserLimit', render: (v) => v ?? '∞' },
    { title: 'Active', render: (_, r) => <Switch checked={!!r.active} onChange={() => toggle(r)} /> },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={3} style={{ margin: 0 }}>Coupons</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>New coupon</Button>
      </div>
      <Paragraph type="secondary">Platform-wide promo codes (MakeMyTrip style). Hosts create their own per-listing coupons from their dashboard.</Paragraph>

      <Table rowKey="id" loading={loading} columns={columns} dataSource={rows} pagination={{ pageSize: 20 }} />

      <Modal title="New coupon" open={open} onOk={submit} onCancel={() => setOpen(false)} okText="Create" width={560}>
        <Form form={form} layout="vertical" initialValues={{ discountType: 'PERCENT' }}>
          <Form.Item name="code" label="Code" rules={[{ required: true }]}>
            <Input placeholder="STAY20" style={{ textTransform: 'uppercase' }} />
          </Form.Item>
          <Space>
            <Form.Item name="discountType" label="Type" rules={[{ required: true }]}>
              <Select style={{ width: 160 }} options={[{ value: 'PERCENT', label: 'Percent off' }, { value: 'FLAT', label: 'Flat off' }]} />
            </Form.Item>
            {discountType === 'PERCENT' ? (
              <>
                <Form.Item name="percentOff" label="Percent" rules={[{ required: true }]}>
                  <InputNumber min={1} max={100} addonAfter="%" />
                </Form.Item>
                <Form.Item name="maxDiscountRupees" label="Max discount (₹)">
                  <InputNumber min={0} placeholder="cap" />
                </Form.Item>
              </>
            ) : (
              <Form.Item name="flatOffRupees" label="Flat off (₹)" rules={[{ required: true }]}>
                <InputNumber min={1} />
              </Form.Item>
            )}
          </Space>
          <Space>
            <Form.Item name="minBookingRupees" label="Min booking (₹)"><InputNumber min={0} /></Form.Item>
            <Form.Item name="usageLimit" label="Total uses"><InputNumber min={1} placeholder="∞" /></Form.Item>
            <Form.Item name="perUserLimit" label="Per user"><InputNumber min={1} placeholder="∞" /></Form.Item>
          </Space>
          <Space>
            <Form.Item name="validFrom" label="Valid from"><DatePicker /></Form.Item>
            <Form.Item name="validUntil" label="Valid until"><DatePicker /></Form.Item>
          </Space>
          <Form.Item name="description" label="Description"><Input placeholder="Internal note / campaign" /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
