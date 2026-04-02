import { useEffect, useState } from 'react';
import { Table, Tag, Typography, Card, Row, Col, Statistic, Spin, Button, Space, message, Popconfirm, Select } from 'antd';
import { ExperimentOutlined, CheckCircleOutlined, PauseCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;
const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const statusColor: Record<string, string> = {
  DRAFT: 'default', ACTIVE: 'green', PAUSED: 'orange', REJECTED: 'red',
};

export default function ExperiencesPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [experiences, setExperiences] = useState<any[]>([]);
  const [stats, setStats] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string | undefined>(undefined);

  useEffect(() => {
    loadData();
  }, [token, filter]);

  function loadData() {
    setLoading(true);
    Promise.all([
      adminApi.getExperiences(token, filter).then((d: any) => d?.content || []),
      adminApi.getExperienceStats(token),
    ]).then(([exp, st]) => {
      setExperiences(Array.isArray(exp) ? exp : []);
      setStats(st ?? {});
    }).finally(() => setLoading(false));
  }

  async function handleStatusChange(id: string, newStatus: string) {
    try {
      await adminApi.updateExperienceStatus(id, newStatus, token);
      message.success(`Experience ${newStatus.toLowerCase()}`);
      loadData();
    } catch {
      message.error('Failed to update status');
    }
  }

  const columns: ColumnsType<any> = [
    {
      title: 'Title',
      dataIndex: 'title',
      ellipsis: true,
      width: 220,
    },
    {
      title: 'Host',
      dataIndex: 'hostName',
      width: 140,
    },
    {
      title: 'Category',
      dataIndex: 'category',
      width: 110,
      render: (cat: string) => <Tag color="blue">{cat}</Tag>,
    },
    {
      title: 'City',
      dataIndex: 'city',
      width: 120,
    },
    {
      title: 'Price',
      dataIndex: 'pricePaise',
      width: 100,
      render: (v: number) => INR(v),
    },
    {
      title: 'Duration',
      dataIndex: 'durationMinutes',
      width: 90,
      render: (v: number) => v < 60 ? `${v}m` : `${Math.floor(v/60)}h`,
    },
    {
      title: 'Rating',
      dataIndex: 'avgRating',
      width: 90,
      render: (v: number, r: any) => v ? `${v.toFixed(1)} (${r.reviewCount})` : '-',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 100,
      render: (s: string) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag>,
    },
    {
      title: 'Actions',
      width: 200,
      render: (_: any, record: any) => (
        <Space size="small">
          {record.status === 'DRAFT' && (
            <Popconfirm title="Approve this experience?" onConfirm={() => handleStatusChange(record.id, 'ACTIVE')}>
              <Button size="small" type="primary" icon={<CheckCircleOutlined />}>Approve</Button>
            </Popconfirm>
          )}
          {record.status === 'ACTIVE' && (
            <Popconfirm title="Pause this experience?" onConfirm={() => handleStatusChange(record.id, 'PAUSED')}>
              <Button size="small" icon={<PauseCircleOutlined />}>Pause</Button>
            </Popconfirm>
          )}
          {record.status === 'PAUSED' && (
            <Button size="small" type="primary" onClick={() => handleStatusChange(record.id, 'ACTIVE')}>
              Reactivate
            </Button>
          )}
          {(record.status === 'DRAFT' || record.status === 'ACTIVE') && (
            <Popconfirm title="Reject this experience?" onConfirm={() => handleStatusChange(record.id, 'REJECTED')}>
              <Button size="small" danger icon={<CloseCircleOutlined />}>Reject</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  if (loading) return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}><ExperimentOutlined /> Experiences Management</Title>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={4}><Card><Statistic title="Total" value={stats.TOTAL ?? 0} /></Card></Col>
        <Col span={4}><Card><Statistic title="Active" value={stats.ACTIVE ?? 0} valueStyle={{ color: '#52c41a' }} /></Card></Col>
        <Col span={4}><Card><Statistic title="Draft" value={stats.DRAFT ?? 0} /></Card></Col>
        <Col span={4}><Card><Statistic title="Paused" value={stats.PAUSED ?? 0} valueStyle={{ color: '#faad14' }} /></Card></Col>
        <Col span={4}><Card><Statistic title="Rejected" value={stats.REJECTED ?? 0} valueStyle={{ color: '#ff4d4f' }} /></Card></Col>
      </Row>

      <div style={{ marginBottom: 16 }}>
        <Select
          placeholder="Filter by status"
          allowClear
          style={{ width: 200 }}
          value={filter}
          onChange={(v) => setFilter(v)}
          options={[
            { value: 'DRAFT', label: 'Draft' },
            { value: 'ACTIVE', label: 'Active' },
            { value: 'PAUSED', label: 'Paused' },
            { value: 'REJECTED', label: 'Rejected' },
          ]}
        />
      </div>

      <Table
        dataSource={experiences}
        columns={columns}
        rowKey="id"
        pagination={{ pageSize: 20 }}
        size="small"
      />
    </div>
  );
}
