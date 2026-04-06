import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Card, Row, Col, Statistic, Select, DatePicker, Input, Button,
  Popconfirm, Modal, message, Descriptions, Spin, Tabs, Radio,
} from 'antd';
import {
  CalendarOutlined, CheckCircleOutlined, CloseCircleOutlined,
  DollarOutlined, LoginOutlined, LogoutOutlined, RollbackOutlined, SearchOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;
const { RangePicker } = DatePicker;

const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const statusColor: Record<string, string> = {
  DRAFT: 'default', PENDING_PAYMENT: 'orange', CONFIRMED: 'blue',
  CHECKED_IN: 'cyan', COMPLETED: 'green', CANCELLED: 'red', NO_SHOW: 'volcano',
};

const depositStatusColor: Record<string, string> = {
  PENDING: 'orange', COLLECTED: 'blue', REFUNDED: 'green', PARTIAL_REFUND: 'cyan',
};

const STATUSES = ['', 'DRAFT', 'PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];

export default function BookingsPage() {
  const token = localStorage.getItem('admin_token') ?? '';

  const [bookings, setBookings] = useState<any[]>([]);
  const [total, setTotal]       = useState(0);
  const [loading, setLoading]   = useState(true);
  const [stats, setStats]       = useState<any>(null);
  const [detail, setDetail]     = useState<any>(null);
  const [refundModal, setRefundModal] = useState<{ open: boolean; booking: any }>({ open: false, booking: null });
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReason, setRefundReason] = useState('');
  const [refundLoading, setRefundLoading] = useState(false);

  // Deposit refund modal
  const [depositModal, setDepositModal] = useState<{ open: boolean; booking: any }>({ open: false, booking: null });
  const [depositRefundType, setDepositRefundType] = useState<'FULL' | 'PARTIAL'>('FULL');
  const [depositDeduction, setDepositDeduction] = useState('');
  const [depositReason, setDepositReason] = useState('');
  const [depositLoading, setDepositLoading] = useState(false);

  // Pending deposits tab
  const [pendingDeposits, setPendingDeposits] = useState<any[]>([]);
  const [pendingTotal, setPendingTotal] = useState(0);
  const [pendingLoading, setPendingLoading] = useState(false);
  const [pendingPage, setPendingPage] = useState(0);

  // Filters
  const [status, setStatus]     = useState('');
  const [search, setSearch]     = useState('');
  const [dateRange, setDateRange] = useState<[string, string] | null>(null);
  const [page, setPage]         = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [activeTab, setActiveTab] = useState('bookings');

  const load = () => {
    setLoading(true);
    const params: any = { page, size: pageSize, sortBy: 'createdAt', sortDir: 'desc' };
    if (status) params.status = status;
    if (search) params.search = search;
    if (dateRange) { params.dateFrom = dateRange[0]; params.dateTo = dateRange[1]; }

    adminApi.getBookings(token, params)
      .then(({ data }) => {
        setBookings(data.content || []);
        setTotal(data.totalElements || 0);
      })
      .catch(() => { setBookings([]); setTotal(0); })
      .finally(() => setLoading(false));
  };

  const loadPendingDeposits = () => {
    setPendingLoading(true);
    adminApi.getPendingDeposits(pendingPage, 20, token)
      .then((data: any) => {
        setPendingDeposits(data.content || []);
        setPendingTotal(data.totalElements || 0);
      })
      .catch(() => { setPendingDeposits([]); setPendingTotal(0); })
      .finally(() => setPendingLoading(false));
  };

  useEffect(() => { load(); }, [page, pageSize, status]);
  useEffect(() => { adminApi.getBookingStats(token).then(setStats); }, []);
  useEffect(() => { if (activeTab === 'deposits') loadPendingDeposits(); }, [activeTab, pendingPage]);

  const handleSearch = () => { setPage(0); load(); };

  const openDepositRefund = (booking: any) => {
    setDepositModal({ open: true, booking });
    setDepositRefundType('FULL');
    setDepositDeduction('');
    setDepositReason('');
  };

  const handleDepositRefund = async () => {
    const b = depositModal.booking;
    if (!b) return;
    if (depositRefundType === 'PARTIAL' && !depositDeduction) {
      message.warning('Enter deduction amount'); return;
    }
    setDepositLoading(true);
    try {
      await adminApi.adminDepositRefund(
        b.id,
        depositRefundType,
        depositRefundType === 'PARTIAL' ? Math.round(Number(depositDeduction) * 100) : null,
        depositReason.trim(),
        token
      );
      message.success('Deposit refund processed');
      setDepositModal({ open: false, booking: null });
      load();
      if (activeTab === 'deposits') loadPendingDeposits();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Deposit refund failed');
    } finally { setDepositLoading(false); }
  };

  const columns: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'bookingRef', width: 120, ellipsis: true,
      render: (ref, r) => (
        <a onClick={() => setDetail(r)} style={{ fontFamily: 'monospace' }}>{ref || r.id?.substring(0, 8)}</a>
      ),
    },
    { title: 'Guest', width: 150,
      render: (_, r) => `${r.guestFirstName || ''} ${r.guestLastName || ''}`.trim() || '—' },
    { title: 'Listing', dataIndex: 'listingTitle', width: 180, ellipsis: true },
    { title: 'Check-in', dataIndex: 'checkIn', width: 105,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—',
      sorter: true,
    },
    { title: 'Check-out', dataIndex: 'checkOut', width: 105,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    { title: 'Amount', dataIndex: 'totalAmountPaise', width: 100,
      render: (v) => v ? INR(v) : '—' },
    { title: 'Status', dataIndex: 'status', width: 120,
      render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag> },
    { title: 'Created', dataIndex: 'createdAt', width: 105,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    {
      title: 'Action', width: 220, fixed: 'right',
      render: (_, r) => (
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {(r.status === 'CONFIRMED' || r.status === 'CHECKED_IN') && (
            <Popconfirm title="Cancel this booking as admin?" onConfirm={async () => {
              try {
                await adminApi.adminCancelBooking(r.id, 'Cancelled by admin', token);
                message.success('Booking cancelled');
                load();
              } catch { message.error('Cancel failed'); }
            }}>
              <Button size="small" danger>Cancel</Button>
            </Popconfirm>
          )}
          {r.status === 'CANCELLED' && r.totalAmountPaise > 0 && (
            <Button size="small" icon={<RollbackOutlined />}
              onClick={() => { setRefundModal({ open: true, booking: r }); setRefundAmount(String(r.totalAmountPaise / 100)); setRefundReason(''); }}>
              Refund
            </Button>
          )}
          {r.status === 'COMPLETED' && (
            <Popconfirm title="Process settlement for this booking?" onConfirm={async () => {
              try {
                await adminApi.processSettlementByBooking(r.id, token);
                message.success('Settlement processed');
              } catch (e: any) { message.error(e?.response?.data?.detail || 'Settlement failed'); }
            }}>
              <Button size="small" icon={<DollarOutlined />}>Settle</Button>
            </Popconfirm>
          )}
          {r.securityDepositPaise > 0 && r.securityDepositStatus !== 'REFUNDED' &&
           (r.status === 'CANCELLED' || r.status === 'COMPLETED') && (
            <Button size="small" icon={<SafetyOutlined />} type="primary" ghost
              onClick={() => openDepositRefund(r)}>
              Deposit
            </Button>
          )}
        </div>
      ),
    },
  ];

  const depositColumns: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'bookingRef', width: 120, ellipsis: true,
      render: (ref, r) => (
        <a onClick={() => setDetail(r)} style={{ fontFamily: 'monospace' }}>{ref || r.id?.substring(0, 8)}</a>
      ),
    },
    { title: 'Guest', width: 150,
      render: (_, r) => `${r.guestFirstName || ''} ${r.guestLastName || ''}`.trim() || '—' },
    { title: 'Listing', dataIndex: 'listingTitle', width: 180, ellipsis: true },
    { title: 'Booking Status', dataIndex: 'status', width: 120,
      render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag> },
    { title: 'Deposit', dataIndex: 'securityDepositPaise', width: 110,
      render: (v) => v ? INR(v) : '—' },
    { title: 'Deposit Status', dataIndex: 'securityDepositStatus', width: 130,
      render: (s) => s ? <Tag color={depositStatusColor[s] ?? 'default'}>{s}</Tag> : '—' },
    { title: 'Check-out', dataIndex: 'checkOut', width: 105,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    {
      title: 'Action', width: 160, fixed: 'right',
      render: (_, r) => (
        <div style={{ display: 'flex', gap: 4 }}>
          {r.securityDepositStatus !== 'REFUNDED' && (r.status === 'CANCELLED' || r.status === 'COMPLETED') ? (
            <Button size="small" type="primary" icon={<RollbackOutlined />}
              onClick={() => openDepositRefund(r)}>
              Refund Deposit
            </Button>
          ) : r.securityDepositStatus !== 'REFUNDED' ? (
            <Tag color="orange">Booking in progress</Tag>
          ) : (
            <Tag color="green">Already refunded</Tag>
          )}
        </div>
      ),
    },
  ];

  const byStatus = stats?.byStatus || {};

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>Bookings</Title>

      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={4}><Card size="small"><Statistic title="Total" value={stats?.total || 0} prefix={<CalendarOutlined />} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Confirmed" value={byStatus.CONFIRMED || 0} valueStyle={{ color: '#1677ff' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Checked-In" value={byStatus.CHECKED_IN || 0} prefix={<LoginOutlined />} valueStyle={{ color: '#13c2c2' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Completed" value={byStatus.COMPLETED || 0} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Cancelled" value={byStatus.CANCELLED || 0} prefix={<CloseCircleOutlined />} valueStyle={{ color: '#ff4d4f' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Revenue" value={stats?.totalRevenuePaise ? INR(stats.totalRevenuePaise) : '₹0'} /></Card></Col>
      </Row>

      <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
        {
          key: 'bookings',
          label: 'All Bookings',
          children: (
            <>
              {/* Filters */}
              <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
                <Select placeholder="Status" value={status || undefined} onChange={v => { setStatus(v || ''); setPage(0); }}
                  allowClear style={{ width: 160 }}>
                  {STATUSES.filter(Boolean).map(s => <Select.Option key={s} value={s}>{s}</Select.Option>)}
                </Select>
                <RangePicker onChange={(_, ds) => setDateRange(ds[0] ? [ds[0], ds[1]] : null)} />
                <Input prefix={<SearchOutlined />} placeholder="Search ref, guest, listing..."
                  value={search} onChange={e => setSearch(e.target.value)}
                  onPressEnter={handleSearch} style={{ width: 260 }} allowClear />
                <Button type="primary" onClick={handleSearch}>Search</Button>
              </div>

              {/* Table */}
              <Table
                columns={columns}
                dataSource={bookings}
                rowKey="id"
                loading={loading}
                scroll={{ x: 1200 }}
                pagination={{
                  current: page + 1, pageSize, total,
                  onChange: (p, ps) => { setPage(p - 1); setPageSize(ps); },
                  showSizeChanger: true, showTotal: (t) => `${t} bookings`,
                }}
                locale={{ emptyText: 'No bookings found' }}
              />
            </>
          ),
        },
        {
          key: 'deposits',
          label: (
            <span><SafetyOutlined /> Pending Deposits</span>
          ),
          children: (
            <>
              <p style={{ marginBottom: 16, color: '#666' }}>
                Bookings with security deposits pending refund. Refund deposits for cancelled or completed bookings.
              </p>
              <Table
                columns={depositColumns}
                dataSource={pendingDeposits}
                rowKey="id"
                loading={pendingLoading}
                scroll={{ x: 1100 }}
                pagination={{
                  current: pendingPage + 1, pageSize: 20, total: pendingTotal,
                  onChange: (p) => setPendingPage(p - 1),
                  showTotal: (t) => `${t} pending deposits`,
                }}
                locale={{ emptyText: 'No pending deposits' }}
              />
            </>
          ),
        },
      ]} />

      {/* Detail modal */}
      <Modal open={!!detail} onCancel={() => setDetail(null)} width={640}
        title={`Booking ${detail?.bookingRef || ''}`}
        footer={detail ? (
          <div style={{ display: 'flex', gap: 8 }}>
            {detail.status === 'CANCELLED' && detail.totalAmountPaise > 0 && (
              <Button icon={<RollbackOutlined />} onClick={() => {
                setRefundModal({ open: true, booking: detail });
                setRefundAmount(String(detail.totalAmountPaise / 100));
                setRefundReason('');
              }}>Initiate Refund</Button>
            )}
            {detail.securityDepositPaise > 0 && detail.securityDepositStatus !== 'REFUNDED' &&
             (detail.status === 'CANCELLED' || detail.status === 'COMPLETED') && (
              <Button icon={<SafetyOutlined />} type="primary" ghost
                onClick={() => openDepositRefund(detail)}>Refund Deposit</Button>
            )}
            {detail.status === 'COMPLETED' && (
              <Popconfirm title="Process settlement for this booking?" onConfirm={async () => {
                try {
                  await adminApi.processSettlementByBooking(detail.id, token);
                  message.success('Settlement processed');
                } catch (e: any) { message.error(e?.response?.data?.detail || 'Settlement failed'); }
              }}>
                <Button icon={<DollarOutlined />}>Process Settlement</Button>
              </Popconfirm>
            )}
            <Button onClick={() => setDetail(null)}>Close</Button>
          </div>
        ) : null}>
        {detail && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="Status">
              <Tag color={statusColor[detail.status]}>{detail.status}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Booking Ref">{detail.bookingRef}</Descriptions.Item>
            <Descriptions.Item label="Guest">{detail.guestFirstName} {detail.guestLastName}</Descriptions.Item>
            <Descriptions.Item label="Email">{detail.guestEmail || '—'}</Descriptions.Item>
            <Descriptions.Item label="Phone">{detail.guestPhone || '—'}</Descriptions.Item>
            <Descriptions.Item label="Listing">{detail.listingTitle || '—'}</Descriptions.Item>
            <Descriptions.Item label="Check-in">{detail.checkIn ? new Date(detail.checkIn).toLocaleDateString('en-IN') : '—'}</Descriptions.Item>
            <Descriptions.Item label="Check-out">{detail.checkOut ? new Date(detail.checkOut).toLocaleDateString('en-IN') : '—'}</Descriptions.Item>
            <Descriptions.Item label="Guests">{detail.numberOfGuests || '—'}</Descriptions.Item>
            <Descriptions.Item label="Rooms">{detail.numberOfRooms || 1}</Descriptions.Item>
            <Descriptions.Item label="Base Amount">{detail.basePricePaise ? INR(detail.basePricePaise) : '—'}</Descriptions.Item>
            <Descriptions.Item label="Total Amount">{detail.totalAmountPaise ? INR(detail.totalAmountPaise) : '—'}</Descriptions.Item>
            <Descriptions.Item label="Payment Mode">{detail.paymentMode || '—'}</Descriptions.Item>
            <Descriptions.Item label="Payment ID">{detail.razorpayPaymentId || '—'}</Descriptions.Item>
            {detail.securityDepositPaise > 0 && (
              <>
                <Descriptions.Item label="Security Deposit">{INR(detail.securityDepositPaise)}</Descriptions.Item>
                <Descriptions.Item label="Deposit Status">
                  <Tag color={depositStatusColor[detail.securityDepositStatus] ?? 'default'}>
                    {detail.securityDepositStatus || 'N/A'}
                  </Tag>
                </Descriptions.Item>
              </>
            )}
            <Descriptions.Item label="Has Review">{detail.hasReview ? <Tag color="green">Yes</Tag> : 'No'}</Descriptions.Item>
            <Descriptions.Item label="Created">{detail.createdAt ? new Date(detail.createdAt).toLocaleString('en-IN') : '—'}</Descriptions.Item>
            {detail.cancellationReason && (
              <Descriptions.Item label="Cancel Reason" span={2}>
                <span style={{ color: '#ff4d4f' }}>{detail.cancellationReason}</span>
              </Descriptions.Item>
            )}
            {detail.specialRequests && (
              <Descriptions.Item label="Special Requests" span={2}>{detail.specialRequests}</Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Modal>

      {/* Refund modal */}
      <Modal open={refundModal.open} title="Initiate Refund" onCancel={() => setRefundModal({ open: false, booking: null })}
        confirmLoading={refundLoading}
        onOk={async () => {
          const b = refundModal.booking;
          if (!b || !refundAmount || !refundReason.trim()) { message.warning('Fill all fields'); return; }
          setRefundLoading(true);
          try {
            await adminApi.initiateRefund({
              paymentId: b.razorpayPaymentId || b.id,
              bookingId: b.id,
              amountPaise: Math.round(Number(refundAmount) * 100),
              reason: refundReason.trim(),
              refundType: 'FULL',
            }, token);
            message.success('Refund initiated');
            setRefundModal({ open: false, booking: null });
            load();
          } catch (e: any) {
            message.error(e?.response?.data?.detail || 'Refund failed');
          } finally { setRefundLoading(false); }
        }}>
        {refundModal.booking && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <p>Booking: <strong>{refundModal.booking.bookingRef}</strong></p>
            <p>Total Paid: <strong>{INR(refundModal.booking.totalAmountPaise)}</strong></p>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Refund Amount (INR)</label>
              <Input type="number" value={refundAmount} onChange={e => setRefundAmount(e.target.value)}
                prefix="₹" placeholder="Amount in rupees" />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Reason</label>
              <Input.TextArea rows={2} value={refundReason} onChange={e => setRefundReason(e.target.value)}
                placeholder="Reason for refund" />
            </div>
          </div>
        )}
      </Modal>

      {/* Deposit refund modal */}
      <Modal open={depositModal.open} title="Refund Security Deposit"
        onCancel={() => setDepositModal({ open: false, booking: null })}
        confirmLoading={depositLoading}
        onOk={handleDepositRefund}>
        {depositModal.booking && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="Booking">{depositModal.booking.bookingRef}</Descriptions.Item>
              <Descriptions.Item label="Guest">
                {depositModal.booking.guestFirstName} {depositModal.booking.guestLastName}
              </Descriptions.Item>
              <Descriptions.Item label="Deposit Amount">
                <strong>{INR(depositModal.booking.securityDepositPaise)}</strong>
              </Descriptions.Item>
              <Descriptions.Item label="Current Status">
                <Tag color={depositStatusColor[depositModal.booking.securityDepositStatus] ?? 'default'}>
                  {depositModal.booking.securityDepositStatus}
                </Tag>
              </Descriptions.Item>
            </Descriptions>

            <div>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>Refund Type</label>
              <Radio.Group value={depositRefundType} onChange={e => setDepositRefundType(e.target.value)}>
                <Radio.Button value="FULL">Full Refund</Radio.Button>
                <Radio.Button value="PARTIAL">Partial (with deductions)</Radio.Button>
              </Radio.Group>
            </div>

            {depositRefundType === 'PARTIAL' && (
              <div>
                <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Deduction Amount (INR)</label>
                <Input type="number" value={depositDeduction} onChange={e => setDepositDeduction(e.target.value)}
                  prefix="₹" placeholder="Amount to deduct from deposit" />
                {depositDeduction && Number(depositDeduction) > 0 && (
                  <p style={{ marginTop: 4, color: '#52c41a', fontSize: 12 }}>
                    Refund to guest: {INR((depositModal.booking.securityDepositPaise) - Math.round(Number(depositDeduction) * 100))}
                  </p>
                )}
              </div>
            )}

            {depositRefundType === 'FULL' && (
              <p style={{ color: '#52c41a' }}>
                Full deposit of <strong>{INR(depositModal.booking.securityDepositPaise)}</strong> will be refunded to the guest.
              </p>
            )}

            <div>
              <label style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>Reason (optional)</label>
              <Input.TextArea rows={2} value={depositReason} onChange={e => setDepositReason(e.target.value)}
                placeholder="e.g. Property damage deduction, cleaning charges, etc." />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
