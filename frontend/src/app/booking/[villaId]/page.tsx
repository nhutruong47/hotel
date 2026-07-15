import { Suspense } from 'react';
import { BookingPage } from '../../../features/booking/BookingPage';
import { RequireAuth } from '../../../shared/auth/RequireAuth';

export default function Page() {
  return (
    
    <RequireAuth>
      <BookingPage />
    </RequireAuth>
    
  );
}
