import { useState, useEffect } from 'react';
import { Table, Tag, Button, Space, Card, Statistic, Row, Col, Timeline, message } from 'antd';
import { SyncOutlined, LinkOutlined, DisconnectOutlined } from '@ant-design/icons';
import axios from 'axios';

const API_BASE = '/api/v1';

interface ChannelProperty {
  id: string;
  listingId: string;
  channexPropertyId: string;
  status: string;
  connectedChannels: string;
  lastSyncAt: string;
}

const statusColors: Record<string, string> = {
  CONNECTED: 'green',
  PENDING: 'orange',
  SYNCING: 'blue',
  ERROR: 'red',
  DISCONNECTED: 'default',
};

export default function ChannelManagerPage() {
  const [properties, setProperties] = useState<ChannelProperty[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncLogs, setSyncLogs] = useState<any[]>([]);
  const [selectedProperty, setSelectedProperty] = useState<string | null>(null);

  useEffect(() => { loadProperties(); }, []);

  async function loadProperties() {
    setLoading(true);
    try {
      // Admin endpoint to list all connected properties
      const token = localStorage.getItem('admin_token');
      const res = await axios.get(`${API_BASE}/channel-manager/status/all`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setProperties(res.data || []);
    } catch {
      // Fallback: show empty
      setProperties([]);
    }
    setLoading(false);
  }

  async function loadLogs(listingId: string) {
    const token = localStorage.getItem('admin_token');
    try {
      const res = await axios.get(`${API_BASE}/channel-manager/logs/${listingId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setSyncLogs(res.data?.content || []);
      setSelectedProperty(listingId);
    } catch { setSyncLogs([]); }
  }

  async function triggerSync(listingId: string, type: string) {
    const token = localStorage.getItem('admin_token');
    try {
      await axios.post(`${API_BASE}/channel-manager/sync/${type}/${listingId}`, {}, {
        headers: { Authorization: `Bearer ${token}` },
      });
      message.success(`${type} sync triggered`);
      loadProperties();
    } catch (e: any) {
      message.error(`Sync failed: ${e.response?.data?.message || e.message}`);
    }
  }

  const columns = [
    { title: 'Listing ID', dataIndex: 'listingId', key: 'listingId',
      render: (id: string) => <span className="font-mono text-xs">{id?.slice(0, 8)}...</span> },
    { title: 'Channex ID', dataIndex: 'channexPropertyId', key: 'channex',
      render: (id: string) => <span className="font-mono text-xs">{id?.slice(0, 12)}</span> },
    { title: 'Status', dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={statusColors[s]}>{s}</Tag> },
    { title: 'Last Sync', dataIndex: 'lastSyncAt', key: 'lastSync',
      render: (d: string) => d ? new Date(d).toLocaleString() : 'Never' },
    { title: 'Actions', key: 'actions',
      render: (_: any, record: ChannelProperty) => (
        <Space>
          <Button size="small" icon={<SyncOutlined />}
            onClick={() => triggerSync(record.listingId, 'rates')}>Rates</Button>
          <Button size="small" icon={<SyncOutlined />}
            onClick={() => triggerSync(record.listingId, 'availability')}>Avail</Button>
          <Button size="small" type="link"
            onClick={() => loadLogs(record.listingId)}>Logs</Button>
        </Space>
      ),
    },
  ];

  const connected = properties.filter(p => p.status === 'CONNECTED').length;
  const errors = properties.filter(p => p.status === 'ERROR').length;

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 24, fontWeight: 600, marginBottom: 24 }}>Channel Manager</h1>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card><Statistic title="Total Properties" value={properties.length} prefix={<LinkOutlined />} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Connected" value={connected} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Errors" value={errors} valueStyle={{ color: '#ff4d4f' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Disconnected" value={properties.filter(p => p.status === 'DISCONNECTED').length} /></Card>
        </Col>
      </Row>

      <Table
        columns={columns}
        dataSource={properties}
        loading={loading}
        rowKey="id"
        pagination={{ pageSize: 20 }}
      />

      {selectedProperty && syncLogs.length > 0 && (
        <Card title={`Sync Logs — ${selectedProperty.slice(0, 8)}...`} style={{ marginTop: 24 }}>
          <Timeline>
            {syncLogs.slice(0, 20).map((log: any, i: number) => (
              <Timeline.Item key={i} color={log.success ? 'green' : 'red'}>
                <strong>{log.direction}</strong> {log.syncType}
                {log.channelName && ` — ${log.channelName}`}
                <br />
                <span style={{ fontSize: 12, color: '#999' }}>
                  {new Date(log.syncedAt).toLocaleString()}
                  {log.recordsAffected > 0 && ` · ${log.recordsAffected} records`}
                </span>
                {log.errorMessage && <div style={{ color: '#ff4d4f', fontSize: 12 }}>{log.errorMessage}</div>}
              </Timeline.Item>
            ))}
          </Timeline>
        </Card>
      )}
    </div>
  );
}
