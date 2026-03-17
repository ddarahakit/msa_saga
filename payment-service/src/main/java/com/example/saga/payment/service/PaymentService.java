package com.example.saga.payment.service;

import com.example.saga.common.event.OrderEvent;
import com.example.saga.common.event.PaymentEvent;
import com.example.saga.payment.entity.Payment;
import com.example.saga.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;

    @Bean
    public Function<OrderEvent, PaymentEvent> orderProcessor() {
        return event -> {
            log.info("Processing Payment for OrderId: {}", event.getOrderId());
            if ("PENDING".equals(event.getStatus())) {
                return processPayment(event);
            }
            return null;
        };
    }

    @Bean
    public Consumer<OrderEvent> orderCancelledConsumer() {
        return event -> {
            if ("CANCELLED".equals(event.getStatus())) {
                log.info("Compensating Payment for OrderId: {}", event.getOrderId());
                refundPayment(event.getOrderId());
            }
        };
    }

    @Transactional
    public PaymentEvent processPayment(OrderEvent event) {
        // Simple logic: Fail if amount > 1000
        String status = event.getPrice().doubleValue() > 1000 ? "FAILED" : "SUCCESS";
        
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .amount(event.getPrice())
                .status(status)
                .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
        return PaymentEvent.builder()
                .orderId(event.getOrderId())
                .paymentId(savedPayment.getId())
                .amount(savedPayment.getAmount())
                .status(status)
                .build();
    }

    @Transactional
    public void refundPayment(Long orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus("CANCELLED");
            paymentRepository.save(payment);
            log.info("Payment REFUNDED/CANCELLED for OrderId: {}", orderId);
        });
    }
}
