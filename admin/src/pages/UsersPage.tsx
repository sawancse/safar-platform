import { useState, useEffect } from 'react';
import { Table, Tag, Card, Row, Col, Statistic, Select, Input, Button, DatePicker, Tabs, Modal, Descriptions, Switch, Progress, message, Space, Badge } from 'antd';
import { UserOutlined, TeamOutlined, SearchOutlined, MailOutlined, RiseOutlined, ThunderboltOutlined, FunnelPlotOutlined } from '@ant-design/icons';
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
  const [leadStats, setLeadStats] = useState<any>(null);
  const [campaigns, setCampaigns] = useState<any[]>([]);

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
    Promise.all([
      adminApi.getLeads(token, { sortBy: 'score' }).then((data: any) => data.content || []).catch(() => []),
      adminApi.getLeadStats(token),
      adminApi.getLeadCampaigns(token),
    ]).then(([l, s, c]) => {
      setLeads(l);
      setLeadStats(s);
      setCampaigns(Array.isArray(c) ? c : []);
    }).finally(() => setLeadsLoading(false));
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

  const SEGMENT_COLORS: Record<string, string> = {
    NEW: 'default', WARM: 'orange', HOT: 'red', COLD: 'blue', CONVERTED: 'green', HOST_PROSPECT: 'purple',
  };

  const leadColumns: ColumnsType<any> = [
    { title: 'Email', dataIndex: 'email', width: 200, ellipsis: true },
    { title: 'Name', dataIndex: 'name', width: 130, render: (v: string) => v || '—' },
    { title: 'City', dataIndex: 'city', width: 100, render: (v: string) => v || '—' },
    { title: 'Score', dataIndex: 'leadScore', width: 80, sorter: (a: any, b: any) => (a.leadScore || 0) - (b.leadScore || 0),
      render: (v: number) => <Progress percent={Math.min(v || 0, 100)} size="small" strokeColor={v >= 80 ? '#f5222d' : v >= 40 ? '#fa8c16' : '#1890ff'} format={() => v || 0} /> },
    { title: 'Segment', dataIndex: 'segment', width: 110,
      render: (v: string) => <Tag color={SEGMENT_COLORS[v] || 'default'}>{v}</Tag>,
      filters: Object.keys(SEGMENT_COLORS).map(k => ({ text: k, value: k })),
      onFilter: (val: any, r: any) => r.segment === val },
    { title: 'Source', dataIndex: 'source', width: 120, render: (v: string) => <Tag>{v?.replace(/_/g, ' ')}</Tag>,
      filters: ['WEBSITE_POPUP', 'EXIT_INTENT', 'PRICE_ALERT', 'LOCALITY_ALERT', 'HOST_CALCULATOR', 'REFERRAL', 'LANDING_PAGE'].map(k => ({ text: k.replace(/_/g, ' '), value: k })),
      onFilter: (val: any, r: any) => r.source === val },
    { title: 'Type', dataIndex: 'leadType', width: 80, render: (v: string) => v === 'HOST_PROSPECT' ? <Tag color="purple">Host</Tag> : <Tag>Guest</Tag> },
    { title: 'Converted', dataIndex: 'converted', width: 85,
      render: (v: boolean) => v ? <Tag color="green">Yes</Tag> : '—',
      filters: [{ text: 'Converted', value: true }, { text: 'Not converted', value: false }],
      onFilter: (val: any, r: any) => r.converted === val },
    { title: 'WhatsApp', dataIndex: 'whatsappOptin', width: 85, render: (v: boolean) => v ? <Tag color="green">Yes</Tag> : '—' },
    { title: 'Nurture', dataIndex: 'nurtureStage', width: 100, render: (v: string) => v && v !== 'NONE' ? <Tag color="cyan">{v}</Tag> : '—' },
    { title: 'Activity', width: 110, render: (_: any, r: any) => (
      <span style={{ fontSize: 11 }}>
        {r.pagesViewed > 0 && `${r.pagesViewed}pg `}
        {r.searchesPerformed > 0 && `${r.searchesPerformed}srch `}
        {r.listingsViewed > 0 && `${r.listingsViewed}list `}
        {r.wishlistCount > 0 && `${r.wishlistCount}wish`}
        {!r.pagesViewed && !r.searchesPerformed && !r.listingsViewed && !r.wishlistCount && '—'}
      </span>
    )},
    { title: 'Captured', dataIndex: 'createdAt', width: 100,
      render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
      sorter: (a: any, b: any) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime() },
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
            key: 'leads', label: <Badge count={leadStats?.totalLeads || 0} offset={[16, 0]} overflowCount={9999}>Leads</Badge>,
            children: (
              <>
                {/* Lead Stats Row */}
                {leadStats && (
                  <Row gutter={12} style={{ marginBottom: 16 }}>
                    <Col span={3}><Card size="small"><Statistic title="Total Leads" value={leadStats.totalLeads} /></Card></Col>
                    <Col span={3}><Card size="small"><Statistic title="This Week" value={leadStats.leadsThisWeek} valueStyle={{ color: '#1890ff' }} /></Card></Col>
                    <Col span={3}><Card size="small"><Statistic title="This Month" value={leadStats.leadsThisMonth} valueStyle={{ color: '#52c41a' }} /></Card></Col>
                    <Col span={3}><Card size="small"><Statistic title="Converted" value={leadStats.convertedLeads} valueStyle={{ color: '#f97316' }} /></Card></Col>
                    <Col span={3}><Card size="small"><Statistic title="Conv. Rate" value={leadStats.conversionRate} /></Card></Col>
                    <Col span={3}><Card size="small"><Statistic title="Price Alerts" value={leadStats.activePriceAlerts} prefix={<ThunderboltOutlined />} /></Card></Col>
                    <Col span={3}><Card size="small"><Statistic title="Area Alerts" value={leadStats.activeLocalityAlerts} /></Card></Col>
                  </Row>
                )}

                {/* Segment Funnel + Source Breakdown */}
                {leadStats?.bySegment && (
                  <Row gutter={12} style={{ marginBottom: 16 }}>
                    <Col span={12}>
                      <Card size="small" title={<><FunnelPlotOutlined /> Lead Funnel</>}>
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                          {Object.entries(leadStats.bySegment as Record<string, number>).map(([seg, cnt]) => (
                            <Tag key={seg} color={SEGMENT_COLORS[seg] || 'default'} style={{ fontSize: 13, padding: '4px 12px' }}>
                              {seg}: <strong>{cnt}</strong>
                            </Tag>
                          ))}
                        </div>
                      </Card>
                    </Col>
                    <Col span={12}>
                      <Card size="small" title="Source Attribution">
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                          {Object.entries(leadStats.bySource as Record<string, number>).map(([src, cnt]) => {
                            const conv = (leadStats.conversionBySource as Record<string, number>)?.[src] || 0;
                            return (
                              <Tag key={src} style={{ fontSize: 12, padding: '4px 10px' }}>
                                {src.replace(/_/g, ' ')}: <strong>{cnt}</strong> {conv > 0 && <span style={{ color: '#52c41a' }}>({conv} conv)</span>}
                              </Tag>
                            );
                          })}
                        </div>
                      </Card>
                    </Col>
                  </Row>
                )}

                {/* Campaigns */}
                {campaigns.length > 0 && (
                  <Card size="small" title="Nurture Campaigns" style={{ marginBottom: 16 }}>
                    <Table dataSource={campaigns} rowKey="id" size="small" pagination={false} columns={[
                      { title: 'Campaign', dataIndex: 'name', width: 200 },
                      { title: 'Type', dataIndex: 'campaignType', width: 140, render: (v: string) => <Tag>{v?.replace(/_/g, ' ')}</Tag> },
                      { title: 'Segment', dataIndex: 'targetSegment', width: 100, render: (v: string) => <Tag color={SEGMENT_COLORS[v]}>{v}</Tag> },
                      { title: 'Delay', dataIndex: 'delayHours', width: 70, render: (h: number) => h > 0 ? `${h}h` : 'Instant' },
                      { title: 'Sent', dataIndex: 'sentCount', width: 60 },
                      { title: 'Active', dataIndex: 'active', width: 80, render: (v: boolean, r: any) => (
                        <Switch checked={v} size="small" onChange={async () => {
                          try { await adminApi.toggleCampaign(r.id, token); loadLeads(); }
                          catch { message.error('Toggle failed'); }
                        }} />
                      )},
                    ]} />
                  </Card>
                )}

                <Button onClick={loadLeads} style={{ marginBottom: 12 }}>Refresh</Button>
                <Table columns={leadColumns} dataSource={leads} rowKey="id" loading={leadsLoading}
                  scroll={{ x: 1400 }} size="small"
                  pagination={{ pageSize: 25, showSizeChanger: true, showTotal: t => `${t} leads` }} />
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
