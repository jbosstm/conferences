package io.narayana;

import com.arjuna.ats.internal.jts.ORBManager;
import com.arjuna.orbportability.OA;
import com.arjuna.orbportability.ORB;
import com.arjuna.orbportability.Services;
import org.omg.CORBA.TRANSACTION_ROLLEDBACK;
import org.omg.CosTransactions.Control;
import org.omg.CosTransactions.Resource;
import org.omg.CosTransactions.TransactionFactory;
import org.omg.CosTransactions.TransactionFactoryHelper;

import java.util.Properties;

public class JTSMain implements AutoCloseable {

    private static String NAME_SERVER_IP = System.getProperty("name.server.ip", "127.0.0.1");
    private static String NAME_SERVER_PORT = System.getProperty("name.server.port", "3528");
    private static String CLIENT_IP = System.getProperty("client.ip", "127.0.0.1");

    private static enum Action {
        COMMIT, ROLLBACK, RESOURCE_ROLLBACK;
    }
    private Action action = Action.valueOf(System.getProperty("action", "COMMIT"));

    private ORB testORB;
    private OA testOA;
    private TransactionFactory transactionFactory;

    public void setup() {
        /**
         * Initialise ORB
         */
        final Properties orbProperties = new Properties();
        orbProperties.setProperty("ORBInitRef.NameService", "corbaloc::" + NAME_SERVER_IP + ":" + NAME_SERVER_PORT
                + "/StandardNS/NameServer-POA/_root");
        orbProperties.setProperty("OAIAddr", CLIENT_IP);

        testORB = ORB.getInstance("test");
        testOA = OA.getRootOA(testORB);

        try {
            testORB.initORB(new String[]{}, orbProperties);
            testOA.initOA();

            ORBManager.setORB(testORB);
            ORBManager.setPOA(testOA);

            /**
             * Initialise transaction factory
             */
            final Services services = new Services(testORB);
            final int resolver = Services.getResolver();
            final String[] serviceParameters = new String[]{Services.otsKind};

            org.omg.CORBA.Object service = services.getService(Services.transactionService, serviceParameters, resolver);
            transactionFactory = TransactionFactoryHelper.narrow(service);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize ORB for " + NAME_SERVER_IP + ":" + NAME_SERVER_PORT
                    + ", clientIP: " + CLIENT_IP, e);
        }
    }

    @Override
    public void close() {
        if (testOA != null) testOA.destroy();
        if (testORB != null) testORB.shutdown();
    }

    public static void main(String[] args) {
        // JTSMain jtsMain = new JTSMain();
        try (JTSMain jtsMain = new JTSMain()) {
            jtsMain.setup();

            switch (jtsMain.action) {
                case COMMIT:
                    jtsMain.commit();
                    return;
                case ROLLBACK:
                    jtsMain.rollback();
                    return;
                case RESOURCE_ROLLBACK:
                    jtsMain.resourceRollback();
                    return;
            }
        }
    }

    public void commit() {
        System.out.println("Mission hide and steal");
        System.out.println("Begin transaction to commit");
        Control control = transactionFactory.create(0);

        final CloakHiding hidingResource = new CloakHiding(true);
        final Lockpicking stealingResource = new Lockpicking(true);

        final Resource hidingReference = hidingResource.getReference();
        final Resource stealingReference = stealingResource.getReference();

        try {
            System.out.println("Register resources to be part of the mission");
            control.get_coordinator().register_resource(hidingReference);
            control.get_coordinator().register_resource(stealingReference);

            System.out.println("Commit transaction");
            control.get_terminator().commit(true);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot enlist and/or commit ORB transaction.", e);
        }
    }

    public void rollback() {
        System.out.println("Mission hide, steal and withdraw");
        System.out.println("Begin transaction to rollback");
        Control control = transactionFactory.create(0);

        final CloakHiding hidingResource = new CloakHiding(true);
        final Lockpicking stealingResource = new Lockpicking(true);

        final Resource hidingReference = hidingResource.getReference();
        final Resource stealingReference = stealingResource.getReference();

        try {
            System.out.println("Register resources to be part of the mission");
            control.get_coordinator().register_resource(hidingReference);
            control.get_coordinator().register_resource(stealingReference);

            System.out.println("Rollback transaction");
            control.get_terminator().rollback();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot enlist and/or rollback ORB transaction.", e);
        }
    }

    public void resourceRollback() {
        System.out.println("Mission hide and fail to steal");
        System.out.println("Begin transaction to fail to steal");
        Control control = transactionFactory.create(0);

        final CloakHiding hidingResource = new CloakHiding(true);
        final Lockpicking stealingResource = new Lockpicking(false);

        final Resource hidingReference = hidingResource.getReference();
        final Resource stealingReference = stealingResource.getReference();

        try {
            System.out.println("Enlist resources");
            control.get_coordinator().register_resource(hidingReference);
            control.get_coordinator().register_resource(stealingReference);

            System.out.println("Commit transaction (but fail as voted to rollback)");
            control.get_terminator().commit(true);
            throw new IllegalStateException("Rollback expected, commit cannot work.");
        } catch (TRANSACTION_ROLLEDBACK e) {
            // Expected
        } catch (Exception e) {
            throw new IllegalStateException("Cannot enlist and/or resource rollback ORB transaction.", e);
        }
    }

}
