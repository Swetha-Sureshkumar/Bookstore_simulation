package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import model.Customer;
import model.Bookstore;
import model.Genre;
import services.BookstoreService;
import java.util.*;

public class SellerAgent extends Agent {

    private static class MessageHandlerBehaviour extends CyclicBehaviour {
        private final Map<String, AID> activeConversations = new HashMap<> ();
        public MessageHandlerBehaviour (Agent agent) {
            super (agent);
        }
        private void handleCallForProposal (ACLMessage message) {
            String content = message.getContent ();
            Customer.Order order = Customer.Order.deserialize (content);
            ACLMessage reply = message.createReply ();
            System.out.printf ("%s from %s with response %s\n", getAgent ().getName (), message.getSender ().getName (), reply.getInReplyTo ());
            if (order == null) {
                reply.setPerformative (ACLMessage.NOT_UNDERSTOOD);
                reply.setContent ("invalid_format");

                getAgent ().send (reply);}
            else {
                SellerAgent sellerAgent = (SellerAgent) getAgent ();
                if (!sellerAgent.ready) {
                    reply.setPerformative (ACLMessage.REFUSE);
                    reply.setContent ("unavailable");

                    getAgent ().send (reply);}
                else {
                    String conversationId = message.getReplyWith ();
                    activeConversations.put (conversationId, message.getSender ());

                    ACLMessage query = new ACLMessage (ACLMessage.PROPOSE);
                    query.setReplyWith (conversationId);
                    query.setContent (order.serialize ());
                    query.setConversationId ("book_query");
                    query.addReceiver (sellerAgent.managerName);

                    System.out.printf ("%s sending query to its manager about %s\n", getAgent ().getName (), query.getContent ());
                    getAgent ().send (query);}
            }
        }

        private void handleHandshake (ACLMessage message) {
            SellerAgent sellerAgent = (SellerAgent) getAgent();
            if (message.getInReplyTo().equals(sellerAgent.handshakeId) && !sellerAgent.ready) {
                sellerAgent.ready = true;
                System.out.printf("[handshake complete!] between %s and %s!\n", getAgent().getName(), message.getSender().getName());

            }
        }


        private void handleBookQueryResponse (ACLMessage message) {
            SellerAgent sellerAgent = (SellerAgent) getAgent ();
            if (!message.getSender ().equals (sellerAgent.managerName)) {
                System.err.printf ("invalid sender id %s\n", message.getSender ().getName ());
                return;}
            String cfpId = message.getInReplyTo ();
            AID clientId = activeConversations.get (cfpId);
            if (clientId != null) {
                ACLMessage reply = new ACLMessage (message.getPerformative () == ACLMessage.AGREE ? ACLMessage.PROPOSE : ACLMessage.REFUSE);
                if (message.getPerformative () == ACLMessage.AGREE) {
                    System.out.printf ("%s accepted order %s, sending feedback to %s\n", getAgent ().getName (), cfpId, clientId.getName ());}
                else {
                    System.out.printf ("%s rejected order %s, sending feedback to %s\n", getAgent ().getName (), cfpId, clientId.getName ());}
                reply.addReceiver (clientId);
                reply.setContent ("seller_response");
                reply.setConversationId ("book_order");
                reply.setInReplyTo (cfpId);
                activeConversations.remove (cfpId);
                getAgent ().send (reply);}
            else {
                System.err.printf ("invalid cfp id %s\n", cfpId);}
        }
        private void handleBuy (ACLMessage message) {
            SellerAgent sellerAgent = (SellerAgent) getAgent ();
            ACLMessage reply = message.createReply ();
            if (!sellerAgent.ready) {
                reply.setPerformative (ACLMessage.REFUSE);
                reply.setContent ("unavailable");
                getAgent ().send (reply);}
            else {
                String conversationId = message.getReplyWith ();
                activeConversations.put (conversationId, message.getSender ());
                ACLMessage query = new ACLMessage (ACLMessage.PROPOSE);
                query.setReplyWith (conversationId);
                query.setContent (message.getContent ());
                query.setConversationId ("book_buy");
                query.addReceiver (sellerAgent.managerName);
                System.out.printf ("%s passing order purchase %s\n", getAgent ().getName (), query.getContent ());
                getAgent ().send (query);
            }
        }

        @Override
        public void action () {
            ACLMessage message = getAgent ().receive ();
            if (message != null) {
                if (message.getPerformative () == ACLMessage.CFP) {
                    handleCallForProposal (message);}
                else if (message.getConversationId ().equals ("handshake")) {
                    if (message.getPerformative () == ACLMessage.AGREE) {
                        handleHandshake (message);
                    }}
                else if (message.getConversationId ().equals ("book_query")) {
                    handleBookQueryResponse (message);}
                else if (message.getConversationId ().equals ("book_buy")) {
                    handleBuy (message);}
            } else {
                block ();}
        }
    }
    private boolean ready = false;
    private AID managerName;
    private String bookstoreId;
    private String handshakeId;

    @Override
    protected void setup () {
        Object [] args = getArguments ();
        bookstoreId = (String) args [0];
        managerName = new AID ((String) args [1], AID.ISLOCALNAME);
        ready = false;
        Bookstore bookstore = BookstoreService.getInstance ().getBookstore (bookstoreId);
        DFAgentDescription dfDesc = new DFAgentDescription ();
        dfDesc.setName (getAID ());
        System.out.printf ("registering agent %s for bookstore %s in DF\n", getName (), bookstore.getName ());
        for (int genre : bookstore.getGenresAdvertised ()) {
            ServiceDescription sd = new ServiceDescription ();

            sd.setType ("genre" + genre);
            sd.setName ("genre" + genre);

            dfDesc.addServices (sd);}
        try {
            DFService.register (this, dfDesc);
        } catch (FIPAException fipaException) {
            fipaException.printStackTrace ();}
        addBehaviour (new MessageHandlerBehaviour (this));
        addBehaviour (new OneShotBehaviour () {
            @Override
            public void action () {
                ACLMessage message = new ACLMessage (ACLMessage.PROPOSE);
                String replyWith = String.format ("hs-%s-%d", bookstoreId, System.currentTimeMillis ());
                handshakeId = replyWith;
                message.setConversationId ("handshake");
                message.setContent (bookstoreId);
                message.addReceiver (managerName);
                message.setReplyWith (replyWith);
                send (message);
            }
        });
        super.setup ();
    }
    @Override
    protected void takeDown () {
        try {
            DFService.deregister (this);
        } catch (FIPAException fipaException) {
            fipaException.printStackTrace ();
        }
        super.takeDown ();
    }
}
