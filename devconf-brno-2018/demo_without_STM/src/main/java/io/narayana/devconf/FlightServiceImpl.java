package io.narayana.devconf;

public class FlightServiceImpl implements FlightService {
    private int numberOfBookings;

    @Override
    public int getNumberOfBookings() {
        return numberOfBookings;
    }

    @Override
    public void makeBooking(String details) {
        numberOfBookings += 1;
    }
}
