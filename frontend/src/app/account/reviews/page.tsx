import { Suspense } from 'react';
import { ReviewsPage } from '../../../features/account/ReviewsPage';

export default function Page() {
  return <Suspense fallback={null}><ReviewsPage /></Suspense>;
}
