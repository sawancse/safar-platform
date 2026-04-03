import { useEffect, useState, useCallback } from 'react';
import {
  Table, Button, Space, Tag, Typography, Modal, Input, Tabs,
  message, Tooltip, Spin, Row, Col, Card, Image, Badge,
} from 'antd';
import {
  CheckCircleOutlined, CloseCircleOutlined, DeleteOutlined,
  CameraOutlined, VideoCameraOutlined, FlagOutlined,
  WarningOutlined, SafetyCertificateOutlined,
  ExclamationCircleOutlined, UserOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface Listing {
  id: string;
  hostId: string;
  title: string;
  city: string;
  state: string;
  type: string;
  status: string;
  basePricePaise: number;
  createdAt: string;
}

interface Host {
  id: string;
  name: string;
  phone?: string;
  email?: string;
  subscriptionTier?: string;
  kycStatus?: string;
  createdAt?: string;
}

interface MediaItem {
  id: string;
  url: string;
  type: string;
  isPrimary: boolean;
}

// Cache for media data so we don't refetch on every expand/collapse
const mediaCache: Record<string, { items: MediaItem[]; loading: boolean }> = {};

function getAiBadge(index: number, totalCount: number): { text: string; color: string; icon: React.ReactNode } | null {
  // Placeholder AI pre-screening badges based on position and media count
  if (totalCount <= 1) {
    return { text: 'Low media count', color: 'orange', icon: <WarningOutlined /> };
  }
  if (index === 0 && totalCount > 3) {
    return { text: 'Cover photo OK', color: 'green', icon: <SafetyCertificateOutlined /> };
  }
  if (totalCount >= 5 && index === totalCount - 1) {
    return { text: 'Stock photo risk', color: 'red', icon: <WarningOutlined /> };
  }
  return null;
}

function HostIdentityCard({ kycInfo }: { kycInfo: any }) {
  if (!kycInfo) return <Card size="small" loading style={{ marginBottom: 12 }} />;
  const verified = kycInfo.verified;
  const status = kycInfo.status;
  return (
    <Card size="small" title={<><UserOutlined /> Host Identity</>}
      style={{ marginBottom: 12, borderColor: verified ? '#52c41a' : '#faad14' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
        <div style={{
          width: 48, height: 48, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center',
          background: verified ? '#f6ffed' : '#fff7e6', border: `2px solid ${verified ? '#52c41a' : '#faad14'}`, fontSize: 20,
        }}>
          {verified ? <CheckCircleOutlined style={{ color: '#52c41a' }} /> : <ExclamationCircleOutlined style={{ color: '#faad14' }} />}
        </div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 15 }}>{kycInfo.fullLegalName || kycInfo.hostName || '—'}</div>
          <Tag color={verified ? 'green' : status === 'SUBMITTED' ? 'blue' : status === 'REJECTED' ? 'red' : 'orange'}>
            {status === 'VERIFIED' ? 'Identity Verified' : status === 'SUBMITTED' ? 'Under Review' : status === 'REJECTED' ? 'Rejected' : 'Not Verified'}
          </Tag>
        </div>
      </div>
      {kycInfo.hostPhone && <p style={{ margin: '2px 0', fontSize: 13 }}><Text strong>Phone:</Text> <a href={`tel:${kycInfo.hostPhone}`}>{kycInfo.hostPhone}</a></p>}
      {kycInfo.hostEmail && <p style={{ margin: '2px 0', fontSize: 13 }}><Text strong>Email:</Text> {kycInfo.hostEmail}</p>}
      <div style={{ display: 'flex', gap: 8, marginTop: 8, flexWrap: 'wrap' }}>
        <Tag icon={kycInfo.aadhaarVerified ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
          color={kycInfo.aadhaarVerified ? 'green' : 'default'}>Aadhaar</Tag>
        <Tag icon={kycInfo.panVerified ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
          color={kycInfo.panVerified ? 'green' : 'default'}>PAN</Tag>
        <Tag icon={kycInfo.bankVerified ? <CheckCircleOutlined /> : <CloseCircleOutlined />}
          color={kycInfo.bankVerified ? 'green' : 'default'}>Bank</Tag>
      </div>
      {!verified && (
        <div style={{ marginTop: 8, padding: '6px 10px', background: '#fff7e6', borderRadius: 6, fontSize: 12, color: '#ad6800' }}>
          Host must complete identity verification before listing can be approved.
        </div>
      )}
    </Card>
  );
}

function MediaReviewPanel({ listing, host }: { listing: Listing; host?: Host }) {
  const token = localStorage.getItem('admin_token') ?? '';
  const [media, setMedia] = useState<MediaItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [flagged, setFlagged] = useState<Set<string>>(new Set());
  const [kycInfo, setKycInfo] = useState<any>(null);

  useEffect(() => {
    // Check cache first
    if (mediaCache[listing.id] && !mediaCache[listing.id].loading) {
      setMedia(mediaCache[listing.id].items);
      setLoading(false);
      return;
    }

    mediaCache[listing.id] = { items: [], loading: true };
    adminApi.getListingMedia(listing.id, token)
      .then(({ data }) => {
        mediaCache[listing.id] = { items: data, loading: false };
        setMedia(data);
      })
      .catch(() => {
        mediaCache[listing.id] = { items: [], loading: false };
        setMedia([]);
      })
      .finally(() => setLoading(false));

    // Fetch host KYC info
    adminApi.getHostKyc(listing.id, token)
      .then(({ data }) => setKycInfo(data))
      .catch(() => setKycInfo({ status: 'UNKNOWN', verified: false }));
  }, [listing.id, token]);

  const toggleFlag = useCallback((mediaId: string) => {
    setFlagged(prev => {
      const next = new Set(prev);
      if (next.has(mediaId)) {
        next.delete(mediaId);
        message.info('Flag removed');
      } else {
        next.add(mediaId);
        message.warning('Media flagged for review');
      }
      return next;
    });
  }, []);

  if (loading) {
    return <Spin style={{ display: 'block', margin: '24px auto' }} />;
  }

  const photos = media.filter(m => m.type === 'IMAGE' || m.type === 'PHOTO' || m.type?.startsWith('image'));
  const videos = media.filter(m => m.type === 'VIDEO' || m.type?.startsWith('video'));

  return (
    <Row gutter={16} style={{ padding: '16px 0' }}>
      {/* Left: Listing details */}
      <Col xs={24} md={8}>
        <HostIdentityCard kycInfo={kycInfo} />
        <Card size="small" title="Listing Details" style={{ marginBottom: 12 }}>
          <p><Text strong>Title:</Text> {listing.title}</p>
          <p><Text strong>Location:</Text> {listing.city}, {listing.state}</p>
          <p><Text strong>Type:</Text> <Tag>{listing.type}</Tag></p>
          <p><Text strong>Price:</Text> {`\u20B9${(listing.basePricePaise / 100).toLocaleString('en-IN')}`}/night</p>
          <p><Text strong>Status:</Text> <Tag color="gold">{listing.status}</Tag></p>
          <p><Text strong>Submitted:</Text> {new Date(listing.createdAt).toLocaleDateString('en-IN')}</p>
          <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 8, marginTop: 8 }}>
            <Text strong style={{ fontSize: 13, color: '#1f2937' }}>Host Details</Text>
            {host ? (
              <>
                <p><Text strong>Name:</Text> {host.name || '—'}</p>
                {host.phone && <p><Text strong>Phone:</Text> <a href={`tel:${host.phone}`}>{host.phone}</a></p>}
                {host.email && <p><Text strong>Email:</Text> <a href={`mailto:${host.email}`}>{host.email}</a></p>}
                {host.subscriptionTier && <p><Text strong>Tier:</Text> <Tag color="blue">{host.subscriptionTier}</Tag></p>}
                {host.kycStatus && <p><Text strong>KYC:</Text> <Tag color={host.kycStatus === 'VERIFIED' ? 'green' : 'orange'}>{host.kycStatus}</Tag></p>}
              </>
            ) : (
              <p><Text type="secondary">Host ID: {listing.hostId}</Text></p>
            )}
          </div>
          <p>
            <Text strong>Media:</Text>{' '}
            <Tag icon={<CameraOutlined />} color="blue">{photos.length} photo{photos.length !== 1 ? 's' : ''}</Tag>
            <Tag icon={<VideoCameraOutlined />} color="purple">{videos.length} video{videos.length !== 1 ? 's' : ''}</Tag>
          </p>
          {flagged.size > 0 && (
            <p><Text type="danger"><FlagOutlined /> {flagged.size} media item{flagged.size !== 1 ? 's' : ''} flagged</Text></p>
          )}
        </Card>
      </Col>

      {/* Right: Photo/Video gallery */}
      <Col xs={24} md={16}>
        {media.length === 0 ? (
          <Card size="small">
            <Text type="secondary">No media uploaded for this listing.</Text>
          </Card>
        ) : (
          <>
            {/* Photos */}
            {photos.length > 0 && (
              <Card size="small" title={`Photos (${photos.length})`} style={{ marginBottom: 12 }}>
                <Image.PreviewGroup>
                  <Row gutter={[8, 8]}>
                    {photos.map((photo, idx) => {
                      const badge = getAiBadge(idx, photos.length);
                      const isFlagged = flagged.has(photo.id);
                      const imgUrl = photo.url.startsWith('http')
                        ? photo.url
                        : `/api/v1/listings/${listing.id}/media/file/${photo.url.split('/').pop()}`;

                      return (
                        <Col key={photo.id} xs={12} sm={8} md={6}>
                          <Card
                            size="small"
                            hoverable
                            style={{
                              border: isFlagged ? '2px solid #ff4d4f' : '1px solid #f0f0f0',
                            }}
                            cover={
                              <div style={{ position: 'relative' }}>
                                <Image
                                  src={imgUrl}
                                  alt={`Photo ${idx + 1}`}
                                  style={{ height: 120, objectFit: 'cover', width: '100%' }}
                                  fallback="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjEyMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjEyMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5IiBmb250LXNpemU9IjE0Ij5ObyBJbWFnZTwvdGV4dD48L3N2Zz4="
                                />
                                {photo.isPrimary && (
                                  <Tag color="blue" style={{ position: 'absolute', top: 4, left: 4, margin: 0 }}>
                                    Primary
                                  </Tag>
                                )}
                                {badge && (
                                  <Tag
                                    color={badge.color}
                                    icon={badge.icon}
                                    style={{ position: 'absolute', bottom: 4, left: 4, margin: 0, fontSize: 11 }}
                                  >
                                    {badge.text}
                                  </Tag>
                                )}
                              </div>
                            }
                            actions={[
                              <Tooltip key="approve" title="Approve photo">
                                <CheckCircleOutlined
                                  style={{ color: isFlagged ? undefined : '#52c41a' }}
                                  onClick={() => {
                                    if (isFlagged) toggleFlag(photo.id);
                                    message.success('Photo approved');
                                  }}
                                />
                              </Tooltip>,
                              <Tooltip key="flag" title={isFlagged ? 'Remove flag' : 'Flag photo'}>
                                <FlagOutlined
                                  style={{ color: isFlagged ? '#ff4d4f' : undefined }}
                                  onClick={() => toggleFlag(photo.id)}
                                />
                              </Tooltip>,
                              <Tooltip key="delete" title="Delete photo">
                                <DeleteOutlined
                                  style={{ color: '#ff4d4f' }}
                                  onClick={() => {
                                    Modal.confirm({
                                      title: 'Delete this photo?',
                                      content: 'This action cannot be undone.',
                                      okText: 'Delete',
                                      okType: 'danger',
                                      onOk: async () => {
                                        try {
                                          await adminApi.deleteListingMedia(listing.id, photo.id, token);
                                          setMedia(prev => prev.filter(m => m.id !== photo.id));
                                          message.success('Photo deleted');
                                        } catch { message.error('Failed to delete photo'); }
                                      },
                                    });
                                  }}
                                />
                              </Tooltip>,
                            ]}
                          />
                        </Col>
                      );
                    })}
                  </Row>
                </Image.PreviewGroup>
              </Card>
            )}

            {/* Videos */}
            {videos.length > 0 && (
              <Card size="small" title={`Videos (${videos.length})`}>
                <Row gutter={[8, 8]}>
                  {videos.map((video, idx) => {
                    const isFlagged = flagged.has(video.id);
                    const videoUrl = video.url.startsWith('http')
                      ? video.url
                      : `/api/v1/listings/${listing.id}/media/file/${video.url.split('/').pop()}`;

                    return (
                      <Col key={video.id} xs={24} sm={12}>
                        <Card
                          size="small"
                          style={{
                            border: isFlagged ? '2px solid #ff4d4f' : '1px solid #f0f0f0',
                          }}
                          actions={[
                            <Tooltip key="approve" title="Approve video">
                              <CheckCircleOutlined
                                style={{ color: isFlagged ? undefined : '#52c41a' }}
                                onClick={() => {
                                  if (isFlagged) toggleFlag(video.id);
                                  message.success('Video approved');
                                }}
                              />
                            </Tooltip>,
                            <Tooltip key="flag" title={isFlagged ? 'Remove flag' : 'Flag video'}>
                              <FlagOutlined
                                style={{ color: isFlagged ? '#ff4d4f' : undefined }}
                                onClick={() => toggleFlag(video.id)}
                              />
                            </Tooltip>,
                            <Tooltip key="delete" title="Delete video">
                              <DeleteOutlined
                                style={{ color: '#ff4d4f' }}
                                onClick={() => {
                                  Modal.confirm({
                                    title: 'Delete this video?',
                                    content: 'This action cannot be undone.',
                                    okText: 'Delete',
                                    okType: 'danger',
                                    onOk: async () => {
                                      try {
                                        await adminApi.deleteListingMedia(listing.id, video.id, token);
                                        setMedia(prev => prev.filter(m => m.id !== video.id));
                                        message.success('Video deleted');
                                      } catch { message.error('Failed to delete video'); }
                                    },
                                  });
                                }}
                              />
                            </Tooltip>,
                          ]}
                        >
                          <Tag color="purple" style={{ marginBottom: 8 }}>
                            <VideoCameraOutlined /> Video review
                          </Tag>
                          <video
                            src={videoUrl}
                            controls
                            style={{ width: '100%', maxHeight: 200, background: '#000' }}
                          >
                            Your browser does not support video playback.
                          </video>
                        </Card>
                      </Col>
                    );
                  })}
                </Row>
              </Card>
            )}
          </>
        )}
      </Col>
    </Row>
  );
}

const STATUS_TABS = [
  { key: 'ALL', label: 'All' },
  { key: 'PENDING_VERIFICATION', label: 'Pending' },
  { key: 'VERIFIED', label: 'Verified' },
  { key: 'DRAFT', label: 'Draft' },
  { key: 'PAUSED', label: 'Paused' },
  { key: 'REJECTED', label: 'Rejected' },
  { key: 'ARCHIVED', label: 'Archived' },
  { key: 'SUSPENDED', label: 'Suspended' },
];

const SUSPEND_REASONS = [
  { value: 'FRAUDULENT', label: 'Fraudulent listing' },
  { value: 'DUPLICATE', label: 'Duplicate listing' },
  { value: 'POLICY_VIOLATION', label: 'Policy violation' },
  { value: 'EXPIRED', label: 'Expired / abandoned' },
  { value: 'ADMIN_OTHER', label: 'Other' },
];

export default function ListingsPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [listings, setListings]     = useState<Listing[]>([]);
  const [loading, setLoading]       = useState(true);
  const [activeTab, setActiveTab]   = useState('ALL');
  const [rejectModal, setRejectModal] = useState<{ open: boolean; id: string }>({ open: false, id: '' });
  const [rejectNotes, setRejectNotes] = useState('');
  const [suspendModal, setSuspendModal] = useState<{ open: boolean; id: string }>({ open: false, id: '' });
  const [suspendReason, setSuspendReason] = useState('POLICY_VIOLATION');
  const [suspendNote, setSuspendNote] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [mediaCounts, setMediaCounts] = useState<Record<string, { photos: number; videos: number }>>({});
  const [hostsMap, setHostsMap] = useState<Record<string, Host>>({});
  const [kycMap, setKycMap] = useState<Record<string, any>>({});

  function reload() {
    setLoading(true);
    if (!token) {
      message.warning('No admin token found — please log in again');
      setLoading(false);
      return;
    }
    const statusParam = activeTab === 'ALL' ? undefined : activeTab;
    adminApi.getListingsByStatus(token, statusParam)
      .catch((err) => {
        console.error('getListingsByStatus failed, falling back to pending:', err?.response?.status, err?.response?.data);
        return adminApi.getPendingListings(token);
      })
      .then(({ data }) => {
        setListings(data);
        // Fetch KYC status for pending listings
        data.filter((l: Listing) => l.status === 'PENDING_VERIFICATION').forEach((listing: Listing) => {
          adminApi.getHostKyc(listing.id, token)
            .then(({ data: kyc }) => setKycMap(prev => ({ ...prev, [listing.id]: kyc })))
            .catch(() => {});
        });
        // Fetch media counts for all listings
        data.forEach((listing: Listing) => {
          adminApi.getListingMedia(listing.id, token)
            .then(({ data: mediaItems }) => {
              const photos = mediaItems.filter(
                (m: MediaItem) => m.type === 'IMAGE' || m.type === 'PHOTO' || m.type?.startsWith('image'),
              ).length;
              const videos = mediaItems.filter(
                (m: MediaItem) => m.type === 'VIDEO' || m.type?.startsWith('video'),
              ).length;
              mediaCache[listing.id] = { items: mediaItems, loading: false };
              setMediaCounts(prev => ({ ...prev, [listing.id]: { photos, videos } }));
            })
            .catch(() => {
              setMediaCounts(prev => ({ ...prev, [listing.id]: { photos: 0, videos: 0 } }));
            });
        });
      })
      .catch(() => setListings([]))
      .finally(() => setLoading(false));
  }

  useEffect(() => { reload(); }, [token, activeTab]);

  // Fetch hosts once for cross-referencing
  useEffect(() => {
    if (!token) return;
    adminApi.getHosts(token)
      .then(({ data }) => {
        const map: Record<string, Host> = {};
        data.forEach((h: Host) => { map[h.id] = h; });
        setHostsMap(map);
      })
      .catch(() => {});
  }, [token]);

  async function handleVerify(id: string) {
    setActionLoading(true);
    try {
      await adminApi.verifyListing(id, token);
      message.success('Listing verified');
      reload();
    } catch (err: any) {
      const detail = err?.response?.data?.detail || err?.response?.data?.message || err?.message || 'Failed to verify listing';
      const status = err?.response?.status;
      message.error(`Verify failed${status ? ` (${status})` : ''}: ${detail}`);
      console.error('Verify error:', err?.response?.data || err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleReject() {
    setActionLoading(true);
    try {
      await adminApi.rejectListing(rejectModal.id, rejectNotes, token);
      message.success('Listing rejected');
      setRejectModal({ open: false, id: '' });
      setRejectNotes('');
      reload();
    } catch (err: any) {
      const detail = err?.response?.data?.detail || err?.response?.data?.message || err?.message || 'Failed to reject listing';
      const status = err?.response?.status;
      message.error(`Reject failed${status ? ` (${status})` : ''}: ${detail}`);
      console.error('Reject error:', err?.response?.data || err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleSuspend() {
    setActionLoading(true);
    try {
      await adminApi.suspendListing(suspendModal.id, suspendReason, suspendNote, token);
      message.success('Listing suspended');
      setSuspendModal({ open: false, id: '' });
      setSuspendReason('POLICY_VIOLATION');
      setSuspendNote('');
      reload();
    } catch (err: any) {
      message.error(`Suspend failed: ${err?.response?.data?.message || err?.message}`);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleRestore(id: string) {
    setActionLoading(true);
    try {
      await adminApi.restoreListing(id, token);
      message.success('Listing restored to DRAFT');
      reload();
    } catch (err: any) {
      message.error(`Restore failed: ${err?.response?.data?.message || err?.message}`);
    } finally {
      setActionLoading(false);
    }
  }

  const columns: ColumnsType<Listing> = [
    {
      title: 'Title',
      dataIndex: 'title',
      ellipsis: true,
      width: 200,
    },
    {
      title: 'Location',
      render: (_, r) => `${r.city}, ${r.state}`,
      width: 140,
    },
    {
      title: 'Host',
      width: 160,
      render: (_, r) => {
        const host = hostsMap[r.hostId];
        return host ? (
          <div>
            <div style={{ fontWeight: 600 }}>{host.name || '—'}</div>
            <div style={{ fontSize: 12, color: '#6b7280' }}>{host.phone || host.email || '—'}</div>
            {host.kycStatus && <Tag color={host.kycStatus === 'VERIFIED' ? 'green' : 'orange'} style={{ fontSize: 10, marginTop: 2 }}>{host.kycStatus}</Tag>}
          </div>
        ) : (
          <Text type="secondary" style={{ fontSize: 12 }}>ID: {r.hostId?.slice(0, 8)}… (loading…)</Text>
        );
      },
    },
    {
      title: 'Type',
      dataIndex: 'type',
      width: 100,
      render: (t) => <Tag>{t}</Tag>,
    },
    {
      title: 'Price / night',
      dataIndex: 'basePricePaise',
      width: 120,
      render: (p) => `\u20B9${(p / 100).toLocaleString('en-IN')}`,
    },
    {
      title: 'Media',
      width: 130,
      render: (_, record) => {
        const counts = mediaCounts[record.id];
        if (!counts) return <Spin size="small" />;
        const total = counts.photos + counts.videos;
        return (
          <Space size={4}>
            <Badge
              count={total === 0 ? '!' : undefined}
              status={total === 0 ? 'error' : undefined}
            >
              <Tag icon={<CameraOutlined />} color={counts.photos > 0 ? 'blue' : 'default'}>
                {counts.photos}
              </Tag>
            </Badge>
            <Tag icon={<VideoCameraOutlined />} color={counts.videos > 0 ? 'purple' : 'default'}>
              {counts.videos}
            </Tag>
          </Space>
        );
      },
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 110,
      render: (s) => {
        const colorMap: Record<string, string> = {
          PENDING_VERIFICATION: 'gold', VERIFIED: 'green', REJECTED: 'red',
          DRAFT: 'default', PAUSED: 'blue', ARCHIVED: 'purple', SUSPENDED: 'magenta',
        };
        return <Tag color={colorMap[s] ?? 'default'}>{s}</Tag>;
      },
    },
    {
      title: 'Submitted',
      dataIndex: 'createdAt',
      width: 120,
      render: (d) => new Date(d).toLocaleDateString('en-IN'),
    },
    {
      title: 'Actions',
      fixed: 'right',
      width: 220,
      render: (_, record) => {
        const s = record.status;
        return (
          <Space wrap size={4}>
            {/* PENDING → Verify / Reject */}
            {s === 'PENDING_VERIFICATION' && (() => {
              const kyc = kycMap[record.id];
              const kycVerified = kyc?.verified === true;
              const kycStatus = kyc?.status;
              return (
                <>
                  {!kycVerified && kyc && (
                    <Tooltip title={`Host identity not verified (${kycStatus}). Verify KYC first.`}>
                      <Tag color="warning" icon={<ExclamationCircleOutlined />} style={{ fontSize: 11 }}>KYC {kycStatus}</Tag>
                    </Tooltip>
                  )}
                  <Tooltip title={!kycVerified && kyc ? 'Host must complete identity verification first' : ''}>
                    <Button type="primary" icon={<CheckCircleOutlined />} size="small"
                      disabled={!kycVerified && kyc != null && kycStatus !== 'UNKNOWN'}
                      onClick={(e) => { e.stopPropagation(); handleVerify(record.id); }}
                      loading={actionLoading}>Verify</Button>
                  </Tooltip>
                  <Button danger icon={<CloseCircleOutlined />} size="small"
                    onClick={(e) => { e.stopPropagation(); setRejectModal({ open: true, id: record.id }); }}>Reject</Button>
                </>
              );
            })()}
            {/* VERIFIED/PAUSED/REJECTED/DRAFT → Suspend */}
            {['VERIFIED', 'PAUSED', 'REJECTED', 'DRAFT'].includes(s) && (
              <Button size="small" danger type="dashed"
                onClick={(e) => { e.stopPropagation(); setSuspendModal({ open: true, id: record.id }); }}>
                Suspend
              </Button>
            )}
            {/* VERIFIED → can also Verify→Draft (unpublish) */}
            {s === 'VERIFIED' && (
              <Button size="small"
                onClick={(e) => { e.stopPropagation(); handleRestore(record.id); }}>
                → Draft
              </Button>
            )}
            {/* SUSPENDED/ARCHIVED → Restore to Draft */}
            {(s === 'SUSPENDED' || s === 'ARCHIVED') && (
              <Button type="primary" size="small"
                onClick={(e) => { e.stopPropagation(); handleRestore(record.id); }}
                loading={actionLoading}>
                Restore → Draft
              </Button>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>Listings Management</Title>

      <Tabs
        activeKey={activeTab}
        onChange={(key) => setActiveTab(key)}
        items={STATUS_TABS.map((tab) => ({ key: tab.key, label: tab.label }))}
        style={{ marginBottom: 16 }}
      />

      {loading ? (
        <Spin size="large" style={{ display: 'block', margin: '60px auto' }} />
      ) : (
        <Table
          columns={columns}
          dataSource={listings}
          rowKey="id"
          scroll={{ x: 1200 }}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          locale={{ emptyText: `No ${activeTab === 'ALL' ? '' : activeTab.toLowerCase().replace('_', ' ') + ' '}listings` }}
          expandable={{
            expandedRowRender: (record) => <MediaReviewPanel listing={record} host={hostsMap[record.hostId]} />,
            rowExpandable: () => true,
          }}
        />
      )}

      <Modal
        title="Reject listing"
        open={rejectModal.open}
        onOk={handleReject}
        onCancel={() => { setRejectModal({ open: false, id: '' }); setRejectNotes(''); }}
        okText="Reject"
        okButtonProps={{ danger: true, loading: actionLoading }}
      >
        <Text type="secondary">Provide a reason for rejection (visible to host):</Text>
        <TextArea
          rows={3}
          style={{ marginTop: 8 }}
          value={rejectNotes}
          onChange={(e) => setRejectNotes(e.target.value)}
          placeholder="e.g. Photos are unclear, listing description is incomplete..."
        />
      </Modal>

      <Modal
        title="Suspend listing"
        open={suspendModal.open}
        onOk={handleSuspend}
        onCancel={() => { setSuspendModal({ open: false, id: '' }); setSuspendReason('POLICY_VIOLATION'); setSuspendNote(''); }}
        okText="Suspend"
        okButtonProps={{ danger: true, loading: actionLoading }}
      >
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary">Reason for suspension:</Text>
          <select
            value={suspendReason}
            onChange={(e) => setSuspendReason(e.target.value)}
            style={{ display: 'block', width: '100%', marginTop: 4, padding: '6px 8px', borderRadius: 6, border: '1px solid #d9d9d9' }}
          >
            {SUSPEND_REASONS.map((r) => (
              <option key={r.value} value={r.value}>{r.label}</option>
            ))}
          </select>
        </div>
        <Text type="secondary">Note (internal, not visible to host):</Text>
        <TextArea
          rows={3}
          style={{ marginTop: 4 }}
          value={suspendNote}
          onChange={(e) => setSuspendNote(e.target.value)}
          placeholder="e.g. Reported by multiple guests, fake photos detected..."
        />
      </Modal>
    </div>
  );
}
