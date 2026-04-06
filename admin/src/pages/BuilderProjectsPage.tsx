import { useState, useEffect } from 'react';
import { Table, Tag, Button, Space, Select, Modal, message, Card, Row, Col, Statistic, Tabs, Progress, Input, DatePicker } from 'antd';
import { CheckCircleOutlined, SafetyCertificateOutlined, EyeOutlined, SearchOutlined } from '@ant-design/icons';
import { adminApi as api } from '../lib/api';

const { RangePicker } = DatePicker;

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'default', ACTIVE: 'green', SOLD_OUT: 'blue', SUSPENDED: 'red',
};

const PROJECT_STATUS_LABELS: Record<string, string> = {
  UPCOMING: 'Upcoming', UNDER_CONSTRUCTION: 'Under Construction',
  READY_TO_MOVE: 'Ready to Move', COMPLETED: 'Completed',
};

function formatPrice(paise: number): string {
  if (!paise) return '--';
  const inr = paise / 100;
  if (inr >= 10000000) return `₹${(inr / 10000000).toFixed(2)} Cr`;
  if (inr >= 100000) return `₹${(inr / 100000).toFixed(1)} Lakh`;
  return `₹${inr.toLocaleString('en-IN')}`;
}

export default function BuilderProjectsPage() {
  const [projects, setProjects] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [cityFilter, setCityFilter] = useState('');
  const [localityFilter, setLocalityFilter] = useState('');
  const [searchFilter, setSearchFilter] = useState('');
  const [verifiedFilter, setVerifiedFilter] = useState<boolean | undefined>(undefined);
  const [dateRange, setDateRange] = useState<[string, string] | null>(null);
  const [stats, setStats] = useState({ total: 0, active: 0, draft: 0, suspended: 0 });
  const [detailModal, setDetailModal] = useState<any>(null);
  const token = localStorage.getItem('admin_token') ?? '';

  useEffect(() => { loadData(); }, [statusFilter, cityFilter, verifiedFilter]);

  async function loadData() {
    setLoading(true);
    try {
      const params: Record<string, any> = {};
      if (statusFilter) params.status = statusFilter;
      if (cityFilter) params.city = cityFilter;
      if (localityFilter) params.locality = localityFilter;
      if (searchFilter) params.search = searchFilter;
      if (verifiedFilter !== undefined) params.verified = verifiedFilter;
      if (dateRange) { params.dateFrom = dateRange[0]; params.dateTo = dateRange[1]; }
      const res = await api.getBuilderProjects(token, params);
      const items = res.content || [];
      setProjects(items);
      setStats({
        total: items.length,
        active: items.filter((p: any) => p.status === 'ACTIVE').length,
        draft: items.filter((p: any) => p.status === 'DRAFT').length,
        suspended: items.filter((p: any) => p.status === 'SUSPENDED').length,
      });
    } catch { message.error('Failed to load builder projects'); }
    setLoading(false);
  }

  async function verify(id: string) {
    await api.verifyBuilderProject(id, token);
    message.success('Project verified');
    loadData();
  }

  async function verifyRera(id: string) {
    await api.verifyBuilderRera(id, token);
    message.success('RERA verified');
    loadData();
  }

  const columns = [
    {
      title: 'Project', dataIndex: 'projectName', key: 'name',
      render: (t: string, r: any) => (
        <a onClick={() => setDetailModal(r)}>
          <div>{t}</div>
          <div style={{ fontSize: 12, color: '#888' }}>{r.builderName}</div>
        </a>
      ),
    },
    {
      title: 'Host / Contact', key: 'contact',
      width: 200,
      render: (_: any, r: any) => (
        <div style={{ fontSize: 12 }}>
          {r.hostName && <div style={{ fontWeight: 500 }}>{r.hostName}</div>}
          {r.builderPhone && <div>📞 {r.builderPhone}</div>}
          {r.builderEmail && <div style={{ color: '#888' }}>✉ {r.builderEmail}</div>}
          {!r.hostName && !r.builderPhone && !r.builderEmail && (
            <span style={{ color: '#ccc' }}>No contact (ID: {r.builderId?.substring(0, 8)})</span>
          )}
        </div>
      ),
    },
    { title: 'City', dataIndex: 'city', key: 'city' },
    { title: 'Locality', dataIndex: 'locality', key: 'locality' },
    {
      title: 'Price Range', key: 'price',
      render: (_: any, r: any) => r.minPricePaise
        ? `${formatPrice(r.minPricePaise)} - ${formatPrice(r.maxPricePaise)}`
        : '--',
    },
    {
      title: 'Units', key: 'units',
      render: (_: any, r: any) => `${r.availableUnits ?? 0}/${r.totalUnits ?? 0}`,
    },
    {
      title: 'Progress', dataIndex: 'constructionProgressPercent', key: 'progress',
      render: (p: number) => <Progress percent={p || 0} size="small" />,
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
          {r.reraVerified && <Tag color="blue">RERA</Tag>}
        </Space>
      ),
    },
    {
      title: 'Created', dataIndex: 'createdAt', key: 'created',
      render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '--',
      sorter: (a: any, b: any) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
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
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Builder Projects</h2>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}><Card><Statistic title="Total" value={stats.total} /></Card></Col>
        <Col span={6}><Card><Statistic title="Active" value={stats.active} valueStyle={{ color: '#3f8600' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="Draft" value={stats.draft} /></Card></Col>
        <Col span={6}><Card><Statistic title="Suspended" value={stats.suspended} valueStyle={{ color: '#cf1322' }} /></Card></Col>
      </Row>

      <div style={{ display: 'flex', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          placeholder="Status" allowClear style={{ width: 140 }}
          onChange={v => setStatusFilter(v)} value={statusFilter}
          options={[
            { label: 'All', value: undefined },
            { label: 'Draft', value: 'DRAFT' },
            { label: 'Active', value: 'ACTIVE' },
            { label: 'Sold Out', value: 'SOLD_OUT' },
            { label: 'Suspended', value: 'SUSPENDED' },
          ]}
        />
        <Input placeholder="City" value={cityFilter} onChange={e => setCityFilter(e.target.value)}
          onPressEnter={loadData} allowClear style={{ width: 140 }} />
        <Input placeholder="Locality" value={localityFilter} onChange={e => setLocalityFilter(e.target.value)}
          onPressEnter={loadData} allowClear style={{ width: 150 }} />
        <Input prefix={<SearchOutlined />} placeholder="Project / Builder name"
          value={searchFilter} onChange={e => setSearchFilter(e.target.value)}
          onPressEnter={loadData} allowClear style={{ width: 200 }} />
        <Select
          placeholder="Verified" allowClear style={{ width: 120 }}
          onChange={v => setVerifiedFilter(v)} value={verifiedFilter}
          options={[
            { label: 'All', value: undefined },
            { label: 'Verified', value: true },
            { label: 'Unverified', value: false },
          ]}
        />
        <RangePicker onChange={(_, ds) => setDateRange(ds[0] ? [ds[0], ds[1]] : null)} />
        <Button type="primary" onClick={loadData}>Search</Button>
        <Button onClick={() => { setStatusFilter(undefined); setCityFilter(''); setLocalityFilter(''); setSearchFilter(''); setVerifiedFilter(undefined); setDateRange(null); setTimeout(loadData, 0); }}>Reset</Button>
      </div>

      <Table
        columns={columns} dataSource={projects} loading={loading}
        rowKey="id" size="small" scroll={{ x: 1400 }}
        pagination={{ pageSize: 20, showSizeChanger: true }}
      />

      <Modal
        open={!!detailModal} onCancel={() => setDetailModal(null)}
        width={800} footer={null} title={detailModal?.projectName}
      >
        {detailModal && (
          <Tabs items={[
            {
              key: 'overview', label: 'Overview',
              children: (
                <div>
                  <h4 style={{ marginBottom: 8 }}>Host / Builder Info</h4>
                  <Row gutter={[16, 8]} style={{ marginBottom: 16, padding: '8px 12px', background: '#fafafa', borderRadius: 6 }}>
                    <Col span={8}><strong>Host Name:</strong> {detailModal.hostName || '--'}</Col>
                    <Col span={8}><strong>Phone:</strong> {detailModal.builderPhone || '--'}</Col>
                    <Col span={8}><strong>Email:</strong> {detailModal.builderEmail || '--'}</Col>
                    <Col span={8}><strong>Builder/Company:</strong> {detailModal.builderName}</Col>
                    <Col span={8}><strong>Host ID:</strong> <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{detailModal.builderId?.substring(0, 12) || '--'}</span></Col>
                    <Col span={8}>
                      {detailModal.builderLogoUrl && <img src={detailModal.builderLogoUrl} alt="logo" style={{ height: 24, borderRadius: 4 }} />}
                    </Col>
                  </Row>
                  <h4 style={{ marginBottom: 8 }}>Project Details</h4>
                  <Row gutter={[16, 8]}>
                    <Col span={8}><strong>Phase:</strong> {PROJECT_STATUS_LABELS[detailModal.projectStatus] || detailModal.projectStatus}</Col>
                    <Col span={8}><strong>Progress:</strong> {detailModal.constructionProgressPercent}%</Col>
                    <Col span={8}><strong>Created:</strong> {detailModal.createdAt ? new Date(detailModal.createdAt).toLocaleDateString('en-IN') : '--'}</Col>
                    <Col span={8}><strong>Total Units:</strong> {detailModal.totalUnits}</Col>
                    <Col span={8}><strong>Available:</strong> {detailModal.availableUnits}</Col>
                    <Col span={8}><strong>Towers:</strong> {detailModal.totalTowers || '--'}</Col>
                    <Col span={8}><strong>Max Floors:</strong> {detailModal.totalFloorsMax || '--'}</Col>
                    <Col span={8}><strong>Land Area:</strong> {detailModal.landAreaSqft ? `${detailModal.landAreaSqft} sqft` : '--'}</Col>
                    <Col span={8}><strong>Project Area:</strong> {detailModal.projectAreaSqft ? `${detailModal.projectAreaSqft} sqft` : '--'}</Col>
                    <Col span={8}><strong>Launch:</strong> {detailModal.launchDate || '--'}</Col>
                    <Col span={8}><strong>Possession:</strong> {detailModal.possessionDate || '--'}</Col>
                    <Col span={8}><strong>Price Range:</strong> {detailModal.minPricePaise ? `${formatPrice(detailModal.minPricePaise)} - ${formatPrice(detailModal.maxPricePaise)}` : '--'}</Col>
                  </Row>
                  <h4 style={{ marginTop: 16 }}>Location</h4>
                  <p>{detailModal.address || `${detailModal.locality || ''}, ${detailModal.city}, ${detailModal.state} - ${detailModal.pincode}`}</p>
                  {detailModal.tagline && <><h4>Tagline</h4><p>{detailModal.tagline}</p></>}
                  {detailModal.description && <><h4>Description</h4><p>{detailModal.description}</p></>}
                  {detailModal.amenities && <><h4>Amenities</h4><p>{detailModal.amenities}</p></>}
                </div>
              ),
            },
            {
              key: 'units', label: `Unit Types (${detailModal.unitTypes?.length || 0})`,
              children: (
                <Table
                  size="small" rowKey="id" pagination={false}
                  dataSource={detailModal.unitTypes || []}
                  columns={[
                    { title: 'Name', dataIndex: 'name' },
                    { title: 'BHK', dataIndex: 'bhk' },
                    { title: 'Carpet (sqft)', dataIndex: 'carpetAreaSqft' },
                    { title: 'Base Price', dataIndex: 'basePricePaise', render: (p: number) => formatPrice(p) },
                    { title: 'Total', dataIndex: 'totalUnits' },
                    { title: 'Available', dataIndex: 'availableUnits' },
                  ]}
                />
              ),
            },
            {
              key: 'legal', label: 'RERA / Verification',
              children: (
                <div>
                  <p><strong>RERA ID:</strong> {detailModal.reraId || 'Not provided'}</p>
                  <p><strong>RERA Verified:</strong> {detailModal.reraVerified ? 'Yes' : 'No'}</p>
                  <p><strong>Safar Verified:</strong> {detailModal.verified ? 'Yes' : 'No'}</p>
                  <p><strong>Bank Approvals:</strong> {detailModal.bankApprovals || '--'}</p>
                  <Space style={{ marginTop: 16 }}>
                    {!detailModal.verified && <Button type="primary" onClick={() => { verify(detailModal.id); setDetailModal(null); }}>Verify Project</Button>}
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
                  <Col span={12}><Statistic title="Created" value={detailModal.createdAt ? new Date(detailModal.createdAt).toLocaleDateString() : '--'} /></Col>
                </Row>
              ),
            },
          ]} />
        )}
      </Modal>
    </div>
  );
}
