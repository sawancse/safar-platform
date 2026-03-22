'use client';

import { useState } from 'react';
import { api } from '@/lib/api';

const DEV_MOCK_PAYMENT = process.env.NEXT_PUBLIC_MOCK_PAYMENT === 'true';

interface Props {
  bookingId: string;
  amountPaise: number;
  token: string;
  onSuccess: (paymentId: string) => void;
}

declare global {
  interface Window {
    Razorpay: any;
  }
}

export default function RazorpayButton({ bookingId, amountPaise, token, onSuccess }: Props) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleMockPay() {
    setLoading(true);
    setError('');
    // Simulate 1.5s payment delay
    await new Promise((r) => setTimeout(r, 1500));
    onSuccess('mock_pay_' + Date.now());
  }

  async function handlePay() {
    if (DEV_MOCK_PAYMENT) {
      return handleMockPay();
    }

    setLoading(true);
    setError('');
    try {
      const order = await api.createPaymentOrder(bookingId, amountPaise, token);

      // Load Razorpay script dynamically
      if (!window.Razorpay) {
        await new Promise<void>((resolve, reject) => {
          const script = document.createElement('script');
          script.src = 'https://checkout.razorpay.com/v1/checkout.js';
          script.onload = () => resolve();
          script.onerror = () => reject(new Error('Failed to load Razorpay'));
          document.body.appendChild(script);
        });
      }

      const rzp = new window.Razorpay({
        key: order.razorpayKeyId,
        amount: order.amountPaise,
        currency: 'INR',
        name: 'Safar',
        description: 'Property Booking',
        order_id: order.razorpayOrderId,
        handler: async function (response: any) {
          try {
            await api.verifyPayment(
              {
                bookingId,
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature,
              },
              token
            );
            onSuccess(response.razorpay_payment_id);
          } catch {
            setError('Payment verification failed. Please contact support.');
          }
        },
        prefill: {},
        theme: { color: '#f97316' },
        modal: {
          ondismiss: () => setLoading(false),
        },
      });

      rzp.on('payment.failed', (response: any) => {
        setError(response.error?.description || 'Payment failed');
        setLoading(false);
      });

      rzp.open();
    } catch (e: any) {
      setError(e.message || 'Failed to initiate payment');
      setLoading(false);
    }
  }

  return (
    <div>
      {error && (
        <div className="bg-red-50 text-red-600 rounded-xl px-4 py-3 text-sm mb-4">
          {error}
        </div>
      )}
      {DEV_MOCK_PAYMENT && (
        <p className="text-xs text-amber-500 text-center mb-2">Dev mode: Payment will be simulated</p>
      )}
      <button
        onClick={handlePay}
        disabled={loading}
        className="w-full bg-green-500 hover:bg-green-600 text-white font-semibold py-3 rounded-xl disabled:opacity-50 transition"
      >
        {loading ? 'Processing payment...' : `Pay ₹${(amountPaise / 100).toLocaleString('en-IN')}`}
      </button>
    </div>
  );
}
