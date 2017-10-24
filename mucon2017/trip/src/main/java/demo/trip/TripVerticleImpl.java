package demo.trip;

import com.arjuna.ats.arjuna.AtomicAction;
import demo.domain.Booking;
import demo.domain.ServiceResult;
import demo.domain.TaxiService;
import demo.domain.TheatreService;
import demo.domain.TrainService;
import io.vertx.core.AbstractVerticle;

/**
 * The base class encapsulates the filter and STM specific logic
 */
class TripVerticleImpl extends AbstractVerticle {

    // STM manipulation
    ServiceResult bookTrip(String serviceName,
                           TheatreService theatreService, TaxiService taxiService, TrainService trainService,
                           String showName, String taxiName, String trainName) throws Exception {
        AtomicAction A = new AtomicAction();

        A.begin();

        try {
            // book the theatre seats inside a top level transaction
            boolean commitTrip = theatreService.bookShow(showName);

            // book the transport inside a nested transaction since we want to keep the theatre booking
            AtomicAction B = new AtomicAction();

            B.begin();

            if (!taxiService.bookTaxi(taxiName)) {
                B.abort(); // that fail unwind any transaction state changes made by the bookTaxi call

                // if train is set then attempt to book it in the context of the out transaction A:
                if (trainName == null || !trainService.bookTrain(trainName))
                    commitTrip = false; // cannot get any transport so abort everything
            } else {
                B.commit();
            }

            String msg = String.format("%s=%d, %s=%d, %s=%d",
                    showName, theatreService.getBookings(),
                    taxiName, taxiService.getBookings(),
                    trainName, trainService.getBookings());

            if (commitTrip)
                A.commit();
            else
                A.abort(); // since the taxi or train book were nested they too will be aborted (event though the committed)

            return new ServiceResult(serviceName, Thread.currentThread().getName(),
                    msg,
                    theatreService.getBookings(), taxiService.getBookings(), trainService.getBookings());
        } catch (Exception e) {
            A.abort();
            throw e;
        }
    }

    int getBookings(Booking serviceClone) throws Exception {
        AtomicAction A = new AtomicAction();
        int bookings;

        A.begin();

        try {
            bookings = serviceClone.getBookings();
            A.commit();
        } catch (Exception e) {
            A.abort();
            throw e;
        }

        return bookings;
    }

    int bookShow(TheatreService service, String name) throws Exception {
        AtomicAction A = new AtomicAction();
        int bookings;

        A.begin();

        try {
            service.bookShow(name);
            bookings = service.getBookings();
            A.commit();
        } catch (Exception e) {
            A.abort();
            throw e;
        }

        return bookings;
    }

    int bookTaxi(TaxiService service, String name) throws Exception {
        AtomicAction A = new AtomicAction();
        int bookings;

        A.begin();
        try {
            service.bookTaxi(name);
            bookings = service.getBookings();
            A.commit();
        } catch (Exception e) {
            A.abort();
            throw e;
        }

        return bookings;
    }

    int bookTrain(TrainService service, String name) throws Exception {
        AtomicAction A = new AtomicAction();
        int bookings;

        A.begin();
        try {
            service.bookTrain(name);
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
