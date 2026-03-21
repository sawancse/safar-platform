import { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Typography, Spin, Alert } from 'antd';
import {
  HomeOutlined,
  ClockCircleOutlined,
  CalendarOutlined,
  DollarOutlined,
  UserOutlined,
  TeamOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import { adminApi } from '../lib/api';

const { Title } = Typography;

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
  const [data, setData]     = useState<Analytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]   = useState('');
  const [pendingKycCount, setPendingKycCount] = useState(0);

  useEffect(() => {
    adminApi.getAnalytics(token)
      .then(({ data }) => setData(data))
      .catch(() => {
        // Backend endpoint may not exist yet — show zeros
        setData({ totalListings: 0, pendingListings: 0, totalBookings: 0, totalRevenuePaise: 0, activeHosts: 0, activeGuests: 0 });
      })
      .finally(() => setLoading(false));

    adminApi.getPendingKycs(token)
      .then(({ data }) => setPendingKycCount(Array.isArray(data) ? data.length : 0))
      .catch(() => setPendingKycCount(0));
  }, [token]);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />;

  const stats = data ?? { totalListings: 0, pendingListings: 0, totalBookings: 0, totalRevenuePaise: 0, activeHosts: 0, activeGuests: 0 };
  const revenueRupees = (stats.totalRevenuePaise / 100).toLocaleString('en-IN');

  return (
    <div>
      <Title level={4} style={{ marginBottom: 24 }}>Overview</Title>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="Total Listings"
              value={stats.totalListings}
              prefix={<HomeOutlined />}
              valueStyle={{ color: '#f97316' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="Pending Review"
              value={stats.pendingListings}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: stats.pendingListings > 0 ? '#d97706' : '#374151' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="Total Bookings"
              value={stats.totalBookings}
              prefix={<CalendarOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="Total Revenue"
              value={`₹${revenueRupees}`}
              prefix={<DollarOutlined />}
              valueStyle={{ color: '#16a34a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="Active Hosts"
              value={stats.activeHosts}
              prefix={<UserOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="Active Guests"
              value={stats.activeGuests}
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card>
            <Statistic
              title="Pending KYC"
              value={pendingKycCount}
              prefix={<SafetyOutlined />}
              valueStyle={{ color: pendingKycCount > 0 ? '#d97706' : '#374151' }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
