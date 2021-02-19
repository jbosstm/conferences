package io.narayana;

import com.arjuna.ats.internal.jts.ORBManager;
import org.omg.CORBA.SystemException;
import org.omg.CosTransactions.Resource;
import org.omg.CosTransactions.ResourcePOA;
import org.omg.CosTransactions.Vote;

public class Lockpicking extends ResourcePOA {

    private final boolean doCommit;
    private final Resource reference;

    public Lockpicking(final boolean doCommit) {
        ORBManager.getPOA().objectIsReady(this);
        this.doCommit = doCommit;
        reference = org.omg.CosTransactions.ResourceHelper.narrow(ORBManager.getPOA().corbaReference(this));
    }

    public Resource getReference() {
        return reference;
    }

    public Vote prepare() throws SystemException {
        System.out.println("Lockpicking : in preparation");

        if (doCommit) {
            System.out.println("\tLockpicking : VoteCommit");
            return Vote.VoteCommit;
        } else {
            System.out.println("\tLockpicking : VoteRollback");
            return Vote.VoteRollback;
        }
    }

    public void rollback() throws SystemException {
        System.out.println("Lockpicking : rollback");
    }

    public void commit() throws SystemException {
        System.out.println("Lockpicking : commit");
    }

    public void forget() throws SystemException {
        System.out.println("Lockpicking : forget");
    }

    public void commit_one_phase() throws SystemException {
        System.out.println("Lockpicking : commit_one_phase");
    }

}
