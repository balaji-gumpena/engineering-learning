package com.example.aicommerce.commerce.customer;

import com.example.aicommerce.commerce.common.BadRequestException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentCustomerService {
    private final CustomerRepository customers;

    public CurrentCustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    public Customer resolve(Jwt identity) {
        String issuer = identity.getIssuer().toString();
        String subject = identity.getSubject();
        Optional<Customer> existing =
                customers.findByIdentityIssuerAndIdentitySubject(issuer, subject);
        if (existing.isPresent()) {
            return existing.get();
        }
        String email = identity.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Your identity provider must supply an email address");
        }
        String name = identity.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = identity.getClaimAsString("preferred_username");
        }
        if (name == null || name.isBlank()) {
            name = "Customer";
        }
        try {
            // Repository transaction completes here; concurrent first requests can safely recover.
            return customers.saveAndFlush(new Customer(name, email, issuer, subject));
        } catch (DataIntegrityViolationException exception) {
            return customers
                    .findByIdentityIssuerAndIdentitySubject(issuer, subject)
                    .orElseThrow(
                            () ->
                                    new BadRequestException(
                                            "An account already uses this email. Contact support."));
        }
    }
}
