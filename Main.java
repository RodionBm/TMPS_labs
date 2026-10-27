import java.util.*;

class BakeryConfig {
    private static BakeryConfig instance;
    private final String bakeryName;
    private final String currency;
    private final double taxRate;
    
    private BakeryConfig() 
    {
        this.bakeryName = "Sweet Crumb Bakery";
        this.currency = "USD";
        this.taxRate = 0.08;
    }
    
    public static synchronized BakeryConfig getInstance() 
    {
        if (instance == null) instance = new BakeryConfig();
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

abstract class PaymentProcessor 
{
    public abstract void processPayment(double amount);
}

class CreditCardPaymentProcessor extends PaymentProcessor 
{
    @Override public void processPayment(double amount) 
    {
        System.out.println("Processing credit card payment of $" + amount);
    }
}

class PayPalPaymentProcessor extends PaymentProcessor 
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
                return new CreditCardPaymentProcessor();
            case "paypal":
                return new PayPalPaymentProcessor();
            default:
                throw new IllegalArgumentException(
                        "Unsupported payment type: " + type);
        }
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
        this.items           = b.items;
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

    public double calculateTotal() 
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
               "' | delivery=" + delivery + " | total=$" + calculateTotal();
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

    public void serveCustomer(CustomerOrder order) 
    {
        System.out.println("Serving customer " + order);
        paymentProcessor.processPayment(order.calculateTotal());
    }
}

public class Main 
{
    public static void main(String[] args) 
    {
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