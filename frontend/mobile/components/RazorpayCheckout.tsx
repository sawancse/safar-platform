import React, { useState, useRef } from 'react';
import { View, StyleSheet, ActivityIndicator, Text, TouchableOpacity } from 'react-native';
import { WebView, WebViewMessageEvent } from 'react-native-webview';
import { PaymentOrder, PaymentResult, generateCheckoutHtml, verifyPayment } from '../lib/payment';

interface RazorpayCheckoutProps {
  order: PaymentOrder;
  prefill: { name: string; email: string; phone: string };
  onSuccess: (result: PaymentResult) => void;
  onFailure: (error: string) => void;
  onDismiss: () => void;
}

export default function RazorpayCheckout({
  order, prefill, onSuccess, onFailure, onDismiss,
}: RazorpayCheckoutProps) {
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const webViewRef = useRef<WebView>(null);

  const html = generateCheckoutHtml(order, prefill);

  const handleMessage = async (event: WebViewMessageEvent) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);

      switch (data.type) {
        case 'PAYMENT_SUCCESS':
          setVerifying(true);
          try {
            const verification = await verifyPayment(
              data.paymentId, data.orderId, data.signature
            );
            if (verification.verified) {
              onSuccess({
                success: true,
                paymentId: data.paymentId,
                orderId: data.orderId,
                signature: data.signature,
              });
            } else {
              onFailure('Payment verification failed');
            }
          } catch {
            onFailure('Payment verification error');
          }
          setVerifying(false);
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

  if (verifying) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" color="#f97316" />
        <Text style={styles.verifyText}>Verifying payment...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {loading && (
        <View style={styles.loader}>
          <ActivityIndicator size="large" color="#f97316" />
          <Text style={styles.loadingText}>Loading payment...</Text>
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
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  webview: {
    flex: 1,
  },
  loader: {
    position: 'absolute',
    top: 0, left: 0, right: 0, bottom: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    zIndex: 10,
  },
  loadingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#64748b',
  },
  verifyText: {
    marginTop: 16,
    fontSize: 18,
    fontWeight: '600',
    color: '#1e293b',
  },
  cancelButton: {
    padding: 16,
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: '#e2e8f0',
  },
  cancelText: {
    fontSize: 16,
    color: '#ef4444',
    fontWeight: '600',
  },
});
