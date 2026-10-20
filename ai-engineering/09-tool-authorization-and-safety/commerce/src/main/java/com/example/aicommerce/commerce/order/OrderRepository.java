package com.example.aicommerce.commerce.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = {"items", "customer"})
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"items", "customer"})
    Optional<CustomerOrder> findById(Long id);
}
