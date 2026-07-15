import { BookingDetailPage } from '../../../../features/account/BookingDetailPage';

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = await params;
  return <BookingDetailPage bookingId={resolvedParams.id} />;
}
