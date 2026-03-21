import axios from 'axios';
import * as SecureStore from 'expo-secure-store';

const API_BASE = 'http://localhost:8080/api/v1';

const getAuthHeaders = async () => {
  const token = await SecureStore.getItemAsync('access_token');
  return { Authorization: `Bearer ${token}` };
};

export interface PaymentOrder {
  orderId: string;
  amount: number; // in paise
  currency: string;
  bookingId: string;
  razorpayKeyId: string;
}

export interface PaymentResult {
  success: boolean;
  paymentId?: string;
  orderId?: string;
  signature?: string;
  error?: string;
}

/**
 * Create a Razorpay order for a booking.
 */
export async function createPaymentOrder(bookingId: string): Promise<PaymentOrder> {
  const headers = await getAuthHeaders();
  const response = await axios.post(
    `${API_BASE}/payments/order`,
    { bookingId },
    { headers }
  );
  return response.data;
}

/**
 * Verify payment after Razorpay checkout.
 */
export async function verifyPayment(
  paymentId: string,
  orderId: string,
  signature: string
): Promise<{ verified: boolean }> {
  const headers = await getAuthHeaders();
  const response = await axios.post(
    `${API_BASE}/payments/verify`,
    { paymentId, orderId, signature },
    { headers }
  );
  return response.data;
}

/**
 * Get payment status for a booking.
 */
export async function getPaymentStatus(bookingId: string): Promise<{
  status: string;
  paymentId?: string;
  amount: number;
}> {
  const headers = await getAuthHeaders();
  const response = await axios.get(
    `${API_BASE}/payments/status/${bookingId}`,
    { headers }
  );
  return response.data;
}

/**
 * Generate Razorpay checkout HTML for WebView.
 */
export function generateCheckoutHtml(order: PaymentOrder, prefill: {
  name: string;
  email: string;
  phone: string;
}): string {
  return `
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
</head>
<body>
  <script>
    var options = {
      key: '${order.razorpayKeyId}',
      amount: ${order.amount},
      currency: '${order.currency}',
      name: 'Safar',
      description: 'Booking Payment',
      order_id: '${order.orderId}',
      prefill: {
        name: '${prefill.name}',
        email: '${prefill.email}',
        contact: '${prefill.phone}'
      },
      theme: { color: '#f97316' },
      handler: function(response) {
        window.ReactNativeWebView.postMessage(JSON.stringify({
          type: 'PAYMENT_SUCCESS',
          paymentId: response.razorpay_payment_id,
          orderId: response.razorpay_order_id,
          signature: response.razorpay_signature
        }));
      },
      modal: {
        ondismiss: function() {
          window.ReactNativeWebView.postMessage(JSON.stringify({
            type: 'PAYMENT_DISMISSED'
          }));
        }
      }
    };
    var rzp = new Razorpay(options);
    rzp.on('payment.failed', function(response) {
      window.ReactNativeWebView.postMessage(JSON.stringify({
        type: 'PAYMENT_FAILED',
        error: response.error.description
      }));
    });
    rzp.open();
  </script>
</body>
</html>`;
}
