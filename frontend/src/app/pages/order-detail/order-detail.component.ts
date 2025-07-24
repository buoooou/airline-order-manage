import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router'; // 引入RouterModule
import { Observable, switchMap } from 'rxjs';
import { Order } from '../../shared/models/order.model';
import { OrderService } from '../../core/services/order.service';

// 引入需要的模块和管道
import { CommonModule } from '@angular/common'; // AsyncPipe, JsonPipe, CurrencyPipe, DatePipe
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzPageHeaderModule } from 'ng-zorro-antd/page-header';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzDescriptionsModule,
    NzTagModule,
    NzPageHeaderModule
  ],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss']
})
export class OrderDetailComponent implements OnInit {
  // ... (属性和方法保持不变) ...
  order$!: Observable<Order>;

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    this.order$ = this.route.paramMap.pipe(
      switchMap(params => {
        const id = params.get('id');
        if (id) {
          return this.orderService.getOrderById(id);
        }
        throw new Error('Order ID not found in route');
      })
    );
  }
}