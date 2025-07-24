import { User } from './user.model';

export interface Order {
  id: number;
  orderNumber: string;
  status: 'PENDING_PAYMENT' | 'PAID' | 'TICKETED' | 'CANCELLED';
  amount: number;
  creationDate: Date;
  user: User;
  flightInfo?: any; // 模拟的航班信息
}