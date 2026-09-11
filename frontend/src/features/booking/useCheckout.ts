'use client';

import { useCallback, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { api, API_PATHS, ApiError } from '../../shared/api/client';
import { useStripePayment, useMockPayment } from '../../shared/hooks/useStripePayment';

type CheckoutMode = 'stripe' | 'mock' | 'bank-transfer';

type CreateBookingRequest = {
  roomId: number;
  checkIn: string;
  checkOut: string;
  guestName: string;
  guestEmail: string;
  guestPhone: string;
  notes?: string;
  voucherCode?: string;
  guests: number;
  paymentMethod: string;
};

type CreateBookingResponse = {
  booking: {
    id: number;
    status: string;
    totalPrice: number;
  };
  reference: string;
};

type StripeCheckoutResponse = {
  mode: 'mock' | 'real';
  sessionId?: string;
  checkoutUrl?: string;
  paymentIntentId?: string;
  amount?: number;
  currency?: string;
};

type BookingSuccessData = {
  bookingId: number;
  reference: string;
  guestName: string;
  email: string;
  phone: string;
  paymentMethod: string;
  status: string;
};

type UseCheckoutReturn = {
  // State
  isProcessing: boolean;
  error: string | null;
  checkoutMode: CheckoutMode;
  
  // Actions
  createBooking: (data: CreateBookingRequest) => Promise<CreateBookingResponse>;
  initiateStripeCheckout: (bookingId: number, successUrl?: string, cancelUrl?: string) => Promise<void>;
  simulateMockPayment: (bookingId: number, amount: number) => Promise<BookingSuccessData | null>;
  
  // Stripe helpers
  stripeIsLoading: boolean;
  stripeError: string | null;
  processStripePayment: (clientSecret: string, returnUrl: string) => Promise<{ success: boolean; error?: string }>;
  
  // Utils
  clearError: () => void;
  setCheckoutMode: (mode: CheckoutMode) => void;
};

export function useCheckout(): UseCheckoutReturn {
  const [error, setError] = useState<string | null>(null);
  const [checkoutMode, setCheckoutMode] = useState<CheckoutMode>('stripe');
  const [stripePublishableKey, setStripePublishableKey] = useState<string | null>(null);

  // Initialize Stripe with publishable key
  const { 
    isLoading: stripeIsLoading, 
    error: stripeError, 
    processPayment: processStripePayment,
    redirectToCheckout,
    clearError: clearStripeError,
  } = useStripePayment(stripePublishableKey);

  const { isLoading: mockIsLoading, processMockPayment } = useMockPayment();

  // Fetch Stripe publishable key
  const fetchStripeKey = useCallback(async () => {
    try {
      // In a real app, this would be a dedicated endpoint
      // For now, we'll use environment variable pattern
      const key = process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY;
      if (key) {
        setStripePublishableKey(key);
      }
    } catch (err) {
      console.warn('Stripe key not configured');
    }
  }, []);

  // Create booking mutation
  const bookingMutation = useMutation<
    CreateBookingResponse,
    ApiError,
    CreateBookingRequest
  >({
    mutationFn: async (data) => {
      return api.post<CreateBookingResponse>(API_PATHS.bookings.list, data);
    },
    onError: (err) => {
      setError(err.message || 'Failed to create booking');
    },
  });

  // Mock/bank-transfer flows may register a pending payment request, but only
  // a verified webhook or an admin can confirm it as paid.
  const submitPaymentMutation = useMutation<
    { payment: any },
    ApiError,
    { bookingId: number; transactionRef?: string; amount: number }
  >({
    mutationFn: async ({ bookingId, transactionRef, amount }) => {
      return api.post<{ payment: any }>(
        API_PATHS.payments.create,
        {
          bookingId,
          transactionRef,
          amount,
          method: checkoutMode === 'stripe' ? 'CARD' : 'BANK_TRANSFER',
        }
      );
    },
  });

  // Create booking
  const createBooking = useCallback(async (data: CreateBookingRequest): Promise<CreateBookingResponse> => {
    setError(null);
    clearStripeError();
    
    const result = await bookingMutation.mutateAsync(data);
    return result;
  }, [bookingMutation, clearStripeError]);

  // Initiate Stripe Checkout
  const initiateStripeCheckout = useCallback(async (
    bookingId: number,
    successUrl?: string,
    cancelUrl?: string
  ): Promise<void> => {
    setError(null);
    
    try {
      const response = await api.post<StripeCheckoutResponse>(
        API_PATHS.payments.stripeCheckout,
        {
          bookingId,
          successUrl: successUrl || `${window.location.origin}/booking/success`,
          cancelUrl: cancelUrl || `${window.location.origin}/checkout`,
        }
      );

      if (response.mode === 'mock') {
        // Mock mode - simulate payment
        return;
      }

      if (response.checkoutUrl) {
        // Real Stripe - redirect to checkout
        await redirectToCheckout(response.checkoutUrl);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Failed to initiate checkout');
      }
      throw err;
    }
  }, [redirectToCheckout]);

  // Simulate mock payment
  const simulateMockPayment = useCallback(async (
    bookingId: number,
    amount: number
  ): Promise<BookingSuccessData | null> => {
    setError(null);
    
    try {
      // Process mock payment
      const mockResult = await processMockPayment(bookingId, amount);
      
      if (!mockResult.success) {
        setError(mockResult.error || 'Payment failed');
        return null;
      }

      // Register the attempt. This intentionally leaves the booking pending;
      // client-side mock success is never trusted as proof of payment.
      await submitPaymentMutation.mutateAsync({
        bookingId,
        transactionRef: mockResult.paymentIntentId,
        amount,
      });

      // Get booking details
      const booking = await api.get<any>(API_PATHS.bookings.booking(bookingId));

      // Store success data
      const successData: BookingSuccessData = {
        bookingId,
        reference: `NV-${bookingId}`,
        guestName: booking.guestName,
        email: booking.guestEmail,
        phone: booking.guestPhone,
        paymentMethod: checkoutMode === 'stripe' ? 'Card' : 'Bank Transfer',
        status: 'Pending payment verification',
      };

      return successData;
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Payment processing failed');
      }
      return null;
    }
  }, [checkoutMode, processMockPayment, submitPaymentMutation]);

  // Clear errors
  const clearError = useCallback(() => {
    setError(null);
    clearStripeError();
  }, [clearStripeError]);

  // Initialize
  if (!stripePublishableKey) {
    fetchStripeKey();
  }

  return {
    isProcessing: bookingMutation.isPending || submitPaymentMutation.isPending || mockIsLoading,
    error,
    checkoutMode,
    
    createBooking,
    initiateStripeCheckout,
    simulateMockPayment,
    
    stripeIsLoading,
    stripeError,
    processStripePayment,
    
    clearError,
    setCheckoutMode,
  };
}
