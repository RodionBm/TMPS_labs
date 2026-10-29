# Bakery Management System — Laboratory Work #2

- **Author:** Bulimar Rodion
- **Group:** FAF-242
- **Checked by:** Istrati D., university assistant
- **Faculty:** FCIM, UTM

> **Laboratory Work #2 — Design Patterns.**  
> This report extends the codebase delivered in *Laboratory Work #1* (Bakery Management System) with three classic design patterns: **Singleton**, **Builder**, and **Factory Method**. The **Strategy** pattern from Lab 1 is preserved and now coexists with the factory. All customer data is read **interactively from the keyboard** using `java.util.Scanner`, with full input validation.

---

## Table of Contents

- [1. Introduction](#1-introduction)
- [2. Problem Statement](#2-problem-statement)
- [3. Design](#3-design)
  - [3.1 Overview](#31-overview)
  - [3.2 UML Class Diagram — Full System](#32-uml-class-diagram--full-system)
  - [3.3 UML Diagram per Pattern](#33-uml-diagram-per-pattern)
  - [3.4 Design Rationale](#34-design-rationale)
  - [3.5 Relationship to Laboratory Work #1](#35-relationship-to-laboratory-work-1)
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

The program simulates a typical bakery day: baking bread, managing inventory, ordering supplies, serving customers with different payment methods, and cleaning the bakery. The patterns are applied because they address concrete pain points that arose in the Lab 1 code (hard-coded configuration, constructor telescoping, scattered `new` calls) — not for their own sake.

Additionally, the program is now **interactive**: all customer data (name, items, discount, note, delivery, payment type) is read from the keyboard using `java.util.Scanner`. Every input is validated so the program never crashes on malformed data.

The **Strategy** pattern introduced in Lab 1 (the `PaymentProcessor` abstraction injected into `CustomerService`) is kept as-is and now coexists with the factory, showing how multiple patterns compose in one small codebase.

**Pattern count:** 3 new patterns (Singleton, Builder, Factory Method) + 1 inherited pattern (Strategy) = **4 patterns total**.

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

New in Lab 2:

7. **Accept customer orders and payment choices interactively from the keyboard**, validating every input (non-empty names, integer ranges, recognized payment types) before using it.

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
- One entry point: `Main`, which wires all components together and reads input from the user via `Scanner`.

The design avoids conditional branching for payment handling inside `CustomerService`. Instead, the service depends on the `PaymentProcessor` abstraction and receives a concrete processor via its constructor. New payment methods are added by extending `PaymentProcessor` and registering them in the factory — without touching `CustomerService`.

### 3.2 UML Class Diagram — Full System

Figure 1 shows the complete class structure, color-coded by pattern role.

<img width="1283" height="539" alt="Full UML class diagram" src="docs/bakery-uml.png" />

**Figure 1:** Full UML class diagram of the Bakery Management System (Lab 2).

**Legend — colors:**

| Color | Pattern / role |
|-------|----------------|
| Green | **Singleton** (`BakeryConfig`) |
| Orange | **Builder** (`CustomerOrder` + `CustomerOrder.Builder`) |
| Yellow | **Factory Method** (`PaymentProcessorFactory`) |
| Light blue | Abstract / Strategy classes (`PaymentProcessor`, `CustomerService`) |
| Pink | Entry point (`Main`) |
| Grey | Plain single-responsibility components |

**Legend — arrows:**

| Arrow | Meaning |
|-------|---------|
| Solid line, open triangle | Inheritance (`extends`) |
| Solid line, open diamond | Aggregation (has-a) |
| Dashed line, open "vee" | Creates / uses |
| Dotted line, open "vee" | Orchestration from `Main` |

### 3.3 UML Diagram per Pattern

Each pattern is shown in isolation below so the structure is unambiguous.

#### 3.3.1 Singleton — `BakeryConfig`

<img width="940" height="588" alt="image" src="https://github.com/user-attachments/assets/c0ee40c8-d1d5-409b-8ef8-3fdbcd5af976" />


**Figure 2:** Singleton pattern — `BakeryConfig`.

- `BakeryConfig` has a **private constructor** (not shown, implicit) and a `static` field `instance`.
- `getInstance()` is `static` and `synchronized` — one global entry point, thread-safe.
- Both `Main` and `CustomerOrder` read from it via `«uses»`.

#### 3.3.2 Builder — `CustomerOrder` + `Builder`

<img width="874" height="289" alt="image" src="https://github.com/user-attachments/assets/69b71e11-200d-43a0-b238-ff562af4ebb7" />


**Figure 3:** Builder pattern — `CustomerOrder` + `CustomerOrder.Builder`.

- `Builder` is a **static nested class** inside `CustomerOrder` (hence `CustomerOrder$Builder.class` at compile time).
- Fluent methods (`addItem`, `discount`, `note`, `withDelivery`) return `Builder` (`return this`) — shown as the self-loop.
- `build()` is the **only** way to create a `CustomerOrder` (its constructor is private).

#### 3.3.3 Factory Method — `PaymentProcessorFactory`

<img width="957" height="597" alt="image" src="https://github.com/user-attachments/assets/2dc6bc38-917f-4b87-9e43-633ad81aaa42" />


**Figure 4:** Factory Method — `PaymentProcessorFactory`.

- `PaymentProcessor` is the abstract base; the two concrete processors extend it.
- `PaymentProcessorFactory.create(type)` is the **single place** that instantiates concrete processors.
- It returns the abstraction `PaymentProcessor`, never a concrete class.
- `CustomerService` depends on `PaymentProcessor` (has-a), not on the factory.

### 3.4 Design Rationale

#### 3.4.1 Singleton — `BakeryConfig`

Bakery-wide settings (name, currency, tax rate) must be consistent everywhere they are read. If `CustomerOrder` computed tax from one value and a future reporting class from another, orders would silently disagree. The **Singleton** pattern guarantees one instance and one source of truth.

Key points:

- Constructor is **private** — prevents accidental `new BakeryConfig()`.
- Access is only through `getInstance()`, which is `synchronized` to be thread-safe.
- All fields are `final` — the configuration is immutable after creation.

#### 3.4.2 Builder — `CustomerOrder`

An order may have any combination of items, discount, note, and delivery. A telescoping constructor would require many overloads; setters would leave the object mutable. The **Builder** pattern solves both with a fluent API and an immutable product.

Key points:

- `CustomerOrder` has a **private** constructor accepting the builder — the only way to create it is `Builder.build()`.
- All product fields are `final`, so a built order cannot be modified.
- The builder is a **static nested class**, keeping it close to its product.
- Only `customerName` is required; everything else is optional.
- In `Main`, each answer the user gives is applied to the builder via a fluent call, and `.build()` is called once at the end.

#### 3.4.3 Factory Method — `PaymentProcessorFactory`

Instead of `new CreditCardPaymentProcessor()` and `new PayPalPaymentProcessor()` scattered across the codebase, creation is delegated to one factory.

Key points:

- Callers (`Main`, `CustomerService`) never reference concrete processor classes — only the `PaymentProcessor` abstraction and the factory.
- Adding a new payment method requires adding one `case` — the rest of the system is untouched.
- Invalid types fail fast with a clear exception instead of returning `null`.
- In `Main`, the payment type the user types is passed directly to `PaymentProcessorFactory.create(type)`.

#### 3.4.4 Supporting pattern — Strategy (from Lab 1)

`CustomerService` still depends only on the `PaymentProcessor` abstraction and receives a concrete processor via constructor injection. This is unchanged from Lab 1 and is what makes the factory's output usable: the factory decides *what* processor to create; `CustomerService` decides *when* to invoke it.

### 3.5 Relationship to Laboratory Work #1

Lab 2 does **not** discard Lab 1 — it builds on it. The table below shows what changed.

| Component | Lab 1 status | Lab 2 status |
|---|---|---|
| `PaymentProcessor` + 2 subclasses | Present (Strategy) | Unchanged |
| `CustomerService` | Present (Strategy, DIP) | Unchanged signature; now accepts `CustomerOrder` instead of a raw `double` |
| `BreadBaker`, `InventoryManager`, `SupplyOrder`, `BakeryCleaner` | Present (SRP) | Unchanged |
| `Main` | Direct `new` for payment processors; hard-coded data | Uses `PaymentProcessorFactory`; reads all data from the keyboard via `Scanner` |
| `PaymentProcessorFactory` | — | **New** (Factory Method) |
| `BakeryConfig` | — | **New** (Singleton) |
| `CustomerOrder` + `Builder` | — | **New** (Builder) |

**Pattern count:** 3 new patterns in Lab 2 + 1 inherited from Lab 1 = **4 patterns total**.

---

## 4. Implementation

### 4.1 Project Structure

The whole program is intentionally kept in a single `Main.java` file (Java 8 compatible):

```
.
├── .gitignore
├── README.md
├── docs/
│   ├── bakery-uml.dot            # Graphviz source — full diagram
│   ├── bakery-uml.png
│   ├── singleton-uml.dot         # Graphviz source — Singleton
│   ├── singleton-uml.png
│   ├── builder-uml.dot           # Graphviz source — Builder
│   ├── builder-uml.png
│   ├── factory-uml.dot           # Graphviz source — Factory Method
│   ├── factory-uml.png
│   ├── strategy-uml.dot          # Graphviz source — Strategy
│   └── strategy-uml.png
└── Main.java                     # Whole program, single-file Java 8 compatible
```

### 4.2 Source Code

**`Main.java`** (complete, Java 8 compatible, interactive):

```java
import java.util.*;

// ---------- 1. SINGLETON ----------
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

// ---------- 2. FACTORY METHOD ----------
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
            case "credit":
            case "card":
                return new CreditCardPaymentProcessor();
            case "paypal":
                return new PayPalPaymentProcessor();
            default:
                throw new IllegalArgumentException(
                        "Unsupported payment type: " + type);
        }
    }
}

// ---------- 3. BUILDER ----------
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

// ---------- Bakery roles ----------
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

// ---------- Strategy service ----------
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

// ---------- MAIN ----------
public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        BakeryConfig config = BakeryConfig.getInstance();
        System.out.println("Welcome to " + config.getBakeryName() +
                " (currency=" + config.getCurrency() +
                ", tax=" + (config.getTaxRate() * 100) + "%)");

        new BreadBaker().bakeBread();
        new InventoryManager().manageInventory();
        new SupplyOrder().orderSupplies();

        int numberOfOrders = readInt("How many customer orders? ", 1, 100);

        for (int i = 1; i <= numberOfOrders; i++) {
            System.out.println("\n=== Order #" + i + " ===");
            CustomerOrder order = readOrder();

            String payType = readPaymentType();
            PaymentProcessor processor = PaymentProcessorFactory.create(payType);

            CustomerService service = new CustomerService(processor);
            service.serveCustomer(order);

            new BakeryCleaner().cleanBakery();
        }

        System.out.println("\nAll orders processed. Have a nice day!");
        sc.close();
    }

    private static CustomerOrder readOrder() {
        System.out.print("Customer name: ");
        String name = sc.nextLine().trim();
        while (name.isEmpty()) {
            System.out.print("Name cannot be empty. Customer name: ");
            name = sc.nextLine().trim();
        }

        CustomerOrder.Builder builder = new CustomerOrder.Builder(name);

        int itemCount = readInt("How many items? ", 1, 50);
        for (int j = 1; j <= itemCount; j++) {
            System.out.print("  Item #" + j + ": ");
            String item = sc.nextLine().trim();
            while (item.isEmpty()) {
                System.out.print("  Item cannot be empty. Item #" + j + ": ");
                item = sc.nextLine().trim();
            }
            builder.addItem(item);
        }

        int discount = readInt("Discount percent (0-100): ", 0, 100);
        if (discount > 0) builder.discount(discount);

        System.out.print("Special note (leave empty for none): ");
        String note = sc.nextLine().trim();
        if (!note.isEmpty()) builder.note(note);

        String delivery = readYesNo("Delivery? (y/n): ");
        if (delivery.equalsIgnoreCase("y")) builder.withDelivery();

        return builder.build();
    }

    private static String readPaymentType() {
        while (true) {
            System.out.print("Payment type (creditcard / paypal): ");
            String type = sc.nextLine().trim().toLowerCase();
            if (type.equals("creditcard") || type.equals("credit")
                    || type.equals("card")
                    || type.equals("paypal")) {
                return type;
            }
            System.out.println("  Invalid type. Try 'creditcard' or 'paypal'.");
        }
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
                System.out.println("  Please enter a number between "
                        + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Not a valid integer. Try again.");
            }
        }
    }

    private static String readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("n")) return line;
            System.out.println("  Please enter 'y' or 'n'.");
        }
    }
}
```

### 4.3 Getting Started

**Prerequisites:** JDK 8+, Graphviz (only to regenerate the diagrams).

```bash
javac --release 8 Main.java
java Main
```

The program is **interactive** — after launching it, follow the on-screen prompts. Input is validated: invalid numbers, empty names, or unknown payment types cause the program to re-ask instead of crashing.

Regenerate all UML diagrams:

```bash
dot -Tpng docs/bakery-uml.dot    -o docs/bakery-uml.png
dot -Tpng docs/singleton-uml.dot -o docs/singleton-uml.png
dot -Tpng docs/builder-uml.dot   -o docs/builder-uml.png
dot -Tpng docs/factory-uml.dot   -o docs/factory-uml.png
dot -Tpng docs/strategy-uml.dot  -o docs/strategy-uml.png
```

---

## 5. Sample Output

```
Welcome to Sweet Crumb Bakery (currency=USD, tax=8.0%)
Baking high-quality bread...
Managing inventory...
Ordering supplies...
How many customer orders? 2

=== Order #1 ===
Customer name: Alice
How many items? 2
  Item #1: Sourdough
  Item #2: Croissant
Discount percent (0-100): 10
Special note (leave empty for none): No nuts
Delivery? (y/n): y
Payment type (creditcard / paypal): creditcard
Serving customer Order for Alice | items=[Sourdough, Croissant] | discount=10.0% | note='No nuts' | delivery=true | total=$17.22
Processing credit card payment of $17.22
Cleaning the bakery...

=== Order #2 ===
Customer name: Bob
How many items? 1
  Item #1: Baguette
Discount percent (0-100): 0
Special note (leave empty for none): 
Delivery? (y/n): n
Payment type (creditcard / paypal): paypal
Serving customer Order for Bob | items=[Baguette] | discount=0.0% | note='' | delivery=false | total=$5.4
Processing PayPal payment of $5.4
Cleaning the bakery...

All orders processed. Have a nice day!
```

---

## 6. Extending the Program

### 6.1 New payment method (Factory Method)

Add one class and one `case` in the factory — nothing else changes:

```java
class CryptoPaymentProcessor extends PaymentProcessor {
    @Override public void processPayment(double amount) {
        System.out.println("Processing crypto payment of $" + amount);
    }
}
// inside PaymentProcessorFactory.create(...):
// case "crypto":
//     return new CryptoPaymentProcessor();
```

The user can then type `crypto` at the payment prompt — no other change needed.

### 6.2 New order attribute (Builder)

Add one field to `CustomerOrder`, one field to `Builder`, and one fluent setter. Existing callers are unaffected because the builder is append-only.

### 6.3 New bakery-wide setting (Singleton)

Add a `final` field to `BakeryConfig`'s private constructor and a getter. Every consumer reads through `getInstance()` and immediately sees the new value.

### 6.4 New validation rule (input layer)

Add a new helper method in `Main` and call it wherever needed. The patterns are unaffected — this only touches the input layer.

---

## 7. Conclusion

Laboratory Work #2 extended the Lab 1 codebase with three design patterns, each solving a concrete pain point:

- **Singleton** (`BakeryConfig`) — one consistent, immutable source of bakery-wide settings.
- **Builder** (`CustomerOrder` + `Builder`) — fluent construction of complex orders with only required fields, producing an immutable product.
- **Factory Method** (`PaymentProcessorFactory`) — centralized, type-safe creation of payment processors; adding a new method touches one file.

The **Strategy** pattern from Lab 1 is preserved and now complements the factory: the factory decides *what* to create, the injected abstraction decides *how* `CustomerService` uses it.

Additionally, the program now accepts **interactive keyboard input** via `java.util.Scanner`, with validation loops and exception handling so that malformed input is rejected gracefully. This does not change the pattern structure — only `main()` and its helper methods.

**Final count: 3 new patterns + 1 inherited = 4 patterns.**

---

## 8. References

1. Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
2. Martin, R. C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.
3. Oracle. *The Java Tutorials — Object-Oriented Programming Concepts*. https://docs.oracle.com/javase/tutorial/java/concepts/
4. Oracle. *Java API — java.util.Scanner*. https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html
5. Graphviz Documentation. https://graphviz.org/documentation/

---

## 9. License

This project is provided as-is for educational purposes. Feel free to use, modify, and share it.
