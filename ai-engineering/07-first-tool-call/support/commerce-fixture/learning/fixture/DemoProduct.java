package learning.fixture;

import java.math.BigDecimal;

record DemoProduct(long id, BigDecimal price) {
    String json() {
        return """
                {"id":%d,"slug":"demo-%d","name":"Demo product %d",
                 "description":"Synthetic fixture product","brand":"Demo","price":"%s",
                 "currency":"EUR","categoryName":"Demo","stockQuantity":3,"rating":"4.0","active":true}
                """
                .formatted(id, id, id, price.toPlainString())
                .strip();
    }
}
