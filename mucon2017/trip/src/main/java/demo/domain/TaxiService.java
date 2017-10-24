package demo.domain;

import org.jboss.stm.annotations.Nested;
import org.jboss.stm.annotations.Transactional;

@Transactional
@Nested
public interface TaxiService extends Booking {
    boolean bookTaxi(String name);
}
