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
} from '@ant-design/icons';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/listings',  icon: <HomeOutlined />,      label: 'Listings' },
  { key: '/hosts',     icon: <TeamOutlined />,       label: 'Hosts' },
  { key: '/kyc',       icon: <SafetyOutlined />,     label: 'KYC Verification' },
  { key: '/revenue',  icon: <FundOutlined />,       label: 'Revenue' },
  { key: '/payouts',  icon: <BankOutlined />,       label: 'Payouts' },
];

export default function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();

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
          🧳 Safar Admin
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
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
