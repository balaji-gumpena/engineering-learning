package com.example.aiassistant.commerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.aiassistant.chat.AiException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class CommerceApiTest {
    MockRestServiceServer server;
    CommerceApi api;

    @BeforeEach
    void setup() {
        Builder builder = RestClient.builder().baseUrl("http://commerce.test");
        server = MockRestServiceServer.bindTo(builder).build();
        api = new CommerceApi(builder.build(), JsonMapper.builder().build());
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void cartForwardsBearerButRemovesCustomerIdentifiers() {
        server.expect(requestTo("http://commerce.test/api/cart"))
                .andExpect(header("Authorization", "Bearer synthetic-token"))
                .andRespond(
                        withSuccess(
                                "{\"customerId\":99,\"items\":[],\"total\":\"0.00\"}",
                                MediaType.APPLICATION_JSON));
        Map<String, Object> result = api.cart("synthetic-token");
        assertFalse(result.containsKey("customerId"));
        assertEquals("0.00", result.get("total"));
    }

    @Test
    void ordersRemovePaymentReferences() {
        server.expect(requestTo("http://commerce.test/api/orders/7"))
                .andRespond(
                        withSuccess(
                                """
                {"id":7,"customerId":99,"status":"CONFIRMED","subtotal":49,"paymentStatus":"SUCCESS",
                 "paymentReference":"synthetic-private-reference","createdAt":"2026-10-13T12:00:00Z","items":[]}
                """,
                                MediaType.APPLICATION_JSON));
        Map<String, Object> result = api.order(7, "synthetic-token");
        assertFalse(result.containsKey("paymentReference"));
        assertFalse(result.containsKey("customerId"));
    }

    @Test
    void inaccessibleOrderBecomesExplicitToolResult() {
        server.expect(requestTo("http://commerce.test/api/orders/7"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertEquals("not_found", api.order(7, "synthetic-token").get("error"));
    }

    @Test
    void malformedDataDoesNotBecomeAnEmptyCart() {
        server.expect(requestTo("http://commerce.test/api/cart"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(AiException.class, () -> api.cart("synthetic-token"));
    }

    @Test
    void malformedAmountIsNotGivenToTheModelAsVerifiedData() {
        server.expect(requestTo("http://commerce.test/api/cart"))
                .andRespond(
                        withSuccess(
                                "{\"items\":[],\"total\":\"not-a-number\"}",
                                MediaType.APPLICATION_JSON));
        assertThrows(AiException.class, () -> api.cart("synthetic-token"));
    }

    @Test
    void catalogUsesPublicApiWithoutCustomerToken() {
        server.expect(requestTo("http://commerce.test/api/products?page=0&size=50"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(
                        withSuccess(
                                """
                {"totalElements":1,"content":[{"id":1,"slug":"headphones","name":"Headphones","description":"Quiet audio",
                 "brand":"Demo","price":89,"currency":"EUR","imagePath":"/image.svg","categoryName":"Electronics","stockQuantity":3,"active":true}]}
                """,
                                MediaType.APPLICATION_JSON));
        assertEquals("headphones", api.publicCatalog().getFirst().slug());
    }

    @Test
    void invalidBudgetResultsAreRejected() {
        server.expect(
                        requestTo(
                                "http://commerce.test/api/products?maxPriceExclusive=50&page=0&size=10"))
                .andRespond(
                        withSuccess(
                                "{\"totalElements\":1,\"content\":[{\"id\":1,\"name\":\"Demo\",\"price\":50,\"currency\":\"EUR\"}]}",
                                MediaType.APPLICATION_JSON));
        assertThrows(AiException.class, () -> api.search(new BigDecimal("50"), "synthetic-token"));
    }

    @Test
    void malformedProductStockRatingAndTextAreRejected() {
        String template =
                """
                {"id":1,"name":"Demo","description":"Guide","brand":"Demo","price":49,"currency":"EUR",
                 "stockQuantity":STOCK,"rating":RATING}
                """;
        for (String body :
                List.of(
                        template.replace("STOCK", "-1").replace("RATING", "4"),
                        template.replace("STOCK", "3").replace("RATING", "null"),
                        template.replace("STOCK", "3")
                                .replace("RATING", "4")
                                .replace("\"Guide\"", "null"))) {
            server.expect(requestTo("http://commerce.test/api/products/1"))
                    .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertThrows(AiException.class, () -> api.product(1, "synthetic-token"));
            server.verify();
            server.reset();
        }
    }

    @Test
    void missingCatalogFieldsCannotBecomeFabricatedStockOrEvidence() {
        server.expect(requestTo("http://commerce.test/api/products?page=0&size=50"))
                .andRespond(
                        withSuccess(
                                "{\"totalElements\":1,\"content\":[{\"id\":1,\"name\":\"Demo\",\"price\":49,\"currency\":\"EUR\"}]}",
                                MediaType.APPLICATION_JSON));
        assertThrows(AiException.class, () -> api.publicCatalog());
    }
}
