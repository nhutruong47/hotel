import { Suspense } from 'react';
import { BookingPage } from '../../../features/booking/BookingPage';
import { RequireAuth } from '../../../shared/auth/RequireAuth';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <RequireAuth>
        <BookingPage />
      </RequireAuth>
    </Suspense>
  );
}
