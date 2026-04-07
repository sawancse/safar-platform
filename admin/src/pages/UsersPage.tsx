import { useState, useEffect } from 'react';
import { Table, Tag, Card, Row, Col, Statistic, Select, Input, Button, DatePicker, Tabs, Modal, Descriptions } from 'antd';
import { UserOutlined, TeamOutlined, SearchOutlined, MailOutlined, RiseOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { RangePicker } = DatePicker;

const ROLE_COLORS: Record<string, string> = {
  GUEST: 'blue', HOST: 'green', BOTH: 'purple', ADMIN: 'red',
};

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'green', SUSPENDED: 'orange', BANNED: 'red',
};

const TIER_COLORS: Record<string, string> = {
  BRONZE: 'default', SILVER: 'blue', GOLD: 'gold', PLATINUM: 'purple',
};

export default function UsersPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<any>(null);
  const [leads, setLeads] = useState<any[]>([]);
  const [leadsLoading, setLeadsLoading] = useState(false);
  const [detail, setDetail] = useState<any>(null);

  // Filters
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [dateRange, setDateRange] = useState<[string, string] | null>(null);

  const loadUsers = () => {
    setLoading(true);
    const params: Record<string, any> = {};
    if (role) params.role = role;
    if (status) params.status = status;
    if (search) params.search = search;
    if (dateRange) { params.dateFrom = dateRange[0]; params.dateTo = dateRange[1]; }
    adminApi.getUsers(token, params)
      .then((data: any) => setUsers(data.content || []))
      .catch(() => setUsers([]))
      .finally(() => setLoading(false));
  };

  const loadStats = () => {
    adminApi.getUserStats(token).then(setStats).catch(() => {});
  };

  const loadLeads = () => {
    setLeadsLoading(true);
    adminApi.getLeads(token)
      .then((data: any) => setLeads(data.content || []))
      .catch(() => setLeads([]))
      .finally(() => setLeadsLoading(false));
  };

  useEffect(() => { loadUsers(); loadStats(); }, [role, status]);

  const userColumns: ColumnsType<any> = [
    {
      title: 'User', key: 'user', width: 220,
      render: (_, r) => (
        <a onClick={() => setDetail(r)}>
          <div className="font-medium">{r.name || '—'}</div>
          <div style={{ fontSize: 11, color: '#888' }}>{r.email || r.phone || r.userId?.substring(0, 12)}</div>
        </a>
      ),
    },
    { title: 'Phone', dataIndex: 'phone', width: 130, render: (v: string) => v || '—' },
    {
      title: 'Role', dataIndex: 'role', width: 90,
      render: (v: string) => <Tag color={ROLE_COLORS[v] || 'default'}>{v}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'accountStatus', width: 100,
      render: (v: string) => <Tag color={STATUS_COLORS[v] || 'green'}>{v || 'ACTIVE'}</Tag>,
    },
    {
      title: 'Loyalty', dataIndex: 'loyaltyTier', width: 90,
      render: (v: string) => v ? <Tag color={TIER_COLORS[v]}>{v}</Tag> : '—',
    },
    {
      title: 'Verified', dataIndex: 'verificationLevel', width: 80,
      render: (v: string) => v === 'VERIFIED' ? <Tag color="green">Yes</Tag> : <Tag>No</Tag>,
    },
    {
      title: 'Star Host', dataIndex: 'starHost', width: 80,
      render: (v: boolean) => v ? <Tag color="gold">Star</Tag> : '—',
    },
    {
      title: 'Stays', dataIndex: 'completedStays', width: 70, align: 'center',
      render: (v: number) => v || 0,
    },
    {
      title: 'Last Active', dataIndex: 'lastActiveAt', width: 110,
      render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
    },
    {
      title: 'Joined', dataIndex: 'createdAt', width: 110,
      render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
      sorter: (a: any, b: any) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    },
  ];

  const leadColumns: ColumnsType<any> = [
    { title: 'Email', dataIndex: 'email', width: 220 },
    { title: 'Name', dataIndex: 'name', width: 150, render: (v: string) => v || '—' },
    { title: 'City', dataIndex: 'city', width: 120, render: (v: string) => v || '—' },
    { title: 'Source', dataIndex: 'source', width: 130, render: (v: string) => <Tag>{v}</Tag> },
    {
      title: 'Subscribed', dataIndex: 'subscribed', width: 90,
      render: (v: boolean) => v ? <Tag color="green">Yes</Tag> : <Tag color="red">No</Tag>,
    },
    {
      title: 'Converted', dataIndex: 'converted', width: 90,
      render: (v: boolean) => v ? <Tag color="blue">Yes</Tag> : '—',
    },
    {
      title: 'Captured', dataIndex: 'createdAt', width: 120,
      render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}><TeamOutlined style={{ marginRight: 8 }} />Users & Leads</h2>

      {/* Stats */}
      {stats && (
        <Row gutter={16} style={{ marginBottom: 20 }}>
          <Col span={4}><Card size="small"><Statistic title="Total Users" value={stats.totalUsers} prefix={<UserOutlined />} /></Card></Col>
          <Col span={4}><Card size="small"><Statistic title="New This Week" value={stats.newThisWeek} prefix={<RiseOutlined />} valueStyle={{ color: '#3f8600' }} /></Card></Col>
          <Col span={4}><Card size="small"><Statistic title="New This Month" value={stats.newThisMonth} /></Card></Col>
          <Col span={4}><Card size="small"><Statistic title="Hosts" value={stats.byRole?.HOST || 0} valueStyle={{ color: '#52c41a' }} /></Card></Col>
          <Col span={4}><Card size="small"><Statistic title="Total Leads" value={stats.totalLeads} prefix={<MailOutlined />} /></Card></Col>
          <Col span={4}><Card size="small"><Statistic title="Leads This Week" value={stats.leadsThisWeek} valueStyle={{ color: '#1890ff' }} /></Card></Col>
        </Row>
      )}

      <Tabs defaultActiveKey="users" onChange={k => { if (k === 'leads' && leads.length === 0) loadLeads(); }}
        items={[
          {
            key: 'users', label: `Users (${users.length})`,
            children: (
              <>
                <div style={{ display: 'flex', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
                  <Select placeholder="Role" allowClear style={{ width: 120 }} value={role || undefined}
                    onChange={v => setRole(v || '')}
                    options={[
                      { label: 'All', value: '' },
                      { label: 'Guest', value: 'GUEST' },
                      { label: 'Host', value: 'HOST' },
                      { label: 'Both', value: 'BOTH' },
                      { label: 'Admin', value: 'ADMIN' },
                    ]} />
                  <Select placeholder="Status" allowClear style={{ width: 130 }} value={status || undefined}
                    onChange={v => setStatus(v || '')}
                    options={[
                      { label: 'All', value: '' },
                      { label: 'Active', value: 'ACTIVE' },
                      { label: 'Suspended', value: 'SUSPENDED' },
                      { label: 'Banned', value: 'BANNED' },
                    ]} />
                  <Input prefix={<SearchOutlined />} placeholder="Name, email, phone..."
                    value={search} onChange={e => setSearch(e.target.value)}
                    onPressEnter={loadUsers} style={{ width: 220 }} allowClear />
                  <RangePicker onChange={(_, ds) => setDateRange(ds[0] ? [ds[0], ds[1]] : null)} />
                  <Button type="primary" onClick={loadUsers}>Search</Button>
                </div>
                <Table columns={userColumns} dataSource={users} rowKey="userId" loading={loading}
                  scroll={{ x: 1200 }} size="small"
                  pagination={{ pageSize: 20, showSizeChanger: true, showTotal: t => `${t} users` }} />
              </>
            ),
          },
          {
            key: 'leads', label: `Leads`,
            children: (
              <>
                <Button onClick={loadLeads} style={{ marginBottom: 12 }}>Refresh</Button>
                <Table columns={leadColumns} dataSource={leads} rowKey="id" loading={leadsLoading}
                  scroll={{ x: 900 }} size="small"
                  pagination={{ pageSize: 20, showSizeChanger: true, showTotal: t => `${t} leads` }} />
              </>
            ),
          },
        ]} />

      {/* User Detail Modal */}
      <Modal open={!!detail} onCancel={() => setDetail(null)} width={600} footer={null}
        title={`User: ${detail?.name || detail?.userId?.substring(0, 12)}`}>
        {detail && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="Name">{detail.name || '—'}</Descriptions.Item>
            <Descriptions.Item label="Email">{detail.email || '—'}</Descriptions.Item>
            <Descriptions.Item label="Phone">{detail.phone || '—'}</Descriptions.Item>
            <Descriptions.Item label="Role"><Tag color={ROLE_COLORS[detail.role]}>{detail.role}</Tag></Descriptions.Item>
            <Descriptions.Item label="Status"><Tag color={STATUS_COLORS[detail.accountStatus || 'ACTIVE']}>{detail.accountStatus || 'ACTIVE'}</Tag></Descriptions.Item>
            <Descriptions.Item label="Verification">{detail.verificationLevel || '—'}</Descriptions.Item>
            <Descriptions.Item label="Loyalty Tier">{detail.loyaltyTier ? <Tag color={TIER_COLORS[detail.loyaltyTier]}>{detail.loyaltyTier}</Tag> : '—'}</Descriptions.Item>
            <Descriptions.Item label="Star Host">{detail.starHost ? 'Yes' : 'No'}</Descriptions.Item>
            <Descriptions.Item label="Completed Stays">{detail.completedStays || 0}</Descriptions.Item>
            <Descriptions.Item label="Profile Completion">{detail.profileCompletion || 0}%</Descriptions.Item>
            <Descriptions.Item label="Last Active">{detail.lastActiveAt ? new Date(detail.lastActiveAt).toLocaleString('en-IN') : '—'}</Descriptions.Item>
            <Descriptions.Item label="Joined">{detail.createdAt ? new Date(detail.createdAt).toLocaleString('en-IN') : '—'}</Descriptions.Item>
            <Descriptions.Item label="User ID" span={2}>
              <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{detail.userId}</span>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
}
