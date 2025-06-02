# Documentation

## Hook
```mermaid
flowchart LR
    %% Entities
    frame__source__products("
        **FRAME__SOURCE__PRODUCTS**
        **Primary Key:**
        hook__product__id
        &nbsp;
        **Foreign Keys:**
        hook__product__name
        &nbsp;
        **Fields:**
        ...
    ")

    frame__source__customers("
        **FRAME__SOURCE__CUSTOMERS**
        **Primary Key:**
        hook__customer__id
        &nbsp;
        **Foreign Keys:**
        hook__customer__name
        &nbsp;
        **Fields:**
        ...
    ")

    frame__source__orders("
        **FRAME__SOURCE__ORDERS**
        **Primary Key:**
        hook__order__id
        &nbsp;
        **Foreign Keys:**
        hook__order__order_number
        hook__customer__id
        &nbsp;
        **Fields:**
        ...
    ")

    frame__source__order_lines("
        **FRAME__SOURCE__ORDER_LINES**
        **Primary Key:**
        hook__order__product__id
        &nbsp;
        **Foreign Keys:**
        hook__order__id
        hook__product__id
        hook__order__order_number
        &nbsp;
        **Fields:**
        ...
    ")

    frame__source__customer_orders("
        **FRAME__SOURCE__CUSTOMER_ORDERS**
        **Primary Key:**
        hook__customer__order__id
        &nbsp;
        **Foreign Keys:**
        hook__customer__id
        hook__order__id
        &nbsp;
        **Fields:**
        ...
    ")

    %% Relations
    frame__source__orders -- hook__customer__id --> frame__source__customers

    frame__source__order_lines -- hook__order__id --> frame__source__orders
    frame__source__order_lines -- hook__product__id --> frame__source__products

    frame__source__customer_orders -- hook__customer__id --> frame__source__customers
    frame__source__customer_orders -- hook__order__id --> frame__source__orders
```

## Unified Star Schema
```mermaid
flowchart TD
    %% Entities
    bridge("
        **_BRIDGE**
        **Primary Key:**
        key__bridge
        &nbsp;
        **Foreign Keys:**
        pit_hook__product__id
        pit_hook__customer__id
        pit_hook__order__id
        pit_hook__order__product__id
        pit_hook__customer__order__id
        &nbsp;
        **Fields:**
        Peripheral
    ")
    source__products("
        **SOURCE__PRODUCTS**
        **Primary Key:**
        pit_hook__product__id
        &nbsp;
        **Fields:**
        ...
    ")

    source__customers("
        **SOURCE__CUSTOMERS**
        **Primary Key:**
        pit_hook__customer__id
        &nbsp;
        **Fields:**
        ...
    ")

    source__orders("
        **SOURCE__ORDERS**
        **Primary Key:**
        pit_hook__order__id
        &nbsp;
        **Fields:**
        ...
    ")

    source__order_lines("
        **SOURCE__ORDER_LINES**
        **Primary Key:**
        pit_hook__order__product__id
        &nbsp;
        **Fields:**
        ...
    ")

    source__customer_orders("
        **SOURCE__CUSTOMER_ORDERS**
        **Primary Key:**
        pit_hook__customer__order__id
        &nbsp;
        **Fields:**
        ...
    ")

    %% Relations
    bridge -- pit_hook__product__id --> source__products
    bridge -- pit_hook__customer__id --> source__customers
    bridge -- pit_hook__order__id --> source__orders
    bridge -- pit_hook__order__product__id --> source__order_lines
    bridge -- pit_hook__customer__order__id --> source__customer_orders

```