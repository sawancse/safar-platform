import { useEffect, useMemo, useState } from 'react';
import {
  Table, Tag, Typography, Button, Input, Modal, Form, message, Popconfirm,
  Space, Switch, InputNumber, Select, Tabs, Drawer, Descriptions, Upload, Image, Rate,
} from 'antd';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, UploadOutlined, EyeOutlined, SafetyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

type VendorRow = {
  id: string;
  serviceType: string;
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
  serviceCities?: string[];
  serviceRadiusKm?: number;
  portfolioJson?: string;
  pricingOverrideJson?: string;
  kycStatus?: 'PENDING' | 'VERIFIED' | 'REJECTED';
  kycNotes?: string;
  ratingAvg?: number;
  ratingCount?: number;
  jobsCompleted?: number;
  active: boolean;
  notes?: string;
  createdAt?: string;
};

const SERVICE_TYPES: { key: string; label: string; icon: string }[] = [
  { key: 'CAKE_DESIGNER',    label: 'Cake Designers', icon: '🎂' },
  { key: 'EVENT_DECOR',      label: 'Decorators',     icon: '🌸' },
  { key: 'PANDIT_PUJA',      label: 'Pandits',        icon: '🪔' },
  { key: 'LIVE_MUSIC',       label: 'Singers / Bands',icon: '🎺' },
  { key: 'APPLIANCE_RENTAL', label: 'Appliances',     icon: '🍳' },
  { key: 'STAFF_HIRE',       label: 'Staff Vendors',  icon: '🧑‍🍳' },
];

const INDIA_CITIES = [
  'Bengaluru', 'Mumbai', 'Delhi', 'Gurugram', 'Noida', 'Hyderabad', 'Chennai',
  'Pune', 'Kolkata', 'Ahmedabad', 'Jaipur', 'Surat', 'Lucknow', 'Indore',
  'Chandigarh', 'Bhopal', 'Vadodara', 'Coimbatore', 'Kochi', 'Visakhapatnam',
].map(c => ({ value: c.toLowerCase(), label: c }));

const KYC_COLORS: Record<string, string> = {
  PENDING: 'orange',
  VERIFIED: 'green',
  REJECTED: 'red',
};

