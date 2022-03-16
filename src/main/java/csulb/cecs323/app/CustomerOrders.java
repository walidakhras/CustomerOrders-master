/*
 * Licensed under the Academic Free License (AFL 3.0).
 *     http://opensource.org/licenses/AFL-3.0
 *
 *  This code is distributed to CSULB students in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, other than educational.
 *
 *  2018 Alvaro Monge <alvaro.monge@csulb.edu>
 *
 */

package csulb.cecs323.app;

// Import all of the entity classes that we have written for this application.

import csulb.cecs323.model.*;
import org.eclipse.persistence.exceptions.DatabaseException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple application to demonstrate how to persist an object in JPA.
 * <p>
 * This is for demonstration and educational purposes only.
 * </p>
 * <p>
 * Originally provided by Dr. Alvaro Monge of CSULB, and subsequently modified by Dave Brown.
 * </p>
 * Licensed under the Academic Free License (AFL 3.0).
 * http://opensource.org/licenses/AFL-3.0
 * <p>
 * This code is distributed to CSULB students in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, other than educational.
 * <p>
 * 2021 David Brown <david.brown@csulb.edu>
 */
//test
public class CustomerOrders {
    /**
     * You will likely need the entityManager in a great many functions throughout your application.
     * Rather than make this a global variable, we will make it an instance variable within the CustomerOrders
     * class, and create an instance of CustomerOrders in the main.
     */
    private EntityManager entityManager;

    /**
     * The Logger can easily be configured to log to a file, rather than, or in addition to, the console.
     * We use it because it is easy to control how much or how little logging gets done without having to
     * go through the application and comment out/uncomment code and run the risk of introducing a bug.
     * Here also, we want to make sure that the one Logger instance is readily available throughout the
     * application, without resorting to creating a global variable.
     */
    private static final Logger LOGGER = Logger.getLogger(CustomerOrders.class.getName());

    /**
     * The constructor for the CustomerOrders class.  All that it does is stash the provided EntityManager
     * for use later in the application.
     *
     * @param manager The EntityManager that we will use.
     */
    public CustomerOrders(EntityManager manager) {
        this.entityManager = manager;
    }

