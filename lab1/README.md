# Bakery Management System

- **Author:** Bulimar Rodion
- **Group:** FAF-242
- **Checked by:** Istrati D., university assistant
- **Faculty:** FCIM, UTM

---

## Table of Contents

- [1. Introduction](#1-introduction)
- [2. Problem Statement](#2-problem-statement)
- [3. Design](#3-design)
  - [3.1 Overview](#31-overview)
  - [3.2 UML Class Diagram](#32-uml-class-diagram)
  - [3.3 Design Rationale](#33-design-rationale)
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

This report presents a small bakery management system written in Java. The purpose of the project is to demonstrate how three of the five **SOLID principles** of object-oriented design can be applied to a realistic, easy-to-understand scenario:

- **S** — Single Responsibility Principle (SRP)
- **O** — Open/Closed Principle (OCP)
- **D** — Dependency Inversion Principle (DIP)

The program simulates a typical bakery day: baking bread, managing inventory, ordering supplies, serving customers with different payment methods, and cleaning the bakery. Each activity is modeled as a separate, focused component so that the design principles are visible directly in the code.

The remaining two principles — Liskov Substitution (LSP) and Interface Segregation (ISP) — are not the focus of this particular example. The project deliberately narrows its scope to three principles in order to keep the demonstration clear and unambiguous.

---

## 2. Problem Statement

A small bakery needs a simple system that can:

1. Perform daily bakery operations (baking, inventory, supply ordering, cleaning).
2. Accept payments from customers via multiple methods (credit card, PayPal, and potentially more in the future).
3. Be easy to extend when new payment methods are introduced, without rewriting existing code.
4. Be easy to test and maintain.

A naive implementation might place all of these responsibilities inside one class, with conditional logic to handle different payment types. That approach quickly becomes unmaintainable. This project demonstrates a cleaner design that respects the SOLID principles.

---

## 3. Design

### 3.1 Overview

The system is composed of **independent, single-purpose components**:

- Four bakery operation classes: `BreadBaker`, `InventoryManager`, `SupplyOrder`, and `BakeryCleaner`.
- One service class: `CustomerService`, which handles customer interaction.
- One abstract payment base class: `PaymentProcessor`, with two concrete implementations: `CreditCardPaymentProcessor` and `PayPalPaymentProcessor`.
- One entry point: `Main`, which wires all components together.

The design deliberately avoids conditional branching for payment handling. Instead, the `CustomerService` depends on the `PaymentProcessor` abstraction and receives a concrete processor via its constructor. New payment methods are added by extending `PaymentProcessor`, without touching `CustomerService` or any existing class.

### 3.2 UML Class Diagram

Figure 1 shows the class structure of the system.

<img width="1283" height="539" alt="image" src="https://github.com/user-attachments/assets/8af118bc-54e1-4bc4-b789-291e5ede2aac" />

**Figure 1:** UML class diagram of the Bakery Management System.

**Figure 1 — Legend:**

| Color | Meaning |
|-------|---------|
| Yellow | Abstract class |
| Blue | Concrete class |
| Pink | Single-responsibility component |
| Cyan | Service using DIP |
| Orange | Entry point |

| Arrow | Meaning |
|-------|---------|
| Solid line, open triangle | Inheritance (`extends`) |
| Dashed line, open "vee" | Uses / depends on |
| Dotted line, open "vee" | Creates / instantiates |

As shown in Figure 1:

- `PaymentProcessor` is an abstract class with two concrete subclasses: `CreditCardPaymentProcessor` and `PayPalPaymentProcessor`.
- `CustomerService` holds a reference to a `PaymentProcessor` (not to any concrete class) and receives the concrete processor through its constructor.
- `BreadBaker`, `InventoryManager`, `SupplyOrder`, and `BakeryCleaner` are standalone classes with no dependencies on each other.
- `Main` acts as the composition root — it creates all objects and wires them together.

### 3.3 Design Rationale

The design decisions below map directly to the three SOLID principles being demonstrated.

#### 3.3.1 Single Responsibility Principle (SRP)

Every class in the system has exactly one reason to change:

| Class | Its single responsibility |
|-------|--------------------------|
| `BreadBaker` | Bakes bread. |
| `InventoryManager` | Manages inventory. |
| `SupplyOrder` | Orders supplies. |
| `BakeryCleaner` | Cleans the bakery. |
| `CustomerService` | Serves customers. |
| `PaymentProcessor` | Defines the payment contract. |
| `CreditCardPaymentProcessor` | Processes credit card payments. |
| `PayPalPaymentProcessor` | Processes PayPal payments. |
| `Main` | Wires components together and runs the demo. |

If, for example, the bakery changes how it bakes bread, only `BreadBaker` needs to be modified. If the credit card processing logic changes, only `CreditCardPaymentProcessor` is affected. No other class is touched.

#### 3.3.2 Open/Closed Principle (OCP)

The system is **open for extension but closed for modification**.

Adding a new payment method — say, cryptocurrency — requires only the creation of a new class:

```java
class CryptoPaymentProcessor extends PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing crypto payment of $" + amount);
    }
}
```

No existing class needs to change:

- `PaymentProcessor` — untouched.
- `CustomerService` — untouched.
- `CreditCardPaymentProcessor` and `PayPalPaymentProcessor` — untouched.
- `Main` — only needs one additional line to *use* the new processor, which is expected (it is the composition root).

This is the essence of the Open/Closed Principle: extending behavior without modifying existing code.

#### 3.3.3 Dependency Inversion Principle (DIP)

The `CustomerService` class illustrates DIP clearly:

```java
class CustomerService {
    private final PaymentProcessor paymentProcessor;

    public CustomerService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    public void serveCustomer(double amount) {
        System.out.println("Serving customer...");
        paymentProcessor.processPayment(amount);
    }
}
```

Key observations:

- The high-level module (`CustomerService`) does **not** import or reference `CreditCardPaymentProcessor` or `PayPalPaymentProcessor`.
- It depends only on the `PaymentProcessor` abstraction.
- The concrete processor is supplied from outside (constructor injection), decided by the caller — in this case, `Main`.
- This makes `CustomerService` easy to test (a mock `PaymentProcessor` can be injected) and easy to extend at runtime.

---

## 4. Implementation

### 4.1 Project Structure

```
.
├── .gitignore
├── README.md
├── docs/
│   ├── bakery-uml.dot                    # Graphviz source for Figure 1
│   └── bakery-uml.png                    # Rendered UML diagram
├── Main.java                             # Entry point / composition root
├── PaymentProcessor.java                 # Abstract base for payment processors
├── CreditCardPaymentProcessor.java       # Concrete credit card processor
├── PayPalPaymentProcessor.java           # Concrete PayPal processor
├── BreadBaker.java                       # Bakes bread
├── InventoryManager.java                 # Manages inventory
├── SupplyOrder.java                      # Orders supplies
├── BakeryCleaner.java                    # Cleans the bakery
└── CustomerService.java                  # Serves customers
```

### 4.2 Source Code

**`PaymentProcessor.java`**

```java
abstract class PaymentProcessor {
    public abstract void processPayment(double amount);
}
```

**`CreditCardPaymentProcessor.java`**

```java
class CreditCardPaymentProcessor extends PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing credit card payment of $" + amount);
    }
}
```

**`PayPalPaymentProcessor.java`**

```java
class PayPalPaymentProcessor extends PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing PayPal payment of $" + amount);
    }
}
```

**`BreadBaker.java`**

```java
class BreadBaker {
    public void bakeBread() {
        System.out.println("Baking high-quality bread...");
    }
}
```

**`InventoryManager.java`**

```java
class InventoryManager {
    public void manageInventory() {
        System.out.println("Managing inventory...");
    }
}
```

**`SupplyOrder.java`**

```java
class SupplyOrder {
    public void orderSupplies() {
        System.out.println("Ordering supplies...");
    }
}
```

**`BakeryCleaner.java`**

```java
class BakeryCleaner {
    public void cleanBakery() {
        System.out.println("Cleaning the bakery...");
    }
}
```

**`CustomerService.java`**

```java
class CustomerService {
    private final PaymentProcessor paymentProcessor;

    public CustomerService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    public void serveCustomer(double amount) {
        System.out.println("Serving customer...");
        paymentProcessor.processPayment(amount);
    }
}
```

**`Main.java`**

```java
public class Main {
    public static void main(String[] args) {
        BreadBaker baker = new BreadBaker();
        InventoryManager inventoryManager = new InventoryManager();
        SupplyOrder supplyOrder = new SupplyOrder();
        BakeryCleaner bakeryCleaner = new BakeryCleaner();

        baker.bakeBread();
        inventoryManager.manageInventory();
        supplyOrder.orderSupplies();

        CustomerService creditCardService =
                new CustomerService(new CreditCardPaymentProcessor());
        creditCardService.serveCustomer(100.00);

        bakeryCleaner.cleanBakery();

        System.out.println("New customer pays with PayPal...");
        CustomerService paypalService =
                new CustomerService(new PayPalPaymentProcessor());
        paypalService.serveCustomer(150.00);
    }
}
```

### 4.3 Getting Started

**Prerequisites:**

- Java Development Kit (JDK) 8 or higher.
- A terminal or any Java IDE.

Verify the installation:

```bash
java -version
javac -version
```

**Compile:**

```bash
javac *.java
```

**Run:**

```bash
java Main
```

---

## 5. Sample Output

```
Baking high-quality bread...
Managing inventory...
Ordering supplies...
Serving customer...
Processing credit card payment of $100.0
Cleaning the bakery...
New customer pays with PayPal...
Serving customer...
Processing PayPal payment of $150.0
```

---

## 6. Extending the Program

To add a new payment method, extend `PaymentProcessor` and use it in `Main`:

```java
class CryptoPaymentProcessor extends PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing crypto payment of $" + amount);
    }
}
```

```java
CustomerService cryptoService = new CustomerService(new CryptoPaymentProcessor());
cryptoService.serveCustomer(200.00);
```

No other class requires modification. This demonstrates the Open/Closed Principle in practice.

---

## 7. Conclusion

The Bakery Management System demonstrates three core SOLID principles:

- **Single Responsibility** — every class has one clearly defined job.
- **Open/Closed** — new payment methods can be added by extension, without modifying existing code.
- **Dependency Inversion** — `CustomerService` depends on the `PaymentProcessor` abstraction, not on concrete implementations, and receives its dependency via constructor injection.

The result is a small but well-structured program that is easy to read, easy to test, and easy to extend. Liskov Substitution and Interface Segregation were deliberately excluded from the scope of this example.

---

## 8. References

1. Martin, R. C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.
2. Martin, R. C. (2002). *Agile Software Development: Principles, Patterns, and Practices*. Prentice Hall.
3. Oracle. *The Java Tutorials — Object-Oriented Programming Concepts*. https://docs.oracle.com/javase/tutorial/java/concepts/
4. Graphviz Documentation. https://graphviz.org/documentation/

---

## 9. License

This project is provided as-is for educational purposes. Feel free to use, modify, and share it.
