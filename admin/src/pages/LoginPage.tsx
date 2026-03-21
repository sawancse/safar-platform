import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, Alert, Steps } from 'antd';
import { adminApi } from '../lib/api';

const { Title, Text } = Typography;

export default function LoginPage() {
  const navigate = useNavigate();
  const [step, setStep]         = useState(0); // 0 = phone, 1 = otp
  const [phone, setPhone]       = useState('');
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');

  async function handlePhone(values: { phone: string }) {
    setLoading(true);
    setError('');
    try {
      await adminApi.sendOtp(`+91${values.phone}`);
      setPhone(values.phone);
      setStep(1);
    } catch (e: any) {
      setError(e?.response?.data?.detail || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  }

  async function handleOtp(values: { otp: string }) {
    setLoading(true);
    setError('');
    try {
      const { data } = await adminApi.verifyOtp(`+91${phone}`, values.otp);
      if (data.user.role !== 'ADMIN') {
        setError('Access denied: admin account required');
        setLoading(false);
        return;
      }
      localStorage.setItem('admin_token', data.accessToken);
      localStorage.setItem('admin_refresh_token', data.refreshToken);
      navigate('/dashboard');
    } catch (e: any) {
      setError(e?.response?.data?.detail || e?.response?.data?.message || 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f9fafb',
      }}
    >
      <Card style={{ width: 380, borderRadius: 16 }} variant="outlined">
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{ fontSize: 40, marginBottom: 8 }}>🧳</div>
          <Title level={3} style={{ margin: 0 }}>Safar Admin</Title>
          <Text type="secondary">Admin portal</Text>
        </div>

        {error && <Alert message={error} type="error" showIcon style={{ marginBottom: 16 }} />}

        {step === 0 ? (
          <Form layout="vertical" onFinish={handlePhone}>
            <Form.Item label="Admin phone number" name="phone" rules={[{ required: true, len: 10, message: 'Enter 10-digit phone' }]}>
              <Input addonBefore="+91" placeholder="9876543210" maxLength={10} />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              Send OTP
            </Button>
          </Form>
        ) : (
          <Form layout="vertical" onFinish={handleOtp}>
            <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
              OTP sent to +91 {phone}
            </Text>
            <Form.Item label="OTP" name="otp" rules={[{ required: true, len: 6, message: 'Enter 6-digit OTP' }]}>
              <Input placeholder="000000" maxLength={6} style={{ textAlign: 'center', letterSpacing: 8, fontSize: 20 }} />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              Verify OTP
            </Button>
            <Button type="link" block style={{ marginTop: 4 }} onClick={() => { setStep(0); setError(''); }}>
              ← Change number
            </Button>
          </Form>
        )}
      </Card>
    </div>
  );
}
