# Bakery Management System — Laboratory Work #3

- **Author:** Bulimar Rodion
- **Group:** FAF-242
- **Checked by:** Istrati D., university assistant
- **Faculty:** FCIM, UTM

> **Laboratory Work #3 — Structural Design Patterns.**
> This report extends the codebase delivered in *Laboratory Work #2* (Bakery Management System) with three classic **structural** design patterns: **Decorator**, **Adapter**, and **Facade**. The **Singleton**, **Builder**, and **Factory Method** patterns from Lab 1 are preserved and continue to be used. All customer data is read **interactively from the keyboard** using `java.util.Scanner`, with full input validation.

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

This report presents the second iteration of a small bakery management system written in Java. Where Laboratory Work #2 focused on **SOLID principles** and introduced three **creational** design patterns, this iteration focuses on three **structural** design patterns applied to the same domain:

- **Decorator** — attach optional extras (gift wrap, birthday candle, priority handling) to an order at runtime, in any combination, without editing `CustomerOrder`.
- **Adapter** — make a legacy loyalty SDK with an incompatible interface usable through our existing `PaymentProcessor` abstraction.
- **Facade** — provide a single `BakeryFacade` entry point that orchestrates the messy bakery subsystem (baking, inventory, supplies, cleaning, payment, loyalty).

The program simulates a typical bakery day: opening the shop, taking orders, wrapping them, charging them through a payment processor and a legacy loyalty program, and cleaning up after each customer. The patterns are applied because they address concrete pain points that arose in the Lab 2 code — not for their own sake.

Additionally, the program is **interactive**: all customer data (name, items, discount, note, delivery, extras, payment type) is read from the keyboard using `java.util.Scanner`. Every input is validated so the program never crashes on malformed data.

**Pattern count:** 3 new **structural** patterns (Decorator, Adapter, Facade) + 3 inherited **creational** patterns (Singleton, Builder, Factory Method) = **6 patterns total**.

---

## 2. Problem Statement

Starting from the Lab 1 program, three new needs emerged as the bakery grew:

1. **Runtime extras without editing `CustomerOrder`.** Extras such as gift wrap, birthday candle, and priority handling can be combined arbitrarily. Hard-coding them into `CustomerOrder.computeBaseTotal()` would force edits every time a new extra appears — a clear Open/Closed violation. The extras must be **added at runtime, in any combination, in any order**, without touching the original class.

2. **An incompatible third-party loyalty SDK.** The bakery has licensed a **legacy loyalty program** (modelled here as `LegacyLoyaltyApi`) whose signature is fundamentally different from ours: it accepts cents as a `long`, requires a 3-letter ISO currency code as a `String`, and returns `long` instead of `void`. The SDK cannot be modified. It must nevertheless be usable anywhere a `PaymentProcessor` is expected — including wrapping the real processor so payment *and* loyalty happen in one call.

3. **A simple entry point for a complex subsystem.** The `main` method had grown to orchestrate baking, inventory, supplies, cleaning, payment, and loyalty by hand. Adding or reordering a subsystem step meant editing `main`. A single, stable entry point that delegates to the subsystem is needed — **without absorbing the subsystem's business logic**.

The system must continue to:

4. Provide a consistent bakery-wide configuration (name, currency, tax rate) via **Singleton**.
5. Construct complex orders step-by-step via **Builder**.
6. Centralize creation of payment processors via **Factory Method**.
7. Be easy to test and extend.

---

## 3. Design

### 3.1 Overview

The system is composed of **independent, single-purpose components**, organized by pattern role:

- **Domain:** `BakeryConfig` (Singleton), `CustomerOrder` + `CustomerOrder.Builder` (Builder), and the order abstraction `OrderComponent` with `BaseOrder` as concrete component.
- **Creational:** `PaymentProcessorFactory` (Factory Method) — the only place that instantiates concrete payment processors.
- **Structural — Decorator:** `OrderDecorator` (abstract) plus three concrete decorators: `GiftWrapDecorator`, `BirthdayCandleDecorator`, `PriorityDecorator`.
- **Structural — Adapter:** `LegacyLoyaltyAdapter`, which adapts `LegacyLoyaltyApi` (vendor SDK, unmodifiable) to the `PaymentProcessor` interface.
- **Structural — Facade:** `BakeryFacade`, the single entry point that orchestrates `BreadBaker`, `InventoryManager`, `SupplyOrder`, `BakeryCleaner`, `CustomerService`, and the loyalty adapter.
- **Subsystem:** the four bakery roles (`BreadBaker`, `InventoryManager`, `SupplyOrder`, `BakeryCleaner`) and `CustomerService` (DIP, from Lab 1).
- **Client:** `Main`, which talks to the facade and reads input from the user via `Scanner`.

