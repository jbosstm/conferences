import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.DSYNC;


public class X {
    public static void main(String[] args) throws IOException {
        String uid = shareUid(args[0]);

        System.out.println("uid is: " + uid);
    }

    private static String shareUid(String filename) throws IOException {
        String uid;

        try (FileChannel fileChannel = FileChannel.open(Paths.get(filename), WRITE, CREATE_NEW, DSYNC)) {
            fileChannel.lock(0, Long.MAX_VALUE, false);

            uid = createUid();

            fileChannel.write(ByteBuffer.wrap(uid.getBytes()));
        } catch (FileAlreadyExistsException e) {
            // exists so read the uid from it
            try (FileChannel fileChannel = FileChannel.open(Paths.get(filename), READ)) {
                fileChannel.lock(0, Long.MAX_VALUE, true);
                ByteBuffer buf = ByteBuffer.allocate(1024);

                int sz = fileChannel.read(buf);

                if (sz == 0)
                    throw new IllegalStateException("null uid in store");

                uid = new String(buf.array(), "UTF-8");
            }
        }

        return uid;
    }

    private static String createUid() {
        return "the uid";
    }
}
