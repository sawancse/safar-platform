import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Card, Row, Col, Statistic, Select, Input, Button,
  Modal, message, Descriptions, Tabs, Form, InputNumber, DatePicker,
} from 'antd';
import {
  FormatPainterOutlined, SearchOutlined, CheckCircleOutlined,
  UserAddOutlined, ProjectOutlined, PlusOutlined, CheckOutlined,
  ToolOutlined, ExperimentOutlined, AppstoreOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title } = Typography;

const INR = (paise: number) => `₹${(paise / 100).toLocaleString('en-IN')}`;

const statusColor: Record<string, string> = {
  CONSULTATION_BOOKED: 'blue', MEASUREMENT_DONE: 'cyan', DESIGN_IN_PROGRESS: 'geekblue',
  DESIGN_APPROVED: 'purple', MATERIAL_SELECTED: 'gold', QUOTE_APPROVED: 'lime',
  EXECUTION: 'orange', QC_IN_PROGRESS: 'volcano',
  COMPLETED: 'green', CANCELLED: 'red',
};

const projectTypes = [
  'FULL_HOME', 'MODULAR_KITCHEN', 'WARDROBE', 'FULL_ROOM', 'RENOVATION',
];

const ALL_STATUSES = [
  'CONSULTATION_BOOKED', 'MEASUREMENT_DONE', 'DESIGN_IN_PROGRESS', 'DESIGN_APPROVED',
  'MATERIAL_SELECTED', 'QUOTE_APPROVED', 'EXECUTION', 'QC_IN_PROGRESS', 'COMPLETED', 'CANCELLED',
];

const STATUS_FLOW = ALL_STATUSES.filter(s => s !== 'CANCELLED');

const milestoneStatusColor: Record<string, string> = {
  PENDING: 'default', IN_PROGRESS: 'blue', COMPLETED: 'green', DELAYED: 'red',
};

const qcStatusColor: Record<string, string> = {
  PASS: 'green', FAIL: 'red', REWORK: 'orange', PENDING: 'default',
};

const roomTypes = ['LIVING_ROOM', 'BEDROOM', 'KITCHEN', 'BATHROOM', 'DINING', 'STUDY', 'KIDS_ROOM', 'BALCONY', 'POOJA_ROOM', 'FOYER'];
const designStyles = ['MODERN', 'CONTEMPORARY', 'MINIMALIST', 'TRADITIONAL', 'INDUSTRIAL', 'SCANDINAVIAN', 'BOHEMIAN', 'CLASSIC'];

