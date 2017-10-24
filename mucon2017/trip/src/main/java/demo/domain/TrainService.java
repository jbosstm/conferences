package demo.domain;

import org.jboss.stm.annotations.Nested;
import org.jboss.stm.annotations.Transactional;

@Transactional
@Nested
public interface TrainService extends Booking {
    boolean bookTrain(String name);
}
