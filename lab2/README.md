# Bakery Management System — Laboratory Work #2

- **Author:** Bulimar Rodion
- **Group:** FAF-242
- **Checked by:** Istrati D., university assistant
- **Faculty:** FCIM, UTM

> **Laboratory Work #2 — Design Patterns.**  
> This report extends the codebase delivered in *Laboratory Work #1* (Bakery Management System) with three classic design patterns: **Singleton**, **Builder**, and **Factory Method**.

---

## Table of Contents

- [1. Introduction](#1-introduction)
- [2. Problem Statement](#2-problem-statement)
- [3. Design](#3-design)
  - [3.1 Overview](#31-overview)
  - [3.2 UML Class Diagram](#32-uml-class-diagram)
  - [3.3 Design Rationale](#33-design-rationale)
  - [3.4 Relationship to Laboratory Work #1](#34-relationship-to-laboratory-work-1)
- [4. Implementation](#4-implementation)
  - [4.1 Project Structure](#41-project-structure)
  - [4.2 Source Code](#42-source-code)
  - [4.3 Getting Started](#43-getting-started)
- [5. Sample Output](#5-sample-output)
- [6. Extending the Program](#6-extending-the-program)
- [7. Conclusion](#7-conclusion)
- [8. References](#8-references)
- [9. License](#9-license)

---

## 1. Introduction

This report presents the second iteration of a small bakery management system written in Java. Where Laboratory Work #1 focused on the **SOLID principles**, this iteration focuses on **three design patterns** applied to the same domain:

- **Singleton** — one consistent source of bakery-wide settings.
- **Builder** — step-by-step construction of complex `CustomerOrder` objects.
- **Factory Method** — centralized creation of payment processors.

The program still simulates a typical bakery day: baking bread, managing inventory, ordering supplies, serving customers with different payment methods, and cleaning the bakery. The patterns are applied because they address concrete pain points that arose in the Lab 1 code (hard-coded configuration, constructor telescoping, scattered `new` calls) — not for their own sake.

The **Strategy** pattern introduced in Lab 1 (the `PaymentProcessor` abstraction injected into `CustomerService`) is kept as-is and now coexists with the factory, showing how multiple patterns compose in one small codebase.

---

## 2. Problem Statement

Starting from the Lab 1 program, three new needs emerged as the bakery grew:

1. **Global consistency.** Bakery-wide settings (name, currency, tax rate) were previously absent from the model; every order must now compute tax from the *same* configuration, so a single shared instance is needed.
2. **Complex orders.** A customer order can have any combination of items, a percentage discount, a special note, and a delivery flag. A telescoping constructor would need up to 16 overloads; setters would leave the object mutable and half-initialized.
3. **Centralized creation.** Payment processors were instantiated directly in `main`. As more payment methods appear, creation logic should live in one place so the rest of the code depends only on the `PaymentProcessor` abstraction.

The system must continue to:

4. Perform daily bakery operations (baking, inventory, supply ordering, cleaning).
5. Accept payments via multiple methods and remain **open for extension** when new methods are added.
6. Be easy to test and maintain.

---

## 3. Design

### 3.1 Overview

The system is composed of **independent, single-purpose components**:

- Four bakery operation classes: `BreadBaker`, `InventoryManager`, `SupplyOrder`, and `BakeryCleaner`.
- One service class: `CustomerService`, which handles customer interaction.
- One abstract payment base class: `PaymentProcessor`, with two concrete implementations: `CreditCardPaymentProcessor` and `PayPalPaymentProcessor`.
- **`PaymentProcessorFactory`** — *new in Lab 2* — the single place that constructs concrete payment processors.
- **`BakeryConfig`** — *new in Lab 2* — a singleton exposing the bakery name, currency, and tax rate.
- **`CustomerOrder` with nested `CustomerOrder.Builder`** — *new in Lab 2* — an immutable order constructed fluently.
- One entry point: `Main`, which wires all components together.

The design avoids conditional branching for payment handling inside `CustomerService`. Instead, the service depends on the `PaymentProcessor` abstraction and receives a concrete processor via its constructor. New payment methods are added by extending `PaymentProcessor` and registering them in the factory — without touching `CustomerService`.

### 3.2 UML Class Diagram

Figure 1 shows the class structure of the system, color-coded by pattern role.

```dot
digraph BakeryUML {
    graph [rankdir=TB, fontname="Helvetica", nodesep=0.5, ranksep=0.7, splines=ortho];
    node  [shape=record, fontname="Helvetica", fontsize=10, style="filled", fillcolor="#f7f7f7"];
    edge  [fontname="Helvetica", fontsize=9, color="#444444"];

    PaymentProcessor [fillcolor="#e8eef7",
        label="{«abstract»\nPaymentProcessor|\l|+ processPayment(amount: double): void\l}"];
    CreditCardPaymentProcessor [fillcolor="#dbe5f1",
        label="{CreditCardPaymentProcessor|\l|+ processPayment(amount: double): void\l}"];
    PayPalPaymentProcessor [fillcolor="#dbe5f1",
        label="{PayPalPaymentProcessor|\l|+ processPayment(amount: double): void\l}"];

    PaymentProcessorFactory [fillcolor="#fff2cc",
        label="{PaymentProcessorFactory|\l|{static} create(type: String): PaymentProcessor\l}"];

    BakeryConfig [fillcolor="#d9ead3",
        label="{«singleton»\nBakeryConfig|\l- {static} instance: BakeryConfig\l- bakeryName: String\l- currency: String\l- taxRate: double\l|+ {static} getInstance(): BakeryConfig\l+ getBakeryName(): String\l+ getCurrency(): String\l+ getTaxRate(): double\l}"];

    CustomerOrder [fillcolor="#fce5cd",
        label="{CustomerOrder|\l- customerName: String\l- items: List<String>\l- discountPercent: double\l- specialNote: String\l- delivery: boolean\l|+ calculateTotal(): double\l+ toString(): String\l}"];
    CustomerOrderBuilder [fillcolor="#fce5cd",
        label="{«builder»\nCustomerOrder.Builder|\l- customerName: String\l- items: List<String>\l- discountPercent: double\l- specialNote: String\l- delivery: boolean\l|+ Builder(name: String)\l+ addItem(item: String): Builder\l+ discount(p: double): Builder\l+ note(n: String): Builder\l+ withDelivery(): Builder\l+ build(): CustomerOrder\l}"];

    CustomerService [fillcolor="#dbe5f1",
        label="{CustomerService|\l- paymentProcessor: PaymentProcessor\l|+ CustomerService(p: PaymentProcessor)\l+ serveCustomer(order: CustomerOrder): void\l}"];

    BreadBaker       [label="{BreadBaker|\l|+ bakeBread(): void\l}"];
    InventoryManager [label="{InventoryManager|\l|+ manageInventory(): void\l}"];
    SupplyOrder      [label="{SupplyOrder|\l|+ orderSupplies(): void\l}"];
    BakeryCleaner    [label="{BakeryCleaner|\l|+ cleanBakery(): void\l}"];

    Main [shape=box, fillcolor="#ead1dc", label="Main\n+ main(args: String[]): void"];

    CreditCardPaymentProcessor -> PaymentProcessor [arrowhead=onormal];
    PayPalPaymentProcessor     -> PaymentProcessor [arrowhead=onormal];

    PaymentProcessorFactory -> PaymentProcessor            [style=dashed, arrowhead=vee, label="«creates»"];
    PaymentProcessorFactory -> CreditCardPaymentProcessor  [style=dashed, arrowhead=vee];
    PaymentProcessorFactory -> PayPalPaymentProcessor      [style=dashed, arrowhead=vee];

    CustomerOrderBuilder -> CustomerOrder [style=dashed, arrowhead=vee, label="«creates»"];

    CustomerService -> PaymentProcessor [arrowhead=diamond, style=solid, label="has-a"];
    CustomerService -> CustomerOrder    [arrowhead=vee,   style=solid, label="uses"];
    CustomerOrder -> BakeryConfig       [style=dashed, arrowhead=vee, label="«uses»"];

    Main -> CustomerService          [style=dotted, arrowhead=vee];
    Main -> PaymentProcessorFactory  [style=dotted, arrowhead=vee];
    Main -> CustomerOrderBuilder     [style=dotted, arrowhead=vee];
    Main -> BakeryConfig             [style=dotted, arrowhead=vee];
}
```
<img width="1447" height="488" alt="image" src="https://github.com/user-attachments/assets/96f8744b-8c6a-4392-8544-a695b0042b75" />

**Figure 1:** UML class diagram of the Bakery Management System (Lab 2).

**Figure 1 — Legend:**

| Color | Pattern / role |
|-------|----------------|
| Green | **Singleton** (`BakeryConfig`) |
| Orange | **Builder** (`CustomerOrder` + `CustomerOrder.Builder`) |
| Yellow | **Factory Method** (`PaymentProcessorFactory`) |
| Light blue | Abstract / Strategy classes (`PaymentProcessor`, `CustomerService`) |
| Pink | Entry point (`Main`) |
| Grey | Plain single-responsibility components |

| Arrow | Meaning |
|-------|---------|
| Solid line, open triangle | Inheritance (`extends`) |
| Solid line, open diamond | Aggregation (has-a) |
| Dashed line, open "vee" | Creates / uses |
| Dotted line, open "vee" | Orchestration from `Main` |

### 3.3 Design Rationale

#### 3.3.1 Singleton — `BakeryConfig`

Bakery-wide settings (name, currency, tax rate) must be consistent everywhere they are read. If `CustomerOrder` computed tax from one value and a future reporting class from another, orders would silently disagree. The **Singleton** pattern guarantees one instance and one source of truth.

Key points:

- Constructor is **private** — prevents accidental `new BakeryConfig()`.
- Access is only through `getInstance()`, which is `synchronized` to be thread-safe.
- All fields are `final` — the configuration is immutable after creation.

#### 3.3.2 Builder — `CustomerOrder`

An order may have any combination of items, discount, note, and delivery. A telescoping constructor would require many overloads; setters would leave the object mutable. The **Builder** pattern solves both with a fluent API and an immutable product.

Key points:

- `CustomerOrder` has a **private** constructor accepting the builder — the only way to create it is `Builder.build()`.
- All product fields are `final`, so a built order cannot be modified.
- The builder is a **static nested class**, keeping it close to its product.
- Only `customerName` is required; everything else is optional.

#### 3.3.3 Factory Method — `PaymentProcessorFactory`

Instead of `new CreditCardPaymentProcessor()` and `new PayPalPaymentProcessor()` scattered across the codebase, creation is delegated to one factory.

Key points:

- Callers (`Main`, `CustomerService`) never reference concrete processor classes — only the `PaymentProcessor` abstraction and the factory.
- Adding a new payment method requires adding one `case` — the rest of the system is untouched.
- Invalid types fail fast with a clear exception instead of returning `null`.

#### 3.3.4 Supporting pattern — Strategy (from Lab 1)

`CustomerService` still depends only on the `PaymentProcessor` abstraction and receives a concrete processor via constructor injection. This is unchanged from Lab 1 and is what makes the factory's output usable: the factory decides *what* processor to create; `CustomerService` decides *when* to invoke it.

### 3.4 Relationship to Laboratory Work #1

Lab 2 does **not** discard Lab 1 — it builds on it. The table below shows what changed.

| Component | Lab 1 status | Lab 2 status |
|---|---|---|
| `PaymentProcessor` + 2 subclasses | Present (Strategy) | Unchanged |
| `CustomerService` | Present (Strategy, DIP) | Unchanged signature; now accepts `CustomerOrder` instead of a raw `double` |
| `BreadBaker`, `InventoryManager`, `SupplyOrder`, `BakeryCleaner` | Present (SRP) | Unchanged |
| `Main` | Direct `new` for payment processors | Uses `PaymentProcessorFactory` |
| `PaymentProcessorFactory` | — | **New** (Factory Method) |
| `BakeryConfig` | — | **New** (Singleton) |
| `CustomerOrder` + `Builder` | — | **New** (Builder) |

The SOLID principles from Lab 1 remain visible (SRP in the bakery roles, OCP via the factory, DIP in `CustomerService`), but they are now **implied** rather than the headline of the report.

---

## 4. Implementation

### 4.1 Project Structure

The whole program is intentionally kept in a single `Main.java` file (Java 8 compatible) for a self-contained lab submission:

```
.
├── .gitignore
├── README.md
├── docs/
│   ├── bakery-uml.dot        # Graphviz source for Figure 1
│   └── bakery-uml.png        # Rendered UML diagram
└── Main.java                 # Whole program, single-file Java 8 compatible
```

### 4.2 Source Code

**`Main.java`** (complete, Java 8 compatible):

```java
import java.util.*;

class BakeryConfig {
    private static BakeryConfig instance;
    private final String bakeryName;
    private final String currency;
    private final double taxRate;
    private BakeryConfig() {
        this.bakeryName = "Sweet Crumb Bakery";
        this.currency = "USD";
        this.taxRate = 0.08;
    }
    public static synchronized BakeryConfig getInstance() {
        if (instance == null) instance = new BakeryConfig();
        return instance;
    }
    public String getBakeryName() { return bakeryName; }
    public String getCurrency()   { return currency; }
    public double getTaxRate()    { return taxRate; }
}

abstract class PaymentProcessor {
    public abstract void processPayment(double amount);
}
class CreditCardPaymentProcessor extends PaymentProcessor {
    @Override public void processPayment(double amount) {
        System.out.println("Processing credit card payment of $" + amount);
    }
}
class PayPalPaymentProcessor extends PaymentProcessor {
    @Override public void processPayment(double amount) {
        System.out.println("Processing PayPal payment of $" + amount);
    }
}
class PaymentProcessorFactory {
    public static PaymentProcessor create(String type) {
        switch (type.toLowerCase()) {
            case "creditcard":
                return new CreditCardPaymentProcessor();
            case "paypal":
                return new PayPalPaymentProcessor();
            default:
                throw new IllegalArgumentException(
                        "Unsupported payment type: " + type);
        }
    }
}

class CustomerOrder {
    private final String customerName;
    private final List<String> items;
    private final double discountPercent;
    private final String specialNote;
    private final boolean delivery;

    private CustomerOrder(Builder b) {
        this.customerName    = b.customerName;
        this.items           = b.items;
        this.discountPercent = b.discountPercent;
        this.specialNote     = b.specialNote;
        this.delivery        = b.delivery;
    }

    public static class Builder {
        private final String customerName;
        private List<String> items = new ArrayList<>();
        private double discountPercent = 0.0;
        private String specialNote = "";
        private boolean delivery = false;

        public Builder(String customerName) { this.customerName = customerName; }
        public Builder addItem(String item) { items.add(item); return this; }
        public Builder discount(double p)   { this.discountPercent = p; return this; }
        public Builder note(String n)       { this.specialNote = n; return this; }
        public Builder withDelivery()       { this.delivery = true; return this; }
        public CustomerOrder build()        { return new CustomerOrder(this); }
    }

    public double calculateTotal() {
        double subtotal = items.size() * 5.0;
        double afterDiscount = subtotal * (1 - discountPercent / 100.0);
        double tax = afterDiscount * BakeryConfig.getInstance().getTaxRate();
        double total = afterDiscount + tax + (delivery ? 7.5 : 0.0);
        return Math.round(total * 100.0) / 100.0;
    }

    @Override public String toString() {
        return "Order for " + customerName + " | items=" + items +
               " | discount=" + discountPercent + "% | note='" + specialNote +
               "' | delivery=" + delivery + " | total=$" + calculateTotal();
    }
}

class BreadBaker {
    public void bakeBread() { System.out.println("Baking high-quality bread..."); }
}
class InventoryManager {
    public void manageInventory() { System.out.println("Managing inventory..."); }
}
class SupplyOrder {
    public void orderSupplies() { System.out.println("Ordering supplies..."); }
}
class BakeryCleaner {
    public void cleanBakery() { System.out.println("Cleaning the bakery..."); }
}

class CustomerService {
    private final PaymentProcessor paymentProcessor;
    public CustomerService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
    public void serveCustomer(CustomerOrder order) {
        System.out.println("Serving customer " + order);
        paymentProcessor.processPayment(order.calculateTotal());
    }
}

public class Main {
    public static void main(String[] args) {
        BakeryConfig config = BakeryConfig.getInstance();
        System.out.println("Welcome to " + config.getBakeryName() +
                " (currency=" + config.getCurrency() +
                ", tax=" + (config.getTaxRate() * 100) + "%)");

        new BreadBaker().bakeBread();
        new InventoryManager().manageInventory();
        new SupplyOrder().orderSupplies();

        CustomerOrder order1 = new CustomerOrder.Builder("Alice")
                .addItem("Sourdough")
                .addItem("Croissant")
                .discount(10)
                .note("No nuts")
                .withDelivery()
                .build();

        CustomerService ccService = new CustomerService(
                PaymentProcessorFactory.create("creditcard"));
        ccService.serveCustomer(order1);

        new BakeryCleaner().cleanBakery();

        CustomerOrder order2 = new CustomerOrder.Builder("Bob")
                .addItem("Baguette")
                .build();
        System.out.println("\nNew customer pays with PayPal...");
        CustomerService ppService = new CustomerService(
                PaymentProcessorFactory.create("paypal"));
        ppService.serveCustomer(order2);
    }
}
```

### 4.3 Getting Started

**Prerequisites:** JDK 8+, Graphviz (only to regenerate the diagram).

```bash
javac --release 8 Main.java
java Main
```

Optional — regenerate the UML diagram:

```bash
dot -Tpng docs/bakery-uml.dot -o docs/bakery-uml.png
```

---

## 5. Sample Output

```
Welcome to Sweet Crumb Bakery (currency=USD, tax=8.0%)
Baking high-quality bread...
Managing inventory...
Ordering supplies...
Serving customer Order for Alice | items=[Sourdough, Croissant] | discount=10.0% | note='No nuts' | delivery=true | total=$17.22
Processing credit card payment of $17.22
Cleaning the bakery...

New customer pays with PayPal...
Serving customer Order for Bob | items=[Baguette] | discount=0.0% | note='' | delivery=false | total=$5.4
Processing PayPal payment of $5.4
```

---

## 6. Extending the Program

### 6.1 New payment method (Factory Method)

```java
class CryptoPaymentProcessor extends PaymentProcessor {
    @Override public void processPayment(double amount) {
        System.out.println("Processing crypto payment of $" + amount);
    }
}
// add to PaymentProcessorFactory.create(...):
// case "crypto": return new CryptoPaymentProcessor();
```

Only one new class + one `case` — everything else untouched.

### 6.2 New order attribute (Builder)

```java
// in CustomerOrder.Builder
private boolean giftWrap = false;
public Builder giftWrap() { this.giftWrap = true; return this; }
```

Existing callers compile and behave unchanged; new callers opt in fluently.

### 6.3 New bakery-wide setting (Singleton)

Add a `final` field to `BakeryConfig`'s private constructor and a getter. Because all consumers go through `getInstance()`, they immediately see the new value without any code change on their side.

---

## 7. Conclusion

Laboratory Work #2 extended the Lab 1 codebase with three design patterns, each solving a concrete pain point:

- **Singleton** (`BakeryConfig`) — one consistent, immutable source of bakery-wide settings.
- **Builder** (`CustomerOrder` + `Builder`) — fluent construction of complex orders with only required fields, producing an immutable product.
- **Factory Method** (`PaymentProcessorFactory`) — centralized, type-safe creation of payment processors; adding a new method touches one file.

The **Strategy** pattern from Lab 1 is preserved and now complements the factory: the factory decides *what* to create, the injected abstraction decides *how* `CustomerService` uses it. Together the patterns keep the program open for extension while keeping each class small and testable.

Comparing the two labs: Lab 1 taught *why* classes should be small and depend on abstractions (SOLID); Lab 2 shows *how* to instantiate and assemble those abstractions cleanly as the program grows (patterns). Neither lab works as well without the other.

---

## 8. References

1. Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
2. Martin, R. C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.
3. Oracle. *The Java Tutorials — Object-Oriented Programming Concepts*. https://docs.oracle.com/javase/tutorial/java/concepts/
4. Graphviz Documentation. https://graphviz.org/documentation/

---

## 9. License

This project is provided as-is for educational purposes. Feel free to use, modify, and share it.