export default function InteriorsPage() {
  const token = localStorage.getItem('admin_token') ?? '';

  const [projects, setProjects] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<any>(null);
  const [detailTab, setDetailTab] = useState('overview');
  const [milestones, setMilestones] = useState<any[]>([]);
  const [qualityChecks, setQualityChecks] = useState<any[]>([]);
  const [rooms, setRooms] = useState<any[]>([]);
  const [materials, setMaterials] = useState<any[]>([]);
  const [designers, setDesigners] = useState<any[]>([]);
  const [assignModal, setAssignModal] = useState<{ open: boolean; projectId: string }>({ open: false, projectId: '' });
  const [selectedDesigner, setSelectedDesigner] = useState('');

  // Add modals
  const [milestoneModal, setMilestoneModal] = useState(false);
  const [roomModal, setRoomModal] = useState(false);
  const [qcModal, setQcModal] = useState(false);
  const [materialModal, setMaterialModal] = useState(false);

  // Forms
  const [milestoneForm] = Form.useForm();
  const [roomForm] = Form.useForm();
  const [qcForm] = Form.useForm();
  const [materialForm] = Form.useForm();

  // Filters
  const [status, setStatus] = useState('');
  const [projectType, setProjectType] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);

  const load = () => {
    setLoading(true);
    const params: any = { page, size: pageSize };
    if (status) params.status = status;
    if (projectType) params.projectType = projectType;
    if (search) params.search = search;

    adminApi.getInteriorProjects(token, params)
      .then(({ data }) => {
        const items = data.content || data || [];
        setProjects(Array.isArray(items) ? items : []);
      })
      .catch(() => setProjects([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [page, pageSize, status, projectType]);
  useEffect(() => {
    adminApi.getDesigners(token)
      .then(({ data }) => setDesigners(Array.isArray(data) ? data : data.content || []))
      .catch(() => setDesigners([]));
  }, []);

  const handleSearch = () => { setPage(0); load(); };

  const loadDetailData = async (record: any) => {
    try {
      const [ms, qc, rm, mat] = await Promise.all([
        adminApi.getInteriorMilestones(record.id, token).then(r => r.data).catch(() => []),
        adminApi.getQualityChecks(record.id, token).then(r => r.data).catch(() => []),
        adminApi.getRoomDesigns(record.id, token).then(r => r.data).catch(() => []),
        adminApi.getMaterials(record.id, token).then(r => r.data).catch(() => []),
      ]);
      setMilestones(Array.isArray(ms) ? ms : ms?.content || []);
      setQualityChecks(Array.isArray(qc) ? qc : qc?.content || []);
      setRooms(Array.isArray(rm) ? rm : rm?.content || []);
      setMaterials(Array.isArray(mat) ? mat : mat?.content || []);
    } catch {
      setMilestones([]); setQualityChecks([]); setRooms([]); setMaterials([]);
    }
  };

  const openDetail = async (record: any) => {
    setDetail(record);
    setDetailTab('overview');
    await loadDetailData(record);
  };

  const refreshDetail = async () => {
    if (!detail) return;
    try {
      const { data } = await adminApi.getInteriorProject(detail.id, token);
      setDetail(data);
      await loadDetailData(data);
      load();
    } catch { /* keep current */ }
  };

  const handleStatusUpdate = async (id: string, newStatus: string) => {
    try {
      await adminApi.updateInteriorStatus(id, newStatus, token);
      message.success(`Status updated to ${newStatus.replace(/_/g, ' ')}`);
      await refreshDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Status update failed');
    }
  };

  const handleAssignDesigner = async () => {
    if (!selectedDesigner) { message.warning('Select a designer'); return; }
    try {
      await adminApi.assignDesigner(assignModal.projectId, selectedDesigner, token);
      message.success('Designer assigned');
      setAssignModal({ open: false, projectId: '' });
      setSelectedDesigner('');
      await refreshDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Assignment failed');
    }
  };

  const handleAddMilestone = async () => {
    try {
      const vals = await milestoneForm.validateFields();
      await adminApi.addInteriorMilestone(detail.id, {
        name: vals.name,
        description: vals.description,
        scheduledDate: vals.scheduledDate.format('YYYY-MM-DD'),
        paymentAmountPaise: vals.paymentAmount ? vals.paymentAmount * 100 : undefined,
      }, token);
      message.success('Milestone added');
      setMilestoneModal(false);
      milestoneForm.resetFields();
      await refreshDetail();
    } catch (e: any) {
      if (e?.response) message.error(e.response.data?.detail || 'Failed to add milestone');
    }
  };

  const handleCompleteMilestone = async (milestoneId: string) => {
    try {
      await adminApi.completeMilestone(milestoneId, token);
      message.success('Milestone completed');
      await refreshDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Failed to complete milestone');
    }
  };

  const handleAddRoom = async () => {
    try {
      const vals = await roomForm.validateFields();
      await adminApi.addRoomDesign(detail.id, {
        roomType: vals.roomType,
        areaSqft: vals.areaSqft,
        designStyle: vals.designStyle,
      }, token);
      message.success('Room design added');
      setRoomModal(false);
      roomForm.resetFields();
      await refreshDetail();
    } catch (e: any) {
      if (e?.response) message.error(e.response.data?.detail || 'Failed to add room');
    }
  };

  const handleApproveRoom = async (roomId: string) => {
    try {
      await adminApi.approveRoomDesign(detail.id, roomId, token);
      message.success('Room design approved');
      await refreshDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Failed to approve room');
    }
  };

  const handleGenerateQuote = async () => {
    try {
      await adminApi.generateInteriorQuote(detail.id, token);
      message.success('Quote generated');
      await refreshDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.detail || 'Failed to generate quote');
    }
  };

  const handleAddQc = async () => {
    try {
      const vals = await qcForm.validateFields();
      await adminApi.addQualityCheck(detail.id, {
        checkpointName: vals.checkpointName,
        category: vals.category,
        status: vals.status,
        notes: vals.notes,
      }, token);
      message.success('Quality check added');
      setQcModal(false);
      qcForm.resetFields();
      await refreshDetail();
    } catch (e: any) {
      if (e?.response) message.error(e.response.data?.detail || 'Failed to add QC');
    }
  };

  const handleAddMaterial = async () => {
    try {
      const vals = await materialForm.validateFields();
      await adminApi.addMaterialSelection(detail.id, {
        category: vals.category,
        materialName: vals.materialName,
        brand: vals.brand,
        quantity: vals.quantity,
        unit: vals.unit,
        unitPricePaise: vals.unitPrice ? vals.unitPrice * 100 : 0,
        roomDesignId: vals.roomDesignId,
      }, token);
      message.success('Material added');
      setMaterialModal(false);
      materialForm.resetFields();
      await refreshDetail();
    } catch (e: any) {
      if (e?.response) message.error(e.response.data?.detail || 'Failed to add material');
    }
  };

  // Stats
  const total = projects.length;
  const consultation = projects.filter(p => p.status === 'CONSULTATION_BOOKED').length;
  const designPhase = projects.filter(p => ['DESIGN_IN_PROGRESS', 'DESIGN_APPROVED', 'MEASUREMENT_DONE'].includes(p.status)).length;
  const execution = projects.filter(p => ['EXECUTION', 'QC_IN_PROGRESS'].includes(p.status)).length;
  const completed = projects.filter(p => p.status === 'COMPLETED').length;
  const totalQuoted = projects.reduce((sum, p) => sum + (p.quotedAmountPaise || 0), 0);

  const columns: ColumnsType<any> = [
    {
      title: 'ID', dataIndex: 'id', width: 100, ellipsis: true,
      render: (id, r) => (
        <a onClick={() => openDetail(r)} style={{ fontFamily: 'monospace' }}>{id?.substring(0, 8)}</a>
      ),
    },
    {
      title: 'Type', dataIndex: 'projectType', width: 130,
      render: (t) => <Tag>{t?.replace(/_/g, ' ') || '-'}</Tag>,
    },
    {
      title: 'City', dataIndex: 'city', width: 120,
      render: (v) => v || '-',
    },
    {
      title: 'Status', dataIndex: 'status', width: 150,
      render: (s) => <Tag color={statusColor[s] ?? 'default'}>{s?.replace(/_/g, ' ')}</Tag>,
    },
    {
      title: 'Designer', dataIndex: 'designerName', width: 140,
      render: (v) => v || '-',
    },
    {
      title: 'Rooms', dataIndex: 'roomDesignsCount', width: 70, align: 'center',
      render: (v) => v ?? '-',
    },
    {
      title: 'Budget', width: 160,
      render: (_, r) => {
        const min = r.budgetMinPaise;
        const max = r.budgetMaxPaise;
        if (min && max) return `${INR(min)} - ${INR(max)}`;
        if (min) return `From ${INR(min)}`;
        return '-';
      },
    },
    {
      title: 'Quoted', dataIndex: 'quotedAmountPaise', width: 120,
      render: (v) => v ? INR(v) : '-',
      sorter: (a, b) => (a.quotedAmountPaise || 0) - (b.quotedAmountPaise || 0),
    },
    {
      title: 'Created', dataIndex: 'createdAt', width: 110,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '-',
    },
    {
      title: 'Actions', width: 220, fixed: 'right',
      render: (_, r) => {
        const idx = STATUS_FLOW.indexOf(r.status);
        const next = idx >= 0 && idx < STATUS_FLOW.length - 1 ? STATUS_FLOW[idx + 1] : null;
        return (
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            {!r.designerName && (
              <Button size="small" icon={<UserAddOutlined />}
                onClick={() => { setAssignModal({ open: true, projectId: r.id }); setSelectedDesigner(''); }}>
                Designer
              </Button>
            )}
            {next && (
              <Button size="small" type="primary"
                onClick={() => handleStatusUpdate(r.id, next)}>
                {next.replace(/_/g, ' ')}
              </Button>
            )}
          </div>
        );
      },
    },
  ];

  const milestoneColumns: ColumnsType<any> = [
    { title: 'Milestone', dataIndex: 'title', width: 200, render: (v, r) => v || r.name || '-' },
    {
      title: 'Scheduled', dataIndex: 'scheduledDate', width: 120,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '-',
    },
    {
      title: 'Completed', dataIndex: 'completedDate', width: 120,
      render: (d) => d ? new Date(d).toLocaleDateString('en-IN') : '-',
    },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (s) => <Tag color={milestoneStatusColor[s] ?? 'default'}>{s || 'PENDING'}</Tag>,
    },
    {
      title: 'Payment', dataIndex: 'paymentAmountPaise', width: 110,
      render: (v) => v ? INR(v) : '-',
    },
    {
      title: 'Actions', width: 100,
      render: (_, r) => r.status !== 'COMPLETED' ? (
        <Button size="small" icon={<CheckOutlined />} onClick={() => handleCompleteMilestone(r.id)}>Done</Button>
      ) : null,
    },
  ];

  const roomColumns: ColumnsType<any> = [
    { title: 'Room Type', dataIndex: 'roomType', width: 140, render: (v) => v?.replace(/_/g, ' ') || '-' },
    { title: 'Area (sqft)', dataIndex: 'areaSqft', width: 100, render: (v) => v || '-' },
    { title: 'Design Style', dataIndex: 'designStyle', width: 130, render: (v) => v?.replace(/_/g, ' ') || '-' },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (s) => <Tag color={s === 'APPROVED' ? 'green' : s === 'REVISION_REQUESTED' ? 'orange' : 'default'}>{s || 'PENDING'}</Tag>,
    },
    {
      title: 'Est. Cost', dataIndex: 'estimatedCostPaise', width: 110,
      render: (v) => v ? INR(v) : '-',
    },
    {
      title: 'Actions', width: 100,
      render: (_, r) => r.status !== 'APPROVED' ? (
        <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => handleApproveRoom(r.id)}>Approve</Button>
      ) : <Tag color="green">Approved</Tag>,
    },
  ];

  const materialColumns: ColumnsType<any> = [
    { title: 'Category', dataIndex: 'category', width: 120 },
    { title: 'Material', dataIndex: 'materialName', width: 160 },
    { title: 'Brand', dataIndex: 'brand', width: 120, render: (v) => v || '-' },
    { title: 'Qty', dataIndex: 'quantity', width: 70, align: 'center' },
    { title: 'Unit Price', dataIndex: 'unitPricePaise', width: 110, render: (v) => v ? INR(v) : '-' },
    { title: 'Total', width: 110, render: (_, r) => r.unitPricePaise && r.quantity ? INR(r.unitPricePaise * r.quantity) : '-' },
  ];

  const qcColumns: ColumnsType<any> = [
    { title: 'Checkpoint', dataIndex: 'checkType', width: 200, render: (v, r) => v || r.checkpointName || '-' },
    { title: 'Category', dataIndex: 'category', width: 120, render: (v) => v || '-' },
    { title: 'Findings', dataIndex: 'findings', width: 200, ellipsis: true },
    {
      title: 'Status', dataIndex: 'status', width: 100,
      render: (s) => <Tag color={qcStatusColor[s] ?? 'default'}>{s || 'PENDING'}</Tag>,
    },
    { title: 'Notes', dataIndex: 'notes', ellipsis: true },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>
        <FormatPainterOutlined style={{ marginRight: 8 }} />Interiors
      </Title>

      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 20 }}>
        <Col span={4}>
          <Card size="small"><Statistic title="Total Projects" value={total} prefix={<ProjectOutlined />} /></Card>
        </Col>
        <Col span={4}>
          <Card size="small"><Statistic title="Consultation" value={consultation} valueStyle={{ color: '#1677ff' }} /></Card>
        </Col>
        <Col span={4}>
          <Card size="small"><Statistic title="Design Phase" value={designPhase} valueStyle={{ color: '#722ed1' }} /></Card>
        </Col>
        <Col span={4}>
          <Card size="small"><Statistic title="Execution" value={execution} valueStyle={{ color: '#fa8c16' }} /></Card>
        </Col>
        <Col span={4}>
          <Card size="small"><Statistic title="Completed" value={completed} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col span={4}>
          <Card size="small"><Statistic title="Total Quoted" value={totalQuoted ? INR(totalQuoted) : '₹0'} /></Card>
        </Col>
      </Row>

      {/* Filters */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select placeholder="Status" value={status || undefined}
          onChange={v => { setStatus(v || ''); setPage(0); }}
          allowClear style={{ width: 180 }}>
          {ALL_STATUSES.map(s => <Select.Option key={s} value={s}>{s.replace(/_/g, ' ')}</Select.Option>)}
        </Select>
        <Select placeholder="Project Type" value={projectType || undefined}
          onChange={v => { setProjectType(v || ''); setPage(0); }}
          allowClear style={{ width: 170 }}>
          {projectTypes.map(t => <Select.Option key={t} value={t}>{t.replace(/_/g, ' ')}</Select.Option>)}
        </Select>
        <Input prefix={<SearchOutlined />} placeholder="Search by city, designer..."
          value={search} onChange={e => setSearch(e.target.value)}
          onPressEnter={handleSearch} style={{ width: 240 }} allowClear />
        <Button type="primary" onClick={handleSearch}>Search</Button>
      </div>

      {/* Table */}
      <Table
        columns={columns}
        dataSource={projects}
        rowKey="id"
        loading={loading}
        scroll={{ x: 1400 }}
        pagination={{
          current: page + 1, pageSize, total: projects.length,
          onChange: (p, ps) => { setPage(p - 1); setPageSize(ps); },
          showSizeChanger: true, showTotal: (t) => `${t} projects`,
        }}
        locale={{ emptyText: 'No interior projects found' }}
      />

      {/* Assign Designer modal */}
      <Modal
        open={assignModal.open}
        title="Assign Designer"
        onCancel={() => { setAssignModal({ open: false, projectId: '' }); setSelectedDesigner(''); }}
        onOk={handleAssignDesigner}
      >
        <div style={{ marginBottom: 8 }}>Select a designer to assign to this project:</div>
        <Select
          placeholder="Select designer"
          value={selectedDesigner || undefined}
          onChange={setSelectedDesigner}
          style={{ width: '100%' }}
        >
          {designers.map((d: any) => (
            <Select.Option key={d.id} value={d.id}>
              {d.fullName || d.id?.substring(0, 8)} — {d.specializations?.join(', ') || 'General'}
            </Select.Option>
          ))}
        </Select>
      </Modal>

      {/* Detail modal with tabs */}
      <Modal
        open={!!detail}
        onCancel={() => setDetail(null)}
        width={900}
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span>Interior Project {detail?.id?.substring(0, 8) || ''}</span>
            {detail && <Tag color={statusColor[detail.status]}>{detail.status?.replace(/_/g, ' ')}</Tag>}
          </div>
        }
        footer={
          detail ? (
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {!detail.designerName && (
                <Button icon={<UserAddOutlined />}
                  onClick={() => { setAssignModal({ open: true, projectId: detail.id }); setSelectedDesigner(''); }}>
                  Assign Designer
                </Button>
              )}
              {(() => {
                const idx = STATUS_FLOW.indexOf(detail.status);
                const next = idx >= 0 && idx < STATUS_FLOW.length - 1 ? STATUS_FLOW[idx + 1] : null;
                return next ? (
                  <Button type="primary" onClick={() => handleStatusUpdate(detail.id, next)}>
                    Move to {next.replace(/_/g, ' ')}
                  </Button>
                ) : null;
              })()}
              {detail.status === 'DESIGN_APPROVED' && (
                <Button icon={<AppstoreOutlined />} onClick={handleGenerateQuote}>Generate Quote</Button>
              )}
              {detail.status !== 'COMPLETED' && detail.status !== 'CANCELLED' && (
                <Button danger onClick={() => handleStatusUpdate(detail.id, 'CANCELLED')}>Cancel Project</Button>
              )}
              <Button onClick={() => setDetail(null)}>Close</Button>
            </div>
          ) : null
        }
      >
        {detail && (
          <Tabs activeKey={detailTab} onChange={setDetailTab} items={[
            {
              key: 'overview',
              label: 'Overview',
              children: (
                <Descriptions column={2} bordered size="small">
                  <Descriptions.Item label="Status">
                    <Tag color={statusColor[detail.status]}>{detail.status?.replace(/_/g, ' ')}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="Project Type">{detail.projectType?.replace(/_/g, ' ') || '-'}</Descriptions.Item>
                  <Descriptions.Item label="City">{detail.city || '-'}</Descriptions.Item>
                  <Descriptions.Item label="Rooms">{detail.roomDesignsCount ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="Address" span={2}>{detail.propertyAddress || '-'}</Descriptions.Item>
                  <Descriptions.Item label="Designer">{detail.designerName || <Tag color="warning">Not Assigned</Tag>}</Descriptions.Item>
                  <Descriptions.Item label="Designer Phone">{detail.designerPhone || '-'}</Descriptions.Item>
                  <Descriptions.Item label="Designer Email">{detail.designerEmail || '-'}</Descriptions.Item>
                  <Descriptions.Item label="Budget Min">{detail.budgetMinPaise ? INR(detail.budgetMinPaise) : '-'}</Descriptions.Item>
                  <Descriptions.Item label="Budget Max">{detail.budgetMaxPaise ? INR(detail.budgetMaxPaise) : '-'}</Descriptions.Item>
                  <Descriptions.Item label="Quoted Amount">{detail.quotedAmountPaise ? INR(detail.quotedAmountPaise) : '-'}</Descriptions.Item>
                  <Descriptions.Item label="Approved">{detail.approvedAmountPaise ? INR(detail.approvedAmountPaise) : '-'}</Descriptions.Item>
                  <Descriptions.Item label="Paid">{detail.paidAmountPaise ? INR(detail.paidAmountPaise) : '-'}</Descriptions.Item>
                  <Descriptions.Item label="Consultation Date">
                    {detail.consultationDate ? new Date(detail.consultationDate).toLocaleDateString('en-IN') : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Expected Start">
                    {detail.expectedStartDate ? new Date(detail.expectedStartDate).toLocaleDateString('en-IN') : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Expected Completion">
                    {detail.expectedEndDate ? new Date(detail.expectedEndDate).toLocaleDateString('en-IN') : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Actual Start">
                    {detail.actualStartDate ? new Date(detail.actualStartDate).toLocaleDateString('en-IN') : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Actual Completion">
                    {detail.actualEndDate ? new Date(detail.actualEndDate).toLocaleDateString('en-IN') : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="User ID" span={2}>
                    <span style={{ fontFamily: 'monospace', fontSize: 12 }}>{detail.userId}</span>
                  </Descriptions.Item>
                  <Descriptions.Item label="Created" span={2}>
                    {detail.createdAt ? new Date(detail.createdAt).toLocaleString('en-IN') : '-'}
                  </Descriptions.Item>
                  {detail.notes && (
                    <Descriptions.Item label="Notes" span={2}>{detail.notes}</Descriptions.Item>
                  )}
                </Descriptions>
              ),
            },
            {
              key: 'rooms',
              label: `Room Designs (${rooms.length})`,
              children: (
                <>
                  <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#666', fontSize: 13 }}>Add room designs, then approve when ready</span>
                    <Button size="small" icon={<PlusOutlined />} onClick={() => { roomForm.resetFields(); setRoomModal(true); }}>
                      Add Room
                    </Button>
                  </div>
                  <Table
                    columns={roomColumns}
                    dataSource={rooms}
                    rowKey={(r: any, i) => r.id || String(i)}
                    pagination={false}
                    size="small"
                    locale={{ emptyText: 'No room designs yet. Add rooms to start designing.' }}
                  />
                </>
              ),
            },
            {
              key: 'materials',
              label: `Materials (${materials.length})`,
              children: (
                <>
                  <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#666', fontSize: 13 }}>Select materials for each room</span>
                    <Button size="small" icon={<PlusOutlined />} onClick={() => { materialForm.resetFields(); setMaterialModal(true); }}>
                      Add Material
                    </Button>
                  </div>
                  <Table
                    columns={materialColumns}
                    dataSource={materials}
                    rowKey={(r: any, i) => r.id || String(i)}
                    pagination={false}
                    size="small"
                    locale={{ emptyText: 'No materials selected yet' }}
                  />
                </>
              ),
            },
            {
              key: 'milestones',
              label: `Milestones (${milestones.length})`,
              children: (
                <>
                  <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#666', fontSize: 13 }}>Track project milestones and linked payments</span>
                    <Button size="small" icon={<PlusOutlined />} onClick={() => { milestoneForm.resetFields(); setMilestoneModal(true); }}>
                      Add Milestone
                    </Button>
                  </div>
                  <Table
                    columns={milestoneColumns}
                    dataSource={milestones}
                    rowKey={(r: any, i) => r.id || String(i)}
                    pagination={false}
                    size="small"
                    locale={{ emptyText: 'No milestones yet. Add milestones for execution phase.' }}
                  />
                </>
              ),
            },
            {
              key: 'quality',
              label: `Quality Checks (${qualityChecks.length})`,
              children: (
                <>
                  <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#666', fontSize: 13 }}>QC inspections during execution</span>
                    <Button size="small" icon={<PlusOutlined />} onClick={() => { qcForm.resetFields(); setQcModal(true); }}>
                      Add QC Check
                    </Button>
                  </div>
                  <Table
                    columns={qcColumns}
                    dataSource={qualityChecks}
                    rowKey={(r: any, i) => r.id || String(i)}
                    pagination={false}
                    size="small"
                    locale={{ emptyText: 'No quality checks yet' }}
                  />
                </>
              ),
            },
          ]} />
        )}
      </Modal>

      {/* Add Milestone Modal */}
      <Modal open={milestoneModal} title="Add Milestone" onCancel={() => setMilestoneModal(false)} onOk={handleAddMilestone}>
        <Form form={milestoneForm} layout="vertical">
          <Form.Item name="name" label="Milestone Name" rules={[{ required: true }]}>
            <Input placeholder="e.g. Kitchen Modular Installation" />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} placeholder="Details about this milestone" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="scheduledDate" label="Scheduled Date" rules={[{ required: true }]}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="paymentAmount" label="Linked Payment (INR)">
                <InputNumber style={{ width: '100%' }} min={0} placeholder="50000" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Add Room Design Modal */}
      <Modal open={roomModal} title="Add Room Design" onCancel={() => setRoomModal(false)} onOk={handleAddRoom}>
        <Form form={roomForm} layout="vertical">
          <Form.Item name="roomType" label="Room Type" rules={[{ required: true }]}>
            <Select placeholder="Select room type">
              {roomTypes.map(t => <Select.Option key={t} value={t}>{t.replace(/_/g, ' ')}</Select.Option>)}
            </Select>
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="areaSqft" label="Area (sqft)">
                <InputNumber style={{ width: '100%' }} min={1} placeholder="150" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="designStyle" label="Design Style">
                <Select placeholder="Select style" allowClear>
                  {designStyles.map(s => <Select.Option key={s} value={s}>{s}</Select.Option>)}
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Add Quality Check Modal */}
      <Modal open={qcModal} title="Add Quality Check" onCancel={() => setQcModal(false)} onOk={handleAddQc}>
        <Form form={qcForm} layout="vertical">
          <Form.Item name="checkpointName" label="Checkpoint Name" rules={[{ required: true }]}>
            <Input placeholder="e.g. Kitchen Countertop Alignment" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="category" label="Category">
                <Select placeholder="Select category" allowClear>
                  {['STRUCTURAL', 'ELECTRICAL', 'PLUMBING', 'FINISHING', 'ALIGNMENT', 'PAINT', 'HARDWARE'].map(c =>
                    <Select.Option key={c} value={c}>{c}</Select.Option>
                  )}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="Result">
                <Select placeholder="Select result" allowClear>
                  {['PASS', 'FAIL', 'REWORK', 'PENDING'].map(s =>
                    <Select.Option key={s} value={s}>{s}</Select.Option>
                  )}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="notes" label="Notes">
            <Input.TextArea rows={2} placeholder="Inspection notes" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Add Material Modal */}
      <Modal open={materialModal} title="Add Material Selection" onCancel={() => setMaterialModal(false)} onOk={handleAddMaterial}>
        <Form form={materialForm} layout="vertical">
          <Form.Item name="roomDesignId" label="Room">
            <Select placeholder="Select room (optional)" allowClear>
              {rooms.map((r: any) => (
                <Select.Option key={r.id} value={r.id}>{r.roomType?.replace(/_/g, ' ')} — {r.designStyle || 'No style'}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="category" label="Category" rules={[{ required: true }]}>
                <Select placeholder="Select category">
                  {['TILES', 'WOOD', 'LAMINATE', 'PAINT', 'HARDWARE', 'COUNTERTOP', 'GLASS', 'FABRIC', 'ELECTRICAL', 'PLUMBING'].map(c =>
                    <Select.Option key={c} value={c}>{c}</Select.Option>
                  )}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="materialName" label="Material Name" rules={[{ required: true }]}>
                <Input placeholder="e.g. Italian Marble" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={8}>
              <Form.Item name="brand" label="Brand">
                <Input placeholder="e.g. Kajaria" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="quantity" label="Quantity" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} min={1} placeholder="10" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="unitPrice" label="Unit Price (INR)" rules={[{ required: true }]}>
                <InputNumber style={{ width: '100%' }} min={0} placeholder="500" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}
