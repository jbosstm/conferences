package io.narayana.devconf;

import com.arjuna.ats.arjuna.AtomicAction;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.objectstore.ParticipantStore;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.arjuna.objectstore.TxLog;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import org.jboss.stm.Container;

import java.io.IOException;

public class Helper {
    static private String stmDemoUid = "0:ffffc0a80008:8ac3:5a0de48d:2";
    static private String type = "/STMDemos";

    static FlightService getFlightService() {
        String uid = Helper.readSharedUid();
        FlightService flightService;
        Container<FlightService> flightContainer = new Container<>(Container.TYPE.PERSISTENT, Container.MODEL.SHARED);

        if (uid == null) {

            flightService = flightContainer.create(new FlightServiceImpl());

            uid = flightContainer.getIdentifier(flightService).toString();

            Helper.writeSharedUid(uid);

            System.out.printf("Created uid %s%n", uid);
        } else {
            flightService = flightContainer.clone(new FlightServiceImpl(), new Uid(uid));
            System.out.printf("Using uid %s%n", uid);
        }

        return flightService;
    }

    private static void writeSharedUid(String uid) {
        try {
            OutputObjectState oState = new OutputObjectState();
            oState.packString(uid);

            TxLog txLog = StoreManager.getCommunicationStore();
            txLog.write_committed( new Uid(stmDemoUid), type, oState);
        } catch (IOException | ObjectStoreException e) {
            System.out.printf("WARNING: could not write transaction log: %s%n", e.getMessage());
        }
    }

    private static String readSharedUid() {
        ParticipantStore participantStore = StoreManager.getCommunicationStore();
        try {
            InputObjectState iState = participantStore.read_committed(new Uid(stmDemoUid), type);

            if (iState != null)
                return iState.unpackString();
        } catch (ObjectStoreException | IOException ignore) {
        }

        return null;
    }
}
