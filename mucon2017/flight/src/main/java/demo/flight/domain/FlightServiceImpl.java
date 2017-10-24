package demo.flight.domain;

import org.jboss.stm.annotations.ReadLock;
import org.jboss.stm.annotations.State;
import org.jboss.stm.annotations.WriteLock;

public class FlightServiceImpl implements FlightService {
    @State
    private int noOfBookings = 0;

    public FlightServiceImpl() {
    }

    @Override
    @WriteLock
    public void init() {
    }

    @Override
    @WriteLock
    public void book() {
        noOfBookings += 1;
    }

    @Override
    @ReadLock
    public int getBookings() {
        return noOfBookings;
    }
}