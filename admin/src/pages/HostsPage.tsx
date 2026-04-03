import { useEffect, useState } from 'react';
import { Table, Tag, Typography, Spin, Avatar, Input, Button, Space, Modal, message, Tabs, Tooltip } from 'antd';
import {
  UserOutlined, SearchOutlined, EyeOutlined, LoginOutlined,
  StopOutlined, CheckCircleOutlined, ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';
import { useNavigate } from 'react-router-dom';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface Host {
  id: string;
  name: string;
  phone?: string;
  email?: string;
  role?: string;
  subscriptionTier?: string;
  accountStatus?: string;
  suspensionReason?: string;
  suspendedAt?: string;
  createdAt?: string;
}

export default function HostsPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const navigate = useNavigate();
  const [hosts, setHosts]     = useState<Host[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch]   = useState('');
  const [activeTab, setActiveTab] = useState('active');
  const [actionLoading, setActionLoading] = useState(false);
  const [suspendModal, setSuspendModal] = useState<{ open: boolean; host: Host | null; action: 'suspend' | 'ban' }>({ open: false, host: null, action: 'suspend' });
  const [suspendReason, setSuspendReason] = useState('');

  function reload() {
    setLoading(true);
    adminApi.getHosts(token)
      .then(({ data }) => setHosts(data))
      .catch(() => setHosts([]))
      .finally(() => setLoading(false));
  }

  useEffect(() => { reload(); }, [token]);

  const activeHosts = hosts.filter(h => !h.accountStatus || h.accountStatus === 'ACTIVE');
  const suspendedHosts = hosts.filter(h => h.accountStatus === 'SUSPENDED' || h.accountStatus === 'BANNED');

  const displayHosts = activeTab === 'active' ? activeHosts : suspendedHosts;

  const filtered = displayHosts.filter(h => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (h.name?.toLowerCase().includes(q)) || (h.email?.toLowerCase().includes(q)) || (h.phone?.includes(q));
  });

  async function handleSuspend() {
    if (!suspendModal.host || !suspendReason) return;
    setActionLoading(true);
    try {
      const endpoint = suspendModal.action === 'ban' ? 'banHost' : 'suspendHost';
      await (adminApi as any)[endpoint](suspendModal.host.id, suspendReason, token);
      message.success(`Host ${suspendModal.action === 'ban' ? 'banned' : 'suspended'} successfully`);
      setSuspendModal({ open: false, host: null, action: 'suspend' });
      setSuspendReason('');
      reload();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Failed to suspend host');
    }
    setActionLoading(false);
  }

  async function handleUnsuspend(hostId: string) {
    setActionLoading(true);
    try {
      await (adminApi as any).unsuspendHost(hostId, token);
      message.success('Host reactivated');
      reload();
    } catch {
      message.error('Failed to reactivate host');
    }
    setActionLoading(false);
  }

  const columns: ColumnsType<Host> = [
    {
      title: 'Host',
      render: (_, r) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}
          onClick={() => navigate(`/hosts/${r.id}`)}>
          <Avatar icon={<UserOutlined />} style={{ backgroundColor: r.accountStatus === 'BANNED' ? '#ff4d4f' : r.accountStatus === 'SUSPENDED' ? '#faad14' : '#f97316' }} />
          <div>
            <div style={{ fontWeight: 600, color: '#1677ff' }}>{r.name || '—'}</div>
            <div style={{ fontSize: 12, color: '#6b7280' }}>{r.phone || '—'}</div>
          </div>
        </div>
      ),
      width: 220,
    },
    {
      title: 'Email',
      dataIndex: 'email',
      render: (e) => e ?? '—',
      width: 180,
      ellipsis: true,
    },
    {
      title: 'Tier',
      dataIndex: 'subscriptionTier',
      width: 100,
      render: (t) => {
        const colorMap: Record<string, string> = { FREE: 'default', BASIC: 'blue', PRO: 'purple', ENTERPRISE: 'gold' };
        return <Tag color={colorMap[t ?? 'FREE'] ?? 'default'}>{t ?? 'FREE'}</Tag>;
      },
    },
    {
      title: 'Status',
      width: 120,
      render: (_, r) => {
        const s = r.accountStatus || 'ACTIVE';
        if (s === 'BANNED') return <Tag color="red" icon={<StopOutlined />}>Banned</Tag>;
        if (s === 'SUSPENDED') return <Tooltip title={r.suspensionReason}><Tag color="orange" icon={<ExclamationCircleOutlined />}>Suspended</Tag></Tooltip>;
        return <Tag color="green" icon={<CheckCircleOutlined />}>Active</Tag>;
      },
    },
    {
      title: 'Joined',
      dataIndex: 'createdAt',
      width: 110,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
      sorter: (a, b) => new Date(a.createdAt ?? 0).getTime() - new Date(b.createdAt ?? 0).getTime(),
    },
    {
      title: 'Actions',
      width: 200,
      render: (_, r) => {
        const s = r.accountStatus || 'ACTIVE';
        return (
          <Space size={4}>
            <Button size="small" icon={<EyeOutlined />} onClick={() => navigate(`/hosts/${r.id}`)}>View</Button>
            <Button size="small" icon={<LoginOutlined />} onClick={async () => {
              try {
                const res = await adminApi.impersonateUser(r.id, token);
                const { accessToken, refreshToken, user } = res.data;
                const webUrl = window.location.origin.replace(/:\d+$/, ':3000');
                const params = new URLSearchParams({
                  impersonate: 'true', token: accessToken, refreshToken,
                  userId: user.id, name: user.name || '', role: user.role,
                });
                window.open(`${webUrl}/auth?${params.toString()}`, '_blank');
                message.success(`Logged in as ${user.name || r.name}`);
              } catch { message.error('Failed to login as host'); }
            }}>Login&nbsp;as</Button>
            {s === 'ACTIVE' && (
              <>
                <Button size="small" danger onClick={() => { setSuspendModal({ open: true, host: r, action: 'suspend' }); setSuspendReason(''); }}>
                  Suspend
                </Button>
                <Button size="small" danger type="primary" onClick={() => { setSuspendModal({ open: true, host: r, action: 'ban' }); setSuspendReason(''); }}>
                  Ban
                </Button>
              </>
            )}
            {(s === 'SUSPENDED') && (
              <Button size="small" type="primary" loading={actionLoading}
                onClick={() => handleUnsuspend(r.id)}>Reactivate</Button>
            )}
            {s === 'BANNED' && <Text type="secondary" style={{ fontSize: 12 }}>Permanently banned</Text>}
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>Hosts</Title>
        <Input
          prefix={<SearchOutlined />}
          placeholder="Search hosts..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ width: 260 }}
          allowClear
        />
      </div>

      <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
        { key: 'active', label: `Active (${activeHosts.length})` },
        { key: 'suspended', label: `Suspended / Banned (${suspendedHosts.length})` },
      ]} style={{ marginBottom: 8 }} />

      {loading ? (
        <Spin size="large" style={{ display: 'block', margin: '60px auto' }} />
      ) : (
        <Table
          columns={columns}
          dataSource={filtered}
          rowKey="id"
          scroll={{ x: 900 }}
          pagination={{ pageSize: 25, showSizeChanger: false, showTotal: (t) => `${t} hosts` }}
          locale={{ emptyText: activeTab === 'active' ? 'No active hosts' : 'No suspended or banned hosts' }}
        />
      )}

      {/* Suspend / Ban Modal */}
      <Modal
        title={suspendModal.action === 'ban' ? `Ban Host: ${suspendModal.host?.name}` : `Suspend Host: ${suspendModal.host?.name}`}
        open={suspendModal.open}
        onCancel={() => setSuspendModal({ open: false, host: null, action: 'suspend' })}
        onOk={handleSuspend}
        okText={suspendModal.action === 'ban' ? 'Ban Permanently' : 'Suspend Host'}
        okButtonProps={{ danger: true, disabled: !suspendReason, loading: actionLoading }}
        okType={suspendModal.action === 'ban' ? 'primary' : 'default'}
      >
        <div style={{ marginBottom: 12 }}>
          {suspendModal.action === 'ban' ? (
            <Text type="danger">This will permanently ban the host and suspend all their listings. This action is difficult to reverse.</Text>
          ) : (
            <Text>This will temporarily suspend the host and all their active listings. You can reactivate later.</Text>
          )}
        </div>
        <div style={{ marginBottom: 8 }}>
          <Text strong>Reason *</Text>
        </div>
        <TextArea
          rows={3}
          placeholder="Enter reason for suspension..."
          value={suspendReason}
          onChange={e => setSuspendReason(e.target.value)}
        />
      </Modal>
    </div>
  );
}
