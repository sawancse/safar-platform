import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Card, Row, Col, Statistic, Select, Input, Button,
  Popconfirm, Modal, message, Descriptions,
} from 'antd';
import {
  SafetyOutlined, SearchOutlined, CheckCircleOutlined, WarningOutlined,
  UserAddOutlined, FileSearchOutlined, DownloadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import axios from 'axios';
import { adminApi } from '../lib/api';

const { Title } = Typography;

const statusColor: Record<string, string> = {
  CREATED: 'blue', DOCUMENTS_UPLOADED: 'cyan', ASSIGNED: 'orange', IN_PROGRESS: 'processing',
  REPORT_READY: 'green', CONSULTATION_DONE: 'purple', CLOSED: 'default',
};

const riskColor: Record<string, string> = {
  GREEN: 'green', YELLOW: 'orange', RED: 'red',
};

const packageColor: Record<string, string> = {
  TITLE_SEARCH: 'default', DUE_DILIGENCE: 'blue', BUYER_ASSIST: 'green', PREMIUM: 'gold',
};

const STATUSES = ['CREATED', 'DOCUMENTS_UPLOADED', 'ASSIGNED', 'IN_PROGRESS', 'REPORT_READY', 'CONSULTATION_DONE', 'CLOSED'];
const RISK_LEVELS = ['GREEN', 'YELLOW', 'RED'];
const PACKAGES = ['TITLE_SEARCH', 'DUE_DILIGENCE', 'BUYER_ASSIST', 'PREMIUM'];

const verificationStatusColor: Record<string, string> = {
  PASS: 'green', FAIL: 'red', PENDING: 'orange', IN_PROGRESS: 'blue', FLAGGED: 'volcano',
};

export default function LegalCasesPage() {
  const token = localStorage.getItem('admin_token') ?? '';

  const [cases, setCases] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<any>(null);
  const [advocates, setAdvocates] = useState<any[]>([]);
  const [assignModal, setAssignModal] = useState<{ open: boolean; caseId: string }>({ open: false, caseId: '' });
  const [selectedAdvocate, setSelectedAdvocate] = useState('');

  // Filters
  const [status, setStatus] = useState('');
  const [packageType, setPackageType] = useState('');
  const [riskLevel, setRiskLevel] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const load = () => {
    setLoading(true);
    const params: any = { page, size: pageSize };
    if (status) params.status = status;
    if (packageType) params.packageType = packageType;
    if (riskLevel) params.riskLevel = riskLevel;
    if (search) params.search = search;

    adminApi.getLegalCases(token, params)
      .then(({ data }) => {
        const items = data.content || data || [];
        setCases(Array.isArray(items) ? items : []);
      })
      .catch(() => setCases([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [page, pageSize, status, packageType, riskLevel]);
  useEffect(() => {
    adminApi.getAdvocates(token)
      .then(({ data }) => setAdvocates(Array.isArray(data) ? data : data.content || []))
      .catch(() => setAdvocates([]));
  }, []);

  const handleSearch = () => { setPage(0); load(); };

  const handleStatusUpdate = async (id: string, newStatus: string) => {
    try {
      await adminApi.updateLegalCaseStatus(id, newStatus, token);
      message.success(`Case status updated to ${newStatus}`);
      load();
      if (detail?.id === id) setDetail(null);
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Status update failed');
    }
  };

  const handleAssignAdvocate = async () => {
    if (!selectedAdvocate) { message.warning('Select an advocate'); return; }
    try {
      await adminApi.assignAdvocate(assignModal.caseId, selectedAdvocate, token);
      message.success('Advocate assigned');
      setAssignModal({ open: false, caseId: '' });
      setSelectedAdvocate('');
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Assignment failed');
    }
  };

  const handleGenerateReport = async (caseId: string) => {
    try {
      await adminApi.generateLegalReport(caseId, token);
      message.success('Report generation initiated');
      load();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Report generation failed');
    }
  };

  // Stats
  const total = cases.length;
  const open = cases.filter(c => ['CREATED', 'DOCUMENTS_UPLOADED'].includes(c.status)).length;
  const assigned = cases.filter(c => ['ASSIGNED', 'IN_PROGRESS'].includes(c.status)).length;
  const reportReady = cases.filter(c => c.status === 'REPORT_READY').length;
  const closed = cases.filter(c => ['CONSULTATION_DONE', 'CLOSED'].includes(c.status)).length;
  const greenRisk = cases.filter(c => c.riskLevel === 'GREEN').length;
  const yellowRisk = cases.filter(c => c.riskLevel === 'YELLOW').length;
  const redRisk = cases.filter(c => c.riskLevel === 'RED').length;

  const columns: ColumnsType<any> = [
    {
      title: 'Case ID', dataIndex: 'id', width: 100, ellipsis: true,
      render: (id, r) => (
        <a onClick={() => setDetail(r)} style={{ fontFamily: 'monospace' }}>{id?.substring(0, 8)}</a>
      ),
    },
    {
      title: 'Package', dataIndex: 'packageType', width: 130,
      render: (p) => <Tag color={packageColor[p] ?? 'default'}>{p || '—'}</Tag>,
    },
    {
      title: 'Property City', dataIndex: 'propertyCity', width: 130,
      render: (v) => v || '—',
    },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s?.replace(/_/g, ' ')}</Tag>,
    },
    {
      title: 'Risk', dataIndex: 'riskLevel', width: 90,
      render: (r) => r ? <Tag color={riskColor[r]}>{r}</Tag> : <Tag>N/A</Tag>,
    },
    {
      title: 'Advocate', dataIndex: 'advocateName', width: 140,
      render: (v) => v || '—',
    },
    {
      title: 'Documents', dataIndex: 'documentCount', width: 90, align: 'center',
      render: (v) => v ?? '—',
    },
    {
      title: 'Due Date', dataIndex: 'dueDate', width: 105,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
    },
    {
      title: 'Created', dataIndex: 'createdAt', width: 105,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
    },
    {
      title: 'Actions', width: 260, fixed: 'right',
      render: (_, r) => (
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {(['CREATED', 'DOCUMENTS_UPLOADED'].includes(r.status)) && (
            <Button size="small" icon={<UserAddOutlined />}
              onClick={() => { setAssignModal({ open: true, caseId: r.id }); setSelectedAdvocate(''); }}>
              Assign
            </Button>
          )}
          {r.status === 'ASSIGNED' && (
            <Popconfirm title="Mark as In Progress?" onConfirm={() => handleStatusUpdate(r.id, 'IN_PROGRESS')}>
              <Button size="small" type="primary">Start</Button>
            </Popconfirm>
          )}
          {r.status === 'IN_PROGRESS' && (
            <Popconfirm title="Generate legal report?" onConfirm={() => handleGenerateReport(r.id)}>
              <Button size="small" icon={<FileSearchOutlined />} type="primary">Report</Button>
            </Popconfirm>
          )}
          {r.status === 'REPORT_READY' && (
            <Popconfirm title="Close this case?" onConfirm={() => handleStatusUpdate(r.id, 'CLOSED')}>
              <Button size="small" icon={<CheckCircleOutlined />}>Close</Button>
            </Popconfirm>
          )}
        </div>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>
        <SafetyOutlined style={{ marginRight: 8 }} />Legal Cases
      </Title>

      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={3}><Card size="small"><Statistic title="Total" value={total} prefix={<SafetyOutlined />} /></Card></Col>
        <Col span={3}><Card size="small"><Statistic title="Open" value={open} valueStyle={{ color: '#1677ff' }} /></Card></Col>
        <Col span={3}><Card size="small"><Statistic title="Assigned" value={assigned} valueStyle={{ color: '#13c2c2' }} /></Card></Col>
        <Col span={3}><Card size="small"><Statistic title="Report Ready" value={reportReady} valueStyle={{ color: '#52c41a' }} /></Card></Col>
        <Col span={3}><Card size="small"><Statistic title="Closed" value={closed} valueStyle={{ color: '#8c8c8c' }} /></Card></Col>
        <Col span={3}><Card size="small"><Statistic title="Green" value={greenRisk} valueStyle={{ color: '#52c41a' }} /></Card></Col>
        <Col span={3}><Card size="small"><Statistic title="Yellow" value={yellowRisk} valueStyle={{ color: '#faad14' }} prefix={<WarningOutlined />} /></Card></Col>
        <Col span={3}><Card size="small"><Statistic title="Red" value={redRisk} valueStyle={{ color: '#ff4d4f' }} prefix={<WarningOutlined />} /></Card></Col>
      </Row>

      {/* Filters */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select placeholder="Status" value={status || undefined}
          onChange={v => { setStatus(v || ''); setPage(0); }}
          allowClear style={{ width: 160 }}>
          {STATUSES.map(s => <Select.Option key={s} value={s}>{s.replace(/_/g, ' ')}</Select.Option>)}
        </Select>
        <Select placeholder="Package" value={packageType || undefined}
          onChange={v => { setPackageType(v || ''); setPage(0); }}
          allowClear style={{ width: 160 }}>
          {PACKAGES.map(p => <Select.Option key={p} value={p}>{p}</Select.Option>)}
        </Select>
        <Select placeholder="Risk Level" value={riskLevel || undefined}
          onChange={v => { setRiskLevel(v || ''); setPage(0); }}
          allowClear style={{ width: 140 }}>
          {RISK_LEVELS.map(r => <Select.Option key={r} value={r}>{r}</Select.Option>)}
        </Select>
        <Input prefix={<SearchOutlined />} placeholder="Search case, city..."
          value={search} onChange={e => setSearch(e.target.value)}
          onPressEnter={handleSearch} style={{ width: 220 }} allowClear />
        <Button type="primary" onClick={handleSearch}>Search</Button>
      </div>

      {/* Table */}
      <Table
        columns={columns}
        dataSource={cases}
        rowKey="id"
        loading={loading}
        scroll={{ x: 1300 }}
        pagination={{
          current: page + 1, pageSize, total: cases.length,
          onChange: (p, ps) => { setPage(p - 1); setPageSize(ps); },
          showSizeChanger: true, showTotal: (t) => `${t} cases`,
        }}
        locale={{ emptyText: 'No legal cases found' }}
      />

      {/* Assign Advocate modal */}
      <Modal
        open={assignModal.open}
        title="Assign Advocate"
        onCancel={() => { setAssignModal({ open: false, caseId: '' }); setSelectedAdvocate(''); }}
        onOk={handleAssignAdvocate}
      >
        <div style={{ marginBottom: 8 }}>Select an advocate to assign to this case:</div>
        <Select
          placeholder="Select advocate"
          value={selectedAdvocate || undefined}
          onChange={setSelectedAdvocate}
          style={{ width: '100%' }}
        >
          {advocates.map((a: any) => (
            <Select.Option key={a.id} value={a.id}>
              {a.name || a.advocateName || a.id?.substring(0, 8)} — {a.specialization || 'General'}
            </Select.Option>
          ))}
        </Select>
      </Modal>

      {/* Detail modal */}
      <Modal
        open={!!detail}
        onCancel={() => setDetail(null)}
        width={750}
        title={`Legal Case ${detail?.id?.substring(0, 8) || ''}`}
        footer={
          detail ? (
            <div style={{ display: 'flex', gap: 8 }}>
              {detail.status === 'OPEN' && (
                <Button icon={<UserAddOutlined />}
                  onClick={() => { setAssignModal({ open: true, caseId: detail.id }); setSelectedAdvocate(''); }}>
                  Assign Advocate
                </Button>
              )}
              {detail.status === 'IN_PROGRESS' && (
                <Popconfirm title="Generate legal report?" onConfirm={() => handleGenerateReport(detail.id)}>
                  <Button type="primary" icon={<FileSearchOutlined />}>Generate Report</Button>
                </Popconfirm>
              )}
              {detail.status === 'REPORT_READY' && (
                <Popconfirm title="Close this case?" onConfirm={() => handleStatusUpdate(detail.id, 'CLOSED')}>
                  <Button icon={<CheckCircleOutlined />}>Close Case</Button>
                </Popconfirm>
              )}
              {detail.reportUrl && (
                <Button icon={<DownloadOutlined />} onClick={async () => {
                  try {
                    const token = localStorage.getItem('admin_token');
                    const res = await axios.get(detail.reportUrl, {
                      responseType: 'blob',
                      headers: token ? { Authorization: `Bearer ${token}` } : {},
                    });
                    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = `legal-report-${detail.id}.pdf`;
                    a.click();
                    window.URL.revokeObjectURL(url);
                  } catch {
                    message.error('Failed to download report');
                  }
                }}>Download Report</Button>
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
              <Descriptions.Item label="Risk Level">
                {detail.riskLevel ? <Tag color={riskColor[detail.riskLevel]}>{detail.riskLevel}</Tag> : '—'}
              </Descriptions.Item>
              <Descriptions.Item label="Package">
                <Tag color={packageColor[detail.packageType]}>{detail.packageType || '—'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Case Type">{detail.caseType || '—'}</Descriptions.Item>
              <Descriptions.Item label="Property Address" span={2}>{detail.propertyAddress || '—'}</Descriptions.Item>
              <Descriptions.Item label="City">{detail.propertyCity || '—'}</Descriptions.Item>
              <Descriptions.Item label="State">{detail.propertyState || '—'}</Descriptions.Item>
              <Descriptions.Item label="Due Date">{detail.dueDate ? new Date(detail.dueDate).toLocaleDateString('en-IN') : '—'}</Descriptions.Item>
              <Descriptions.Item label="Created">{detail.createdAt ? new Date(detail.createdAt).toLocaleString('en-IN') : '—'}</Descriptions.Item>
            </Descriptions>

            {/* Advocate info */}
            {detail.advocateName && (
              <>
                <Title level={5} style={{ marginBottom: 8 }}>Advocate</Title>
                <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
                  <Descriptions.Item label="Name">{detail.advocateName || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Bar Council ID">{detail.advocateBarId || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Specialization">{detail.advocateSpecialization || '—'}</Descriptions.Item>
                  <Descriptions.Item label="Phone">{detail.advocatePhone || '—'}</Descriptions.Item>
                </Descriptions>
              </>
            )}

            {/* Verifications */}
            {Array.isArray(detail.verifications) && detail.verifications.length > 0 && (
              <>
                <Title level={5} style={{ marginBottom: 8 }}>Verifications</Title>
                <Table
                  size="small"
                  dataSource={detail.verifications}
                  rowKey={(r: any, i) => r.id || String(i)}
                  pagination={false}
                  style={{ marginBottom: 16 }}
                  columns={[
                    { title: 'Type', dataIndex: 'verificationType', render: (t: string) => t?.replace(/_/g, ' ') || '—' },
                    {
                      title: 'Status', dataIndex: 'status',
                      render: (s: string) => <Tag color={verificationStatusColor[s] ?? 'default'}>{s || 'PENDING'}</Tag>,
                    },
                    { title: 'Findings', dataIndex: 'findings', ellipsis: true },
                  ]}
                />
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
                    { title: 'Uploaded', dataIndex: 'uploadedAt', render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
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
