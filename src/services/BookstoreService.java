package services;

import model.Bookstore;
import java.util.*;

/*
 *This code reads an XML file and extracts information about bookstores.
 * Specifically, it retrieves the name, maxRejectedOrders, and maximum quantity of books for each bookstore,
 * as well as the IDs and prices of all the books sold by each store.
 */

public final class BookstoreService {
    private static BookstoreService instance = null;
    public static BookstoreService getInstance () {
        if (instance == null) {
            instance = new BookstoreService ();}
        return instance;
    }
    private final Map<String, Bookstore> bookstores;
    private BookstoreService () {
        bookstores = new HashMap<> ();
    }
    public void registerBookstore (String id, Bookstore bookstore) {
        bookstores.put (id, bookstore);
    }
    public Bookstore getBookstore (String id) {
        return bookstores.get (id);
    }
}
