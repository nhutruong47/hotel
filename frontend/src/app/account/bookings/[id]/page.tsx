import { Suspense } from 'react';
import { BookingDetailPage } from '../../../../features/account/BookingDetailPage';

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = await params;
  return (
    <Suspense fallback={null}>
      <BookingDetailPage bookingId={resolvedParams.id}/>
    </Suspense>
  );
}
