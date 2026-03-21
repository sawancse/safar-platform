import { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Table, Tag, Descriptions } from 'antd';
import { DollarOutlined, BankOutlined, AuditOutlined, FundOutlined } from '@ant-design/icons';
import { adminApi } from '../lib/api';

interface Analytics {
  totalRevenue: number;
  totalListings: number;
  pendingListings: number;
  totalBookings: number;
  activeHosts: number;
  activeGuests: number;
}

function formatPaise(paise: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
  }).format(paise / 100);
}

export default function RevenuePage() {
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [settlements, setSettlements] = useState<any[]>([]);
  const [commissionSummary, setCommissionSummary] = useState<any>(null);
  const [commissionByHost, setCommissionByHost] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const token = localStorage.getItem('admin_token') || '';

  useEffect(() => {
    setLoading(true);
    Promise.all([
      adminApi.getRevenueSummary(token).catch(() => null),
      adminApi.getSettlements(token).catch(() => []),
      adminApi.getCommissionSummary(token).catch(() => null),
      adminApi.getCommissionByHost(token).catch(() => []),
    ]).then(([a, s, cs, cbh]) => {
      setAnalytics(a);
      setSettlements(Array.isArray(s) ? s : []);
      setCommissionSummary(cs);
      setCommissionByHost(Array.isArray(cbh) ? cbh : []);
    }).finally(() => setLoading(false));
  }, []);

  const settlementColumns = [
    { title: 'Booking ID', dataIndex: 'bookingId', key: 'bookingId',
      render: (id: string) => <span className="font-mono text-xs">{id?.substring(0, 8)}...</span> },
    { title: 'Total', dataIndex: 'totalAmountPaise', key: 'total',
      render: (v: number) => formatPaise(v || 0) },
    { title: 'Status', dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={s === 'COMPLETED' ? 'green' : s === 'PENDING' ? 'gold' : 'red'}>{s}</Tag> },
    { title: 'Created', dataIndex: 'createdAt', key: 'created',
      render: (d: string) => d ? new Date(d).toLocaleDateString('en-IN') : '-' },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Platform Revenue</h2>

      {/* Revenue Stats */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic title="Total Revenue" value={analytics?.totalRevenue || 0}
              prefix={<DollarOutlined />} formatter={(v) => formatPaise(Number(v))} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Total Bookings" value={analytics?.totalBookings || 0}
              prefix={<AuditOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Active Hosts" value={analytics?.activeHosts || 0}
              prefix={<BankOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="Active Guests" value={analytics?.activeGuests || 0}
              prefix={<FundOutlined />} />
          </Card>
        </Col>
      </Row>

      {/* Revenue breakdown */}
      <Card title="Revenue Model" style={{ marginBottom: 24 }}>
        <Descriptions bordered size="small" column={2}>
          <Descriptions.Item label="Commission (Starter hosts)">18% of booking</Descriptions.Item>
          <Descriptions.Item label="Commission (Pro hosts)">12% of booking</Descriptions.Item>
          <Descriptions.Item label="Commission (Commercial)">10% of booking</Descriptions.Item>
          <Descriptions.Item label="Commission (Medical)">8% of accommodation only</Descriptions.Item>
          <Descriptions.Item label="Commission (Aashray)">0%</Descriptions.Item>
          <Descriptions.Item label="Subscription Revenue">Starter free / Pro Rs 2,499 / Commercial Rs 3,999 per month</Descriptions.Item>
        </Descriptions>
      </Card>

      {/* Settlements Table */}
      <Card title="Recent Settlements" style={{ marginBottom: 24 }}>
        <Table
          dataSource={settlements}
          columns={settlementColumns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 20 }}
          locale={{ emptyText: 'No settlements yet — settlements are created when bookings are completed' }}
        />
      </Card>

      {/* Commission Breakdown */}
      <Card title="Commission Revenue" style={{ marginBottom: 24 }}>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Statistic title="Total Commission" value={commissionSummary?.totalCommissionPaise || 0}
              formatter={(v) => formatPaise(Number(v))} />
          </Col>
          <Col span={6}>
            <Statistic title="Avg Commission Rate" value={commissionSummary?.avgCommissionRate || 0}
              suffix="%" precision={1} />
          </Col>
          <Col span={6}>
            <Statistic title="Total Invoices" value={commissionSummary?.totalInvoices || 0} />
          </Col>
          <Col span={6}>
            <Statistic title="Total Payouts" value={commissionSummary?.totalPayouts || 0} />
          </Col>
        </Row>
      </Card>

      {/* Commission by Host */}
      <Card title="Commission by Host">
        <Table
          dataSource={commissionByHost}
          rowKey="hostId"
          pagination={{ pageSize: 10 }}
          columns={[
            { title: 'Host ID', dataIndex: 'hostId', key: 'host',
              render: (id: string) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{id?.substring(0, 8)}...</span> },
            { title: 'Bookings', dataIndex: 'bookings', key: 'bookings', sorter: (a: any, b: any) => a.bookings - b.bookings },
            { title: 'Gross Revenue', dataIndex: 'grossRevenuePaise', key: 'revenue',
              render: (v: number) => formatPaise(v), sorter: (a: any, b: any) => a.grossRevenuePaise - b.grossRevenuePaise },
            { title: 'GST Collected', dataIndex: 'gstCollectedPaise', key: 'gst', render: (v: number) => formatPaise(v) },
            { title: 'Total', dataIndex: 'totalWithGstPaise', key: 'total',
              render: (v: number) => <strong>{formatPaise(v)}</strong>, sorter: (a: any, b: any) => a.totalWithGstPaise - b.totalWithGstPaise },
          ]}
        />
      </Card>
    </div>
  );
}
