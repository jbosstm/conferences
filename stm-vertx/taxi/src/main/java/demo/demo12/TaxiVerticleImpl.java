package demo.demo12;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.domain.Booking;
import demo.domain.TaxiService;
import io.vertx.core.AbstractVerticle;

/**
 * The base class encapsulates the filter and STM specific logic
 */
class TaxiVerticleImpl extends AbstractVerticle {
    // STM manipulation
    int getBookings(Booking service) throws Exception {
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

    int makeBooking(TaxiService service) throws Exception {
        AtomicAction A = new AtomicAction();
        int bookings;

        A.begin();
        try {
            service.bookTaxi();
            bookings = service.getBookings();
            A.commit();
        } catch (Exception e) {
            A.abort();
            throw e;
        }

        return bookings;
    }

    // workaround for JBTM-1732
    static void initSTMMemory(Booking service) {
        AtomicAction A = new AtomicAction();

        A.begin();
        service.init();
        A.commit();
    }

}
