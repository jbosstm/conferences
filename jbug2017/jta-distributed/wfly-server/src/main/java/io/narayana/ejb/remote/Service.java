package io.narayana.ejb.remote;

import javax.ejb.Remote;
import javax.transaction.xa.XAException;

@Remote
public interface Service {

    public void exec() throws XAException;
}