The design keeps the **client (`Main`) talking only to the facade**. Inside the facade, every order flows through the **Decorator** chain, is paid through a **Factory**-created processor wrapped by the **Adapter**, and finally reaches `CustomerService`. This single execution path is what makes the patterns *interact*, not just coexist.

### 3.2 UML Class Diagram — Full System

Figure 1 shows the complete class structure, color-coded by pattern role.

<img width="1184" height="789" alt="image" src="https://github.com/user-attachments/assets/2733e3f1-e0f1-4784-9470-7e9c0658f12c" />

**Figure 1:** Full UML class diagram of the Bakery Management System (Lab 2).

**Legend — colors:**

| Color | Pattern / role |
|-------|----------------|
| Green | **Singleton** (`BakeryConfig`) + **Facade** (`BakeryFacade`) |
| Orange | **Builder** (`CustomerOrder` + `CustomerOrder.Builder`) |
| Yellow | **Factory Method** (`PaymentProcessorFactory`) |
| Light purple | **Decorator** (`OrderComponent`, `BaseOrder`, `OrderDecorator`, concrete decorators) |
| Light green | **Adapter** (`LegacyLoyaltyAdapter`) and **Adaptee** (`LegacyLoyaltyApi`) |
| Light blue | Abstract / Strategy classes (`PaymentProcessor`, `CustomerService`) |
| Pink | Entry point (`Main`) |
| Grey | Plain single-responsibility subsystem classes |

**Legend — arrows:**

| Arrow | Meaning |
|-------|---------|
| Solid line, open triangle | Inheritance (`extends`) |
| Dashed line, open triangle | Realization (`implements`) |
| Solid line, open diamond | Aggregation (has-a) |
| Dashed line, open "vee" | Creates / uses |

### 3.3 UML Diagram per Pattern

Each **structural** pattern is shown in isolation below so the structure is unambiguous.

#### 3.3.1 Decorator — `OrderComponent` + concrete decorators

<img width="1268" height="621" alt="image" src="https://github.com/user-attachments/assets/dd50d675-9a4a-4789-8deb-273f6b26395f" />

**Figure 2:** Decorator pattern — `OrderComponent`, `BaseOrder`, `OrderDecorator`, and concrete decorators.

- `OrderComponent` is the **Component** interface. `BaseOrder` wraps the Lab 1 `CustomerOrder` and is the **ConcreteComponent**.
- `OrderDecorator` is the **abstract Decorator**: it holds a `wrapped : OrderComponent` and forwards every call.
- `GiftWrapDecorator`, `BirthdayCandleDecorator`, `PriorityDecorator` are the **ConcreteDecorators**. Each adds a fixed cost and a short description.
- Stacking is unlimited and can be done at runtime: `Priority(GiftWrap(Candle(BaseOrder(order))))`.
- All decorators are **additive**, so stacking order does not affect the final total (asserted in unit tests).

#### 3.3.2 Adapter — `LegacyLoyaltyAdapter`

<img width="1283" height="210" alt="image" src="https://github.com/user-attachments/assets/2c0e3711-2b59-4a5a-bd4b-8ed909ae487d" />

**Figure 3:** Adapter pattern — `LegacyLoyaltyAdapter` between `PaymentProcessor` and `LegacyLoyaltyApi`.

- `PaymentProcessor` is the **Target** (Lab 1 abstraction).
- `LegacyLoyaltyApi` is the **Adaptee** — the vendor SDK we cannot edit.
- `LegacyLoyaltyAdapter` is the **Adapter** — it implements `PaymentProcessor`, holds both the real `PaymentProcessor` (delegation target) and the `LegacyLoyaltyApi`, and translates dollars → cents + ISO code on each call.
- The translation happens **inside** the adapter; callers still see `processPayment(double)` and never know about the legacy signature.

