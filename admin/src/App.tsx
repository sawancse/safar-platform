import { Routes, Route, Navigate } from 'react-router-dom';
import AdminLayout from './components/AdminLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ListingsPage from './pages/ListingsPage';
import HostsPage from './pages/HostsPage';
import KycPage from './pages/KycPage';
import RevenuePage from './pages/RevenuePage';
import PayoutsPage from './pages/PayoutsPage';

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
        <Route path="listings" element={<ListingsPage />} />
        <Route path="hosts" element={<HostsPage />} />
        <Route path="kyc" element={<KycPage />} />
        <Route path="revenue" element={<RevenuePage />} />
        <Route path="payouts" element={<PayoutsPage />} />
      </Route>
    </Routes>
  );
}
