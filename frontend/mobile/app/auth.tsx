import { useState, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
  Alert,
  ScrollView,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { saveTokens } from '@/lib/auth';

type Step =
  | 'input'       // phone or email entry
  | 'otp'         // phone OTP verification
  | 'email-otp'   // email OTP verification
  | 'password'    // email password sign-in
  | 'set-password' // optional: set password after OTP login
  | 'forgot-password'; // reset password via OTP

type InputMode = 'phone' | 'email';

function getPasswordStrength(pw: string): { label: string; color: string } {
  if (pw.length < 6) return { label: 'Too short', color: '#dc2626' };
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  if (score <= 1) return { label: 'Weak', color: '#f97316' };
  if (score === 2) return { label: 'Fair', color: '#eab308' };
  if (score === 3) return { label: 'Good', color: '#22c55e' };
  return { label: 'Strong', color: '#16a34a' };
}

export default function AuthScreen() {
  const router = useRouter();

  // shared state
  const [step, setStep] = useState<Step>('input');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // input step
  const [inputMode, setInputMode] = useState<InputMode>('phone');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');

  // OTP
  const [otp, setOtp] = useState('');
  const [name, setName] = useState('');

  // password
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [hasPassword, setHasPassword] = useState(false);

  // set-password / forgot-password
  const [newPassword, setNewPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [forgotOtp, setForgotOtp] = useState('');

  const resetToInput = useCallback(() => {
    setStep('input');
    setOtp('');
    setPassword('');
    setNewPassword('');
    setForgotOtp('');
    setError('');
    setShowPassword(false);
    setShowNewPassword(false);
  }, []);

  const isValidEmail = (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

  // ─── Phone OTP ───────────────────────────────────────────
  async function sendPhoneOtp() {
    setLoading(true);
    setError('');
    try {
      await api.sendOtp(`+91${phone}`);
      setStep('otp');
    } catch (e: any) {
      setError(e.message || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  }

  async function verifyPhoneOtp() {
    setLoading(true);
    setError('');
    try {
      const auth = await api.verifyOtp(`+91${phone}`, otp, name || undefined);
      await saveTokens(auth);
      router.back();
    } catch (e: any) {
      setError(e.message || 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  }

  // ─── Email flow ──────────────────────────────────────────
  async function handleEmailContinue() {
    setLoading(true);
    setError('');
    try {
      const result = await api.checkAuthMethod(email);
      if (result.hasPassword) {
        setHasPassword(true);
        setStep('password');
      } else {
        // no password set — send email OTP
        await api.sendEmailOtp(email);
        setHasPassword(false);
        setStep('email-otp');
      }
    } catch (e: any) {
      // if check-method fails (new user), send OTP directly
      try {
        await api.sendEmailOtp(email);
        setHasPassword(false);
        setStep('email-otp');
      } catch (e2: any) {
        setError(e2.message || 'Failed to continue');
      }
    } finally {
      setLoading(false);
    }
  }

  async function handlePasswordSignIn() {
    setLoading(true);
    setError('');
    try {
      const auth = await api.passwordSignIn(email, password);
      await saveTokens(auth);
      router.back();
    } catch (e: any) {
      setError(e.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  }

  async function handleEmailOtpVerify() {
    setLoading(true);
    setError('');
    try {
      const auth = await api.verifyEmailOtp(email, otp, name || undefined);
      await saveTokens(auth);
      // offer set-password for existing users without password
      if (!hasPassword) {
        setStep('set-password');
      } else {
        router.back();
      }
    } catch (e: any) {
      setError(e.message || 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  }

  async function handleSetPassword() {
    setLoading(true);
    setError('');
    try {
      const { getAccessToken } = require('@/lib/auth');
      const token = await getAccessToken();
      if (token) {
        await api.setPassword(newPassword, token);
      }
      router.back();
    } catch (e: any) {
      setError(e.message || 'Failed to set password');
    } finally {
      setLoading(false);
    }
  }

  async function handleForgotPassword() {
    setLoading(true);
    setError('');
    try {
      await api.sendEmailOtp(email);
      setStep('forgot-password');
    } catch (e: any) {
      setError(e.message || 'Failed to send reset code');
    } finally {
      setLoading(false);
    }
  }

  async function handleResetPassword() {
    setLoading(true);
    setError('');
    try {
      const auth = await api.resetPassword(email, forgotOtp, newPassword);
      await saveTokens(auth);
      router.back();
    } catch (e: any) {
      setError(e.message || 'Failed to reset password');
    } finally {
      setLoading(false);
    }
  }

  async function switchToEmailOtp() {
    setLoading(true);
    setError('');
    try {
      await api.sendEmailOtp(email);
      setOtp('');
      setStep('email-otp');
    } catch (e: any) {
      setError(e.message || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  }

  // ─── Subtitle helper ────────────────────────────────────
  function getSubtitle(): string {
    switch (step) {
      case 'input':
        return inputMode === 'phone'
          ? 'Enter your Indian mobile number'
          : 'Enter your email address';
      case 'otp':
        return `OTP sent to +91 ${phone}`;
      case 'email-otp':
        return `OTP sent to ${email}`;
      case 'password':
        return 'Welcome back';
      case 'set-password':
        return 'Set a password for faster sign-ins';
      case 'forgot-password':
        return 'Reset your password';
      default:
        return '';
    }
  }

  // ─── Render helpers ──────────────────────────────────────
  function renderInput() {
    if (inputMode === 'phone') {
      return (
        <>
          <View style={styles.phoneRow}>
            <Text style={styles.countryCode}>+91</Text>
            <TextInput
              style={styles.phoneInput}
              placeholder="9876543210"
              keyboardType="number-pad"
              maxLength={10}
              value={phone}
              onChangeText={(t) => setPhone(t.replace(/\D/g, ''))}
            />
          </View>
          <TouchableOpacity
            style={[styles.btn, (loading || phone.length !== 10) && styles.btnDisabled]}
            disabled={loading || phone.length !== 10}
            onPress={sendPhoneOtp}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.btnText}>Send OTP</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => { setInputMode('email'); setError(''); }}
            style={styles.switchLink}
          >
            <Text style={styles.linkText}>Use email instead</Text>
          </TouchableOpacity>
        </>
      );
    }

    // email input
    return (
      <>
        <TextInput
          style={styles.emailInput}
          placeholder="you@example.com"
          keyboardType="email-address"
          autoCapitalize="none"
          autoComplete="email"
          value={email}
          onChangeText={setEmail}
        />
        <TouchableOpacity
          style={[styles.btn, (loading || !isValidEmail(email)) && styles.btnDisabled]}
          disabled={loading || !isValidEmail(email)}
          onPress={handleEmailContinue}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.btnText}>Continue</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => { setInputMode('phone'); setError(''); }}
          style={styles.switchLink}
        >
          <Text style={styles.linkText}>Use phone instead</Text>
        </TouchableOpacity>
      </>
    );
  }

  function renderPhoneOtp() {
    return (
      <>
        <TextInput
          style={styles.otpInput}
          placeholder="000000"
          keyboardType="number-pad"
          maxLength={6}
          textContentType="oneTimeCode"
          autoComplete="sms-otp"
          value={otp}
          onChangeText={(t) => setOtp(t.replace(/\D/g, ''))}
        />
        <TextInput
          style={styles.nameInput}
          placeholder="Your name (first time only)"
          value={name}
          onChangeText={setName}
        />
        <TouchableOpacity
          style={[styles.btn, (loading || otp.length !== 6) && styles.btnDisabled]}
          disabled={loading || otp.length !== 6}
          onPress={verifyPhoneOtp}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.btnText}>Verify OTP</Text>
          )}
        </TouchableOpacity>
        <TouchableOpacity onPress={resetToInput}>
          <Text style={styles.backLink}>Change number</Text>
        </TouchableOpacity>
      </>
    );
  }

  function renderEmailOtp() {
    return (
      <>
        <TextInput
          style={styles.otpInput}
          placeholder="000000"
          keyboardType="number-pad"
          maxLength={6}
          textContentType="oneTimeCode"
          value={otp}
          onChangeText={(t) => setOtp(t.replace(/\D/g, ''))}
        />
        <TextInput
          style={styles.nameInput}
          placeholder="Your name (first time only)"
          value={name}
          onChangeText={setName}
        />
        <TouchableOpacity
          style={[styles.btn, (loading || otp.length !== 6) && styles.btnDisabled]}
          disabled={loading || otp.length !== 6}
          onPress={handleEmailOtpVerify}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.btnText}>Verify OTP</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity
          onPress={async () => {
            setLoading(true);
            try { await api.sendEmailOtp(email); setOtp(''); } catch {}
            setLoading(false);
          }}
        >
          <Text style={styles.linkText}>Resend OTP</Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={resetToInput}>
          <Text style={styles.backLink}>Change email</Text>
        </TouchableOpacity>
      </>
    );
  }

  function renderPasswordStep() {
    return (
      <>
        <Text style={styles.emailLabel}>{email}</Text>

        <View style={styles.passwordRow}>
          <TextInput
            style={styles.passwordInput}
            placeholder="Password"
            secureTextEntry={!showPassword}
            autoCapitalize="none"
            value={password}
            onChangeText={setPassword}
          />
          <TouchableOpacity
            style={styles.eyeBtn}
            onPress={() => setShowPassword(!showPassword)}
          >
            <Text style={styles.eyeText}>{showPassword ? 'Hide' : 'Show'}</Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={[styles.btn, (loading || password.length < 6) && styles.btnDisabled]}
          disabled={loading || password.length < 6}
          onPress={handlePasswordSignIn}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.btnText}>Sign In</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity onPress={handleForgotPassword}>
          <Text style={styles.linkText}>Forgot password?</Text>
        </TouchableOpacity>

        <View style={styles.dividerRow}>
          <View style={styles.dividerLine} />
          <Text style={styles.dividerText}>or</Text>
          <View style={styles.dividerLine} />
        </View>

        <TouchableOpacity onPress={switchToEmailOtp}>
          <Text style={styles.linkText}>Use OTP instead</Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={resetToInput}>
          <Text style={styles.backLink}>Change email</Text>
        </TouchableOpacity>
      </>
    );
  }

  function renderSetPassword() {
    const strength = getPasswordStrength(newPassword);
    return (
      <>
        <Text style={styles.helperText}>You can skip OTP next time</Text>

        <View style={styles.passwordRow}>
          <TextInput
            style={styles.passwordInput}
            placeholder="Create a password"
            secureTextEntry={!showNewPassword}
            autoCapitalize="none"
            value={newPassword}
            onChangeText={setNewPassword}
          />
          <TouchableOpacity
            style={styles.eyeBtn}
            onPress={() => setShowNewPassword(!showNewPassword)}
          >
            <Text style={styles.eyeText}>{showNewPassword ? 'Hide' : 'Show'}</Text>
          </TouchableOpacity>
        </View>

        {newPassword.length > 0 && (
          <Text style={[styles.strengthText, { color: strength.color }]}>
            Password strength: {strength.label}
          </Text>
        )}

        <TouchableOpacity
          style={[styles.btn, (loading || newPassword.length < 6) && styles.btnDisabled]}
          disabled={loading || newPassword.length < 6}
          onPress={handleSetPassword}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.btnText}>Set Password</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.linkText}>Maybe Later</Text>
        </TouchableOpacity>
      </>
    );
  }

  function renderForgotPassword() {
    const strength = getPasswordStrength(newPassword);
    return (
      <>
        <Text style={styles.helperText}>Enter the code sent to {email}</Text>

        <TextInput
          style={styles.otpInput}
          placeholder="000000"
          keyboardType="number-pad"
          maxLength={6}
          textContentType="oneTimeCode"
          value={forgotOtp}
          onChangeText={(t) => setForgotOtp(t.replace(/\D/g, ''))}
        />

        <View style={styles.passwordRow}>
          <TextInput
            style={styles.passwordInput}
            placeholder="New password"
            secureTextEntry={!showNewPassword}
            autoCapitalize="none"
            value={newPassword}
            onChangeText={setNewPassword}
          />
          <TouchableOpacity
            style={styles.eyeBtn}
            onPress={() => setShowNewPassword(!showNewPassword)}
          >
            <Text style={styles.eyeText}>{showNewPassword ? 'Hide' : 'Show'}</Text>
          </TouchableOpacity>
        </View>

        {newPassword.length > 0 && (
          <Text style={[styles.strengthText, { color: strength.color }]}>
            Password strength: {strength.label}
          </Text>
        )}

        <TouchableOpacity
          style={[
            styles.btn,
            (loading || forgotOtp.length !== 6 || newPassword.length < 6) && styles.btnDisabled,
          ]}
          disabled={loading || forgotOtp.length !== 6 || newPassword.length < 6}
          onPress={handleResetPassword}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.btnText}>Reset Password</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity
          onPress={async () => {
            setLoading(true);
            try { await api.sendEmailOtp(email); setForgotOtp(''); } catch {}
            setLoading(false);
          }}
        >
          <Text style={styles.linkText}>Resend OTP</Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={() => { setStep('password'); setError(''); setNewPassword(''); setForgotOtp(''); }}>
          <Text style={styles.backLink}>Back to password</Text>
        </TouchableOpacity>
      </>
    );
  }

  // ─── Main render ─────────────────────────────────────────
  function renderStepContent() {
    switch (step) {
      case 'input':       return renderInput();
      case 'otp':         return renderPhoneOtp();
      case 'email-otp':   return renderEmailOtp();
      case 'password':    return renderPasswordStep();
      case 'set-password': return renderSetPassword();
      case 'forgot-password': return renderForgotPassword();
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.card}>
          <Text style={styles.logoIcon}>🧳</Text>
          <Text style={styles.title}>Sign in to Safar</Text>
          <Text style={styles.subtitle}>{getSubtitle()}</Text>

          {error !== '' && (
            <View style={styles.errorBox}>
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          {renderStepContent()}

          {/* Google Sign-In — shown only on input step */}
          {step === 'input' && (
            <>
              <View style={styles.dividerRow}>
                <View style={styles.dividerLine} />
                <Text style={styles.dividerText}>or</Text>
                <View style={styles.dividerLine} />
              </View>

              <TouchableOpacity
                style={styles.googleBtn}
                onPress={() => {
                  Alert.alert(
                    'Google Sign-In',
                    'Google Sign-In requires additional setup. Please configure GOOGLE_CLIENT_ID in your environment and install expo-auth-session.',
                    [{ text: 'OK' }]
                  );
                }}
              >
                <Text style={styles.googleBtnText}>Continue with Google</Text>
              </TouchableOpacity>
            </>
          )}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  scrollContent: { flexGrow: 1, justifyContent: 'center', padding: 24 },
  card:          { backgroundColor: '#fff', borderRadius: 20, padding: 28, shadowColor: '#000', shadowOpacity: 0.06, shadowRadius: 12, elevation: 4 },
  logoIcon:      { fontSize: 40, textAlign: 'center', marginBottom: 8 },
  title:         { fontSize: 22, fontWeight: '700', textAlign: 'center', color: '#111827' },
  subtitle:      { fontSize: 13, color: '#6b7280', textAlign: 'center', marginTop: 4, marginBottom: 20 },
  errorBox:      { backgroundColor: '#fef2f2', borderRadius: 12, padding: 12, marginBottom: 12 },
  errorText:     { color: '#dc2626', fontSize: 13 },
  phoneRow:      { flexDirection: 'row', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, overflow: 'hidden', marginBottom: 12 },
  countryCode:   { backgroundColor: '#f3f4f6', paddingHorizontal: 12, paddingVertical: 14, fontSize: 14, color: '#374151', fontWeight: '600', borderRightWidth: 1, borderRightColor: '#e5e7eb' },
  phoneInput:    { flex: 1, paddingHorizontal: 12, fontSize: 15, color: '#111827' },
  emailInput:    { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingHorizontal: 14, paddingVertical: 14, fontSize: 15, color: '#111827', marginBottom: 12 },
  emailLabel:    { fontSize: 14, color: '#6b7280', textAlign: 'center', marginBottom: 12 },
  otpInput:      { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, textAlign: 'center', fontSize: 24, letterSpacing: 6, paddingVertical: 14, marginBottom: 10, fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace' },
  nameInput:     { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, fontSize: 14, marginBottom: 12 },
  passwordRow:   { flexDirection: 'row', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, overflow: 'hidden', marginBottom: 12 },
  passwordInput: { flex: 1, paddingHorizontal: 14, paddingVertical: 14, fontSize: 15, color: '#111827' },
  eyeBtn:        { justifyContent: 'center', paddingHorizontal: 14, backgroundColor: '#f3f4f6' },
  eyeText:       { fontSize: 13, color: '#6b7280', fontWeight: '600' },
  helperText:    { fontSize: 13, color: '#6b7280', textAlign: 'center', marginBottom: 16 },
  strengthText:  { fontSize: 12, marginBottom: 12, marginTop: -4 },
  btn:           { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center', marginBottom: 8 },
  btnDisabled:   { opacity: 0.5 },
  btnText:       { color: '#fff', fontWeight: '700', fontSize: 15 },
  backLink:      { textAlign: 'center', color: '#6b7280', fontSize: 13, marginTop: 4 },
  linkText:      { textAlign: 'center', color: '#f97316', fontSize: 13, fontWeight: '600', marginTop: 8 },
  switchLink:    { marginTop: 4, marginBottom: -4 },
  dividerRow:    { flexDirection: 'row', alignItems: 'center', marginVertical: 16 },
  dividerLine:   { flex: 1, height: 1, backgroundColor: '#e5e7eb' },
  dividerText:   { paddingHorizontal: 12, color: '#9ca3af', fontSize: 12 },
  googleBtn:     { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  googleBtnText: { color: '#374151', fontWeight: '600', fontSize: 14 },
});
