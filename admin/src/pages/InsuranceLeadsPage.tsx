import { useEffect, useMemo, useState } from 'react';
import { Table, Tag, Typography, Select, message, Row, Col, Card, Statistic, Empty, Space, Button } from 'antd';
import { WhatsAppOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Paragraph, Text } = Typography;

type Lead = {
  id: string;
  name: string;
  phone: string;
  email?: string;
  product?: string;
  coverageType?: string;
  city?: string;
  preferredTime?: string;
  notes?: string;
  status: string;
  source: string;
  createdAt: string;
};

const STATUSES = ['NEW', 'CONTACTED', 'CONVERTED', 'LOST'];
const STATUS_COLOR: Record<string, string> = { NEW: 'blue', CONTACTED: 'orange', CONVERTED: 'green', LOST: 'default' };

export default function InsuranceLeadsPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string | undefined>(undefined);

  async function load() {
    setLoading(true);
    try {
      const page = await adminApi.listInsuranceLeads(token, filter);
      setLeads(page?.content || []);
    } catch (err: any) {
      message.error(err?.response?.data?.detail || 'Failed to load leads');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [filter]);

  const stats = useMemo(() => ({
    total: leads.length,
    isNew: leads.filter(l => l.status === 'NEW').length,
    contacted: leads.filter(l => l.status === 'CONTACTED').length,
    converted: leads.filter(l => l.status === 'CONVERTED').length,
  }), [leads]);

  async function setStatus(id: string, status: string) {
    try {
      await adminApi.updateInsuranceLeadStatus(id, status, token);
      message.success(`Marked ${status.toLowerCase()}`);
      load();
    } catch (err: any) {
      message.error(err?.response?.data?.detail || 'Update failed');
    }
  }

  const columns: ColumnsType<Lead> = useMemo(() => [
    {
      title: 'Customer', key: 'cust',
      render: (_, l) => (
        <div>
          <div style={{ fontWeight: 500 }}>{l.name}</div>
          <Text type="secondary" style={{ fontSize: 11 }}>{l.phone}{l.email ? ` · ${l.email}` : ''}</Text>
        </div>
      ),
    },
    {
      title: 'Product', key: 'product',
      render: (_, l) => <Tag>{l.product || l.coverageType || '—'}</Tag>,
    },
    { title: 'City', dataIndex: 'city', render: (c?: string) => c || <Text type="secondary">—</Text> },
    { title: 'Preferred time', dataIndex: 'preferredTime', render: (t?: string) => t || <Text type="secondary">—</Text> },
    {
      title: 'Status', key: 'status',
      render: (_, l) => <Tag color={STATUS_COLOR[l.status] || 'default'}>{l.status}</Tag>,
    },
    {
      title: 'Received', dataIndex: 'createdAt',
      render: (iso: string) => new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }),
      sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
      defaultSortOrder: 'descend',
    },
    {
      title: 'Actions', key: 'actions', width: 280,
      render: (_, l) => {
        const phoneNoPlus = l.phone.replace(/\D/g, '');
        const msg = `Hi ${l.name}, this is BhramanKaro Insurance regarding your ${l.product || 'insurance'} enquiry. Is now a good time to talk?`;
        return (
          <Space size={4} wrap>
            <Button size="small" type="primary" ghost icon={<WhatsAppOutlined />}
              href={`https://wa.me/${phoneNoPlus}?text=${encodeURIComponent(msg)}`} target="_blank" rel="noreferrer" />
            <Select size="small" value={l.status} style={{ width: 130 }}
              onChange={(v) => setStatus(l.id, v)}
              options={STATUSES.map(s => ({ value: s, label: s }))} />
          </Space>
        );
      },
    },
  ], []);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ marginBottom: 4 }}>Insurance Leads</Title>
          <Paragraph type="secondary">
            Talk-to-advisor callback requests from the insurance marketplace. WhatsApp the customer, then update status.
          </Paragraph>
        </div>
        <Space>
          <Select allowClear placeholder="All statuses" style={{ width: 160 }} value={filter}
            onChange={(v) => setFilter(v)} options={STATUSES.map(s => ({ value: s, label: s }))} />
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}><Card><Statistic title="Total" value={stats.total} /></Card></Col>
        <Col span={6}><Card><Statistic title="New" value={stats.isNew} valueStyle={{ color: '#1677ff' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="Contacted" value={stats.contacted} valueStyle={{ color: '#fa8c16' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="Converted" value={stats.converted} valueStyle={{ color: '#52c41a' }} /></Card></Col>
      </Row>

      <Table
        rowKey="id"
        loading={loading}
        dataSource={leads}
        columns={columns}
        size="middle"
        locale={{ emptyText: <Empty description="No leads yet" /> }}
      />
    </div>
  );
}
