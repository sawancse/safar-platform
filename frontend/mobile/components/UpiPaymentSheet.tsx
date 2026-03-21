import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { launchUpiPayment, generateTxnRef, type UpiPaymentResult } from '@/lib/upi';

interface UpiPaymentSheetProps {
  /** Amount to pay in paise */
  amountPaise: number;
  /** Booking ID used as part of the transaction note */
  bookingId: string;
  /** Payee VPA — the merchant UPI address configured on the platform */
  merchantVpa?: string;
  /** Payee display name */
  merchantName?: string;
  /** Called after UPI app is launched (intent sent). Payment confirmation happens server-side. */
  onPaymentLaunched: (txnRef: string) => void;
  /** Called when the UPI launch fails or is cancelled */
  onPaymentFailed: (message: string) => void;
}

/**
 * UPI payment method component shown during the booking payment step.
 *
 * Renders:
 * - A UPI ID input field (e.g. user@paytm, user@ybl)
 * - A "Pay with UPI" button that launches the UPI intent
 * - Validation for the UPI ID format
 */
export default function UpiPaymentSheet({
  amountPaise,
  bookingId,
  merchantVpa = 'safar@ybl',
  merchantName = 'Safar',
  onPaymentLaunched,
  onPaymentFailed,
}: UpiPaymentSheetProps) {
  const [upiId, setUpiId] = useState('');
  const [launching, setLaunching] = useState(false);
  const [error, setError] = useState('');

  const amountRupees = amountPaise / 100;

  function validateUpiId(id: string): boolean {
    // UPI IDs follow the format: localpart@handle
    // localpart: alphanumeric, dots, hyphens (min 1 char)
    // handle: alphanumeric (min 1 char), e.g. ybl, paytm, oksbi, okhdfcbank
    const UPI_REGEX = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$/;
    return UPI_REGEX.test(id.trim());
  }

  async function handlePay() {
    setError('');

    const trimmedId = upiId.trim();
    if (!trimmedId) {
      setError('Please enter your UPI ID');
      return;
    }

    if (!validateUpiId(trimmedId)) {
      setError('Invalid UPI ID format. Example: yourname@paytm');
      return;
    }

    setLaunching(true);
    const txnRef = generateTxnRef();

    try {
      const result: UpiPaymentResult = await launchUpiPayment({
        vpa: merchantVpa,
        name: merchantName,
        amount: amountRupees,
        txnRef,
        note: `Safar Booking ${bookingId}`,
      });

      if (result.status === 'success') {
        onPaymentLaunched(result.txnRef);
      } else if (result.status === 'cancelled') {
        onPaymentFailed('Payment was cancelled');
      } else {
        onPaymentFailed(result.message);
      }
    } catch (e: any) {
      const msg = e?.message ?? 'UPI payment failed';
      setError(msg);
      onPaymentFailed(msg);
    } finally {
      setLaunching(false);
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerIcon}>UPI</Text>
        <Text style={styles.headerTitle}>Pay with UPI</Text>
      </View>

      <Text style={styles.description}>
        Enter your UPI ID to pay via Google Pay, PhonePe, Paytm, or any UPI app.
      </Text>

      <View style={styles.inputRow}>
        <TextInput
          style={[styles.input, error ? styles.inputError : null]}
          placeholder="e.g. yourname@paytm"
          placeholderTextColor="#9ca3af"
          value={upiId}
          onChangeText={(text) => {
            setUpiId(text);
            if (error) setError('');
          }}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="email-address"
          returnKeyType="done"
          editable={!launching}
        />
      </View>

      {error !== '' && <Text style={styles.errorText}>{error}</Text>}

      <View style={styles.amountRow}>
        <Text style={styles.amountLabel}>Amount</Text>
        <Text style={styles.amountValue}>
          {'\u20B9'}{amountRupees.toLocaleString('en-IN')}
        </Text>
      </View>

      <TouchableOpacity
        style={[styles.payBtn, launching && styles.payBtnDisabled]}
        onPress={handlePay}
        disabled={launching}
        activeOpacity={0.85}
      >
        {launching ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.payBtnText}>
            Pay {'\u20B9'}{amountRupees.toLocaleString('en-IN')} with UPI
          </Text>
        )}
      </TouchableOpacity>

      <Text style={styles.footnote}>
        You will be redirected to your UPI app to authorize the payment.
        Payment confirmation may take a few moments.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#f3f4f6',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  headerIcon: {
    backgroundColor: '#5b21b6',
    color: '#fff',
    fontSize: 11,
    fontWeight: '800',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    overflow: 'hidden',
    letterSpacing: 1,
  },
  headerTitle: {
    fontSize: 15,
    fontWeight: '600',
    color: '#111827',
    marginLeft: 8,
  },
  description: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 12,
    lineHeight: 18,
  },
  inputRow: {
    marginBottom: 4,
  },
  input: {
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    color: '#111827',
    backgroundColor: '#f9fafb',
  },
  inputError: {
    borderColor: '#ef4444',
  },
  errorText: {
    fontSize: 12,
    color: '#ef4444',
    marginTop: 4,
    marginBottom: 4,
  },
  amountRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
    marginBottom: 14,
    paddingVertical: 10,
    borderTopWidth: 1,
    borderTopColor: '#f3f4f6',
  },
  amountLabel: {
    fontSize: 14,
    color: '#6b7280',
  },
  amountValue: {
    fontSize: 16,
    fontWeight: '700',
    color: '#111827',
  },
  payBtn: {
    backgroundColor: '#5b21b6',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
  },
  payBtnDisabled: {
    opacity: 0.5,
  },
  payBtnText: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 15,
  },
  footnote: {
    fontSize: 11,
    color: '#9ca3af',
    marginTop: 10,
    lineHeight: 16,
    textAlign: 'center',
  },
});