export default function PartnerVendorsPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [activeTab, setActiveTab] = useState<string>('CAKE_DESIGNER');
  const [rows, setRows] = useState<VendorRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [activeOnly, setActiveOnly] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<VendorRow | null>(null);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [portfolioPhotos, setPortfolioPhotos] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerVendor, setDrawerVendor] = useState<VendorRow | null>(null);
  const [kycNotes, setKycNotes] = useState('');

  const load = () => {
    setLoading(true);
    adminApi.listVendors(token, activeTab, activeOnly)
      .then(data => setRows(data || []))
      .catch((e: any) => message.error(e?.response?.data?.message || 'Failed to load vendors'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [activeTab, activeOnly]);

  const filtered = useMemo(() => rows.filter(r => {
    if (!search) return true;
    const s = search.toLowerCase();
    return r.businessName.toLowerCase().includes(s)
      || (r.ownerName || '').toLowerCase().includes(s)
      || (r.phone || '').includes(s)
      || (r.serviceCities || []).some(c => c.includes(s));
  }), [rows, search]);

  const openAdd = () => {
    setEditing(null);
    setPortfolioPhotos([]);
    form.resetFields();
    form.setFieldsValue({ serviceType: activeTab, active: true, kycStatus: 'PENDING', serviceRadiusKm: 25 });
    setModalOpen(true);
  };

  const openEdit = (row: VendorRow) => {
    setEditing(row);
    let photos: string[] = [];
    try {
      if (row.portfolioJson) {
        const parsed = JSON.parse(row.portfolioJson);
        if (Array.isArray(parsed?.photos)) photos = parsed.photos;
      }
    } catch { /* ignore parse errors */ }
    setPortfolioPhotos(photos);
    form.setFieldsValue({
      ...row,
      serviceCities: row.serviceCities ?? [],
    });
    setModalOpen(true);
  };

  const openDrawer = (row: VendorRow) => {
    setDrawerVendor(row);
    setKycNotes(row.kycNotes || '');
    setDrawerOpen(true);
  };

  const onSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const portfolio = portfolioPhotos.length ? JSON.stringify({ photos: portfolioPhotos }) : null;
      const payload = {
        ...values,
        portfolioJson: portfolio,
        // pricingOverrideJson left as raw string from form (advanced field)
      };
      if (editing) {
        await adminApi.updateVendor(editing.id, payload, token);
        message.success('Vendor updated');
      } else {
        await adminApi.createVendor(payload, token);
        message.success('Vendor created');
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

  const onToggleActive = async (row: VendorRow, value: boolean) => {
    try {
      await adminApi.setVendorActive(row.id, value, token);
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Update failed');
    }
  };

  const onVerifyKyc = async (verified: boolean) => {
    if (!drawerVendor) return;
    try {
      await adminApi.verifyVendorKyc(drawerVendor.id, verified, kycNotes || null, token);
      message.success(`KYC ${verified ? 'verified' : 'rejected'}`);
      setDrawerOpen(false);
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'KYC update failed');
    }
  };

  const onDelete = async (row: VendorRow) => {
    try {
      await adminApi.deleteVendor(row.id, token);
      message.success('Vendor deactivated');
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Delete failed');
    }
  };

  const handleUpload = async (file: File) => {
    setUploading(true);
    try {
      const url = await adminApi.uploadFile(file, 'vendor-portfolio', token);
      setPortfolioPhotos(prev => [...prev, url]);
      message.success('Photo uploaded');
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
    return false;
  };

  const columns: ColumnsType<VendorRow> = [
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
      title: 'Cities',
      dataIndex: 'serviceCities',
      render: (cities?: string[]) => cities?.length
        ? <Space size={[4, 4]} wrap>{cities.slice(0, 3).map(c => <Tag key={c}>{c}</Tag>)}{cities.length > 3 && <Tag>+{cities.length - 3}</Tag>}</Space>
        : <Tag color="blue">Anywhere</Tag>,
    },
    {
      title: 'KYC',
      dataIndex: 'kycStatus',
      render: (s?: string) => <Tag color={KYC_COLORS[s || 'PENDING']}>{s || 'PENDING'}</Tag>,
      filters: [
        { text: 'Verified', value: 'VERIFIED' },
        { text: 'Pending',  value: 'PENDING' },
        { text: 'Rejected', value: 'REJECTED' },
      ],
      onFilter: (val, row) => row.kycStatus === val,
    },
    {
      title: 'Rating',
      dataIndex: 'ratingAvg',
      render: (avg?: number, row?) => avg
        ? <span>⭐ {Number(avg).toFixed(1)} <span style={{ color: '#888', fontSize: 11 }}>({row?.ratingCount})</span></span>
        : <span style={{ color: '#bbb' }}>—</span>,
      sorter: (a, b) => (a.ratingAvg || 0) - (b.ratingAvg || 0),
    },
    {
      title: 'Jobs',
      dataIndex: 'jobsCompleted',
      width: 70,
      sorter: (a, b) => (a.jobsCompleted || 0) - (b.jobsCompleted || 0),
    },
    {
      title: 'Active',
      dataIndex: 'active',
      width: 80,
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
          <Popconfirm title="Deactivate this vendor?" onConfirm={() => onDelete(row)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>Partner Vendors</Title>
            <Text type="secondary">Onboard cake designers, decorators, pandits, singers, appliance providers, and staff vendors. Admin-only — vendors don't self-serve in this phase.</Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>Add Vendor</Button>
        </div>

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={SERVICE_TYPES.map(s => ({
            key: s.key,
            label: <span>{s.icon} {s.label}</span>,
          }))}
        />

        <Space>
          <Input.Search
            placeholder="Search business / owner / phone / city"
            value={search}
            onChange={e => setSearch(e.target.value)}
            allowClear
            style={{ width: 380 }}
          />
          <Switch checked={activeOnly} onChange={setActiveOnly} /> <Text>Active only</Text>
        </Space>

        <Table
          rowKey="id"
          dataSource={filtered}
          columns={columns}
          loading={loading}
          pagination={{ pageSize: 20 }}
          size="small"
        />
      </Space>

      <Modal
        title={editing ? `Edit ${editing.businessName}` : 'Add Vendor'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={onSave}
        confirmLoading={saving}
        okText={editing ? 'Update' : 'Create'}
        width={760}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item label="Service type" name="serviceType" rules={[{ required: true }]}>
            <Select options={SERVICE_TYPES.map(s => ({ value: s.key, label: `${s.icon} ${s.label}` }))} disabled={!!editing} />
          </Form.Item>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Form.Item label="Business name" name="businessName" rules={[{ required: true }]}>
              <Input placeholder="e.g. Sweet Bites Bakery" />
            </Form.Item>
            <Form.Item label="Owner / contact name" name="ownerName">
              <Input placeholder="e.g. Priya Sharma" />
            </Form.Item>
            <Form.Item label="Phone" name="phone" rules={[{ required: true }]}>
              <Input placeholder="+91 9XXXXXXXXX" />
            </Form.Item>
            <Form.Item label="WhatsApp (optional)" name="whatsapp">
              <Input placeholder="defaults to phone" />
            </Form.Item>
            <Form.Item label="Email" name="email">
              <Input placeholder="hello@vendor.com" />
            </Form.Item>
            <Form.Item label="GST (optional)" name="gst">
              <Input placeholder="22ABCDE1234F1Z5" />
            </Form.Item>
            <Form.Item label="PAN" name="pan">
              <Input placeholder="ABCDE1234F" />
            </Form.Item>
            <Form.Item label="Service radius (km)" name="serviceRadiusKm">
              <InputNumber min={1} max={500} style={{ width: '100%' }} />
            </Form.Item>
          </div>

          <Form.Item label="Service cities" name="serviceCities" extra="Empty = serves anywhere. Pick all cities the vendor covers.">
            <Select mode="tags" options={INDIA_CITIES} placeholder="bengaluru, mumbai, …" tokenSeparators={[',']} />
          </Form.Item>

          <Form.Item label="Address" name="address">
            <Input.TextArea rows={2} placeholder="Full business address" />
          </Form.Item>

          <Title level={5} style={{ marginTop: 8 }}>Bank (for manual NEFT payout)</Title>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
            <Form.Item label="Account number" name="bankAccount"><Input /></Form.Item>
            <Form.Item label="IFSC" name="bankIfsc"><Input placeholder="HDFC0001234" /></Form.Item>
            <Form.Item label="Account holder" name="bankHolder"><Input /></Form.Item>
          </div>

          <Title level={5} style={{ marginTop: 8 }}>Portfolio</Title>
          <Upload beforeUpload={handleUpload} showUploadList={false} accept="image/*" multiple>
            <Button icon={<UploadOutlined />} loading={uploading}>Upload photo</Button>
          </Upload>
          {portfolioPhotos.length > 0 && (
            <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
              {portfolioPhotos.map((url, i) => (
                <div key={url} style={{ position: 'relative' }}>
                  <Image src={url} width={80} height={80} style={{ objectFit: 'cover', borderRadius: 4 }} />
                  <Button
                    danger size="small" type="primary"
                    onClick={() => setPortfolioPhotos(p => p.filter(x => x !== url))}
                    style={{ position: 'absolute', top: 2, right: 2, padding: '0 4px', height: 18, fontSize: 10 }}
                  >×</Button>
                </div>
              ))}
            </div>
          )}

          <Form.Item label="Pricing override JSON (advanced)" name="pricingOverrideJson" style={{ marginTop: 12 }}
                     extra="Leave blank to use platform defaults from event_pricing_defaults.">
            <Input.TextArea rows={2} placeholder='{"basePaise": 800000, "perKgPaise": 100000}' />
          </Form.Item>

          <Form.Item label="Admin notes" name="notes">
            <Input.TextArea rows={2} placeholder="Anything other admins should know" />
          </Form.Item>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Form.Item label="KYC status" name="kycStatus">
              <Select options={['PENDING','VERIFIED','REJECTED'].map(v => ({ value: v, label: v }))} />
            </Form.Item>
            <Form.Item label="Active" name="active" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Drawer
        title={drawerVendor?.businessName}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={520}
      >
        {drawerVendor && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="Service">{drawerVendor.serviceType}</Descriptions.Item>
              <Descriptions.Item label="Owner">{drawerVendor.ownerName || '—'}</Descriptions.Item>
              <Descriptions.Item label="Phone">{drawerVendor.phone}</Descriptions.Item>
              <Descriptions.Item label="WhatsApp">{drawerVendor.whatsapp || drawerVendor.phone}</Descriptions.Item>
              <Descriptions.Item label="Email">{drawerVendor.email || '—'}</Descriptions.Item>
              <Descriptions.Item label="GST / PAN">{drawerVendor.gst || '—'} / {drawerVendor.pan || '—'}</Descriptions.Item>
              <Descriptions.Item label="Bank">
                {drawerVendor.bankHolder ? `${drawerVendor.bankHolder} · ${drawerVendor.bankAccount} · ${drawerVendor.bankIfsc}` : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Cities">
                {drawerVendor.serviceCities?.length ? drawerVendor.serviceCities.join(', ') : 'Anywhere'}
              </Descriptions.Item>
              <Descriptions.Item label="Radius">{drawerVendor.serviceRadiusKm} km</Descriptions.Item>
              <Descriptions.Item label="Rating">
                {drawerVendor.ratingAvg
                  ? <><Rate disabled allowHalf value={drawerVendor.ratingAvg} style={{ fontSize: 14 }} /> {Number(drawerVendor.ratingAvg).toFixed(2)} ({drawerVendor.ratingCount})</>
                  : 'No ratings yet'}
              </Descriptions.Item>
              <Descriptions.Item label="Jobs done">{drawerVendor.jobsCompleted || 0}</Descriptions.Item>
              <Descriptions.Item label="Notes">{drawerVendor.notes || '—'}</Descriptions.Item>
            </Descriptions>

            <div>
              <Title level={5}>KYC</Title>
              <Tag color={KYC_COLORS[drawerVendor.kycStatus || 'PENDING']}>{drawerVendor.kycStatus || 'PENDING'}</Tag>
              <Input.TextArea
                rows={2}
                value={kycNotes}
                onChange={e => setKycNotes(e.target.value)}
                placeholder="Notes for the verification decision"
                style={{ marginTop: 8 }}
              />
              <Space style={{ marginTop: 8 }}>
                <Button type="primary" icon={<SafetyOutlined />} onClick={() => onVerifyKyc(true)}>Verify</Button>
                <Button danger onClick={() => onVerifyKyc(false)}>Reject</Button>
              </Space>
            </div>
          </Space>
        )}
      </Drawer>
    </div>
  );
}
