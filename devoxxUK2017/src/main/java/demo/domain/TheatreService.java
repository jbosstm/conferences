package demo.domain;

import org.jboss.stm.annotations.Transactional;
import org.jboss.stm.annotations.NestedTopLevel;

@Transactional
@NestedTopLevel
public interface TheatreService extends Booking {
    void bookShow();
}
