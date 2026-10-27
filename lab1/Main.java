import java.util.*;

abstract class PaymentProcessor {
    public abstract void processPayment(double amount);
}

class CreditCardPaymentProcessor extends PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing credit card payment of $" + amount);
    }
}

class PayPalPaymentProcessor extends PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing PayPal payment of $" + amount);
    }
}

class BreadBaker {
    public void bakeBread() {
        System.out.println("Baking high-quality bread...");
    }
}

class InventoryManager {
    public void manageInventory() {
        System.out.println("Managing inventory...");
    }
}

class SupplyOrder {
    public void orderSupplies() {
        System.out.println("Ordering supplies...");
    }
}

class BakeryCleaner {
    public void cleanBakery() {
        System.out.println("Cleaning the bakery...");
    }
}

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
