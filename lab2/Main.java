import java.util.*;

class BakeryConfig 
{
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
            items.add(item); return this; 
        }
        public Builder discount(double p)   
        { 
            this.discountPercent = p; return this;
        }
        public Builder note(String n)       
        { 
            this.specialNote = n; return this; 
        }
        public Builder withDelivery()       
        { 
            this.delivery = true; return this; 
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

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) 
    {
        BakeryConfig config = BakeryConfig.getInstance();
        System.out.println("Welcome to " + config.getBakeryName() +
                " (currency=" + config.getCurrency() +
                ", tax=" + (config.getTaxRate() * 100) + "%)");
        new BreadBaker().bakeBread();
        new InventoryManager().manageInventory();
        new SupplyOrder().orderSupplies();
        int numberOfOrders = readInt("How many customer orders? ", 1, 100);
        for (int i = 1; i <= numberOfOrders; i++) 
        {
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
        String delivery = readYesNo("Delivery? (y/n): ");
        if (delivery.equalsIgnoreCase("y")) 
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
                    || type.equals("card")
                    || type.equals("paypal")) 
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
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) 
                    return value;
                System.out.println("  Please enter a number between "
                        + min + " and " + max + ".");
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
