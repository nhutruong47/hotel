import { Suspense } from 'react';
import { RequireAuth } from '../../shared/auth/RequireAuth';
import { AccountShell } from '../../features/account/AccountShell';

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={null}>
      <RequireAuth>
        <AccountShell>{children}</AccountShell>
      </RequireAuth>
    </Suspense>
  );
}
