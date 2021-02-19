package io.narayana;

import com.arjuna.ats.internal.jts.ORBManager;
import org.omg.CORBA.SystemException;
import org.omg.CosTransactions.HeuristicCommit;
import org.omg.CosTransactions.HeuristicHazard;
import org.omg.CosTransactions.HeuristicMixed;
import org.omg.CosTransactions.HeuristicRollback;
import org.omg.CosTransactions.NotPrepared;
import org.omg.CosTransactions.Resource;
import org.omg.CosTransactions.ResourcePOA;
import org.omg.CosTransactions.Vote;

public class CloakHiding extends ResourcePOA {

    private final boolean doCommit;
    private final Resource reference;

    public CloakHiding(final boolean doCommit) {
        ORBManager.getPOA().objectIsReady(this);
        this.doCommit = doCommit;
        reference = org.omg.CosTransactions.ResourceHelper.narrow(ORBManager.getPOA().corbaReference(this));
    }

    public Resource getReference() {
        return reference;
    }

    public org.omg.CosTransactions.Vote prepare() throws SystemException {
        System.out.println("CloakHiding : in preparation");

        if (doCommit) {
            System.out.println("\tCloakHiding : VoteCommit");
            return Vote.VoteCommit;
        } else {
            System.out.println("\tCloakHiding : VoteRollback");
            return Vote.VoteRollback;
        }
    }

    public void rollback() throws SystemException {
        System.out.println("CloakHiding : rollback");
    }

    public void commit() throws SystemException {
        System.out.println("CloakHiding : commit");
    }

    public void forget() throws SystemException {
        System.out.println("CloakHiding : forget");
    }

    public void commit_one_phase() throws SystemException {
        System.out.println("CloakHiding : commit_one_phase");
    }

}
