import { Suspense } from 'react';
import { CheckoutPage } from '../../features/booking/CheckoutPage';
import { RequireAuth } from '../../shared/auth/RequireAuth';

export default function Page() {
  return (
    
    <RequireAuth>
      <CheckoutPage />
    </RequireAuth>
    
  );
}
