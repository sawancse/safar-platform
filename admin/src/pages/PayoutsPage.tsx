import { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Table, Tag, Tabs, Button, Popconfirm, message } from 'antd';
import { BankOutlined, CheckCircleOutlined, ClockCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { adminApi } from '../lib/api';

function formatPaise(paise: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', minimumFractionDigits: 0,
  }).format(paise / 100);
}

interface Payout {
  id: string;
  hostId: string;
  bookingId: string;
  amountPaise: number;
  tdsPaise: number;
  netAmountPaise: number;
  method: string;
  status: string;
  initiatedAt: string;
  completedAt: string;
  failureReason: string;
}

export default function PayoutsPage() {
  const [payouts, setPayouts] = useState<Payout[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ALL');
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const token = localStorage.getItem('admin_token') || '';

  const reload = () => {
    setLoading(true);
    adminApi.getRecentPayouts(token)
      .then(data => setPayouts(Array.isArray(data) ? data : []))
      .catch(() => setPayouts([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { reload(); }, []);

  const filtered = activeTab === 'ALL' ? payouts : payouts.filter(p => p.status === activeTab);

  const pendingCount = payouts.filter(p => p.status === 'PENDING').length;
  const completedCount = payouts.filter(p => p.status === 'COMPLETED').length;
  const failedCount = payouts.filter(p => p.status === 'FAILED').length;
  const totalPaidPaise = payouts.filter(p => p.status === 'COMPLETED').reduce((s, p) => s + p.netAmountPaise, 0);

  const handleProcessSettlement = async (bookingId: string) => {
    setActionLoading(bookingId);
    try {
      await adminApi.processSettlementByBooking(bookingId, token);
      message.success('Settlement processed successfully');
      reload();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Settlement processing failed');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRetryPayout = async (payoutId: string) => {
    setActionLoading(payoutId);
    try {
      await adminApi.retryPayout(payoutId, token);
      message.success('Payout queued for retry');
      setPayouts(prev => prev.map(p => p.id === payoutId ? { ...p, status: 'PENDING', failureReason: '' } : p));
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Retry failed');
    } finally {
      setActionLoading(null);
    }
  };

  const columns = [
    { title: 'Host ID', dataIndex: 'hostId', key: 'host',
      render: (id: string) => <span className="font-mono text-xs">{id?.substring(0, 8)}...</span> },
    { title: 'Booking', dataIndex: 'bookingId', key: 'booking',
      render: (id: string) => id ? <span className="font-mono text-xs">{id.substring(0, 8)}...</span> : '-' },
    { title: 'Amount', dataIndex: 'amountPaise', key: 'amount',
      render: (v: number) => formatPaise(v || 0) },
    { title: 'TDS', dataIndex: 'tdsPaise', key: 'tds',
      render: (v: number) => v ? formatPaise(v) : '-' },
    { title: 'Net Payout', dataIndex: 'netAmountPaise', key: 'net',
      render: (v: number) => <strong>{formatPaise(v || 0)}</strong> },
    { title: 'Method', dataIndex: 'method', key: 'method',
      render: (m: string) => <Tag>{m || 'NEFT'}</Tag> },
    { title: 'Status', dataIndex: 'status', key: 'status',
      render: (s: string) => (
        <Tag color={s === 'COMPLETED' ? 'green' : s === 'PENDING' ? 'gold' : s === 'PROCESSING' ? 'blue' : 'red'}>
          {s}
        </Tag>
      ) },
    { title: 'Date', dataIndex: 'initiatedAt', key: 'date',
      render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '-' },
    { title: 'Failure Reason', dataIndex: 'failureReason', key: 'failure',
      render: (r: string) => r ? <span style={{ color: '#ff4d4f', fontSize: 12 }}>{r}</span> : '-' },
    {
      title: 'Action', key: 'action', width: 130,
      render: (_: any, r: Payout) => {
        if (r.status === 'PENDING') return (
          <Popconfirm title="Process this settlement manually?" okText="Yes, Process"
            onConfirm={() => handleProcessSettlement(r.bookingId)}>
            <Button size="small" type="primary" loading={actionLoading === r.bookingId}>
              Settle
            </Button>
          </Popconfirm>
        );
        if (r.status === 'FAILED') return (
          <Popconfirm title="Retry this failed payout?" okText="Yes, Retry"
            onConfirm={() => handleRetryPayout(r.id)}>
            <Button size="small" danger loading={actionLoading === r.id}>
              Retry
            </Button>
          </Popconfirm>
        );
        return null;
      },
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Host Payouts</h2>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic title="Total Paid Out" value={totalPaidPaise}
              prefix={<BankOutlined />} formatter={(v) => formatPaise(Number(v))} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Pending" value={pendingCount}
              prefix={<ClockCircleOutlined />} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Completed" value={completedCount}
              prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Failed" value={failedCount}
              prefix={<ExclamationCircleOutlined />} valueStyle={{ color: '#ff4d4f' }} />
          </Card>
        </Col>
      </Row>

      <Tabs activeKey={activeTab} onChange={setActiveTab}
        items={[
          { key: 'ALL', label: `All (${payouts.length})` },
          { key: 'PENDING', label: `Pending (${pendingCount})` },
          { key: 'COMPLETED', label: `Completed (${completedCount})` },
          { key: 'FAILED', label: `Failed (${failedCount})` },
        ]}
      />

      <Table
        dataSource={filtered}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 20, showSizeChanger: true }}
        locale={{ emptyText: 'No payouts yet' }}
        scroll={{ x: 1100 }}
      />
    </div>
  );
}
