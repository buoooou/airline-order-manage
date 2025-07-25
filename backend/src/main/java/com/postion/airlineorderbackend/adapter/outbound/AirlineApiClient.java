package com.postion.airlineorderbackend.adapter.outbound;


import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class AirlineApiClient {

    /**
     * 模拟调用航司API出票
     * @param orderId 订单ID
     * @return 票号
     * @throws InterruptedException 如果线程被中断
     * @throws RuntimeException 模拟API调用失败
     */
    public String issueTicket(Long orderId) throws InterruptedException {
        System.out.println("开始为订单 " + orderId + " 调用航司接口出票...");

        // 模拟网络延迟和处理时间 (2-5秒)
        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(2, 6));

        // 模拟接口成功率 (80%成功, 20%失败)
        if (ThreadLocalRandom.current().nextInt(10) < 8) {
            System.out.println("订单 " + orderId + " 出票成功！");
            return "TKT" + System.currentTimeMillis();
        } else {
            System.err.println("订单 " + orderId + " 出票失败：航司返回错误。");
            throw new RuntimeException("Airline API error: Insufficient seats");
        }
    }
}