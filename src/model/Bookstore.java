package model;

import java.util.*;
public class Bookstore {
    public static record Offer (int book, int amount, float price) {
        public String serialize () {
            return String.format ("offer:%d:%d:%f", book, amount, price);}
        public static Offer deserialize (String str) {
            String [] parts = str.split (":");
            if (parts.length != 4) {
                return null;}
            if (!parts [0].equals ("offer")) {
                return null;}
            int book = Integer.parseInt (parts [1]);
            int amount = Integer.parseInt (parts [2]);
            float price = Float.parseFloat (parts [3]);
            return new Offer (book, amount, price);
        }
    }
    private final Map<Integer, Float> booksavailable;
    private final List<Integer> booksAdvertised;
    private final String name;
    private final int maxQuantity;
    private final int maxRejectedOrders;
    public List<Integer> getSelledBooks () {
        return new ArrayList<> (booksavailable.keySet ());}
    public List<Integer> getGenresAdvertised () {
        return new ArrayList<> (booksAdvertised);}
    public Iterator<Integer> getGenreIterator () {
        return booksAdvertised.iterator ();}
    public Iterator<Integer> getBookIterator () {
        return booksavailable.keySet ().iterator ();}
    public boolean sells (int book) {
        return booksavailable.containsKey (book);}
    public int getMaxRejectedOrders () {
        return maxRejectedOrders;}
    public Float price (int book, int quantity) {
        if (quantity > maxQuantity) {
            return null;}

        if (sells (book)) {
            return booksavailable.get (book) * quantity;}
        return null;
    }
    public String getName () {
        return name;
    }

    public Bookstore (String name, int maxQuantity, int maxRejectedOrders) {
        this.name = name;
        this.maxQuantity = maxQuantity;
        this.maxRejectedOrders = maxRejectedOrders;

        booksavailable = new HashMap<> ();
        booksAdvertised = new ArrayList<> ();
    }
    public void addBook (int book, float price) {
        int genreId = book / 100;
        booksAdvertised.add (genreId);
        booksavailable.put (book, price);
    }
}
