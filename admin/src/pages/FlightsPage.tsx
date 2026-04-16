import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Card, Row, Col, Statistic, Select, DatePicker, Input, Button,
  Modal, message, Space,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';

const { Title } = Typography;
const { RangePicker } = DatePicker;

const INR = (paise: number) => `\u20B9${(paise / 100).toLocaleString('en-IN')}`;

const BASE = (import.meta.env.VITE_API_URL || '') + '/api/v1';

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

const statusColor: Record<string, string> = {
  PENDING_PAYMENT: 'orange',
  CONFIRMED: 'blue',
  TICKETED: 'cyan',
  COMPLETED: 'green',
  CANCELLED: 'red',
  REFUNDED: 'purple',
};

const STATUSES = ['', 'PENDING_PAYMENT', 'CONFIRMED', 'TICKETED', 'COMPLETED', 'CANCELLED', 'REFUNDED'];

export default function FlightsPage() {
  const token = localStorage.getItem('admin_token') ?? '';

  const [bookings, setBookings] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<any>(null);

  // Filters
  const [status, setStatus] = useState('');
  const [originFilter, setOriginFilter] = useState('');
  const [destFilter, setDestFilter] = useState('');
  const [dateRange, setDateRange] = useState<[string, string] | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  // Cancel modal
  const [cancelModal, setCancelModal] = useState<{ open: boolean; booking: any }>({ open: false, booking: null });
  const [cancelReason, setCancelReason] = useState('');
  const [cancelLoading, setCancelLoading] = useState(false);

  const load = () => {
    setLoading(true);
    const params = new URLSearchParams();
    params.set('page', String(page));
    params.set('size', String(pageSize));
    params.set('sortBy', 'createdAt');
    params.set('sortDir', 'desc');
    if (status) params.set('status', status);
    if (originFilter) params.set('origin', originFilter.toUpperCase());
    if (destFilter) params.set('destination', destFilter.toUpperCase());
    if (dateRange) {
      params.set('dateFrom', dateRange[0]);
      params.set('dateTo', dateRange[1]);
    }

    fetch(`${BASE}/flights/admin/all?${params.toString()}`, { headers: authHeaders(token) })
      .then(res => res.json())
      .then(data => {
        setBookings(data.content || []);
        setTotal(data.totalElements || 0);
      })
      .catch(() => { setBookings([]); setTotal(0); })
      .finally(() => setLoading(false));
  };

  const loadStats = () => {
    fetch(`${BASE}/flights/admin/stats`, { headers: authHeaders(token) })
      .then(res => res.json())
      .then(setStats)
      .catch(() => setStats(null));
  };

  useEffect(() => { load(); }, [page, pageSize, status]);
  useEffect(() => { loadStats(); }, []);

  const handleSearch = () => { setPage(0); load(); };

  const handleCancel = async () => {
    const b = cancelModal.booking;
    if (!b || !cancelReason.trim()) {
      message.warning('Please provide a cancellation reason');
      return;
    }
    setCancelLoading(true);
    try {
      const res = await fetch(`${BASE}/flights/admin/${b.id}/cancel`, {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify({ reason: cancelReason.trim() }),
      });
      if (!res.ok) throw new Error('Cancel failed');
      message.success('Flight booking cancelled');
      setCancelModal({ open: false, booking: null });
      setCancelReason('');
      load();
      loadStats();
    } catch {
      message.error('Failed to cancel booking');
    } finally {
      setCancelLoading(false);
    }
  };

  const columns: ColumnsType<any> = [
    {
      title: 'Booking Ref', dataIndex: 'bookingRef', width: 130, ellipsis: true,
      render: (ref, r) => <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{ref || r.id?.substring(0, 10)}</span>,
    },
    {
      title: 'Route', width: 140,
      render: (_, r) => (
        <span>
          <strong>{r.origin}</strong> &rarr; <strong>{r.destination}</strong>
          {r.international && <Tag color="purple" style={{ marginLeft: 4, fontSize: 10 }}>INTL</Tag>}
        </span>
      ),
    },
    {
      title: 'Date', dataIndex: 'departureDate', width: 110,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '\u2014',
    },
    {
      title: 'Airline', width: 130,
      render: (_, r) => `${r.airline || ''} ${r.flightNumber || ''}`.trim() || '\u2014',
    },
    {
      title: 'Passengers', width: 90, align: 'center' as const,
      render: (_, r) => {
        if (r.passengerCount) return r.passengerCount;
        try {
          const pax = JSON.parse(r.passengersJson || '[]');
          return pax.length;
        } catch { return '\u2014'; }
      },
    },
    {
      title: 'Amount', dataIndex: 'totalAmountPaise', width: 110,
      render: (v) => v ? INR(v) : '\u2014',
    },
    {
      title: 'Status', dataIndex: 'status', width: 130,
      render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag>,
    },
    {
      title: 'Actions', width: 100, fixed: 'right' as const,
      render: (_, r) => (
        <Space>
          {(r.status === 'CONFIRMED' || r.status === 'TICKETED') && (
            <Button size="small" danger onClick={() => { setCancelModal({ open: true, booking: r }); setCancelReason(''); }}>
              Cancel
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>Flight Bookings</Title>

      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Total Bookings"
              value={stats?.totalBookings ?? 0}
              valueStyle={{ color: '#003B95' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Confirmed"
              value={stats?.confirmedCount ?? 0}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Revenue"
              value={stats?.totalRevenuePaise ? INR(stats.totalRevenuePaise) : '\u20B90'}
              valueStyle={{ color: '#003B95' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Cancellation Rate"
              value={stats?.cancellationRate ?? 0}
              suffix="%"
              precision={1}
              valueStyle={{ color: (stats?.cancellationRate ?? 0) > 10 ? '#ff4d4f' : '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            style={{ width: 180 }}
            placeholder="Filter by status"
            value={status || undefined}
            onChange={(v) => { setStatus(v || ''); setPage(0); }}
            allowClear
          >
            {STATUSES.filter(Boolean).map(s => (
              <Select.Option key={s} value={s}>{s}</Select.Option>
            ))}
          </Select>
          <Input
            placeholder="Origin (e.g. DEL)"
            style={{ width: 140 }}
            value={originFilter}
            onChange={(e) => setOriginFilter(e.target.value)}
            onPressEnter={handleSearch}
          />
          <Input
            placeholder="Destination (e.g. BOM)"
            style={{ width: 160 }}
            value={destFilter}
            onChange={(e) => setDestFilter(e.target.value)}
            onPressEnter={handleSearch}
          />
          <RangePicker
            onChange={(_, dateStrings) => {
              if (dateStrings[0] && dateStrings[1]) {
                setDateRange([dateStrings[0], dateStrings[1]]);
              } else {
                setDateRange(null);
              }
            }}
          />
          <Button type="primary" onClick={handleSearch}>Search</Button>
        </Space>
      </Card>

      {/* Table */}
      <Table
        columns={columns}
        dataSource={bookings}
        rowKey={(r) => r.id}
        loading={loading}
        scroll={{ x: 960 }}
        pagination={{
          current: page + 1,
          pageSize,
          total,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50'],
          onChange: (p, ps) => { setPage(p - 1); setPageSize(ps); },
          showTotal: (t) => `${t} bookings`,
        }}
      />

      {/* Cancel Modal */}
      <Modal
        title="Cancel Flight Booking"
        open={cancelModal.open}
        onCancel={() => { setCancelModal({ open: false, booking: null }); setCancelReason(''); }}
        onOk={handleCancel}
        confirmLoading={cancelLoading}
        okText="Cancel Booking"
        okButtonProps={{ danger: true }}
      >
        {cancelModal.booking && (
          <div style={{ marginBottom: 16 }}>
            <p>
              <strong>Booking:</strong> {cancelModal.booking.bookingRef || cancelModal.booking.id?.substring(0, 10)}
            </p>
            <p>
              <strong>Route:</strong> {cancelModal.booking.origin} &rarr; {cancelModal.booking.destination}
            </p>
            <p>
              <strong>Amount:</strong> {cancelModal.booking.totalAmountPaise ? INR(cancelModal.booking.totalAmountPaise) : '\u2014'}
            </p>
          </div>
        )}
        <Input.TextArea
          placeholder="Reason for cancellation"
          rows={3}
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
        />
      </Modal>
    </div>
  );
}
