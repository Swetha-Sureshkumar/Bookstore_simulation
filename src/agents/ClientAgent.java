package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.Customer;
import services.CustomerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.Genre;

public class ClientAgent extends Agent {
    private static class RequestBehaviour extends Behaviour {
        private final AID [] sellers;
        private final List<AID> offers;
        private final Customer.Order order;
        private MessageTemplate pattern;
        private String respondWith;
        private long timeOfLastResponse = 0;
        private int step = 0;
        private int refusalCount=0;
        public RequestBehaviour (Agent agent, Customer.Order order, AID [] sellers) {
            super(agent);
            offers = new ArrayList<> ();
            this.order = order;
            this.sellers = sellers;
            }

        @Override
        public void action () {

            ClientAgent clientAgent = (ClientAgent) getAgent ();
            switch (step) {
                case 0 -> {
                    ACLMessage message = new ACLMessage (ACLMessage.CFP);
                    System.out.printf("\n");
                    for (AID seller : sellers) {
                        System.out.printf ("%s [ordering] %d (x%d) from %s\n", getAgent ().getName (), order.book (), order.amount (), seller.getName ());
                        message.addReceiver (seller);}

                    respondWith = String.format ("cfp-%s-%d", clientAgent.customerId, System.currentTimeMillis ());
                    message.setContent (order.serialize ());
                    message.setConversationId ("book_order");
                    message.setReplyWith (respondWith);
                    pattern = MessageTemplate.and (MessageTemplate.MatchConversationId ("book_order"), MessageTemplate.MatchInReplyTo (respondWith));
                    getAgent ().send (message);
                    timeOfLastResponse = System.currentTimeMillis ();
                    step = 1;
                }

                case 1 -> {
                    ACLMessage message = getAgent ().receive (pattern);
                    long now = System.currentTimeMillis ();

                    if (message != null) {
                        timeOfLastResponse = now;
                        String senderName = message.getSender ().getName ();

                        if (message.getPerformative () == ACLMessage.PROPOSE) {
                            System.out.printf ("%s received proposal from %s\n", getAgent ().getName (), senderName);
                            offers.add (message.getSender ());}
                        else if (message.getPerformative () == ACLMessage.REFUSE) {
                            System.out.printf ("%s received refuse from %s\n", getAgent ().getName (), senderName);
                            refusalCount++;

                            if (refusalCount == 3) {
                                System.out.println("[Looks like all the bookstores have gone on vacation. Please come back later!]");}}
                        else {
                            System.out.printf ("client %s did not understand response %s\n", getAgent ().getName (), message.getContent ());
                            ACLMessage reply = message.createReply ();
                            getAgent ().send (reply);}}
                    else {
                        if (now - timeOfLastResponse >= 3000) {
                            step = 2;}
                        else {
                            block (3500);}
                    }
                }
                case 2 -> {
                    if (offers.size () > 0) {
                        System.out.printf ("Displaying potential sellers for:%s\n", getAgent ().getName ());
                        for (AID seller : offers) {
                            System.out.printf (" - %s\n", seller.getName ());}
                        //System.out.printf("\n");

                        int selectedSeller = clientAgent.random.nextInt (offers.size ());
                        AID seller = offers.get (selectedSeller);
                        System.out.printf ("selected seller %s, sending order %s\n", seller.getName (), respondWith);
                        ACLMessage message = new ACLMessage (ACLMessage.REQUEST);

                        message.addReceiver (seller);
                        message.setConversationId ("book_buy");
                        message.setReplyWith (respondWith);
                        message.setContent (order.serialize ());

                        getAgent ().send (message);
                    }
                    step = 3;}}
        }

        @Override
        public boolean done () {
            return (step >= 3);
        }
    };
    private Customer customer = null;
    private Random random;
    private String customerId;

    @Override
    protected void setup () {
        Object [] args = getArguments ();
        customerId = (String) args [0];

        random = new Random ();
        customer = CustomerService.getInstance ().getCustomer (customerId);
        // Customers place their next pre-programmed order every 5 seconds.
        Behaviour orderBehaviour = new TickerBehaviour (this, 5000) {
            private int nextOrder = 0;
            @Override
            protected void onTick () {
                List<Customer.Order> orders = customer.getOrders ();
                if (nextOrder < orders.size ()) {
                    Customer.Order order = orders.get (nextOrder);
                    String genreId = Integer.toString (order.book () / 100);
                    DFAgentDescription pattern = new DFAgentDescription ();
                    ServiceDescription serviceDescription = new ServiceDescription ();
                    serviceDescription.setType ("genre" + genreId);
                    pattern.addServices (serviceDescription);
                    try {
                        DFAgentDescription [] result = DFService.search (getAgent (), pattern);
                        AID [] names = new AID [result.length];
                        for (int i = 0; i < result.length; ++i) {
                            names [i] = result [i].getName ();}
                        addBehaviour (new RequestBehaviour (this.getAgent (), order, names));}
                    catch (FIPAException exception) {
                        System.err.printf ("failed to order %d\n", order.book ());
                        exception.printStackTrace ();
                    }
                    nextOrder++;}
                else {
                    removeBehaviour (this);}
            }
        };
        System.out.printf ("a new client with name %s was created\n", getName ());
        addBehaviour (orderBehaviour);
        super.setup ();
    }
}

