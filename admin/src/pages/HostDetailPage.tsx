import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Tabs, Table, Tag, Typography, Spin, Statistic, Row, Col, Button, Avatar, Descriptions, Popconfirm, message,
} from 'antd';
import {
  ArrowLeftOutlined, UserOutlined, CalendarOutlined, DollarOutlined, HomeOutlined, CheckCircleOutlined,
  LoginOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const statusColor: Record<string, string> = {
  DRAFT: 'default', PENDING_PAYMENT: 'orange', CONFIRMED: 'blue',
  CHECKED_IN: 'cyan', COMPLETED: 'green', CANCELLED: 'red', NO_SHOW: 'volcano',
};

export default function HostDetailPage() {
  const { hostId } = useParams<{ hostId: string }>();
  const navigate = useNavigate();
  const token = localStorage.getItem('admin_token') ?? '';

  const [host, setHost]         = useState<any>(null);
  const [bookings, setBookings] = useState<any[]>([]);
  const [payouts, setPayouts]   = useState<any[]>([]);
  const [listings, setListings] = useState<any[]>([]);
  const [loading, setLoading]   = useState(true);

  useEffect(() => {
    if (!hostId) return;
    Promise.all([
      adminApi.getHosts(token).then(r => r.data.find((h: any) => h.id === hostId)).catch(() => null),
      adminApi.getBookingsByHost(hostId, token).then(r => r.data).catch(() => []),
      adminApi.getPayoutsByHost(hostId, token).catch(() => []),
      adminApi.getListingsByStatus(token).then(r => r.data.filter((l: any) => l.hostId === hostId)).catch(() => []),
    ]).then(([h, b, p, l]) => {
      setHost(h);
      setBookings(Array.isArray(b) ? b : []);
      setPayouts(Array.isArray(p) ? p : []);
      setListings(Array.isArray(l) ? l : []);
    }).finally(() => setLoading(false));
  }, [hostId, token]);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />;

  const totalEarnings = bookings
    .filter((b: any) => b.status === 'COMPLETED' || b.status === 'CONFIRMED')
    .reduce((sum: number, b: any) => sum + (b.totalAmountPaise || 0), 0);
  const totalPaidOut = payouts
    .filter((p: any) => p.status === 'COMPLETED')
    .reduce((sum: number, p: any) => sum + (p.netAmountPaise || 0), 0);

  // ── Booking columns ────────────────────────────────────────────────
  const bookingCols: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'bookingRef', width: 120, ellipsis: true },
    { title: 'Guest', render: (_, r) => `${r.guestFirstName || ''} ${r.guestLastName || ''}`.trim() || '—', width: 150 },
    { title: 'Listing', dataIndex: 'listingTitle', width: 180, ellipsis: true },
    { title: 'Check-in', dataIndex: 'checkIn', width: 110, render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    { title: 'Check-out', dataIndex: 'checkOut', width: 110, render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    { title: 'Amount', dataIndex: 'totalAmountPaise', width: 100, render: (v) => v ? INR(v) : '—' },
    { title: 'Status', dataIndex: 'status', width: 110, render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag> },
    {
      title: 'Action', width: 100,
      render: (_, r) => (r.status === 'CONFIRMED' || r.status === 'CHECKED_IN') ? (
        <Popconfirm title="Cancel this booking?" onConfirm={async () => {
          await adminApi.adminCancelBooking(r.id, 'Cancelled by admin', token);
          message.success('Booking cancelled');
          setBookings(prev => prev.map(b => b.id === r.id ? { ...b, status: 'CANCELLED' } : b));
        }}>
          <Button size="small" danger>Cancel</Button>
        </Popconfirm>
      ) : null,
    },
  ];

  // ── Payout columns ─────────────────────────────────────────────────
  const payoutCols: ColumnsType<any> = [
    { title: 'Booking', dataIndex: 'bookingId', width: 120, ellipsis: true,
      render: (id) => id ? id.substring(0, 8) + '...' : '—' },
    { title: 'Amount', dataIndex: 'amountPaise', width: 100, render: (v) => v ? INR(v) : '—' },
    { title: 'TDS', dataIndex: 'tdsPaise', width: 80, render: (v) => v ? INR(v) : '—' },
    { title: 'Net', dataIndex: 'netAmountPaise', width: 100, render: (v) => v ? INR(v) : '—',
      onCell: () => ({ style: { fontWeight: 600 } }) },
    { title: 'Method', dataIndex: 'method', width: 80 },
    { title: 'Status', dataIndex: 'status', width: 100,
      render: (s) => <Tag color={s === 'COMPLETED' ? 'green' : s === 'PENDING' ? 'gold' : s === 'FAILED' ? 'red' : 'blue'}>{s}</Tag> },
    { title: 'Date', dataIndex: 'initiatedAt', width: 110, render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    {
      title: 'Action', width: 120,
      render: (_, r) => {
        if (r.status === 'FAILED') return (
          <Popconfirm title="Retry this payout?" onConfirm={async () => {
            await adminApi.retryPayout(r.id, token);
            message.success('Payout queued for retry');
            setPayouts(prev => prev.map(p => p.id === r.id ? { ...p, status: 'PENDING', failureReason: null } : p));
          }}>
            <Button size="small" type="primary">Retry</Button>
          </Popconfirm>
        );
        if (r.status === 'PENDING') return (
          <Popconfirm title="Process this settlement?" onConfirm={async () => {
            try {
              await adminApi.processSettlementByBooking(r.bookingId, token);
              message.success('Settlement processed');
            } catch { message.error('Settlement failed'); }
          }}>
            <Button size="small">Settle</Button>
          </Popconfirm>
        );
        return null;
      },
    },
  ];

  // ── Listing columns ────────────────────────────────────────────────
  const listingCols: ColumnsType<any> = [
    { title: 'Title', dataIndex: 'title', width: 220, ellipsis: true },
    { title: 'City', dataIndex: 'city', width: 120 },
    { title: 'Type', dataIndex: 'type', width: 100, render: (t) => <Tag>{t}</Tag> },
    { title: 'Price/Night', dataIndex: 'basePricePaise', width: 110,
      render: (v) => v ? INR(v) : '—' },
    { title: 'Status', dataIndex: 'status', width: 120,
      render: (s) => {
        const c: Record<string, string> = { VERIFIED: 'green', DRAFT: 'default', PAUSED: 'orange', SUSPENDED: 'red', PENDING_VERIFICATION: 'blue' };
        return <Tag color={c[s] ?? 'default'}>{s}</Tag>;
      },
    },
  ];

  return (
    <div>
      <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate('/hosts')}
        style={{ padding: 0, marginBottom: 16 }}>
        Back to Hosts
      </Button>

      {/* Host Overview Card */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
          <Avatar size={64} icon={<UserOutlined />} style={{ backgroundColor: '#f97316' }} />
          <div style={{ flex: 1 }}>
            <Title level={4} style={{ margin: 0 }}>{host?.name || 'Unknown Host'}</Title>
            <Text type="secondary">{host?.email || '—'} | {host?.phone || '—'}</Text>
            <div style={{ marginTop: 4 }}>
              <Tag color={host?.subscriptionTier === 'PRO' ? 'purple' : host?.subscriptionTier === 'ENTERPRISE' ? 'gold' : 'default'}>
                {host?.subscriptionTier || 'FREE'}
              </Tag>
              {host?.role === 'ADMIN' && <Tag color="red">ADMIN</Tag>}
            </div>
          </div>
          <Popconfirm
            title="Login as this host?"
            description="You will be redirected to the host dashboard in a new tab."
            onConfirm={async () => {
              try {
                const res = await adminApi.impersonateUser(hostId!, token);
                const { accessToken, refreshToken, user } = res.data;
                // Open safar-web in a new tab with impersonated session
                const webUrl = window.location.origin.replace(/:\d+$/, ':3000');
                const params = new URLSearchParams({
                  impersonate: 'true',
                  token: accessToken,
                  refreshToken,
                  userId: user.id,
                  name: user.name || '',
                  role: user.role,
                });
                window.open(`${webUrl}/auth?${params.toString()}`, '_blank');
                message.success(`Logged in as ${user.name || host?.name}`);
              } catch (e: any) {
                message.error(e.response?.data?.message || 'Failed to impersonate host');
              }
            }}
          >
            <Button type="primary" icon={<LoginOutlined />} ghost>
              Login as Host
            </Button>
          </Popconfirm>
        </div>

        <Row gutter={24}>
          <Col span={6}>
            <Statistic title="Total Bookings" value={bookings.length} prefix={<CalendarOutlined />} />
          </Col>
          <Col span={6}>
            <Statistic title="Total Earnings" value={INR(totalEarnings)} prefix={<DollarOutlined />}
              valueStyle={{ color: '#3f8600' }} />
          </Col>
          <Col span={6}>
            <Statistic title="Paid Out" value={INR(totalPaidOut)} prefix={<CheckCircleOutlined />} />
          </Col>
          <Col span={6}>
            <Statistic title="Active Listings" value={listings.filter(l => l.status === 'VERIFIED').length}
              prefix={<HomeOutlined />} />
          </Col>
        </Row>
      </Card>

      {/* Tabs */}
      <Tabs defaultActiveKey="bookings" items={[
        {
          key: 'bookings',
          label: `Bookings (${bookings.length})`,
          children: (
            <Table columns={bookingCols} dataSource={bookings} rowKey="id"
              scroll={{ x: 900 }} pagination={{ pageSize: 15 }}
              locale={{ emptyText: 'No bookings' }} />
          ),
        },
        {
          key: 'earnings',
          label: `Earnings & Payouts (${payouts.length})`,
          children: (
            <Table columns={payoutCols} dataSource={payouts} rowKey="id"
              scroll={{ x: 800 }} pagination={{ pageSize: 15 }}
              locale={{ emptyText: 'No payouts' }} />
          ),
        },
        {
          key: 'listings',
          label: `Listings (${listings.length})`,
          children: (
            <Table columns={listingCols} dataSource={listings} rowKey="id"
              scroll={{ x: 700 }} pagination={{ pageSize: 15 }}
              locale={{ emptyText: 'No listings' }} />
          ),
        },
      ]} />
    </div>
  );
}
