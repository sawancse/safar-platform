import { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Typography, Spin, Table, Tag, Alert } from 'antd';
import { Link } from 'react-router-dom';
import {
  HomeOutlined, ClockCircleOutlined, CalendarOutlined, DollarOutlined,
  UserOutlined, TeamOutlined, SafetyOutlined, ExclamationCircleOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const statusColor: Record<string, string> = {
  DRAFT: 'default', PENDING_PAYMENT: 'orange', CONFIRMED: 'blue',
  CHECKED_IN: 'cyan', COMPLETED: 'green', CANCELLED: 'red', NO_SHOW: 'volcano',
};

interface Analytics {
  totalListings: number;
  pendingListings: number;
  totalBookings: number;
  totalRevenuePaise: number;
  activeHosts: number;
  activeGuests: number;
}

export default function DashboardPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [data, setData]         = useState<Analytics | null>(null);
  const [loading, setLoading]   = useState(true);
  const [pendingKycCount, setPendingKycCount] = useState(0);
  const [bookingStats, setBookingStats]       = useState<any>(null);
  const [recentBookings, setRecentBookings]   = useState<any[]>([]);
  const [failedPayouts, setFailedPayouts]     = useState(0);
  const [recentUsers, setRecentUsers]         = useState<any[]>([]);

  useEffect(() => {
    const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
    // Load all dashboard data in parallel
    Promise.all([
      adminApi.getAnalytics(token).then(r => r.data).catch(() => null),
      adminApi.getPendingKycs(token).then(r => Array.isArray(r.data) ? r.data.length : 0).catch(() => 0),
      adminApi.getBookingStats(token).catch(() => null),
      adminApi.getBookings(token, { page: 0, size: 10, sortBy: 'createdAt', sortDir: 'desc' })
        .then(r => r.data?.content || []).catch(() => []),
      adminApi.getRecentPayouts(token).then(d => Array.isArray(d) ? d.filter((p: any) => p.status === 'FAILED').length : 0).catch(() => 0),
      adminApi.getUsers(token, { dateFrom: sevenDaysAgo, size: 10 })
        .then((d: any) => d?.content || []).catch(() => []),
    ]).then(([analytics, kycCount, stats, recent, failed, users]) => {
      setData(analytics || { totalListings: 0, pendingListings: 0, totalBookings: 0, totalRevenuePaise: 0, activeHosts: 0, activeGuests: 0 });
      setPendingKycCount(kycCount);
      setBookingStats(stats);
      setRecentBookings(recent);
      setFailedPayouts(failed);
      setRecentUsers(users);
    }).finally(() => setLoading(false));
  }, [token]);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />;

  const stats = data ?? { totalListings: 0, pendingListings: 0, totalBookings: 0, totalRevenuePaise: 0, activeHosts: 0, activeGuests: 0 };
  const revenueRupees = (stats.totalRevenuePaise / 100).toLocaleString('en-IN');
  const trend = bookingStats?.trend || [];

  // Alerts
  const oneDayAgo = Date.now() - 24 * 60 * 60 * 1000;
  const newSignups24h = recentUsers.filter(u => u.createdAt && new Date(u.createdAt).getTime() >= oneDayAgo).length;

  const alerts: { msg: React.ReactNode; type: 'warning' | 'error' | 'info' }[] = [];
  if (newSignups24h > 0) alerts.push({
    msg: <>{newSignups24h} new user{newSignups24h > 1 ? 's' : ''} signed up in the last 24 hours — <Link to="/users">Review</Link></>,
    type: 'info'
  });
  if (stats.pendingListings > 0) alerts.push({ msg: `${stats.pendingListings} listings pending review`, type: 'warning' });
  if (pendingKycCount > 0) alerts.push({ msg: `${pendingKycCount} KYC verifications pending`, type: 'warning' });
  if (failedPayouts > 0) alerts.push({ msg: `${failedPayouts} failed payouts need attention`, type: 'error' });

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>Overview</Title>

      {/* Alerts */}
      {alerts.length > 0 && (
        <div style={{ marginBottom: 20 }}>
          {alerts.map((a, i) => (
            <Alert key={i} message={a.msg} type={a.type} showIcon closable
              icon={<ExclamationCircleOutlined />} style={{ marginBottom: 8 }} />
          ))}
        </div>
      )}

      {/* Stat cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} lg={6}>
          <Card size="small"><Statistic title="Total Listings" value={stats.totalListings} prefix={<HomeOutlined />} valueStyle={{ color: '#f97316' }} /></Card>
        </Col>
        <Col xs={12} lg={6}>
          <Card size="small"><Statistic title="Total Bookings" value={stats.totalBookings} prefix={<CalendarOutlined />} /></Card>
        </Col>
        <Col xs={12} lg={6}>
          <Card size="small"><Statistic title="Total Revenue" value={`₹${revenueRupees}`} prefix={<DollarOutlined />} valueStyle={{ color: '#16a34a' }} /></Card>
        </Col>
        <Col xs={12} lg={6}>
          <Card size="small"><Statistic title="Active Hosts" value={stats.activeHosts} prefix={<UserOutlined />} /></Card>
        </Col>
      </Row>

      {/* Charts */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="Bookings (Last 30 Days)" size="small">
            {trend.length > 0 ? (
              <ResponsiveContainer width="100%" height={240}>
                <AreaChart data={trend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" tickFormatter={(d: string) => d.slice(5)} fontSize={11} />
                  <YAxis fontSize={11} />
                  <Tooltip />
                  <Area type="monotone" dataKey="count" stroke="#1677ff" fill="#e6f4ff" name="Bookings" />
                </AreaChart>
              </ResponsiveContainer>
            ) : <Text type="secondary">No data yet</Text>}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Revenue (Last 30 Days)" size="small">
            {trend.length > 0 ? (
              <ResponsiveContainer width="100%" height={240}>
                <AreaChart data={trend.map((t: any) => ({ ...t, revenue: (t.revenuePaise || 0) / 100 }))}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" tickFormatter={(d: string) => d.slice(5)} fontSize={11} />
                  <YAxis fontSize={11} tickFormatter={(v: number) => `₹${v.toLocaleString('en-IN')}`} />
                  <Tooltip formatter={(v: number) => [`₹${v.toLocaleString('en-IN')}`, 'Revenue']} />
                  <Area type="monotone" dataKey="revenue" stroke="#16a34a" fill="#f0fdf4" name="Revenue" />
                </AreaChart>
              </ResponsiveContainer>
            ) : <Text type="secondary">No data yet</Text>}
          </Card>
        </Col>
      </Row>

      {/* Recent Bookings + Recent Signups */}
      <Row gutter={16}>
        <Col xs={24} lg={14}>
          <Card title="Recent Bookings" size="small">
            <Table
              dataSource={recentBookings}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'No bookings yet' }}
              columns={[
                { title: 'Ref', dataIndex: 'bookingRef', width: 110, ellipsis: true },
                { title: 'Guest', render: (_, r: any) => `${r.guestFirstName || ''} ${r.guestLastName || ''}`.trim() || '—', width: 140 },
                { title: 'Listing', dataIndex: 'listingTitle', width: 180, ellipsis: true },
                { title: 'Check-in', dataIndex: 'checkIn', width: 100, render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
                { title: 'Amount', dataIndex: 'totalAmountPaise', width: 100, render: (v: number) => v ? INR(v) : '—' },
                { title: 'Status', dataIndex: 'status', width: 110, render: (s: string) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag> },
                { title: 'Created', dataIndex: 'createdAt', width: 100, render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card
            size="small"
            title={<><UserAddOutlined style={{ marginRight: 8 }} />Recent Signups (last 7 days)</>}
            extra={<Link to="/users">View all</Link>}
          >
            <Table
              dataSource={recentUsers}
              rowKey="userId"
              size="small"
              pagination={false}
              locale={{ emptyText: 'No recent signups' }}
              columns={[
                {
                  title: 'User', render: (_, r: any) => (
                    <div>
                      <div style={{ fontWeight: 500 }}>
                        {r.name || '—'}
                        {r.createdAt && (Date.now() - new Date(r.createdAt).getTime() < 24 * 60 * 60 * 1000) && (
                          <Tag color="green" style={{ marginLeft: 6, fontSize: 10 }}>NEW</Tag>
                        )}
                      </div>
                      <div style={{ fontSize: 11, color: '#888' }}>{r.email || r.phone || '—'}</div>
                    </div>
                  ),
                },
                { title: 'Role', dataIndex: 'role', width: 70, render: (v: string) => <Tag>{v || 'GUEST'}</Tag> },
                {
                  title: 'Joined', dataIndex: 'createdAt', width: 100,
                  render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : '—',
                },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
