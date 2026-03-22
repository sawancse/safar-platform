'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { api } from '@/lib/api';
import { generateDeviceFingerprint, getDeviceName } from '@/lib/device-fingerprint';

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: { client_id: string; callback: (response: { credential: string }) => void; auto_select?: boolean }) => void;
          prompt: () => void;
          renderButton: (element: HTMLElement, config: { theme: string; size: string; width?: number; text?: string }) => void;
        };
      };
    };
    AppleID?: {
      auth: {
        init: (config: any) => void;
        signIn: () => Promise<any>;
      };
    };
  }
}

type Step = 'input' | 'password' | 'otp' | 'name' | 'set-password' | 'forgot-password';
type AuthMethod = 'phone' | 'email';

const COUNTRY_CODES = [
  { code: '+91', country: 'India', flag: '\u{1F1EE}\u{1F1F3}' },
  { code: '+1', country: 'US/Canada', flag: '\u{1F1FA}\u{1F1F8}' },
  { code: '+44', country: 'UK', flag: '\u{1F1EC}\u{1F1E7}' },
  { code: '+971', country: 'UAE', flag: '\u{1F1E6}\u{1F1EA}' },
  { code: '+65', country: 'Singapore', flag: '\u{1F1F8}\u{1F1EC}' },
  { code: '+61', country: 'Australia', flag: '\u{1F1E6}\u{1F1FA}' },
  { code: '+49', country: 'Germany', flag: '\u{1F1E9}\u{1F1EA}' },
  { code: '+33', country: 'France', flag: '\u{1F1EB}\u{1F1F7}' },
  { code: '+81', country: 'Japan', flag: '\u{1F1EF}\u{1F1F5}' },
  { code: '+86', country: 'China', flag: '\u{1F1E8}\u{1F1F3}' },
];

function getPasswordStrength(pw: string): 'Weak' | 'Fair' | 'Strong' {
  const hasUpper = /[A-Z]/.test(pw);
  const hasNumber = /\d/.test(pw);
  const hasSpecial = /[^A-Za-z0-9]/.test(pw);
  if (pw.length < 8 || !hasUpper || !hasNumber) return 'Weak';
  if (pw.length >= 12 && hasUpper && hasNumber && hasSpecial) return 'Strong';
  return 'Fair';
}

function PasswordStrengthBar({ password }: { password: string }) {
  if (!password) return null;
  const strength = getPasswordStrength(password);
  const color = strength === 'Weak' ? 'bg-red-500' : strength === 'Fair' ? 'bg-yellow-500' : 'bg-green-500';
  const width = strength === 'Weak' ? 'w-1/3' : strength === 'Fair' ? 'w-2/3' : 'w-full';
  return (
    <div className="mt-1.5">
      <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden">
        <div className={`h-full ${color} ${width} transition-all duration-300 rounded-full`} />
      </div>
      <p className={`text-xs mt-1 ${strength === 'Weak' ? 'text-red-500' : strength === 'Fair' ? 'text-yellow-600' : 'text-green-600'}`}>
        {strength}
      </p>
    </div>
  );
}

function EyeIcon({ open }: { open: boolean }) {
  if (open) {
    return (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
      </svg>
    );
  }
  return (
    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.542-7a9.97 9.97 0 012.223-3.592m3.1-2.15A9.956 9.956 0 0112 5c4.478 0 8.268 2.943 9.542 7a9.97 9.97 0 01-4.043 5.206M9.88 9.88a3 3 0 104.24 4.24" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3l18 18" />
    </svg>
  );
}

function saveAuthAndRedirect(auth: any, router: any, redirect: string) {
  localStorage.setItem('access_token', auth.accessToken);
  localStorage.setItem('refresh_token', auth.refreshToken);
  localStorage.setItem('user_id', auth.user.id);
  localStorage.setItem('user_role', auth.user.role);
  localStorage.setItem('user_name', auth.user.name || '');
  if (auth.user.avatarUrl) localStorage.setItem('user_avatar', auth.user.avatarUrl);
  document.cookie = `access_token=${auth.accessToken}; path=/; max-age=${30 * 24 * 60 * 60}; SameSite=Lax`;

  // Trust this device for future passwordless login
  try {
    const fp = generateDeviceFingerprint();
    const dn = getDeviceName();
    api.trustDevice(fp, dn, auth.accessToken).catch(() => {});
  } catch {}

  router.push(redirect);
}

