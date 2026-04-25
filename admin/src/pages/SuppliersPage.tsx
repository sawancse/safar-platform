import { useEffect, useMemo, useState } from 'react';
import {
  Table, Tag, Typography, Button, Input, Modal, Form, message, Popconfirm,
  Space, Switch, InputNumber, Select, Drawer, Descriptions, Tabs,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, SafetyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

type SupplierRow = {
  id: string;
  businessName: string;
  ownerName?: string;
  phone: string;
  email?: string;
  whatsapp?: string;
  gst?: string;
  pan?: string;
  bankAccount?: string;
  bankIfsc?: string;
  bankHolder?: string;
  address?: string;
  categories?: string[];
  serviceCities?: string[];
  leadTimeDays?: number;
  paymentTerms?: string;
  creditLimitPaise?: number;
  kycStatus?: 'PENDING' | 'VERIFIED' | 'REJECTED';
  kycNotes?: string;
  ratingAvg?: number;
  ratingCount?: number;
  posCompleted?: number;
  active: boolean;
  notes?: string;
  integrationType?: string;
  integrationConfig?: string;
  catalogSyncedAt?: string;
  createdAt?: string;
};

const INTEGRATION_OPTIONS = [
  { value: 'MANUAL',           label: '✋ Manual (admin-driven, no API)' },
  { value: 'UDAAN',            label: '🛒 Udaan B2B (groceries)' },
  { value: 'FNP',              label: '🌸 FernsNPetals B2B (decor)' },
  { value: 'AMAZON_BUSINESS',  label: '📦 Amazon Business (long-tail)' },
  { value: 'METRO_CASH_CARRY', label: '🏪 Metro C&C (HoReCa)' },
  { value: 'JUMBOTAIL',        label: '🥬 Jumbotail (regional)' },
  { value: 'NINJACART',        label: '🥕 Ninjacart (fresh produce)' },
];

type CatalogRow = {
  id: string;
  itemKey: string;
  itemLabel: string;
  category: string;
  unit: string;
  pricePaise: number;
  moqQty?: number;
  packSize?: number;
  leadTimeDays?: number;
  active: boolean;
};

const CATEGORY_OPTIONS = [
  { value: 'GROCERY',     label: '🥬 Grocery' },
  { value: 'BAKERY',      label: '🎂 Bakery' },
  { value: 'DECOR',       label: '🌸 Decor' },
  { value: 'PG_LINEN',    label: '🛏 PG Linen' },
  { value: 'MAINTENANCE', label: '🔧 Maintenance' },
];

const UNIT_OPTIONS = ['KG', 'GRAM', 'LITRE', 'MILLILITRE', 'PIECE', 'METRE', 'DOZEN', 'PACK', 'SET']
  .map(u => ({ value: u, label: u }));

const PAYMENT_TERMS = ['NET_0', 'NET_7', 'NET_15', 'NET_30'].map(t => ({ value: t, label: t.replace('_', ' Day ') }));

const INDIA_CITIES = [
  'Bengaluru', 'Mumbai', 'Delhi', 'Gurugram', 'Noida', 'Hyderabad', 'Chennai', 'Pune',
  'Kolkata', 'Ahmedabad', 'Jaipur', 'Surat', 'Lucknow', 'Indore', 'Chandigarh',
].map(c => ({ value: c.toLowerCase(), label: c }));

const KYC_COLORS: Record<string, string> = { PENDING: 'orange', VERIFIED: 'green', REJECTED: 'red' };
const INR = (paise?: number) => paise ? `₹${(paise / 100).toLocaleString('en-IN')}` : '—';

export default function SuppliersPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [rows, setRows] = useState<SupplierRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [activeOnly, setActiveOnly] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<SupplierRow | null>(null);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerSupplier, setDrawerSupplier] = useState<SupplierRow | null>(null);
  const [kycNotes, setKycNotes] = useState('');
  const [catalog, setCatalog] = useState<CatalogRow[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [catalogModalOpen, setCatalogModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<CatalogRow | null>(null);
  const [catalogForm] = Form.useForm();

  const load = () => {
    setLoading(true);
    adminApi.listSuppliers(token, activeOnly)
      .then(d => setRows(d || []))
      .catch((e: any) => message.error(e?.response?.data?.message || 'Failed to load suppliers'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [activeOnly]);

  const filtered = useMemo(() => rows.filter(r => {
    if (!search) return true;
    const s = search.toLowerCase();
    return r.businessName.toLowerCase().includes(s)
      || (r.ownerName || '').toLowerCase().includes(s)
      || (r.phone || '').includes(s)
      || (r.categories || []).some(c => c.toLowerCase().includes(s));
  }), [rows, search]);

  const openAdd = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ active: true, kycStatus: 'PENDING', leadTimeDays: 2, paymentTerms: 'NET_15', integrationType: 'MANUAL' });
    setModalOpen(true);
  };
  const openEdit = (row: SupplierRow) => {
    setEditing(row);
    form.setFieldsValue({ ...row, categories: row.categories ?? [], serviceCities: row.serviceCities ?? [] });
    setModalOpen(true);
  };

  const openDrawer = async (row: SupplierRow) => {
    setDrawerSupplier(row);
    setKycNotes(row.kycNotes || '');
    setDrawerOpen(true);
    setCatalogLoading(true);
    try {
      const items = await adminApi.listSupplierCatalog(row.id, token, false);
      setCatalog(items || []);
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed to load catalog');
    } finally {
      setCatalogLoading(false);
    }
  };

  const onSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      if (editing) {
        await adminApi.updateSupplier(editing.id, values, token);
        message.success('Supplier updated');
      } else {
        await adminApi.createSupplier(values, token);
        message.success('Supplier created');
      }
      setModalOpen(false);
      load();
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const onToggleActive = async (row: SupplierRow, value: boolean) => {
    try {
      await adminApi.setSupplierActive(row.id, value, token);
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Update failed');
    }
  };

  const onVerifyKyc = async (verified: boolean) => {
    if (!drawerSupplier) return;
    try {
      await adminApi.verifySupplierKyc(drawerSupplier.id, verified, kycNotes || null, token);
      message.success(`KYC ${verified ? 'verified' : 'rejected'}`);
      setDrawerOpen(false);
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'KYC update failed');
    }
  };

  const onDelete = async (row: SupplierRow) => {
    try {
      await adminApi.deleteSupplier(row.id, token);
      message.success('Supplier deactivated');
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Delete failed');
    }
  };

  const openAddCatalogItem = () => {
    setEditingItem(null);
    catalogForm.resetFields();
    catalogForm.setFieldsValue({ active: true, unit: 'KG' });
    setCatalogModalOpen(true);
  };

  const openEditCatalogItem = (item: CatalogRow) => {
    setEditingItem(item);
    catalogForm.setFieldsValue(item);
    setCatalogModalOpen(true);
  };

  const onSaveCatalogItem = async () => {
    if (!drawerSupplier) return;
    try {
      const values = await catalogForm.validateFields();
      if (editingItem) {
        await adminApi.updateCatalogItem(drawerSupplier.id, editingItem.id, values, token);
        message.success('Catalog item updated');
      } else {
        await adminApi.addCatalogItem(drawerSupplier.id, values, token);
        message.success('Catalog item added');
      }
      setCatalogModalOpen(false);
      const items = await adminApi.listSupplierCatalog(drawerSupplier.id, token, false);
      setCatalog(items || []);
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message || 'Save failed');
    }
  };

  const onDeleteCatalogItem = async (item: CatalogRow) => {
    if (!drawerSupplier) return;
    try {
      await adminApi.deleteCatalogItem(drawerSupplier.id, item.id, token);
      const items = await adminApi.listSupplierCatalog(drawerSupplier.id, token, false);
      setCatalog(items || []);
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Delete failed');
    }
  };

  const columns: ColumnsType<SupplierRow> = [
    {
      title: 'Business',
      dataIndex: 'businessName',
      render: (name, row) => (
        <div>
          <div style={{ fontWeight: 600 }}>{name}</div>
          {row.ownerName && <div style={{ fontSize: 12, color: '#888' }}>{row.ownerName}</div>}
        </div>
      ),
    },
    {
      title: 'Contact',
      render: (_, row) => (
        <div style={{ fontSize: 12 }}>
          <div>📞 {row.phone}</div>
          {row.email && <div>✉ {row.email}</div>}
        </div>
      ),
    },
    {
      title: 'Categories',
      dataIndex: 'categories',
      render: (cats?: string[]) => cats?.length
        ? <Space size={[4, 4]} wrap>{cats.slice(0, 3).map(c => <Tag key={c}>{c}</Tag>)}{cats.length > 3 && <Tag>+{cats.length - 3}</Tag>}</Space>
        : <span style={{ color: '#bbb' }}>—</span>,
    },
    {
      title: 'Cities',
      dataIndex: 'serviceCities',
      render: (cities?: string[]) => cities?.length
        ? <Space size={[4, 4]} wrap>{cities.slice(0, 2).map(c => <Tag key={c}>{c}</Tag>)}{cities.length > 2 && <Tag>+{cities.length - 2}</Tag>}</Space>
        : <Tag color="blue">Anywhere</Tag>,
    },
    {
      title: 'Integration',
      dataIndex: 'integrationType',
      width: 120,
      render: (t?: string) => {
        const meta = INTEGRATION_OPTIONS.find(o => o.value === (t || 'MANUAL'));
        const color = (t && t !== 'MANUAL') ? 'purple' : 'default';
        return <Tag color={color}>{(t || 'MANUAL')}</Tag>;
      },
      filters: INTEGRATION_OPTIONS.map(o => ({ text: o.value, value: o.value })),
      onFilter: (val: any, r) => (r.integrationType || 'MANUAL') === val,
    },
    {
      title: 'KYC',
      dataIndex: 'kycStatus',
      render: (s?: string) => <Tag color={KYC_COLORS[s || 'PENDING']}>{s || 'PENDING'}</Tag>,
    },
    {
      title: 'Terms',
      dataIndex: 'paymentTerms',
      width: 90,
      render: (t?: string) => <Tag>{t || '—'}</Tag>,
    },
    {
      title: 'POs',
      dataIndex: 'posCompleted',
      width: 60,
    },
    {
      title: 'Active',
      dataIndex: 'active',
      width: 70,
      render: (val: boolean, row) => (
        <Switch checked={val} onChange={v => onToggleActive(row, v)} size="small" />
      ),
    },
    {
      title: 'Actions',
      width: 160,
      render: (_, row) => (
        <Space size="small">
          <Button size="small" icon={<EyeOutlined />} onClick={() => openDrawer(row)}>View</Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(row)} />
          <Popconfirm title="Deactivate this supplier?" onConfirm={() => onDelete(row)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const catalogColumns: ColumnsType<CatalogRow> = [
    { title: 'Item key', dataIndex: 'itemKey', render: (k) => <code style={{ fontSize: 11 }}>{k}</code> },
    { title: 'Label', dataIndex: 'itemLabel' },
    { title: 'Category', dataIndex: 'category', render: c => <Tag>{c}</Tag> },
    { title: 'Unit', dataIndex: 'unit' },
    { title: 'Price', dataIndex: 'pricePaise', render: (p, r) => <span>{INR(p)} / {r.unit}</span> },
    { title: 'MOQ', dataIndex: 'moqQty' },
    { title: 'Active', dataIndex: 'active', render: v => v ? <Tag color="green">Yes</Tag> : <Tag>No</Tag> },
    { title: '', width: 100, render: (_, r) => (
      <Space size="small">
        <Button size="small" icon={<EditOutlined />} onClick={() => openEditCatalogItem(r)} />
        <Popconfirm title="Remove this item?" onConfirm={() => onDeleteCatalogItem(r)}>
          <Button size="small" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      </Space>
    )},
  ];

  return (
    <div>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>Suppliers</Title>
            <Text type="secondary">Procurement supply-side. Onboard suppliers we buy FROM (groceries, bakery, decor, linen, maintenance).</Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>Add Supplier</Button>
        </div>

        <Space>
          <Input.Search
            placeholder="Search business / owner / phone / category"
            value={search} onChange={e => setSearch(e.target.value)}
            allowClear style={{ width: 380 }}
          />
          <Switch checked={activeOnly} onChange={setActiveOnly} /> <Text>Active only</Text>
        </Space>

        <Table
          rowKey="id" dataSource={filtered} columns={columns} loading={loading}
          pagination={{ pageSize: 20 }} size="small"
        />
      </Space>

      <Modal
        title={editing ? `Edit ${editing.businessName}` : 'Add Supplier'}
        open={modalOpen} onCancel={() => setModalOpen(false)} onOk={onSave}
        confirmLoading={saving} okText={editing ? 'Update' : 'Create'} width={760} destroyOnClose
      >
        <Form form={form} layout="vertical">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Form.Item label="Business name" name="businessName" rules={[{ required: true }]}>
              <Input placeholder="e.g. FreshKart Wholesale" />
            </Form.Item>
            <Form.Item label="Owner / contact name" name="ownerName">
              <Input placeholder="e.g. Ramesh Kumar" />
            </Form.Item>
            <Form.Item label="Phone" name="phone" rules={[{ required: true }]}>
              <Input placeholder="+91 9XXXXXXXXX" />
            </Form.Item>
            <Form.Item label="WhatsApp" name="whatsapp">
              <Input placeholder="defaults to phone" />
            </Form.Item>
            <Form.Item label="Email" name="email"><Input /></Form.Item>
            <Form.Item label="GST" name="gst"><Input /></Form.Item>
            <Form.Item label="PAN" name="pan"><Input /></Form.Item>
            <Form.Item label="Lead time (days)" name="leadTimeDays">
              <InputNumber min={0} max={60} style={{ width: '100%' }} />
            </Form.Item>
          </div>

          <Form.Item label="Categories supplied" name="categories" rules={[{ required: true }]}>
            <Select mode="multiple" options={CATEGORY_OPTIONS} placeholder="What does this supplier sell?" />
          </Form.Item>

          <Form.Item label="Service cities" name="serviceCities" extra="Empty = delivers anywhere">
            <Select mode="tags" options={INDIA_CITIES} placeholder="bengaluru, mumbai, …" tokenSeparators={[',']} />
          </Form.Item>

          <Form.Item label="Address" name="address">
            <Input.TextArea rows={2} />
          </Form.Item>

          <Title level={5}>Bank (for NEFT payouts)</Title>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
            <Form.Item label="Account" name="bankAccount"><Input /></Form.Item>
            <Form.Item label="IFSC" name="bankIfsc"><Input /></Form.Item>
            <Form.Item label="Holder" name="bankHolder"><Input /></Form.Item>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
            <Form.Item label="Payment terms" name="paymentTerms">
              <Select options={PAYMENT_TERMS} />
            </Form.Item>
            <Form.Item label="Credit limit (₹)" name="creditLimitPaise" extra="Stored in paise">
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="KYC status" name="kycStatus">
              <Select options={['PENDING', 'VERIFIED', 'REJECTED'].map(v => ({ value: v, label: v }))} />
            </Form.Item>
          </div>

          <Title level={5} style={{ marginTop: 8 }}>Integration</Title>
          <Form.Item label="Integration type" name="integrationType"
                     extra="MANUAL = no API. Other types require sandbox creds + supply.adapters.enabled=true on the backend.">
            <Select options={INTEGRATION_OPTIONS} />
          </Form.Item>
          <Form.Item label="Integration config (JSON)" name="integrationConfig"
                     extra={'Per-supplier API credentials path / IDs. Example for Udaan: {"accountNumber":"UDN-123","warehouseCode":"BLR-WH-01"}. Leave empty for MANUAL.'}>
            <Input.TextArea rows={3} placeholder='{"accountNumber":"UDN-...", "warehouseCode":"..."}' />
          </Form.Item>

          <Form.Item label="Admin notes" name="notes">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="Active" name="active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer title={drawerSupplier?.businessName} open={drawerOpen} onClose={() => setDrawerOpen(false)} width={720}>
        {drawerSupplier && (
          <Tabs items={[
            {
              key: 'info', label: 'Info',
              children: (
                <Space direction="vertical" size={16} style={{ width: '100%' }}>
                  <Descriptions column={1} size="small" bordered>
                    <Descriptions.Item label="Owner">{drawerSupplier.ownerName || '—'}</Descriptions.Item>
                    <Descriptions.Item label="Phone">{drawerSupplier.phone}</Descriptions.Item>
                    <Descriptions.Item label="Email">{drawerSupplier.email || '—'}</Descriptions.Item>
                    <Descriptions.Item label="GST / PAN">{drawerSupplier.gst || '—'} / {drawerSupplier.pan || '—'}</Descriptions.Item>
                    <Descriptions.Item label="Bank">
                      {drawerSupplier.bankHolder ? `${drawerSupplier.bankHolder} · ${drawerSupplier.bankAccount} · ${drawerSupplier.bankIfsc}` : '—'}
                    </Descriptions.Item>
                    <Descriptions.Item label="Categories">{drawerSupplier.categories?.join(', ') || '—'}</Descriptions.Item>
                    <Descriptions.Item label="Cities">{drawerSupplier.serviceCities?.length ? drawerSupplier.serviceCities.join(', ') : 'Anywhere'}</Descriptions.Item>
                    <Descriptions.Item label="Lead time">{drawerSupplier.leadTimeDays} days</Descriptions.Item>
                    <Descriptions.Item label="Payment terms">{drawerSupplier.paymentTerms}</Descriptions.Item>
                    <Descriptions.Item label="POs completed">{drawerSupplier.posCompleted || 0}</Descriptions.Item>
                    <Descriptions.Item label="Integration">
                      <Tag color={drawerSupplier.integrationType && drawerSupplier.integrationType !== 'MANUAL' ? 'purple' : 'default'}>
                        {drawerSupplier.integrationType || 'MANUAL'}
                      </Tag>
                    </Descriptions.Item>
                    {drawerSupplier.integrationConfig && (
                      <Descriptions.Item label="Integration config"><code style={{ fontSize: 11 }}>{drawerSupplier.integrationConfig}</code></Descriptions.Item>
                    )}
                    {drawerSupplier.catalogSyncedAt && (
                      <Descriptions.Item label="Catalog last synced">{new Date(drawerSupplier.catalogSyncedAt).toLocaleString()}</Descriptions.Item>
                    )}
                  </Descriptions>

                  <div>
                    <Title level={5}>KYC</Title>
                    <Tag color={KYC_COLORS[drawerSupplier.kycStatus || 'PENDING']}>{drawerSupplier.kycStatus || 'PENDING'}</Tag>
                    <Input.TextArea rows={2} value={kycNotes} onChange={e => setKycNotes(e.target.value)}
                      placeholder="Verification notes" style={{ marginTop: 8 }} />
                    <Space style={{ marginTop: 8 }}>
                      <Button type="primary" icon={<SafetyOutlined />} onClick={() => onVerifyKyc(true)}>Verify</Button>
                      <Button danger onClick={() => onVerifyKyc(false)}>Reject</Button>
                    </Space>
                  </div>
                </Space>
              ),
            },
            {
              key: 'catalog', label: `Catalog (${catalog.length})`,
              children: (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openAddCatalogItem}>Add Item</Button>
                  </div>
                  <Table rowKey="id" dataSource={catalog} columns={catalogColumns} loading={catalogLoading}
                    pagination={false} size="small" />
                </div>
              ),
            },
          ]} />
        )}
      </Drawer>

      <Modal
        title={editingItem ? `Edit ${editingItem.itemLabel}` : 'Add Catalog Item'}
        open={catalogModalOpen} onCancel={() => setCatalogModalOpen(false)} onOk={onSaveCatalogItem}
        okText={editingItem ? 'Update' : 'Add'} destroyOnClose
      >
        <Form form={catalogForm} layout="vertical">
          <Form.Item label="Item key" name="itemKey" rules={[{ required: true }]}
                     extra="Canonical, lowercase, snake_case (e.g. flour_maida, marigold_garland)">
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
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
            <Form.Item label="Price (paise)" name="pricePaise" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="MOQ" name="moqQty">
              <InputNumber min={0} step={0.5} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Pack size" name="packSize">
              <InputNumber min={0} step={0.5} style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item label="Lead time override (days)" name="leadTimeDays"><InputNumber min={0} max={60} style={{ width: '100%' }} /></Form.Item>
          <Form.Item label="Notes" name="notes"><Input.TextArea rows={2} /></Form.Item>
          <Form.Item label="Active" name="active" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
