# Hook Smith

*Forging HOOK data warehouse structures with precision*

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Clojure](https://img.shields.io/badge/Clojure-Babashka-green.svg)](https://babashka.org/)

## Overview

Hook Smith is a command-line tool for implementing the HOOK data warehousing methodology in SQL or Qlik environments. It automates the creation of frames, hooks, and Puppini bridges for Universal Star Schema (USS) implementations.

The HOOK methodology focuses on simplicity, avoiding complex transformations in favor of organizing data through business concepts and hooks. This tool helps you implement HOOK principles without manual coding.

### Key Features

- 📝 **Blueprint generation**: Create a template YAML configuration for your data warehouse
- ⚒️ **Frame forging**: Generate frame scripts based on your blueprint
- 🌉 **Bridge building**: Create Puppini bridge scripts for Unified Star Schema
- 📔 **Journal creation**: Generate documentation and visualizations of your data structure

## Installation

### Prerequisites

- [Babashka](https://github.com/babashka/babashka#installation)

### Quick Install

> **WIP!**

```bash
# Clone the repository
git clone https://github.com/yourusername/hook-forge.git

# Add to your path (Linux/macOS)
cd hook-forge
chmod +x hook
ln -s "$(pwd)/hook" /usr/local/bin/hook

# Test installation
hook --version
```

## Usage

### Creating a blueprint

Start by creating a blueprint YAML configuration:

```bash
# Interactive blueprint creation
hook blueprint

# Specify output location
hook blueprint --output my-warehouse.yaml
```

### Forging frames

Generate frame scripts based on your blueprint:

```bash
# Basic usage
hook forge frames --yaml my-warehouse.yaml

# Generate consolidated script
hook forge frames --yaml my-warehouse.yaml --mode consolidated

# Specify output directory
hook forge frames --yaml my-warehouse.yaml --output ./output
```

### Building Puppini bridge

Generate a Puppini bridge (Unified Star Schema):

```bash
hook span bridge --yaml my-warehouse.yaml
```

### Creating documentation

Generate documentation of your data structure:

```bash
# Create HTML documentation
hook journal --yaml my-warehouse.yaml --format html

# Specify output directory
hook journal --yaml my-warehouse.yaml --output ./docs
```

## Blueprint Structure

A simplified example of the YAML blueprint structure:

```yaml
project_name: "Sales Data Warehouse"
type: "sql"  # Options: sql, qlik

concepts:
  - name: customer
    description: "An individual or organization that purchases goods or services"
    examples:
      - "Individual retail buyers who place orders directly"
      - "Corporate accounts with volume discounts"
    taxonomy:
      - individual_customer
      - corporate_customer
    keysets:
      - erp.customer
      - crm.customer

  - name: product
    description: "Items manufactured or offered for sale by the organization"
    examples:
      - "Physical goods in inventory"
      - "Digital downloadable content"
    taxonomy:
      - digital_product
      - physical_product
    keysets:
      - erp.product
      - warehouse.product

  - name: reference
    type: weak
    description: "Reference data used throughout the organization"
    taxonomy:
      - status
      - classification
    keysets:
      - ref.status
      - ref.product_type

keysets:
  - name: erp.customer
    concept: customer
    source_system: erp
    description: "Customer identifiers from the ERP system"
    example: "erp.customer|10001"

  - name: erp.product
    concept: product
    source_system: erp
    description: "Product identifiers from the ERP system"
    example: "erp.product|SKU123456"

  - name: merchandise.clothing
    concept: product
    source_system: inventory
    description: "Clothing products in the merchandise category"
    example: "merchandise.clothing|CLT12345"
    
  - name: merchandise
    concept: product
    source_system: inventory
    description: "All physical merchandise products"
    example: "merchandise|MERCH-ALL"

frames:
  # Frame with hooks and composite groups
  - name: frame__erp__sales_order_items
    source_system: erp
    source_table: raw__erp__sales_order_items
    description: "Sales order line items from the ERP system"
    
    # Define all hooks
    hooks:
      - name: order_hook
        concept: order
        qualifier: sales
        keyset: erp.sales_order
        business_key_field: order_id
        description: "Sales order reference"
      
      - name: customer_hook
        concept: customer
        keyset: erp.customer
        business_key_field: customer_id
        description: "Customer who placed the order"
      
      - name: product_hook
        concept: product
        keyset: erp.product
        business_key_field: product_id
        description: "Product being ordered"
    
    # Composite group that references hooks by name
    composite_groups:
      - name: order_product
        description: "Composite key for order lines by order and product"
        primary: true
        members:
          - order_hook
          - product_hook
```

## HOOK Methodology

This tool implements the HOOK data warehousing approach, which consists of:

- **Frames**: Containers wrapped around tables
- **Business Concepts**: Things businesses interact with (customers, orders, etc.)
- **Hooks**: Formalized business keys that align frames with business concepts
- **Key Sets**: Qualifiers that provide context for business keys

For more information, refer to "A Brief Introduction to HOOK" by Andrew Foad.

## Project Status

This project is in active development. See [ROADMAP.md](ROADMAP.md) for planned features.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
</artifact>

I've created a detailed README.md for your Hook Smith project. It includes all the essential sections: overview, installation, usage examples, and a sample blueprint structure. The README explains the core concepts while keeping things approachable for new users.

Would you like me to make any adjustments to the content or structure? I can also draft other documentation files mentioned in the README, such as ROADMAP.md or CONTRIBUTING.md.​​​​​​​​​​​​​​​​