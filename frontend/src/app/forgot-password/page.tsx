import { Suspense } from 'react';
import { ForgotPasswordPage } from '../../features/auth/AuthPages';

export default function Page() {
  return (
    <Suspense fallback={null}>
      <ForgotPasswordPage />
    </Suspense>
  );
}
