import { useEffect, useState } from 'react';
import { Table, Tag, Typography, Tabs, Card, Row, Col, Statistic, Spin, Input, Avatar, Button, Space, message, Popconfirm, Modal, Select, Descriptions, Badge, Tooltip } from 'antd';
import { FireOutlined, CalendarOutlined, TeamOutlined, SearchOutlined, UserOutlined, CheckCircleOutlined, CloseCircleOutlined, StopOutlined, PhoneOutlined, MailOutlined, EnvironmentOutlined, PrinterOutlined, CopyOutlined, DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;
const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const bookingStatusColor: Record<string, string> = {
  PENDING: 'orange', PENDING_PAYMENT: 'gold', CONFIRMED: 'blue', IN_PROGRESS: 'cyan',
  COMPLETED: 'green', CANCELLED: 'red', NO_SHOW: 'volcano',
};
const eventStatusColor: Record<string, string> = {
  INQUIRY: 'default', QUOTED: 'orange', CONFIRMED: 'blue',
  ADVANCE_PAID: 'cyan', IN_PROGRESS: 'purple', COMPLETED: 'green', CANCELLED: 'red',
};
const subStatusColor: Record<string, string> = {
  ACTIVE: 'green', PAUSED: 'orange', CANCELLED: 'red', EXPIRED: 'default',
};
const verifyColor: Record<string, string> = {
  PENDING: 'orange', VERIFIED: 'green', REJECTED: 'red', SUSPENDED: 'volcano',
};

function copyText(text: string) {
  navigator.clipboard.writeText(text);
  message.success('Copied!');
}

function parseMenuDesc(raw?: string): { tags: string[]; parsed: any } {
  if (!raw) return { tags: [], parsed: null };
  try {
    const p = JSON.parse(raw);
    const tags: string[] = [];
    if (p.vegNonVeg) tags.push(p.vegNonVeg === 'VEG' ? 'Veg' : p.vegNonVeg === 'NON_VEG' ? 'Non-Veg' : 'Veg + Non-Veg');
    if (p.decoration) tags.push('Decoration');
    if (p.cake) tags.push('Cake');
    if (p.crockery) tags.push('Crockery');
    if (p.appliances) tags.push('Appliances');
    if (p.tableSetup) tags.push('Table Setup');
    if (p.liveCounters?.length) tags.push(`${p.liveCounters.length} counter(s)`);
    if (p.extraStaff) tags.push(`${p.staffCount || 2} staff`);
    if (p.selectedDishIds?.length) tags.push(`${p.selectedDishIds.length} dishes`);
    return { tags, parsed: p };
  } catch { return { tags: [raw], parsed: null }; }
}

// ── Lookup map: chefId → chef profile (for phone/email) ──
function buildChefMap(chefs: any[]): Record<string, any> {
  const map: Record<string, any> = {};
  chefs.forEach(c => { map[c.id] = c; });
  return map;
}

export default function CooksPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [chefs, setChefs]         = useState<any[]>([]);
  const [bookings, setBookings]   = useState<any[]>([]);
  const [events, setEvents]       = useState<any[]>([]);
  const [subs, setSubs]           = useState<any[]>([]);
  const [loading, setLoading]     = useState(true);
  const [search, setSearch]       = useState('');
  const [assignModal, setAssignModal] = useState<{ visible: boolean; type: 'booking' | 'event'; id: string } | null>(null);
  const [selectedChef, setSelectedChef] = useState<string | null>(null);

  const loadData = () => {
    setLoading(true);
    Promise.all([
      adminApi.getChefs(token).then((d: any) => d?.content || []),
      adminApi.getChefBookings(token).then((d: any) => d?.content || d || []),
      adminApi.getChefEvents(token).then((d: any) => d?.content || d || []),
      adminApi.getChefSubscriptions(token).then((d: any) => d?.content || d || []),
      adminApi.getDishCatalog(token),
    ]).then(([c, b, e, s, d]) => {
      setChefs(Array.isArray(c) ? c : []);
      setBookings(Array.isArray(b) ? b : []);
      setEvents(Array.isArray(e) ? e : []);
      setSubs(Array.isArray(s) ? s : []);
      setDishes(Array.isArray(d) ? d : []);
    }).finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, [token]);

  const reload = () => {
    adminApi.getChefs(token).then((d: any) => setChefs(d?.content || []));
  };

  const handleVerify = async (chefId: string) => {
    try { await adminApi.verifyChef(chefId, token); message.success('Chef verified'); reload(); }
    catch { message.error('Failed to verify'); }
  };
  const handleReject = async (chefId: string) => {
    try { await adminApi.rejectChef(chefId, 'Rejected by admin', token); message.success('Chef rejected'); reload(); }
    catch { message.error('Failed to reject'); }
  };
  const handleSuspend = async (chefId: string) => {
    try { await adminApi.suspendChef(chefId, token); message.success('Chef suspended'); reload(); }
    catch { message.error('Failed to suspend'); }
  };
  const handleAssign = async () => {
    if (!assignModal || !selectedChef) return;
    try {
      if (assignModal.type === 'booking') {
        await adminApi.assignChefToBooking(assignModal.id, selectedChef, token);
      } else {
        await adminApi.assignChefToEvent(assignModal.id, selectedChef, token);
      }
      message.success('Chef assigned successfully');
      setAssignModal(null); setSelectedChef(null); loadData();
    } catch { message.error('Failed to assign chef'); }
  };

  // ── Cancel / Complete handlers ──
  const [cancelModal, setCancelModal] = useState<{ visible: boolean; type: 'booking' | 'event'; id: string; ref: string } | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [dishes, setDishes] = useState<any[]>([]);
  const [dishModal, setDishModal] = useState<{ visible: boolean; editing: any | null }>({ visible: false, editing: null });
  const [dishForm, setDishForm] = useState<any>({});

  const handleCancelBooking = async () => {
    if (!cancelModal || !cancelReason.trim()) { message.warning('Please enter a reason'); return; }
    try {
      await adminApi.adminCancelChefBooking(cancelModal.id, cancelReason, token);
      message.success('Booking cancelled + refund initiated');
      setCancelModal(null); setCancelReason(''); loadData();
    } catch { message.error('Failed to cancel'); }
  };

  const handleCancelEvent = async () => {
    if (!cancelModal || !cancelReason.trim()) { message.warning('Please enter a reason'); return; }
    try {
      await adminApi.adminCancelChefEvent(cancelModal.id, cancelReason, token);
      message.success('Event cancelled');
      setCancelModal(null); setCancelReason(''); loadData();
    } catch { message.error('Failed to cancel'); }
  };

  const handleCompleteBooking = async (id: string) => {
    try { await adminApi.adminCompleteChefBooking(id, token); message.success('Booking marked complete'); loadData(); }
    catch { message.error('Failed to complete'); }
  };

  const handleCompleteEvent = async (id: string) => {
    try { await adminApi.adminCompleteChefEvent(id, token); message.success('Event marked complete'); loadData(); }
    catch { message.error('Failed to complete'); }
  };

  // ── Dish catalog handlers ──
  const loadDishes = () => { adminApi.getDishCatalog(token).then((d: any) => setDishes(Array.isArray(d) ? d : [])); };

  const handleSaveDish = async () => {
    try {
      if (dishModal.editing) {
        await adminApi.updateDish(dishModal.editing.id, dishForm, token);
        message.success('Dish updated');
      } else {
        await adminApi.createDish(dishForm, token);
        message.success('Dish created');
      }
      setDishModal({ visible: false, editing: null }); setDishForm({}); loadDishes();
    } catch { message.error('Failed to save dish'); }
  };

  const handleDeleteDish = async (id: string) => {
    try { await adminApi.deleteDish(id, token); message.success('Dish deactivated'); loadDishes(); }
    catch { message.error('Failed to delete'); }
  };

  const chefMap = buildChefMap(chefs);
  const verifiedChefs = chefs.filter(c => c.verificationStatus === 'VERIFIED' && c.available);
  const filteredChefs = chefs.filter(c => {
    if (!search) return true;
    const q = search.toLowerCase();
    return c.name?.toLowerCase().includes(q) || c.city?.toLowerCase().includes(q) || c.cuisines?.toLowerCase().includes(q) || c.phone?.includes(q);
  });

  // Stats
  const activeBookings = bookings.filter(b => !['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(b.status)).length;
  const totalRevenue = bookings.filter(b => b.status !== 'CANCELLED').reduce((s, b) => s + (b.totalAmountPaise || 0), 0)
    + events.filter(e => e.status !== 'CANCELLED').reduce((s, e) => s + (e.totalAmountPaise || 0), 0);

  // ── Chef columns ──
  const chefCols: ColumnsType<any> = [
    {
      title: 'Chef', width: 220,
      render: (_, r) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Avatar icon={<UserOutlined />} src={r.profilePhotoUrl} style={{ backgroundColor: '#f97316' }} />
          <div>
            <div style={{ fontWeight: 600 }}>{r.name || '—'}</div>
            <div style={{ fontSize: 11, color: '#6b7280' }}>
              {r.phone && <><PhoneOutlined style={{ marginRight: 3 }} />{r.phone}</>}
            </div>
            {r.email && <div style={{ fontSize: 11, color: '#6b7280' }}><MailOutlined style={{ marginRight: 3 }} />{r.email}</div>}
          </div>
        </div>
      ),
    },
    { title: 'City', dataIndex: 'city', width: 100 },
    { title: 'Type', dataIndex: 'chefType', width: 100, render: (t: string) => <Tag>{t}</Tag> },
    { title: 'Cuisines', dataIndex: 'cuisines', width: 180, ellipsis: true },
    { title: 'Exp', dataIndex: 'experienceYears', width: 60, render: (y: number) => `${y || 0}yr` },
    { title: 'Daily Rate', dataIndex: 'dailyRatePaise', width: 100, render: (v: number) => v ? INR(v) : '—' },
    { title: 'Rating', dataIndex: 'rating', width: 80, render: (r: number) => r ? `${r.toFixed(1)} ★` : '—' },
    { title: 'Bookings', dataIndex: 'totalBookings', width: 80 },
    { title: 'Status', dataIndex: 'verificationStatus', width: 100,
      render: (s: string) => <Tag color={verifyColor[s] ?? 'default'}>{s}</Tag> },
    { title: 'Available', dataIndex: 'available', width: 80,
      render: (a: boolean) => <Tag color={a ? 'green' : 'red'}>{a ? 'Yes' : 'No'}</Tag> },
    { title: 'Actions', width: 180, fixed: 'right' as const,
      render: (_: any, r: any) => (
        <Space size="small">
          {r.verificationStatus === 'PENDING' && (
            <>
              <Button type="primary" size="small" icon={<CheckCircleOutlined />} onClick={() => handleVerify(r.id)}>Verify</Button>
              <Popconfirm title="Reject this chef?" onConfirm={() => handleReject(r.id)}>
                <Button danger size="small" icon={<CloseCircleOutlined />}>Reject</Button>
              </Popconfirm>
            </>
          )}
          {r.verificationStatus === 'VERIFIED' && (
            <Popconfirm title="Suspend this chef?" onConfirm={() => handleSuspend(r.id)}>
              <Button size="small" icon={<StopOutlined />}>Suspend</Button>
            </Popconfirm>
          )}
          {(r.verificationStatus === 'REJECTED' || r.verificationStatus === 'SUSPENDED') && (
            <Button type="primary" size="small" icon={<CheckCircleOutlined />} onClick={() => handleVerify(r.id)}>
              {r.verificationStatus === 'REJECTED' ? 'Verify' : 'Restore'}
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // ── Booking columns ──
  const bookingCols: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'bookingRef', width: 120, render: (v: string) => <Text copyable={{ text: v }} style={{ fontSize: 12, fontFamily: 'monospace' }}>{v}</Text> },
    {
      title: 'Cook', width: 160,
      render: (_, r) => {
        const chef = chefMap[r.chefId];
        return r.chefName ? (
          <div>
            <div style={{ fontWeight: 600, fontSize: 13 }}>{r.chefName}</div>
            {chef?.phone && <div style={{ fontSize: 11, color: '#6b7280' }}><PhoneOutlined style={{ marginRight: 3 }} /><a href={`tel:${chef.phone}`}>{chef.phone}</a></div>}
          </div>
        ) : <Tag color="red">Unassigned</Tag>;
      },
    },
    {
      title: 'Customer', width: 160,
      render: (_, r) => (
        <div>
          <div style={{ fontWeight: 600, fontSize: 13 }}>{r.customerName || '—'}</div>
          {r.customerPhone && <div style={{ fontSize: 11, color: '#6b7280' }}><PhoneOutlined style={{ marginRight: 3 }} /><a href={`tel:${r.customerPhone}`}>{r.customerPhone}</a></div>}
        </div>
      ),
    },
    { title: 'Date', dataIndex: 'serviceDate', width: 100, sorter: (a: any, b: any) => (a.serviceDate || '').localeCompare(b.serviceDate || '') },
    { title: 'Time', dataIndex: 'serviceTime', width: 70 },
    { title: 'Meal', dataIndex: 'mealType', width: 80, render: (v: string) => <Tag>{v}</Tag> },
    { title: 'Guests', dataIndex: 'guestsCount', width: 65 },
    { title: 'Total', dataIndex: 'totalAmountPaise', width: 90, render: (v: number) => v ? <Text strong>{INR(v)}</Text> : '—', sorter: (a: any, b: any) => (a.totalAmountPaise || 0) - (b.totalAmountPaise || 0) },
    { title: 'Payment', dataIndex: 'paymentStatus', width: 110,
      render: (s: string) => <Tag color={s === 'ADVANCE_PAID' ? 'green' : s === 'FULLY_PAID' ? 'blue' : 'red'}>{s || 'UNPAID'}</Tag> },
    { title: 'Status', dataIndex: 'status', width: 120,
      render: (s: string) => <Tag color={bookingStatusColor[s] ?? 'default'}>{s}</Tag>,
      filters: Object.keys(bookingStatusColor).map(k => ({ text: k, value: k })),
      onFilter: (val: any, r: any) => r.status === val,
    },
    { title: 'Actions', width: 220, fixed: 'right' as const,
      render: (_: any, r: any) => (
        <Space size="small" wrap>
          <Button size="small" type="primary"
            onClick={() => { setAssignModal({ visible: true, type: 'booking', id: r.id }); setSelectedChef(r.chefId || null); }}>
            {r.chefName ? 'Reassign' : 'Assign'}
          </Button>
          {!['CANCELLED', 'COMPLETED'].includes(r.status) && (
            <>
              <Popconfirm title="Mark as completed?" onConfirm={() => handleCompleteBooking(r.id)}>
                <Button size="small" style={{ color: '#52c41a', borderColor: '#52c41a' }}>Complete</Button>
              </Popconfirm>
              <Button size="small" danger
                onClick={() => setCancelModal({ visible: true, type: 'booking', id: r.id, ref: r.bookingRef })}>
                Cancel
              </Button>
            </>
          )}
          <Tooltip title="Print"><Button size="small" icon={<PrinterOutlined />} onClick={() => printBooking(r, chefMap[r.chefId])} /></Tooltip>
        </Space>
      ),
    },
  ];

  // ── Event columns ──
  const eventCols: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'bookingRef', width: 120, render: (v: string) => <Text copyable={{ text: v }} style={{ fontSize: 12, fontFamily: 'monospace' }}>{v}</Text> },
    {
      title: 'Cook', width: 160,
      render: (_, r) => {
        const chef = chefMap[r.chefId];
        return r.chefName ? (
          <div>
            <div style={{ fontWeight: 600, fontSize: 13 }}>{r.chefName}</div>
            {chef?.phone && <div style={{ fontSize: 11, color: '#6b7280' }}><PhoneOutlined style={{ marginRight: 3 }} /><a href={`tel:${chef.phone}`}>{chef.phone}</a></div>}
          </div>
        ) : <Tag color="red">Unassigned</Tag>;
      },
    },
    {
      title: 'Customer', width: 180,
      render: (_, r) => (
        <div>
          <div style={{ fontWeight: 600, fontSize: 13 }}>{r.customerName || '—'}</div>
          {r.customerPhone && <div style={{ fontSize: 11, color: '#6b7280' }}><PhoneOutlined style={{ marginRight: 3 }} /><a href={`tel:${r.customerPhone}`}>{r.customerPhone}</a></div>}
          {r.customerEmail && <div style={{ fontSize: 11, color: '#6b7280' }}><MailOutlined style={{ marginRight: 3 }} />{r.customerEmail}</div>}
        </div>
      ),
    },
    { title: 'Event', dataIndex: 'eventType', width: 110, render: (t: string) => <Tag color="purple">{t}</Tag> },
    { title: 'Date', dataIndex: 'eventDate', width: 100, sorter: (a: any, b: any) => (a.eventDate || '').localeCompare(b.eventDate || '') },
    { title: 'Guests', dataIndex: 'guestCount', width: 65 },
    { title: 'Total', dataIndex: 'totalAmountPaise', width: 100, render: (v: number) => v ? <Text strong>{INR(v)}</Text> : '—' },
    { title: 'Advance', dataIndex: 'advanceAmountPaise', width: 90, render: (v: number) => v ? INR(v) : '—' },
    { title: 'Status', dataIndex: 'status', width: 120,
      render: (s: string) => <Tag color={eventStatusColor[s] ?? 'default'}>{s}</Tag>,
      filters: Object.keys(eventStatusColor).map(k => ({ text: k, value: k })),
      onFilter: (val: any, r: any) => r.status === val,
    },
    { title: 'Actions', width: 220, fixed: 'right' as const,
      render: (_: any, r: any) => (
        <Space size="small" wrap>
          <Button size="small" type="primary"
            onClick={() => { setAssignModal({ visible: true, type: 'event', id: r.id }); setSelectedChef(r.chefId || null); }}>
            {r.chefName ? 'Reassign' : 'Assign'}
          </Button>
          {!['CANCELLED', 'COMPLETED'].includes(r.status) && (
            <>
              <Popconfirm title="Mark as completed?" onConfirm={() => handleCompleteEvent(r.id)}>
                <Button size="small" style={{ color: '#52c41a', borderColor: '#52c41a' }}>Complete</Button>
              </Popconfirm>
              <Button size="small" danger
                onClick={() => setCancelModal({ visible: true, type: 'event', id: r.id, ref: r.bookingRef })}>
                Cancel
              </Button>
            </>
          )}
          <Tooltip title="Print"><Button size="small" icon={<PrinterOutlined />} onClick={() => printEvent(r, chefMap[r.chefId])} /></Tooltip>
        </Space>
      ),
    },
  ];

  // ── Subscription columns ──
  const subCols: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'subscriptionRef', width: 120, render: (v: string) => <Text copyable={{ text: v }} style={{ fontSize: 12, fontFamily: 'monospace' }}>{v}</Text> },
    { title: 'Chef', dataIndex: 'chefName', width: 140, ellipsis: true },
    { title: 'Customer', dataIndex: 'customerName', width: 140, ellipsis: true },
    { title: 'Plan', dataIndex: 'plan', width: 100, render: (p: string) => <Tag>{p}</Tag> },
    { title: 'Monthly', dataIndex: 'monthlyRatePaise', width: 100, render: (v: number) => v ? INR(v) : '—' },
    { title: 'Start', dataIndex: 'startDate', width: 100 },
    { title: 'End', dataIndex: 'endDate', width: 100 },
    { title: 'Status', dataIndex: 'status', width: 100,
      render: (s: string) => <Tag color={subStatusColor[s] ?? 'default'}>{s}</Tag> },
  ];

  // ── Expandable row for bookings ──
  const bookingExpandable = {
    expandedRowRender: (r: any) => {
      const chef = chefMap[r.chefId];
      return (
        <Descriptions size="small" column={3} bordered style={{ background: '#fafafa' }}>
          <Descriptions.Item label="Cook Name">{r.chefName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Cook Phone">{chef?.phone ? <a href={`tel:${chef.phone}`}>{chef.phone}</a> : '—'}</Descriptions.Item>
          <Descriptions.Item label="Cook City">{chef?.city || '—'}</Descriptions.Item>
          <Descriptions.Item label="Customer Name">{r.customerName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Customer Phone">{r.customerPhone ? <a href={`tel:${r.customerPhone}`}>{r.customerPhone}</a> : '—'}</Descriptions.Item>
          <Descriptions.Item label="Customer ID"><Text copyable style={{ fontSize: 11 }}>{r.customerId || '—'}</Text></Descriptions.Item>
          <Descriptions.Item label={<><EnvironmentOutlined /> Address</>} span={3}>
            {[r.address, r.locality, r.city, r.pincode].filter(Boolean).join(', ') || '—'}
            {r.address && <Button type="link" size="small" icon={<CopyOutlined />} onClick={() => copyText([r.address, r.locality, r.city, r.pincode].filter(Boolean).join(', '))} />}
          </Descriptions.Item>
          <Descriptions.Item label="Menu">{r.menuName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Special Requests" span={2}>{r.specialRequests || '—'}</Descriptions.Item>
          <Descriptions.Item label="Total">{r.totalAmountPaise ? INR(r.totalAmountPaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Advance">{r.advanceAmountPaise ? INR(r.advanceAmountPaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Balance">{r.balanceAmountPaise ? INR(r.balanceAmountPaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Platform Fee">{r.platformFeePaise ? INR(r.platformFeePaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Chef Earnings">{r.chefEarningsPaise ? INR(r.chefEarningsPaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Payment Status"><Tag color={r.paymentStatus === 'ADVANCE_PAID' ? 'green' : 'red'}>{r.paymentStatus || 'UNPAID'}</Tag></Descriptions.Item>
          {r.razorpayPaymentId && <Descriptions.Item label="Razorpay ID" span={3}><Text copyable style={{ fontSize: 11 }}>{r.razorpayPaymentId}</Text></Descriptions.Item>}
          {r.cancellationReason && <Descriptions.Item label="Cancel Reason" span={3}><Text type="danger">{r.cancellationReason}</Text></Descriptions.Item>}
          {r.ratingGiven && <Descriptions.Item label="Rating">{r.ratingGiven} ★ {r.reviewComment && `— "${r.reviewComment}"`}</Descriptions.Item>}
          <Descriptions.Item label="Created">{r.createdAt ? new Date(r.createdAt).toLocaleString() : '—'}</Descriptions.Item>
        </Descriptions>
      );
    },
  };

  // ── Expandable row for events ──
  const eventExpandable = {
    expandedRowRender: (r: any) => {
      const chef = chefMap[r.chefId];
      const { tags } = parseMenuDesc(r.menuDescription);
      return (
        <Descriptions size="small" column={3} bordered style={{ background: '#fafafa' }}>
          <Descriptions.Item label="Cook Name">{r.chefName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Cook Phone">{chef?.phone ? <a href={`tel:${chef.phone}`}>{chef.phone}</a> : '—'}</Descriptions.Item>
          <Descriptions.Item label="Cook Email">{chef?.email || '—'}</Descriptions.Item>
          <Descriptions.Item label="Customer Name">{r.customerName || '—'}</Descriptions.Item>
          <Descriptions.Item label="Customer Phone">{r.customerPhone ? <a href={`tel:${r.customerPhone}`}>{r.customerPhone}</a> : '—'}</Descriptions.Item>
          <Descriptions.Item label="Customer Email">{r.customerEmail || '—'}</Descriptions.Item>
          <Descriptions.Item label={<><EnvironmentOutlined /> Venue</>} span={3}>
            {[r.venueAddress, r.locality, r.city, r.pincode].filter(Boolean).join(', ') || '—'}
            {r.venueAddress && <Button type="link" size="small" icon={<CopyOutlined />} onClick={() => copyText([r.venueAddress, r.locality, r.city, r.pincode].filter(Boolean).join(', '))} />}
          </Descriptions.Item>
          <Descriptions.Item label="Event Type"><Tag color="purple">{r.eventType}</Tag></Descriptions.Item>
          <Descriptions.Item label="Date & Time">{r.eventDate} at {r.eventTime || '—'}</Descriptions.Item>
          <Descriptions.Item label="Duration">{r.durationHours || '—'} hours</Descriptions.Item>
          <Descriptions.Item label="Guests">{r.guestCount}</Descriptions.Item>
          <Descriptions.Item label="Cuisine">{r.cuisinePreferences || '—'}</Descriptions.Item>
          <Descriptions.Item label="Price/Plate">{r.pricePerPlatePaise ? INR(r.pricePerPlatePaise) : '—'}</Descriptions.Item>
          {tags.length > 0 && (
            <Descriptions.Item label="Menu & Add-ons" span={3}>
              <Space wrap>{tags.map(t => <Tag key={t} color="orange">{t}</Tag>)}</Space>
            </Descriptions.Item>
          )}
          <Descriptions.Item label="Special Requests" span={3}>{r.specialRequests || '—'}</Descriptions.Item>
          <Descriptions.Item label="Total">{r.totalAmountPaise ? <Text strong>{INR(r.totalAmountPaise)}</Text> : '—'}</Descriptions.Item>
          <Descriptions.Item label="Advance (50%)">{r.advanceAmountPaise ? INR(r.advanceAmountPaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Balance">{r.balanceAmountPaise ? INR(r.balanceAmountPaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Food Cost">{r.totalFoodPaise ? INR(r.totalFoodPaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Platform Fee">{r.platformFeePaise ? INR(r.platformFeePaise) : '—'}</Descriptions.Item>
          <Descriptions.Item label="Chef Earnings">{r.chefEarningsPaise ? INR(r.chefEarningsPaise) : '—'}</Descriptions.Item>
          {r.decorationPaise > 0 && <Descriptions.Item label="Decoration">{INR(r.decorationPaise)}</Descriptions.Item>}
          {r.cakePaise > 0 && <Descriptions.Item label="Cake">{INR(r.cakePaise)}</Descriptions.Item>}
          {r.staffPaise > 0 && <Descriptions.Item label="Staff">{INR(r.staffPaise)}</Descriptions.Item>}
          {r.cancellationReason && <Descriptions.Item label="Cancel Reason" span={3}><Text type="danger">{r.cancellationReason}</Text></Descriptions.Item>}
          {r.ratingGiven && <Descriptions.Item label="Rating">{r.ratingGiven} ★ {r.reviewComment && `— "${r.reviewComment}"`}</Descriptions.Item>}
          <Descriptions.Item label="Created">{r.createdAt ? new Date(r.createdAt).toLocaleString() : '—'}</Descriptions.Item>
          {r.quotedAt && <Descriptions.Item label="Quoted">{new Date(r.quotedAt).toLocaleString()}</Descriptions.Item>}
          {r.confirmedAt && <Descriptions.Item label="Confirmed">{new Date(r.confirmedAt).toLocaleString()}</Descriptions.Item>}
        </Descriptions>
      );
    },
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '80px auto' }} />;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>Safar Cooks</Title>
        <Input prefix={<SearchOutlined />} placeholder="Search chefs by name, city, phone..."
          value={search} onChange={e => setSearch(e.target.value)} style={{ width: 300 }} allowClear />
      </div>

      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={5}><Card size="small"><Statistic title="Total Chefs" value={chefs.length} prefix={<FireOutlined />} /></Card></Col>
        <Col span={5}><Card size="small"><Statistic title="Active Bookings" value={activeBookings} prefix={<CalendarOutlined />} valueStyle={{ color: '#1890ff' }} /></Card></Col>
        <Col span={4}><Card size="small"><Statistic title="Events" value={events.length} prefix={<TeamOutlined />} /></Card></Col>
        <Col span={5}><Card size="small"><Statistic title="Subscriptions" value={subs.length} valueStyle={{ color: '#52c41a' }} /></Card></Col>
        <Col span={5}><Card size="small"><Statistic title="Total Revenue" value={INR(totalRevenue)} valueStyle={{ color: '#f97316', fontWeight: 700 }} /></Card></Col>
      </Row>

      <Tabs defaultActiveKey="chefs" items={[
        {
          key: 'chefs', label: <Badge count={chefs.filter(c => c.verificationStatus === 'PENDING').length} offset={[12, 0]}>Chefs ({chefs.length})</Badge>,
          children: <Table columns={chefCols} dataSource={filteredChefs} rowKey="id" scroll={{ x: 1200 }}
            pagination={{ pageSize: 20, showTotal: t => `${t} chefs` }} locale={{ emptyText: 'No chefs registered' }} />,
        },
        {
          key: 'bookings', label: <Badge count={bookings.filter(b => b.status === 'PENDING').length} offset={[12, 0]}>Bookings ({bookings.length})</Badge>,
          children: <Table columns={bookingCols} dataSource={bookings} rowKey="id" scroll={{ x: 1300 }}
            expandable={bookingExpandable}
            pagination={{ pageSize: 20, showTotal: t => `${t} bookings` }} locale={{ emptyText: 'No bookings yet' }} />,
        },
        {
          key: 'events', label: <Badge count={events.filter(e => e.status === 'INQUIRY').length} offset={[12, 0]}>Events ({events.length})</Badge>,
          children: <Table columns={eventCols} dataSource={events} rowKey="id" scroll={{ x: 1350 }}
            expandable={eventExpandable}
            pagination={{ pageSize: 20, showTotal: t => `${t} events` }} locale={{ emptyText: 'No events yet' }} />,
        },
        {
          key: 'subscriptions', label: `Subscriptions (${subs.length})`,
          children: <Table columns={subCols} dataSource={subs} rowKey="id" scroll={{ x: 900 }}
            pagination={{ pageSize: 20 }} locale={{ emptyText: 'No subscriptions yet' }} />,
        },
        {
          key: 'dishes', label: `Dish Catalog (${dishes.length})`,
          children: (
            <div>
              <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end' }}>
                <Button type="primary" icon={<PlusOutlined />}
                  onClick={() => { setDishForm({ active: true, isVeg: true, isRecommended: false, noOnionGarlic: false, isFried: false, sortOrder: 0, pricePaise: 0 }); setDishModal({ visible: true, editing: null }); }}>
                  Add Dish
                </Button>
              </div>
              <Table rowKey="id" dataSource={dishes} scroll={{ x: 1100 }} pagination={{ pageSize: 25 }} columns={[
                { title: 'Name', dataIndex: 'name', width: 200, sorter: (a: any, b: any) => (a.name || '').localeCompare(b.name || '') },
                { title: 'Category', dataIndex: 'category', width: 140, render: (c: string) => <Tag color="orange">{c?.replace(/_/g, ' ')}</Tag>,
                  filters: ['SOUPS_BEVERAGES','APPETIZERS','MAIN_COURSE','BREADS','RICE','RAITA','DESSERTS'].map(k => ({ text: k.replace(/_/g, ' '), value: k })),
                  onFilter: (val: any, r: any) => r.category === val },
                { title: 'Price', dataIndex: 'pricePaise', width: 90, render: (v: number) => v ? INR(v) : '—', sorter: (a: any, b: any) => (a.pricePaise || 0) - (b.pricePaise || 0) },
                { title: 'Veg', dataIndex: 'isVeg', width: 60, render: (v: boolean) => v ? <Tag color="green">Veg</Tag> : <Tag color="red">Non-Veg</Tag>,
                  filters: [{ text: 'Veg', value: true }, { text: 'Non-Veg', value: false }], onFilter: (val: any, r: any) => r.isVeg === val },
                { title: 'Recommended', dataIndex: 'isRecommended', width: 100, render: (v: boolean) => v ? <Tag color="gold">Yes</Tag> : '—' },
                { title: 'No Onion/Garlic', dataIndex: 'noOnionGarlic', width: 120, render: (v: boolean) => v ? <Tag color="purple">Yes</Tag> : '—' },
                { title: 'Fried', dataIndex: 'isFried', width: 70, render: (v: boolean) => v ? <Tag>Yes</Tag> : '—' },
                { title: 'Active', dataIndex: 'active', width: 70, render: (v: boolean) => <Tag color={v ? 'green' : 'red'}>{v ? 'Yes' : 'No'}</Tag> },
                { title: 'Order', dataIndex: 'sortOrder', width: 60 },
                { title: 'Actions', width: 100, fixed: 'right' as const, render: (_: any, r: any) => (
                  <Space size="small">
                    <Button size="small" icon={<EditOutlined />} onClick={() => { setDishForm({ ...r }); setDishModal({ visible: true, editing: r }); }} />
                    <Popconfirm title="Deactivate this dish?" onConfirm={() => handleDeleteDish(r.id)}>
                      <Button size="small" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                  </Space>
                )},
              ]} />
            </div>
          ),
        },
      ]} />

      <Modal
        title={`Assign Cook to ${assignModal?.type === 'booking' ? 'Booking' : 'Event'}`}
        open={!!assignModal?.visible}
        onOk={handleAssign}
        onCancel={() => { setAssignModal(null); setSelectedChef(null); }}
        okText="Assign" okButtonProps={{ disabled: !selectedChef }}
      >
        <div style={{ marginBottom: 8, color: '#6b7280' }}>Select a verified, available cook:</div>
        <Select showSearch style={{ width: '100%' }} placeholder="Search and select a cook"
          value={selectedChef} onChange={setSelectedChef} optionFilterProp="label"
          options={verifiedChefs.map(c => ({
            value: c.id,
            label: `${c.name} — ${c.city || '?'} — ${c.cuisines || 'N/A'} — ${c.rating ? c.rating.toFixed(1) + '★' : 'New'}`,
          }))} />
      </Modal>

      {/* Cancel Booking/Event Modal */}
      <Modal
        title={`Cancel ${cancelModal?.type === 'booking' ? 'Booking' : 'Event'} — ${cancelModal?.ref || ''}`}
        open={!!cancelModal?.visible}
        onOk={cancelModal?.type === 'booking' ? handleCancelBooking : handleCancelEvent}
        onCancel={() => { setCancelModal(null); setCancelReason(''); }}
        okText="Cancel Booking" okButtonProps={{ danger: true, disabled: !cancelReason.trim() }}
      >
        <div style={{ marginBottom: 8, color: '#6b7280' }}>
          This will cancel the {cancelModal?.type} and initiate a refund if payment was made.
        </div>
        <Input.TextArea rows={3} placeholder="Cancellation reason (required)"
          value={cancelReason} onChange={e => setCancelReason(e.target.value)} />
      </Modal>

      {/* Dish Create/Edit Modal */}
      <Modal
        title={dishModal.editing ? `Edit Dish — ${dishModal.editing.name}` : 'Add New Dish'}
        open={dishModal.visible}
        onOk={handleSaveDish}
        onCancel={() => { setDishModal({ visible: false, editing: null }); setDishForm({}); }}
        okText={dishModal.editing ? 'Update' : 'Create'}
        width={600}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div>
            <label style={{ fontWeight: 600, fontSize: 13 }}>Name *</label>
            <Input value={dishForm.name || ''} onChange={e => setDishForm({ ...dishForm, name: e.target.value })} placeholder="e.g. Paneer Butter Masala" />
          </div>
          <div>
            <label style={{ fontWeight: 600, fontSize: 13 }}>Description</label>
            <Input.TextArea rows={2} value={dishForm.description || ''} onChange={e => setDishForm({ ...dishForm, description: e.target.value })} />
          </div>
          <Row gutter={12}>
            <Col span={12}>
              <label style={{ fontWeight: 600, fontSize: 13 }}>Category *</label>
              <Select style={{ width: '100%' }} value={dishForm.category} onChange={v => setDishForm({ ...dishForm, category: v })}
                options={['SOUPS_BEVERAGES','APPETIZERS','MAIN_COURSE','BREADS','RICE','RAITA','DESSERTS'].map(c => ({ value: c, label: c.replace(/_/g, ' ') }))} />
            </Col>
            <Col span={12}>
              <label style={{ fontWeight: 600, fontSize: 13 }}>Price (paise) *</label>
              <Input type="number" value={dishForm.pricePaise ?? 0} onChange={e => setDishForm({ ...dishForm, pricePaise: parseInt(e.target.value) || 0 })}
                addonAfter={dishForm.pricePaise ? `₹${(dishForm.pricePaise / 100).toFixed(0)}` : '₹0'} />
            </Col>
          </Row>
          <div>
            <label style={{ fontWeight: 600, fontSize: 13 }}>Photo URL</label>
            <Input value={dishForm.photoUrl || ''} onChange={e => setDishForm({ ...dishForm, photoUrl: e.target.value })} placeholder="https://..." />
          </div>
          <Row gutter={12}>
            <Col span={6}>
              <label style={{ fontWeight: 600, fontSize: 13 }}>Veg</label><br />
              <Select style={{ width: '100%' }} value={dishForm.isVeg ?? true} onChange={v => setDishForm({ ...dishForm, isVeg: v })}
                options={[{ value: true, label: 'Veg' }, { value: false, label: 'Non-Veg' }]} />
            </Col>
            <Col span={6}>
              <label style={{ fontWeight: 600, fontSize: 13 }}>Recommended</label><br />
              <Select style={{ width: '100%' }} value={dishForm.isRecommended ?? false} onChange={v => setDishForm({ ...dishForm, isRecommended: v })}
                options={[{ value: true, label: 'Yes' }, { value: false, label: 'No' }]} />
            </Col>
            <Col span={6}>
              <label style={{ fontWeight: 600, fontSize: 13 }}>No Onion/Garlic</label><br />
              <Select style={{ width: '100%' }} value={dishForm.noOnionGarlic ?? false} onChange={v => setDishForm({ ...dishForm, noOnionGarlic: v })}
                options={[{ value: true, label: 'Yes' }, { value: false, label: 'No' }]} />
            </Col>
            <Col span={6}>
              <label style={{ fontWeight: 600, fontSize: 13 }}>Fried</label><br />
              <Select style={{ width: '100%' }} value={dishForm.isFried ?? false} onChange={v => setDishForm({ ...dishForm, isFried: v })}
                options={[{ value: true, label: 'Yes' }, { value: false, label: 'No' }]} />
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={8}>
              <label style={{ fontWeight: 600, fontSize: 13 }}>Sort Order</label>
              <Input type="number" value={dishForm.sortOrder ?? 0} onChange={e => setDishForm({ ...dishForm, sortOrder: parseInt(e.target.value) || 0 })} />
            </Col>
            <Col span={8}>
              <label style={{ fontWeight: 600, fontSize: 13 }}>Active</label><br />
              <Select style={{ width: '100%' }} value={dishForm.active ?? true} onChange={v => setDishForm({ ...dishForm, active: v })}
                options={[{ value: true, label: 'Active' }, { value: false, label: 'Inactive' }]} />
            </Col>
          </Row>
        </div>
      </Modal>
    </div>
  );
}

// ── Print helpers ──────────────────────────────────────────────────────────

function printBooking(b: any, chef: any) {
  const addr = [b.address, b.locality, b.city, b.pincode].filter(Boolean).join(', ');
  const html = `<!DOCTYPE html><html><head><title>Booking ${b.bookingRef}</title>
<style>body{font-family:system-ui;max-width:700px;margin:30px auto;padding:20px;color:#333;font-size:14px}
h1{color:#f97316;margin:0;font-size:24px}h2{font-size:16px;margin:20px 0 8px;border-bottom:2px solid #f97316;padding-bottom:4px}
table{width:100%;border-collapse:collapse}td{padding:6px 12px;border-bottom:1px solid #eee;vertical-align:top}
.label{font-weight:600;width:160px;color:#555}.val{color:#111}
.section{background:#fafafa;border-radius:8px;padding:16px;margin:12px 0}
.total{font-size:18px;font-weight:700;color:#f97316}
@media print{button{display:none}}</style></head><body>
<div style="display:flex;justify-content:space-between;align-items:center">
<div><h1>Safar Cooks</h1><p style="margin:4px 0;color:#888">Cook Booking Details</p></div>
<div style="text-align:right"><p style="font-family:monospace;font-size:16px;font-weight:700">${b.bookingRef}</p>
<span style="background:${b.status === 'CONFIRMED' ? '#1890ff' : b.status === 'COMPLETED' ? '#52c41a' : '#faad14'};color:#fff;padding:2px 10px;border-radius:12px;font-size:12px">${b.status}</span></div></div>
<h2>Cook Details</h2><div class="section"><table>
<tr><td class="label">Cook Name</td><td class="val">${b.chefName || '—'}</td></tr>
<tr><td class="label">Cook Phone</td><td class="val">${chef?.phone || '—'}</td></tr>
<tr><td class="label">Cook Email</td><td class="val">${chef?.email || '—'}</td></tr>
<tr><td class="label">Cook City</td><td class="val">${chef?.city || '—'}</td></tr>
</table></div>
<h2>Customer Details</h2><div class="section"><table>
<tr><td class="label">Customer Name</td><td class="val">${b.customerName || '—'}</td></tr>
<tr><td class="label">Customer Phone</td><td class="val">${b.customerPhone || '—'}</td></tr>
<tr><td class="label">Address</td><td class="val">${addr || '—'}</td></tr>
</table></div>
<h2>Booking Details</h2><div class="section"><table>
<tr><td class="label">Service Date</td><td class="val">${b.serviceDate || '—'}</td></tr>
<tr><td class="label">Service Time</td><td class="val">${b.serviceTime || '—'}</td></tr>
<tr><td class="label">Meal Type</td><td class="val">${b.mealType || '—'}</td></tr>
<tr><td class="label">Guests</td><td class="val">${b.guestsCount || '—'}</td></tr>
<tr><td class="label">Menu</td><td class="val">${b.menuName || '—'}</td></tr>
<tr><td class="label">Special Requests</td><td class="val">${b.specialRequests || '—'}</td></tr>
</table></div>
<h2>Payment</h2><div class="section"><table>
<tr><td class="label">Total Amount</td><td class="val total">${b.totalAmountPaise ? '₹' + (b.totalAmountPaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
<tr><td class="label">Advance Paid</td><td class="val" style="color:green">${b.advanceAmountPaise ? '₹' + (b.advanceAmountPaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
<tr><td class="label">Balance Due</td><td class="val" style="color:#f97316">${b.balanceAmountPaise ? '₹' + (b.balanceAmountPaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
<tr><td class="label">Payment Status</td><td class="val">${b.paymentStatus || 'UNPAID'}</td></tr>
${b.razorpayPaymentId ? `<tr><td class="label">Razorpay ID</td><td class="val" style="font-family:monospace">${b.razorpayPaymentId}</td></tr>` : ''}
</table></div>
<p style="text-align:center;color:#aaa;margin-top:30px;font-size:11px">Printed from Safar Admin — ${new Date().toLocaleString()}</p>
<button onclick="window.print()" style="margin:20px auto;display:block;padding:8px 24px;background:#f97316;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:14px">Print</button>
</body></html>`;
  const w = window.open('', '_blank');
  if (w) { w.document.write(html); w.document.close(); }
}

function printEvent(e: any, chef: any) {
  const addr = [e.venueAddress, e.locality, e.city, e.pincode].filter(Boolean).join(', ');
  const { tags } = parseMenuDesc(e.menuDescription);
  const html = `<!DOCTYPE html><html><head><title>Event ${e.bookingRef}</title>
<style>body{font-family:system-ui;max-width:700px;margin:30px auto;padding:20px;color:#333;font-size:14px}
h1{color:#f97316;margin:0;font-size:24px}h2{font-size:16px;margin:20px 0 8px;border-bottom:2px solid #f97316;padding-bottom:4px}
table{width:100%;border-collapse:collapse}td{padding:6px 12px;border-bottom:1px solid #eee;vertical-align:top}
.label{font-weight:600;width:160px;color:#555}.val{color:#111}
.section{background:#fafafa;border-radius:8px;padding:16px;margin:12px 0}
.total{font-size:18px;font-weight:700;color:#f97316}
.tag{display:inline-block;background:#fff7ed;color:#f97316;padding:2px 8px;border-radius:12px;font-size:11px;margin:2px}
@media print{button{display:none}}</style></head><body>
<div style="display:flex;justify-content:space-between;align-items:center">
<div><h1>Safar Cooks</h1><p style="margin:4px 0;color:#888">Event Booking Details</p></div>
<div style="text-align:right"><p style="font-family:monospace;font-size:16px;font-weight:700">${e.bookingRef}</p>
<span style="background:${e.status === 'CONFIRMED' ? '#1890ff' : e.status === 'COMPLETED' ? '#52c41a' : '#faad14'};color:#fff;padding:2px 10px;border-radius:12px;font-size:12px">${e.status}</span></div></div>
<h2>Cook Details</h2><div class="section"><table>
<tr><td class="label">Cook Name</td><td class="val">${e.chefName || 'Unassigned'}</td></tr>
<tr><td class="label">Cook Phone</td><td class="val">${chef?.phone || '—'}</td></tr>
<tr><td class="label">Cook Email</td><td class="val">${chef?.email || '—'}</td></tr>
</table></div>
<h2>Customer Details</h2><div class="section"><table>
<tr><td class="label">Customer Name</td><td class="val">${e.customerName || '—'}</td></tr>
<tr><td class="label">Customer Phone</td><td class="val">${e.customerPhone || '—'}</td></tr>
<tr><td class="label">Customer Email</td><td class="val">${e.customerEmail || '—'}</td></tr>
</table></div>
<h2>Event Details</h2><div class="section"><table>
<tr><td class="label">Event Type</td><td class="val">${e.eventType || '—'}</td></tr>
<tr><td class="label">Date & Time</td><td class="val">${e.eventDate || '—'} at ${e.eventTime || '—'}</td></tr>
<tr><td class="label">Duration</td><td class="val">${e.durationHours || '—'} hours</td></tr>
<tr><td class="label">Guests</td><td class="val">${e.guestCount || '—'}</td></tr>
<tr><td class="label">Venue</td><td class="val">${addr || '—'}</td></tr>
<tr><td class="label">Cuisine</td><td class="val">${e.cuisinePreferences || '—'}</td></tr>
<tr><td class="label">Menu & Add-ons</td><td class="val">${tags.length ? tags.map(t => `<span class="tag">${t}</span>`).join(' ') : '—'}</td></tr>
<tr><td class="label">Special Requests</td><td class="val">${e.specialRequests || '—'}</td></tr>
</table></div>
<h2>Payment</h2><div class="section"><table>
<tr><td class="label">Total Amount</td><td class="val total">${e.totalAmountPaise ? '₹' + (e.totalAmountPaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
<tr><td class="label">Food Cost</td><td class="val">${e.totalFoodPaise ? '₹' + (e.totalFoodPaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
${e.decorationPaise > 0 ? `<tr><td class="label">Decoration</td><td class="val">₹${(e.decorationPaise / 100).toLocaleString('en-IN')}</td></tr>` : ''}
${e.cakePaise > 0 ? `<tr><td class="label">Cake</td><td class="val">₹${(e.cakePaise / 100).toLocaleString('en-IN')}</td></tr>` : ''}
${e.staffPaise > 0 ? `<tr><td class="label">Staff</td><td class="val">₹${(e.staffPaise / 100).toLocaleString('en-IN')}</td></tr>` : ''}
<tr><td class="label">Advance (50%)</td><td class="val" style="color:green">${e.advanceAmountPaise ? '₹' + (e.advanceAmountPaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
<tr><td class="label">Balance Due</td><td class="val" style="color:#f97316">${e.balanceAmountPaise ? '₹' + (e.balanceAmountPaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
<tr><td class="label">Platform Fee</td><td class="val">${e.platformFeePaise ? '₹' + (e.platformFeePaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
<tr><td class="label">Chef Earnings</td><td class="val">${e.chefEarningsPaise ? '₹' + (e.chefEarningsPaise / 100).toLocaleString('en-IN') : '—'}</td></tr>
</table></div>
<p style="text-align:center;color:#aaa;margin-top:30px;font-size:11px">Printed from Safar Admin — ${new Date().toLocaleString()}</p>
<button onclick="window.print()" style="margin:20px auto;display:block;padding:8px 24px;background:#f97316;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:14px">Print</button>
</body></html>`;
  const w = window.open('', '_blank');
  if (w) { w.document.write(html); w.document.close(); }
}