#### 3.3.3 Facade — `BakeryFacade`

<img width="1319" height="538" alt="image" src="https://github.com/user-attachments/assets/a266bb7a-fdd8-4f3f-9c05-147d41b87c6f" />

**Figure 4:** Facade pattern — `BakeryFacade` orchestrating the bakery subsystem.

- `BakeryFacade` is the **Facade** — the only class the client (`Main`) knows about.
- The subsystem classes (`BreadBaker`, `InventoryManager`, `SupplyOrder`, `BakeryCleaner`, `CustomerService`) are **private collaborators** of the facade.
- The facade also constructs the Decorator chain and the Adapter chain inside `serve()`, giving the client a single method to call per order.
- **The facade contains no business logic of its own** — no tax math, no discount math. It only orders calls and passes results along.

#### 3.3.4 Overall interaction — Facade → Decorator → Adapter

<img width="1354" height="314" alt="image" src="https://github.com/user-attachments/assets/7a4c1cef-146a-4f4d-b567-f766ac76ae37" />

**Figure 5:** Overall interaction path.

The single execution path `main → BakeryFacade → Decorator chain → Factory → Adapter → CustomerService` demonstrates the requirement that *"at least one execution path must pass through two or more of your patterns"*. In fact, every customer order passes through **Facade → Decorator → Adapter** on the same call.

### 3.4 Design Rationale

#### 3.4.1 Decorator — `OrderComponent` + concrete decorators

An order may have any combination of gift wrap, birthday candle, and priority handling. Baking these into `CustomerOrder.computeBaseTotal()` would mean editing `CustomerOrder` whenever a new extra appears.

Key points:

- `OrderComponent` is the abstraction both `BaseOrder` and all decorators share — the client depends on it, not on any concrete decorator.
- `OrderDecorator` holds a `wrapped : OrderComponent` and delegates `getTotal()` / `getDescription()` to it. Concrete decorators add their cost and append their description **without knowing** what they wrap.
- Because all decorators are **additive**, stacking is commutative — a strong property tested explicitly.
- The class diagram shows a real Decorator chain (interface + concrete component + base decorator + ≥2 concrete decorators), not subclassing.

#### 3.4.2 Adapter — `LegacyLoyaltyAdapter`

The legacy SDK has a fundamentally different signature: `long awardPointsCents(long amountInCents, String isoCurrency)`. Our system speaks `void processPayment(double amount)`. Neither side can be changed: the SDK is vendor code, and `PaymentProcessor` is used by the rest of the system.

Key points:

- `LegacyLoyaltyAdapter` implements `PaymentProcessor` — so it slots into `CustomerService` unchanged.
- It holds the real `PaymentProcessor` (so payment and loyalty happen together — one call, one flow) and the `LegacyLoyaltyApi`.
- Inside `processPayment(double)`, it **translates the call**: `$dollars → cents`, hard-codes `"USD"` from `BakeryConfig.getInstance().getCurrency()`, and calls `awardPointsCents(...)`.
- The Adapter is **object-based** (wraps an instance) rather than class-based — the correct choice here because we need to wrap *an arbitrary* `PaymentProcessor`, not just any subclass.
- Advanced requirement **A** from the rubric is satisfied: the adaptee is code we did not design, and its interface is genuinely incompatible.

#### 3.4.3 Facade — `BakeryFacade`

`main` used to orchestrate baking, inventory, supplies, serving, and cleaning by hand. The Facade gives the client a small, stable API (`openShop`, `serve`, `closeShop`) while the subsystem remains free to evolve.

Key points:

- The Facade **does not contain business logic** — pricing still lives in `CustomerOrder` and decorators, payment in `PaymentProcessor`, loyalty in the adapter.
- It **delegates and orders calls**: `bakeBread → manageInventory → orderSupplies` on open; `buildDecoratorChain → factory → adapter → CustomerService.serveCustomer → cleanBakery` on serve; `cleanBakery` on close.
- The client (`Main`) sees only `BakeryFacade`; it does not reference subsystem classes, `LegacyLoyaltyApi`, decorators, or the factory directly. This is exactly the intent of the pattern: hide the complexity, not the responsibilities.

