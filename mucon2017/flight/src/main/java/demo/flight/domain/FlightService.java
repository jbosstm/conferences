package demo.flight.domain;

import demo.common.domain.BookingService;
import org.jboss.stm.annotations.Transactional;

@Transactional
public interface FlightService extends BookingService {
}
