package com.example.aicommerce.commerce.order;

import com.example.aicommerce.commerce.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        BigDecimal subtotal,
        PaymentStatus paymentStatus,
        String paymentReference,
        Instant createdAt,
        List<OrderItemResponse> items) {

    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getPaymentStatus(),
                order.getPaymentReference(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
