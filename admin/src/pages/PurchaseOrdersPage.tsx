import { useEffect, useMemo, useState } from 'react';
import {
  Table, Tag, Typography, Button, Input, Modal, Form, message, Space, Select,
  InputNumber, DatePicker, Drawer, Descriptions, Card, Row, Col, Statistic, Popconfirm,
} from 'antd';
import { PlusOutlined, EyeOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'default', ISSUED: 'blue', ACKNOWLEDGED: 'cyan', IN_TRANSIT: 'gold',
  DELIVERED: 'green', INVOICED: 'purple', PAID: 'green', CANCELLED: 'red',
};
const INR = (paise?: number) => paise ? `₹${(paise / 100).toLocaleString('en-IN')}` : '—';

type PoRow = {
  id: string;
  poNumber: string;
  supplierId: string;
  status: string;
  expectedDelivery?: string;
  deliveredAt?: string;
  invoiceNumber?: string;
  invoicePaise?: number;
  paymentRef?: string;
  totalPaise: number;
  taxPaise?: number;
  grandTotalPaise?: number;
  adminNotes?: string;
  createdAt?: string;
  externalRef?: string;
  externalStatus?: string;
  externalSyncedAt?: string;
  externalError?: string;
};

type CatalogItem = {
  id: string;
  itemKey: string;
  itemLabel: string;
  category: string;
  unit: string;
  pricePaise: number;
};

