package model;

import java.util.*;

public class Customer {
    public static record Order(int book, int amount) {
        public String serialize () {
            return String.format ("order:%d:%d", book, amount);}
        public static Order deserialize (String str) {
            String [] parts = str.split (":");
            if (parts.length != 3) {
                return null;}
            if (!parts [0].equals ("order")) {
                return null;}
            int book = Integer.parseInt (parts [1]);
            int amount = Integer.parseInt (parts [2]);
            return new Order (book, amount);}}
    private final List<Order> orders;
    private final String name;
    public String getName () {
        return name;}
    public List<Order> getOrders () {
        return new ArrayList<> (orders);}
    public Customer (String name) {
        this.name = name;
        this.orders = new ArrayList<> ();}
    public void addOrder (int book, int amount) {
        orders.add (new Order (book, amount));}
}

