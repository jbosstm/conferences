package demo.common.domain;

import org.jboss.stm.annotations.Transactional;

@Transactional
public interface BookingService extends Booking {
    void book();
}
