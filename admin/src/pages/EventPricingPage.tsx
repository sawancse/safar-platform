import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Button, Input, Select, Modal, Form, message, Popconfirm,
  Space, Switch, InputNumber, Card, Tabs,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

type Row = {
  id: string;
  category: string;
  itemKey: string;
  label: string;
  description?: string;
  icon?: string;
  defaultPricePaise: number;
  priceType: string;
  minPricePaise?: number;
  maxPricePaise?: number;
  sortOrder?: number;
  active: boolean;
};

const CATEGORIES = [
  { value: 'PARTNER_SERVICE', label: 'Partner services',       hint: 'Photographer, DJ, Pandit, Decor…' },
  { value: 'STAFF_ROLE',      label: 'Service staff',          hint: 'Waiter, Bartender, Cleaner'       },
  { value: 'ADDON',           label: 'Event add-ons',          hint: 'Cake, Decoration, Crockery…'      },
  { value: 'LIVE_COUNTER',    label: 'Live counters',          hint: 'Dosa, BBQ, Chaat, Tandoor'        },
  { value: 'BASE_CONFIG',     label: 'Base config',            hint: 'Per-plate rate, platform fee…'    },
];

const PRICE_TYPES = [
  { value: 'PER_EVENT',  label: 'Per event (flat)' },
  { value: 'PER_PERSON', label: 'Per person'       },
  { value: 'PER_HOUR',   label: 'Per hour'         },
  { value: 'PER_UNIT',   label: 'Per unit'         },
  { value: 'PER_PLATE',  label: 'Per plate'        },
];

const INR = (p?: number) => p != null ? `₹${(p / 100).toLocaleString('en-IN')}` : '—';

