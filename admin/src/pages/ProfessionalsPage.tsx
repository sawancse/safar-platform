import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, Tabs, Tag, message, Popconfirm, InputNumber } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons';
import { adminApi as api } from '../lib/api';

const { TabPane } = Tabs;
const { TextArea } = Input;

/* ── Shared Types ── */
interface Advocate {
  id: string; fullName: string; barCouncilId?: string; email?: string; phone?: string;
  city?: string; state?: string; experienceYears?: number; specializations?: string[];
  verified?: boolean; active?: boolean; consultationFeePaise?: number; bio?: string;
}

interface Designer {
  id: string; fullName: string; companyName?: string; email?: string; phone?: string;
  city?: string; state?: string; experienceYears?: number; specializations?: string[];
  verified?: boolean; active?: boolean; consultationFeePaise?: number; bio?: string;
}

interface Bank {
  id: string; bankName: string; interestRateMin?: number; interestRateMax?: number;
  maxTenureMonths?: number; processingFeePercent?: number; contactName?: string;
  contactEmail?: string; contactPhone?: string; active?: boolean; specialOffers?: string;
}

export default function ProfessionalsPage() {
  const [advocates, setAdvocates] = useState<Advocate[]>([]);
  const [designers, setDesigners] = useState<Designer[]>([]);
  const [banks, setBanks] = useState<Bank[]>([]);
  const [pendingAdvocates, setPendingAdvocates] = useState<Advocate[]>([]);
  const [pendingDesigners, setPendingDesigners] = useState<Designer[]>([]);
  const [loading, setLoading] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectTarget, setRejectTarget] = useState<{ type: string; id: string } | null>(null);
  const [searchText, setSearchText] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingType, setEditingType] = useState<'advocate' | 'designer' | 'bank'>('advocate');
  const [editingItem, setEditingItem] = useState<any>(null);
  const [form] = Form.useForm();

  const token = localStorage.getItem('admin_token') || '';

  useEffect(() => { loadAll(); }, []);

  async function loadAll() {
    setLoading(true);
    try {
      const [a, d, b, pa, pd] = await Promise.all([
        api.get('/api/v1/legal/advocates').then(r => r.data),
        api.get('/api/v1/interiors/designers').then(r => r.data),
        api.get('/api/v1/homeloan/banks').then(r => r.data),
        api.get('/api/v1/legal/advocates/pending').then(r => r.data).catch(() => []),
        api.get('/api/v1/interiors/designers/pending').then(r => r.data).catch(() => []),
      ]);
      setAdvocates(Array.isArray(a) ? a : []);
      setDesigners(Array.isArray(d) ? d : []);
      setBanks(Array.isArray(b) ? b : []);
      setPendingAdvocates(Array.isArray(pa) ? pa : []);
      setPendingDesigners(Array.isArray(pd) ? pd : []);
    } catch (e) { message.error('Failed to load professionals'); }
    finally { setLoading(false); }
  }

  function openCreate(type: 'advocate' | 'designer' | 'bank') {
    setEditingType(type);
    setEditingItem(null);
    form.resetFields();
    setModalOpen(true);
  }

  function openEdit(type: 'advocate' | 'designer' | 'bank', item: any) {
    setEditingType(type);
    setEditingItem(item);
    form.setFieldsValue(item);
    setModalOpen(true);
  }

  async function handleSave() {
    const values = await form.validateFields();
    try {
      const endpoints: Record<string, string> = {
        advocate: '/api/v1/legal/advocates',
        designer: '/api/v1/interiors/designers',
        bank: '/api/v1/homeloan/banks',
      };
      if (editingItem) {
        await api.put(`${endpoints[editingType]}/${editingItem.id}`, values);
        message.success('Updated successfully');
      } else {
        await api.post(endpoints[editingType], values);
        message.success('Created successfully');
      }
      setModalOpen(false);
      loadAll();
    } catch (e: any) { message.error(e.response?.data?.detail || 'Save failed'); }
  }

  async function handleDelete(type: 'advocate' | 'designer' | 'bank', id: string) {
    const endpoints: Record<string, string> = {
      advocate: '/api/v1/legal/advocates',
      designer: '/api/v1/interiors/designers',
      bank: '/api/v1/homeloan/banks',
    };
    try {
      await api.delete(`${endpoints[type]}/${id}`);
      message.success('Deactivated successfully');
      loadAll();
    } catch { message.error('Failed to delete'); }
  }

  const advocateColumns = [
    { title: 'Name', dataIndex: 'fullName', key: 'fullName' },
    { title: 'City', dataIndex: 'city', key: 'city' },
    { title: 'Bar Council', dataIndex: 'barCouncilId', key: 'barCouncilId' },
    { title: 'Experience', dataIndex: 'experienceYears', key: 'exp', render: (v: number) => v ? `${v} yrs` : '-' },
    { title: 'Cases', dataIndex: 'completedCases', key: 'cases', render: (v: number) => v ?? 0 },
    { title: 'Status', key: 'status', render: (_: any, r: Advocate) => (
      <Space>
        {r.verified && <Tag color="green">Verified</Tag>}
        <Tag color={r.active ? 'blue' : 'red'}>{r.active ? 'Active' : 'Inactive'}</Tag>
      </Space>
    )},
    { title: 'Actions', key: 'actions', render: (_: any, r: Advocate) => (
      <Space>
        <Button icon={<EditOutlined />} size="small" onClick={() => openEdit('advocate', r)} />
        <Popconfirm title="Deactivate this advocate?" onConfirm={() => handleDelete('advocate', r.id)}>
          <Button icon={<DeleteOutlined />} size="small" danger />
        </Popconfirm>
      </Space>
    )},
  ];

  const designerColumns = [
    { title: 'Name', dataIndex: 'fullName', key: 'fullName' },
    { title: 'Company', dataIndex: 'companyName', key: 'companyName' },
    { title: 'City', dataIndex: 'city', key: 'city' },
    { title: 'Experience', dataIndex: 'experienceYears', key: 'exp', render: (v: number) => v ? `${v} yrs` : '-' },
    { title: 'Projects', dataIndex: 'completedProjects', key: 'projects', render: (v: number) => v ?? 0 },
    { title: 'Status', key: 'status', render: (_: any, r: Designer) => (
      <Space>
        {r.verified && <Tag color="green">Verified</Tag>}
        <Tag color={r.active ? 'blue' : 'red'}>{r.active ? 'Active' : 'Inactive'}</Tag>
      </Space>
    )},
    { title: 'Actions', key: 'actions', render: (_: any, r: Designer) => (
      <Space>
        <Button icon={<EditOutlined />} size="small" onClick={() => openEdit('designer', r)} />
        <Popconfirm title="Deactivate?" onConfirm={() => handleDelete('designer', r.id)}>
          <Button icon={<DeleteOutlined />} size="small" danger />
        </Popconfirm>
      </Space>
    )},
  ];

  const bankColumns = [
    { title: 'Bank', dataIndex: 'bankName', key: 'bankName' },
    { title: 'Interest Rate', key: 'rate', render: (_: any, r: Bank) => `${r.interestRateMin}% - ${r.interestRateMax}%` },
    { title: 'Max Tenure', dataIndex: 'maxTenureMonths', key: 'tenure', render: (v: number) => v ? `${v} months` : '-' },
    { title: 'Processing Fee', dataIndex: 'processingFeePercent', key: 'fee', render: (v: number) => v ? `${v}%` : '-' },
    { title: 'Contact', dataIndex: 'contactName', key: 'contact' },
    { title: 'Status', dataIndex: 'active', key: 'active', render: (v: boolean) => <Tag color={v ? 'blue' : 'red'}>{v ? 'Active' : 'Inactive'}</Tag> },
    { title: 'Actions', key: 'actions', render: (_: any, r: Bank) => (
      <Space>
        <Button icon={<EditOutlined />} size="small" onClick={() => openEdit('bank', r)} />
        <Popconfirm title="Deactivate?" onConfirm={() => handleDelete('bank', r.id)}>
          <Button icon={<DeleteOutlined />} size="small" danger />
        </Popconfirm>
      </Space>
    )},
  ];

  return (
    <div style={{ padding: 24 }}>
      <h2>Professional Management</h2>

      <Tabs defaultActiveKey={pendingAdvocates.length + pendingDesigners.length > 0 ? 'verification' : 'advocates'}>
        {/* Verification Queue */}
        <TabPane tab={<span>Verification Queue {(pendingAdvocates.length + pendingDesigners.length) > 0 && <Tag color="red" style={{ marginLeft: 4 }}>{pendingAdvocates.length + pendingDesigners.length}</Tag>}</span>} key="verification">
          <h3 style={{ marginBottom: 12 }}>Pending Advocate Registrations ({pendingAdvocates.length})</h3>
          {pendingAdvocates.length === 0 ? <p style={{ color: '#999', marginBottom: 24 }}>No pending advocates</p> : (
            <Table dataSource={pendingAdvocates} rowKey="id" pagination={false} style={{ marginBottom: 24 }} columns={[
              { title: 'Name', dataIndex: 'fullName' },
              { title: 'City', dataIndex: 'city' },
              { title: 'Bar Council', dataIndex: 'barCouncilId' },
              { title: 'Experience', dataIndex: 'experienceYears', render: (v: number) => v ? `${v} yrs` : '-' },
              { title: 'Phone', dataIndex: 'phone' },
              { title: 'Email', dataIndex: 'email' },
              { title: 'Registered', dataIndex: 'createdAt', render: (v: string) => v ? new Date(v).toLocaleDateString() : '-' },
              { title: 'Actions', key: 'actions', render: (_: any, r: any) => (
                <Space>
                  <Button type="primary" icon={<CheckOutlined />} size="small" onClick={async () => {
                    try { await api.post(`/api/v1/legal/advocates/${r.id}/approve`); message.success('Approved!'); loadAll(); } catch { message.error('Failed'); }
                  }}>Approve</Button>
                  <Button danger icon={<CloseOutlined />} size="small" onClick={() => { setRejectTarget({ type: 'advocate', id: r.id }); setRejectReason(''); setRejectModalOpen(true); }}>Reject</Button>
                </Space>
              )},
            ]} />
          )}

          <h3 style={{ marginBottom: 12 }}>Pending Designer Registrations ({pendingDesigners.length})</h3>
          {pendingDesigners.length === 0 ? <p style={{ color: '#999' }}>No pending designers</p> : (
            <Table dataSource={pendingDesigners} rowKey="id" pagination={false} columns={[
              { title: 'Name', dataIndex: 'fullName' },
              { title: 'Company', dataIndex: 'companyName' },
              { title: 'City', dataIndex: 'city' },
              { title: 'Experience', dataIndex: 'experienceYears', render: (v: number) => v ? `${v} yrs` : '-' },
              { title: 'Phone', dataIndex: 'phone' },
              { title: 'Registered', dataIndex: 'createdAt', render: (v: string) => v ? new Date(v).toLocaleDateString() : '-' },
              { title: 'Actions', key: 'actions', render: (_: any, r: any) => (
                <Space>
                  <Button type="primary" icon={<CheckOutlined />} size="small" onClick={async () => {
                    try { await api.post(`/api/v1/interiors/designers/${r.id}/approve`); message.success('Approved!'); loadAll(); } catch { message.error('Failed'); }
                  }}>Approve</Button>
                  <Button danger icon={<CloseOutlined />} size="small" onClick={() => { setRejectTarget({ type: 'designer', id: r.id }); setRejectReason(''); setRejectModalOpen(true); }}>Reject</Button>
                </Space>
              )},
            ]} />
          )}
        </TabPane>

        <TabPane tab={`Advocates (${advocates.length})`} key="advocates">
          <Space style={{ marginBottom: 16 }}>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate('advocate')}>Add Advocate</Button>
            <Input placeholder="Search by name, city, bar council..." value={searchText} onChange={e => setSearchText(e.target.value)} allowClear style={{ width: 300 }} />
          </Space>
          <Table dataSource={advocates.filter(a => !searchText || [a.fullName, a.city, a.barCouncilId, a.email].some(f => f?.toLowerCase().includes(searchText.toLowerCase())))} columns={advocateColumns} rowKey="id" loading={loading} pagination={{ pageSize: 15 }} />
        </TabPane>

        <TabPane tab={`Interior Designers (${designers.length})`} key="designers">
          <Space style={{ marginBottom: 16 }}>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate('designer')}>Add Designer</Button>
            <Input placeholder="Search by name, company, city..." value={searchText} onChange={e => setSearchText(e.target.value)} allowClear style={{ width: 300 }} />
          </Space>
          <Table dataSource={designers.filter(d => !searchText || [d.fullName, d.companyName, d.city, d.email].some(f => f?.toLowerCase().includes(searchText.toLowerCase())))} columns={designerColumns} rowKey="id" loading={loading} pagination={{ pageSize: 15 }} />
        </TabPane>

        <TabPane tab={`Partner Banks (${banks.length})`} key="banks">
          <Space style={{ marginBottom: 16 }}>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate('bank')}>Add Bank</Button>
            <Input placeholder="Search by bank name, contact..." value={searchText} onChange={e => setSearchText(e.target.value)} allowClear style={{ width: 300 }} />
          </Space>
          <Table dataSource={banks.filter(b => !searchText || [b.bankName, b.contactName, b.contactEmail].some(f => f?.toLowerCase().includes(searchText.toLowerCase())))} columns={bankColumns} rowKey="id" loading={loading} pagination={{ pageSize: 15 }} />
        </TabPane>
      </Tabs>

      {/* Create/Edit Modal */}
      <Modal
        title={`${editingItem ? 'Edit' : 'Add'} ${editingType === 'advocate' ? 'Advocate' : editingType === 'designer' ? 'Interior Designer' : 'Partner Bank'}`}
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          {editingType === 'advocate' && (
            <>
              <Form.Item name="fullName" label="Full Name" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="barCouncilId" label="Bar Council Number"><Input /></Form.Item>
              <Form.Item name="email" label="Email"><Input /></Form.Item>
              <Form.Item name="phone" label="Phone"><Input /></Form.Item>
              <Form.Item name="city" label="City"><Input /></Form.Item>
              <Form.Item name="state" label="State"><Input /></Form.Item>
              <Form.Item name="experienceYears" label="Experience (years)"><InputNumber min={0} /></Form.Item>
              <Form.Item name="consultationFeePaise" label="Consultation Fee (paise)"><InputNumber min={0} /></Form.Item>
              <Form.Item name="bio" label="Bio"><TextArea rows={3} /></Form.Item>
              <Form.Item name="verified" label="Verified" valuePropName="checked">
                <Select options={[{ value: true, label: 'Yes' }, { value: false, label: 'No' }]} />
              </Form.Item>
            </>
          )}
          {editingType === 'designer' && (
            <>
              <Form.Item name="fullName" label="Full Name" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="companyName" label="Company Name"><Input /></Form.Item>
              <Form.Item name="email" label="Email"><Input /></Form.Item>
              <Form.Item name="phone" label="Phone"><Input /></Form.Item>
              <Form.Item name="city" label="City"><Input /></Form.Item>
              <Form.Item name="state" label="State"><Input /></Form.Item>
              <Form.Item name="experienceYears" label="Experience (years)"><InputNumber min={0} /></Form.Item>
              <Form.Item name="consultationFeePaise" label="Consultation Fee (paise)"><InputNumber min={0} /></Form.Item>
              <Form.Item name="bio" label="Bio"><TextArea rows={3} /></Form.Item>
              <Form.Item name="verified" label="Verified">
                <Select options={[{ value: true, label: 'Yes' }, { value: false, label: 'No' }]} />
              </Form.Item>
            </>
          )}
          {editingType === 'bank' && (
            <>
              <Form.Item name="bankName" label="Bank Name" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="interestRateMin" label="Min Interest Rate (%)"><InputNumber min={0} step={0.1} /></Form.Item>
              <Form.Item name="interestRateMax" label="Max Interest Rate (%)"><InputNumber min={0} step={0.1} /></Form.Item>
              <Form.Item name="maxTenureMonths" label="Max Tenure (months)"><InputNumber min={0} /></Form.Item>
              <Form.Item name="processingFeePercent" label="Processing Fee (%)"><InputNumber min={0} step={0.01} /></Form.Item>
              <Form.Item name="contactName" label="Contact Person"><Input /></Form.Item>
              <Form.Item name="contactEmail" label="Contact Email"><Input /></Form.Item>
              <Form.Item name="contactPhone" label="Contact Phone"><Input /></Form.Item>
              <Form.Item name="specialOffers" label="Special Offers"><TextArea rows={2} /></Form.Item>
            </>
          )}
        </Form>
      </Modal>

      {/* Reject Modal */}
      <Modal
        title="Reject Application"
        open={rejectModalOpen}
        onOk={async () => {
          if (!rejectTarget || !rejectReason.trim()) { message.warning('Please provide a reason'); return; }
          try {
            const endpoint = rejectTarget.type === 'advocate'
              ? `/api/v1/legal/advocates/${rejectTarget.id}/reject?reason=${encodeURIComponent(rejectReason)}`
              : `/api/v1/interiors/designers/${rejectTarget.id}/reject?reason=${encodeURIComponent(rejectReason)}`;
            await api.post(endpoint);
            message.success('Rejected');
            setRejectModalOpen(false);
            loadAll();
          } catch { message.error('Failed to reject'); }
        }}
        onCancel={() => setRejectModalOpen(false)}
        okText="Reject"
        okButtonProps={{ danger: true }}
      >
        <p style={{ marginBottom: 8 }}>Why are you rejecting this application?</p>
        <TextArea rows={3} value={rejectReason} onChange={e => setRejectReason(e.target.value)} placeholder="Reason for rejection (will be shown to the applicant)" />
      </Modal>
    </div>
  );
}
