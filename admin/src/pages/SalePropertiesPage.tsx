import { useState, useEffect } from 'react';
import { Table, Tag, Button, Space, Select, Modal, message, Card, Row, Col, Statistic, Input, Tabs } from 'antd';
import { CheckCircleOutlined, StopOutlined, SafetyCertificateOutlined, SearchOutlined, EyeOutlined } from '@ant-design/icons';
import axios from 'axios';

const API = '/api/v1/sale-properties';

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'default', ACTIVE: 'green', SOLD: 'blue', EXPIRED: 'orange', SUSPENDED: 'red',
};

const TYPE_LABELS: Record<string, string> = {
  APARTMENT: 'Apartment', INDEPENDENT_HOUSE: 'House', VILLA: 'Villa', PLOT: 'Plot',
  PENTHOUSE: 'Penthouse', STUDIO: 'Studio', BUILDER_FLOOR: 'Builder Floor',
  FARM_HOUSE: 'Farm House', ROW_HOUSE: 'Row House',
  COMMERCIAL_OFFICE: 'Office', COMMERCIAL_SHOP: 'Shop',
  COMMERCIAL_SHOWROOM: 'Showroom', COMMERCIAL_WAREHOUSE: 'Warehouse', INDUSTRIAL: 'Industrial',
};

function formatPrice(paise: number): string {
  const inr = paise / 100;
  if (inr >= 10000000) return `₹${(inr / 10000000).toFixed(2)} Cr`;
  if (inr >= 100000) return `₹${(inr / 100000).toFixed(1)} Lakh`;
  return `₹${inr.toLocaleString('en-IN')}`;
}

