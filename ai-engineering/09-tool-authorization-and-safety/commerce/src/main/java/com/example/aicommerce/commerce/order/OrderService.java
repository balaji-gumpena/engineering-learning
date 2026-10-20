package com.example.aicommerce.commerce.order;

import com.example.aicommerce.commerce.common.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> history(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse detail(Long orderId) {
        return OrderResponse.from(
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new NotFoundException("Order not found: " + orderId)));
    }

    @Transactional(readOnly = true)
    public OrderResponse detailForCustomer(Long orderId, Long customerId) {
        return OrderResponse.from(
                orderRepository
                        .findById(orderId)
                        .filter(order -> order.getCustomer().getId().equals(customerId))
                        .orElseThrow(() -> new NotFoundException("Order not found")));
    }
}
