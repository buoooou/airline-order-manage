import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Order } from '../../shared/models/order.model';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';

// 引入需要的模块和管道
import { CommonModule } from '@angular/common'; // NgFor, AsyncPipe, TitleCasePipe, CurrencyPipe, DatePipe都在这里
import { RouterModule } from '@angular/router'; // 引入 RouterModule 以使用 routerLink
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';

@Component({
  selector: 'app-order-list',
  standalone: true, // 关键！
  imports: [ // 关键！
    CommonModule,
    RouterModule,
    NzTableModule,
    NzTagModule,
    NzButtonModule
  ],
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.scss']
})
export class OrderListComponent implements OnInit {
  // ... (属性和方法保持不变) ...
  orders$!: Observable<Order[]>;

  constructor(
    private orderService: OrderService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.orders$ = this.orderService.getOrders();
  }
  
  getStatusColor(status: Order['status']): string {
    switch (status) {
      case 'PAID': return 'green';
      case 'TICKETED': return 'blue';
      case 'CANCELLED': return 'red';
      case 'PENDING_PAYMENT': return 'orange';
      default: return 'default';
    }
  }

  logout() {
    this.authService.logout();
  }
}