export default function PurchaseOrdersPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [pos, setPos] = useState<PoRow[]>([]);
  const [overdue, setOverdue] = useState<PoRow[]>([]);
  const [suppliers, setSuppliers] = useState<any[]>([]);
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [supplierFilter, setSupplierFilter] = useState<string | undefined>();
  const [loading, setLoading] = useState(true);

  const [createOpen, setCreateOpen] = useState(false);
  const [createSupplier, setCreateSupplier] = useState<string | null>(null);
  const [createCatalog, setCreateCatalog] = useState<CatalogItem[]>([]);
  const [createItems, setCreateItems] = useState<{ catalogItemId: string; qty: number }[]>([]);
  const [createDate, setCreateDate] = useState<dayjs.Dayjs | null>(null);
  const [createDeliveryAddr, setCreateDeliveryAddr] = useState('');
  const [createNotes, setCreateNotes] = useState('');
  const [issueImmediately, setIssueImmediately] = useState(false);
  const [creating, setCreating] = useState(false);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerPo, setDrawerPo] = useState<PoRow | null>(null);
  const [drawerItems, setDrawerItems] = useState<any[]>([]);

  const [invoiceModal, setInvoiceModal] = useState<PoRow | null>(null);
  const [invoiceNum, setInvoiceNum] = useState('');
  const [invoiceAmt, setInvoiceAmt] = useState<number | null>(null);

  const [payModal, setPayModal] = useState<PoRow | null>(null);
  const [payRef, setPayRef] = useState('');

  const supplierMap = useMemo(() => {
    const m: Record<string, any> = {};
    suppliers.forEach(s => { m[s.id] = s; });
    return m;
  }, [suppliers]);

  const load = async () => {
    setLoading(true);
    try {
      const [list, ov, sup] = await Promise.all([
        adminApi.listPurchaseOrders(token, { status: statusFilter, supplierId: supplierFilter }),
        adminApi.listOverduePurchaseOrders(token),
        adminApi.listSuppliers(token, true),
      ]);
      setPos(list || []); setOverdue(ov || []); setSuppliers(sup || []);
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed to load POs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [statusFilter, supplierFilter]);

  const monthSpend = pos
    .filter(p => p.status !== 'CANCELLED' && dayjs(p.createdAt).isSame(dayjs(), 'month'))
    .reduce((s, p) => s + (p.grandTotalPaise || 0), 0);

  const openSupplierForCreate = async (supplierId: string) => {
    setCreateSupplier(supplierId);
    setCreateItems([]);
    try {
      const cat = await adminApi.listSupplierCatalog(supplierId, token, true);
      setCreateCatalog(cat || []);
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed to load catalog');
    }
  };

  const handleCreate = async () => {
    if (!createSupplier) { message.warning('Pick a supplier'); return; }
    if (createItems.length === 0) { message.warning('Add at least one item'); return; }

    const items = createItems
      .filter(li => li.catalogItemId && li.qty > 0)
      .map(li => {
        const c = createCatalog.find(c => c.id === li.catalogItemId);
        return c ? { catalogItemId: c.id, itemKey: c.itemKey, qty: li.qty } : null;
      })
      .filter(Boolean);

    if (items.length === 0) { message.warning('Pick valid items + qty'); return; }

    setCreating(true);
    try {
      await adminApi.createPurchaseOrder({
        supplierId: createSupplier,
        expectedDelivery: createDate ? createDate.format('YYYY-MM-DD') : null,
        deliveryAddress: createDeliveryAddr || null,
        adminNotes: createNotes || null,
        issueImmediately,
        items,
      }, token);
      message.success(`PO ${issueImmediately ? 'issued' : 'saved as draft'}`);
      setCreateOpen(false);
      setCreateSupplier(null); setCreateItems([]); setCreateDate(null);
      setCreateDeliveryAddr(''); setCreateNotes(''); setIssueImmediately(false);
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Create failed');
    } finally {
      setCreating(false);
    }
  };

  const openDrawer = async (po: PoRow) => {
    setDrawerPo(po); setDrawerOpen(true);
    try {
      const items = await adminApi.getPurchaseOrderItems(po.id, token);
      setDrawerItems(items || []);
    } catch { setDrawerItems([]); }
  };

  const transition = async (po: PoRow, action: 'issue' | 'ack' | 'in-transit' | 'deliver') => {
    try {
      await adminApi.transitionPurchaseOrder(po.id, action, token);
      message.success('Status updated');
      setDrawerOpen(false);
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Transition failed');
    }
  };

  const cancelPo = async (po: PoRow) => {
    try {
      await adminApi.cancelPurchaseOrder(po.id, 'Cancelled by admin', token);
      message.success('PO cancelled');
      setDrawerOpen(false);
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Cancel failed');
    }
  };

  const submitInvoice = async () => {
    if (!invoiceModal || !invoiceNum.trim() || !invoiceAmt) {
      message.warning('Invoice number + amount required'); return;
    }
    try {
      await adminApi.invoicePurchaseOrder(invoiceModal.id, {
        invoiceNumber: invoiceNum.trim(),
        invoicePaise: Math.round(invoiceAmt * 100),
      }, token);
      message.success('Invoice recorded');
      setInvoiceModal(null); setInvoiceNum(''); setInvoiceAmt(null);
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed');
    }
  };

  const submitPay = async () => {
    if (!payModal || !payRef.trim()) { message.warning('Payment ref required'); return; }
    try {
      await adminApi.payPurchaseOrder(payModal.id, payRef.trim(), token);
      message.success('Payment recorded');
      setPayModal(null); setPayRef('');
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed');
    }
  };

  const columns: ColumnsType<PoRow> = [
    { title: 'PO#', dataIndex: 'poNumber', render: v => <code style={{ fontSize: 12 }}>{v}</code> },
    { title: 'Supplier', dataIndex: 'supplierId', render: id => supplierMap[id]?.businessName || '—' },
    { title: 'Status', dataIndex: 'status', render: s => <Tag color={STATUS_COLORS[s]}>{s}</Tag> },
    { title: 'Total', dataIndex: 'grandTotalPaise', render: INR },
    { title: 'Expected', dataIndex: 'expectedDelivery', render: d => d || '—' },
    { title: 'Delivered', dataIndex: 'deliveredAt', render: d => d ? dayjs(d).format('YYYY-MM-DD') : '—' },
    { title: 'Invoice', dataIndex: 'invoiceNumber', render: v => v || '—' },
    { title: 'Payment', dataIndex: 'paymentRef', render: v => v || '—' },
    { title: '', width: 80, render: (_, r) => (
      <Button size="small" icon={<EyeOutlined />} onClick={() => openDrawer(r)}>View</Button>
    )},
  ];

  const itemsCols: ColumnsType<any> = [
    { title: 'Item', dataIndex: 'itemLabel', render: (l, r) => <span><code style={{ fontSize: 11 }}>{r.itemKey}</code> · {l}</span> },
    { title: 'Qty', dataIndex: 'qty' },
    { title: 'Unit', dataIndex: 'unit' },
    { title: 'Price', dataIndex: 'unitPricePaise', render: INR },
    { title: 'Line', dataIndex: 'lineTotalPaise', render: INR },
    { title: 'Recv', dataIndex: 'receivedQty' },
  ];

  return (
    <div>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>Purchase Orders</Title>
            <Text type="secondary">Order from suppliers, track delivery, mark paid. Receiving auto-credits stock.</Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>Create PO</Button>
        </div>

        <Row gutter={16}>
          <Col span={6}><Card size="small"><Statistic title="Open POs" value={pos.filter(p => !['DELIVERED','INVOICED','PAID','CANCELLED'].includes(p.status)).length} /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="Overdue" value={overdue.length} valueStyle={{ color: overdue.length ? '#cf1322' : undefined }} /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="Pending Payment" value={pos.filter(p => p.status === 'INVOICED').length} /></Card></Col>
          <Col span={6}><Card size="small"><Statistic title="This Month Spend" value={INR(monthSpend)} valueStyle={{ color: '#f97316', fontWeight: 700 }} /></Card></Col>
        </Row>

        <Space>
          <Select placeholder="Status" allowClear style={{ width: 160 }} value={statusFilter} onChange={setStatusFilter}
                  options={Object.keys(STATUS_COLORS).map(s => ({ value: s, label: s }))} />
          <Select placeholder="Supplier" allowClear showSearch style={{ width: 240 }} value={supplierFilter} onChange={setSupplierFilter}
                  optionFilterProp="label"
                  options={suppliers.map(s => ({ value: s.id, label: s.businessName }))} />
        </Space>

        <Table rowKey="id" dataSource={pos} columns={columns} loading={loading} pagination={{ pageSize: 20 }} size="small" />
      </Space>

      {/* Create PO modal */}
      <Modal title="Create Purchase Order" open={createOpen} onCancel={() => setCreateOpen(false)} onOk={handleCreate}
             okText={issueImmediately ? 'Create & Issue' : 'Save Draft'} confirmLoading={creating} width={760}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Select showSearch placeholder="Pick supplier" style={{ width: '100%' }}
                  value={createSupplier} onChange={openSupplierForCreate}
                  optionFilterProp="label"
                  options={suppliers.map(s => ({ value: s.id, label: `${s.businessName} — ${s.phone}` }))} />

          {createSupplier && (
            <>
              <Text strong>Add line items</Text>
              <Button onClick={() => setCreateItems(prev => [...prev, { catalogItemId: '', qty: 1 }])}>+ Add item</Button>
              {createItems.map((li, idx) => {
                const c = createCatalog.find(c => c.id === li.catalogItemId);
                return (
                  <Space key={idx} style={{ width: '100%' }}>
                    <Select showSearch style={{ width: 320 }} placeholder="Item from catalog"
                            value={li.catalogItemId} optionFilterProp="label"
                            onChange={v => setCreateItems(items => items.map((it, i) => i === idx ? { ...it, catalogItemId: v } : it))}
                            options={createCatalog.map(c => ({ value: c.id, label: `${c.itemLabel} · ${INR(c.pricePaise)}/${c.unit}` }))} />
                    <InputNumber min={0.01} step={0.5} value={li.qty} onChange={v => setCreateItems(items => items.map((it, i) => i === idx ? { ...it, qty: Number(v) } : it))} />
                    {c && <span style={{ color: '#6b7280', fontSize: 12 }}>= {INR((c.pricePaise || 0) * li.qty)}</span>}
                    <Button danger size="small" onClick={() => setCreateItems(items => items.filter((_, i) => i !== idx))}>×</Button>
                  </Space>
                );
              })}

              <DatePicker placeholder="Expected delivery" value={createDate} onChange={setCreateDate} style={{ width: '100%' }} />
              <Input placeholder="Delivery address (warehouse / drop point)" value={createDeliveryAddr} onChange={e => setCreateDeliveryAddr(e.target.value)} />
              <Input.TextArea placeholder="Admin notes" rows={2} value={createNotes} onChange={e => setCreateNotes(e.target.value)} />
              <Space>
                <input type="checkbox" id="issue-now" checked={issueImmediately} onChange={e => setIssueImmediately(e.target.checked)} />
                <label htmlFor="issue-now">Issue immediately (skip DRAFT)</label>
              </Space>
            </>
          )}
        </Space>
      </Modal>

      {/* PO detail drawer */}
      <Drawer title={drawerPo?.poNumber} open={drawerOpen} onClose={() => setDrawerOpen(false)} width={760}>
        {drawerPo && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Descriptions column={2} size="small" bordered>
              <Descriptions.Item label="Supplier">{supplierMap[drawerPo.supplierId]?.businessName || '—'}</Descriptions.Item>
              <Descriptions.Item label="Status"><Tag color={STATUS_COLORS[drawerPo.status]}>{drawerPo.status}</Tag></Descriptions.Item>
              <Descriptions.Item label="Total">{INR(drawerPo.totalPaise)}</Descriptions.Item>
              <Descriptions.Item label="Tax">{INR(drawerPo.taxPaise)}</Descriptions.Item>
              <Descriptions.Item label="Grand total" span={2}><Text strong>{INR(drawerPo.grandTotalPaise)}</Text></Descriptions.Item>
              <Descriptions.Item label="Expected">{drawerPo.expectedDelivery || '—'}</Descriptions.Item>
              <Descriptions.Item label="Delivered">{drawerPo.deliveredAt || '—'}</Descriptions.Item>
              <Descriptions.Item label="Invoice #">{drawerPo.invoiceNumber || '—'}</Descriptions.Item>
              <Descriptions.Item label="Invoice ₹">{INR(drawerPo.invoicePaise)}</Descriptions.Item>
              <Descriptions.Item label="Payment ref" span={2}>{drawerPo.paymentRef || '—'}</Descriptions.Item>
              <Descriptions.Item label="Notes" span={2}>{drawerPo.adminNotes || '—'}</Descriptions.Item>
              {drawerPo.externalRef && (
                <Descriptions.Item label="External ref" span={2}>
                  <code style={{ fontSize: 12 }}>{drawerPo.externalRef}</code>
                  {drawerPo.externalStatus && <Tag style={{ marginLeft: 8 }}>{drawerPo.externalStatus}</Tag>}
                  {drawerPo.externalSyncedAt && <span style={{ marginLeft: 8, color: '#888', fontSize: 11 }}>synced {dayjs(drawerPo.externalSyncedAt).format('YYYY-MM-DD HH:mm')}</span>}
                </Descriptions.Item>
              )}
              {drawerPo.externalError && (
                <Descriptions.Item label="External error" span={2}>
                  <Text type="danger" style={{ fontSize: 12 }}>{drawerPo.externalError}</Text>
                </Descriptions.Item>
              )}
            </Descriptions>

            <div>
              <Title level={5}>Line items</Title>
              <Table rowKey="id" dataSource={drawerItems} columns={itemsCols} pagination={false} size="small" />
            </div>

            <Space wrap>
              {drawerPo.status === 'DRAFT' && <Button type="primary" onClick={() => transition(drawerPo, 'issue')}>Issue PO</Button>}
              {drawerPo.status === 'ISSUED' && <Button onClick={() => transition(drawerPo, 'ack')}>Mark Acknowledged</Button>}
              {(drawerPo.status === 'ISSUED' || drawerPo.status === 'ACKNOWLEDGED') && <Button onClick={() => transition(drawerPo, 'in-transit')}>In Transit</Button>}
              {(drawerPo.status === 'ISSUED' || drawerPo.status === 'ACKNOWLEDGED' || drawerPo.status === 'IN_TRANSIT') &&
                <Button type="primary" onClick={() => transition(drawerPo, 'deliver')}>Mark Delivered (credits stock)</Button>}
              {drawerPo.status === 'DELIVERED' && <Button onClick={() => { setInvoiceModal(drawerPo); setInvoiceAmt((drawerPo.grandTotalPaise || 0) / 100); }}>Record Invoice</Button>}
              {drawerPo.status === 'INVOICED' && <Button type="primary" onClick={() => setPayModal(drawerPo)}>Mark Paid</Button>}
              {!['DELIVERED','INVOICED','PAID','CANCELLED'].includes(drawerPo.status) && (
                <Popconfirm title="Cancel this PO?" onConfirm={() => cancelPo(drawerPo)}>
                  <Button danger>Cancel PO</Button>
                </Popconfirm>
              )}
            </Space>
          </Space>
        )}
      </Drawer>

      <Modal title={`Record Invoice — ${invoiceModal?.poNumber}`} open={!!invoiceModal} onCancel={() => setInvoiceModal(null)} onOk={submitInvoice} okText="Record">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input placeholder="Supplier invoice number" value={invoiceNum} onChange={e => setInvoiceNum(e.target.value)} />
          <InputNumber placeholder="Invoice amount (₹)" min={0} value={invoiceAmt} onChange={v => setInvoiceAmt(Number(v))} style={{ width: '100%' }} />
        </Space>
      </Modal>

      <Modal title={`Mark Paid — ${payModal?.poNumber}`} open={!!payModal} onCancel={() => setPayModal(null)} onOk={submitPay} okText="Mark Paid">
        <Input placeholder="NEFT UTR or payment reference" value={payRef} onChange={e => setPayRef(e.target.value)} />
      </Modal>
    </div>
  );
}