export const dynamic = 'force-dynamic';

export default function AuthPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirect = searchParams.get('redirect') ?? '/dashboard';

  const [step, setStep] = useState<Step>('input');
  const [authMethod, setAuthMethod] = useState<AuthMethod>('phone');
  const [inputValue, setInputValue] = useState('');
  const [countryCode, setCountryCode] = useState('+91');
  const [showCountryPicker, setShowCountryPicker] = useState(false);
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isNewUser, setIsNewUser] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);

  // Password auth state
  const [password, setPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [hasPasswordOption, setHasPasswordOption] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [pendingAuth, setPendingAuth] = useState<any>(null);
  const [failedAttempts, setFailedAttempts] = useState(0);

  // -- Check trusted device on mount --
  const [checkingDevice, setCheckingDevice] = useState(true);

  useEffect(() => {
    async function checkDevice() {
      try {
        const fp = generateDeviceFingerprint();
        const lastPhone = localStorage.getItem('last_login_phone');
        const lastEmail = localStorage.getItem('last_login_email');
        if (!lastPhone && !lastEmail) { setCheckingDevice(false); return; }

        const auth = await api.checkTrustedDevice(fp, lastPhone || undefined, lastEmail || undefined);
        saveAuthAndRedirect(auth, router, redirect);
      } catch {
        setCheckingDevice(false);
      }
    }
    checkDevice();
  }, [router, redirect]);

  const otpRefs = useRef<(HTMLInputElement | null)[]>([]);
  const countryRef = useRef<HTMLDivElement>(null);

  // Auto-detect if input is email or phone
  const isEmail = inputValue.includes('@');
  const isPhone = /^\d+$/.test(inputValue) && inputValue.length >= 8;

  // Resend timer countdown
  useEffect(() => {
    if (resendTimer <= 0) return;
    const timer = setInterval(() => setResendTimer(t => t - 1), 1000);
    return () => clearInterval(timer);
  }, [resendTimer]);

  // Close country picker on outside click
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (countryRef.current && !countryRef.current.contains(e.target as Node)) {
        setShowCountryPicker(false);
      }
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  // -- Google Sign-In --
  const [googleLoaded, setGoogleLoaded] = useState(false);

  const handleGoogleResponse = useCallback(async (response: { credential: string }) => {
    setLoading(true);
    setError('');
    try {
      const auth = await api.googleSignIn(response.credential);
      saveAuthAndRedirect(auth, router, redirect);
    } catch (e: any) {
      setError(e.message || 'Google sign-in failed');
    } finally {
      setLoading(false);
    }
  }, [redirect, router]);

  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => {
      setGoogleLoaded(true);
      const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || 'GOOGLE_CLIENT_ID';
      window.google?.accounts.id.initialize({
        client_id: clientId,
        callback: handleGoogleResponse,
      });
    };
    document.head.appendChild(script);
    return () => { try { document.head.removeChild(script); } catch {} };
  }, [handleGoogleResponse]);

  // -- Send OTP (for email or phone) --
  async function sendOtpForEmail() {
    await api.sendEmailOtp(inputValue);
    setAuthMethod('email');
    localStorage.setItem('last_login_email', inputValue);
    setResendTimer(30);
    setOtp(['', '', '', '', '', '']);
    setTimeout(() => otpRefs.current[0]?.focus(), 100);
  }

  // -- handleContinue: modified for password check --
  async function handleContinue() {
    if (!isEmail && !isPhone) {
      setError('Please enter a valid email or phone number');
      return;
    }

    setLoading(true);
    setError('');
    try {
      if (isEmail) {
        // Check auth method for email users
        try {
          const check = await api.checkAuthMethod(inputValue);
          if (check.hasPassword) {
            // User has a password set - show password step
            setAuthMethod('email');
            setHasPasswordOption(true);
            setStep('password');
            setPassword('');
            setPasswordError('');
            setFailedAttempts(0);
            localStorage.setItem('last_login_email', inputValue);
          } else {
            // No password - send OTP directly
            await sendOtpForEmail();
            setStep('otp');
          }
        } catch {
          // checkAuthMethod failed (endpoint may not exist yet) - fall back to OTP
          await sendOtpForEmail();
          setStep('otp');
        }
      } else {
        // Phone: existing flow unchanged
        await api.sendOtp(`${countryCode}${inputValue}`);
        setAuthMethod('phone');
        setStep('otp');
        localStorage.setItem('last_login_phone', `${countryCode}${inputValue}`);
        setResendTimer(30);
        setOtp(['', '', '', '', '', '']);
        setTimeout(() => otpRefs.current[0]?.focus(), 100);
      }
    } catch (e: any) {
      setError(e.message || 'Failed to send verification code');
    } finally {
      setLoading(false);
    }
  }

  // -- Password sign-in --
  async function handlePasswordSignIn() {
    if (!password) {
      setPasswordError('Please enter your password');
      return;
    }
    setLoading(true);
    setPasswordError('');
    setError('');
    try {
      const auth = await api.passwordSignIn(inputValue, password);
      saveAuthAndRedirect(auth, router, redirect);
    } catch (e: any) {
      const msg = e.message || 'Invalid password';
      setFailedAttempts(prev => prev + 1);
      if (msg.toLowerCase().includes('locked')) {
        setPasswordError(msg);
      } else {
        const remaining = Math.max(0, 5 - (failedAttempts + 1));
        setPasswordError(remaining > 0
          ? `Invalid password. ${remaining} attempt${remaining !== 1 ? 's' : ''} remaining before lockout`
          : msg
        );
      }
    } finally {
      setLoading(false);
    }
  }

  // -- Use OTP instead (from password step) --
  async function handleUseOtpInstead() {
    setLoading(true);
    setError('');
    setPasswordError('');
    try {
      await sendOtpForEmail();
      setStep('otp');
    } catch (e: any) {
      setError(e.message || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  }

  // -- Forgot password: send OTP then go to forgot-password step --
  async function handleForgotPassword() {
    setLoading(true);
    setError('');
    setPasswordError('');
    try {
      await api.sendEmailOtp(inputValue);
      setResendTimer(30);
      setOtp(['', '', '', '', '', '']);
      setNewPassword('');
      setStep('forgot-password');
      setTimeout(() => otpRefs.current[0]?.focus(), 100);
    } catch (e: any) {
      setError(e.message || 'Failed to send reset code');
    } finally {
      setLoading(false);
    }
  }

  // -- Reset password --
  async function handleResetPassword() {
    const otpCode = otp.join('');
    if (otpCode.length !== 6) {
      setError('Please enter the 6-digit code');
      return;
    }
    if (getPasswordStrength(newPassword) === 'Weak') {
      setError('Password must be 8+ characters with 1 uppercase and 1 number');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const auth = await api.resetPassword(inputValue, otpCode, newPassword);
      saveAuthAndRedirect(auth, router, redirect);
    } catch (e: any) {
      setError(e.message || 'Password reset failed');
    } finally {
      setLoading(false);
    }
  }

  // -- OTP input handlers --
  function handleOtpChange(index: number, value: string) {
    if (!/^\d*$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);

    // Auto-advance to next input
    if (value && index < 5) {
      otpRefs.current[index + 1]?.focus();
    }

    // Auto-submit when all 6 digits entered (only for OTP step, not forgot-password)
    if (step === 'otp' && newOtp.every(d => d !== '') && newOtp.join('').length === 6) {
      verifyOtpCode(newOtp.join(''));
    }
  }

  function handleOtpKeyDown(index: number, e: React.KeyboardEvent) {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpRefs.current[index - 1]?.focus();
    }
  }

  function handleOtpPaste(e: React.ClipboardEvent) {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted.length === 6) {
      const newOtp = pasted.split('');
      setOtp(newOtp);
      otpRefs.current[5]?.focus();
      if (step === 'otp') {
        verifyOtpCode(pasted);
      }
    }
  }

  // -- Verify OTP --
  async function verifyOtpCode(code?: string) {
    const otpCode = code || otp.join('');
    if (otpCode.length !== 6) return;

    setLoading(true);
    setError('');
    try {
      const auth = authMethod === 'phone'
        ? await api.verifyOtp(`${countryCode}${inputValue}`, otpCode, name || undefined)
        : await api.verifyEmailOtp(inputValue, otpCode, name || undefined);

      // For email users: check if they have a password. If not, offer to set one.
      if (authMethod === 'email' && !hasPasswordOption) {
        // User logged in via OTP and has no password - offer to set one
        setPendingAuth(auth);
        setStep('set-password');
        setNewPassword('');
        return;
      }

      saveAuthAndRedirect(auth, router, redirect);
    } catch (e: any) {
      const msg = e.message || 'Invalid code';
      if (msg.toLowerCase().includes('name is required')) {
        setIsNewUser(true);
        setStep('name');
        setError('');
      } else {
        setError(msg);
        setOtp(['', '', '', '', '', '']);
        otpRefs.current[0]?.focus();
      }
    } finally {
      setLoading(false);
    }
  }

  // -- Complete registration (new user name) --
  async function handleCompleteName() {
    if (!name.trim()) {
      setError('Please enter your name');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const auth = authMethod === 'phone'
        ? await api.verifyOtp(`${countryCode}${inputValue}`, otp.join(''), name)
        : await api.verifyEmailOtp(inputValue, otp.join(''), name);

      // New email user - offer to set password
      if (authMethod === 'email') {
        setPendingAuth(auth);
        setStep('set-password');
        setNewPassword('');
        return;
      }

      saveAuthAndRedirect(auth, router, redirect);
    } catch (e: any) {
      setError(e.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  }

  // -- Set password (optional, after OTP login) --
  async function handleSetPassword() {
    if (getPasswordStrength(newPassword) === 'Weak') {
      setError('Password must be 8+ characters with 1 uppercase and 1 number');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await api.setPassword(newPassword, pendingAuth.accessToken);
    } catch {
      // Non-blocking: if set-password fails, still log in
    }
    saveAuthAndRedirect(pendingAuth, router, redirect);
  }

  function handleSkipSetPassword() {
    saveAuthAndRedirect(pendingAuth, router, redirect);
  }

  // -- Resend OTP --
  async function handleResend() {
    if (resendTimer > 0) return;
    setLoading(true);
    setError('');
    try {
      if (authMethod === 'email') {
        await api.sendEmailOtp(inputValue);
      } else {
        await api.sendOtp(`${countryCode}${inputValue}`);
      }
      setResendTimer(30);
      setOtp(['', '', '', '', '', '']);
      otpRefs.current[0]?.focus();
    } catch (e: any) {
      setError(e.message || 'Failed to resend code');
    } finally {
      setLoading(false);
    }
  }

  // -- Social login handlers --
  function handleGoogleClick() {
    if (!googleLoaded || !window.google) {
      setError('Google Sign-In is loading. Please try again.');
      return;
    }
    window.google.accounts.id.prompt();
  }

  async function handleAppleClick() {
    setError('Apple Sign-In coming soon. Please use another method.');
  }

  async function handleWhatsAppClick() {
    if (!isPhone && !inputValue) {
      setError('Please enter your phone number first, then use WhatsApp');
      return;
    }
    const phoneNum = isPhone ? inputValue : '';
    if (!phoneNum || phoneNum.length < 8) {
      setError('Enter your phone number above, then click WhatsApp');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await api.sendWhatsAppOtp(`${countryCode}${phoneNum}`);
      setAuthMethod('phone');
      setStep('otp');
      setResendTimer(30);
      setOtp(['', '', '', '', '', '']);
      setTimeout(() => otpRefs.current[0]?.focus(), 100);
    } catch (e: any) {
      setError(e.message || 'WhatsApp OTP failed');
    } finally {
      setLoading(false);
    }
  }

  // -- Back button logic --
  function handleBack() {
    setError('');
    setPasswordError('');
    if (step === 'name') {
      setStep('otp');
    } else if (step === 'forgot-password') {
      setStep('password');
      setOtp(['', '', '', '', '', '']);
      setNewPassword('');
    } else if (step === 'set-password') {
      // Skip set-password and proceed with login
      if (pendingAuth) {
        saveAuthAndRedirect(pendingAuth, router, redirect);
      } else {
        setStep('input');
      }
    } else {
      setStep('input');
      setOtp(['', '', '', '', '', '']);
      setPassword('');
    }
  }

  // -- Header title --
  function getHeaderTitle() {
    switch (step) {
      case 'input': return 'Log in or sign up';
      case 'password': return 'Welcome back';
      case 'otp': return 'Confirm your code';
      case 'name': return 'Finish signing up';
      case 'set-password': return 'Set a password';
      case 'forgot-password': return 'Reset your password';
    }
  }

  if (checkingDevice) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-orange-50 to-rose-50">
        <div className="text-center">
          <div className="w-8 h-8 border-3 border-orange-400 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
          <p className="text-sm text-gray-500">Checking your device...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-orange-50 to-rose-50 px-4">
      <div className="w-full max-w-md">
        {/* Card */}
        <div className="bg-white rounded-2xl shadow-xl overflow-hidden">
          {/* Header */}
          <div className="border-b px-6 py-4 flex items-center justify-center relative">
            {step !== 'input' && (
              <button
                onClick={handleBack}
                className="absolute left-4 text-gray-600 hover:text-gray-800 p-1"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
              </button>
            )}
            <h2 className="text-base font-semibold text-gray-900">
              {getHeaderTitle()}
            </h2>
          </div>

          {/* Body */}
          <div className="p-6">
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-xl px-4 py-3 mb-4 flex items-start gap-2">
                <svg className="w-4 h-4 mt-0.5 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                {error}
              </div>
            )}

            {/* == Step: Email or Phone Input == */}
            {step === 'input' && (
              <div className="space-y-4">
                <p className="text-sm text-gray-600">Welcome to Safar</p>

                {/* Input field with country code */}
                <div className="border rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-orange-500 focus-within:border-orange-500">
                  {/* Country code row (shown only when phone detected or empty) */}
                  {!isEmail && (
                    <div ref={countryRef} className="relative border-b">
                      <button
                        type="button"
                        onClick={() => setShowCountryPicker(!showCountryPicker)}
                        className="w-full flex items-center justify-between px-4 py-3 text-sm text-gray-700 hover:bg-gray-50"
                      >
                        <span>
                          {COUNTRY_CODES.find(c => c.code === countryCode)?.flag}{' '}
                          {COUNTRY_CODES.find(c => c.code === countryCode)?.country} ({countryCode})
                        </span>
                        <svg className={`w-4 h-4 transition ${showCountryPicker ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                        </svg>
                      </button>

                      {showCountryPicker && (
                        <div className="absolute top-full left-0 right-0 bg-white border rounded-b-xl shadow-lg z-20 max-h-48 overflow-y-auto">
                          {COUNTRY_CODES.map((cc) => (
                            <button
                              key={cc.code}
                              type="button"
                              onClick={() => { setCountryCode(cc.code); setShowCountryPicker(false); }}
                              className={`w-full text-left px-4 py-2.5 text-sm hover:bg-orange-50 flex items-center gap-2 ${
                                countryCode === cc.code ? 'bg-orange-50 font-medium' : ''
                              }`}
                            >
                              <span>{cc.flag}</span>
                              <span>{cc.country}</span>
                              <span className="text-gray-400 ml-auto">{cc.code}</span>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Main input */}
                  <input
                    type={isEmail ? 'email' : 'text'}
                    inputMode={isEmail ? 'email' : 'tel'}
                    value={inputValue}
                    onChange={(e) => { setInputValue(e.target.value); setError(''); }}
                    onKeyDown={(e) => e.key === 'Enter' && (isEmail || isPhone) && handleContinue()}
                    placeholder="Phone number or email"
                    className="w-full px-4 py-3.5 text-sm outline-none placeholder-gray-400"
                    autoFocus
                  />
                </div>

                {/* Continue button */}
                <button
                  onClick={handleContinue}
                  disabled={loading || (!isEmail && !isPhone)}
                  className="w-full bg-gradient-to-r from-orange-500 to-rose-500 hover:from-orange-600 hover:to-rose-600 text-white font-semibold py-3.5 rounded-xl disabled:opacity-50 transition text-sm"
                >
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                      </svg>
                      Checking...
                    </span>
                  ) : 'Continue'}
                </button>

                {/* Divider */}
                <div className="relative my-2">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-gray-200" />
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-white px-3 text-gray-400">or</span>
                  </div>
                </div>

                {/* Social buttons */}
                <div className="space-y-3">
                  {/* Google */}
                  <button
                    onClick={handleGoogleClick}
                    className="w-full flex items-center border border-gray-300 rounded-xl px-4 py-3 hover:bg-gray-50 transition"
                  >
                    <svg className="w-5 h-5" viewBox="0 0 24 24">
                      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                    </svg>
                    <span className="flex-1 text-sm font-medium text-gray-700">Continue with Google</span>
                  </button>

                  {/* Apple */}
                  <button
                    onClick={handleAppleClick}
                    className="w-full flex items-center border border-gray-300 rounded-xl px-4 py-3 hover:bg-gray-50 transition"
                  >
                    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M17.05 20.28c-.98.95-2.05.88-3.08.4-1.09-.5-2.08-.52-3.23 0-1.44.62-2.2.44-3.06-.4C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.54 4.09zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z"/>
                    </svg>
                    <span className="flex-1 text-sm font-medium text-gray-700">Continue with Apple</span>
                  </button>

                  {/* WhatsApp — disabled until WhatsApp Business API integration */}
                  {/* <button
                    onClick={handleWhatsAppClick}
                    className="w-full flex items-center border border-gray-300 rounded-xl px-4 py-3 hover:bg-gray-50 transition"
                  >
                    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="#25D366">
                      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                    </svg>
                    <span className="flex-1 text-sm font-medium text-gray-700">Continue with WhatsApp</span>
                  </button> */}
                </div>

                {/* Terms */}
                <p className="text-xs text-gray-400 text-center pt-2">
                  By continuing, you agree to Safar&apos;s{' '}
                  <a href="/terms" className="text-orange-500 underline">Terms of Service</a> and{' '}
                  <a href="/privacy" className="text-orange-500 underline">Privacy Policy</a>.
                </p>
              </div>
            )}

            {/* == Step: Password Sign-In == */}
            {step === 'password' && (
              <div className="space-y-4">
                <div>
                  <p className="text-sm text-gray-600">
                    <span className="font-semibold text-gray-900">{inputValue}</span>
                    <button
                      onClick={() => { setStep('input'); setPassword(''); setPasswordError(''); setError(''); }}
                      className="ml-2 text-xs text-orange-500 hover:text-orange-600 underline"
                    >
                      Change
                    </button>
                  </p>
                </div>

                {/* Password input with eye toggle */}
                <div>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => { setPassword(e.target.value); setPasswordError(''); }}
                      onKeyDown={(e) => e.key === 'Enter' && password && handlePasswordSignIn()}
                      placeholder="Enter your password"
                      className="w-full border-2 rounded-xl px-4 py-3 pr-12 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200 transition"
                      autoFocus
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      <EyeIcon open={showPassword} />
                    </button>
                  </div>

                  {passwordError && (
                    <p className="text-xs text-red-500 mt-1.5">{passwordError}</p>
                  )}
                </div>

                {/* Sign In button */}
                <button
                  onClick={handlePasswordSignIn}
                  disabled={loading || !password}
                  className="w-full bg-gradient-to-r from-orange-500 to-rose-500 hover:from-orange-600 hover:to-rose-600 text-white font-semibold py-3.5 rounded-xl disabled:opacity-50 transition text-sm"
                >
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                      </svg>
                      Signing in...
                    </span>
                  ) : 'Sign In'}
                </button>

                {/* Forgot password link */}
                <div className="text-center">
                  <button
                    onClick={handleForgotPassword}
                    disabled={loading}
                    className="text-sm text-orange-500 hover:text-orange-600 underline"
                  >
                    Forgot password?
                  </button>
                </div>

                {/* Failed attempts hint */}
                {failedAttempts >= 3 && (
                  <div className="bg-orange-50 border border-orange-200 text-orange-700 text-sm rounded-xl px-4 py-3">
                    Having trouble? Try signing in with a one-time code instead.
                  </div>
                )}

                {/* Divider */}
                <div className="relative my-2">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-gray-200" />
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-white px-3 text-gray-400">or</span>
                  </div>
                </div>

                {/* Use OTP instead */}
                <button
                  onClick={handleUseOtpInstead}
                  disabled={loading}
                  className="w-full flex items-center justify-center border border-gray-300 rounded-xl px-4 py-3 hover:bg-gray-50 transition text-sm font-medium text-gray-700"
                >
                  <svg className="w-5 h-5 mr-2 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                  </svg>
                  Use OTP instead
                </button>

                {/* Social buttons */}
                <div className="space-y-3">
                  <button
                    onClick={handleGoogleClick}
                    className="w-full flex items-center border border-gray-300 rounded-xl px-4 py-3 hover:bg-gray-50 transition"
                  >
                    <svg className="w-5 h-5" viewBox="0 0 24 24">
                      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                    </svg>
                    <span className="flex-1 text-sm font-medium text-gray-700">Continue with Google</span>
                  </button>

                  <button
                    onClick={handleAppleClick}
                    className="w-full flex items-center border border-gray-300 rounded-xl px-4 py-3 hover:bg-gray-50 transition"
                  >
                    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M17.05 20.28c-.98.95-2.05.88-3.08.4-1.09-.5-2.08-.52-3.23 0-1.44.62-2.2.44-3.06-.4C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.54 4.09zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z"/>
                    </svg>
                    <span className="flex-1 text-sm font-medium text-gray-700">Continue with Apple</span>
                  </button>
                </div>
              </div>
            )}

            {/* == Step: OTP Verification == */}
            {step === 'otp' && (
              <div className="space-y-5">
                <div>
                  <p className="text-sm text-gray-600">
                    Enter the code we sent to{' '}
                    <span className="font-semibold text-gray-900">
                      {authMethod === 'phone' ? `${countryCode} ${inputValue}` : inputValue}
                    </span>
                  </p>
                </div>

                {/* 6-digit OTP boxes */}
                <div className="flex gap-2 justify-center">
                  {otp.map((digit, i) => (
                    <input
                      key={i}
                      ref={(el) => { otpRefs.current[i] = el; }}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={digit}
                      onChange={(e) => handleOtpChange(i, e.target.value)}
                      onKeyDown={(e) => handleOtpKeyDown(i, e)}
                      onPaste={i === 0 ? handleOtpPaste : undefined}
                      className="w-12 h-14 text-center text-xl font-semibold border-2 rounded-xl outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200 transition"
                    />
                  ))}
                </div>

                {/* Verify button */}
                <button
                  onClick={() => verifyOtpCode()}
                  disabled={loading || otp.join('').length !== 6}
                  className="w-full bg-gradient-to-r from-orange-500 to-rose-500 hover:from-orange-600 hover:to-rose-600 text-white font-semibold py-3.5 rounded-xl disabled:opacity-50 transition text-sm"
                >
                  {loading ? 'Verifying...' : 'Verify'}
                </button>

                {/* Resend */}
                <div className="text-center">
                  {resendTimer > 0 ? (
                    <p className="text-sm text-gray-400">
                      Resend code in <span className="font-medium text-gray-600">{resendTimer}s</span>
                    </p>
                  ) : (
                    <button
                      onClick={handleResend}
                      disabled={loading}
                      className="text-sm font-semibold text-orange-500 hover:text-orange-600 underline"
                    >
                      Resend code
                    </button>
                  )}
                </div>
              </div>
            )}

            {/* == Step: Name (new user) == */}
            {step === 'name' && (
              <div className="space-y-5">
                <div>
                  <p className="text-sm text-gray-600 mb-1">Welcome! Let&apos;s set up your account.</p>
                  <p className="text-xs text-gray-400">This is how your name will appear on Safar.</p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Full name</label>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && name.trim() && handleCompleteName()}
                    placeholder="e.g. Ravi Kumar"
                    className="w-full border-2 rounded-xl px-4 py-3 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200 transition"
                    autoFocus
                  />
                </div>

                <button
                  onClick={handleCompleteName}
                  disabled={loading || !name.trim()}
                  className="w-full bg-gradient-to-r from-orange-500 to-rose-500 hover:from-orange-600 hover:to-rose-600 text-white font-semibold py-3.5 rounded-xl disabled:opacity-50 transition text-sm"
                >
                  {loading ? 'Creating account...' : 'Agree and continue'}
                </button>

                <p className="text-xs text-gray-400 text-center">
                  By selecting Agree and continue, I agree to Safar&apos;s Terms of Service and Privacy Policy.
                </p>
              </div>
            )}

            {/* == Step: Set Password (optional, after OTP login) == */}
            {step === 'set-password' && (
              <div className="space-y-5">
                <div>
                  <p className="text-sm text-gray-600">
                    Set a password for faster sign-ins next time.
                  </p>
                </div>

                {/* New password input */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Password</label>
                  <div className="relative">
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => { setNewPassword(e.target.value); setError(''); }}
                      onKeyDown={(e) => e.key === 'Enter' && newPassword && handleSetPassword()}
                      placeholder="Create a password"
                      className="w-full border-2 rounded-xl px-4 py-3 pr-12 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200 transition"
                      autoFocus
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      <EyeIcon open={showNewPassword} />
                    </button>
                  </div>
                  <PasswordStrengthBar password={newPassword} />
                  <p className="text-xs text-gray-400 mt-1">
                    8+ characters with 1 uppercase letter and 1 number
                  </p>
                </div>

                {/* Set Password button */}
                <button
                  onClick={handleSetPassword}
                  disabled={loading || !newPassword || getPasswordStrength(newPassword) === 'Weak'}
                  className="w-full bg-gradient-to-r from-orange-500 to-rose-500 hover:from-orange-600 hover:to-rose-600 text-white font-semibold py-3.5 rounded-xl disabled:opacity-50 transition text-sm"
                >
                  {loading ? 'Setting password...' : 'Set Password'}
                </button>

                {/* Maybe later */}
                <div className="text-center">
                  <button
                    onClick={handleSkipSetPassword}
                    className="text-sm text-gray-500 hover:text-gray-700 underline"
                  >
                    Maybe later
                  </button>
                </div>
              </div>
            )}

            {/* == Step: Forgot Password (OTP + new password) == */}
            {step === 'forgot-password' && (
              <div className="space-y-5">
                <div>
                  <p className="text-sm text-gray-600">
                    We sent a code to{' '}
                    <span className="font-semibold text-gray-900">{inputValue}</span>
                  </p>
                </div>

                {/* 6-digit OTP boxes */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">Verification code</label>
                  <div className="flex gap-2 justify-center">
                    {otp.map((digit, i) => (
                      <input
                        key={i}
                        ref={(el) => { otpRefs.current[i] = el; }}
                        type="text"
                        inputMode="numeric"
                        maxLength={1}
                        value={digit}
                        onChange={(e) => handleOtpChange(i, e.target.value)}
                        onKeyDown={(e) => handleOtpKeyDown(i, e)}
                        onPaste={i === 0 ? handleOtpPaste : undefined}
                        className="w-12 h-14 text-center text-xl font-semibold border-2 rounded-xl outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200 transition"
                      />
                    ))}
                  </div>
                </div>

                {/* New password input */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">New password</label>
                  <div className="relative">
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => { setNewPassword(e.target.value); setError(''); }}
                      onKeyDown={(e) => e.key === 'Enter' && otp.join('').length === 6 && newPassword && handleResetPassword()}
                      placeholder="Enter new password"
                      className="w-full border-2 rounded-xl px-4 py-3 pr-12 text-sm outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-200 transition"
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      <EyeIcon open={showNewPassword} />
                    </button>
                  </div>
                  <PasswordStrengthBar password={newPassword} />
                  <p className="text-xs text-gray-400 mt-1">
                    8+ characters with 1 uppercase letter and 1 number
                  </p>
                </div>

                {/* Reset Password button */}
                <button
                  onClick={handleResetPassword}
                  disabled={loading || otp.join('').length !== 6 || !newPassword || getPasswordStrength(newPassword) === 'Weak'}
                  className="w-full bg-gradient-to-r from-orange-500 to-rose-500 hover:from-orange-600 hover:to-rose-600 text-white font-semibold py-3.5 rounded-xl disabled:opacity-50 transition text-sm"
                >
                  {loading ? 'Resetting...' : 'Reset Password'}
                </button>

                {/* Resend */}
                <div className="text-center">
                  {resendTimer > 0 ? (
                    <p className="text-sm text-gray-400">
                      Didn&apos;t receive? Resend in <span className="font-medium text-gray-600">{resendTimer}s</span>
                    </p>
                  ) : (
                    <button
                      onClick={handleResend}
                      disabled={loading}
                      className="text-sm font-semibold text-orange-500 hover:text-orange-600 underline"
                    >
                      Resend OTP
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-gray-400 mt-6">
          Need help? <a href="/contact" className="text-orange-500 hover:underline">Contact support</a>
        </p>
      </div>
    </div>
  );
}
