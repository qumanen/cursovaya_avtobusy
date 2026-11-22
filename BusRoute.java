package buses;

public class BusRoute {
    private int id;
    private String routeNumber;
    private String busType;
    private String destination;
    private String departureTime;
    private String arrivalTime;

    public BusRoute(int id, String routeNumber, String busType, String destination, String departureTime, String arrivalTime) {
        this.id = id;
        this.routeNumber = routeNumber;
        this.busType = busType;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public int getId() { return id; }
    public String getRouteNumber() { return routeNumber; }
    public String getBusType() { return busType; }
    public String getDestination() { return destination; }
    public String getDepartureTime() { return departureTime; }
    public String getArrivalTime() { return arrivalTime; }
}
