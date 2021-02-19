package cz.devconf2021.stm;

import org.jboss.stm.annotations.ReadLock;
import org.jboss.stm.annotations.State;
import org.jboss.stm.annotations.WriteLock;

public class LockpickingServiceImpl implements LockpickingService {
    @State
    private int actionNumber;

    @WriteLock
    @Override
    public void doAction() {
        actionNumber += 1;
    }

    @ReadLock
    @Override
    public int getActionNumber() {
        return actionNumber;
    }
}
