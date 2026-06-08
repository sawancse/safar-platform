import AsyncStorage from '@react-native-async-storage/async-storage';

const KEY = 'safar_cook_cart';

export interface CartItem {
  /** local cart id */
  cartId: string;
  serviceType: 'DAILY' | 'MONTHLY' | 'EVENT';
  chefId?: string;
  chefName?: string;
  /** human summary line shown in the cart */
  summary: string;
  /** estimated total in paise (for display) */
  estTotalPaise: number;
  /** raw payload sent to the create endpoint at checkout */
  payload: Record<string, any>;
}

export async function getCart(): Promise<CartItem[]> {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as CartItem[]) : [];
  } catch {
    return [];
  }
}

export async function addToCart(item: Omit<CartItem, 'cartId'>): Promise<void> {
  const cart = await getCart();
  // index-based id (no Math.random needed; collisions are fine for a local cart)
  const cartId = `c${cart.length}_${item.serviceType}_${item.chefId ?? 'x'}`;
  cart.push({ ...item, cartId });
  await AsyncStorage.setItem(KEY, JSON.stringify(cart));
}

export async function removeFromCart(cartId: string): Promise<CartItem[]> {
  const cart = (await getCart()).filter((c) => c.cartId !== cartId);
  await AsyncStorage.setItem(KEY, JSON.stringify(cart));
  return cart;
}

export async function clearCart(): Promise<void> {
  await AsyncStorage.removeItem(KEY);
}

export async function cartCount(): Promise<number> {
  return (await getCart()).length;
}
