package demo.common.api;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.common.domain.Booking;
import demo.common.domain.BookingService;
import io.vertx.core.AbstractVerticle;

/**
 * The base class encapsulates the filter and STM specific logic
 */
class BookingVerticleImpl extends AbstractVerticle {
    // STM manipulation
    protected int getBookings(Booking service) throws Exception {
        AtomicAction A = new AtomicAction();
        int bookings;

        A.begin();

        try {
            bookings = service.getBookings();
            A.commit();
        } catch (Exception e) {
            A.abort();
            throw e;
        }

        return bookings;
    }

    protected int makeBooking(BookingService service) throws Exception {
        AtomicAction A = new AtomicAction();
        int bookings;

        A.begin();

        try {
            service.book();
            bookings = service.getBookings();
            A.commit();
        } catch (Exception e) {
            A.abort();
            throw e;
        }

        return bookings;
    }

    // workaround for JBTM-1732
    protected static void initSTMMemory(Booking service) {
        AtomicAction A = new AtomicAction();

        A.begin();
        service.init();
        A.commit();
    }
}
