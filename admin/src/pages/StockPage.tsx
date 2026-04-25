import { useEffect, useMemo, useState } from 'react';
import {
  Table, Tag, Typography, Button, Input, Modal, Form, message, Space, Select,
  InputNumber, Tabs, Drawer, Switch, Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, EyeOutlined, ToolOutlined, WarningOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

const CATEGORY_OPTIONS = [
  { value: 'GROCERY',     label: '🥬 Grocery' },
  { value: 'BAKERY',      label: '🎂 Bakery' },
  { value: 'DECOR',       label: '🌸 Decor' },
  { value: 'PG_LINEN',    label: '🛏 PG Linen' },
  { value: 'MAINTENANCE', label: '🔧 Maintenance' },
];
const UNIT_OPTIONS = ['KG', 'GRAM', 'LITRE', 'MILLILITRE', 'PIECE', 'METRE', 'DOZEN', 'PACK', 'SET']
  .map(u => ({ value: u, label: u }));

type StockRow = {
  id: string;
  itemKey: string;
  itemLabel: string;
  category: string;
  unit: string;
  onHandQty: number;
  reservedQty: number;
  reorderPoint?: number;
  reorderQty?: number;
  lastUnitCostPaise?: number;
  lastReceivedAt?: string;
  active: boolean;
};

type Movement = {
  id: string;
  itemKey: string;
  direction: 'IN' | 'OUT' | 'ADJUSTMENT';
  qty: number;
  reason: string;
  refType?: string;
  refId?: string;
  unitCostPaise?: number;
  notes?: string;
  createdAt?: string;
};

const INR = (paise?: number) => paise ? `₹${(paise / 100).toLocaleString('en-IN')}` : '—';
const stockStatus = (row: StockRow): { label: string; color: string } => {
  if (row.onHandQty <= 0) return { label: 'OUT', color: 'red' };
  if (row.reorderPoint != null && row.onHandQty <= row.reorderPoint) return { label: 'LOW', color: 'orange' };
  return { label: 'OK', color: 'green' };
};

export default function StockPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [activeTab, setActiveTab] = useState<string>('GROCERY');
  const [rows, setRows] = useState<StockRow[]>([]);
  const [lowOnly, setLowOnly] = useState(false);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);

  const [itemModalOpen, setItemModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<StockRow | null>(null);
  const [itemForm] = Form.useForm();

  const [adjustModalOpen, setAdjustModalOpen] = useState(false);
  const [adjustItem, setAdjustItem] = useState<StockRow | null>(null);
  const [adjustForm] = Form.useForm();

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerItem, setDrawerItem] = useState<StockRow | null>(null);
  const [drawerMovements, setDrawerMovements] = useState<Movement[]>([]);

  const load = () => {
    setLoading(true);
    adminApi.listStock(token, { category: lowOnly ? undefined : activeTab, lowOnly })
      .then(d => setRows(d || []))
      .catch((e: any) => message.error(e?.response?.data?.message || 'Failed to load stock'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [activeTab, lowOnly]);

  const filtered = useMemo(() => rows.filter(r => {
    if (!search) return true;
    const s = search.toLowerCase();
    return r.itemKey.toLowerCase().includes(s) || r.itemLabel.toLowerCase().includes(s);
  }), [rows, search]);

  const lowCount = rows.filter(r => r.reorderPoint != null && r.onHandQty <= r.reorderPoint).length;
  const outCount = rows.filter(r => r.onHandQty <= 0).length;

  const openAddItem = () => {
    setEditingItem(null);
    itemForm.resetFields();
    itemForm.setFieldsValue({ category: activeTab, unit: 'KG', active: true });
    setItemModalOpen(true);
  };

  const openEditItem = (row: StockRow) => {
    setEditingItem(row);
    itemForm.setFieldsValue(row);
    setItemModalOpen(true);
  };

  const onSaveItem = async () => {
    try {
      const values = await itemForm.validateFields();
      await adminApi.upsertStockItem(values, token);
      message.success('Stock item saved');
      setItemModalOpen(false);
      load();
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message || 'Save failed');
    }
  };

  const openAdjust = (row: StockRow) => {
    setAdjustItem(row);
    adjustForm.resetFields();
    adjustForm.setFieldsValue({ reason: 'ADJUSTMENT_COUNT' });
    setAdjustModalOpen(true);
  };

  const onSaveAdjust = async () => {
    if (!adjustItem) return;
    try {
      const values = await adjustForm.validateFields();
      await adminApi.adjustStock(adjustItem.itemKey, values, token);
      message.success('Stock adjusted');
      setAdjustModalOpen(false);
      load();
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message || 'Adjust failed');
    }
  };

  const openDrawer = async (row: StockRow) => {
    setDrawerItem(row); setDrawerOpen(true);
    try {
      const m = await adminApi.getStockMovements(row.itemKey, token);
      setDrawerMovements(m || []);
    } catch { setDrawerMovements([]); }
  };

  const columns: ColumnsType<StockRow> = [
    { title: 'Item', dataIndex: 'itemLabel', render: (l, r) => (
      <div>
        <div style={{ fontWeight: 600 }}>{l}</div>
        <code style={{ fontSize: 11, color: '#888' }}>{r.itemKey}</code>
      </div>
    )},
    { title: 'Category', dataIndex: 'category', render: c => <Tag>{c}</Tag> },
    { title: 'On hand', dataIndex: 'onHandQty', render: (q, r) => <span>{q} {r.unit}</span> },
    { title: 'Reserved', dataIndex: 'reservedQty', render: (q, r) => q ? <span>{q} {r.unit}</span> : '—' },
    { title: 'Reorder pt', dataIndex: 'reorderPoint', render: (p, r) => p != null ? <span>{p} {r.unit}</span> : '—' },
    { title: 'Status', render: (_, r) => {
      const s = stockStatus(r);
      return <Tag color={s.color}>{s.label}</Tag>;
    }},
    { title: 'Last cost', dataIndex: 'lastUnitCostPaise', render: INR },
    { title: 'Last received', dataIndex: 'lastReceivedAt', render: d => d ? dayjs(d).format('YYYY-MM-DD') : '—' },
    { title: 'Actions', width: 220, render: (_, r) => (
      <Space size="small">
        <Button size="small" icon={<EyeOutlined />} onClick={() => openDrawer(r)}>View</Button>
        <Button size="small" icon={<ToolOutlined />} onClick={() => openAdjust(r)}>Adjust</Button>
        <Button size="small" onClick={() => openEditItem(r)}>Edit</Button>
      </Space>
    )},
  ];

  const movementCols: ColumnsType<Movement> = [
    { title: 'When', dataIndex: 'createdAt', render: d => d ? dayjs(d).format('YYYY-MM-DD HH:mm') : '—' },
    { title: 'Direction', dataIndex: 'direction', render: d => (
      <Tag color={d === 'IN' ? 'green' : d === 'OUT' ? 'red' : 'blue'}>{d}</Tag>
    )},
    { title: 'Qty', dataIndex: 'qty', render: (q, r) => <span>{q}{drawerItem ? ` ${drawerItem.unit}` : ''}</span> },
    { title: 'Reason', dataIndex: 'reason' },
    { title: 'Ref', render: (_, r) => r.refType ? <span><Tag>{r.refType}</Tag> {r.refId ? <code style={{ fontSize: 10 }}>{String(r.refId).slice(0, 8)}…</code> : ''}</span> : '—' },
    { title: 'Cost', dataIndex: 'unitCostPaise', render: INR },
    { title: 'Notes', dataIndex: 'notes', ellipsis: true },
  ];

  return (
    <div>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>Stock</Title>
            <Text type="secondary">Current on-hand inventory across categories. Movements are append-only — adjustments are tracked.</Text>
          </div>
          <Space>
            <Button onClick={openAddItem} icon={<PlusOutlined />}>Define Item</Button>
          </Space>
        </div>

        <Row gutter={16}>
          <Col span={6}><Card size="small"><Statistic title="Total Items" value={rows.length} /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="Low Stock" value={lowCount} valueStyle={{ color: lowCount ? '#fa8c16' : undefined }} prefix={lowCount ? <WarningOutlined /> : null} /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="Out of Stock" value={outCount} valueStyle={{ color: outCount ? '#cf1322' : undefined }} /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="Active Categories" value={CATEGORY_OPTIONS.length} /></Card></Col>
        </Row>

        <Tabs activeKey={lowOnly ? 'LOW' : activeTab}
              onChange={k => { if (k === 'LOW') setLowOnly(true); else { setLowOnly(false); setActiveTab(k); } }}
              items={[
                ...CATEGORY_OPTIONS.map(c => ({ key: c.value, label: c.label })),
                { key: 'LOW', label: <span style={{ color: '#fa8c16' }}>⚠ Low / Out ({lowCount})</span> },
              ]} />

        <Input.Search placeholder="Search item key / label" value={search} onChange={e => setSearch(e.target.value)}
                      allowClear style={{ width: 320 }} />

        <Table rowKey="id" dataSource={filtered} columns={columns} loading={loading}
               pagination={{ pageSize: 25 }} size="small" />
      </Space>

      <Modal title={editingItem ? `Edit ${editingItem.itemLabel}` : 'Define Stock Item'}
             open={itemModalOpen} onCancel={() => setItemModalOpen(false)} onOk={onSaveItem}
             okText={editingItem ? 'Update' : 'Create'} destroyOnClose>
        <Form form={itemForm} layout="vertical">
          <Form.Item label="Item key" name="itemKey" rules={[{ required: true }]}
                     extra="Canonical, snake_case (e.g. flour_maida). Must match what services use when consuming.">
            <Input placeholder="flour_maida" disabled={!!editingItem} />
          </Form.Item>
          <Form.Item label="Item label" name="itemLabel" rules={[{ required: true }]}>
            <Input placeholder="Maida Flour" />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Form.Item label="Category" name="category" rules={[{ required: true }]}>
              <Select options={CATEGORY_OPTIONS} />
            </Form.Item>
            <Form.Item label="Unit" name="unit" rules={[{ required: true }]}>
              <Select options={UNIT_OPTIONS} />
            </Form.Item>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Form.Item label="Reorder point" name="reorderPoint" extra="Alert threshold for low-stock">
              <InputNumber min={0} step={0.5} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Reorder qty" name="reorderQty" extra="Suggested PO qty when reordering">
              <InputNumber min={0} step={0.5} style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item label="Active" name="active" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>

      <Modal title={`Adjust — ${adjustItem?.itemLabel}`} open={adjustModalOpen} onCancel={() => setAdjustModalOpen(false)} onOk={onSaveAdjust}
             okText="Adjust" destroyOnClose>
        <Form form={adjustForm} layout="vertical">
          <Form.Item label="Quantity delta" name="qtyDelta" rules={[{ required: true }]}
                     extra={`Positive = add stock, negative = remove. Current: ${adjustItem?.onHandQty} ${adjustItem?.unit}`}>
            <InputNumber step={0.5} style={{ width: '100%' }} placeholder="-2.5 or +10" />
          </Form.Item>
          <Form.Item label="Reason" name="reason" rules={[{ required: true }]}>
            <Select options={[
              { value: 'ADJUSTMENT_COUNT',  label: 'Stock count correction' },
              { value: 'ADJUSTMENT_DAMAGE', label: 'Damage / spoilage' },
              { value: 'RETURN',            label: 'Return to supplier' },
            ]} />
          </Form.Item>
          <Form.Item label="Notes" name="notes">
            <Input.TextArea rows={2} placeholder="What happened?" />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer title={drawerItem?.itemLabel} open={drawerOpen} onClose={() => setDrawerOpen(false)} width={760}>
        {drawerItem && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Card size="small">
              <Row gutter={16}>
                <Col span={6}><Statistic title="On hand" value={`${drawerItem.onHandQty} ${drawerItem.unit}`} /></Col>
                <Col span={6}><Statistic title="Reserved" value={drawerItem.reservedQty || 0} suffix={drawerItem.unit} /></Col>
                <Col span={6}><Statistic title="Reorder point" value={drawerItem.reorderPoint ?? '—'} suffix={drawerItem.reorderPoint != null ? drawerItem.unit : ''} /></Col>
                <Col span={6}><Statistic title="Last cost" value={INR(drawerItem.lastUnitCostPaise)} /></Col>
              </Row>
            </Card>

            <div>
              <Title level={5}>Movement history</Title>
              <Table rowKey="id" dataSource={drawerMovements} columns={movementCols} pagination={false} size="small" />
            </div>
          </Space>
        )}
      </Drawer>
    </div>
  );
}
