import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Card, Row, Col, Statistic, Select, DatePicker, Input, Button,
  Popconfirm, Modal, message, Descriptions,
} from 'antd';
import {
  FileTextOutlined, SearchOutlined, CheckCircleOutlined,
  DownloadOutlined, UserOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;
const { RangePicker } = DatePicker;

const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const statusColor: Record<string, string> = {
  DRAFT: 'default', STAMPED: 'blue', PENDING_SIGN: 'orange', SIGNED: 'cyan',
  PENDING_REGISTRATION: 'gold', REGISTERED: 'green', DELIVERED: 'purple',
};

const packageColor: Record<string, string> = {
  BASIC: 'default', ESTAMP: 'blue', REGISTERED: 'green', PREMIUM: 'gold',
};

const STATUSES = ['DRAFT', 'STAMPED', 'PENDING_SIGN', 'SIGNED', 'PENDING_REGISTRATION', 'REGISTERED', 'DELIVERED'];
const AGREEMENT_TYPES = ['SALE_AGREEMENT', 'SALE_DEED', 'RENTAL_AGREEMENT', 'LEAVE_LICENSE', 'PG_AGREEMENT'];

export default function AgreementsPage() {
  const token = localStorage.getItem('admin_token') ?? '';

  const [agreements, setAgreements] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<any>(null);

  // Filters
  const [status, setStatus] = useState('');
  const [agreementType, setAgreementType] = useState('');
  const [search, setSearch] = useState('');
  const [dateRange, setDateRange] = useState<[string, string] | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const load = () => {
    setLoading(true);
    const params: any = { page, size: pageSize };
    if (status) params.status = status;
    if (agreementType) params.type = agreementType;
    if (search) params.search = search;
    if (dateRange) { params.dateFrom = dateRange[0]; params.dateTo = dateRange[1]; }

    adminApi.getAgreements(token, params)
      .then(({ data }) => {
        const items = data.content || data || [];
        setAgreements(Array.isArray(items) ? items : []);
      })
      .catch(() => setAgreements([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [page, pageSize, status, agreementType]);

  const handleSearch = () => { setPage(0); load(); };

  const handleStatusUpdate = async (id: string, newStatus: string) => {
    try {
      await adminApi.updateAgreementStatus(id, newStatus, token);
      message.success(`Agreement marked as ${newStatus}`);
      load();
      if (detail?.id === id) setDetail(null);
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Status update failed');
    }
  };

  // Stats from data
  const total = agreements.length;
  const draft = agreements.filter(a => a.status === 'DRAFT').length;
  const signed = agreements.filter(a => a.status === 'SIGNED').length;
  const registered = agreements.filter(a => a.status === 'REGISTERED').length;

  const columns: ColumnsType<any> = [
    {
      title: 'ID', dataIndex: 'id', width: 90, ellipsis: true,
      render: (id, r) => (
        <a onClick={() => setDetail(r)} style={{ fontFamily: 'monospace' }}>{id?.substring(0, 8)}</a>
      ),
    },
    {
      title: 'Raised By', key: 'user', width: 180,
      render: (_, r) => (
        <div style={{ fontSize: 12 }}>
          {r.userName && <div style={{ fontWeight: 500 }}><UserOutlined style={{ marginRight: 4 }} />{r.userName}</div>}
          {r.userPhone && <div>{r.userPhone}</div>}
          {r.userEmail && <div style={{ color: '#888' }}>{r.userEmail}</div>}
          {!r.userName && !r.userPhone && !r.userEmail && (
            <span style={{ color: '#bbb' }}>ID: {r.userId?.substring(0, 8)}</span>
          )}
        </div>
      ),
    },
    {
      title: 'Type', dataIndex: 'agreementType', width: 130,
      render: (t) => <Tag>{t?.replace(/_/g, ' ') || '--'}</Tag>,
    },
    {
      title: 'Package', dataIndex: 'packageType', width: 100,
      render: (p) => <Tag color={packageColor[p] ?? 'default'}>{p || '--'}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'status', width: 110,
      render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag>,
    },
    {
      title: 'State / City', key: 'location', width: 130,
      render: (_, r) => (
        <div style={{ fontSize: 12 }}>
          {r.state && <div>{r.state}</div>}
          {r.city && <div style={{ color: '#888' }}>{r.city}</div>}
          {!r.state && !r.city && '--'}
        </div>
      ),
    },
    {
      title: 'Parties', dataIndex: 'parties', width: 65, align: 'center',
      render: (parties) => Array.isArray(parties) ? parties.length : (parties || 0),
    },
    {
      title: 'Total', dataIndex: 'totalAmountPaise', width: 100,
      render: (v) => v ? INR(v) : '--',
      sorter: (a, b) => (a.totalAmountPaise || 0) - (b.totalAmountPaise || 0),
    },
    {
      title: 'Created', dataIndex: 'createdAt', width: 100,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '--',
    },
    {
      title: 'Actions', width: 200, fixed: 'right',
      render: (_, r) => (
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {r.status === 'DRAFT' && (
            <Popconfirm title="Mark as Stamped?" onConfirm={() => handleStatusUpdate(r.id, 'STAMPED')}>
              <Button size="small">Stamped</Button>
            </Popconfirm>
          )}
          {r.status === 'SIGNED' && (
            <Popconfirm title="Mark as Registered?" onConfirm={() => handleStatusUpdate(r.id, 'REGISTERED')}>
              <Button size="small" type="primary">Registered</Button>
            </Popconfirm>
          )}
          {r.status === 'REGISTERED' && (
            <Popconfirm title="Mark as Delivered?" onConfirm={() => handleStatusUpdate(r.id, 'DELIVERED')}>
              <Button size="small" style={{ color: '#722ed1', borderColor: '#722ed1' }}>Delivered</Button>
            </Popconfirm>
          )}
        </div>
      ),
    },
  ];

  const parseJson = (val: any) => {
    if (!val) return null;
    if (typeof val === 'object') return val;
    try { return JSON.parse(val); } catch { return null; }
  };

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>
        <FileTextOutlined style={{ marginRight: 8 }} />Agreements
      </Title>

      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={6}>
          <Card size="small"><Statistic title="Total Agreements" value={total} prefix={<FileTextOutlined />} /></Card>
        </Col>
        <Col span={6}>
          <Card size="small"><Statistic title="Draft" value={draft} valueStyle={{ color: '#8c8c8c' }} /></Card>
        </Col>
        <Col span={6}>
          <Card size="small"><Statistic title="Signed" value={signed} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#13c2c2' }} /></Card>
        </Col>
        <Col span={6}>
          <Card size="small"><Statistic title="Registered" value={registered} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
      </Row>

      {/* Filters */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select placeholder="Status" value={status || undefined} onChange={v => { setStatus(v || ''); setPage(0); }}
          allowClear style={{ width: 160 }}>
          {STATUSES.map(s => <Select.Option key={s} value={s}>{s}</Select.Option>)}
        </Select>
        <Select placeholder="Agreement Type" value={agreementType || undefined}
          onChange={v => { setAgreementType(v || ''); setPage(0); }}
          allowClear style={{ width: 180 }}>
          {AGREEMENT_TYPES.map(t => <Select.Option key={t} value={t}>{t.replace(/_/g, ' ')}</Select.Option>)}
        </Select>
        <RangePicker onChange={(_, ds) => setDateRange(ds[0] ? [ds[0], ds[1]] : null)} />
        <Input prefix={<SearchOutlined />} placeholder="Search by party name, ID..."
          value={search} onChange={e => setSearch(e.target.value)}
          onPressEnter={handleSearch} style={{ width: 240 }} allowClear />
        <Button type="primary" onClick={handleSearch}>Search</Button>
      </div>

      {/* Table */}
      <Table
        columns={columns}
        dataSource={agreements}
        rowKey="id"
        loading={loading}
        scroll={{ x: 1300 }}
        pagination={{
          current: page + 1, pageSize, total: agreements.length,
          onChange: (p, ps) => { setPage(p - 1); setPageSize(ps); },
          showSizeChanger: true, showTotal: (t) => `${t} agreements`,
        }}
        locale={{ emptyText: 'No agreements found' }}
      />

      {/* Detail modal */}
      <Modal
        open={!!detail}
        onCancel={() => setDetail(null)}
        width={780}
        title={`Agreement ${detail?.id?.substring(0, 8) || ''}`}
        footer={
          detail ? (
            <div style={{ display: 'flex', gap: 8 }}>
              {detail.status === 'DRAFT' && (
                <Popconfirm title="Mark as Stamped?" onConfirm={() => handleStatusUpdate(detail.id, 'STAMPED')}>
                  <Button>Mark Stamped</Button>
                </Popconfirm>
              )}
              {detail.status === 'SIGNED' && (
                <Popconfirm title="Mark as Registered?" onConfirm={() => handleStatusUpdate(detail.id, 'REGISTERED')}>
                  <Button type="primary">Mark Registered</Button>
                </Popconfirm>
              )}
              {detail.status === 'REGISTERED' && (
                <Popconfirm title="Mark as Delivered?" onConfirm={() => handleStatusUpdate(detail.id, 'DELIVERED')}>
                  <Button>Mark Delivered</Button>
                </Popconfirm>
              )}
              <Button onClick={() => setDetail(null)}>Close</Button>
            </div>
          ) : null
        }
      >
        {detail && (
          <>
            {/* User / Raised By */}
            <Title level={5} style={{ marginBottom: 8 }}><UserOutlined style={{ marginRight: 6 }} />Raised By</Title>
            <Descriptions column={3} bordered size="small" style={{ marginBottom: 16, background: '#fafafa' }}>
              <Descriptions.Item label="Name">{detail.userName || '--'}</Descriptions.Item>
              <Descriptions.Item label="Phone">{detail.userPhone || '--'}</Descriptions.Item>
              <Descriptions.Item label="Email">{detail.userEmail || '--'}</Descriptions.Item>
              <Descriptions.Item label="User ID" span={3}>
                <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{detail.userId || '--'}</span>
              </Descriptions.Item>
            </Descriptions>

            {/* Agreement Info */}
            <Title level={5} style={{ marginBottom: 8 }}>Agreement Details</Title>
            <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Status">
                <Tag color={statusColor[detail.status]}>{detail.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Type">{detail.agreementType?.replace(/_/g, ' ') || '--'}</Descriptions.Item>
              <Descriptions.Item label="Package">
                <Tag color={packageColor[detail.packageType]}>{detail.packageType || '--'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="State / City">{[detail.state, detail.city].filter(Boolean).join(', ') || '--'}</Descriptions.Item>
              <Descriptions.Item label="Agreement Date">{detail.agreementDate || '--'}</Descriptions.Item>
              <Descriptions.Item label="Period">
                {detail.startDate && detail.endDate ? `${detail.startDate} to ${detail.endDate}` : '--'}
              </Descriptions.Item>
              <Descriptions.Item label="E-Stamp No.">{detail.stampCertificateNumber || '--'}</Descriptions.Item>
              <Descriptions.Item label="Razorpay ID">
                <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{detail.razorpayPaymentId || '--'}</span>
              </Descriptions.Item>
            </Descriptions>

            {/* Financial */}
            <Title level={5} style={{ marginBottom: 8 }}>Financials</Title>
            <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Stamp Duty">{detail.stampDutyPaise ? INR(detail.stampDutyPaise) : '--'}</Descriptions.Item>
              <Descriptions.Item label="Registration Fee">{detail.registrationFeePaise ? INR(detail.registrationFeePaise) : '--'}</Descriptions.Item>
              <Descriptions.Item label="Service Fee">{detail.serviceFeePaise ? INR(detail.serviceFeePaise) : '--'}</Descriptions.Item>
              <Descriptions.Item label="Total Amount">
                <strong>{detail.totalAmountPaise ? INR(detail.totalAmountPaise) : '--'}</strong>
              </Descriptions.Item>
              {detail.monthlyRentPaise && (
                <Descriptions.Item label="Monthly Rent">{INR(detail.monthlyRentPaise)}</Descriptions.Item>
              )}
              {detail.securityDepositPaise && (
                <Descriptions.Item label="Security Deposit">{INR(detail.securityDepositPaise)}</Descriptions.Item>
              )}
              {detail.saleConsiderationPaise && (
                <Descriptions.Item label="Sale Consideration">{INR(detail.saleConsiderationPaise)}</Descriptions.Item>
              )}
            </Descriptions>

            {/* Parties */}
            {Array.isArray(detail.parties) && detail.parties.length > 0 && (
              <>
                <Title level={5} style={{ marginBottom: 8 }}>Parties ({detail.parties.length})</Title>
                {detail.parties.map((p: any, i: number) => (
                  <Descriptions key={i} column={2} bordered size="small" style={{ marginBottom: 8 }}
                    title={`${p.partyType || 'Party'} ${i + 1}`}>
                    <Descriptions.Item label="Name">{p.fullName || '--'}</Descriptions.Item>
                    <Descriptions.Item label="Phone">{p.phone || '--'}</Descriptions.Item>
                    <Descriptions.Item label="Email">{p.email || '--'}</Descriptions.Item>
                    <Descriptions.Item label="Aadhaar">{p.aadhaarNumber ? `****${p.aadhaarNumber.slice(-4)}` : '--'}</Descriptions.Item>
                    <Descriptions.Item label="PAN">{p.panNumber || '--'}</Descriptions.Item>
                    <Descriptions.Item label="Signed">{p.verified ? <Tag color="green">Yes</Tag> : <Tag>No</Tag>}</Descriptions.Item>
                    <Descriptions.Item label="Address" span={2}>{p.address || '--'}</Descriptions.Item>
                  </Descriptions>
                ))}
              </>
            )}

            {/* Property details from JSON */}
            {(() => {
              const prop = parseJson(detail.propertyDetailsJson);
              if (!prop) return null;
              return (
                <>
                  <Title level={5} style={{ margin: '12px 0 8px' }}>Property Details</Title>
                  <Descriptions column={2} bordered size="small">
                    <Descriptions.Item label="Address">{prop.address || '--'}</Descriptions.Item>
                    <Descriptions.Item label="City">{prop.city || '--'}</Descriptions.Item>
                    <Descriptions.Item label="Area">{prop.areaSqft ? `${prop.areaSqft} sqft` : '--'}</Descriptions.Item>
                    <Descriptions.Item label="Monthly Rent">{prop.monthlyRentPaise ? INR(prop.monthlyRentPaise) : '--'}</Descriptions.Item>
                  </Descriptions>
                </>
              );
            })()}

            {/* Notes */}
            {detail.notes && (
              <>
                <Title level={5} style={{ margin: '12px 0 8px' }}>Notes</Title>
                <p style={{ background: '#fffbe6', padding: 8, borderRadius: 4, border: '1px solid #ffe58f' }}>{detail.notes}</p>
              </>
            )}

            {/* Timestamps */}
            <Descriptions column={2} bordered size="small" style={{ marginTop: 12 }}>
              <Descriptions.Item label="Created">{detail.createdAt ? new Date(detail.createdAt).toLocaleString('en-IN') : '--'}</Descriptions.Item>
              <Descriptions.Item label="Updated">{detail.updatedAt ? new Date(detail.updatedAt).toLocaleString('en-IN') : '--'}</Descriptions.Item>
              {detail.signedAt && <Descriptions.Item label="Signed At">{new Date(detail.signedAt).toLocaleString('en-IN')}</Descriptions.Item>}
              {detail.registeredAt && <Descriptions.Item label="Registered At">{new Date(detail.registeredAt).toLocaleString('en-IN')}</Descriptions.Item>}
            </Descriptions>

            {/* PDF links */}
            <div style={{ marginTop: 16, display: 'flex', gap: 8 }}>
              {detail.draftPdfUrl && (
                <Button icon={<DownloadOutlined />} href={detail.draftPdfUrl} target="_blank">Draft PDF</Button>
              )}
              {detail.signedPdfUrl && (
                <Button icon={<DownloadOutlined />} href={detail.signedPdfUrl} target="_blank" type="primary">Signed PDF</Button>
              )}
              {detail.registeredPdfUrl && (
                <Button icon={<DownloadOutlined />} href={detail.registeredPdfUrl} target="_blank"
                  style={{ color: '#52c41a', borderColor: '#52c41a' }}>Registered PDF</Button>
              )}
            </div>
          </>
        )}
      </Modal>
    </div>
  );
}
