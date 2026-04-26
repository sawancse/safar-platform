import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Card, Row, Col, Statistic, Select, DatePicker, Input, Button,
  Modal, message, Space, Drawer, Descriptions, InputNumber, List,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

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

const providerColor: Record<string, string> = {
  AMADEUS: 'geekblue',
  DUFFEL: 'magenta',
  TRIPJACK: 'volcano',
  TBO: 'gold',
  TRAVCLAN: 'lime',
};

const STATUSES = ['PENDING_PAYMENT', 'CONFIRMED', 'TICKETED', 'COMPLETED', 'CANCELLED', 'REFUNDED'];

interface TopRoute {
  origin: string;
  destination: string;
  count: number;
}

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

  // Refund modal
  const [refundModal, setRefundModal] = useState<{ open: boolean; booking: any }>({ open: false, booking: null });
  const [refundAmount, setRefundAmount] = useState<number | null>(null);
  const [refundLoading, setRefundLoading] = useState(false);

  // Detail drawer
  const [drawerBooking, setDrawerBooking] = useState<any | null>(null);

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
      const res = await fetch(`${BASE}/flights/admin/${b.id}/cancel?reason=${encodeURIComponent(cancelReason.trim())}`, {
        method: 'POST',
        headers: authHeaders(token),
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

  const handleRefund = async () => {
    const b = refundModal.booking;
    if (!b) return;
    setRefundLoading(true);
    try {
      const url = refundAmount != null && refundAmount > 0
        ? `${BASE}/flights/admin/${b.id}/refund?amountPaise=${Math.round(refundAmount * 100)}`
        : `${BASE}/flights/admin/${b.id}/refund`;
      const res = await fetch(url, { method: 'POST', headers: authHeaders(token) });
      if (!res.ok) throw new Error('Refund failed');
      message.success('Refund initiated');
      setRefundModal({ open: false, booking: null });
      setRefundAmount(null);
      load();
      loadStats();
    } catch {
      message.error('Failed to initiate refund');
    } finally {
      setRefundLoading(false);
    }
  };

  const parsePassengers = (json: string | null | undefined): any[] => {
    if (!json) return [];
    try { return JSON.parse(json); } catch { return []; }
  };

  const columns: ColumnsType<any> = [
    {
      title: 'Booking Ref', dataIndex: 'bookingRef', width: 130, ellipsis: true,
      render: (ref, r) => (
        <a onClick={() => setDrawerBooking(r)} style={{ fontFamily: 'monospace', fontWeight: 600 }}>
          {ref || r.id?.substring(0, 10)}
        </a>
      ),
    },
    {
      title: 'Route', width: 160,
      render: (_, r) => (
        <span>
          <strong>{r.departureCityCode || r.origin}</strong> &rarr; <strong>{r.arrivalCityCode || r.destination}</strong>
          {r.isInternational && <Tag color="purple" style={{ marginLeft: 4, fontSize: 10 }}>INTL</Tag>}
        </span>
      ),
    },
    {
      title: 'Date', dataIndex: 'departureDate', width: 110,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '—',
    },
    {
      title: 'Airline', width: 130,
      render: (_, r) => `${r.airline || ''} ${r.flightNumber || ''}`.trim() || '—',
    },
    {
      title: 'Provider', dataIndex: 'provider', width: 100,
      render: (p) => p ? <Tag color={providerColor[p] ?? 'default'}>{p}</Tag> : <Tag>—</Tag>,
    },
    {
      title: 'Pax', width: 60, align: 'center' as const,
      render: (_, r) => parsePassengers(r.passengersJson).length || '—',
    },
    {
      title: 'Amount', dataIndex: 'totalAmountPaise', width: 110,
      render: (v) => v ? INR(v) : '—',
    },
    {
      title: 'Status', dataIndex: 'status', width: 130,
      render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag>,
    },
    {
      title: 'Actions', width: 160, fixed: 'right' as const,
      render: (_, r) => (
        <Space>
          {(r.status === 'CONFIRMED' || r.status === 'TICKETED') && (
            <Button size="small" danger onClick={(e) => { e.stopPropagation(); setCancelModal({ open: true, booking: r }); setCancelReason(''); }}>
              Cancel
            </Button>
          )}
          {r.paymentStatus === 'PAID' && r.status !== 'REFUNDED' && (
            <Button size="small" onClick={(e) => { e.stopPropagation(); setRefundModal({ open: true, booking: r }); setRefundAmount(null); }}>
              Refund
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
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={6}>
          <Card><Statistic title="Total Bookings" value={stats?.totalBookings ?? 0} valueStyle={{ color: '#003B95' }} /></Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card><Statistic title="Confirmed" value={stats?.confirmed ?? stats?.confirmedCount ?? 0} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card><Statistic title="Revenue" value={stats?.totalRevenuePaise ? INR(stats.totalRevenuePaise) : '₹0'} valueStyle={{ color: '#003B95' }} /></Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card>
            <Statistic
              title="Cancelled"
              value={stats?.cancelled ?? 0}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Top routes */}
      {stats?.topRoutes && stats.topRoutes.length > 0 && (
        <Card title="Top Routes" size="small" style={{ marginBottom: 16 }}>
          <List
            size="small"
            dataSource={stats.topRoutes as TopRoute[]}
            renderItem={(r) => (
              <List.Item>
                <Text strong>{r.origin} &rarr; {r.destination}</Text>
                <Tag color="blue">{r.count} bookings</Tag>
              </List.Item>
            )}
          />
        </Card>
      )}

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
            {STATUSES.map(s => (
              <Select.Option key={s} value={s}>{s}</Select.Option>
            ))}
          </Select>
          <Input placeholder="Origin (e.g. DEL)" style={{ width: 140 }} value={originFilter} onChange={(e) => setOriginFilter(e.target.value)} onPressEnter={handleSearch} />
          <Input placeholder="Destination (e.g. BOM)" style={{ width: 160 }} value={destFilter} onChange={(e) => setDestFilter(e.target.value)} onPressEnter={handleSearch} />
          <RangePicker
            onChange={(_, dateStrings) => {
              if (dateStrings[0] && dateStrings[1]) setDateRange([dateStrings[0], dateStrings[1]]);
              else setDateRange(null);
            }}
          />
          <Button type="primary" onClick={handleSearch}>Search</Button>
        </Space>
      </Card>

      <Table
        columns={columns}
        dataSource={bookings}
        rowKey={(r) => r.id}
        loading={loading}
        scroll={{ x: 1100 }}
        onRow={(r) => ({ onClick: () => setDrawerBooking(r) })}
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
            <p><strong>Booking:</strong> {cancelModal.booking.bookingRef || cancelModal.booking.id?.substring(0, 10)}</p>
            <p><strong>Route:</strong> {cancelModal.booking.departureCityCode} &rarr; {cancelModal.booking.arrivalCityCode}</p>
            <p><strong>Amount:</strong> {cancelModal.booking.totalAmountPaise ? INR(cancelModal.booking.totalAmountPaise) : '—'}</p>
            <p><strong>Provider:</strong> {cancelModal.booking.provider || '—'}</p>
            <Text type="secondary" style={{ fontSize: 12 }}>
              The cancellation will be sent to the provider; refund follows the airline's fare-rule policy.
            </Text>
          </div>
        )}
        <Input.TextArea placeholder="Reason for cancellation" rows={3} value={cancelReason} onChange={(e) => setCancelReason(e.target.value)} />
      </Modal>

      {/* Refund Modal */}
      <Modal
        title="Issue Refund"
        open={refundModal.open}
        onCancel={() => { setRefundModal({ open: false, booking: null }); setRefundAmount(null); }}
        onOk={handleRefund}
        confirmLoading={refundLoading}
        okText="Initiate Refund"
      >
        {refundModal.booking && (
          <div style={{ marginBottom: 16 }}>
            <p><strong>Booking:</strong> {refundModal.booking.bookingRef}</p>
            <p><strong>Total Paid:</strong> {refundModal.booking.totalAmountPaise ? INR(refundModal.booking.totalAmountPaise) : '—'}</p>
            <p><strong>Refund Amount (INR):</strong></p>
            <InputNumber
              style={{ width: '100%' }}
              placeholder={`Leave blank for full refund (${refundModal.booking.totalAmountPaise ? INR(refundModal.booking.totalAmountPaise) : ''})`}
              min={0}
              max={refundModal.booking.totalAmountPaise ? refundModal.booking.totalAmountPaise / 100 : undefined}
              value={refundAmount}
              onChange={setRefundAmount}
              addonBefore="₹"
            />
            <Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 8 }}>
              Leave amount blank to refund the full paid value.
            </Text>
          </div>
        )}
      </Modal>

      {/* Detail Drawer */}
      <Drawer
        title={drawerBooking ? `Booking ${drawerBooking.bookingRef}` : 'Booking detail'}
        placement="right"
        width={560}
        onClose={() => setDrawerBooking(null)}
        open={!!drawerBooking}
      >
        {drawerBooking && (
          <>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="Booking ref">
                <Text code>{drawerBooking.bookingRef}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={statusColor[drawerBooking.status] ?? 'default'}>{drawerBooking.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Provider">
                <Tag color={providerColor[drawerBooking.provider] ?? 'default'}>{drawerBooking.provider || '—'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="External order id">
                <Text code style={{ fontSize: 11 }}>{drawerBooking.duffelOrderId || '—'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Route">
                <strong>{drawerBooking.departureCityCode}</strong> &rarr; <strong>{drawerBooking.arrivalCityCode}</strong>
                {drawerBooking.isInternational && <Tag color="purple" style={{ marginLeft: 6 }}>INTL</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="Trip type">{drawerBooking.tripType}</Descriptions.Item>
              <Descriptions.Item label="Cabin">{drawerBooking.cabinClass}</Descriptions.Item>
              <Descriptions.Item label="Departure">{drawerBooking.departureDate}</Descriptions.Item>
              {drawerBooking.returnDate && <Descriptions.Item label="Return">{drawerBooking.returnDate}</Descriptions.Item>}
              <Descriptions.Item label="Airline">{drawerBooking.airline} {drawerBooking.flightNumber}</Descriptions.Item>
              <Descriptions.Item label="Total">{drawerBooking.totalAmountPaise ? INR(drawerBooking.totalAmountPaise) : '—'}</Descriptions.Item>
              <Descriptions.Item label="Tax">{drawerBooking.taxPaise ? INR(drawerBooking.taxPaise) : '—'}</Descriptions.Item>
              <Descriptions.Item label="Platform fee">{drawerBooking.platformFeePaise ? INR(drawerBooking.platformFeePaise) : '—'}</Descriptions.Item>
              <Descriptions.Item label="Payment status">{drawerBooking.paymentStatus}</Descriptions.Item>
              <Descriptions.Item label="Razorpay order">
                <Text code style={{ fontSize: 11 }}>{drawerBooking.razorpayOrderId || '—'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Razorpay payment">
                <Text code style={{ fontSize: 11 }}>{drawerBooking.razorpayPaymentId || '—'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Contact">
                {drawerBooking.contactEmail}<br/>
                {drawerBooking.contactPhone}
              </Descriptions.Item>
              {drawerBooking.cancellationReason && (
                <Descriptions.Item label="Cancellation reason">{drawerBooking.cancellationReason}</Descriptions.Item>
              )}
              {drawerBooking.refundAmountPaise && (
                <Descriptions.Item label="Refund">{INR(drawerBooking.refundAmountPaise)}</Descriptions.Item>
              )}
              <Descriptions.Item label="Created">{drawerBooking.createdAt ? new Date(drawerBooking.createdAt).toLocaleString('en-IN') : '—'}</Descriptions.Item>
            </Descriptions>

            {/* Passengers */}
            {(() => {
              const pax = parsePassengers(drawerBooking.passengersJson);
              if (pax.length === 0) return null;
              return (
                <>
                  <Title level={5} style={{ marginTop: 24 }}>Passengers ({pax.length})</Title>
                  <List
                    size="small"
                    bordered
                    dataSource={pax}
                    renderItem={(p: any) => (
                      <List.Item>
                        <span><strong>{p.firstName} {p.lastName}</strong></span>
                        <span style={{ color: '#888', fontSize: 12 }}>
                          {p.gender} · DOB {p.dateOfBirth}
                          {p.passportNumber && ` · ${p.passportNumber}`}
                        </span>
                      </List.Item>
                    )}
                  />
                </>
              );
            })()}
          </>
        )}
      </Drawer>
    </div>
  );
}
