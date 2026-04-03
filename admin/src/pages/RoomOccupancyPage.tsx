import { useState, useEffect } from 'react';
import { Card, Table, Tag, Progress, Row, Col, Statistic, Select, Spin, Typography, Empty } from 'antd';
import { HomeOutlined, TeamOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

interface Listing {
  id: string;
  title: string;
  type: string;
  city: string;
  hostId: string;
}

interface RoomType {
  id: string;
  listingId: string;
  name: string;
  count: number;
  basePricePaise: number;
  sharingType: string | null;
  totalBeds: number | null;
  occupiedBeds: number | null;
}

interface Tenancy {
  id: string;
  tenancyRef: string;
  tenantId: string;
  roomTypeId: string;
  bedNumber: string;
  sharingType: string;
  status: string;
  moveInDate: string;
  moveOutDate: string | null;
  monthlyRentPaise: number;
}

interface PropertyOccupancy {
  listing: Listing;
  roomTypes: RoomType[];
  tenancies: Tenancy[];
  totalBeds: number;
  occupiedBeds: number;
  occupancyPct: number;
  monthlyRevenue: number;
}

const STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'green',
  NOTICE_PERIOD: 'orange',
  VACATED: 'default',
  TERMINATED: 'red',
};

const PG_TYPES = ['PG', 'HOSTEL', 'CO_LIVING', 'DORMITORY'];

