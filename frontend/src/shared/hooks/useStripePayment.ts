'use client';

import { loadStripe, type Stripe } from '@stripe/stripe-js';
import { useCallback, useEffect, useState } from 'react';

type StripePaymentResult = {
  success: boolean;
  paymentIntentId?: string;
  error?: string;
};

/**
 * Stripe checkout hook for handling card payments.
 * 
 * @param publishableKey - Stripe publishable key from backend
 * @returns Stripe instance and payment handler
 */
export function useStripePayment(publishableKey: string | null) {
  const [stripe, setStripe] = useState<Stripe | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!publishableKey) {
      setStripe(null);
      return;
    }

    loadStripe(publishableKey).then((stripeInstance) => {
      setStripe(stripeInstance);
    }).catch((err) => {
      console.error('Failed to load Stripe:', err);
      setError('Failed to load payment system');
    });
  }, [publishableKey]);

  const processPayment = useCallback(async (
    clientSecret: string,
    returnUrl: string
  ): Promise<StripePaymentResult> => {
    if (!stripe) {
      return { success: false, error: 'Payment system not ready' };
    }

    setIsLoading(true);
    setError(null);

    try {
      const result = await stripe.confirmPayment({
        clientSecret,
        confirmParams: {
          return_url: returnUrl,
        },
        redirect: 'if_required',
      });

      if (result.error) {
        const errorMessage = result.error.message || 'Payment failed';
        setError(errorMessage);
        return { success: false, error: errorMessage };
      }

      if (result.paymentIntent && result.paymentIntent.status === 'succeeded') {
        return {
          success: true,
          paymentIntentId: result.paymentIntent.id,
        };
      }

      return { success: false, error: 'Payment was not completed' };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Payment failed';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setIsLoading(false);
    }
  }, [stripe]);

  const redirectToCheckout = useCallback(async (
    checkoutUrl: string
  ): Promise<void> => {
    if (!checkoutUrl) {
      setError('Checkout URL not available');
      return;
    }

    // Redirect to Stripe Checkout
    window.location.href = checkoutUrl;
  }, []);

  return {
    stripe,
    isLoading,
    error,
    processPayment,
    redirectToCheckout,
    clearError: () => setError(null),
  };
}

/**
 * Mock payment handler for development without Stripe.
 */
export function useMockPayment() {
  const [isLoading, setIsLoading] = useState(false);

  const processMockPayment = useCallback(async (
    bookingId: number,
    amount: number
  ): Promise<StripePaymentResult> => {
    setIsLoading(true);

    try {
      // Simulate API delay
      await new Promise((resolve) => setTimeout(resolve, 1500));

      // Simulate success (90% of the time)
      if (Math.random() > 0.1) {
        return {
          success: true,
          paymentIntentId: `pi_mock_${Date.now()}`,
        };
      }

      return {
        success: false,
        error: 'Simulated payment failure (mock mode)',
      };
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    isLoading,
    processMockPayment,
  };
}
