package cz.devconf2021.jta;

import org.jboss.logging.Logger;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class Lockpicking implements XAResource {
    private static final Logger LOGGER = Logger.getLogger(NoticeMe.class);

    @Override
    public int prepare(Xid xid) throws XAException {
        LOGGER.info("Lockpicking : prepare");
        return XAResource.XA_OK;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        LOGGER.info("Lockpicking : commit");
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        LOGGER.info("Lockpicking : rollback");
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        // end of work with resource
    }

    @Override
    public void forget(Xid xid) throws XAException {
        // RA could not need to remind the Xid anymore
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        return false;
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return new Xid[0];
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return false;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        // flag from app runtime that it will start to work with the resource
    }
}
