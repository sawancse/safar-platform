import { useState, useEffect, useCallback } from 'react';
import {
  Table, Tabs, Card, Tag, Modal, Form, Input, Select, Button, Statistic,
  Badge, Descriptions, Timeline, Space, message, Row, Col, Checkbox, Spin,
} from 'antd';
import {
  WarningOutlined, CheckCircleOutlined, ClockCircleOutlined,
  ExclamationCircleOutlined, MessageOutlined, DollarOutlined,
  SyncOutlined, ArrowUpOutlined, FileTextOutlined, UserOutlined,
} from '@ant-design/icons';
import axios from 'axios';

const BASE = (import.meta.env.VITE_API_URL || '') + '/api/v1';

function authHeaders() {
  const token = localStorage.getItem('admin_token') || localStorage.getItem('access_token') || '';
  return { Authorization: `Bearer ${token}` };
}

function formatPaise(paise: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', minimumFractionDigits: 0,
  }).format(paise / 100);
}

function formatDate(d: string | null | undefined): string {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatDateTime(d: string | null | undefined): string {
  if (!d) return '—';
  return new Date(d).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

// --- Types ---

interface TicketStats {
  totalOpen: number;
  inProgress: number;
  slaBreached: number;
  avgResolutionHours: number;
}

interface Ticket {
  id: string;
  requestNumber: string;
  category: string;
  title: string;
  description: string;
  priority: string;
  status: string;
  slaBreached: boolean;
  escalationLevel: string;
  assignedTo: string | null;
  tenantId: string;
  tenancyId: string;
  listingId: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  resolutionNotes: string | null;
}

interface TicketComment {
  id: string;
  ticketId: string;
  authorId: string;
  authorRole: string;
  commentText: string;
  createdAt: string;
}

interface SettlementStats {
  pending: number;
  disputed: number;
  overdue: number;
  settled: number;
}

interface Settlement {
  id: string;
  refNumber: string;
  tenantId: string;
  tenancyId: string;
  moveOutDate: string;
  depositPaise: number;
  totalDeductionsPaise: number;
  refundPaise: number;
  deadline: string;
  status: string;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
  inspectionDate: string | null;
  inspectorNotes: string | null;
  adminNotes: string | null;
  overrideRefundPaise: number | null;
}

interface InspectionItem {
  id: string;
  item: string;
  condition: string;
  damagePaise: number;
  notes: string;
}

interface Deduction {
  id: string;
  category: string;
  description: string;
  amountPaise: number;
  disputed: boolean;
  disputeReason: string | null;
  decision: string | null;
  adjustedPaise: number | null;
  adminNotes: string | null;
}

interface SettlementEvent {
  event: string;
  timestamp: string;
  actor: string;
  notes: string | null;
}

// --- Color Maps ---

const PRIORITY_COLOR: Record<string, string> = {
  URGENT: 'red', HIGH: 'orange', MEDIUM: 'blue', LOW: 'default',
};

const TICKET_STATUS_COLOR: Record<string, string> = {
  OPEN: 'blue', ASSIGNED: 'cyan', IN_PROGRESS: 'processing', RESOLVED: 'green',
  CLOSED: 'default', REOPENED: 'orange',
};

const SETTLEMENT_STATUS_COLOR: Record<string, string> = {
  INITIATED: 'blue', INSPECTION_DONE: 'orange', APPROVED: 'green',
  REFUND_PROCESSING: 'cyan', SETTLED: 'green', DISPUTED: 'red', ADMIN_RESOLVED: 'purple',
};

const ROLE_COLOR: Record<string, string> = {
  TENANT: '#1677ff', HOST: '#fa8c16', ADMIN: '#f5222d', SYSTEM: '#8c8c8c',
};

// =============================================================================
// TICKETS TAB
// =============================================================================

function TicketsTab() {
  const [stats, setStats] = useState<TicketStats>({ totalOpen: 0, inProgress: 0, slaBreached: 0, avgResolutionHours: 0 });
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);

  // Filters
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [slaFilter, setSlaFilter] = useState(false);
  const [searchText, setSearchText] = useState('');

  // Detail modal
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [comments, setComments] = useState<TicketComment[]>([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);

  // Actions
  const [reassignTo, setReassignTo] = useState('');
  const [escalationLevel, setEscalationLevel] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const loadStats = useCallback(async () => {
    try {
      const res = await axios.get(`${BASE}/admin/pg/tickets/stats`, { headers: authHeaders() });
      setStats(res.data);
    } catch { /* ignore */ }
  }, []);

  const loadTickets = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = { page: String(page - 1), size: String(pageSize) };
      if (statusFilter.length) params.status = statusFilter.join(',');
      if (slaFilter) params.slaBreached = 'true';
      if (searchText.trim()) params.search = searchText.trim();
      const res = await axios.get(`${BASE}/admin/pg/tickets`, { headers: authHeaders(), params });
      setTickets(res.data.content || []);
      setTotal(res.data.totalElements || 0);
    } catch {
      setTickets([]);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, statusFilter, slaFilter, searchText]);

  useEffect(() => { loadStats(); }, [loadStats]);
  useEffect(() => { loadTickets(); }, [loadTickets]);

  const loadComments = async (ticketId: string) => {
    setCommentsLoading(true);
    try {
      const res = await axios.get(`${BASE}/admin/pg/tickets/${ticketId}/comments`, { headers: authHeaders() });
      setComments(Array.isArray(res.data) ? res.data : []);
    } catch {
      setComments([]);
    } finally {
      setCommentsLoading(false);
    }
  };

  const openDetail = (ticket: Ticket) => {
    setSelectedTicket(ticket);
    setModalOpen(true);
    setNewComment('');
    setReassignTo(ticket.assignedTo || '');
    setEscalationLevel(ticket.escalationLevel || 'L1');
    loadComments(ticket.id);
  };

  const handleAddComment = async () => {
    if (!newComment.trim() || !selectedTicket) return;
    setCommentSubmitting(true);
    try {
      await axios.post(
        `${BASE}/admin/pg/tickets/${selectedTicket.id}/comments`,
        { commentText: newComment.trim() },
        { headers: authHeaders() },
      );
      message.success('Comment added');
      setNewComment('');
      loadComments(selectedTicket.id);
    } catch {
      message.error('Failed to add comment');
    } finally {
      setCommentSubmitting(false);
    }
  };

  const handleUpdateTicket = async (update: Record<string, string>) => {
    if (!selectedTicket) return;
    setActionLoading(true);
    try {
      const res = await axios.put(
        `${BASE}/admin/pg/tickets/${selectedTicket.id}`,
        update,
        { headers: authHeaders() },
      );
      message.success('Ticket updated');
      setSelectedTicket(res.data);
      loadTickets();
      loadStats();
    } catch {
      message.error('Failed to update ticket');
    } finally {
      setActionLoading(false);
    }
  };

  const columns = [
    {
      title: 'Request#', dataIndex: 'requestNumber', key: 'requestNumber', width: 130,
      render: (v: string) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{v}</span>,
    },
    { title: 'Category', dataIndex: 'category', key: 'category', width: 120 },
    { title: 'Title', dataIndex: 'title', key: 'title', ellipsis: true },
    {
      title: 'Priority', dataIndex: 'priority', key: 'priority', width: 100,
      render: (v: string) => <Tag color={PRIORITY_COLOR[v] || 'default'}>{v}</Tag>,
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status', width: 120,
      render: (v: string) => <Tag color={TICKET_STATUS_COLOR[v] || 'default'}>{v}</Tag>,
    },
    {
      title: 'SLA', dataIndex: 'slaBreached', key: 'sla', width: 100,
      render: (breached: boolean) => breached
        ? <Badge status="error" text="Breached" />
        : <Badge status="success" text="OK" />,
    },
    {
      title: 'Escalation', dataIndex: 'escalationLevel', key: 'escalation', width: 80,
      render: (v: string) => <Tag>{v || 'L1'}</Tag>,
    },
    {
      title: 'Created', dataIndex: 'createdAt', key: 'createdAt', width: 140,
      render: (v: string) => formatDate(v),
    },
    {
      title: 'Actions', key: 'actions', width: 90,
      render: (_: unknown, record: Ticket) => (
        <Button type="link" size="small" onClick={() => openDetail(record)}>View</Button>
      ),
    },
  ];

  return (
    <>
      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="Total Open" value={stats.totalOpen} prefix={<FileTextOutlined />} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="In Progress" value={stats.inProgress} prefix={<SyncOutlined />} valueStyle={{ color: '#1677ff' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="SLA Breached" value={stats.slaBreached} prefix={<WarningOutlined />} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Avg Resolution" value={stats.avgResolutionHours} suffix="hrs" prefix={<ClockCircleOutlined />} /></Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            mode="multiple"
            placeholder="Filter by Status"
            style={{ minWidth: 220 }}
            value={statusFilter}
            onChange={v => { setStatusFilter(v); setPage(1); }}
            allowClear
            options={['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED'].map(s => ({ label: s, value: s }))}
          />
          <Checkbox checked={slaFilter} onChange={e => { setSlaFilter(e.target.checked); setPage(1); }}>
            SLA Breached Only
          </Checkbox>
          <Input.Search
            placeholder="Search by request#"
            style={{ width: 220 }}
            allowClear
            onSearch={v => { setSearchText(v); setPage(1); }}
          />
        </Space>
      </Card>

      {/* Table */}
      <Table
        dataSource={tickets}
        columns={columns}
        rowKey="id"
        loading={loading}
        size="small"
        pagination={{
          current: page, pageSize, total, showTotal: t => `${t} tickets`,
          onChange: p => setPage(p),
        }}
        onRow={record => ({ onClick: () => openDetail(record), style: { cursor: 'pointer' } })}
      />

      {/* Detail Modal */}
      <Modal
        title={selectedTicket ? `Ticket ${selectedTicket.requestNumber}` : 'Ticket Detail'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        width={780}
        destroyOnClose
      >
        {selectedTicket && (
          <div style={{ maxHeight: '70vh', overflowY: 'auto' }}>
            <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Request#">{selectedTicket.requestNumber}</Descriptions.Item>
              <Descriptions.Item label="Category">{selectedTicket.category}</Descriptions.Item>
              <Descriptions.Item label="Title" span={2}>{selectedTicket.title}</Descriptions.Item>
              <Descriptions.Item label="Description" span={2}>{selectedTicket.description}</Descriptions.Item>
              <Descriptions.Item label="Priority">
                <Tag color={PRIORITY_COLOR[selectedTicket.priority]}>{selectedTicket.priority}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={TICKET_STATUS_COLOR[selectedTicket.status]}>{selectedTicket.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="SLA">
                {selectedTicket.slaBreached
                  ? <Badge status="error" text="Breached" />
                  : <Badge status="success" text="OK" />}
              </Descriptions.Item>
              <Descriptions.Item label="Escalation">{selectedTicket.escalationLevel || 'L1'}</Descriptions.Item>
              <Descriptions.Item label="Assigned To">{selectedTicket.assignedTo || '—'}</Descriptions.Item>
              <Descriptions.Item label="Tenant ID">
                <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{selectedTicket.tenantId}</span>
              </Descriptions.Item>
              <Descriptions.Item label="Created">{formatDateTime(selectedTicket.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="Resolved">{formatDateTime(selectedTicket.resolvedAt)}</Descriptions.Item>
              {selectedTicket.resolutionNotes && (
                <Descriptions.Item label="Resolution Notes" span={2}>{selectedTicket.resolutionNotes}</Descriptions.Item>
              )}
            </Descriptions>

            {/* Comments */}
            <Card
              title={<><MessageOutlined /> Comments</>}
              size="small"
              style={{ marginBottom: 16 }}
            >
              {commentsLoading ? (
                <Spin size="small" />
              ) : comments.length === 0 ? (
                <div style={{ color: '#999', padding: 8 }}>No comments yet</div>
              ) : (
                <div style={{ maxHeight: 300, overflowY: 'auto', marginBottom: 12 }}>
                  {comments.map(c => (
                    <div key={c.id} style={{ marginBottom: 12, display: 'flex', gap: 10 }}>
                      <div
                        style={{
                          width: 32, height: 32, borderRadius: '50%',
                          background: ROLE_COLOR[c.authorRole] || '#8c8c8c',
                          display: 'flex', alignItems: 'center', justifyContent: 'center',
                          color: '#fff', fontSize: 12, fontWeight: 600, flexShrink: 0,
                        }}
                      >
                        {c.authorRole?.[0] || '?'}
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: 12, color: '#666' }}>
                          <strong style={{ color: ROLE_COLOR[c.authorRole] || '#333' }}>{c.authorRole}</strong>
                          {' '}&middot;{' '}{formatDateTime(c.createdAt)}
                        </div>
                        <div style={{ marginTop: 4 }}>{c.commentText}</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
              <Space.Compact style={{ width: '100%' }}>
                <Input
                  placeholder="Add a comment..."
                  value={newComment}
                  onChange={e => setNewComment(e.target.value)}
                  onPressEnter={handleAddComment}
                />
                <Button type="primary" loading={commentSubmitting} onClick={handleAddComment}>
                  Send
                </Button>
              </Space.Compact>
            </Card>

            {/* Actions */}
            <Card title="Actions" size="small">
              <Space wrap>
                <Input
                  placeholder="Reassign to (user ID)"
                  style={{ width: 220 }}
                  value={reassignTo}
                  onChange={e => setReassignTo(e.target.value)}
                  prefix={<UserOutlined />}
                />
                <Button
                  loading={actionLoading}
                  onClick={() => handleUpdateTicket({ assignedTo: reassignTo, status: 'ASSIGNED' })}
                  disabled={!reassignTo.trim()}
                >
                  Reassign
                </Button>
                <Select
                  value={escalationLevel}
                  onChange={setEscalationLevel}
                  style={{ width: 90 }}
                  options={[
                    { label: 'L1', value: 'L1' },
                    { label: 'L2', value: 'L2' },
                    { label: 'L3', value: 'L3' },
                  ]}
                />
                <Button
                  loading={actionLoading}
                  onClick={() => handleUpdateTicket({ escalationLevel })}
                  icon={<ArrowUpOutlined />}
                >
                  Escalate
                </Button>
                <Button
                  danger
                  loading={actionLoading}
                  onClick={() => handleUpdateTicket({ status: 'CLOSED', resolutionNotes: 'Force closed by admin' })}
                >
                  Force Close
                </Button>
              </Space>
            </Card>
          </div>
        )}
      </Modal>
    </>
  );
}

// =============================================================================
// SETTLEMENTS TAB
// =============================================================================

function SettlementsTab() {
  const [stats, setStats] = useState<SettlementStats>({ pending: 0, disputed: 0, overdue: 0, settled: 0 });
  const [settlements, setSettlements] = useState<Settlement[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);

  // Filters
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [overdueOnly, setOverdueOnly] = useState(false);

  // Detail modal
  const [selected, setSelected] = useState<Settlement | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [inspectionItems, setInspectionItems] = useState<InspectionItem[]>([]);
  const [deductions, setDeductions] = useState<Deduction[]>([]);
  const [timeline, setTimeline] = useState<SettlementEvent[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);

  // Dispute resolution
  const [disputeForm] = Form.useForm();
  const [disputeLoading, setDisputeLoading] = useState(false);

  // Override
  const [overrideNotes, setOverrideNotes] = useState('');
  const [overrideAmount, setOverrideAmount] = useState('');
  const [overrideLoading, setOverrideLoading] = useState(false);

  const loadStats = useCallback(async () => {
    try {
      const res = await axios.get(`${BASE}/admin/pg/settlements/stats`, { headers: authHeaders() });
      setStats(res.data);
    } catch { /* ignore */ }
  }, []);

  const loadSettlements = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = { page: String(page - 1), size: String(pageSize) };
      if (statusFilter) params.status = statusFilter;
      if (overdueOnly) params.overdue = 'true';
      const res = await axios.get(`${BASE}/admin/pg/settlements`, { headers: authHeaders(), params });
      setSettlements(res.data.content || []);
      setTotal(res.data.totalElements || 0);
    } catch {
      setSettlements([]);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, statusFilter, overdueOnly]);

  useEffect(() => { loadStats(); }, [loadStats]);
  useEffect(() => { loadSettlements(); }, [loadSettlements]);

  const openDetail = async (settlement: Settlement) => {
    setSelected(settlement);
    setModalOpen(true);
    setOverrideNotes('');
    setOverrideAmount('');
    disputeForm.resetFields();
    setDetailLoading(true);
    try {
      const [inspRes, dedRes, tlRes] = await Promise.all([
        axios.get(`${BASE}/admin/pg/settlements/${settlement.id}/inspection`, { headers: authHeaders() }).catch(() => ({ data: [] })),
        axios.get(`${BASE}/admin/pg/settlements/${settlement.id}/deductions`, { headers: authHeaders() }).catch(() => ({ data: [] })),
        axios.get(`${BASE}/admin/pg/settlements/${settlement.id}/timeline`, { headers: authHeaders() }).catch(() => ({ data: [] })),
      ]);
      setInspectionItems(Array.isArray(inspRes.data) ? inspRes.data : []);
      setDeductions(Array.isArray(dedRes.data) ? dedRes.data : []);
      setTimeline(Array.isArray(tlRes.data) ? tlRes.data : []);
    } catch { /* ignore */ }
    finally { setDetailLoading(false); }
  };

  const handleResolveDispute = async (deduction: Deduction) => {
    const values = disputeForm.getFieldsValue([
      `decision_${deduction.id}`, `adjusted_${deduction.id}`, `notes_${deduction.id}`,
    ]);
    const decision = values[`decision_${deduction.id}`];
    const adjustedPaise = values[`adjusted_${deduction.id}`] ? Number(values[`adjusted_${deduction.id}`]) * 100 : undefined;
    const notes = values[`notes_${deduction.id}`];

    if (!decision) { message.warning('Select a decision'); return; }
    if (decision === 'REDUCED' && !adjustedPaise) { message.warning('Enter adjusted amount for REDUCED'); return; }

    setDisputeLoading(true);
    try {
      await axios.post(
        `${BASE}/admin/pg/settlements/${selected!.id}/resolve-dispute`,
        { deductionId: deduction.id, decision, adjustedPaise, notes },
        { headers: authHeaders() },
      );
      message.success('Dispute resolved');
      // Reload deductions
      const dedRes = await axios.get(`${BASE}/admin/pg/settlements/${selected!.id}/deductions`, { headers: authHeaders() });
      setDeductions(Array.isArray(dedRes.data) ? dedRes.data : []);
      loadSettlements();
      loadStats();
    } catch {
      message.error('Failed to resolve dispute');
    } finally {
      setDisputeLoading(false);
    }
  };

  const handleOverride = async () => {
    if (!overrideNotes.trim()) { message.warning('Enter override notes'); return; }
    setOverrideLoading(true);
    try {
      await axios.post(
        `${BASE}/admin/pg/settlements/${selected!.id}/override`,
        {
          notes: overrideNotes.trim(),
          overrideRefundPaise: overrideAmount ? Number(overrideAmount) * 100 : undefined,
        },
        { headers: authHeaders() },
      );
      message.success('Settlement overridden');
      setModalOpen(false);
      loadSettlements();
      loadStats();
    } catch {
      message.error('Failed to override settlement');
    } finally {
      setOverrideLoading(false);
    }
  };

  const columns = [
    {
      title: 'Ref#', dataIndex: 'refNumber', key: 'refNumber', width: 130,
      render: (v: string) => <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{v}</span>,
    },
    {
      title: 'Tenant ID', dataIndex: 'tenantId', key: 'tenantId', width: 120,
      render: (v: string) => <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{v?.substring(0, 8)}...</span>,
    },
    {
      title: 'Move Out', dataIndex: 'moveOutDate', key: 'moveOutDate', width: 110,
      render: (v: string) => formatDate(v),
    },
    {
      title: 'Deposit', dataIndex: 'depositPaise', key: 'deposit', width: 110,
      render: (v: number) => formatPaise(v),
    },
    {
      title: 'Deductions', dataIndex: 'totalDeductionsPaise', key: 'deductions', width: 110,
      render: (v: number) => v > 0 ? <span style={{ color: '#cf1322' }}>{formatPaise(v)}</span> : '—',
    },
    {
      title: 'Refund', dataIndex: 'refundPaise', key: 'refund', width: 110,
      render: (v: number) => <span style={{ color: '#389e0d', fontWeight: 600 }}>{formatPaise(v)}</span>,
    },
    {
      title: 'Deadline', dataIndex: 'deadline', key: 'deadline', width: 110,
      render: (v: string) => formatDate(v),
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status', width: 140,
      render: (v: string) => <Tag color={SETTLEMENT_STATUS_COLOR[v] || 'default'}>{v?.replace(/_/g, ' ')}</Tag>,
    },
    {
      title: 'Overdue', dataIndex: 'overdue', key: 'overdue', width: 80,
      render: (v: boolean) => v
        ? <Badge status="error" text="Yes" />
        : <Badge status="default" text="No" />,
    },
    {
      title: 'Actions', key: 'actions', width: 80,
      render: (_: unknown, record: Settlement) => (
        <Button type="link" size="small" onClick={() => openDetail(record)}>View</Button>
      ),
    },
  ];

  const deductionColumns = [
    { title: 'Category', dataIndex: 'category', key: 'category', width: 120 },
    { title: 'Description', dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: 'Amount', dataIndex: 'amountPaise', key: 'amount', width: 100,
      render: (v: number) => formatPaise(v),
    },
    {
      title: 'Disputed', dataIndex: 'disputed', key: 'disputed', width: 90,
      render: (v: boolean) => v ? <Tag color="red">Yes</Tag> : <Tag>No</Tag>,
    },
    {
      title: 'Decision', dataIndex: 'decision', key: 'decision', width: 100,
      render: (v: string | null) => v ? <Tag color="purple">{v}</Tag> : '—',
    },
    {
      title: 'Adjusted', dataIndex: 'adjustedPaise', key: 'adjusted', width: 100,
      render: (v: number | null) => v != null ? formatPaise(v) : '—',
    },
  ];

  return (
    <>
      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="Pending" value={stats.pending} prefix={<ClockCircleOutlined />} valueStyle={{ color: '#1677ff' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Disputed" value={stats.disputed} prefix={<ExclamationCircleOutlined />} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Overdue" value={stats.overdue} prefix={<WarningOutlined />} valueStyle={{ color: '#fa8c16' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Settled" value={stats.settled} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#389e0d' }} /></Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            placeholder="Filter by Status"
            style={{ width: 200 }}
            value={statusFilter}
            onChange={v => { setStatusFilter(v); setPage(1); }}
            allowClear
            options={[
              'INITIATED', 'INSPECTION_DONE', 'APPROVED', 'REFUND_PROCESSING',
              'SETTLED', 'DISPUTED', 'ADMIN_RESOLVED',
            ].map(s => ({ label: s.replace(/_/g, ' '), value: s }))}
          />
          <Checkbox checked={overdueOnly} onChange={e => { setOverdueOnly(e.target.checked); setPage(1); }}>
            Overdue Only
          </Checkbox>
        </Space>
      </Card>

      {/* Table */}
      <Table
        dataSource={settlements}
        columns={columns}
        rowKey="id"
        loading={loading}
        size="small"
        pagination={{
          current: page, pageSize, total, showTotal: t => `${t} settlements`,
          onChange: p => setPage(p),
        }}
        onRow={record => ({ onClick: () => openDetail(record), style: { cursor: 'pointer' } })}
      />

      {/* Detail Modal */}
      <Modal
        title={selected ? `Settlement ${selected.refNumber}` : 'Settlement Detail'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        width={860}
        destroyOnClose
      >
        {selected && (
          <Spin spinning={detailLoading}>
            <div style={{ maxHeight: '75vh', overflowY: 'auto' }}>
              {/* Settlement Info */}
              <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
                <Descriptions.Item label="Ref#">{selected.refNumber}</Descriptions.Item>
                <Descriptions.Item label="Status">
                  <Tag color={SETTLEMENT_STATUS_COLOR[selected.status]}>{selected.status?.replace(/_/g, ' ')}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Tenant ID">
                  <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{selected.tenantId}</span>
                </Descriptions.Item>
                <Descriptions.Item label="Tenancy ID">
                  <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{selected.tenancyId}</span>
                </Descriptions.Item>
                <Descriptions.Item label="Move Out">{formatDate(selected.moveOutDate)}</Descriptions.Item>
                <Descriptions.Item label="Deadline">{formatDate(selected.deadline)}</Descriptions.Item>
                <Descriptions.Item label="Deposit">{formatPaise(selected.depositPaise)}</Descriptions.Item>
                <Descriptions.Item label="Total Deductions">
                  <span style={{ color: '#cf1322' }}>{formatPaise(selected.totalDeductionsPaise)}</span>
                </Descriptions.Item>
                <Descriptions.Item label="Refund">
                  <span style={{ color: '#389e0d', fontWeight: 600 }}>{formatPaise(selected.refundPaise)}</span>
                </Descriptions.Item>
                <Descriptions.Item label="Overdue">
                  {selected.overdue ? <Badge status="error" text="Yes" /> : <Badge status="default" text="No" />}
                </Descriptions.Item>
                <Descriptions.Item label="Inspection Date">{formatDate(selected.inspectionDate)}</Descriptions.Item>
                <Descriptions.Item label="Inspector Notes">{selected.inspectorNotes || '—'}</Descriptions.Item>
                {selected.overrideRefundPaise != null && (
                  <Descriptions.Item label="Override Refund" span={2}>
                    <span style={{ color: '#722ed1', fontWeight: 600 }}>{formatPaise(selected.overrideRefundPaise)}</span>
                  </Descriptions.Item>
                )}
                {selected.adminNotes && (
                  <Descriptions.Item label="Admin Notes" span={2}>{selected.adminNotes}</Descriptions.Item>
                )}
              </Descriptions>

              {/* Inspection Checklist */}
              {inspectionItems.length > 0 && (
                <Card title="Inspection Checklist" size="small" style={{ marginBottom: 16 }}>
                  <Table
                    dataSource={inspectionItems}
                    rowKey="id"
                    size="small"
                    pagination={false}
                    columns={[
                      { title: 'Item', dataIndex: 'item', key: 'item' },
                      {
                        title: 'Condition', dataIndex: 'condition', key: 'condition', width: 100,
                        render: (v: string) => (
                          <Tag color={v === 'GOOD' ? 'green' : v === 'DAMAGED' ? 'red' : 'orange'}>{v}</Tag>
                        ),
                      },
                      {
                        title: 'Damage Cost', dataIndex: 'damagePaise', key: 'damage', width: 110,
                        render: (v: number) => v > 0 ? formatPaise(v) : '—',
                      },
                      { title: 'Notes', dataIndex: 'notes', key: 'notes', ellipsis: true },
                    ]}
                  />
                </Card>
              )}

              {/* Deductions */}
              <Card title="Deductions" size="small" style={{ marginBottom: 16 }}>
                <Table
                  dataSource={deductions}
                  columns={deductionColumns}
                  rowKey="id"
                  size="small"
                  pagination={false}
                />
              </Card>

              {/* Dispute Resolution */}
              {deductions.some(d => d.disputed && !d.decision) && (
                <Card
                  title={<><ExclamationCircleOutlined style={{ color: '#cf1322' }} /> Dispute Resolution</>}
                  size="small"
                  style={{ marginBottom: 16 }}
                >
                  <Form form={disputeForm} layout="vertical">
                    {deductions.filter(d => d.disputed && !d.decision).map(d => (
                      <Card
                        key={d.id}
                        size="small"
                        style={{ marginBottom: 12, background: '#fff7e6', border: '1px solid #ffd591' }}
                      >
                        <div style={{ marginBottom: 8 }}>
                          <strong>{d.category}</strong>: {d.description} — {formatPaise(d.amountPaise)}
                        </div>
                        {d.disputeReason && (
                          <div style={{ marginBottom: 8, color: '#cf1322', fontSize: 13 }}>
                            Dispute reason: {d.disputeReason}
                          </div>
                        )}
                        <Row gutter={12}>
                          <Col span={6}>
                            <Form.Item name={`decision_${d.id}`} label="Decision" style={{ marginBottom: 0 }}>
                              <Select
                                placeholder="Select"
                                options={[
                                  { label: 'Upheld', value: 'UPHELD' },
                                  { label: 'Reduced', value: 'REDUCED' },
                                  { label: 'Removed', value: 'REMOVED' },
                                ]}
                              />
                            </Form.Item>
                          </Col>
                          <Col span={6}>
                            <Form.Item name={`adjusted_${d.id}`} label="Adjusted (INR)" style={{ marginBottom: 0 }}>
                              <Input type="number" placeholder="Amount" prefix={<DollarOutlined />} />
                            </Form.Item>
                          </Col>
                          <Col span={8}>
                            <Form.Item name={`notes_${d.id}`} label="Admin Notes" style={{ marginBottom: 0 }}>
                              <Input.TextArea rows={1} placeholder="Notes..." />
                            </Form.Item>
                          </Col>
                          <Col span={4} style={{ display: 'flex', alignItems: 'flex-end' }}>
                            <Button
                              type="primary"
                              size="small"
                              loading={disputeLoading}
                              onClick={() => handleResolveDispute(d)}
                              style={{ marginBottom: 0 }}
                            >
                              Resolve
                            </Button>
                          </Col>
                        </Row>
                      </Card>
                    ))}
                  </Form>
                </Card>
              )}

              {/* Override Section */}
              <Card title="Admin Override" size="small" style={{ marginBottom: 16 }}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Input.TextArea
                    rows={2}
                    placeholder="Override notes (required)..."
                    value={overrideNotes}
                    onChange={e => setOverrideNotes(e.target.value)}
                  />
                  <Space>
                    <Input
                      type="number"
                      placeholder="Override refund amount (INR, optional)"
                      style={{ width: 280 }}
                      value={overrideAmount}
                      onChange={e => setOverrideAmount(e.target.value)}
                      prefix={<DollarOutlined />}
                    />
                    <Button
                      type="primary"
                      danger
                      loading={overrideLoading}
                      onClick={handleOverride}
                      disabled={!overrideNotes.trim()}
                    >
                      Override Settlement
                    </Button>
                  </Space>
                </Space>
              </Card>

              {/* Timeline */}
              {timeline.length > 0 && (
                <Card title="Settlement Timeline" size="small">
                  <Timeline
                    items={timeline.map(evt => ({
                      color: evt.event.includes('DISPUTE') ? 'red'
                        : evt.event.includes('SETTLED') || evt.event.includes('APPROVED') ? 'green'
                        : evt.event.includes('OVERRIDE') ? 'purple'
                        : 'blue',
                      children: (
                        <div>
                          <strong>{evt.event.replace(/_/g, ' ')}</strong>
                          <div style={{ fontSize: 12, color: '#666' }}>
                            {formatDateTime(evt.timestamp)} &middot; {evt.actor}
                          </div>
                          {evt.notes && <div style={{ fontSize: 12, marginTop: 2 }}>{evt.notes}</div>}
                        </div>
                      ),
                    }))}
                  />
                </Card>
              )}
            </div>
          </Spin>
        )}
      </Modal>
    </>
  );
}

// =============================================================================
// MAIN PAGE
// =============================================================================

export default function PgManagementPage() {
  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>PG Management</h2>
      <Tabs
        defaultActiveKey="tickets"
        items={[
          {
            key: 'tickets',
            label: 'Tickets',
            children: <TicketsTab />,
          },
          {
            key: 'settlements',
            label: 'Settlements',
            children: <SettlementsTab />,
          },
        ]}
      />
    </div>
  );
}
