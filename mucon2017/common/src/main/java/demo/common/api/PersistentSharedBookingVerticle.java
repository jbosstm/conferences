package demo.common.api;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Demonstrates how to use persistent STM objects with verticles
 */
public abstract class PersistentSharedBookingVerticle extends BookingVerticle {
    protected static int port = 8080;

    protected PersistentSharedBookingVerticle(String serviceName) {
        super(serviceName);
    }

    protected static String initSharedState(String serviceName, Supplier<String> uidCreator) throws IOException, InterruptedException {
        String sd = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore").getObjectStoreDir();
        File storeDir = new File(sd);
        File file = new File(String.format("%s/%s-uid", sd, serviceName));//"/vertx/data/uid");
        String uid;

        if (!storeDir.exists() && !new File(sd).mkdirs())
            throw new IOException("unable to create shared store%n");

        if (file.createNewFile()) {
            uid = uidCreator.get();

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(uid);
            fileWriter.close();

            port = Integer.getInteger("port", 8080);
            System.out.printf("%s STM created uid: %s%n", serviceName, uid);
        } else {
            while (file.getTotalSpace() == 0) {
                 System.out.println("Waiting for Uid");
                 Thread.sleep(1000);
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            uid = reader.readLine();
            reader.close();

            port = Integer.getInteger("port", 8082);
            System.out.printf("%s STM recovered uid: %s%n", serviceName, uid);
        }

        return uid;
    }
}
