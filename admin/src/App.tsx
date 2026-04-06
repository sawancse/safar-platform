import { Routes, Route, Navigate } from 'react-router-dom';
import AdminLayout from './components/AdminLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ListingsPage from './pages/ListingsPage';
import HostsPage from './pages/HostsPage';
import HostDetailPage from './pages/HostDetailPage';
import KycPage from './pages/KycPage';
import RevenuePage from './pages/RevenuePage';
import PayoutsPage from './pages/PayoutsPage';
import BookingsPage from './pages/BookingsPage';
import GuestsPage from './pages/GuestsPage';
import ChannelManagerPage from './pages/ChannelManagerPage';
import SalePropertiesPage from './pages/SalePropertiesPage';
import RoomOccupancyPage from './pages/RoomOccupancyPage';
import CooksPage from './pages/CooksPage';
import DonorsPage from './pages/DonorsPage';
import ExperiencesPage from './pages/ExperiencesPage';
import AgreementsPage from './pages/AgreementsPage';
import HomeLoanPage from './pages/HomeLoanPage';
import LegalCasesPage from './pages/LegalCasesPage';
import InteriorsPage from './pages/InteriorsPage';
import BuilderProjectsPage from './pages/BuilderProjectsPage';
import PgManagementPage from './pages/PgManagementPage';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('admin_token');
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="bookings" element={<BookingsPage />} />
        <Route path="listings" element={<ListingsPage />} />
        <Route path="hosts" element={<HostsPage />} />
        <Route path="hosts/:hostId" element={<HostDetailPage />} />
        <Route path="guests" element={<GuestsPage />} />
        <Route path="kyc" element={<KycPage />} />
        <Route path="revenue" element={<RevenuePage />} />
        <Route path="payouts" element={<PayoutsPage />} />
        <Route path="channel-manager" element={<ChannelManagerPage />} />
        <Route path="sale-properties" element={<SalePropertiesPage />} />
        <Route path="builder-projects" element={<BuilderProjectsPage />} />
        <Route path="room-occupancy" element={<RoomOccupancyPage />} />
        <Route path="cooks" element={<CooksPage />} />
        <Route path="donors" element={<DonorsPage />} />
        <Route path="experiences" element={<ExperiencesPage />} />
        <Route path="agreements" element={<AgreementsPage />} />
        <Route path="home-loans" element={<HomeLoanPage />} />
        <Route path="legal-cases" element={<LegalCasesPage />} />
        <Route path="interiors" element={<InteriorsPage />} />
        <Route path="pg-management" element={<PgManagementPage />} />
      </Route>
    </Routes>
  );
}
