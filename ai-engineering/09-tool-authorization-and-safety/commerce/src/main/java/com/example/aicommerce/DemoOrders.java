package com.example.aicommerce;

import com.example.aicommerce.commerce.customer.Customer;
import com.example.aicommerce.commerce.customer.CustomerRepository;
import com.example.aicommerce.commerce.order.CustomerOrder;
import com.example.aicommerce.commerce.order.OrderRepository;
import com.example.aicommerce.commerce.payment.PaymentStatus;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

@Configuration
@Profile("demo")
public class DemoOrders {
    @Bean
    ApplicationRunner seed(CustomerRepository customers, OrderRepository orders) {
        return args -> {
            Customer alice =
                    customers.saveAndFlush(
                            new Customer(
                                    "Alice",
                                    "alice@example.test",
                                    "http://localhost:8081/realms/lesson",
                                    "alice"));
            customers.saveAndFlush(
                    new Customer(
                            "Bob",
                            "bob@example.test",
                            "http://localhost:8081/realms/lesson",
                            "bob"));
            orders.saveAndFlush(
                    new CustomerOrder(
                            alice, new BigDecimal("49"), PaymentStatus.SUCCESS, "demo-only"));
        };
    }
}