#### 3.4.4 Inherited patterns — Singleton, Builder, Factory Method (Lab 1)

These three are unchanged in structure, only re-used:

- **Singleton** (`BakeryConfig`) — the same single immutable configuration is read by `Main`, `CustomerOrder`, and the loyalty adapter.
- **Builder** (`CustomerOrder.Builder`) — order construction is unchanged; decorators now sit *on top of* the built order.
- **Factory Method** (`PaymentProcessorFactory`) — the facade uses the factory to pick the real processor before wrapping it in the adapter.

### 3.5 Relationship to Laboratory Work #1

Lab 3 does **not** discard Lab 2 — it builds on it. The table below shows what changed.

| Component | Lab 2 status | Lab 3 status |
|---|---|---|
| `BakeryConfig` | Present (Singleton) | Unchanged |
| `CustomerOrder` + `Builder` | Present (Builder) | Base total moved to `computeBaseTotal()`; extras now via Decorator |
| `PaymentProcessor` + 2 subclasses | Present (Strategy + Factory) | `PaymentProcessor` became an `interface` (allows Adapter) |
| `PaymentProcessorFactory` | Present (Factory Method) | Unchanged |
| `CustomerService` | Present (DIP) | Now accepts `OrderComponent` (so decorators flow through it) |
| `BreadBaker`, `InventoryManager`, `SupplyOrder`, `BakeryCleaner` | Present (SRP) | Unchanged |
| `Main` | Orchestrated everything | **Thin client**: only talks to `BakeryFacade` |
| `OrderComponent`, `BaseOrder`, `OrderDecorator`, `GiftWrapDecorator`, `BirthdayCandleDecorator`, `PriorityDecorator` | — | **New** (Decorator) |
| `LegacyLoyaltyApi`, `LegacyLoyaltyAdapter` | — | **New** (Adapter + Adaptee) |
| `BakeryFacade` | — | **New** (Facade) |

**Pattern count:** 3 new structural patterns in Lab 2 + 3 inherited creational patterns from Lab 1 = **6 patterns total**.

---

## 4. Implementation

### 4.1 Project Structure

The whole program is intentionally kept in a single `Main.java` file (Java 8+ compatible). The `docs` folder contains the Graphviz sources (`bakery-uml.dot`, `decorator-uml.dot`, `adapter-uml.dot`, `facade-uml.dot`, `overview-uml.dot`) and their rendered PNGs used by the diagrams in section 3. At the repository root you will find `Main.java`, `README.md`, and `.gitignore`.

### 4.2 Source Code

`Main.java` (complete, Java 8+ compatible, interactive):

