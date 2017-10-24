package demo.domain;

public class ServiceResult {
    private String serviceName;
    private String threadId;
    private String message;
    private int theatreBookings;
    private int taxiBookings;
    private int trainBookings;

    public ServiceResult(String serviceName, String threadId, String message, int theatreBookings, int taxiBookings, int trainBookings) {
        this.serviceName = serviceName;
        this.threadId = threadId;
        this.message = message;
        this.theatreBookings = theatreBookings;
        this.taxiBookings = taxiBookings;
        this.trainBookings = trainBookings;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getMessage() {
        return message;
    }

    public int getTheatreBookings() {
        return theatreBookings;
    }

    public int getTaxiBookings() {
        return taxiBookings;
    }

    public int getTrainBookings() {
        return trainBookings;
    }
}
