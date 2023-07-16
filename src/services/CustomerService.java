package services;

import model.Customer;
import java.util.*;

public final class CustomerService {
    private static CustomerService instance = null;
    public static CustomerService getInstance () {
        if (instance == null) {
            instance = new CustomerService ();}
        return instance;}
    private final Map<String, Customer> customers;
    private CustomerService () {
        customers = new HashMap<> ();
    }
    public void registerCustomer (String id, Customer bookstore) {
        customers.put (id, bookstore);}
    public Customer getCustomer (String id) {
        return customers.get (id);
    }}