    public static void main(String[] args) {
        LOGGER.setLevel(Level.OFF);
        LOGGER.fine("Creating EntityManagerFactory and EntityManager");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("CustomerOrders");
        EntityManager manager = factory.createEntityManager();
        // Create an instance of CustomerOrders and store our new EntityManager as an instance variable.
        CustomerOrders customerOrders = new CustomerOrders(manager);
        final DecimalFormat df = new DecimalFormat("0.00");


        // Any changes to the database need to be done within a transaction.
        // See: https://en.wikibooks.org/wiki/Java_Persistence/Transactions

        LOGGER.fine("Begin of Transaction");
        EntityTransaction tx = manager.getTransaction();

        tx.begin();
        List<Orders> orders = new ArrayList<Orders>();
        List<Order_lines> order_lines = new ArrayList<Order_lines>();
        // List of Products that I want to persist.  I could just as easily done this with the seed-data.sql
        List<Products> products = new ArrayList<Products>();
        List<Customers> customers = new ArrayList<Customers>();
        // Load up my List with the Entities that I want to persist.  Note, this does not put them
        // into the database.
        products.add(new Products("123", "16 oz. hickory hammer", "Stanely Tools", "1", 9.97, 50));
        products.add(new Products("124", "19 oz. Smooth Face Fiberglass", "Milwaukee", "2", 25.88, 10));
        products.add(new Products("125", "20 oz. Fiberglass Rip Claw Hammer", "Crescent", "3", 19.97, 5));
        products.add(new Products("126", "3 lbs Fiberglass Drilling Hammer", "Milwaukee", "4", 18.97, 10));
        // Create the list of owners in the database.
        customerOrders.createEntity(products);

        customers.add(new Customers("Smith", "Bob", "123 Street", "12345", "012-345-6789"));
        customers.add(new Customers("Akhras", "Walid", "124 Street", "90621", "741-532-1111"));
        customers.add(new Customers("West", "Kanye", "125 Street", "90742", "321-344-6789"));
        customers.add(new Customers("Last", "First", "126 Street", "12345", "012-532-6789"));
        customerOrders.createEntity(customers);

        // Commit the changes so that the new data persists and is visible to other users.
//      tx.commit();
        LOGGER.fine("End of Transaction");

        Scanner in = new Scanner(System.in);

        String identity = customerOrders.getSalesman();

        Customers cust = customerOrders.promptCustomers(customers);
        Orders custOrder = new Orders(cust, customerOrders.getLocalDateTime(), identity);

        boolean continueShopping = true;
        Scanner i = new Scanner(System.in);
        double totalPrice = 0;
        while (continueShopping) {
            Products prod = customerOrders.promptProducts(products);
            System.out.println(prod);

            System.out.println("Please enter the quantity of this product you would like to purchase. ");
            int quantity = i.nextInt();
            while (quantity > prod.getUnits_in_stock()) {
                System.out.println("Quantity of " + quantity + " not available for " + prod.getProd_name());
                System.out.println("Please enter a valid value: ");
                quantity = i.nextInt();
            }

            orders.add(custOrder);
            double price = prod.getUnit_list_price() * quantity;
            System.out.println("Total price: " + price);
            System.out.println("Add product? (Y/N)");
            String res = i.nextLine();
            res = customerOrders.validateResponse(i, res);

            if (res.equals("Y")) {
                order_lines.add(new Order_lines(custOrder, prod, quantity, price));
                System.out.println("Product Added");
                totalPrice += price;
                prod.setUnits_in_stock(prod.getUnits_in_stock() - quantity);
            } else { System.out.println("Order successfully aborted. You may add more products, or exit. "); }

            System.out.println("Add another product to the order? (Y/N)");
            String ans = in.nextLine();
            ans = customerOrders.validateResponse(in, ans);
            if (ans.equals("N")) continueShopping = false;
        } //Finished shopping

        boolean abortOrder = false;
        while (!abortOrder) {
            System.out.println("Are you sure you want to purchase this order? (Y/N)");
            String abortAns = i.nextLine();
            abortAns = customerOrders.validateResponse(in, abortAns);
            if (abortAns.equals("Y")) {
                System.out.println("Total price: " + df.format(totalPrice));
                System.out.println("Purchasing");
                customerOrders.createEntity(orders);
                customerOrders.createEntity(order_lines);
                abortOrder = true;
            } else {
                System.out.println("Aborting order...");
            }
        }

        tx.commit();
        System.out.println("Completed satisfactorily");
    } // End of the main method

    /**
     * This function prompts the user to enter a valid customer ID upon a printed list of customers presented.
     *
     * @param customers list of customers that persists. Holds all Customers inserted into customers list
     * @return A single customer chosen via user input
     */
    public Customers promptCustomers(List<Customers> customers) {
        Scanner in = new Scanner(System.in);
        Customers cust = null;

        while (cust == null) {
            System.out.println("Please enter a customer ID from the list below.");
            printCustomers();
            long ID = in.nextLong();
            // Code citation: I got the code below for easily finding an element in an arraylist
            // From an object attribute from this site:
            // https://www.baeldung.com/find-list-element-java
            cust = customers.stream()
                    .filter(c -> ID == (c.getCustomer_id()))
                    .findAny()
                    .orElse(null);
        }
        return cust;
    }

    /**
     * This function prompts the user to enter a valid UPC number given a list of products
     *
     * @param products The list of products already existing that are stored in the list List
     * @return A single product chosen via user input
     */
    public Products promptProducts(List<Products> products) {
        Scanner in = new Scanner(System.in);
        Products prod = null;

        while(prod == null) {
            System.out.println("Please enter the UPC of the product you would like to purchase.");
            printProducts();
            String targetUPC = in.nextLine();
            prod = products.stream()
                    .filter(p -> targetUPC.equals(p.getUPC()))
                    .findAny()
                    .orElse(null);
        }
        return prod;
    }

