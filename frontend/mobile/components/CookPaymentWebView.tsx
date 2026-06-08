import React, { useState, useRef } from 'react';
import { View, StyleSheet, ActivityIndicator, Text, TouchableOpacity } from 'react-native';
import { WebView, WebViewMessageEvent } from 'react-native-webview';
import { PaymentOrder, generateCheckoutHtml } from '../lib/payment';

/**
 * Razorpay checkout for cook / event bookings.
 *
 * Unlike the listing-booking RazorpayCheckout, this does NOT call the generic
 * /payments/verify endpoint. Cook bookings are confirmed via chef-specific
 * endpoints (confirm-payment / pay-balance), so we hand the raw Razorpay
 * response back to the caller via onSuccess.
 */
interface CookPaymentWebViewProps {
  order: PaymentOrder;
  prefill: { name: string; email: string; phone: string };
  onSuccess: (res: { paymentId: string; orderId: string; signature: string }) => void;
  onFailure: (error: string) => void;
  onDismiss: () => void;
}

export default function CookPaymentWebView({
  order, prefill, onSuccess, onFailure, onDismiss,
}: CookPaymentWebViewProps) {
  const [loading, setLoading] = useState(true);
  const webViewRef = useRef<WebView>(null);
  const html = generateCheckoutHtml(order, prefill);

  const handleMessage = (event: WebViewMessageEvent) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);
      switch (data.type) {
        case 'PAYMENT_SUCCESS':
          onSuccess({ paymentId: data.paymentId, orderId: data.orderId, signature: data.signature });
          break;
        case 'PAYMENT_FAILED':
          onFailure(data.error || 'Payment failed');
          break;
        case 'PAYMENT_DISMISSED':
          onDismiss();
          break;
      }
    } catch {
      onFailure('Invalid payment response');
    }
  };

  return (
    <View style={styles.container}>
      {loading && (
        <View style={styles.loader}>
          <ActivityIndicator size="large" color="#f97316" />
          <Text style={styles.loadingText}>Loading payment…</Text>
        </View>
      )}
      <WebView
        ref={webViewRef}
        source={{ html }}
        onLoadEnd={() => setLoading(false)}
        onMessage={handleMessage}
        javaScriptEnabled
        domStorageEnabled
        startInLoadingState
        style={styles.webview}
      />
      <TouchableOpacity style={styles.cancelButton} onPress={onDismiss}>
        <Text style={styles.cancelText}>Cancel Payment</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#fff' },
  webview:      { flex: 1 },
  loader:       { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, justifyContent: 'center', alignItems: 'center', backgroundColor: '#fff', zIndex: 10 },
  loadingText:  { marginTop: 12, fontSize: 16, color: '#64748b' },
  cancelButton: { padding: 16, alignItems: 'center', borderTopWidth: 1, borderTopColor: '#e2e8f0' },
  cancelText:   { fontSize: 16, color: '#ef4444', fontWeight: '600' },
});
