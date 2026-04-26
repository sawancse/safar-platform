import { useEffect, useMemo, useState } from 'react';
import {
  Table, Tag, Typography, Button, Modal, Form, Input, message, Space,
  Drawer, Descriptions, Image, Empty, Tabs, Popconfirm,
} from 'antd';
import { CheckOutlined, CloseOutlined, EyeOutlined, StopOutlined, RedoOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Text, Paragraph } = Typography;

type ListingRow = {
  id: string;
  vendorUserId: string;
  serviceType: string;
  businessName: string;
  vendorSlug: string;
  heroImageUrl?: string;
  tagline?: string;
  aboutMd?: string;
  status: 'DRAFT' | 'PENDING_REVIEW' | 'VERIFIED' | 'PAUSED' | 'SUSPENDED';
  rejectionReason?: string;
  cities?: string[];
  homeCity?: string;
  pricingPattern?: string;
  trustTier?: string;
  avgRating?: number;
  ratingCount?: number;
  completedBookingsCount?: number;
  createdAt?: string;
};

type KycDocRow = {
  id: string;
  documentType: string;
  documentUrl: string;
  documentNumber?: string;
  verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
  expiresAt?: string;
  rejectionReason?: string;
  uploadedAt: string;
};

const STATUS_TABS = [
  { key: 'PENDING_REVIEW', label: 'Pending Review',  color: 'orange' },
  { key: 'VERIFIED',       label: 'Verified',        color: 'green' },
  { key: 'PAUSED',         label: 'Paused',          color: 'gold' },
  { key: 'SUSPENDED',      label: 'Suspended',       color: 'red' },
  { key: 'DRAFT',          label: 'Drafts',          color: 'default' },
];

const SERVICE_TYPE_ICON: Record<string, string> = {
  CAKE_DESIGNER: '🎂',
  SINGER: '🎤',
  PANDIT: '🪔',
  DECORATOR: '🌸',
  STAFF_HIRE: '🧑‍🍳',
  COOK: '👨‍🍳',
  APPLIANCE_RENTAL: '🍳',
  PHOTOGRAPHER: '📷',
  DJ: '🎧',
  MEHENDI: '🌿',
  MAKEUP_ARTIST: '💄',
};

const TRUST_TIER_COLOR: Record<string, string> = {
  LISTED: 'default',
  SAFAR_VERIFIED: 'blue',
  TOP_RATED: 'gold',
};

