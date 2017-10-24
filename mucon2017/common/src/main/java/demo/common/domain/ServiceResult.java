package demo.common.domain;

public class ServiceResult {
    private String serviceName;
    private String threadId;
    private String hostId;
    private String message;
    private int bookingCount;
    private int altBookingCount;

    public ServiceResult(String serviceName, String threadId, String hostId, String message, int bookingCount, int altBookingCount) {
        this.serviceName = serviceName;
        this.threadId = threadId;
        this.hostId = hostId;
        this.message = message;
        this.bookingCount = bookingCount;
        this.altBookingCount = altBookingCount;
    }

    public ServiceResult(String serviceName, String threadId, String hostId, int bookingCount) {
        this(serviceName, threadId, hostId, "", bookingCount, 0);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getHostId() {
        return hostId;
    }

    public String getMessage() {
        return message;
    }

    public int getBookingCount() {
        return bookingCount;
    }

    public int getAltBookingCount() {
        return altBookingCount;
    }
}