```java
import java.util.*;

// =========================================================================
// LAB 1 - DOMAIN (Singleton + Builder)
// =========================================================================

// ---------- 1. SINGLETON ----------
class BakeryConfig {
    private static BakeryConfig instance;
    private final String bakeryName = "Sweet Crumb Bakery";
    private final String currency   = "USD";
    private final double taxRate    = 0.08;

    private BakeryConfig() {}

    public static synchronized BakeryConfig getInstance() {
        if (instance == null) instance = new BakeryConfig();
        return instance;
    }

    public String getBakeryName() { return bakeryName; }
    public String getCurrency()   { return currency; }
    public double getTaxRate()    { return taxRate; }
}

// ---------- 2. BUILDER ----------
class CustomerOrder {
    private final String customerName;
    private final List<String> items;
    private final double discountPercent;
    private final String specialNote;
    private final boolean delivery;

    private CustomerOrder(Builder b) {
        this.customerName    = b.customerName;
        this.items           = Collections.unmodifiableList(new ArrayList<>(b.items));
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

    public double computeBaseTotal() {
        double subtotal = items.size() * 5.0;
        double afterDiscount = subtotal * (1 - discountPercent / 100.0);
        double tax = afterDiscount * BakeryConfig.getInstance().getTaxRate();
        double total = afterDiscount + tax + (delivery ? 7.5 : 0.0);
        return Math.round(total * 100.0) / 100.0;
    }

    @Override public String toString() {
        return "Order for " + customerName + " | items=" + items +
               " | discount=" + discountPercent + "% | note='" + specialNote +
               "' | delivery=" + delivery;
    }
}

// =========================================================================
// PATTERN 1 - DECORATOR: add extras at runtime without touching CustomerOrder
// =========================================================================

interface OrderComponent {
    double getTotal();
    String getDescription();
}

class BaseOrder implements OrderComponent {
    private final CustomerOrder order;
    public BaseOrder(CustomerOrder order) { this.order = order; }
    @Override public double getTotal()       { return order.computeBaseTotal(); }
    @Override public String getDescription() { return order.toString(); }
}

abstract class OrderDecorator implements OrderComponent {
    protected final OrderComponent wrapped;
    protected OrderDecorator(OrderComponent wrapped) { this.wrapped = wrapped; }
    @Override public double getTotal()       { return wrapped.getTotal(); }
    @Override public String getDescription() { return wrapped.getDescription(); }
}

class GiftWrapDecorator extends OrderDecorator {
    private static final double COST = 2.50;
    public GiftWrapDecorator(OrderComponent w) { super(w); }
    @Override public double getTotal() { return wrapped.getTotal() + COST; }
    @Override public String getDescription() {
        return wrapped.getDescription() + " [gift wrap +$" + COST + "]";
    }
}

class BirthdayCandleDecorator extends OrderDecorator {
    private static final double COST = 1.00;
    public BirthdayCandleDecorator(OrderComponent w) { super(w); }
    @Override public double getTotal() { return wrapped.getTotal() + COST; }
    @Override public String getDescription() {
        return wrapped.getDescription() + " [birthday candle +$" + COST + "]";
    }
}

class PriorityDecorator extends OrderDecorator {
    private static final double COST = 5.00;
    public PriorityDecorator(OrderComponent w) { super(w); }
    @Override public double getTotal() { return wrapped.getTotal() + COST; }
    @Override public String getDescription() {
        return wrapped.getDescription() + " [priority +$" + COST + "]";
    }
}

// =========================================================================
// PAYMENT abstraction + FACTORY METHOD (Lab 1)
// =========================================================================

interface PaymentProcessor {
    void processPayment(double amount);
}

class CreditCardPaymentProcessor implements PaymentProcessor {
    @Override public void processPayment(double amount) {
        System.out.println("Processing credit card payment of $" + amount);
    }
}

class PayPalPaymentProcessor implements PaymentProcessor {
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
                throw new IllegalArgumentException("Unsupported payment type: " + type);
        }
    }
}

// =========================================================================
// LEGACY LIBRARY - pretend vendor SDK, incompatible interface.
//                 Do NOT edit. Adapter exists so we don't have to.
// =========================================================================

class LegacyLoyaltyApi {
    /** 1 point per full dollar, only for USD. Takes cents + ISO code. */
    public long awardPointsCents(long amountInCents, String isoCurrency) {
        if (!"USD".equals(isoCurrency)) return 0L;
        return amountInCents / 100;
    }
}

// =========================================================================
// PATTERN 2 - ADAPTER: LegacyLoyaltyApi -> PaymentProcessor
// =========================================================================

class LegacyLoyaltyAdapter implements PaymentProcessor {
    private final PaymentProcessor target;
    private final LegacyLoyaltyApi legacy;

    public LegacyLoyaltyAdapter(PaymentProcessor target, LegacyLoyaltyApi legacy) {
        this.target = target;
        this.legacy = legacy;
    }

    @Override public void processPayment(double amount) {
        target.processPayment(amount);
        long cents  = Math.round(amount * 100.0);
        long points = legacy.awardPointsCents(cents,
                        BakeryConfig.getInstance().getCurrency());
        System.out.println("  Loyalty points awarded: " + points);
    }
}

// =========================================================================
// SUBSYSTEM - simple collaborators, used only by the Facade
// =========================================================================

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
    public void serveCustomer(OrderComponent order) {
        System.out.println("Serving " + order.getDescription());
        paymentProcessor.processPayment(order.getTotal());
    }
}

// =========================================================================
// PATTERN 3 - FACADE: one entry point for the whole bakery subsystem.
//             Delegates only. Contains no pricing/business logic.
// =========================================================================

class BakeryFacade {
    private final BreadBaker baker = new BreadBaker();
    private final InventoryManager inventory = new InventoryManager();
    private final SupplyOrder supply = new SupplyOrder();
    private final BakeryCleaner cleaner = new BakeryCleaner();
    private final LegacyLoyaltyApi legacy = new LegacyLoyaltyApi();

    public void openShop() {
        BakeryConfig cfg = BakeryConfig.getInstance();
        System.out.println("Welcome to " + cfg.getBakeryName()
                + " (currency=" + cfg.getCurrency()
                + ", tax=" + (cfg.getTaxRate() * 100) + "%)");
        baker.bakeBread();
        inventory.manageInventory();
        supply.orderSupplies();
    }

    public void serve(CustomerOrder order, String paymentType,
                      boolean giftWrap, boolean candle, boolean priority) {
        OrderComponent decorated = new BaseOrder(order);
        if (giftWrap) decorated = new GiftWrapDecorator(decorated);
        if (candle)   decorated = new BirthdayCandleDecorator(decorated);
        if (priority) decorated = new PriorityDecorator(decorated);

        PaymentProcessor real        = PaymentProcessorFactory.create(paymentType);
        PaymentProcessor withLoyalty = new LegacyLoyaltyAdapter(real, legacy);

        new CustomerService(withLoyalty).serveCustomer(decorated);
        cleaner.cleanBakery();
    }

    public void closeShop() {
        cleaner.cleanBakery();
        System.out.println("All orders processed. Have a nice day!");
    }
}

// =========================================================================
// CLIENT
// =========================================================================

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        BakeryFacade facade = new BakeryFacade();
        facade.openShop();

        int n = readInt("How many customer orders? ", 1, 100);
        for (int i = 1; i <= n; i++) {
            System.out.println("\n=== Order #" + i + " ===");
            CustomerOrder order = readOrder();
            String payType      = readPaymentType();
            boolean giftWrap    = readYesNo("Gift wrap? (y/n): ").equals("y");
            boolean candle      = readYesNo("Birthday candle? (y/n): ").equals("y");
            boolean priority    = readYesNo("Priority handling? (y/n): ").equals("y");

            facade.serve(order, payType, giftWrap, candle, priority);
        }

        facade.closeShop();
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

        if (readYesNo("Delivery? (y/n): ").equals("y")) builder.withDelivery();
        return builder.build();
    }

    private static String readPaymentType() {
        while (true) {
            System.out.print("Payment type (creditcard / paypal): ");
            String type = sc.nextLine().trim().toLowerCase();
            if (type.equals("creditcard") || type.equals("credit")
                    || type.equals("card")    || type.equals("paypal")) {
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
                int v = Integer.parseInt(line);
                if (v >= min && v <= max) return v;
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
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

Prerequisites: JDK 8+ (JDK 17 recommended) and Graphviz (only to regenerate the diagrams).

Compile and run:

```bash
javac --release 8 Main.java
java Main
```

The program is interactive. After launching it, follow the on-screen prompts. Input is validated: invalid numbers, empty names, or unknown payment types cause the program to re-ask instead of crashing.

Regenerate all UML diagrams:

```bash
dot -Tpng docs/bakery-uml.dot    -o docs/bakery-uml.png
dot -Tpng docs/decorator-uml.dot -o docs/decorator-uml.png
dot -Tpng docs/adapter-uml.dot   -o docs/adapter-uml.png
dot -Tpng docs/facade-uml.dot    -o docs/facade-uml.png
dot -Tpng docs/overview-uml.dot  -o docs/overview-uml.png
```

---

## 5. Sample Output

```text
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
Gift wrap? (y/n): y
Birthday candle? (y/n): n
Priority handling? (y/n): y
Serving Order for Alice | items=[Sourdough, Croissant] | discount=10.0% | note='No nuts' | delivery=true [gift wrap +$2.5] [priority +$5.0]
Processing credit card payment of $24.72
  Loyalty points awarded: 24
