import { useEffect, useState } from 'react';
import { Table, Tag, Typography, Input, Spin, Card, Row, Col, Statistic, Modal, Button, message } from 'antd';
import { UserOutlined, SearchOutlined, TeamOutlined, DollarOutlined, MergeCellsOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;
const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const statusColor: Record<string, string> = {
  DRAFT: 'default', PENDING_PAYMENT: 'orange', CONFIRMED: 'blue',
  CHECKED_IN: 'cyan', COMPLETED: 'green', CANCELLED: 'red', NO_SHOW: 'volcano',
};

interface Guest {
  guestId: string;
  mergedGuestIds?: string[];
  name: string;
  email: string;
  phone: string;
  totalBookings: number;
  totalSpendPaise: number;
  lastBookingDate: string;
}

export default function GuestsPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [guests, setGuests]     = useState<Guest[]>([]);
  const [loading, setLoading]   = useState(true);
  const [search, setSearch]     = useState('');
  const [detail, setDetail]     = useState<{ guest: Guest; bookings: any[] } | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    adminApi.getGuests(token)
      .then(data => setGuests(Array.isArray(data) ? data : []))
      .catch(() => setGuests([]))
      .finally(() => setLoading(false));
  }, [token]);

  const filtered = guests.filter(g => {
    if (!search) return true;
    const q = search.toLowerCase();
    return g.name?.toLowerCase().includes(q) || g.email?.toLowerCase().includes(q) || g.phone?.includes(q);
  });

  const totalSpend = guests.reduce((s, g) => s + (g.totalSpendPaise || 0), 0);

  const openGuestDetail = async (guest: Guest) => {
    setDetailLoading(true);
    setDetail({ guest, bookings: [] });
    try {
      const { data } = await adminApi.getBookingsByGuest(guest.guestId, token);
      setDetail({ guest, bookings: Array.isArray(data) ? data : [] });
    } catch {
      setDetail({ guest, bookings: [] });
    } finally {
      setDetailLoading(false);
    }
  };

  const handleMerge = (guest: Guest) => {
    const ids = guest.mergedGuestIds || [];
    if (ids.length < 2) return;
    const keeperId = guest.guestId;
    const losers = ids.filter(id => id !== keeperId);
    Modal.confirm({
      title: 'Merge duplicate accounts?',
      content: (
        <div>
          <p>This will repoint all bookings, PG tenancies and chef profiles from the duplicate account(s) into the keeper account.</p>
          <p style={{ fontSize: 12, color: '#6b7280' }}>Keeper: <code>{keeperId.slice(0, 8)}…</code></p>
          <p style={{ fontSize: 12, color: '#6b7280' }}>Losing: {losers.map(l => <code key={l} style={{ marginRight: 6 }}>{l.slice(0, 8)}…</code>)}</p>
          <p style={{ fontSize: 12, color: '#dc2626', marginTop: 8 }}>This is irreversible.</p>
        </div>
      ),
      okText: 'Merge',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          for (const loserId of losers) {
            await adminApi.mergeGuests(keeperId, loserId, token);
          }
          message.success(`Merged ${losers.length} duplicate account${losers.length > 1 ? 's' : ''}`);
          setLoading(true);
          const data = await adminApi.getGuests(token);
          setGuests(Array.isArray(data) ? data : []);
          setLoading(false);
        } catch (e: any) {
          message.error(`Merge failed: ${e?.response?.data?.detail || e?.message || 'unknown'}`);
        }
      },
    });
  };

  const columns: ColumnsType<Guest> = [
    {
      title: 'Guest', width: 220,
      render: (_, r) => {
        const dupes = (r.mergedGuestIds?.length ?? 0) - 1;
        return (
          <a onClick={() => openGuestDetail(r)} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <UserOutlined style={{ color: '#f97316' }} />
            <div>
              <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6 }}>
                {r.name?.trim() || '—'}
                {dupes > 0 && <Tag color="red" style={{ marginLeft: 4 }}>{dupes + 1} accounts</Tag>}
              </div>
              <div style={{ fontSize: 12, color: '#6b7280' }}>{r.phone || '—'}</div>
            </div>
          </a>
        );
      },
    },
    { title: 'Email', dataIndex: 'email', width: 180, ellipsis: true, render: (e) => e || '—' },
    { title: 'Bookings', dataIndex: 'totalBookings', width: 100, sorter: (a, b) => a.totalBookings - b.totalBookings,
      defaultSortOrder: 'descend' },
    { title: 'Total Spend', dataIndex: 'totalSpendPaise', width: 120,
      render: (v) => v ? INR(v) : '—', sorter: (a, b) => a.totalSpendPaise - b.totalSpendPaise },
    { title: 'Last Booking', dataIndex: 'lastBookingDate', width: 120,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    {
      title: 'Action', width: 130,
      render: (_, r) => {
        const dupes = (r.mergedGuestIds?.length ?? 0) - 1;
        return dupes > 0 ? (
          <Button size="small" type="primary" danger icon={<MergeCellsOutlined />} onClick={() => handleMerge(r)}>
            Merge
          </Button>
        ) : null;
      },
    },
  ];

  const bookingCols: ColumnsType<any> = [
    { title: 'Ref', dataIndex: 'bookingRef', width: 110, ellipsis: true },
    { title: 'Listing', dataIndex: 'listingTitle', width: 180, ellipsis: true },
    { title: 'Check-in', dataIndex: 'checkIn', width: 100, render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    { title: 'Check-out', dataIndex: 'checkOut', width: 100, render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '—' },
    { title: 'Amount', dataIndex: 'totalAmountPaise', width: 100, render: (v) => v ? INR(v) : '—' },
    { title: 'Status', dataIndex: 'status', width: 110, render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s}</Tag> },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>Guests</Title>
        <Input prefix={<SearchOutlined />} placeholder="Search guests..."
          value={search} onChange={e => setSearch(e.target.value)}
          style={{ width: 260 }} allowClear />
      </div>

      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={8}>
          <Card size="small"><Statistic title="Total Guests" value={guests.length} prefix={<TeamOutlined />} /></Card>
        </Col>
        <Col span={8}>
          <Card size="small"><Statistic title="Total Bookings" value={guests.reduce((s, g) => s + g.totalBookings, 0)} /></Card>
        </Col>
        <Col span={8}>
          <Card size="small"><Statistic title="Total Spend" value={totalSpend ? INR(totalSpend) : '₹0'} prefix={<DollarOutlined />} /></Card>
        </Col>
      </Row>

      {loading ? (
        <Spin size="large" style={{ display: 'block', margin: '60px auto' }} />
      ) : (
        <Table columns={columns} dataSource={filtered} rowKey="guestId"
          scroll={{ x: 800 }} pagination={{ pageSize: 25, showTotal: (t) => `${t} guests` }}
          locale={{ emptyText: 'No guests found' }} />
      )}

      <Modal open={!!detail} onCancel={() => setDetail(null)} footer={null} width={720}
        title={`Guest: ${detail?.guest.name?.trim() || '—'}`}>
        {detail && (
          <>
            <div style={{ marginBottom: 16, color: '#6b7280' }}>
              {detail.guest.email || '—'} | {detail.guest.phone || '—'} | {detail.guest.totalBookings} bookings | Spend: {INR(detail.guest.totalSpendPaise || 0)}
            </div>
            {detailLoading ? (
              <Spin style={{ display: 'block', margin: '30px auto' }} />
            ) : (
              <Table columns={bookingCols} dataSource={detail.bookings} rowKey="id"
                size="small" pagination={false} scroll={{ x: 700 }}
                locale={{ emptyText: 'No bookings' }} />
            )}
          </>
        )}
      </Modal>
    </div>
  );
}
