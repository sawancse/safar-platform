import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Button, theme } from 'antd';
import {
  DashboardOutlined,
  HomeOutlined,
  TeamOutlined,
  SafetyOutlined,
  FundOutlined,
  BankOutlined,
  LogoutOutlined,
  CalendarOutlined,
  UserOutlined,
  ApiOutlined,
  ShopOutlined,
  AppstoreOutlined,
  FireOutlined,
  HeartOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  FormatPainterOutlined,
  BuildOutlined,
  ToolOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/dashboard',       icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/bookings',        icon: <CalendarOutlined />,  label: 'Bookings' },
  { key: '/listings',        icon: <HomeOutlined />,      label: 'Listings' },
  { key: '/hosts',           icon: <TeamOutlined />,      label: 'Hosts' },
  { key: '/guests',          icon: <UserOutlined />,      label: 'Guests' },
  { key: '/kyc',             icon: <SafetyOutlined />,    label: 'KYC Verification' },
  { key: '/revenue',         icon: <FundOutlined />,      label: 'Revenue' },
  { key: '/payouts',         icon: <BankOutlined />,      label: 'Payouts' },
  { key: '/channel-manager', icon: <ApiOutlined />,       label: 'Channel Manager' },
  { key: '/sale-properties', icon: <ShopOutlined />,     label: 'Sale Properties' },
  { key: '/builder-projects', icon: <BuildOutlined />,   label: 'Builder Projects' },
  { key: '/room-occupancy', icon: <AppstoreOutlined />,  label: 'Room Occupancy' },
  { key: '/cooks',          icon: <FireOutlined />,       label: 'Safar Cooks' },
  { key: '/staff-pool',     icon: <TeamOutlined />,       label: 'Staff Pool' },
  { key: '/donors',         icon: <HeartOutlined />,      label: 'Donors' },
  { key: '/experiences',    icon: <ExperimentOutlined />, label: 'Experiences' },
  { key: '/agreements',     icon: <FileTextOutlined />,      label: 'Agreements' },
  { key: '/home-loans',     icon: <BankOutlined />,          label: 'Home Loans' },
  { key: '/legal-cases',    icon: <SafetyOutlined />,        label: 'Legal Cases' },
  { key: '/interiors',      icon: <FormatPainterOutlined />, label: 'Interiors' },
  { key: '/professionals', icon: <TeamOutlined />,           label: 'Professionals' },
  { key: '/pg-management', icon: <ToolOutlined />,           label: 'PG Management' },
  { key: '/users',         icon: <TeamOutlined />,           label: 'Users & Leads' },
];

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();

  // Highlight parent menu item for sub-routes (e.g. /hosts/123 → /hosts)
  const selectedKey = menuItems.find(m => location.pathname.startsWith(m.key))?.key || location.pathname;

  function handleLogout() {
    localStorage.removeItem('admin_token');
    navigate('/login');
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        theme="light"
        breakpoint="lg"
        collapsedWidth="0"
        style={{ borderRight: `1px solid ${token.colorBorderSecondary}` }}
      >
        <div style={{ padding: '20px 16px', fontWeight: 700, fontSize: 18, color: '#f97316' }}>
          Safar Admin
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>

      <Layout>
        <Header
          style={{
            background: '#fff',
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'flex-end',
            padding: '0 24px',
          }}
        >
          <Button
            icon={<LogoutOutlined />}
            onClick={handleLogout}
            type="text"
            danger
          >
            Logout
          </Button>
        </Header>

        <Content style={{ margin: 24, background: '#fff', borderRadius: 8, padding: 24, minHeight: 'calc(100vh - 112px)' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
