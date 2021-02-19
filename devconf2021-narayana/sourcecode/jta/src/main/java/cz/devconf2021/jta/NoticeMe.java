package cz.devconf2021.jta;

import org.jboss.logging.Logger;

import javax.transaction.Status;
import javax.transaction.Synchronization;

public class NoticeMe implements Synchronization {
    private static final Logger LOGGER = Logger.getLogger(NoticeMe.class);

    @Override
    public void beforeCompletion() {
        LOGGER.warn("Before transaction is completed");
    }

    @Override
    public void afterCompletion(int status) {
        LOGGER.warn("After transaction was completed. End status: " + statusToString(status));
    }

    /**
     * Converts transaction status code {@link javax.transaction.Status} to string.
     */
    private static final String statusToString(int status) {
        switch(status) {
            case Status.STATUS_ACTIVE: return "STATUS_ACTIVE";
            case Status.STATUS_MARKED_ROLLBACK: return "STATUS_MARKED_ROLLBACK";
            case Status.STATUS_PREPARED: return "STATUS_PREPARED";
            case Status.STATUS_COMMITTED: return "STATUS_COMMITTED";
            case Status.STATUS_ROLLEDBACK: return "STATUS_ROLLEDBACK";
            case Status.STATUS_UNKNOWN: return "STATUS_UNKNOWN";
            case Status.STATUS_NO_TRANSACTION: return "STATUS_NO_TRANSACTION";
            case Status.STATUS_PREPARING: return "STATUS_PREPARING";
            case Status.STATUS_COMMITTING: return "STATUS_COMMITTING";
            case Status.STATUS_ROLLING_BACK: return "STATUS_ROLLING_BACK";
            default:
                throw new IllegalStateException("Can't determine status code " + status
                        + " as transaction status code defined under " + Status.class.getName());
        }
    }

}
