package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import model.Customer;
import model.Bookstore;
import services.BookstoreService;
import model.Genre;
import java.util.HashSet;
import java.util.Set;

public class ManagerAgent extends Agent {
    private final Set<String> acceptedOrders = new HashSet<> ();
    private int ordersCounter = 1;
    private String bookstoreId;
    private Bookstore bookstore;
    private AID sellerId;

    @Override
    protected void setup () {
        Object [] args = getArguments ();
        bookstoreId = (String) args [0];
        bookstore = BookstoreService.getInstance ().getBookstore (bookstoreId);

        addBehaviour (new CyclicBehaviour () {

            private void handleHandshake (ACLMessage message) {
                ACLMessage reply = message.createReply ();

                if (message.getContent ().equals (bookstoreId)) {
                    System.out.printf ("%s handshake on bookstore %s with %s\n", getName (), bookstoreId, message.getSender ().getName ());
                    sellerId = message.getSender ();

                    reply.setPerformative (ACLMessage.AGREE);
                    reply.setContent ("handshake_agree");
                    }
                else {
                    System.out.printf ("%s incorrect handshake bookstore %s with %s\n", getName (), bookstoreId, message.getSender ().getName ());
                    reply.setPerformative (ACLMessage.REFUSE);
                    reply.setContent ("handshake_refuse_id_mismatch");}
                send (reply);
            }



            private void handleOrderCfp (ACLMessage message) {
                ACLMessage reply = message.createReply ();
                Customer.Order order = Customer.Order.deserialize (message.getContent ());
                String orderId = message.getReplyWith ();

                if (order != null && message.getSender ().equals (sellerId)) {
                    ordersCounter++;
                    Float price = bookstore.price (order.book (), order.amount ());
                    if (ordersCounter % bookstore.getMaxRejectedOrders() != 0 && price != null) {
                        acceptedOrders.add (orderId);
                        System.out.printf ("%s AGREED to order %s (%s)\n", getName (), orderId, message.getContent ());
                        reply.setPerformative (ACLMessage.AGREE);
                        reply.setContent ("agree");}
                    else {
                        System.out.printf ("%s refuse order %s (%s)\n", getName (), orderId, message.getContent ());
                        reply.setPerformative (ACLMessage.REFUSE);
                        reply.setContent ("refuse");}}
                else {
                    System.out.printf ("%s didn't understand order %s (%s)\n", getName (), orderId, message.getContent ());
                    reply.setPerformative (ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent ("invalid_format");}
                send (reply);

            }

            private void handleBuy (ACLMessage message) {
                if (message.getSender ().equals (sellerId)) {
                    String orderId = message.getReplyWith ();

                    if (acceptedOrders.contains (orderId)) {
                        acceptedOrders.remove (orderId);
                        System.out.printf ("[ORDER ACCEPTED!] order %s will be delivered by %s!\n", orderId, getName ());}
                    else {
                        System.err.printf ("there was no prior agreement with %s to deliver %s\n", getName (), orderId);}}
                else {
                    System.err.printf ("sender %s is not valid\n", message.getSender ().getName ());}
            }

            @Override
            public void action () {
                ACLMessage message = receive ();

                if (message != null) {
                    if (message.getPerformative () == ACLMessage.PROPOSE) {
                        switch (message.getConversationId ()) {
                            case "handshake" -> {
                                try {
                                    Thread.sleep (1000);}
                                catch (InterruptedException e) {
                                    e.printStackTrace ();}
                                handleHandshake (message);
                            }
                            case "book_query" -> handleOrderCfp (message);
                            //case "book_genre" -> printGenres();
                            case "book_buy" -> handleBuy (message);
                        }
                    }}
                else {
                    block ();}
            }
        });
        super.setup ();
    }
}

