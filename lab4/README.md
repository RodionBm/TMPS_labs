# Bakery Management System — Laboratory Work #4

- **Author:** Bulimar Rodion
- **Group:** FAF-242
- **Checked by:** Istrati D., university assistant
- **Faculty:** FCIM, UTM

> **Laboratory Work #4 — Behavioral Design Patterns.**
> This report extends the codebase delivered in *Laboratory Work #4* (Bakery Management System) with three classic **behavioral** design patterns: **State**, **Observer**, and **Command**. The **Decorator**, **Adapter**, and **Facade** patterns from Lab 3 and the **Singleton**, **Builder**, and **Factory Method** patterns from Lab 1 are preserved and continue to be used. All customer data and order actions are read **interactively from the keyboard** using `java.util.Scanner`, with full input validation.

---

## Table of Contents

- [1. Introduction](#1-introduction)
- [2. Problem Statement](#2-problem-statement)
- [3. Design](#3-design)
  - [3.1 Overview](#31-overview)
  - [3.2 UML Class Diagram — Full System](#32-uml-class-diagram--full-system)
  - [3.3 UML Diagram per Pattern](#33-uml-diagram-per-pattern)
  - [3.4 Design Rationale](#34-design-rationale)
  - [3.5 Relationship to Laboratory Work #3](#35-relationship-to-laboratory-work-3)
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

This report presents the fourth iteration of a small bakery management system written in Java. Laboratory Work #1 introduced **creational** patterns and Laboratory Work #3 added **structural** patterns. This iteration focuses on three **behavioral** design patterns, i.e. patterns about *how objects communicate and share responsibility at runtime*:

- **State** — every order now has a lifecycle (`PLACED → BAKING → READY → COMPLETED`, or `CANCELLED`). Each stage is its own class that decides which actions are allowed, so there is no `if/switch` on a status field.
- **Observer** — the kitchen display and the customer's SMS notifier subscribe to an order and are told automatically about every state change. Subscribers can be added and removed at runtime.
- **Command** — the operator's actions (*next step*, *cancel*) are objects that are executed through an invoker which keeps a history, so the last action can be **undone**.

In Lab 3 an order was paid immediately inside `BakeryFacade.serve()`. In Lab 4 the facade instead returns an `OrderTicket` — an order with a lifecycle — and the operator drives that ticket through its stages from a small menu. Payment now happens at the right moment: when a `READY` order is handed over and moves to `COMPLETED`.

The program remains **interactive**: customer data (name, items, discount, note, delivery, extras, payment type) and every order action (next step, cancel, undo, SMS on/off) are read from the keyboard using `java.util.Scanner`. Every input is validated, and actions that are illegal in the current state are rejected with a message instead of crashing the program.

**Pattern count:** 3 new **behavioral** patterns (State, Observer, Command) + 3 inherited **structural** patterns (Decorator, Adapter, Facade) + 3 inherited **creational** patterns (Singleton, Builder, Factory Method) = **9 patterns total**.

---

## 2. Problem Statement

Starting from the Lab 3 program, three new needs emerged:

1. **Orders need a lifecycle with rules.** A real bakery order is not paid the instant it is entered — it is placed, baked, made ready, and then handed over and paid. Some actions are only valid at certain stages: an order can be cancelled while it is placed or baking, but not once it is baked, and a paid order cannot be changed at all. Encoding this with a `String status` and nested `if/switch` blocks in every method would scatter the rules and grow with every new stage. Each stage must **own its own rules**.

2. **Several parties must be informed about order progress.** The kitchen needs to see every change; the customer wants an SMS. The order itself should not know *who* is listening or *how* they are notified, and the customer must be able to turn SMS notifications **on and off at runtime**.

3. **Operator actions must be undoable.** The operator may press "next step" or "cancel" by mistake. Actions must be represented so they can be **recorded and reversed** — but reversal must still respect business rules (an order that has already been paid cannot be un-paid).

The system must continue to:

4. Provide a consistent bakery-wide configuration (name, currency, tax rate) via **Singleton**.
5. Construct complex orders step-by-step via **Builder**.
6. Centralize creation of payment processors via **Factory Method**.
7. Add runtime extras to orders via **Decorator**.
8. Integrate the legacy loyalty SDK via **Adapter**.
9. Hide the bakery subsystem behind a single entry point via **Facade**.

---

## 3. Design

### 3.1 Overview

The system is composed of **independent, single-purpose components**, organized by pattern role:

- **Domain:** `BakeryConfig` (Singleton), `CustomerOrder` + `CustomerOrder.Builder` (Builder), and the order abstraction `OrderComponent` with `BaseOrder` as concrete component.
- **Creational:** `PaymentProcessorFactory` (Factory Method).
- **Structural (Lab 3):** `OrderDecorator` + `GiftWrapDecorator`, `BirthdayCandleDecorator`, `PriorityDecorator` (Decorator); `LegacyLoyaltyAdapter` over `LegacyLoyaltyApi` (Adapter); `BakeryFacade` (Facade).
- **Behavioral — State:** the `OrderState` interface and five concrete states: `PlacedState`, `BakingState`, `ReadyState`, `CompletedState`, `CancelledState`. `OrderTicket` is the **Context**.
- **Behavioral — Observer:** the `OrderObserver` interface with two concrete observers, `KitchenDisplay` and `CustomerNotifier`. `OrderTicket` is the **Subject**.
- **Behavioral — Command:** the `Command` interface, two concrete commands `NextStepCommand` and `CancelCommand`, and the invoker `CommandHistory`. `OrderTicket` is the **Receiver**.
- **Subsystem:** `BreadBaker`, `InventoryManager`, `SupplyOrder`, `BakeryCleaner`, `CustomerService`.
- **Client:** `Main`, which talks to the facade, wires the observers, and issues commands based on keyboard input.

`OrderTicket` is the meeting point of all three behavioral patterns: it is the **Context** of State, the **Subject** of Observer, and the **Receiver** of Command. One operator action therefore travels through all three:

```text
Main -> CommandHistory -> NextStepCommand -> OrderTicket -> current OrderState
     -> OrderTicket.setState(...) -> KitchenDisplay / CustomerNotifier
```

And when a `READY` order moves to `COMPLETED`, the same call continues into the Lab 3 chain: `CustomerService → Decorator chain (total) → LegacyLoyaltyAdapter → real PaymentProcessor`.

### 3.2 UML Diagram per Pattern

Each **behavioral** pattern is shown in isolation below.

#### 3.2.1 State — `OrderState` + concrete states

<img width="980" height="444" alt="image" src="https://github.com/user-attachments/assets/9fe9cb60-482e-4488-842e-56021f9ba16a" />

**Figure 1:** State pattern — `OrderTicket` delegates `next()` and `cancel()` to its current `OrderState`.

The resulting lifecycle:

- `OrderState` is the **State** interface; `OrderTicket` is the **Context** that holds the current state and forwards `next()` / `cancel()` to it.
- Each concrete state decides **what happens** for each action: either it moves the ticket to the next state via `ticket.setState(...)`, or it refuses by throwing `IllegalStateException` with a human-readable reason.
- `ReadyState.next()` triggers payment (`ticket.pay()`) before moving to `COMPLETED` — the side effect belongs to the transition, not to `Main`.
- `canUndo()` is a `default` method returning `true`; only `CompletedState` overrides it with `false`. This lets the Command pattern ask the state whether reversal is allowed.

#### 3.2.2 Observer — `OrderObserver` + concrete observers

<img width="773" height="755" alt="image" src="https://github.com/user-attachments/assets/26bf358f-11bf-4f37-a25a-4d699d99614d" />

**Figure 2:** Observer pattern — `OrderTicket` notifies every subscribed `OrderObserver` on each state change.

- `OrderTicket` is the **Subject**: it keeps a `List<OrderObserver>` and calls `notifyObservers(...)` from a single place, `setState(...)`. Every transition — including those caused by undo — is therefore broadcast automatically.
- `OrderObserver` is the **Observer** interface. `KitchenDisplay` prints the raw event; `CustomerNotifier` sends a (simulated) SMS with the new status name. Both use a *push + pull* style: the event string is pushed, and the observer may pull more data from the ticket (`getId()`, `getCustomerName()`, `getState()`).
- `subscribe` ignores duplicates; `unsubscribe` removes an observer at runtime. Menu option **4** toggles the SMS observer on and off.
- `notifyObservers` iterates over a **copy** of the list, so an observer may safely unsubscribe itself during notification without a `ConcurrentModificationException`.

#### 3.2.3 Command — `Command` + `CommandHistory`

<img width="736" height="709" alt="image" src="https://github.com/user-attachments/assets/8a945688-2cec-496e-9f0b-f20e03723e59" />

**Figure 3:** Command pattern
- `Command` is the **Command** interface with `execute()`, `undo()`, and `getName()`.
- `NextStepCommand` and `CancelCommand` are the **ConcreteCommands**. Before executing, each one remembers the ticket's `previous` state; `undo()` restores it with `ticket.setState(previous)`.
- `OrderTicket` is the **Receiver** — the commands do not contain lifecycle logic, they only call `ticket.next()` / `ticket.cancel()`. The rules stay in the states.
- `CommandHistory` is the **Invoker**: it runs commands and keeps a LIFO stack (`ArrayDeque`) for multi-level undo. A command is pushed **only if `execute()` succeeds**, so a rejected action never ends up in the history. A command is popped **only if `undo()` succeeds**, so a refused undo leaves the history intact.
- `NextStepCommand.undo()` asks `ticket.getState().canUndo()` first, so an order that has already been paid (`COMPLETED`) cannot be rolled back.
- `Main` is the **Client**: it creates the concrete command from the user's menu choice and hands it to the invoker.

#### 3.2.4 Overall interaction — Command → State → Observer (→ Lab 3 chain)

This shows that the patterns *interact* on a single execution path rather than merely coexisting in the same file.

### 3.3 Design Rationale

#### 3.3.1 State — `OrderState` + concrete states

The alternative would be a `String status` field and a `switch` in `next()`, `cancel()`, and `undo()`. Every new stage (e.g. `OUT_FOR_DELIVERY`) would require editing all of those switches.

Key points:

- Each state class holds the rules for **one** stage only (SRP). Adding a stage means adding one class and changing the one state that transitions into it (OCP).
- Illegal actions are refused **by the state itself**, with a specific reason (*"Order is already baked, it cannot be cancelled."*). `Main` only catches `IllegalStateException` and prints the message — it never checks the status.
- The payment side effect lives in `ReadyState.next()`, so it is impossible to complete an order without paying for it.
- States are stateless objects, so creating a new instance on each transition is cheap and safe.

#### 3.3.2 Observer — `OrderObserver` + concrete observers

`OrderTicket` should not depend on the kitchen screen or the SMS gateway. With Observer, it only depends on the `OrderObserver` interface.

Key points:

- Notification happens in exactly one place, `setState(...)`, so no transition can forget to notify — this also covers transitions performed by `undo()`.
- Observers are attached and detached at runtime (`subscribe` / `unsubscribe`), which directly implements the "turn SMS on/off" requirement without any flag inside `OrderTicket`.
- Adding a new listener (e.g. a delivery-driver app) means adding one class; `OrderTicket` is untouched.

#### 3.3.3 Command — `Command` + `CommandHistory`

Representing actions as objects decouples **what the user asked for** (`Main` reading a menu choice) from **how it is carried out** (`OrderTicket` and its states), and makes undo possible because each command remembers enough to reverse itself.

Key points:

- Commands are thin: they store the receiver and the previous state, and delegate. Business rules remain in the State classes, so Command and State do not duplicate each other.
- `CommandHistory.execute()` relies on the exception thrown by the state: if the action is illegal, `history.push(c)` is never reached. The history only contains actions that actually happened.
- `CommandHistory.undo()` uses `peek()` → `undo()` → `pop()`, so if undo is refused (paid order) the command stays on the stack and the history remains consistent.
- Undo is multi-level (LIFO), e.g. *next, cancel → undo, undo* returns the order to `PLACED`.

#### 3.3.4 Inherited patterns — Lab 1 and Lab 3

These are unchanged in structure and re-used:

- **Singleton** (`BakeryConfig`) — read by `CustomerOrder` (tax rate) and `BakeryFacade` (name, currency, tax).
- **Builder** (`CustomerOrder.Builder`) — order construction is unchanged; one getter (`getCustomerName()`) was added so observers can address the customer.
- **Factory Method** (`PaymentProcessorFactory`) — still selects the concrete processor inside the facade.
- **Decorator** — the decorated `OrderComponent` is stored in the `OrderTicket` and its total is charged when the order is completed.
- **Adapter** (`LegacyLoyaltyAdapter`) — still wraps the real processor; loyalty points are awarded at payment time.
- **Facade** (`BakeryFacade`) — still the client's entry point; `serve()` now builds and **returns** an `OrderTicket` instead of paying immediately.

### 3.4 Relationship to Laboratory Work #3

Lab 4 does **not** discard Lab 3 — it builds on it. The table below shows what changed.

| Component | Lab 3 status | Lab 4 status |
|---|---|---|
| `BakeryConfig` | Present (Singleton) | Unchanged |
| `CustomerOrder` + `Builder` | Present (Builder) | Added `getCustomerName()` (used by `OrderTicket` / observers) |
| `PaymentProcessor` + 2 implementations | Present | Unchanged |
| `PaymentProcessorFactory` | Present (Factory Method) | Unchanged |
| `OrderComponent`, `BaseOrder`, `OrderDecorator`, 3 decorators | Present (Decorator) | Unchanged |
| `LegacyLoyaltyApi`, `LegacyLoyaltyAdapter` | Present (Adapter) | Unchanged in role |
| `CustomerService` | Present | Unchanged; now called by `OrderTicket.pay()` |
| `BreadBaker`, `InventoryManager`, `SupplyOrder`, `BakeryCleaner` | Present (SRP) | Unchanged |
| `BakeryFacade` | `serve()` paid immediately and cleaned | `serve()` returns an `OrderTicket`; payment is deferred to the lifecycle |
| `Main` | Read order → `facade.serve()` | Also runs `manageOrder(ticket)`: menu, observers, commands, undo |
| `OrderState`, `PlacedState`, `BakingState`, `ReadyState`, `CompletedState`, `CancelledState` | — | **New** (State) |
| `OrderObserver`, `KitchenDisplay`, `CustomerNotifier` | — | **New** (Observer) |
| `Command`, `NextStepCommand`, `CancelCommand`, `CommandHistory` | — | **New** (Command) |
| `OrderTicket` | — | **New** (State Context + Observer Subject + Command Receiver) |

**Pattern count:** 3 new behavioral patterns in Lab 4 + 3 structural patterns from Lab 3 + 3 creational patterns from Lab 1 = **9 patterns total**.

---

## 4. Implementation

### 4.1 Project Structure

The whole program is intentionally kept in a single `Main.java` file (Java 8+ compatible). The `lab3` folder contains `Main.java` and `README.md`.

### 4.2 Source Code

`Main.java` (complete, Java 8+ compatible, interactive):

```java
import java.util.*;

class BakeryConfig 
{
    private static BakeryConfig instance;
    private final String bakeryName = "Sweet Crumb Bakery";
    private final String currency   = "USD";
    private final double taxRate    = 0.08;

    private BakeryConfig() 
    {

    }

    public static synchronized BakeryConfig getInstance() 
    {
        if (instance == null) 
            instance = new BakeryConfig();
        return instance;
    }

    public String getBakeryName() 
    { 
        return bakeryName; 
    }
    public String getCurrency()   
    { 
        return currency; 
    }
    public double getTaxRate()    
    { 
        return taxRate; 
    }
}

class CustomerOrder 
{
    private final String customerName;
    private final List<String> items;
    private final double discountPercent;
    private final String specialNote;
    private final boolean delivery;

    private CustomerOrder(Builder b) 
    {
        this.customerName    = b.customerName;
        this.items           = Collections.unmodifiableList(new ArrayList<>(b.items));
        this.discountPercent = b.discountPercent;
        this.specialNote     = b.specialNote;
        this.delivery        = b.delivery;
    }

    public static class Builder 
    {
        private final String customerName;
        private List<String> items = new ArrayList<>();
        private double discountPercent = 0.0;
        private String specialNote = "";
        private boolean delivery = false;

        public Builder(String customerName) 
        { 
            this.customerName = customerName; 
        }
        public Builder addItem(String item) 
        { 
            items.add(item); 
            return this; 
        }
        public Builder discount(double p)   
        { 
            this.discountPercent = p; 
            return this; 
        }
        public Builder note(String n)       
        { 
            this.specialNote = n;
            return this; 
        }
        public Builder withDelivery()       
        { 
            this.delivery = true; 
            return this; 
        }
        public CustomerOrder build()        
        { 
            return new CustomerOrder(this); 
        }
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public double computeBaseTotal()
    {
        double subtotal = items.size() * 5.0;
        double afterDiscount = subtotal * (1 - discountPercent / 100.0);
        double tax = afterDiscount * BakeryConfig.getInstance().getTaxRate();
        double total = afterDiscount + tax + (delivery ? 7.5 : 0.0);
        return Math.round(total * 100.0) / 100.0;
    }

    @Override public String toString() 
    {
        return "Order for " + customerName + " | items=" + items +
               " | discount=" + discountPercent + "% | note='" + specialNote +
               "' | delivery=" + delivery;
    }
}

interface OrderComponent 
{
    double getTotal();
    String getDescription();
}

class BaseOrder implements OrderComponent 
{
    private final CustomerOrder order;
    public BaseOrder(CustomerOrder order) 
    { 
        this.order = order;
    }

    @Override public double getTotal()       
    { 
        return order.computeBaseTotal(); 
    }

    @Override public String getDescription() 
    { 
        return order.toString(); 
    }
}

abstract class OrderDecorator implements OrderComponent 
{
    protected final OrderComponent wrapped;
    protected OrderDecorator(OrderComponent wrapped) 
    { 
        this.wrapped = wrapped; 
    }

    @Override public double getTotal()       
    { 
        return wrapped.getTotal(); 
    }

    @Override public String getDescription() 
    { 
        return wrapped.getDescription(); 
    }
}

class GiftWrapDecorator extends OrderDecorator 
{
    private static final double COST = 2.50;
    public GiftWrapDecorator(OrderComponent w) 
    { 
        super(w); 
    }

    @Override public double getTotal() 
    { 
        return wrapped.getTotal() + COST; 
    }
    
    @Override public String getDescription() 
    {
        return wrapped.getDescription() + " [gift wrap +$" + COST + "]";
    }
}

class BirthdayCandleDecorator extends OrderDecorator 
{
    private static final double COST = 1.00;
    public BirthdayCandleDecorator(OrderComponent w) 
    { 
        super(w); 
    }

    @Override public double getTotal() 
    { 
        return wrapped.getTotal() + COST; 
    }

    @Override public String getDescription() 
    {
        return wrapped.getDescription() + " [birthday candle +$" + COST + "]";
    }
}

class PriorityDecorator extends OrderDecorator 
{
    private static final double COST = 5.00;
    public PriorityDecorator(OrderComponent w) 
    { 
        super(w); 
    }

    @Override public double getTotal() 
    { 
        return wrapped.getTotal() + COST; 
    }

    @Override public String getDescription() 
    {
        return wrapped.getDescription() + " [priority +$" + COST + "]";
    }
}

interface PaymentProcessor 
{
    void processPayment(double amount);
}

class CreditCardPaymentProcessor implements PaymentProcessor 
{
    @Override public void processPayment(double amount) 
    {
        System.out.println("Processing credit card payment of $" + amount);
    }
}

class PayPalPaymentProcessor implements PaymentProcessor 
{
    @Override public void processPayment(double amount) 
    {
        System.out.println("Processing PayPal payment of $" + amount);
    }
}

class PaymentProcessorFactory 
{
    public static PaymentProcessor create(String type) 
    {
        switch (type.toLowerCase()) 
        {
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

class LegacyLoyaltyApi 
{
    public long awardPointsCents(long amountInCents, String isoCurrency) 
    {
        if (!"USD".equals(isoCurrency)) 
            return 0L;
        return amountInCents / 100;
    }
}

class LegacyLoyaltyAdapter implements PaymentProcessor 
{
    private final PaymentProcessor target;
    private final LegacyLoyaltyApi legacy;

    public LegacyLoyaltyAdapter(PaymentProcessor target, LegacyLoyaltyApi legacy) 
    {
        this.target = target;
        this.legacy = legacy;
    }

    @Override public void processPayment(double amount) 
    {
        target.processPayment(amount);
        long cents  = Math.round(amount * 100.0);
        long points = legacy.awardPointsCents(cents, "USD");
        System.out.println("  Loyalty points awarded: " + points);
    }
}

class BreadBaker 
{
    public void bakeBread() 
    { 
        System.out.println("Baking high-quality bread..."); 
    }
}

class InventoryManager 
{
    public void manageInventory() 
    { 
        System.out.println("Managing inventory..."); 
    }
}

class SupplyOrder 
{
    public void orderSupplies() 
    { 
        System.out.println("Ordering supplies..."); 
    }
}

class BakeryCleaner 
{
    public void cleanBakery() 
    { 
        System.out.println("Cleaning the bakery..."); 
    }
}

class CustomerService 
{
    private final PaymentProcessor paymentProcessor;
    public CustomerService(PaymentProcessor paymentProcessor) 
    {
        this.paymentProcessor = paymentProcessor;
    }

    public void serveCustomer(OrderComponent order) 
    {
        System.out.println("Serving " + order.getDescription());
        paymentProcessor.processPayment(order.getTotal());
    }
}

// ===================== OBSERVER =====================

interface OrderObserver
{
    void update(OrderTicket ticket, String event);
}

class KitchenDisplay implements OrderObserver
{
    @Override public void update(OrderTicket ticket, String event)
    {
        System.out.println("  [Kitchen] order #" + ticket.getId() + " -> " + event);
    }
}

class CustomerNotifier implements OrderObserver
{
    @Override public void update(OrderTicket ticket, String event)
    {
        System.out.println("  [SMS to " + ticket.getCustomerName() + "] your order is " + ticket.getState().getName());
    }
}

// ===================== STATE =====================

interface OrderState
{
    String getName();
    void next(OrderTicket ticket);
    void cancel(OrderTicket ticket);

    default boolean canUndo()
    {
        return true;
    }
}

class PlacedState implements OrderState
{
    @Override public String getName()
    {
        return "PLACED";
    }

    @Override public void next(OrderTicket ticket)
    {
        ticket.setState(new BakingState());
    }

    @Override public void cancel(OrderTicket ticket)
    {
        ticket.setState(new CancelledState());
    }
}

class BakingState implements OrderState
{
    @Override public String getName()
    {
        return "BAKING";
    }

    @Override public void next(OrderTicket ticket)
    {
        ticket.setState(new ReadyState());
    }

    @Override public void cancel(OrderTicket ticket)
    {
        ticket.setState(new CancelledState());
    }
}

class ReadyState implements OrderState
{
    @Override public String getName()
    {
        return "READY";
    }

    @Override public void next(OrderTicket ticket)
    {
        ticket.pay();
        ticket.setState(new CompletedState());
    }

    @Override public void cancel(OrderTicket ticket)
    {
        throw new IllegalStateException("Order is already baked, it cannot be cancelled.");
    }
}

class CompletedState implements OrderState
{
    @Override public String getName()
    {
        return "COMPLETED";
    }

    @Override public boolean canUndo()
    {
        return false;
    }

    @Override public void next(OrderTicket ticket)
    {
        throw new IllegalStateException("Order is already completed.");
    }

    @Override public void cancel(OrderTicket ticket)
    {
        throw new IllegalStateException("Order is paid, it cannot be cancelled.");
    }
}

class CancelledState implements OrderState
{
    @Override public String getName()
    {
        return "CANCELLED";
    }

    @Override public void next(OrderTicket ticket)
    {
        throw new IllegalStateException("Order was cancelled.");
    }

    @Override public void cancel(OrderTicket ticket)
    {
        throw new IllegalStateException("Order is already cancelled.");
    }
}

// Context of the State pattern, Subject of the Observer pattern, Receiver of the Commands
class OrderTicket
{
    private static int nextId = 1;

    private final int id;
    private final CustomerOrder order;
    private final OrderComponent decorated;
    private final CustomerService service;
    private final List<OrderObserver> observers = new ArrayList<>();
    private OrderState state = new PlacedState();

    public OrderTicket(CustomerOrder order, OrderComponent decorated, CustomerService service)
    {
        this.id        = nextId++;
        this.order     = order;
        this.decorated = decorated;
        this.service   = service;
    }

    public void subscribe(OrderObserver o)
    {
        if (!observers.contains(o))
            observers.add(o);
    }

    public void unsubscribe(OrderObserver o)
    {
        observers.remove(o);
    }

    public boolean isSubscribed(OrderObserver o)
    {
        return observers.contains(o);
    }

    private void notifyObservers(String event)
    {
        for (OrderObserver o : new ArrayList<>(observers))
            o.update(this, event);
    }

    // actions are delegated to the current state
    public void next()
    {
        state.next(this);
    }

    public void cancel()
    {
        state.cancel(this);
    }

    public void setState(OrderState newState)
    {
        this.state = newState;
        notifyObservers("state changed to " + newState.getName());
    }

    public void pay()
    {
        service.serveCustomer(decorated);
    }

    public OrderState getState()
    {
        return state;
    }

    public int getId()
    {
        return id;
    }

    public String getCustomerName()
    {
        return order.getCustomerName();
    }
}

// ===================== COMMAND =====================

interface Command
{
    void execute();
    void undo();
    String getName();
}

class NextStepCommand implements Command
{
    private final OrderTicket ticket;
    private OrderState previous;

    public NextStepCommand(OrderTicket ticket)
    {
        this.ticket = ticket;
    }

    @Override public void execute()
    {
        previous = ticket.getState();
        ticket.next();
    }

    @Override public void undo()
    {
        if (!ticket.getState().canUndo())
            throw new IllegalStateException("Order is already paid, cannot undo.");
        ticket.setState(previous);
    }

    @Override public String getName()
    {
        return "next step";
    }
}

class CancelCommand implements Command
{
    private final OrderTicket ticket;
    private OrderState previous;

    public CancelCommand(OrderTicket ticket)
    {
        this.ticket = ticket;
    }

    @Override public void execute()
    {
        previous = ticket.getState();
        ticket.cancel();
    }

    @Override public void undo()
    {
        ticket.setState(previous);
    }

    @Override public String getName()
    {
        return "cancel";
    }
}

// Invoker: runs commands and keeps the history for undo
class CommandHistory
{
    private final Deque<Command> history = new ArrayDeque<>();

    public void execute(Command c)
    {
        c.execute();          // if the state rejects it, it throws and is not saved
        history.push(c);
    }

    public void undo()
    {
        if (history.isEmpty())
        {
            System.out.println("  Nothing to undo.");
            return;
        }
        Command c = history.peek();
        c.undo();
        history.pop();
        System.out.println("  Undone: " + c.getName());
    }
}

class BakeryFacade
{
    private final BreadBaker baker = new BreadBaker();
    private final InventoryManager inventory = new InventoryManager();
    private final SupplyOrder supply = new SupplyOrder();
    private final BakeryCleaner cleaner = new BakeryCleaner();
    private final LegacyLoyaltyApi legacy = new LegacyLoyaltyApi();

    public void openShop() 
    {
        BakeryConfig cfg = BakeryConfig.getInstance();
        System.out.println("Welcome to " + cfg.getBakeryName()
                + " (currency=" + cfg.getCurrency()
                + ", tax=" + (cfg.getTaxRate() * 100) + "%)");
        baker.bakeBread();
        inventory.manageInventory();
        supply.orderSupplies();
    }

    public OrderTicket serve(CustomerOrder order, String paymentType,
                      boolean giftWrap, boolean candle, boolean priority) 
    {
        OrderComponent decorated = new BaseOrder(order);
        if (giftWrap) 
            decorated = new GiftWrapDecorator(decorated);
        if (candle)   
            decorated = new BirthdayCandleDecorator(decorated);
        if (priority) 
            decorated = new PriorityDecorator(decorated);

        PaymentProcessor real        = PaymentProcessorFactory.create(paymentType);
        PaymentProcessor withLoyalty = new LegacyLoyaltyAdapter(real, legacy);
        // Lab 3: instead of paying right away, the order becomes a ticket with a lifecycle
        return new OrderTicket(order, decorated, new CustomerService(withLoyalty));
    }

    public void closeShop() 
    {
        cleaner.cleanBakery();
        System.out.println("All orders processed. Have a nice day!");
    }
}

public class Main 
{
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) 
    {
        BakeryFacade facade = new BakeryFacade();
        facade.openShop();
        int n = readInt("How many customer orders? ", 1, 100);
        for (int i = 1; i <= n; i++) 
        {
            System.out.println("\n Order #" + i + " ");
            CustomerOrder order = readOrder();
            String payType      = readPaymentType();
            boolean giftWrap    = readYesNo("Gift wrap? (y/n): ").equals("y");
            boolean candle      = readYesNo("Birthday candle? (y/n): ").equals("y");
            boolean priority    = readYesNo("Priority handling? (y/n): ").equals("y");
            OrderTicket ticket  = facade.serve(order, payType, giftWrap, candle, priority);
            manageOrder(ticket);
        }
        facade.closeShop();
        sc.close();
    }

    // Lab 3: the user controls the order with commands, the states decide what is allowed,
    // and the observers are notified about every change
    private static void manageOrder(OrderTicket ticket)
    {
        OrderObserver kitchen  = new KitchenDisplay();
        OrderObserver customer = new CustomerNotifier();
        ticket.subscribe(kitchen);
        ticket.subscribe(customer);
        CommandHistory history = new CommandHistory();

        while (true)
        {
            System.out.println("\nOrder #" + ticket.getId() + " is " + ticket.getState().getName());
            System.out.println("1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done");
            int choice = readInt("Choice: ", 0, 4);
            if (choice == 0)
                break;
            try
            {
                if (choice == 1)
                    history.execute(new NextStepCommand(ticket));
                else if (choice == 2)
                    history.execute(new CancelCommand(ticket));
                else if (choice == 3)
                    history.undo();
                else if (ticket.isSubscribed(customer))
                {
                    ticket.unsubscribe(customer);
                    System.out.println("  SMS notifications turned off.");
                }
                else
                {
                    ticket.subscribe(customer);
                    System.out.println("  SMS notifications turned on.");
                }
            }
            catch (IllegalStateException e)
            {
                System.out.println("  Not allowed: " + e.getMessage());
            }
        }
    }

    private static CustomerOrder readOrder() 
    {
        System.out.print("Customer name: ");
        String name = sc.nextLine().trim();
        while (name.isEmpty()) 
        {
            System.out.print("Name cannot be empty. Customer name: ");
            name = sc.nextLine().trim();
        }
        CustomerOrder.Builder builder = new CustomerOrder.Builder(name);
        int itemCount = readInt("How many items? ", 1, 50);
        for (int j = 1; j <= itemCount; j++) 
        {
            System.out.print("  Item #" + j + ": ");
            String item = sc.nextLine().trim();
            while (item.isEmpty()) 
            {
                System.out.print("  Item cannot be empty. Item #" + j + ": ");
                item = sc.nextLine().trim();
            }
            builder.addItem(item);
        }
        int discount = readInt("Discount percent (0-100): ", 0, 100);
        if (discount > 0) 
            builder.discount(discount);
        System.out.print("Special note (leave empty for none): ");
        String note = sc.nextLine().trim();
        if (!note.isEmpty()) 
            builder.note(note);
        if (readYesNo("Delivery? (y/n): ").equals("y")) 
            builder.withDelivery();
        return builder.build();
    }

    private static String readPaymentType() 
    {
        while (true) 
        {
            System.out.print("Payment type (creditcard / paypal): ");
            String type = sc.nextLine().trim().toLowerCase();
            if (type.equals("creditcard") || type.equals("credit")
                    || type.equals("card")    || type.equals("paypal")) 
            {
                return type;
            }
            System.out.println("  Invalid type. Try 'creditcard' or 'paypal'.");
        }
    }

    private static int readInt(String prompt, int min, int max) 
    {
        while (true) 
            {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try 
            {
                int v = Integer.parseInt(line);
                if (v >= min && v <= max) return v;
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
            } 
            catch (NumberFormatException e) 
            {
                System.out.println("  Not a valid integer. Try again.");
            }
        }
    }

    private static String readYesNo(String prompt) 
    {
        while (true) 
        {
            System.out.print(prompt);
            String line = sc.nextLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("n")) 
                return line;
            System.out.println("  Please enter 'y' or 'n'.");
        }
    }
}
```

### 4.3 Getting Started

Prerequisites: JDK 8+ (JDK 17 recommended).

Compile and run:

```bash
javac --release 8 Main.java
java Main
```

The program is interactive. After launching it, enter the order data, then control each order from the menu:

| Choice | Action | Pattern involved |
|---|---|---|
| `1` | Next step (`PLACED → BAKING → READY → COMPLETED`) | Command → State → Observer |
| `2` | Cancel the order (only while `PLACED` or `BAKING`) | Command → State → Observer |
| `3` | Undo the last action | Command (+ State `canUndo()`) → Observer |
| `4` | Turn SMS notifications on / off | Observer (`subscribe` / `unsubscribe`) |
| `0` | Done with this order, go to the next one | — |

Input is validated: invalid numbers, empty names, or unknown payment types cause the program to re-ask instead of crashing, and actions that are not allowed in the current state are reported as `Not allowed: ...`.

---

## 5. Sample Output

The run below shows: a normal lifecycle with payment, undo of a step, turning SMS off, a rejected cancel on a `READY` order, a refused undo on a paid order, and — for the second customer — a cancel followed by undo.

```text
Welcome to Sweet Crumb Bakery (currency=USD, tax=8.0%)
Baking high-quality bread...
Managing inventory...
Ordering supplies...
How many customer orders? 2

 Order #1
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

Order #1 is PLACED
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 1
  [Kitchen] order #1 -> state changed to BAKING
  [SMS to Alice] your order is BAKING

Order #1 is BAKING
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 3
  [Kitchen] order #1 -> state changed to PLACED
  [SMS to Alice] your order is PLACED
  Undone: next step

Order #1 is PLACED
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 1
  [Kitchen] order #1 -> state changed to BAKING
  [SMS to Alice] your order is BAKING

Order #1 is BAKING
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 4
  SMS notifications turned off.

Order #1 is BAKING
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 1
  [Kitchen] order #1 -> state changed to READY

Order #1 is READY
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 2
  Not allowed: Order is already baked, it cannot be cancelled.

Order #1 is READY
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 1
Serving Order for Alice | items=[Sourdough, Croissant] | discount=10.0% | note='No nuts' | delivery=true [gift wrap +$2.5] [priority +$5.0]
Processing credit card payment of $24.72
  Loyalty points awarded: 24
  [Kitchen] order #1 -> state changed to COMPLETED

Order #1 is COMPLETED
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 3
  Not allowed: Order is already paid, cannot undo.

Order #1 is COMPLETED
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 0

 Order #2
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

Order #2 is PLACED
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 1
  [Kitchen] order #2 -> state changed to BAKING
  [SMS to Bob] your order is BAKING

Order #2 is BAKING
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 2
  [Kitchen] order #2 -> state changed to CANCELLED
  [SMS to Bob] your order is CANCELLED

Order #2 is CANCELLED
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 3
  [Kitchen] order #2 -> state changed to BAKING
  [SMS to Bob] your order is BAKING
  Undone: cancel

Order #2 is BAKING
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 2
  [Kitchen] order #2 -> state changed to CANCELLED
  [SMS to Bob] your order is CANCELLED

Order #2 is CANCELLED
1) Next step  2) Cancel  3) Undo  4) Turn SMS on/off  0) Done
Choice: 0
Cleaning the bakery...
All orders processed. Have a nice day!
```

What the output demonstrates:

- **State:** `cancel` on a `READY` order and `undo` on a `COMPLETED` order are refused by the state with a specific reason; payment happens only on the `READY → COMPLETED` transition.
- **Observer:** both observers react to every transition, including those caused by undo; after choice `4`, only the kitchen is notified.
- **Command:** `undo` reverts the last successful action (`next step`, `cancel`); the rejected cancel was never added to the history.
- **Lab 3 chain:** the paid amount ($24.72) includes the gift-wrap and priority decorators, and the adapter awards 24 loyalty points.

---

## 6. Extending the Program

All three behavioral patterns are open for extension by adding new classes, with at most one small change in an existing class.

### 6.1 New lifecycle stage (State)

For example, an `OUT_FOR_DELIVERY` stage between `READY` and `COMPLETED`:

```java
class OutForDeliveryState implements OrderState
{
    @Override public String getName() { return "OUT_FOR_DELIVERY"; }

    @Override public void next(OrderTicket ticket)
    {
        ticket.pay();
        ticket.setState(new CompletedState());
    }

    @Override public void cancel(OrderTicket ticket)
    {
        throw new IllegalStateException("Order is on its way, it cannot be cancelled.");
    }
}
```

Only `ReadyState.next()` changes, to `ticket.setState(new OutForDeliveryState())`. `OrderTicket`, the commands, the observers, and `Main` are untouched.

### 6.2 New listener (Observer)

```java
class DeliveryDriverApp implements OrderObserver
{
    @Override public void update(OrderTicket ticket, String event)
    {
        if (ticket.getState().getName().equals("READY"))
            System.out.println("  [Driver] pick up order #" + ticket.getId());
    }
}
```

Subscribe it in `Main.manageOrder()` with `ticket.subscribe(new DeliveryDriverApp())`. `OrderTicket` does not change.

### 6.3 New operator action (Command)

Add one class implementing `Command` (for example, a `SkipToReadyCommand` that calls `ticket.next()` twice and remembers the original state for undo), and one menu option in `Main.manageOrder()`. `CommandHistory` supports undo for it automatically.

### 6.4 Inherited extension points

- **New order extra (Decorator):** add one `OrderDecorator` subclass and one line in `BakeryFacade.serve()`.
- **New legacy integration (Adapter):** add a class implementing `PaymentProcessor` that translates to the new SDK.
- **New subsystem operation (Facade):** add the method to the subsystem class and call it from `BakeryFacade`.
- **New payment method (Factory Method):** add one class and one `case` in `PaymentProcessorFactory.create(...)`.
- **New bakery-wide setting (Singleton):** add a final field and a getter to `BakeryConfig`.
- **New order attribute (Builder):** add one field to `CustomerOrder`, one to `Builder`, and one fluent setter.

---

## 7. Conclusion

Laboratory Work #4 extended the Lab 3 codebase with three behavioral design patterns, each solving a concrete problem:

- **State** (`OrderState` and five concrete states) gives every order a lifecycle in which each stage owns its rules. Illegal actions are refused by the state itself, and payment is tied to the `READY → COMPLETED` transition.
- **Observer** (`OrderObserver`, `KitchenDisplay`, `CustomerNotifier`) keeps the kitchen and the customer informed of every change without `OrderTicket` knowing who is listening. Subscriptions can be changed at runtime.
- **Command** (`Command`, `NextStepCommand`, `CancelCommand`, `CommandHistory`) turns operator actions into objects with multi-level undo. Undo respects the business rules: only successful actions are recorded, and paid orders cannot be rolled back.

`OrderTicket` connects the three patterns — it is the State Context, the Observer Subject, and the Command Receiver — so a single operator action travels through all of them, and on completion continues into the structural and creational patterns from the previous labs:

```text
Main -> CommandHistory -> Command -> OrderTicket -> OrderState -> observers
                                                 \-> CustomerService -> Decorator chain -> Adapter -> Factory-created processor
```

The program still accepts interactive keyboard input via `java.util.Scanner`, with validation loops and exception handling, so malformed input and illegal actions are rejected gracefully.

Final count: 3 new behavioral patterns (State, Observer, Command), 3 structural patterns from Lab 3 (Decorator, Adapter, Facade), and 3 creational patterns from Lab 1 (Singleton, Builder, Factory Method), for 9 patterns total.

---

## 8. References

1. Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). Design Patterns: Elements of Reusable Object-Oriented Software. Addison-Wesley.
2. Martin, R. C. (2017). Clean Architecture: A Craftsman's Guide to Software Structure and Design. Prentice Hall.
3. Oracle. The Java Tutorials: Object-Oriented Programming Concepts. https://docs.oracle.com/javase/tutorial/java/concepts/
4. Oracle. Java API: java.util.Scanner. https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html
5. Oracle. Java API: java.util.ArrayDeque. https://docs.oracle.com/javase/8/docs/api/java/util/ArrayDeque.html

---

## 9. License

This project is provided as-is for educational purposes. Feel free to use, modify, and share it.
