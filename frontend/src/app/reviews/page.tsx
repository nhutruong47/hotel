import { Suspense } from 'react';
import { ReviewsPage } from '../../features/profile/ProfilePages';
import { RequireAuth } from '../../shared/auth/RequireAuth';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <RequireAuth>
        <ReviewsPage />
      </RequireAuth>
    </Suspense>
  );
}
