import { useEffect, useState } from 'react';
import { Table, Tag, Typography, Tabs, Card, Row, Col, Statistic, Spin, Input, Avatar, Button, Space, message, Popconfirm } from 'antd';
import { FireOutlined, CalendarOutlined, TeamOutlined, SearchOutlined, UserOutlined, CheckCircleOutlined, CloseCircleOutlined, StopOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;
const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const bookingStatusColor: Record<string, string> = {
  PENDING: 'orange', CONFIRMED: 'blue', IN_PROGRESS: 'cyan',
  COMPLETED: 'green', CANCELLED: 'red', NO_SHOW: 'volcano',
};
const eventStatusColor: Record<string, string> = {
  INQUIRY: 'default', QUOTED: 'orange', CONFIRMED: 'blue',
  ADVANCE_PAID: 'cyan', IN_PROGRESS: 'purple', COMPLETED: 'green', CANCELLED: 'red',
};
const subStatusColor: Record<string, string> = {
  ACTIVE: 'green', PAUSED: 'orange', CANCELLED: 'red', EXPIRED: 'default',
};
const verifyColor: Record<string, string> = {
  PENDING: 'orange', VERIFIED: 'green', REJECTED: 'red', SUSPENDED: 'volcano',
};

export default function CooksPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [chefs, setChefs]         = useState<any[]>([]);
  const [bookings, setBookings]   = useState<any[]>([]);
  const [events, setEvents]       = useState<any[]>([]);
  const [subs, setSubs]           = useState<any[]>([]);
  const [loading, setLoading]     = useState(true);
  const [search, setSearch]       = useState('');

  useEffect(() => {
    Promise.all([
      adminApi.getChefs(token).then((d: any) => d?.content || []),
      adminApi.getChefBookings(token),
      adminApi.getChefEvents(token),
      adminApi.getChefSubscriptions(token),
    ]).then(([c, b, e, s]) => {
      setChefs(Array.isArray(c) ? c : []);
      setBookings(Array.isArray(b) ? b : []);
      setEvents(Array.isArray(e) ? e : []);
      setSubs(Array.isArray(s) ? s : []);
    }).finally(() => setLoading(false));
  }, [token]);

  const reload = () => {
    adminApi.getChefs(token).then((d: any) => setChefs(d?.content || []));
  };

  const handleVerify = async (chefId: string) => {
    try {
      await adminApi.verifyChef(chefId, token);
      message.success('Chef verified');
      reload();
    } catch { message.error('Failed to verify'); }
  };

  const handleReject = async (chefId: string) => {
    try {
      await adminApi.rejectChef(chefId, 'Rejected by admin', token);
      message.success('Chef rejected');
      reload();
    } catch { message.error('Failed to reject'); }
  };

  const handleSuspend = async (chefId: string) => {
    try {
      await adminApi.suspendChef(chefId, token);
      message.success('Chef suspended');
      reload();
    } catch { message.error('Failed to suspend'); }
  };

  const filteredChefs = chefs.filter(c => {
    if (!search) return true;
    const q = search.toLowerCase();
    return c.name?.toLowerCase().includes(q) || c.city?.toLowerCase().includes(q) || c.cuisines?.toLowerCase().includes(q);
  });

  const chefCols: ColumnsType<any> = [
    {
      title: 'Chef', width: 200,
      render: (_, r) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Avatar icon={<UserOutlined />} src={r.profilePhotoUrl} style={{ backgroundColor: '#f97316' }} />
          <div>
            <div style={{ fontWeight: 600 }}>{r.name || '—'}</div>
            <div style={{ fontSize: 12, color: '#6b7280' }}>{r.phone || '—'}</div>
          </div>
        </div>
      ),
    },
    { title: 'City', dataIndex: 'city', width: 100 },
    { title: 'Type', dataIndex: 'chefType', width: 100, render: (t: string) => <Tag>{t}</Tag> },
    { title: 'Cuisines', dataIndex: 'cuisines', width: 180, ellipsis: true },
    { title: 'Exp', dataIndex: 'experienceYears', width: 60, render: (y: number) => `${y || 0}yr` },
    { title: 'Daily Rate', dataIndex: 'dailyRatePaise', width: 100, render: (v: number) => v ? INR(v) : '—' },
    { title: 'Rating', dataIndex: 'rating', width: 80, render: (r: number) => r ? `${r.toFixed(1)} ★` : '—' },
    { title: 'Bookings', dataIndex: 'totalBookings', width: 80 },
    { title: 'Status', dataIndex: 'verificationStatus', width: 100,
      render: (s: string) => <Tag color={verifyColor[s] ?? 'default'}>{s}</Tag> },
    { title: 'Available', dataIndex: 'available', width: 80,
      render: (a: boolean) => <Tag color={a ? 'green' : 'red'}>{a ? 'Yes' : 'No'}</Tag> },
    { title: 'Actions', width: 180, fixed: 'right' as const,
      render: (_: any, r: any) => (
        <Space size="small">
          {r.verificationStatus === 'PENDING' && (
            <>
              <Button type="primary" size="small" icon={<CheckCircleOutlined />}
                onClick={() => handleVerify(r.id)}>Verify</Button>
              <Popconfirm title="Reject this chef?" onConfirm={() => handleReject(r.id)}>
                <Button danger size="small" icon={<CloseCircleOutlined />}>Reject</Button>
              </Popconfirm>
            </>
          )}
          {r.verificationStatus === 'VERIFIED' && (
            <Popconfirm title="Suspend this chef?" onConfirm={() => handleSuspend(r.id)}>
              <Button size="small" icon={<StopOutlined />}>Suspend</Button>
            </Popconfirm>
          )}
          {r.verificationStatus === 'REJECTED' && (
            <Button type="primary" size="small" icon={<CheckCircleOutlined />}
              onClick={() => handleVerify(r.id)}>Verify</Button>
          )}
          {r.verificationStatus === 'SUSPENDED' && (
            <Button type="primary" size="small" icon={<CheckCircleOutlined />}
              onClick={() => handleVerify(r.id)}>Restore</Button>
          )}
        </Space>
      ),
    },
  ];

  const bookingCols: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'bookingRef', width: 110 },
    { title: 'Chef', dataIndex: 'chefName', width: 140, ellipsis: true },
    { title: 'Customer', dataIndex: 'customerName', width: 140, ellipsis: true },
    { title: 'Date', dataIndex: 'serviceDate', width: 100 },
    { title: 'Meal', dataIndex: 'mealType', width: 80 },
    { title: 'Guests', dataIndex: 'guestsCount', width: 70 },
    { title: 'Amount', dataIndex: 'totalAmountPaise', width: 100, render: (v: number) => v ? INR(v) : '—' },
    { title: 'Status', dataIndex: 'status', width: 110,
      render: (s: string) => <Tag color={bookingStatusColor[s] ?? 'default'}>{s}</Tag> },
  ];

  const eventCols: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'bookingRef', width: 110 },
    { title: 'Chef', dataIndex: 'chefName', width: 130, ellipsis: true },
    { title: 'Customer', dataIndex: 'customerName', width: 130, ellipsis: true },
    { title: 'Event', dataIndex: 'eventType', width: 100, render: (t: string) => <Tag>{t}</Tag> },
    { title: 'Date', dataIndex: 'eventDate', width: 100 },
    { title: 'Guests', dataIndex: 'guestCount', width: 70 },
    { title: 'Amount', dataIndex: 'totalAmountPaise', width: 100, render: (v: number) => v ? INR(v) : '—' },
    { title: 'Advance', dataIndex: 'advanceAmountPaise', width: 100, render: (v: number) => v ? INR(v) : '—' },
    { title: 'Status', dataIndex: 'status', width: 110,
      render: (s: string) => <Tag color={eventStatusColor[s] ?? 'default'}>{s}</Tag> },
  ];

  const subCols: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'subscriptionRef', width: 110 },
    { title: 'Chef', dataIndex: 'chefName', width: 140, ellipsis: true },
    { title: 'Customer', dataIndex: 'customerName', width: 140, ellipsis: true },
    { title: 'Plan', dataIndex: 'plan', width: 100, render: (p: string) => <Tag>{p}</Tag> },
    { title: 'Monthly', dataIndex: 'monthlyRatePaise', width: 100, render: (v: number) => v ? INR(v) : '—' },
    { title: 'Start', dataIndex: 'startDate', width: 100 },
    { title: 'End', dataIndex: 'endDate', width: 100 },
    { title: 'Status', dataIndex: 'status', width: 100,
      render: (s: string) => <Tag color={subStatusColor[s] ?? 'default'}>{s}</Tag> },
  ];

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>Safar Cooks</Title>
        <Input prefix={<SearchOutlined />} placeholder="Search chefs..."
          value={search} onChange={e => setSearch(e.target.value)} style={{ width: 260 }} allowClear />
      </div>

      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={6}><Card size="small"><Statistic title="Total Chefs" value={chefs.length} prefix={<FireOutlined />} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="Bookings" value={bookings.length} prefix={<CalendarOutlined />} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="Events" value={events.length} prefix={<TeamOutlined />} /></Card></Col>
        <Col span={6}><Card size="small"><Statistic title="Subscriptions" value={subs.length} valueStyle={{ color: '#52c41a' }} /></Card></Col>
      </Row>

      <Tabs defaultActiveKey="chefs" items={[
        {
          key: 'chefs', label: `Chefs (${chefs.length})`,
          children: <Table columns={chefCols} dataSource={filteredChefs} rowKey="id" scroll={{ x: 1100 }}
            pagination={{ pageSize: 20 }} locale={{ emptyText: 'No chefs registered' }} />,
        },
        {
          key: 'bookings', label: `Bookings (${bookings.length})`,
          children: <Table columns={bookingCols} dataSource={bookings} rowKey="id" scroll={{ x: 900 }}
            pagination={{ pageSize: 20 }} locale={{ emptyText: 'No bookings' }} />,
        },
        {
          key: 'events', label: `Events (${events.length})`,
          children: <Table columns={eventCols} dataSource={events} rowKey="id" scroll={{ x: 1000 }}
            pagination={{ pageSize: 20 }} locale={{ emptyText: 'No events' }} />,
        },
        {
          key: 'subscriptions', label: `Subscriptions (${subs.length})`,
          children: <Table columns={subCols} dataSource={subs} rowKey="id" scroll={{ x: 900 }}
            pagination={{ pageSize: 20 }} locale={{ emptyText: 'No subscriptions' }} />,
        },
      ]} />
    </div>
  );
}
