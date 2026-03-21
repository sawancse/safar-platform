import { useEffect, useState } from 'react';
import {
  Table, Card, Tag, Button, Modal, Input, Space, Tabs,
  Statistic, Row, Col, Badge, Descriptions, Progress, message, Select,
} from 'antd';
import {
  CheckCircleOutlined, CloseCircleOutlined, ExclamationCircleOutlined,
  SafetyOutlined, UserOutlined,
} from '@ant-design/icons';
import { adminApi } from '../lib/api';

const { TextArea } = Input;

interface KycRecord {
  id: string;
  userId: string;
  fullLegalName: string;
  dateOfBirth: string;
  aadhaarNumber: string;
  aadhaarVerified: boolean;
  panNumber: string;
  panVerified: boolean;
  addressLine1: string;
  city: string;
  state: string;
  pincode: string;
  bankAccountName: string;
  bankAccountNumber: string;
  bankIfsc: string;
  bankName: string;
  bankVerified: boolean;
  gstin: string;
  gstVerified: boolean;
  businessName: string;
  businessType: string;
  status: string;
  completionPercentage: number;
  submittedAt: string;
  verifiedAt: string;
  rejectedAt: string;
  rejectionReason: string;
  verifiedBy: string;
  trustScore?: number;
  trustBadge?: string;
  verificationLevel?: string;
  fraudRiskScore?: number;
  fraudFlags?: string[];
  hostType?: string;
  residentStatus?: string;
}

const STATUS_COLORS: Record<string, string> = {
  NOT_STARTED: 'default',
  IDENTITY_PENDING: 'processing',
  ADDRESS_PENDING: 'processing',
  BANK_PENDING: 'processing',
  SUBMITTED: 'warning',
  VERIFIED: 'success',
  REJECTED: 'error',
};

