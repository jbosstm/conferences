package demo.domain;

import org.jboss.stm.annotations.ReadLock;
import org.jboss.stm.annotations.State;
import org.jboss.stm.annotations.WriteLock;

public class TaxiServiceImpl implements TaxiService {
    @State
    private int noOfBookings = 0;

    public TaxiServiceImpl() {
    }

    @Override
    @WriteLock
    public void init() {
    }

    @Override
    @WriteLock
    public boolean bookTaxi(String name) {
        noOfBookings += 1;

        return !name.contains("fail");
    }

    @Override
    @ReadLock
    public int getBookings() {
        return noOfBookings;
    }
}
