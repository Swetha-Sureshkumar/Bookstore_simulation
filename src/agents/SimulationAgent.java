package agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import model.Customer;
import model.Bookstore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import services.CustomerService;
import services.BookstoreService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/*
 * Code for initializing the simulation world by reading data from an XML file
 * and creating agents for bookstores and customers.
 * The bookstores are represented by a manager and a seller agent,
 * and the customers are represented by a client agent
 */

public class SimulationAgent extends Agent {
    @Override
    protected void setup () {
        OneShotBehaviour loadSimulationBehaviour = new OneShotBehaviour () {
            private Bookstore parseBookstore (Element node) {
                NodeList bookNodes = node.getElementsByTagName ("book");

                String name = node.getAttributeNode ("name").getTextContent ();
                int maxRejectedOrders = Integer.parseInt (node.getAttributeNode ("maxRejectedOrders").getTextContent ());
                int maxQuantity = Integer.parseInt (node.getAttributeNode ("maxQuantity").getTextContent ());

                Bookstore bookstore = new Bookstore (name, maxQuantity, maxRejectedOrders);

                for (int i = 0; i < bookNodes.getLength (); ++i) {
                    Node bookNode = bookNodes.item (i);

                    int bookId = Integer.parseInt (bookNode.getAttributes ().getNamedItem ("id").getTextContent ());
                    float price = Float.parseFloat (bookNode.getAttributes ().getNamedItem ("price").getTextContent ());
                    bookstore.addBook (bookId, price);}
                return bookstore;
            }
            private Customer parseCustomer (Element node) {
                NodeList orderNodes = node.getElementsByTagName ("order");
                String name = node.getAttributeNode ("name").getTextContent ();
                Customer customer = new Customer (name);
                for (int i = 0; i < orderNodes.getLength (); ++i) {
                    Node orderNode = orderNodes.item (i);
                    int bookId = Integer.parseInt (orderNode.getAttributes ().getNamedItem ("bookId").getTextContent ());
                    int people = Integer.parseInt (orderNode.getAttributes ().getNamedItem ("Quantity").getTextContent ());
                    customer.addOrder (bookId, people);}
                return customer;
            }
            private String generateAgentName (String name) {
                return name.replace (' ', '_');
            }

            @Override
            public void action () {
                System.out.printf ("[%s] will now load the simulation...\n", getName ());
                
                File configFile = new File("simulation.xml");
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance ();
                DocumentBuilder dB = null;

                try {
                    dB = documentBuilderFactory.newDocumentBuilder ();}
                catch (ParserConfigurationException exception) {
                    exception.printStackTrace ();}

                Document doc = null;
                if (dB != null) {
                    try {
                        doc = dB.parse (configFile);}
                    catch (IOException | SAXException exception) {
                        exception.printStackTrace ();}
                }

                if (doc != null) {
                    NodeList bookstores = doc.getElementsByTagName ("bookstore");

                    for (int i = 0; i < bookstores.getLength (); ++i) {
                        Node node = bookstores.item (i);
                        Bookstore bookstore = parseBookstore ((Element) node);

                        String bookstoreId = Integer.toString (i);
                        BookstoreService.getInstance ().registerBookstore (bookstoreId, bookstore);

                        try {
                            String sellerName = generateAgentName (bookstore.getName ());
                            String managerName = sellerName + "_manager";

                            AgentController managerController = getContainerController ().createNewAgent (
                                    managerName, "agents.ManagerAgent", new String [] { bookstoreId }
                            );
                            AgentController sellerController = getContainerController ().createNewAgent (
                                    sellerName, "agents.SellerAgent", new String [] { bookstoreId, managerName }
                            );

                            managerController.start ();
                            sellerController.start ();}
                        catch (StaleProxyException exception) {
                            System.err.printf ("[ERROR]failed to create representative for %s \n", bookstore.getName ());
                            exception.printStackTrace ();}
                    }

                    NodeList customers = doc.getElementsByTagName ("customer");

                    for (int i = 0; i < customers.getLength (); ++i) {
                        Node node = customers.item (i);
                        Customer customer = parseCustomer ((Element) node);

                        String customerId = Integer.toString (i);
                        CustomerService.getInstance ().registerCustomer (customerId, customer);

                        try {
                            AgentController controller = getContainerController ().createNewAgent (
                                    generateAgentName (customer.getName ()),
                                    "agents.ClientAgent", new String [] { customerId }
                            );
                            controller.start ();}
                        catch (StaleProxyException exception) {
                            System.err.printf ("[ERROR]failed to create agent for %s\n", customer.getName ());
                            exception.printStackTrace ();}
                    }
                }
            }
        };
        addBehaviour (loadSimulationBehaviour);
        super.setup ();
    }
}