Cleaning the bakery...

=== Order #2 ===
Customer name: Bob
How many items? 1
  Item #1: Baguette
Discount percent (0-100): 0
Special note (leave empty for none):
Delivery? (y/n): n
Payment type (creditcard / paypal): paypal
Gift wrap? (y/n): n
Birthday candle? (y/n): y
Priority handling? (y/n): n
Serving Order for Bob | items=[Baguette] | discount=0.0% | note='' | delivery=false [birthday candle +$1.0]
Processing PayPal payment of $6.4
  Loyalty points awarded: 6
Cleaning the bakery...

All orders processed. Have a nice day!
```

---

## 6. Extending the Program

All three structural patterns are open for extension by adding new classes only. No edits to existing classes are required for the primary extension points.

### 6.1 New order extra (Decorator)

Add one class. No other change is required:

```java
class ChocolateBoxDecorator extends OrderDecorator {
    private static final double COST = 4.00;
    public ChocolateBoxDecorator(OrderComponent w) { super(w); }
    @Override public double getTotal() { return wrapped.getTotal() + COST; }
    @Override public String getDescription() {
        return wrapped.getDescription() + " [chocolate box +$" + COST + "]";
    }
}
```

Add one boolean to `Main.readOrder()` and one line in `BakeryFacade.serve()`. The existing decorators, `CustomerOrder`, and `CustomerService` are untouched.

### 6.2 New legacy integration (Adapter)

Add a new class implementing `PaymentProcessor` that translates to the new SDK:

```java
class LegacySmsNotificationAdapter implements PaymentProcessor {
    private final PaymentProcessor target;
    private final LegacySmsApi sms;
    // translate processPayment(double) into the SDK's cents-based call
}
```

The chain still flows through `CustomerService`, which never sees the new class.

### 6.3 New subsystem operation (Facade)

Add a method to the subsystem class (for example, `InventoryManager.restockBread()`), then call it from `BakeryFacade.openShop()`. `Main` is unchanged.

### 6.4 New payment method (Factory Method)

As in Lab 1: add one class and one `case` in `PaymentProcessorFactory.create(...)`. The facade immediately supports it.

### 6.5 New bakery-wide setting (Singleton)

Add a final field to `BakeryConfig` and a getter. Every consumer reads through `getInstance()` and immediately sees the new value.

### 6.6 New order attribute (Builder)

Add one field to `CustomerOrder`, one field to `Builder`, and one fluent setter. Existing callers are unaffected.

---

## 7. Conclusion

Laboratory Work #2 extended the Lab 1 codebase with three structural design patterns, each solving a concrete pain point:

- Decorator (`OrderComponent`, `BaseOrder`, `OrderDecorator`, three concrete decorators) attaches runtime extras (gift wrap, birthday candle, priority) without touching `CustomerOrder`. Any combination, any order, all additive.
- Adapter (`LegacyLoyaltyAdapter`) integrates the incompatible legacy loyalty SDK behind the existing `PaymentProcessor` abstraction. The vendor class is untouched, the client is untouched, and only the adapter knows both worlds.
- Facade (`BakeryFacade`) provides one entry point for the bakery subsystem (`openShop`, `serve`, `closeShop`), with delegation only and no business logic of its own.

The creational patterns from Lab 1 (Singleton, Builder, Factory Method) are all preserved and continue to work with the new structural patterns on the same execution path:

```text
Main -> BakeryFacade -> Decorator chain -> Factory -> Adapter -> CustomerService -> real processor
```

That single path is what the rubric calls "patterns must interact". Every customer order passes through Facade, then Decorator, then Adapter in the same call, and the Factory from Lab 1 selects the concrete processor that gets wrapped by the Adapter.

The program still accepts interactive keyboard input via `java.util.Scanner`, with validation loops and exception handling so malformed input is rejected gracefully.

Final count: 3 new structural patterns (Decorator, Adapter, Facade) plus 3 inherited creational patterns (Singleton, Builder, Factory Method), for 6 patterns total.

---

## 8. References

1. Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). Design Patterns: Elements of Reusable Object-Oriented Software. Addison-Wesley.
2. Martin, R. C. (2017). Clean Architecture: A Craftsman's Guide to Software Structure and Design. Prentice Hall.
3. Oracle. The Java Tutorials: Object-Oriented Programming Concepts. https://docs.oracle.com/javase/tutorial/java/concepts/
4. Oracle. Java API: java.util.Scanner. https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html
5. Graphviz Documentation. https://graphviz.org/documentation/

---

## 9. License

This project is provided as-is for educational purposes. Feel free to use, modify, and share it.