export default function KycPage() {
  const [kycs, setKycs] = useState<KycRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('SUBMITTED');
  const [selectedKyc, setSelectedKyc] = useState<KycRecord | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);

  const token = localStorage.getItem('admin_token') || '';

  const fetchKycs = async (status?: string) => {
    setLoading(true);
    try {
      const { data } = await adminApi.getAllKycs(token, status === 'ALL' ? undefined : status);
      setKycs(Array.isArray(data) ? data : []);
    } catch {
      setKycs([]);
    }
    setLoading(false);
  };

  useEffect(() => { fetchKycs(activeTab); }, [activeTab]);

  const handleApprove = async (kycId: string) => {
    setActionLoading(true);
    try {
      await adminApi.approveKyc(kycId, token);
      message.success('KYC approved successfully');
      fetchKycs(activeTab);
      setDetailVisible(false);
    } catch (e: any) {
      message.error(e?.response?.data?.message || e.message || 'Approval failed');
    }
    setActionLoading(false);
  };

  const handleReject = async () => {
    if (!selectedKyc || !rejectReason.trim()) return;
    setActionLoading(true);
    try {
      await adminApi.rejectKyc(selectedKyc.id, rejectReason.trim(), token);
      message.success('KYC rejected');
      setRejectModalVisible(false);
      setRejectReason('');
      fetchKycs(activeTab);
      setDetailVisible(false);
    } catch (e: any) {
      message.error(e?.response?.data?.message || e.message || 'Rejection failed');
    }
    setActionLoading(false);
  };

  const handleBulkApprove = async () => {
    if (selectedRowKeys.length === 0) return;
    setActionLoading(true);
    try {
      await adminApi.bulkApproveKyc(selectedRowKeys, token);
      message.success(`${selectedRowKeys.length} KYCs approved`);
      setSelectedRowKeys([]);
      fetchKycs(activeTab);
    } catch (e: any) {
      message.error(e?.response?.data?.message || e.message || 'Bulk approval failed');
    }
    setActionLoading(false);
  };

  const pendingCount = kycs.filter(k => k.status === 'SUBMITTED').length;
  const verifiedCount = kycs.filter(k => k.status === 'VERIFIED').length;
  const rejectedCount = kycs.filter(k => k.status === 'REJECTED').length;

  const columns = [
    {
      title: 'Host',
      dataIndex: 'fullLegalName',
      key: 'name',
      render: (name: string, record: KycRecord) => (
        <Space>
          <UserOutlined />
          <div>
            <div style={{ fontWeight: 600 }}>{name || 'N/A'}</div>
            <div style={{ fontSize: 12, color: '#999' }}>{record.city}, {record.state}</div>
          </div>
        </Space>
      ),
    },
    {
      title: 'Documents',
      key: 'docs',
      render: (_: any, record: KycRecord) => (
        <Space direction="vertical" size={0}>
          <span>{record.aadhaarVerified ? '\u2713' : '\u25CB'} Aadhaar {record.aadhaarNumber}</span>
          <span>{record.panVerified ? '\u2713' : '\u25CB'} PAN {record.panNumber}</span>
          <span>{record.bankVerified ? '\u2713' : '\u25CB'} Bank {record.bankAccountNumber}</span>
        </Space>
      ),
    },
    {
      title: 'Completion',
      dataIndex: 'completionPercentage',
      key: 'completion',
      render: (pct: number) => <Progress percent={pct} size="small" status={pct === 100 ? 'success' : 'active'} />,
      sorter: (a: KycRecord, b: KycRecord) => a.completionPercentage - b.completionPercentage,
    },
    {
      title: 'Trust Score',
      key: 'trust',
      render: (_: any, record: KycRecord) => (
        <Space>
          <span style={{ fontWeight: 700, fontSize: 16 }}>{record.trustScore ?? 0}</span>
          {record.fraudRiskScore && record.fraudRiskScore > 20 && (
            <Tag color="red">Risk: {record.fraudRiskScore}</Tag>
          )}
        </Space>
      ),
      sorter: (a: KycRecord, b: KycRecord) => (a.trustScore ?? 0) - (b.trustScore ?? 0),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={STATUS_COLORS[status] || 'default'}>{status.replace(/_/g, ' ')}</Tag>
      ),
    },
    {
      title: 'Submitted',
      dataIndex: 'submittedAt',
      key: 'submitted',
      render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '-',
      sorter: (a: KycRecord, b: KycRecord) => new Date(a.submittedAt || 0).getTime() - new Date(b.submittedAt || 0).getTime(),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: KycRecord) => (
        <Space>
          <Button size="small" onClick={() => { setSelectedKyc(record); setDetailVisible(true); }}>
            View
          </Button>
          {record.status === 'SUBMITTED' && (
            <>
              <Button size="small" type="primary" onClick={() => handleApprove(record.id)}
                loading={actionLoading} icon={<CheckCircleOutlined />}>
                Approve
              </Button>
              <Button size="small" danger onClick={() => { setSelectedKyc(record); setRejectModalVisible(true); }}
                icon={<CloseCircleOutlined />}>
                Reject
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Host Identity Verification</h2>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic title="Pending Review" value={pendingCount} valueStyle={{ color: '#faad14' }}
              prefix={<ExclamationCircleOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Verified" value={verifiedCount} valueStyle={{ color: '#52c41a' }}
              prefix={<CheckCircleOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Rejected" value={rejectedCount} valueStyle={{ color: '#ff4d4f' }}
              prefix={<CloseCircleOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Total" value={kycs.length} prefix={<SafetyOutlined />} />
          </Card>
        </Col>
      </Row>

      {selectedRowKeys.length > 0 && (
        <Card size="small" style={{ marginBottom: 16 }}>
          <Space>
            <span>{selectedRowKeys.length} selected</span>
            <Button type="primary" onClick={handleBulkApprove} loading={actionLoading}>
              Bulk Approve ({selectedRowKeys.length})
            </Button>
            <Button onClick={() => setSelectedRowKeys([])}>Clear Selection</Button>
          </Space>
        </Card>
      )}

      <Tabs activeKey={activeTab} onChange={setActiveTab}
        items={[
          { key: 'SUBMITTED', label: <Badge count={pendingCount} offset={[10, 0]}>Pending Review</Badge> },
          { key: 'VERIFIED', label: 'Verified' },
          { key: 'REJECTED', label: 'Rejected' },
          { key: 'ALL', label: 'All' },
        ]}
      />

      <Table
        dataSource={kycs}
        columns={columns}
        rowKey="id"
        loading={loading}
        rowSelection={activeTab === 'SUBMITTED' ? {
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as string[]),
        } : undefined}
        pagination={{ pageSize: 20, showSizeChanger: true }}
      />

      {/* Detail Modal */}
      <Modal
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        width={800}
        title={`KYC Details \u2014 ${selectedKyc?.fullLegalName || 'Host'}`}
        footer={selectedKyc?.status === 'SUBMITTED' ? [
          <Button key="reject" danger onClick={() => { setRejectModalVisible(true); }}
            icon={<CloseCircleOutlined />}>
            Reject
          </Button>,
          <Button key="approve" type="primary" onClick={() => selectedKyc && handleApprove(selectedKyc.id)}
            loading={actionLoading} icon={<CheckCircleOutlined />}>
            Approve
          </Button>,
        ] : null}
      >
        {selectedKyc && (
          <div>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="Trust Score" value={selectedKyc.trustScore ?? 0} suffix="/ 100" />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="Fraud Risk" value={selectedKyc.fraudRiskScore ?? 0} suffix="/ 100"
                    valueStyle={{ color: (selectedKyc.fraudRiskScore ?? 0) > 20 ? '#ff4d4f' : '#52c41a' }} />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic title="Completion" value={selectedKyc.completionPercentage} suffix="%" />
                </Card>
              </Col>
            </Row>

            {selectedKyc.fraudFlags && selectedKyc.fraudFlags.length > 0 && (
              <Card size="small" style={{ marginBottom: 16, borderColor: '#ff4d4f' }}>
                <h4 style={{ color: '#ff4d4f' }}>Fraud Flags</h4>
                {selectedKyc.fraudFlags.map((flag, i) => (
                  <Tag key={i} color="red">{flag}</Tag>
                ))}
              </Card>
            )}

            <Descriptions title="Identity" bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Full Name">{selectedKyc.fullLegalName}</Descriptions.Item>
              <Descriptions.Item label="Date of Birth">{selectedKyc.dateOfBirth}</Descriptions.Item>
              <Descriptions.Item label="Aadhaar">
                {selectedKyc.aadhaarNumber} {selectedKyc.aadhaarVerified ? <Tag color="green">Verified</Tag> : <Tag>Pending</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="PAN">
                {selectedKyc.panNumber} {selectedKyc.panVerified ? <Tag color="green">Verified</Tag> : <Tag>Pending</Tag>}
              </Descriptions.Item>
            </Descriptions>

            <Descriptions title="Address" bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Address" span={2}>{selectedKyc.addressLine1}</Descriptions.Item>
              <Descriptions.Item label="City">{selectedKyc.city}</Descriptions.Item>
              <Descriptions.Item label="State">{selectedKyc.state}</Descriptions.Item>
              <Descriptions.Item label="Pincode">{selectedKyc.pincode}</Descriptions.Item>
            </Descriptions>

            <Descriptions title="Bank / Payout" bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Account Name">{selectedKyc.bankAccountName}</Descriptions.Item>
              <Descriptions.Item label="Account No">{selectedKyc.bankAccountNumber}</Descriptions.Item>
              <Descriptions.Item label="IFSC">{selectedKyc.bankIfsc}</Descriptions.Item>
              <Descriptions.Item label="Bank">
                {selectedKyc.bankName} {selectedKyc.bankVerified ? <Tag color="green">Verified</Tag> : <Tag>Pending</Tag>}
              </Descriptions.Item>
            </Descriptions>

            {(selectedKyc.gstin || selectedKyc.businessName) && (
              <Descriptions title="Business" bordered size="small" column={2} style={{ marginBottom: 16 }}>
                <Descriptions.Item label="Business Name">{selectedKyc.businessName || '-'}</Descriptions.Item>
                <Descriptions.Item label="Type">{selectedKyc.businessType || '-'}</Descriptions.Item>
                <Descriptions.Item label="GSTIN">
                  {selectedKyc.gstin || '-'} {selectedKyc.gstVerified ? <Tag color="green">Verified</Tag> : <Tag>Pending</Tag>}
                </Descriptions.Item>
              </Descriptions>
            )}

            <Descriptions title="Verification Meta" bordered size="small" column={2}>
              <Descriptions.Item label="Host Type">{selectedKyc.hostType || 'INDIVIDUAL'}</Descriptions.Item>
              <Descriptions.Item label="Resident Status">{selectedKyc.residentStatus || 'RESIDENT'}</Descriptions.Item>
              <Descriptions.Item label="Verification Level">{selectedKyc.verificationLevel || 'UNVERIFIED'}</Descriptions.Item>
              <Descriptions.Item label="Status"><Tag color={STATUS_COLORS[selectedKyc.status]}>{selectedKyc.status}</Tag></Descriptions.Item>
              {selectedKyc.rejectionReason && (
                <Descriptions.Item label="Rejection Reason" span={2}>
                  <span style={{ color: '#ff4d4f' }}>{selectedKyc.rejectionReason}</span>
                </Descriptions.Item>
              )}
            </Descriptions>
          </div>
        )}
      </Modal>

      {/* Reject Modal */}
      <Modal
        open={rejectModalVisible}
        onCancel={() => { setRejectModalVisible(false); setRejectReason(''); }}
        title="Reject KYC Verification"
        onOk={handleReject}
        okText="Reject"
        okButtonProps={{ danger: true, loading: actionLoading, disabled: !rejectReason.trim() }}
      >
        <p>Please provide a reason for rejection. The host will see this reason.</p>
        <Select
          style={{ width: '100%', marginBottom: 12 }}
          placeholder="Select common reason..."
          onChange={(value) => setRejectReason(value)}
          options={[
            { value: 'Document is blurry or unreadable', label: 'Document blurry/unreadable' },
            { value: 'Name mismatch between documents', label: 'Name mismatch' },
            { value: 'Document has expired', label: 'Document expired' },
            { value: 'Suspected fraudulent document', label: 'Suspected fraud' },
            { value: 'Incomplete information provided', label: 'Incomplete information' },
          ]}
        />
        <TextArea
          rows={3}
          placeholder="Or type a custom reason..."
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>
    </div>
  );
}
