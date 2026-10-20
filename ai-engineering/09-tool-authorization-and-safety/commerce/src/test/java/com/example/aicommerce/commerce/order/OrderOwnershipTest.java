package com.example.aicommerce.commerce.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.aicommerce.commerce.common.NotFoundException;
import com.example.aicommerce.commerce.customer.CurrentCustomerService;
import com.example.aicommerce.commerce.customer.Customer;
import com.example.aicommerce.commerce.customer.CustomerRepository;
import com.example.aicommerce.commerce.payment.PaymentStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderOwnershipTest {
    @Autowired MockMvc mvc;
    @Autowired OrderRepository orders;
    @Autowired CustomerRepository customers;
    @Autowired OrderService service;
    @Autowired CurrentCustomerService currentCustomer;

    @Test
    void ownershipIsEnforcedByTheActualCommerceService() {
        Customer alice =
                customers.saveAndFlush(
                        new Customer(
                                "Alice", "alice@example.test", "https://issuer.test", "alice"));
        Customer bob =
                customers.saveAndFlush(
                        new Customer("Bob", "bob@example.test", "https://issuer.test", "bob"));
        CustomerOrder order =
                orders.saveAndFlush(
                        new CustomerOrder(
                                alice,
                                new BigDecimal("49"),
                                PaymentStatus.SUCCESS,
                                "synthetic-payment"));
        assertEquals(order.getId(), service.detailForCustomer(order.getId(), alice.getId()).id());
        assertThrows(
                NotFoundException.class,
                () -> service.detailForCustomer(order.getId(), bob.getId()));
        assertThrows(
                NotFoundException.class,
                () -> service.detailForCustomer(Long.MAX_VALUE, bob.getId()));
        assertTrue(service.history(bob.getId()).isEmpty());
    }

    @Test
    void httpRejectsAnotherCustomersOrderAndMissingPermission() throws Exception {
        Customer alice =
                customers.saveAndFlush(
                        new Customer(
                                "Alice", "alice2@example.test", "https://issuer.test", "alice"));
        customers.saveAndFlush(
                new Customer("Bob", "bob2@example.test", "https://issuer.test", "bob"));
        CustomerOrder order =
                orders.saveAndFlush(
                        new CustomerOrder(
                                alice, new BigDecimal("49"), PaymentStatus.SUCCESS, "synthetic"));
        mvc.perform(get("/api/orders/" + order.getId())).andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/orders/" + order.getId())
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.subject("alice")
                                                                        .issuer(
                                                                                "https://issuer.test"))
                                                .authorities(
                                                        new SimpleGrantedAuthority("USERS_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()));
        mvc.perform(
                        get("/api/orders/" + order.getId())
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.subject("bob")
                                                                        .issuer(
                                                                                "https://issuer.test"))
                                                .authorities(
                                                        new SimpleGrantedAuthority("USERS_READ"))))
                .andExpect(status().isNotFound());
        mvc.perform(
                        get("/api/orders/" + order.getId())
                                .with(
                                        jwt().jwt(
                                                        token ->
                                                                token.subject("bob")
                                                                        .issuer(
                                                                                "https://issuer.test"))
                                                .authorities(
                                                        new SimpleGrantedAuthority("USERS_WRITE"))))
                .andExpect(status().isForbidden());
    }
}
