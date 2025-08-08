import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router'; // 引入RouterModule
import { Observable, switchMap, BehaviorSubject, finalize, startWith, catchError, of, EMPTY } from 'rxjs';
import { Order } from '../../shared/models/order.model';
import { OrderService } from '../../core/services/order.service';
import { NzMessageService } from 'ng-zorro-antd/message';
// 引入需要的模块和管道
import { CommonModule } from '@angular/common'; // AsyncPipe, JsonPipe, CurrencyPipe, DatePipe
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzPageHeaderModule } from 'ng-zorro-antd/page-header';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzDescriptionsModule,
    NzTagModule,
    NzPageHeaderModule,
    NzButtonModule,
    NzIconModule,
    NzSpinModule,
  ],
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.scss'],
})
export class OrderDetailComponent implements OnInit {
  // ... (属性和方法保持不变) ...
  order$!: Observable<Order>;

  // 使用 BehaviorSubject 来刷新数据
  private refresh$ = new BehaviorSubject<void>(undefined);

  isLoading = false;
  orderId!: string;

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.order$ = this.route.paramMap.pipe(
      switchMap((params) => {
        const id = params.get('id');
        if (!id) {
          this.message.error('未找到订单ID！');
          throw new Error('Order ID not found');
        }
        this.orderId = id;
        // 每当 refresh$ 发出新值时，重新获取订单数据
        return this.refresh$.pipe(
          switchMap(() => this.orderService.getOrderById(this.orderId))
        );
      })
    );
  }

  // 触发刷新
  private refreshData(): void {
    this.refresh$.next();
  }

  onPay(id: number): void {
    this.handleAction(
      this.orderService.pay(id.toString()),
      '支付成功！订单正在出票中...'
    );
  }

  onCancel(id: number): void {
    this.handleAction(this.orderService.cancel(id.toString()), '订单已取消。');
  }

  onRetry(id: number): void {
    this.isLoading = true;
    this.orderService
      .retryTicketing(id.toString())
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe(() => {
        this.message.success('已发送重试出票请求，请稍后刷新查看状态。');
        this.refreshData();
      });
  }

  private handleAction(
    action$: Observable<Order>,
    successMessage: string
  ): void {
    this.isLoading = true;
    action$
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.refreshData(); // 无论成功失败，都刷新一次数据
        }),
        catchError((err) => {
          this.message.error('操作失败，请重试！');
          console.error(err);
          return EMPTY; // 阻止错误传播
        })
      )
      .subscribe(() => {
        this.message.success(successMessage);
      });
  }

  getStatusColor(status: Order['status']): string {
    switch (status) {
      case 'PAID':
        return 'processing';
      case 'TICKETED':
        return 'success';
      case 'CANCELLED':
        return 'error';
      case 'PENDING_PAYMENT':
        return 'warning';
      case 'TICKETING_IN_PROGRESS':
        return 'geekblue';
      case 'TICKETING_FAILED':
        return 'magenta';
      default:
        return 'default';
    }
  }
}