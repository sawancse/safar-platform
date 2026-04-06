import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Card, Row, Col, Statistic, Select, Input, Button,
  Modal, message, Descriptions, Tabs, Badge,
} from 'antd';
import {
  BankOutlined, SearchOutlined, CheckCircleOutlined, CloseCircleOutlined,
  FileDoneOutlined, DollarOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;

const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const statusColor: Record<string, string> = {
  APPLIED: 'blue', DOCUMENTS_PENDING: 'orange', UNDER_REVIEW: 'cyan',
  SANCTIONED: 'green', DISBURSED: 'purple', REJECTED: 'red',
};

const STATUS_FLOW = ['APPLIED', 'DOCUMENTS_PENDING', 'UNDER_REVIEW', 'SANCTIONED', 'DISBURSED'];
const ALL_STATUSES = [...STATUS_FLOW, 'REJECTED'];

const docStatusColor: Record<string, string> = {
  VERIFIED: 'green', PENDING: 'orange', REJECTED: 'red', UPLOADED: 'blue',
};

export default function HomeLoanPage() {
  const token = localStorage.getItem('admin_token') ?? '';

  const [applications, setApplications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<any>(null);
  const [banks, setBanks] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState('applications');

  // Filters
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const load = () => {
    setLoading(true);
    const params: any = { page, size: pageSize };
    if (status) params.status = status;
    if (search) params.search = search;

    adminApi.getLoanApplications(token, params)
      .then(({ data }) => {
        const items = data.content || data || [];
        setApplications(Array.isArray(items) ? items : []);
      })
      .catch(() => setApplications([]))
      .finally(() => setLoading(false));
  };

  const loadBanks = () => {
    adminApi.getPartnerBanks(token)
      .then(({ data }) => setBanks(Array.isArray(data) ? data : data.content || []))
      .catch(() => setBanks([]));
  };

  useEffect(() => { load(); }, [page, pageSize, status]);
  useEffect(() => { loadBanks(); }, []);

  const handleSearch = () => { setPage(0); load(); };

  const handleStatusUpdate = async (id: string, newStatus: string) => {
    try {
      await adminApi.updateLoanStatus(id, newStatus, token);
      message.success(`Application status updated to ${newStatus}`);
      load();
      if (detail?.id === id) setDetail(null);
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Status update failed');
    }
  };

  // Stats
  const total = applications.length;
  const applied = applications.filter(a => a.status === 'APPLIED').length;
  const underReview = applications.filter(a => a.status === 'UNDER_REVIEW').length;
  const sanctioned = applications.filter(a => a.status === 'SANCTIONED').length;
  const disbursed = applications.filter(a => a.status === 'DISBURSED').length;
  const rejected = applications.filter(a => a.status === 'REJECTED').length;

  const appColumns: ColumnsType<any> = [
    {
      title: 'ID', dataIndex: 'id', width: 100, ellipsis: true,
      render: (id, r) => (
        <a onClick={() => setDetail(r)} style={{ fontFamily: 'monospace' }}>{id?.substring(0, 8)}</a>
      ),
    },
    {
      title: 'Applicant', width: 160,
      render: (_, r) => r.applicantName || `${r.firstName || ''} ${r.lastName || ''}`.trim() || '—',
    },
    {
      title: 'Bank', dataIndex: 'bankName', width: 140,
      render: (name) => name || '—',
    },
    {
      title: 'Loan Amount', dataIndex: 'loanAmountPaise', width: 130,
      render: (v) => v ? INR(v) : '—',
      sorter: (a, b) => (a.loanAmountPaise || 0) - (b.loanAmountPaise || 0),
    },
    {
      title: 'Tenure', dataIndex: 'tenureMonths', width: 90, align: 'center',
      render: (v) => v ? `${v} mo` : '—',
    },
    {
      title: 'Interest %', dataIndex: 'interestRate', width: 100, align: 'center',
      render: (v) => v ? `${v}%` : '—',
    },
    {
      title: 'Status', dataIndex: 'status', width: 140,
      render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s?.replace(/_/g, ' ')}</Tag>,
    },
    {
      title: 'Applied', dataIndex: 'createdAt', width: 105,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
    },
    {
      title: 'Actions', width: 200, fixed: 'right',
      render: (_, r) => {
        const idx = STATUS_FLOW.indexOf(r.status);
        const nextStatus = idx >= 0 && idx < STATUS_FLOW.length - 1 ? STATUS_FLOW[idx + 1] : null;
        return (
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            {nextStatus && (
              <Button size="small" type="primary"
                onClick={() => handleStatusUpdate(r.id, nextStatus)}>
                {nextStatus.replace(/_/g, ' ')}
              </Button>
            )}
            {r.status !== 'REJECTED' && r.status !== 'DISBURSED' && (
              <Button size="small" danger
                onClick={() => handleStatusUpdate(r.id, 'REJECTED')}>
                Reject
              </Button>
            )}
          </div>
        );
      },
    },
  ];

  const bankColumns: ColumnsType<any> = [
    { title: 'Bank Name', dataIndex: 'name', width: 200 },
    {
      title: 'Interest Rate', width: 160,
      render: (_, r) => `${r.minInterestRate || '—'}% - ${r.maxInterestRate || '—'}%`,
    },
    {
      title: 'Processing Fee', dataIndex: 'processingFeePaise', width: 140,
      render: (v) => v ? INR(v) : '—',
    },
    {
      title: 'Max Tenure', dataIndex: 'maxTenureMonths', width: 120,
      render: (v) => v ? `${v} months` : '—',
    },
    {
      title: 'Max LTV', dataIndex: 'maxLtvPercent', width: 100,
      render: (v) => v ? `${v}%` : '—',
    },
    {
      title: 'Commission', dataIndex: 'commissionPercent', width: 120,
      render: (v) => v ? `${v}%` : '—',
    },
    {
      title: 'Active', dataIndex: 'active', width: 80,
      render: (v) => <Tag color={v ? 'green' : 'red'}>{v ? 'Yes' : 'No'}</Tag>,
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>
        <BankOutlined style={{ marginRight: 8 }} />Home Loans
      </Title>

      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={4}><Card size="small"><Statistic title="Total" value={total} prefix={<BankOutlined />} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Applied" value={applied} valueStyle={{ color: '#1677ff' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Under Review" value={underReview} valueStyle={{ color: '#13c2c2' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Sanctioned" value={sanctioned} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Disbursed" value={disbursed} prefix={<DollarOutlined />} valueStyle={{ color: '#722ed1' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Rejected" value={rejected} prefix={<CloseCircleOutlined />} valueStyle={{ color: '#ff4d4f' }} /></Card></Col>
      </Row>

      <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
        {
          key: 'applications',
          label: 'Applications',
          children: (
            <>
              {/* Filters */}
              <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
                <Select placeholder="Status" value={status || undefined}
                  onChange={v => { setStatus(v || ''); setPage(0); }}
                  allowClear style={{ width: 180 }}>
                  {ALL_STATUSES.map(s => <Select.Option key={s} value={s}>{s.replace(/_/g, ' ')}</Select.Option>)}
                </Select>
                <Input prefix={<SearchOutlined />} placeholder="Search by name, bank..."
                  value={search} onChange={e => setSearch(e.target.value)}
                  onPressEnter={handleSearch} style={{ width: 260 }} allowClear />
                <Button type="primary" onClick={handleSearch}>Search</Button>
              </div>

              <Table
                columns={appColumns}
                dataSource={applications}
                rowKey="id"
                loading={loading}
                scroll={{ x: 1200 }}
                pagination={{
                  current: page + 1, pageSize, total: applications.length,
                  onChange: (p, ps) => { setPage(p - 1); setPageSize(ps); },
                  showSizeChanger: true, showTotal: (t) => `${t} applications`,
                }}
                locale={{ emptyText: 'No loan applications found' }}
              />
            </>
          ),
        },
        {
          key: 'banks',
          label: 'Partner Banks',
          children: (
            <Table
              columns={bankColumns}
              dataSource={banks}
              rowKey="id"
              pagination={false}
              locale={{ emptyText: 'No partner banks configured' }}
            />
          ),
        },
      ]} />

      {/* Detail modal */}
      <Modal
        open={!!detail}
        onCancel={() => setDetail(null)}
        width={700}
        title={`Loan Application ${detail?.id?.substring(0, 8) || ''}`}
        footer={
          detail ? (
            <div style={{ display: 'flex', gap: 8 }}>
              {(() => {
                const idx = STATUS_FLOW.indexOf(detail.status);
                const next = idx >= 0 && idx < STATUS_FLOW.length - 1 ? STATUS_FLOW[idx + 1] : null;
                return next ? (
                  <Button type="primary" onClick={() => handleStatusUpdate(detail.id, next)}>
                    Move to {next.replace(/_/g, ' ')}
                  </Button>
                ) : null;
              })()}
              {detail.status !== 'REJECTED' && detail.status !== 'DISBURSED' && (
                <Button danger onClick={() => handleStatusUpdate(detail.id, 'REJECTED')}>Reject</Button>
              )}
              <Button onClick={() => setDetail(null)}>Close</Button>
            </div>
          ) : null
        }
      >
        {detail && (
          <>
            <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Status">
                <Tag color={statusColor[detail.status]}>{detail.status?.replace(/_/g, ' ')}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Applicant">
                {detail.applicantName || `${detail.firstName || ''} ${detail.lastName || ''}`.trim() || '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Phone">{detail.phone || '—'}</Descriptions.Item>
              <Descriptions.Item label="Email">{detail.email || '—'}</Descriptions.Item>
              <Descriptions.Item label="Bank">{detail.bankName || '—'}</Descriptions.Item>
              <Descriptions.Item label="Loan Amount">{detail.loanAmountPaise ? INR(detail.loanAmountPaise) : '—'}</Descriptions.Item>
              <Descriptions.Item label="Property Value">{detail.propertyValuePaise ? INR(detail.propertyValuePaise) : '—'}</Descriptions.Item>
              <Descriptions.Item label="Down Payment">{detail.downPaymentPaise ? INR(detail.downPaymentPaise) : '—'}</Descriptions.Item>
              <Descriptions.Item label="Tenure">{detail.tenureMonths ? `${detail.tenureMonths} months` : '—'}</Descriptions.Item>
              <Descriptions.Item label="Interest Rate">{detail.interestRate ? `${detail.interestRate}%` : '—'}</Descriptions.Item>
              <Descriptions.Item label="EMI">{detail.emiAmountPaise ? INR(detail.emiAmountPaise) : '—'}</Descriptions.Item>
              <Descriptions.Item label="Employment">{detail.employmentType || '—'}</Descriptions.Item>
              <Descriptions.Item label="Annual Income">{detail.annualIncomePaise ? INR(detail.annualIncomePaise) : '—'}</Descriptions.Item>
              <Descriptions.Item label="CIBIL Score">{detail.cibilScore || '—'}</Descriptions.Item>
              <Descriptions.Item label="Applied">
                {detail.createdAt ? new Date(detail.createdAt).toLocaleString('en-IN') : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Updated">
                {detail.updatedAt ? new Date(detail.updatedAt).toLocaleString('en-IN') : '—'}
              </Descriptions.Item>
            </Descriptions>

            {/* Sanction details */}
            {detail.sanctionAmount && (
              <>
                <Title level={5} style={{ marginBottom: 8 }}>Sanction Details</Title>
                <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
                  <Descriptions.Item label="Sanctioned Amount">{detail.sanctionAmountPaise ? INR(detail.sanctionAmountPaise) : '—'}</Descriptions.Item>
                  <Descriptions.Item label="Sanctioned Rate">{detail.sanctionInterestRate ? `${detail.sanctionInterestRate}%` : '—'}</Descriptions.Item>
                  <Descriptions.Item label="Sanction Date">{detail.sanctionDate ? new Date(detail.sanctionDate).toLocaleDateString('en-IN') : '—'}</Descriptions.Item>
                  <Descriptions.Item label="Validity">{detail.sanctionValidity || '—'}</Descriptions.Item>
                </Descriptions>
              </>
            )}

            {/* Documents */}
            {Array.isArray(detail.documents) && detail.documents.length > 0 && (
              <>
                <Title level={5} style={{ marginBottom: 8 }}>Documents</Title>
                <Table
                  size="small"
                  dataSource={detail.documents}
                  rowKey={(r: any, i) => r.id || String(i)}
                  pagination={false}
                  columns={[
                    { title: 'Document', dataIndex: 'documentType', render: (t: string) => t?.replace(/_/g, ' ') || '—' },
                    { title: 'File', dataIndex: 'fileName', ellipsis: true },
                    {
                      title: 'Status', dataIndex: 'verificationStatus',
                      render: (s: string) => <Tag color={docStatusColor[s] ?? 'default'}>{s || 'PENDING'}</Tag>,
                    },
                  ]}
                />
              </>
            )}
          </>
        )}
      </Modal>
    </div>
  );
}
