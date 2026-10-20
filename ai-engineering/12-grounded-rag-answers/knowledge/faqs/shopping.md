---
source_id: shopping-faq
title: Shopping and account FAQ
source_type: faq
demo: true
---
# Shopping FAQ

This document describes the demo application's supported shopping flow.

## Cart and checkout

Add products to the shopping cart from the storefront. In the cart, update
quantities or remove items before checkout. Checkout uses a dummy payment flow;
it does not charge a real payment card. A successful checkout creates an order.
The Java commerce backend validates stock and calculates the cart and order totals.

## Account and orders

Sign in to view your account, cart and orders. The embedded assistant can read
your current cart, recent orders or a specific order through authorized commerce
APIs. It cannot modify the cart, place an order, take payment or issue a refund.
Information about another customer's orders is not available to you.

## Prices and availability

Prices and available quantities can change. Consult the current product page or
the Java product API for these values; a knowledge document is not authoritative
for prices or stock. An item described here may no longer be active in the catalog.

## Questions without supporting information

If the knowledge sources do not specify a compatibility detail, warranty length
or delivery promise, the assistant should state that it cannot confirm the answer.
