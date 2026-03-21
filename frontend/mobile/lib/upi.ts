import { Linking, Alert, Platform } from 'react-native';

export interface UpiPaymentParams {
  /** Payee VPA (Virtual Payment Address), e.g. merchant@paytm */
  vpa: string;
  /** Payee display name */
  name: string;
  /** Amount in INR (rupees, not paise) */
  amount: number;
  /** Unique transaction reference */
  txnRef: string;
  /** Transaction note / description */
  note: string;
}

export type UpiPaymentResult =
  | { status: 'success'; txnRef: string }
  | { status: 'failure'; message: string }
  | { status: 'cancelled' };

/**
 * Build a UPI deep link URI from the given payment parameters.
 * Spec: https://www.npci.org.in/what-we-do/upi/upi-ecosystem
 */
export function buildUpiUri(params: UpiPaymentParams): string {
  const query = new URLSearchParams({
    pa: params.vpa,
    pn: params.name,
    am: params.amount.toFixed(2),
    tn: params.note,
    tr: params.txnRef,
    cu: 'INR',
  }).toString();

  return `upi://pay?${query}`;
}

/**
 * Launch a UPI payment intent on the device.
 *
 * Opens the user's installed UPI app (GPay, PhonePe, Paytm, etc.)
 * via Linking.openURL with the `upi://pay?...` deep link.
 *
 * Note: On iOS, UPI deep links may not be supported by all apps.
 * This approach works best on Android where the OS shows an app chooser.
 *
 * Returns a result indicating whether the launch was successful.
 * Actual payment verification must happen server-side via the
 * payment-service callback / webhook.
 */
export async function launchUpiPayment(
  params: UpiPaymentParams,
): Promise<UpiPaymentResult> {
  const uri = buildUpiUri(params);

  try {
    const canOpen = await Linking.canOpenURL(uri);

    if (!canOpen) {
      // No UPI app installed or platform doesn't support the scheme
      if (Platform.OS === 'ios') {
        Alert.alert(
          'UPI not available',
          'UPI payment apps may not be available on this device. Please use another payment method.',
        );
      } else {
        Alert.alert(
          'No UPI app found',
          'Please install a UPI-enabled app (Google Pay, PhonePe, Paytm, etc.) and try again.',
        );
      }
      return { status: 'failure', message: 'No UPI app available on this device' };
    }

    await Linking.openURL(uri);

    // The UPI app has been launched. The user will complete or cancel
    // the payment in the external app. When they return to our app,
    // we rely on the backend webhook / polling to confirm payment status.
    return { status: 'success', txnRef: params.txnRef };
  } catch (error: any) {
    const message = error?.message ?? 'Failed to launch UPI payment';
    Alert.alert('Payment Error', message);
    return { status: 'failure', message };
  }
}

/**
 * Generate a unique transaction reference for UPI payments.
 * Format: SAFAR-<timestamp>-<random>
 */
export function generateTxnRef(): string {
  const ts = Date.now().toString(36);
  const rand = Math.random().toString(36).substring(2, 8);
  return `SAFAR-${ts}-${rand}`.toUpperCase();
}
