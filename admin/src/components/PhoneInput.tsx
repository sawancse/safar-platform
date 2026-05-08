import { Input, Form } from 'antd';
import type { InputProps } from 'antd';

export function sanitizePhone(raw: string): string {
  let digits = (raw || '').replace(/\D/g, '');
  if (digits.startsWith('91') && digits.length > 10) digits = digits.slice(2);
  if (digits.startsWith('0') && digits.length === 11) digits = digits.slice(1);
  return digits.slice(0, 10);
}

export function isValidPhone(raw: string): boolean {
  const d = sanitizePhone(raw);
  return d.length === 10 && /^[6-9]\d{9}$/.test(d);
}

export function phoneError(raw: string, required = true): string {
  const d = sanitizePhone(raw);
  if (!d) return required ? 'Phone number is required' : '';
  if (d.length < 10) return `Enter ${10 - d.length} more digit${10 - d.length === 1 ? '' : 's'}`;
  if (!/^[6-9]/.test(d)) return 'Indian mobile numbers must start with 6, 7, 8, or 9';
  return '';
}

interface Props extends Omit<InputProps, 'onChange' | 'value' | 'maxLength' | 'addonBefore'> {
  value?: string;
  onChange?: (digits: string) => void;
  required?: boolean;
}

/** "+91" prefix + 10-digit-max input. Show inline error via Form.Item or `help`. */
export default function PhoneInput({ value = '', onChange, required = true, ...rest }: Props) {
  const error = phoneError(value, required);
  const showError = !!value && !!error;
  return (
    <Form.Item validateStatus={showError ? 'error' : ''} help={showError ? error : ''} style={{ marginBottom: 0 }}>
      <Input
        addonBefore="+91"
        maxLength={10}
        inputMode="numeric"
        placeholder="10-digit mobile number"
        {...rest}
        value={value}
        onChange={(e) => onChange?.(sanitizePhone(e.target.value))}
      />
    </Form.Item>
  );
}
