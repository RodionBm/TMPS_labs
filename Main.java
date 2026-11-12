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

    public void serve(CustomerOrder order, String paymentType,
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
        new CustomerService(withLoyalty).serveCustomer(decorated);
        cleaner.cleanBakery();
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
            facade.serve(order, payType, giftWrap, candle, priority);
        }
        facade.closeShop();
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