export default function EventPricingPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [rows, setRows] = useState<Row[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState('PARTNER_SERVICE');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    adminApi.listEventPricing(token)
      .then((data: Row[]) => setRows(data || []))
      .catch((e: any) => message.error(e?.response?.data?.message || 'Failed to load pricing'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, []);

  const filtered = rows.filter(r =>
    r.category === activeTab &&
    (!search || r.label.toLowerCase().includes(search.toLowerCase()) || r.itemKey.toLowerCase().includes(search.toLowerCase()))
  );

  function openAdd() {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ category: activeTab, priceType: 'PER_EVENT', sortOrder: (filtered.length + 1) * 10 });
    setModalOpen(true);
  }

  function openEdit(row: Row) {
    setEditing(row);
    form.setFieldsValue({
      category: row.category,
      itemKey: row.itemKey,
      label: row.label,
      description: row.description,
      icon: row.icon,
      defaultPrice: row.defaultPricePaise != null ? Math.round(row.defaultPricePaise / 100) : null,
      priceType: row.priceType,
      minPrice: row.minPricePaise != null ? Math.round(row.minPricePaise / 100) : null,
      maxPrice: row.maxPricePaise != null ? Math.round(row.maxPricePaise / 100) : null,
      sortOrder: row.sortOrder,
    });
    setModalOpen(true);
  }

  async function onSave() {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const payload = {
        category: values.category,
        itemKey: values.itemKey.trim(),
        label: values.label.trim(),
        description: values.description || null,
        icon: values.icon || null,
        defaultPricePaise: values.defaultPrice * 100,
        priceType: values.priceType,
        minPricePaise: values.minPrice != null ? values.minPrice * 100 : null,
        maxPricePaise: values.maxPrice != null ? values.maxPrice * 100 : null,
        sortOrder: values.sortOrder ?? null,
      };
      if (editing) {
        const updated = await adminApi.updateEventPricing(editing.itemKey, payload, token);
        setRows(prev => prev.map(r => r.id === updated.id ? updated : r));
        message.success('Updated');
      } else {
        const created = await adminApi.createEventPricing(payload, token);
        setRows(prev => [...prev, created]);
        message.success('Added');
      }
      setModalOpen(false);
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  }

  async function onDeactivate(row: Row) {
    try {
      await adminApi.deactivateEventPricing(row.itemKey, token);
      setRows(prev => prev.map(r => r.id === row.id ? { ...r, active: false } : r));
      message.success('Deactivated');
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed');
    }
  }

  const columns: ColumnsType<Row> = [
    {
      title: '#',
      dataIndex: 'sortOrder',
      width: 50,
      sorter: (a, b) => (a.sortOrder || 0) - (b.sortOrder || 0),
      defaultSortOrder: 'ascend',
      render: (v?: number) => v ?? '—',
    },
    {
      title: 'Item',
      key: 'item',
      render: (_, r) => (
        <Space>
          <span style={{ fontSize: 22 }}>{r.icon || '·'}</span>
          <div>
            <div style={{ fontWeight: 600 }}>{r.label}</div>
            <Text code style={{ fontSize: 11 }}>{r.itemKey}</Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Description',
      dataIndex: 'description',
      ellipsis: true,
      render: (d?: string) => d || <Text type="secondary">—</Text>,
    },
    {
      title: 'Default',
      dataIndex: 'defaultPricePaise',
      width: 110,
      render: (v: number) => <Text strong>{INR(v)}</Text>,
      sorter: (a, b) => (a.defaultPricePaise || 0) - (b.defaultPricePaise || 0),
    },
    {
      title: 'Range',
      key: 'range',
      width: 140,
      render: (_, r) => (r.minPricePaise || r.maxPricePaise)
        ? <Text type="secondary" style={{ fontSize: 12 }}>{INR(r.minPricePaise)} – {INR(r.maxPricePaise)}</Text>
        : '—',
    },
    {
      title: 'Unit',
      dataIndex: 'priceType',
      width: 120,
      render: (t: string) => <Tag>{t.replace(/_/g, ' ')}</Tag>,
    },
    {
      title: 'Status',
      dataIndex: 'active',
      width: 80,
      render: (a: boolean) => a ? <Tag color="green">Active</Tag> : <Tag>Inactive</Tag>,
    },
    {
      title: '',
      key: 'actions',
      width: 130,
      render: (_, r) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)} />
          <Popconfirm title={`Deactivate ${r.label}?`} onConfirm={() => onDeactivate(r)} okText="Yes" cancelText="No">
            <Button size="small" danger icon={<DeleteOutlined />} disabled={!r.active} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Event Pricing</Title>
          <Text type="secondary">Edit what the customer sees on /cooks/events. Changes take effect immediately after save.</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>Add Item</Button>
      </div>

      <Card size="small" style={{ marginBottom: 12 }}>
        <Input.Search
          placeholder="Search by label or key"
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ maxWidth: 320 }}
          allowClear
        />
      </Card>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={CATEGORIES.map(c => ({
          key: c.value,
          label: (
            <div style={{ padding: '4px 0' }}>
              <div style={{ fontWeight: 600 }}>{c.label}</div>
              <div style={{ fontSize: 11, color: '#6b7280' }}>{c.hint}</div>
            </div>
          ),
          children: (
            <Table
              rowKey="id"
              columns={columns}
              dataSource={filtered}
              loading={loading}
              pagination={{ pageSize: 20, showSizeChanger: false }}
              rowClassName={r => r.active ? '' : 'opacity-50'}
            />
          ),
        }))}
      />

      <Modal
        open={modalOpen}
        title={editing ? `Edit ${editing.label}` : 'Add pricing item'}
        onCancel={() => !saving && setModalOpen(false)}
        onOk={onSave}
        confirmLoading={saving}
        okText={editing ? 'Save' : 'Add'}
        width={600}
      >
        <Form layout="vertical" form={form}>
          <Form.Item label="Category" name="category" rules={[{ required: true }]}>
            <Select
              options={CATEGORIES.map(c => ({ value: c.value, label: `${c.label} — ${c.hint}` }))}
              disabled={!!editing}
            />
          </Form.Item>
          <Space.Compact style={{ width: '100%' }}>
            <Form.Item label="Item key" name="itemKey" rules={[{ required: true, pattern: /^[a-z0-9_]+$/, message: 'Lowercase, digits, underscore' }]} style={{ flex: 1 }}>
              <Input placeholder="photography" disabled={!!editing} />
            </Form.Item>
            <Form.Item label="Icon (emoji)" name="icon" style={{ width: 120, marginLeft: 8 }}>
              <Input placeholder="📷" maxLength={4} />
            </Form.Item>
          </Space.Compact>
          <Form.Item label="Label" name="label" rules={[{ required: true }]}>
            <Input placeholder="Photographer" />
          </Form.Item>
          <Form.Item label="Description" name="description">
            <Input.TextArea rows={2} placeholder="Candid + posed photos for the event" />
          </Form.Item>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item label="Default price (₹)" name="defaultPrice" rules={[{ required: true, type: 'number', min: 0 }]} style={{ flex: 1, minWidth: 150 }}>
              <InputNumber style={{ width: '100%' }} placeholder="15000" />
            </Form.Item>
            <Form.Item label="Price type" name="priceType" rules={[{ required: true }]} style={{ flex: 1, minWidth: 150 }}>
              <Select options={PRICE_TYPES} />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item label="Min price (₹)" name="minPrice" style={{ flex: 1, minWidth: 150 }}>
              <InputNumber style={{ width: '100%' }} placeholder="5000" />
            </Form.Item>
            <Form.Item label="Max price (₹)" name="maxPrice" style={{ flex: 1, minWidth: 150 }}>
              <InputNumber style={{ width: '100%' }} placeholder="25000" />
            </Form.Item>
            <Form.Item label="Sort order" name="sortOrder" style={{ flex: 1, minWidth: 120 }}>
              <InputNumber style={{ width: '100%' }} placeholder="10" />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