    /**
     * Prompts the user to enter then name of salesperson assisting the transaction
     * @return Returns a String containing the name of a salesman
     */
    public String getSalesman() {
        Scanner in = new Scanner(System.in);
        System.out.println("Enter the name of the salesperson processing this sale. ");
        return in.nextLine();
    }

    /**
     * Gets the local date and time.
     *
     * @return The local date and time when ran.
     */    
    public LocalDateTime getLocalDateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyy HH:mm:ss");
        return LocalDateTime.now();
    }
    
    /**
     * Validates user response for Y/N, to ensure that either "Y" or "N"
     * was input.
     *
     * @param in The scanner used to read input in from the user.
     * @param res The string input by the user to be validated as
     *            a response.
     * @return The validated string.
     */
    public String validateResponse(Scanner in, String res) {
        if (res.equals("Y") || res.equals("N")) return res;
        while (!res.equals("Y") && !res.equals("N")) {
            System.out.println("Please enter a valid response!");
            res = in.nextLine().toUpperCase();
        }
        return res;
    }

    /**
     * Displays current customers in database.
     *
     */    
    public void printCustomers() {
        for (Customers c : getAllCustomers()) {
            System.out.println(c);
        }
    }
    
    /**
     * Displays current products in database.
     *
     */
    public void printProducts() {
        for (Products p : getAllProducts()) {
            System.out.println(p);
        }
    }

    /**
     * Create and persist a list of objects to the database.
     *
     * @param entities The list of entities to persist.  These can be any object that has been
     *                 properly annotated in JPA and marked as "persistable."  I specifically
     *                 used a Java generic so that I did not have to write this over and over.
     */
    public <E> void createEntity(List<E> entities) {
        for (E next : entities) {
            LOGGER.info("Persisting: " + next);
            // Use the CustomerOrders entityManager instance variable to get our EntityManager.
            this.entityManager.persist(next);
        }

        // The auto generated ID (if present) is not passed in to the constructor since JPA will
        // generate a value.  So the previous for loop will not show a value for the ID.  But
        // now that the Entity has been persisted, JPA has generated the ID and filled that in.
        for (E next : entities) {
            LOGGER.info("Persisted object after flush (non-null id): " + next);
        }
    } // End of createEntity member method

    /**
     * Think of this as a simple map from a String to an instance of Products that has the
     * same name, as the string that you pass in.  To create a new Cars instance, you need to pass
     * in an instance of Products to satisfy the foreign key constraint, not just a string
     * representing the name of the style.
     * <p>
     * //    * @param UPC The name of the product that you are looking for.
     *
     * @return The Products instance corresponding to that UPC.
     */
   public Products getProduct(String UPC) {
      // Run the native query that we defined in the Products entity to find the right style.
      List<Products> products = this.entityManager.createNamedQuery("ReturnProduct",
              Products.class).setParameter(1, UPC).getResultList();
      if (products.size() == 0) {
         // Invalid style name passed in.
         return null;
      } else {
         // Return the style object that they asked for.
         return products.get(0);
      }
   }// End of the getStyle method
    
    /**
     * A method to create and return a list of all products in our database.
     *
     * @return A List of all products in out database.
     */
    public List<Products> getAllProducts() {
        // Run the native query that we defined in the Products entity to find the right style.
        List<Products> products = this.entityManager.createNamedQuery("ReturnProducts",
                Products.class).getResultList();
        if (products.size() == 0) {
            // Invalid style name passed in.
            return null;
        } else {
            // Return the style object that they asked for.
            return products;
        }
        
    }// End of the getStyle method
    
    /**
     * A method to create and return a list of all customers in our database.
     *
     * @return A List of all customers in our database.
     */
    public List<Customers> getAllCustomers() {
        List<Customers> allCustomers = this.entityManager.createNamedQuery("ReturnCustomer",
                Customers.class).getResultList();
        if (allCustomers.size() == 0) {
            // Invalid style name passed in.
            return null;
        } else {
            // Return the style object that they asked for.
            return allCustomers;
        }
    }
}// End of CustomerOrders class
