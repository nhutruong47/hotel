import { Suspense } from 'react';
import { VerifyEmailPage } from '../../features/auth/AuthPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <VerifyEmailPage />
    </Suspense>
  );
}
