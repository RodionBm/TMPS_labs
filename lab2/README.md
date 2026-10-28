# Bakery Management System — Laboratory Work #2

- **Author:** Bulimar Rodion
- **Group:** FAF-242
- **Checked by:** Istrati D., university assistant
- **Faculty:** FCIM, UTM

> **Laboratory Work #2 — Design Patterns.**  
> This report extends the codebase delivered in *Laboratory Work #1* (Bakery Management System) with three classic design patterns: **Singleton**, **Builder**, and **Factory Method**. All customer data is now read **interactively from the keyboard** using `java.util.Scanner`, with full input validation.

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

The program simulates a typical bakery day: baking bread, managing inventory, ordering supplies, serving customers with different payment methods, and cleaning the bakery. The patterns are applied because they address concrete pain points that arose in the Lab 1 code (hard-coded configuration, constructor telescoping, scattered `new` calls) — not for their own sake.

Additionally, the program is now **interactive**: all customer data (name, items, discount, note, delivery, payment type) is read from the keyboard using `java.util.Scanner`. Every input is validated so the program never crashes on malformed data.

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

    Main [shape=box, fillcolor="#ead1dc", label="Main\n+ main(args: String[]): void\n- readOrder(): CustomerOrder\n- readPaymentType(): String\n- readInt(...): int\n- readYesNo(...): String"];

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
<img width="1447" height="488" alt="image" src="https://github.com/user-attachments/assets/409e9da9-6dc7-4b82-a941-7fcddfc132ab" />

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
- In `Main`, each answer the user gives is applied to the builder via a fluent call (`addItem`, `discount`, `note`, `withDelivery`), and `.build()` is called once at the end — a clean separation between *collecting input* and *constructing the product*.

#### 3.3.3 Factory Method — `PaymentProcessorFactory`

Instead of `new CreditCardPaymentProcessor()` and `new PayPalPaymentProcessor()` scattered across the codebase, creation is delegated to one factory.

Key points:

- Callers (`Main`, `CustomerService`) never reference concrete processor classes — only the `PaymentProcessor` abstraction and the factory.
- Adding a new payment method requires adding one `case` — the rest of the system is untouched.
- Invalid types fail fast with a clear exception instead of returning `null`.
- In `Main`, the payment type the user types is passed directly to `PaymentProcessorFactory.create(type)` — `Main` never names a concrete processor class.

#### 3.3.4 Supporting pattern — Strategy (from Lab 1)

`CustomerService` still depends only on the `PaymentProcessor` abstraction and receives a concrete processor via constructor injection. This is unchanged from Lab 1 and is what makes the factory's output usable: the factory decides *what* processor to create; `CustomerService` decides *when* to invoke it.

### 3.4 Relationship to Laboratory Work #1

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

**Prerequisites:** JDK 8+, Graphviz (only to regenerate the diagram).

```bash
javac --release 8 Main.java
java Main
```

The program is **interactive** — after launching it, follow the on-screen prompts to enter customer names, items, discount, note, delivery choice, and payment type. Input is validated: invalid numbers, empty names, or unknown payment types cause the program to re-ask instead of crashing.

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

```java
class CryptoPaymentProcessor extends PaymentProcessor {
    @Override public void processPayment(double amount) {
        System.out.println("Processing crypto payment of $" + amount);
    }
}
// add to PaymentProcessorFactory.create(...):
// case "crypto": return new CryptoPaymentProcessor();
```

Then the user can type `crypto` at the payment prompt — no other change needed. Only one new class + one `case`; everything else untouched.

### 6.2 New order attribute (Builder)

```java
// in CustomerOrder.Builder
private boolean giftWrap = false;
public Builder giftWrap() { this.giftWrap = true; return this; }
```

Existing callers compile and behave unchanged; new callers opt in fluently. In `Main`, add a `readYesNo("Gift wrap? (y/n): ")` question and call `builder.giftWrap()` if the answer is `y`.

### 6.3 New bakery-wide setting (Singleton)

Add a `final` field to `BakeryConfig`'s private constructor and a getter. Because all consumers go through `getInstance()`, they immediately see the new value without any code change on their side.

### 6.4 New validation rule (input layer)

Add a new helper method in `Main` (e.g. `readNonEmptyString(prompt)`) and call it wherever a non-empty string is required. The patterns are unaffected — this only touches the input layer.

---

## 7. Conclusion

Laboratory Work #2 extended the Lab 1 codebase with three design patterns, each solving a concrete pain point:

- **Singleton** (`BakeryConfig`) — one consistent, immutable source of bakery-wide settings.
- **Builder** (`CustomerOrder` + `Builder`) — fluent construction of complex orders with only required fields, producing an immutable product.
- **Factory Method** (`PaymentProcessorFactory`) — centralized, type-safe creation of payment processors; adding a new method touches one file.

The **Strategy** pattern from Lab 1 is preserved and now complements the factory: the factory decides *what* to create, the injected abstraction decides *how* `CustomerService` uses it. Together the patterns keep the program open for extension while keeping each class small and testable.

Additionally, the program now accepts **interactive keyboard input** via `java.util.Scanner`, with validation loops (`while`) and exception handling (`try/catch` on `Integer.parseInt`) so that malformed input is rejected gracefully. This does not change the pattern structure — only `main()` and its helper methods — demonstrating that the design remains stable under changes to the entry point.

Comparing the two labs: Lab 1 taught *why* classes should be small and depend on abstractions (SOLID); Lab 2 shows *how* to instantiate and assemble those abstractions cleanly as the program grows (patterns) and how to feed them real, user-supplied data. Neither lab works as well without the other.

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
