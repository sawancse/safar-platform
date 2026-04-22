import { useEffect, useState } from 'react';
import {
  Table, Tag, Typography, Button, Input, Select, Modal, Form, message, Popconfirm,
  Space, Avatar, Switch, InputNumber, Card, Upload,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, UserOutlined, UploadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

type StaffRow = {
  id: string;
  chefId: string | null;
  name: string;
  role: string;
  phone?: string;
  photoUrl?: string;
  kycStatus?: string;
  hourlyRatePaise?: number;
  languages?: string;
  yearsExperience?: number;
  notes?: string;
  active: boolean;
  createdAt?: string;
};

const ROLE_META: Record<string, { label: string; color: string; icon: string }> = {
  waiter:    { label: 'Waiter',    color: 'blue',   icon: '🧑‍🍳' },
  cleaner:   { label: 'Cleaner',   color: 'green',  icon: '🧹' },
  bartender: { label: 'Bartender', color: 'purple', icon: '🍸' },
};

// Suggestions for the language picker. Users can still type custom entries
// (mode="tags"). Kept to the languages most commonly spoken across India's
// metros + the major regional hubs Safar services.
const LANGUAGE_OPTIONS = [
  'Hindi', 'English', 'Tamil', 'Telugu', 'Kannada', 'Malayalam',
  'Marathi', 'Gujarati', 'Bengali', 'Punjabi', 'Odia', 'Assamese',
  'Urdu', 'Konkani', 'Rajasthani', 'Bhojpuri',
].map(l => ({ value: l, label: l }));

export default function StaffPoolPage() {
  const token = localStorage.getItem('admin_token') ?? '';
  const [rows, setRows] = useState<StaffRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [roleFilter, setRoleFilter] = useState<string | undefined>();
  const [activeOnly, setActiveOnly] = useState(false);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<StaffRow | null>(null);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);
  const [photoUrl, setPhotoUrl] = useState<string>('');
  const [uploadingPhoto, setUploadingPhoto] = useState(false);

  const load = () => {
    setLoading(true);
    adminApi.listStaffPool(token, { activeOnly, role: roleFilter })
      .then(data => setRows(data || []))
      .catch((e: any) => message.error(e?.response?.data?.message || 'Failed to load staff pool'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, [roleFilter, activeOnly]);

  const filtered = rows.filter(r => {
    if (!search) return true;
    const s = search.toLowerCase();
    return r.name.toLowerCase().includes(s)
      || (r.phone || '').includes(s)
      || (r.languages || '').toLowerCase().includes(s);
  });

  function splitLanguages(raw?: string): string[] {
    if (!raw) return [];
    return raw.split(',').map(s => s.trim()).filter(Boolean);
  }

  function openAdd() {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ role: 'waiter', active: true, languages: [] });
    setPhotoUrl('');
    setModalOpen(true);
  }

  function openEdit(row: StaffRow) {
    setEditing(row);
    form.setFieldsValue({
      name: row.name,
      role: row.role,
      phone: row.phone,
      languages: splitLanguages(row.languages),
      yearsExperience: row.yearsExperience ?? null,
      hourlyRate: row.hourlyRatePaise != null ? Math.round(row.hourlyRatePaise / 100) : null,
      notes: row.notes,
      active: row.active,
    });
    setPhotoUrl(row.photoUrl || '');
    setModalOpen(true);
  }

  async function handlePhotoUpload(file: File) {
    if (file.size > 5 * 1024 * 1024) {
      message.error('Photo must be under 5MB');
      return false;
    }
    setUploadingPhoto(true);
    try {
      const url = await adminApi.uploadFile(file, 'staff-photos', token);
      setPhotoUrl(url);
      message.success('Photo uploaded');
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Upload failed');
    } finally {
      setUploadingPhoto(false);
    }
    return false; // prevent AntD's default upload behavior
  }

  async function onSave() {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const langs = Array.isArray(values.languages) ? values.languages : splitLanguages(values.languages);
      const payload = {
        name: values.name.trim(),
        role: values.role,
        phone: values.phone || null,
        photoUrl: photoUrl || null,
        languages: langs.length ? langs.map((s: string) => s.trim()).filter(Boolean).join(', ') : null,
        yearsExperience: values.yearsExperience ?? null,
        hourlyRatePaise: values.hourlyRate != null ? values.hourlyRate * 100 : null,
        notes: values.notes || null,
        active: values.active ?? true,
      };
      if (editing) {
        const updated = await adminApi.updatePoolStaff(editing.id, payload, token);
        setRows(prev => prev.map(r => r.id === updated.id ? updated : r));
        message.success('Updated');
      } else {
        const created = await adminApi.createPoolStaff(payload, token);
        setRows(prev => [created, ...prev]);
        message.success('Added to pool');
      }
      setModalOpen(false);
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(row: StaffRow) {
    try {
      await adminApi.deletePoolStaff(row.id, token);
      setRows(prev => prev.map(r => r.id === row.id ? { ...r, active: false } : r));
      message.success('Marked inactive');
    } catch (e: any) {
      message.error(e?.response?.data?.message || 'Failed to remove');
    }
  }

  const columns: ColumnsType<StaffRow> = [
    {
      title: 'Person',
      key: 'person',
      render: (_: any, r) => (
        <Space>
          <Avatar src={r.photoUrl} icon={!r.photoUrl && <UserOutlined />} />
          <div>
            <div style={{ fontWeight: 600 }}>{r.name}</div>
            <Text type="secondary" style={{ fontSize: 12 }}>{r.phone || '—'}</Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Role',
      dataIndex: 'role',
      render: (role: string) => {
        const m = ROLE_META[role] || { label: role, color: 'default', icon: '🧑' };
        return <Tag color={m.color}>{m.icon} {m.label}</Tag>;
      },
      filters: Object.entries(ROLE_META).map(([k, m]) => ({ text: m.label, value: k })),
      onFilter: (v, r) => r.role === v,
    },
    {
      title: 'Rate (custom)',
      dataIndex: 'hourlyRatePaise',
      render: (p?: number) => p != null ? `₹${(p / 100).toLocaleString('en-IN')}` : <Text type="secondary">default</Text>,
    },
    {
      title: 'Experience',
      dataIndex: 'yearsExperience',
      render: (y?: number) => y != null ? `${y}y` : '—',
    },
    {
      title: 'Languages',
      dataIndex: 'languages',
      ellipsis: true,
      render: (l?: string) => l || '—',
    },
    {
      title: 'KYC',
      dataIndex: 'kycStatus',
      render: (k?: string) => <Tag color={k === 'VERIFIED' ? 'green' : k === 'REJECTED' ? 'red' : 'orange'}>{k || 'PENDING'}</Tag>,
    },
    {
      title: 'Status',
      dataIndex: 'active',
      render: (a: boolean) => a ? <Tag color="green">Active</Tag> : <Tag>Inactive</Tag>,
    },
    {
      title: '',
      key: 'actions',
      width: 140,
      render: (_: any, r) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r)} />
          <Popconfirm title={`Deactivate ${r.name}?`} onConfirm={() => onDelete(r)} okText="Yes" cancelText="No">
            <Button size="small" danger icon={<DeleteOutlined />} disabled={!r.active} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Platform Staff Pool</Title>
          <Text type="secondary">Waiters, cleaners, bartenders available to every chef. Chef-owned team is separate and lives on each chef's dashboard.</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={openAdd}>Add Staff</Button>
      </div>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input.Search
            placeholder="Search by name, phone, language"
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ width: 280 }}
            allowClear
          />
          <Select
            placeholder="All roles"
            value={roleFilter}
            onChange={setRoleFilter}
            allowClear
            style={{ width: 160 }}
            options={Object.entries(ROLE_META).map(([k, m]) => ({ value: k, label: `${m.icon} ${m.label}` }))}
          />
          <Space>
            <span>Active only</span>
            <Switch checked={activeOnly} onChange={setActiveOnly} />
          </Space>
        </Space>
      </Card>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={filtered}
        loading={loading}
        pagination={{ pageSize: 20 }}
      />

      <Modal
        open={modalOpen}
        title={editing ? `Edit ${editing.name}` : 'Add Staff to Pool'}
        onCancel={() => !saving && setModalOpen(false)}
        onOk={onSave}
        confirmLoading={saving}
        okText={editing ? 'Save' : 'Add'}
      >
        <Form layout="vertical" form={form}>
          <Form.Item label="Role" name="role" rules={[{ required: true }]}>
            <Select options={Object.entries(ROLE_META).map(([k, m]) => ({ value: k, label: `${m.icon} ${m.label}` }))} />
          </Form.Item>
          <Form.Item label="Name" name="name" rules={[{ required: true, min: 2 }]}>
            <Input placeholder="Ramesh Kumar" />
          </Form.Item>
          <Form.Item label="Phone" name="phone">
            <Input placeholder="9876543210" maxLength={10} />
          </Form.Item>
          <Form.Item label="Photo">
            <Space align="start">
              {photoUrl ? (
                <Avatar src={photoUrl} size={64} />
              ) : (
                <Avatar icon={<UserOutlined />} size={64} />
              )}
              <Space direction="vertical" size={4}>
                <Upload
                  accept="image/*"
                  showUploadList={false}
                  beforeUpload={handlePhotoUpload}
                  disabled={uploadingPhoto}
                >
                  <Button icon={<UploadOutlined />} loading={uploadingPhoto}>
                    {photoUrl ? 'Change photo' : 'Upload photo'}
                  </Button>
                </Upload>
                {photoUrl && (
                  <Button type="link" danger size="small" onClick={() => setPhotoUrl('')}>
                    Remove
                  </Button>
                )}
              </Space>
            </Space>
          </Form.Item>
          <Form.Item label="Languages" name="languages">
            <Select
              mode="tags"
              placeholder="Pick from list or type your own"
              options={LANGUAGE_OPTIONS}
              tokenSeparators={[',']}
              maxTagCount="responsive"
            />
          </Form.Item>
          <Space style={{ width: '100%' }}>
            <Form.Item label="Years experience" name="yearsExperience" style={{ flex: 1, minWidth: 160 }}>
              <InputNumber min={0} max={50} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item label="Custom rate per event (₹)" name="hourlyRate" style={{ flex: 1, minWidth: 200 }}>
              <InputNumber min={0} placeholder="Blank = use default role rate" style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item label="Notes" name="notes">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="Active" name="active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
