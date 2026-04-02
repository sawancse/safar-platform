import { useEffect, useState } from 'react';
import { Card, Statistic, Table, Tabs, Tag, Row, Col, Typography } from 'antd';
import { HeartOutlined, TeamOutlined, CalendarOutlined, HomeOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;

const INR = (paise: number) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
  }).format(paise / 100);

interface Donation {
  id: string;
  razorpayOrderId?: string;
  donorName?: string;
  donorEmail?: string;
  amountPaise: number;
  frequency: string;
  status: string;
  receiptNumber?: string;
  createdAt: string;
}

interface DonationStats {
  totalRaisedPaise: number;
  totalDonors: number;
  monthlyDonors: number;
  familiesHoused: number;
}

const STATUS_COLORS: Record<string, string> = {
  CAPTURED: 'green',
  CREATED: 'blue',
  FAILED: 'red',
};

const FREQ_COLORS: Record<string, string> = {
  MONTHLY: 'purple',
  ONE_TIME: 'default',
};

const columns: ColumnsType<Donation> = [
  {
    title: 'Date',
    dataIndex: 'createdAt',
    key: 'createdAt',
    render: (v: string) =>
      new Date(v).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }),
    sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    defaultSortOrder: 'descend',
  },
  {
    title: 'Ref',
    dataIndex: 'razorpayOrderId',
    key: 'ref',
    render: (v: string) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{v || '-'}</span>,
  },
  {
    title: 'Donor',
    dataIndex: 'donorName',
    key: 'donorName',
    render: (v: string) => v || 'Anonymous',
  },
  {
    title: 'Email',
    dataIndex: 'donorEmail',
    key: 'donorEmail',
    render: (v: string) => v || '-',
  },
  {
    title: 'Amount',
    dataIndex: 'amountPaise',
    key: 'amount',
    align: 'right',
    render: (v: number) => <strong>{INR(v)}</strong>,
    sorter: (a, b) => a.amountPaise - b.amountPaise,
  },
  {
    title: 'Frequency',
    dataIndex: 'frequency',
    key: 'frequency',
    render: (v: string) => (
      <Tag color={FREQ_COLORS[v] || 'default'}>{v === 'MONTHLY' ? 'Monthly' : 'One-time'}</Tag>
    ),
  },
  {
    title: 'Status',
    dataIndex: 'status',
    key: 'status',
    render: (v: string) => <Tag color={STATUS_COLORS[v] || 'default'}>{v}</Tag>,
  },
  {
    title: 'Receipt',
    dataIndex: 'receiptNumber',
    key: 'receipt',
    render: (v: string, record: Donation) =>
      record.status === 'CAPTURED' && v ? <span style={{ color: '#f97316' }}>{v}</span> : '-',
  },
];

export default function DonorsPage() {
  const [donations, setDonations] = useState<Donation[]>([]);
  const [stats, setStats] = useState<DonationStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ALL');

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const token = localStorage.getItem('admin_token') || '';
      const [donationData, statsData] = await Promise.all([
        adminApi.getDonations(token).catch(() => []),
        adminApi.getDonationStats(token).catch(() => null),
      ]);
      setDonations(Array.isArray(donationData) ? donationData : []);
      setStats(statsData);
    } finally {
      setLoading(false);
    }
  }

  const filtered =
    activeTab === 'ALL' ? donations : donations.filter((d) => d.status === activeTab);

  return (
    <div>
      <Title level={3}>Donors &amp; Donations</Title>

      {/* Stats row */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Total Raised"
              value={stats?.totalRaisedPaise ? stats.totalRaisedPaise / 100 : 0}
              prefix={<HeartOutlined style={{ color: '#f97316' }} />}
              formatter={(v) => INR(Number(v) * 100)}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Total Donors"
              value={stats?.totalDonors || 0}
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Monthly SIPs"
              value={stats?.monthlyDonors || 0}
              prefix={<CalendarOutlined style={{ color: '#7c3aed' }} />}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Families Housed"
              value={stats?.familiesHoused || 0}
              prefix={<HomeOutlined style={{ color: '#059669' }} />}
            />
          </Card>
        </Col>
      </Row>

      {/* Tabs + Table */}
      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            { key: 'ALL', label: `All (${donations.length})` },
            { key: 'CAPTURED', label: `Captured (${donations.filter((d) => d.status === 'CAPTURED').length})` },
            { key: 'CREATED', label: `Created (${donations.filter((d) => d.status === 'CREATED').length})` },
            { key: 'FAILED', label: `Failed (${donations.filter((d) => d.status === 'FAILED').length})` },
          ]}
        />
        <Table
          columns={columns}
          dataSource={filtered}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 20, showSizeChanger: true }}
          scroll={{ x: 900 }}
          size="middle"
        />
      </Card>
    </div>
  );
}