export default function ServiceListingsPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [tab, setTab] = useState<string>('PENDING_REVIEW');
  const [rows, setRows] = useState<ListingRow[]>([]);
  const [loading, setLoading] = useState(true);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [active, setActive] = useState<ListingRow | null>(null);
  const [activeKyc, setActiveKyc] = useState<KycDocRow[]>([]);

  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectMode, setRejectMode] = useState<'reject' | 'suspend'>('reject');

  async function load() {
    setLoading(true);
    try {
      const data = await adminApi.listServiceListings(token, tab);
      setRows(data || []);
    } catch (err: any) {
      console.error(err);
      message.error(err?.response?.data?.detail || 'Failed to load listings');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [tab]);

  async function openDetail(row: ListingRow) {
    setActive(row);
    setDrawerOpen(true);
    setActiveKyc([]);
    try {
      const docs = await adminApi.listServiceListingKyc(row.id, token);
      setActiveKyc(docs || []);
    } catch {
      // Non-fatal — show drawer anyway
    }
  }

  async function approve(row: ListingRow) {
    try {
      await adminApi.approveServiceListing(row.id, token);
      message.success(`Approved ${row.businessName}`);
      setDrawerOpen(false);
      load();
    } catch (err: any) {
      message.error(err?.response?.data?.detail || 'Approval failed');
    }
  }

  async function submitReject() {
    if (!active) return;
    if (!rejectReason.trim()) { message.warning('Reason required'); return; }
    try {
      if (rejectMode === 'reject') {
        await adminApi.rejectServiceListing(active.id, rejectReason.trim(), token);
        message.success('Sent back to vendor as DRAFT');
      } else {
        await adminApi.suspendServiceListing(active.id, rejectReason.trim(), token);
        message.success('Listing suspended');
      }
      setRejectOpen(false);
      setRejectReason('');
      setDrawerOpen(false);
      load();
    } catch (err: any) {
      message.error(err?.response?.data?.detail || 'Action failed');
    }
  }

  async function restore(row: ListingRow) {
    try {
      await adminApi.restoreServiceListing(row.id, token);
      message.success(`${row.businessName} restored to DRAFT`);
      load();
    } catch (err: any) {
      message.error(err?.response?.data?.detail || 'Restore failed');
    }
  }

  const columns: ColumnsType<ListingRow> = useMemo(() => [
    {
      title: 'Vendor', dataIndex: 'businessName', key: 'businessName',
      render: (name: string, row) => (
        <Space>
          <span style={{ fontSize: 18 }}>{SERVICE_TYPE_ICON[row.serviceType] ?? '•'}</span>
          <div>
            <div style={{ fontWeight: 600 }}>{name}</div>
            <Text type="secondary" style={{ fontSize: 11 }}>/{row.vendorSlug}</Text>
          </div>
        </Space>
      ),
    },
    { title: 'Type', dataIndex: 'serviceType', key: 'serviceType',
      render: (t: string) => <Tag>{t}</Tag>,
      filters: Object.keys(SERVICE_TYPE_ICON).map(k => ({ text: k, value: k })),
      onFilter: (v, row) => row.serviceType === v,
    },
    { title: 'City', dataIndex: 'homeCity', key: 'homeCity', render: (c: string) => c || '—' },
    { title: 'Pricing', dataIndex: 'pricingPattern', key: 'pricingPattern',
      render: (p: string) => p ? <Tag color="purple">{p}</Tag> : '—' },
    {
      title: 'Trust', dataIndex: 'trustTier', key: 'trustTier',
      render: (tier: string, row) => (
        <Space size={4}>
          <Tag color={TRUST_TIER_COLOR[tier] ?? 'default'}>{tier ?? 'LISTED'}</Tag>
          {row.ratingCount && row.ratingCount > 0 ? (
            <Text type="secondary" style={{ fontSize: 11 }}>★{row.avgRating?.toFixed(1)} ({row.ratingCount})</Text>
          ) : null}
        </Space>
      ),
    },
    {
      title: 'Submitted', dataIndex: 'createdAt', key: 'createdAt',
      render: (iso?: string) => iso ? new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—',
      sorter: (a, b) => (new Date(a.createdAt ?? 0).getTime() - new Date(b.createdAt ?? 0).getTime()),
    },
    {
      title: 'Actions', key: 'actions',
      render: (_, row) => (
        <Space>
          <Button type="link" icon={<EyeOutlined />} onClick={() => openDetail(row)}>Review</Button>
          {row.status === 'PENDING_REVIEW' && (
            <Popconfirm title={`Approve ${row.businessName}?`} okText="Approve" onConfirm={() => approve(row)}>
              <Button type="primary" size="small" icon={<CheckOutlined />}>Approve</Button>
            </Popconfirm>
          )}
          {row.status === 'SUSPENDED' && (
            <Popconfirm title="Restore to DRAFT?" okText="Restore" onConfirm={() => restore(row)}>
              <Button size="small" icon={<RedoOutlined />}>Restore</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ], [/* eslint-disable-line */]);

  return (
    <div>
      <Title level={3}>Service Listings — Admin Queue</Title>
      <Paragraph type="secondary">
        Vendors (cake / singer / pandit / decor / staff) self-onboard via{' '}
        <Text code>/vendor/onboard/{'{type}'}</Text> on safar-web. Review the queue and click Approve / Reject.
        KYC gate enforces FSSAI / police-verification / lineage-proof at submit time, so reject only when
        documents look suspicious or business details are incorrect.
      </Paragraph>

      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={STATUS_TABS.map(s => ({
          key: s.key,
          label: <span><Tag color={s.color}>{s.label}</Tag></span>,
        }))}
      />

      <Table
        rowKey="id"
        loading={loading}
        dataSource={rows}
        columns={columns}
        size="middle"
        locale={{ emptyText: <Empty description={`No ${tab.replace('_',' ').toLowerCase()} listings`} /> }}
      />

      <Drawer
        title={active?.businessName}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={620}
        extra={active?.status === 'PENDING_REVIEW' && (
          <Space>
            <Button type="primary" icon={<CheckOutlined />} onClick={() => active && approve(active)}>
              Approve
            </Button>
            <Button danger icon={<CloseOutlined />} onClick={() => { setRejectMode('reject'); setRejectOpen(true); }}>
              Reject
            </Button>
          </Space>
        )}
      >
        {active && (
          <>
            {active.heroImageUrl && (
              <Image src={active.heroImageUrl} alt={active.businessName} style={{ borderRadius: 12, marginBottom: 16, maxHeight: 240, objectFit: 'cover' }} />
            )}

            <Descriptions bordered column={1} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Service type">
                <Space>
                  <span style={{ fontSize: 16 }}>{SERVICE_TYPE_ICON[active.serviceType]}</span>
                  <Tag>{active.serviceType}</Tag>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Slug">/{active.vendorSlug}</Descriptions.Item>
              {active.tagline && <Descriptions.Item label="Tagline">{active.tagline}</Descriptions.Item>}
              <Descriptions.Item label="Status">
                <Tag color={STATUS_TABS.find(s => s.key === active.status)?.color}>{active.status}</Tag>
              </Descriptions.Item>
              {active.homeCity && <Descriptions.Item label="Home city">{active.homeCity}</Descriptions.Item>}
              {active.cities && active.cities.length > 0 && (
                <Descriptions.Item label="Coverage">{active.cities.join(', ')}</Descriptions.Item>
              )}
              {active.pricingPattern && <Descriptions.Item label="Pricing pattern">{active.pricingPattern}</Descriptions.Item>}
              {active.rejectionReason && (
                <Descriptions.Item label="Last rejection reason">{active.rejectionReason}</Descriptions.Item>
              )}
            </Descriptions>

            {active.aboutMd && (
              <>
                <Title level={5}>About</Title>
                <Paragraph style={{ whiteSpace: 'pre-wrap' }}>{active.aboutMd}</Paragraph>
              </>
            )}

            <Title level={5}>KYC documents</Title>
            {activeKyc.length === 0 ? (
              <Empty description="No KYC documents uploaded" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <Table
                rowKey="id"
                size="small"
                pagination={false}
                dataSource={activeKyc}
                columns={[
                  { title: 'Type', dataIndex: 'documentType', key: 'documentType', render: (t: string) => <Tag>{t}</Tag> },
                  { title: 'Number', dataIndex: 'documentNumber', key: 'documentNumber', render: (n?: string) => n || '—' },
                  {
                    title: 'Status', dataIndex: 'verificationStatus', key: 'verificationStatus',
                    render: (s: string) => (
                      <Tag color={s === 'VERIFIED' ? 'green' : s === 'REJECTED' ? 'red' : 'orange'}>{s}</Tag>
                    ),
                  },
                  {
                    title: 'View', key: 'view',
                    render: (_, d: KycDocRow) => (
                      <a href={d.documentUrl} target="_blank" rel="noreferrer">Open</a>
                    ),
                  },
                ]}
              />
            )}

            {active.status === 'VERIFIED' && (
              <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
                <Button danger icon={<StopOutlined />} onClick={() => { setRejectMode('suspend'); setRejectOpen(true); }}>
                  Suspend listing
                </Button>
              </div>
            )}
          </>
        )}
      </Drawer>

      <Modal
        title={rejectMode === 'reject' ? 'Reject listing' : 'Suspend listing'}
        open={rejectOpen}
        onCancel={() => { setRejectOpen(false); setRejectReason(''); }}
        onOk={submitReject}
        okText={rejectMode === 'reject' ? 'Reject (back to DRAFT)' : 'Suspend'}
        okButtonProps={{ danger: true }}
      >
        <Paragraph type="secondary">
          {rejectMode === 'reject'
            ? 'Listing will move back to DRAFT — vendor can edit + resubmit.'
            : 'Listing will be hidden from customers immediately. Existing bookings honored.'}
        </Paragraph>
        <Form layout="vertical">
          <Form.Item label="Reason" required>
            <Input.TextArea
              rows={4}
              value={rejectReason}
              onChange={e => setRejectReason(e.target.value)}
              placeholder="What needs to be fixed / why this is being suspended"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
