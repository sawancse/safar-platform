import { useEffect, useState } from 'react';
import { Table, Tag, Typography, Spin, Avatar } from 'antd';
import { UserOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;

interface Host {
  id: string;
  name: string;
  phone?: string;
  email?: string;
  role?: string;
  subscriptionTier?: string;
  createdAt?: string;
}

export default function HostsPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [hosts, setHosts]     = useState<Host[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminApi.getHosts(token)
      .then(({ data }) => setHosts(data))
      .catch(() => setHosts([]))
      .finally(() => setLoading(false));
  }, [token]);

  const columns: ColumnsType<Host> = [
    {
      title: 'Host',
      render: (_, r) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#f97316' }} />
          <div>
            <div style={{ fontWeight: 600 }}>{r.name || '—'}</div>
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
      title: 'Role',
      dataIndex: 'role',
      width: 90,
      render: (r) => <Tag color={r === 'ADMIN' ? 'red' : 'orange'}>{r}</Tag>,
    },
    {
      title: 'Joined',
      dataIndex: 'createdAt',
      width: 110,
      render: (d) => new Date(d).toLocaleDateString('en-IN'),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>Hosts</Title>

      {loading ? (
        <Spin size="large" style={{ display: 'block', margin: '60px auto' }} />
      ) : (
        <Table
          columns={columns}
          dataSource={hosts}
          rowKey="id"
          scroll={{ x: 800 }}
          pagination={{ pageSize: 25, showSizeChanger: false }}
          locale={{ emptyText: 'No hosts found' }}
        />
      )}
    </div>
  );
}
