# Hook Smith

*Forging HOOK data warehouse structures with precision*

[![License](https://img.shields.io/badge/License-GPL%203.0-blue.svg)](LICENSE)
[![Clojure](https://img.shields.io/badge/Clojure-Babashka-green.svg)](https://babashka.org/)

## Overview

Hook Smith is a command-line tool for implementing the HOOK data warehousing methodology in SQL or Qlik environments. It automates the creation of frames, hooks, and Puppini bridges for Universal Star Schema (USS) implementations.

The HOOK methodology focuses on simplicity, avoiding complex transformations in favor of organizing data through business concepts and hooks. This tool helps you implement HOOK principles without manual coding.

### Key Features

- 📝 **Blueprint generation**: Create a template YAML configuration files for your data warehouse.
- ⚒️ **Frame forging**: Generate a *Hook* script based on your blueprints.
- 🌉 **Bridge building**: Generate a *Unified Star Schema* script based on your blueprints.
- 📔 **Journal creation**: Generate documentation and visualizations of your data structure.

## Prerequisites

- [Babashka](https://github.com/babashka/babashka#installation)

## Usage
```bash
# Start by creating a blueprint YAML configuration:
bb hook blueprint

# Generate frame scripts based on your blueprint:
bb hook forge

# Generate a Unified Star Schema:
bb hook uss

# Generate markdown documentation of your data structure:
hook journal
```

## Blueprint Structure

### concept.yaml
```yaml
- name: product
  type: core

- name: customer
  type: core

- name: order
  type: core
```

### keysets.yaml
```yaml
- name: source.product.id
  concept: product
  qualifier: id
  source_system: source

- name: source.product.name
  concept: product
  qualifier: name
  source_system: source

- name: source.customer.id
  concept: customer
  qualifier: id
  source_system: source

- name: source.customer.name
  concept: customer
  qualifier: name
  source_system: source

- name: source.order.id
  concept: order
  qualifier: id
  source_system: source

- name: source.order.order_number
  concept: order
  qualifier: order_number
  source_system: source
```

### frames.yaml
```yaml
- name: source__products
  source_system: source
  source_table: lib://adss/das/source/raw__source__products.qvd
  target_table: lib://adss/dab/source/frame__source__products.qvd
  hooks:
  - name: hook__product__id
    primary: true
    concept: product
    qualifier: id
    keyset: source.product.id
    business_key_field: id

  - name: hook__product__name
    primary: false
    concept: product
    qualifier: name
    keyset: source.product.name
    business_key_field: name

- name: source__customers
  source_system: source
  source_table: lib://adss/das/source/raw__source__customers.qvd
  target_table: lib://adss/dab/source/frame__source__customers.qvd
  hooks:
  - name: hook__customer__id
    primary: true
    concept: customer
    qualifier: id
    keyset: source.customer.id
    business_key_field: id

  - name: hook__customer__name
    primary: false
    concept: customer
    qualifier: name
    keyset: source.customer.name
    business_key_field: name

- name: source__orders
  source_system: source
  source_table: lib://adss/das/source/raw__source__orders.qvd
  target_table: lib://adss/dab/source/frame__source__orders.qvd
  hooks:
  - name: hook__order__id
    primary: true
    concept: order
    qualifier: id
    keyset: source.order.id
    business_key_field: id

  - name: hook__order__order_number
    primary: false
    concept: order
    qualifier: order_number
    keyset: source.order.order_number
    business_key_field: order_number

  - name: hook__customer__id
    primary: false
    concept: customer
    qualifier: id
    keyset: source.customer.id
    business_key_field: customer_id

- name: source__order_lines
  source_system: source
  source_table: lib://adss/das/source/raw__source__order_lines.qvd
  target_table: lib://adss/dab/source/frame__source__order_lines.qvd
  hooks:
  - name: hook__order__id
    primary: false
    concept: order
    qualifier: id
    keyset: source.order.id
    business_key_field: order_id

  - name: hook__product__id
    primary: false
    concept: product
    qualifier: id
    keyset: source.product.id
    business_key_field: product_id

  composite_hooks:
  - name: hook__order__product__id
    primary: true
    hooks:
    - hook__order__id
    - hook__product__id
```

## unified-star-schema.yaml
```yaml
bridge_path: lib://adss/dar/_bridge.qvd
peripherals:
- name: source__products
  source_table: lib://adss/dab/source/frame__source__products.qvd
  target_table: lib://adss/dar/source__products.qvd
  valid_from: Record Valid From
  valid_to: Record Valid To

- name: source__customers
  source_table: lib://adss/dab/source/frame__source__customers.qvd
  target_table: lib://adss/dar/source__customers.qvd
  valid_from: Record Valid From
  valid_to: Record Valid To

- name: source__orders
  source_table: lib://adss/dab/source/frame__source__orders.qvd
  target_table: lib://adss/dar/source__orders.qvd
  valid_from: Record Valid From
  valid_to: Record Valid To
  events:
  - name: Order Placed On
  - name: Order Due On
  - name: Order Delivered On
    field: order_delivered_on

- name: source__order_lines
  source_table: lib://adss/dab/source/frame__source__order_lines.qvd
  target_table: lib://adss/dar/source__order_lines.qvd
  valid_from: Record Valid From
  valid_to: Record Valid To
```