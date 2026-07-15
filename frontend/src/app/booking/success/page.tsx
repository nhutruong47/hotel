import { Suspense } from 'react';
import { BookingSuccessPage } from '../../../features/booking/BookingSuccessPage';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <BookingSuccessPage />
    </Suspense>
  );
}
