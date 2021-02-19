package cz.devconf2021.stm;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.stm.Container;

@ApplicationScoped
class LockpickingServiceFactory {
    private LockpickingService lockpickingServiceProxy;

    private void initLockpickingServiceFactory() {
        Container<LockpickingService> container = new Container<>();
        lockpickingServiceProxy = container.create(new LockpickingServiceImpl());
    }

    LockpickingService getInstance() {
        if (lockpickingServiceProxy == null) {
            initLockpickingServiceFactory();
        }
        return lockpickingServiceProxy;
    }
}
