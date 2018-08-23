package io.narayana.rts.lra.demo.model;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class BookingStores {
    private Map<String, BookingStore> stores = new HashMap<>();

    public BookingStore getStore(String name) {
        if (!stores.containsKey(name)) {
            stores.put(name, new BookingStore(name));
        }

        return stores.get(name);
    }
}
