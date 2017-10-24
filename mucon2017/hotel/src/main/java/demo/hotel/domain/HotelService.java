package demo.hotel.domain;

import demo.common.domain.BookingService;
import org.jboss.stm.annotations.Transactional;

@Transactional
public interface HotelService extends BookingService {
}