export default function SalePropertiesPage() {
  const [properties, setProperties] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [stats, setStats] = useState({ total: 0, active: 0, draft: 0, sold: 0, suspended: 0 });
  const [detailModal, setDetailModal] = useState<any>(null);
  const token = localStorage.getItem('admin_token') ?? '';

  const headers = { Authorization: `Bearer ${token}` };

  useEffect(() => { loadData(); }, [statusFilter]);

  async function loadData() {
    setLoading(true);
    try {
      const qs = statusFilter ? `?status=${statusFilter}&size=100` : '?size=100';
      const res = await axios.get(`${API}/admin/list${qs}`, { headers });
      const items = res.data.content || [];
      setProperties(items);
      setStats({
        total: items.length,
        active: items.filter((p: any) => p.status === 'ACTIVE').length,
        draft: items.filter((p: any) => p.status === 'DRAFT').length,
        sold: items.filter((p: any) => p.status === 'SOLD').length,
        suspended: items.filter((p: any) => p.status === 'SUSPENDED').length,
      });
    } catch (e) { message.error('Failed to load properties'); }
    setLoading(false);
  }

  async function verify(id: string) {
    await axios.post(`${API}/${id}/verify`, {}, { headers });
    message.success('Property verified');
    loadData();
  }

  async function verifyRera(id: string) {
    await axios.post(`${API}/${id}/verify-rera`, {}, { headers });
    message.success('RERA verified');
    loadData();
  }

  async function suspend(id: string) {
    Modal.confirm({
      title: 'Suspend this property?',
      content: 'The property will be removed from search results.',
      onOk: async () => {
        await axios.post(`${API}/${id}/suspend`, {}, { headers });
        message.success('Property suspended');
        loadData();
      },
    });
  }

  const columns = [
    {
      title: 'Title', dataIndex: 'title', key: 'title',
      render: (t: string, r: any) => <a onClick={() => setDetailModal(r)}>{t}</a>,
    },
    {
      title: 'Type', dataIndex: 'salePropertyType', key: 'type',
      render: (t: string) => TYPE_LABELS[t] || t,
    },
    { title: 'City', dataIndex: 'city', key: 'city' },
    { title: 'Locality', dataIndex: 'locality', key: 'locality' },
    {
      title: 'Price', dataIndex: 'askingPricePaise', key: 'price',
      render: (p: number) => formatPrice(p),
      sorter: (a: any, b: any) => a.askingPricePaise - b.askingPricePaise,
    },
    { title: 'BHK', dataIndex: 'bedrooms', key: 'bhk' },
    {
      title: 'Area', dataIndex: 'carpetAreaSqft', key: 'area',
      render: (a: number) => a ? `${a} sqft` : '—',
    },
    {
      title: 'Seller', dataIndex: 'sellerType', key: 'seller',
      render: (t: string) => <Tag>{t}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={STATUS_COLORS[s]}>{s}</Tag>,
    },
    {
      title: 'Verified', key: 'verified',
      render: (_: any, r: any) => (
        <Space>
          {r.verified && <Tag color="green">Verified</Tag>}
          {r.reraVerified && <Tag color="blue">RERA ✓</Tag>}
        </Space>
      ),
    },
    {
      title: 'Stats', key: 'stats',
      render: (_: any, r: any) => `${r.viewsCount} views · ${r.inquiriesCount} inquiries`,
    },
    {
      title: 'Actions', key: 'actions',
      render: (_: any, r: any) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />} onClick={() => setDetailModal(r)} />
          {!r.verified && (
            <Button size="small" type="primary" icon={<CheckCircleOutlined />} onClick={() => verify(r.id)}>
              Verify
            </Button>
          )}
          {r.reraId && !r.reraVerified && (
            <Button size="small" icon={<SafetyCertificateOutlined />} onClick={() => verifyRera(r.id)}>
              RERA
            </Button>
          )}
          {r.status !== 'SUSPENDED' && (
            <Button size="small" danger icon={<StopOutlined />} onClick={() => suspend(r.id)}>
              Suspend
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Sale Properties</h2>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={5}><Card><Statistic title="Total" value={stats.total} /></Card></Col>
        <Col span={5}><Card><Statistic title="Active" value={stats.active} valueStyle={{ color: '#3f8600' }} /></Card></Col>
        <Col span={5}><Card><Statistic title="Draft" value={stats.draft} /></Card></Col>
        <Col span={5}><Card><Statistic title="Sold" value={stats.sold} valueStyle={{ color: '#1890ff' }} /></Card></Col>
        <Col span={4}><Card><Statistic title="Suspended" value={stats.suspended} valueStyle={{ color: '#cf1322' }} /></Card></Col>
      </Row>

      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder="Filter by status" allowClear style={{ width: 200 }}
          onChange={v => setStatusFilter(v)} value={statusFilter}
          options={[
            { label: 'All', value: undefined },
            { label: 'Draft', value: 'DRAFT' },
            { label: 'Active', value: 'ACTIVE' },
            { label: 'Sold', value: 'SOLD' },
            { label: 'Expired', value: 'EXPIRED' },
            { label: 'Suspended', value: 'SUSPENDED' },
          ]}
        />
        <Button onClick={loadData}>Refresh</Button>
      </Space>

      <Table
        columns={columns} dataSource={properties} loading={loading}
        rowKey="id" size="small" scroll={{ x: 1400 }}
        pagination={{ pageSize: 20, showSizeChanger: true }}
      />

      <Modal
        open={!!detailModal} onCancel={() => setDetailModal(null)}
        width={800} footer={null} title={detailModal?.title}
      >
        {detailModal && (
          <Tabs items={[
            {
              key: 'overview', label: 'Overview',
              children: (
                <div>
                  <Row gutter={[16, 8]}>
                    <Col span={8}><strong>Type:</strong> {TYPE_LABELS[detailModal.salePropertyType]}</Col>
                    <Col span={8}><strong>Price:</strong> {formatPrice(detailModal.askingPricePaise)}</Col>
                    <Col span={8}><strong>Price/sqft:</strong> {detailModal.pricePerSqftPaise ? formatPrice(detailModal.pricePerSqftPaise) : '—'}</Col>
                    <Col span={8}><strong>BHK:</strong> {detailModal.bedrooms || '—'}</Col>
                    <Col span={8}><strong>Bathrooms:</strong> {detailModal.bathrooms || '—'}</Col>
                    <Col span={8}><strong>Carpet Area:</strong> {detailModal.carpetAreaSqft ? `${detailModal.carpetAreaSqft} sqft` : '—'}</Col>
                    <Col span={8}><strong>Floor:</strong> {detailModal.floorNumber || '—'} / {detailModal.totalFloors || '—'}</Col>
                    <Col span={8}><strong>Facing:</strong> {detailModal.facing || '—'}</Col>
                    <Col span={8}><strong>Furnishing:</strong> {detailModal.furnishing || '—'}</Col>
                    <Col span={8}><strong>Possession:</strong> {detailModal.possessionStatus}</Col>
                    <Col span={8}><strong>Seller:</strong> {detailModal.sellerType}</Col>
                    <Col span={8}><strong>Transaction:</strong> {detailModal.transactionType}</Col>
                  </Row>
                  <h4 style={{ marginTop: 16 }}>Location</h4>
                  <p>{detailModal.addressLine1} {detailModal.addressLine2}, {detailModal.locality}, {detailModal.city}, {detailModal.state} - {detailModal.pincode}</p>
                  {detailModal.description && <><h4>Description</h4><p>{detailModal.description}</p></>}
                </div>
              ),
            },
            {
              key: 'legal', label: 'Legal / RERA',
              children: (
                <div>
                  <p><strong>RERA ID:</strong> {detailModal.reraId || 'Not provided'}</p>
                  <p><strong>RERA Verified:</strong> {detailModal.reraVerified ? '✅ Yes' : '❌ No'}</p>
                  <p><strong>Builder:</strong> {detailModal.builderName || '—'}</p>
                  <p><strong>Project:</strong> {detailModal.projectName || '—'}</p>
                  <p><strong>Safar Verified:</strong> {detailModal.verified ? '✅ Yes' : '❌ No'}</p>
                  <Space style={{ marginTop: 16 }}>
                    {!detailModal.verified && <Button type="primary" onClick={() => { verify(detailModal.id); setDetailModal(null); }}>Verify Property</Button>}
                    {detailModal.reraId && !detailModal.reraVerified && <Button onClick={() => { verifyRera(detailModal.id); setDetailModal(null); }}>Verify RERA</Button>}
                  </Space>
                </div>
              ),
            },
            {
              key: 'stats', label: 'Statistics',
              children: (
                <Row gutter={[16, 16]}>
                  <Col span={6}><Statistic title="Views" value={detailModal.viewsCount} /></Col>
                  <Col span={6}><Statistic title="Inquiries" value={detailModal.inquiriesCount} /></Col>
                  <Col span={12}><Statistic title="Created" value={new Date(detailModal.createdAt).toLocaleDateString()} /></Col>
                </Row>
              ),
            },
          ]} />
        )}
      </Modal>
    </div>
  );
}