export default function RoomOccupancyPage() {
  const [properties, setProperties] = useState<PropertyOccupancy[]>([]);
  const [loading, setLoading] = useState(true);
  const [cityFilter, setCityFilter] = useState<string>('all');
  const [expandedRows, setExpandedRows] = useState<string[]>([]);

  const token = localStorage.getItem('admin_token') || '';

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      // Fetch all listings
      const listingsRes = await adminApi.getListingsByStatus(token);
      const allListings: Listing[] = (listingsRes?.data || []).filter((l: Listing) =>
        PG_TYPES.includes(l.type)
      );

      // Fetch room types + tenancies for each PG listing
      const results: PropertyOccupancy[] = [];

      for (const listing of allListings) {
        try {
          const [rtRes, tnRes] = await Promise.all([
            adminApi.getRoomTypes(listing.id, token),
            adminApi.getPgTenancies(`listingId=${listing.id}`, token),
          ]);

          const roomTypes: RoomType[] = rtRes?.data || [];
          const tenancyData = tnRes?.data;
          const tenancies: Tenancy[] = Array.isArray(tenancyData)
            ? tenancyData
            : tenancyData?.content || [];

          const totalBeds = roomTypes.reduce((s, rt) => s + (rt.totalBeds || rt.count), 0);
          const occupiedBeds = roomTypes.reduce((s, rt) => s + (rt.occupiedBeds || 0), 0);
          const activeTenancies = tenancies.filter(t => t.status === 'ACTIVE' || t.status === 'NOTICE_PERIOD');
          const monthlyRevenue = activeTenancies.reduce((s, t) => s + t.monthlyRentPaise, 0);

          results.push({
            listing,
            roomTypes,
            tenancies,
            totalBeds,
            occupiedBeds,
            occupancyPct: totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0,
            monthlyRevenue,
          });
        } catch {
          // Skip listings that fail
        }
      }

      setProperties(results);
    } catch {
      setProperties([]);
    } finally {
      setLoading(false);
    }
  }

  // Aggregate platform stats
  const totalProperties = properties.length;
  const totalBeds = properties.reduce((s, p) => s + p.totalBeds, 0);
  const totalOccupied = properties.reduce((s, p) => s + p.occupiedBeds, 0);
  const totalVacant = totalBeds - totalOccupied;
  const platformOccupancy = totalBeds > 0 ? Math.round((totalOccupied / totalBeds) * 100) : 0;
  const totalMonthly = properties.reduce((s, p) => s + p.monthlyRevenue, 0);

  const cities = [...new Set(properties.map(p => p.listing.city).filter(Boolean))];
  const filtered = cityFilter === 'all' ? properties : properties.filter(p => p.listing.city === cityFilter);

  const columns = [
    {
      title: 'Property',
      dataIndex: ['listing', 'title'],
      key: 'title',
      render: (_: string, record: PropertyOccupancy) => (
        <div>
          <Text strong>{record.listing.title}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.listing.city} | {record.listing.type}
          </Text>
        </div>
      ),
    },
    {
      title: 'Room Types',
      key: 'roomTypes',
      width: 100,
      render: (_: unknown, record: PropertyOccupancy) => record.roomTypes.length,
    },
    {
      title: 'Total Beds',
      dataIndex: 'totalBeds',
      key: 'totalBeds',
      width: 100,
      sorter: (a: PropertyOccupancy, b: PropertyOccupancy) => a.totalBeds - b.totalBeds,
    },
    {
      title: 'Occupied',
      dataIndex: 'occupiedBeds',
      key: 'occupied',
      width: 100,
      render: (val: number) => <Text style={{ color: '#52c41a', fontWeight: 600 }}>{val}</Text>,
      sorter: (a: PropertyOccupancy, b: PropertyOccupancy) => a.occupiedBeds - b.occupiedBeds,
    },
    {
      title: 'Vacant',
      key: 'vacant',
      width: 100,
      render: (_: unknown, record: PropertyOccupancy) => {
        const vacant = record.totalBeds - record.occupiedBeds;
        return <Text style={{ color: vacant > 0 ? '#fa8c16' : '#52c41a', fontWeight: 600 }}>{vacant}</Text>;
      },
      sorter: (a: PropertyOccupancy, b: PropertyOccupancy) =>
        (a.totalBeds - a.occupiedBeds) - (b.totalBeds - b.occupiedBeds),
    },
    {
      title: 'Occupancy',
      dataIndex: 'occupancyPct',
      key: 'occupancy',
      width: 160,
      render: (pct: number) => (
        <Progress
          percent={pct}
          size="small"
          strokeColor={pct >= 80 ? '#52c41a' : pct >= 50 ? '#fa8c16' : '#f5222d'}
          format={p => `${p}%`}
        />
      ),
      sorter: (a: PropertyOccupancy, b: PropertyOccupancy) => a.occupancyPct - b.occupancyPct,
    },
    {
      title: 'Monthly Revenue',
      dataIndex: 'monthlyRevenue',
      key: 'revenue',
      width: 140,
      render: (val: number) => `₹${(val / 100).toLocaleString('en-IN')}`,
      sorter: (a: PropertyOccupancy, b: PropertyOccupancy) => a.monthlyRevenue - b.monthlyRevenue,
    },
  ];

  // Expanded row: room type breakdown + tenant list
  function expandedRowRender(record: PropertyOccupancy) {
    const activeTenancies = record.tenancies.filter(t => t.status !== 'VACATED');

    return (
      <div style={{ padding: '8px 0' }}>
        {/* Room type breakdown */}
        <Text strong style={{ fontSize: 13 }}>Room Types</Text>
        <Table
          size="small"
          pagination={false}
          dataSource={record.roomTypes}
          rowKey="id"
          style={{ marginTop: 8, marginBottom: 16 }}
          columns={[
            { title: 'Name', dataIndex: 'name', key: 'name' },
            { title: 'Rooms', dataIndex: 'count', key: 'count', width: 70 },
            {
              title: 'Sharing', dataIndex: 'sharingType', key: 'sharing', width: 120,
              render: (v: string) => v?.replace('_', ' ') || 'Private',
            },
            { title: 'Total Beds', dataIndex: 'totalBeds', key: 'total', width: 90 },
            {
              title: 'Occupied', dataIndex: 'occupiedBeds', key: 'occ', width: 90,
              render: (v: number) => <Tag color="green">{v || 0}</Tag>,
            },
            {
              title: 'Vacant', key: 'vacant', width: 90,
              render: (_: unknown, rt: RoomType) => {
                const v = (rt.totalBeds || rt.count) - (rt.occupiedBeds || 0);
                return <Tag color={v > 0 ? 'orange' : 'green'}>{v}</Tag>;
              },
            },
            {
              title: 'Rate/month', dataIndex: 'basePricePaise', key: 'rate', width: 120,
              render: (v: number) => `₹${(v / 100).toLocaleString('en-IN')}`,
            },
          ]}
        />

        {/* Tenant list */}
        {activeTenancies.length > 0 && (
          <>
            <Text strong style={{ fontSize: 13 }}>Active Tenants ({activeTenancies.length})</Text>
            <Table
              size="small"
              pagination={false}
              dataSource={activeTenancies}
              rowKey="id"
              style={{ marginTop: 8 }}
              columns={[
                { title: 'Ref', dataIndex: 'tenancyRef', key: 'ref', width: 130 },
                { title: 'Bed', dataIndex: 'bedNumber', key: 'bed', width: 70 },
                {
                  title: 'Sharing', dataIndex: 'sharingType', key: 'sharing', width: 120,
                  render: (v: string) => v?.replace('_', ' '),
                },
                {
                  title: 'Status', dataIndex: 'status', key: 'status', width: 120,
                  render: (s: string) => <Tag color={STATUS_COLOR[s] || 'default'}>{s.replace('_', ' ')}</Tag>,
                },
                { title: 'Move In', dataIndex: 'moveInDate', key: 'moveIn', width: 110 },
                {
                  title: 'Move Out', dataIndex: 'moveOutDate', key: 'moveOut', width: 110,
                  render: (v: string | null) => v || '-',
                },
                {
                  title: 'Rent/month', dataIndex: 'monthlyRentPaise', key: 'rent', width: 120,
                  render: (v: number) => `₹${(v / 100).toLocaleString('en-IN')}`,
                },
              ]}
            />
          </>
        )}
      </div>
    );
  }

  return (
    <div>
      <Title level={4}>Room Occupancy</Title>
      <Text type="secondary">Platform-wide PG & hostel room occupancy across all properties</Text>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>
      ) : properties.length === 0 ? (
        <Empty description="No PG/Hostel properties found" style={{ padding: 80 }} />
      ) : (
        <>
          {/* Platform Stats */}
          <Row gutter={[16, 16]} style={{ marginTop: 24, marginBottom: 24 }}>
            <Col xs={12} lg={4}>
              <Card size="small">
                <Statistic title="PG Properties" value={totalProperties} prefix={<HomeOutlined />} />
              </Card>
            </Col>
            <Col xs={12} lg={4}>
              <Card size="small">
                <Statistic title="Total Beds" value={totalBeds} prefix={<TeamOutlined />} />
              </Card>
            </Col>
            <Col xs={12} lg={4}>
              <Card size="small">
                <Statistic title="Occupied" value={totalOccupied} valueStyle={{ color: '#52c41a' }} prefix={<CheckCircleOutlined />} />
              </Card>
            </Col>
            <Col xs={12} lg={4}>
              <Card size="small">
                <Statistic title="Vacant" value={totalVacant} valueStyle={{ color: totalVacant > 0 ? '#fa8c16' : '#52c41a' }} prefix={<WarningOutlined />} />
              </Card>
            </Col>
            <Col xs={12} lg={4}>
              <Card size="small">
                <Statistic title="Occupancy Rate" value={platformOccupancy} suffix="%" />
                <Progress
                  percent={platformOccupancy}
                  size="small"
                  showInfo={false}
                  strokeColor={platformOccupancy >= 80 ? '#52c41a' : platformOccupancy >= 50 ? '#fa8c16' : '#f5222d'}
                  style={{ marginTop: 4 }}
                />
              </Card>
            </Col>
            <Col xs={12} lg={4}>
              <Card size="small">
                <Statistic
                  title="Monthly Revenue"
                  value={totalMonthly / 100}
                  precision={0}
                  prefix="₹"
                />
              </Card>
            </Col>
          </Row>

          {/* City Filter */}
          {cities.length > 1 && (
            <div style={{ marginBottom: 16 }}>
              <Text style={{ marginRight: 8 }}>Filter by city:</Text>
              <Select value={cityFilter} onChange={setCityFilter} style={{ width: 200 }}>
                <Select.Option value="all">All Cities</Select.Option>
                {cities.sort().map(c => (
                  <Select.Option key={c} value={c}>{c}</Select.Option>
                ))}
              </Select>
            </div>
          )}

          {/* Properties Table */}
          <Table
            dataSource={filtered}
            columns={columns}
            rowKey={r => r.listing.id}
            expandable={{
              expandedRowRender,
              expandedRowKeys: expandedRows,
              onExpandedRowsChange: (keys) => setExpandedRows(keys as string[]),
            }}
            pagination={{ pageSize: 20, showSizeChanger: true }}
            size="middle"
          />
        </>
      )}
    </div>
  );
}
