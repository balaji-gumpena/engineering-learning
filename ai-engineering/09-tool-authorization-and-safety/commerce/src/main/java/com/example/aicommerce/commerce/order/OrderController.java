package com.example.aicommerce.commerce.order;

import com.example.aicommerce.commerce.customer.CurrentCustomerService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;
    private final CurrentCustomerService currentCustomer;

    OrderController(OrderService orderService, CurrentCustomerService currentCustomer) {
        this.orderService = orderService;
        this.currentCustomer = currentCustomer;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USERS_READ')")
    List<OrderResponse> history(@AuthenticationPrincipal Jwt identity) {
        return orderService.history(currentCustomer.resolve(identity).getId());
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('USERS_READ')")
    OrderResponse detail(@PathVariable Long orderId, @AuthenticationPrincipal Jwt identity) {
        return orderService.detailForCustomer(orderId, currentCustomer.resolve(identity).getId());
    }
}
