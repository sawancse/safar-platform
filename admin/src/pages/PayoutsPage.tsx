import { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Table, Tag, Tabs } from 'antd';
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

  const token = localStorage.getItem('admin_token') || '';

  useEffect(() => {
    setLoading(true);
    adminApi.getRecentPayouts(token)
      .then(data => setPayouts(Array.isArray(data) ? data : []))
      .catch(() => setPayouts([]))
      .finally(() => setLoading(false));
  }, []);

  const filtered = activeTab === 'ALL' ? payouts : payouts.filter(p => p.status === activeTab);

  const pendingCount = payouts.filter(p => p.status === 'PENDING').length;
  const completedCount = payouts.filter(p => p.status === 'COMPLETED').length;
  const failedCount = payouts.filter(p => p.status === 'FAILED').length;
  const totalPaidPaise = payouts.filter(p => p.status === 'COMPLETED').reduce((s, p) => s + p.netAmountPaise, 0);

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
      />
    </div>
  );
}
