import { Suspense } from 'react';
import { AdminDashboardPage } from '../../features/admin/AdminDashboardPage';
import { RequireAuth } from '../../shared/auth/RequireAuth';

export default function Page() {
  return (
    
    <RequireAuth>
      <AdminDashboardPage />
    </RequireAuth>
    
  );
}
