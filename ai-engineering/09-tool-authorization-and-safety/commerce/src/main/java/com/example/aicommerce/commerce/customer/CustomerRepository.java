package com.example.aicommerce.commerce.customer;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select customer from Customer customer where customer.id = :id")
    Optional<Customer> findByIdForCartUpdate(Long id);

    Optional<Customer> findByIdentityIssuerAndIdentitySubject(String issuer, String subject);
}
