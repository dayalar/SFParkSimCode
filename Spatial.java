public class Spatial
{
    private static final double METERS_PER_DEGREE_LATITUDE = 111221;
    private static final double METERS_PER_DEGREE_LONGITUDE = 95234.1;

    public static double distance(SFParkNode n1, SFParkNode n2)
    {
        // Return in meters.

        double lat1 = n1.getLatitude();
        double lon1 = n1.getLongitude();
        double lat2 = n2.getLatitude();
        double lon2 = n2.getLongitude();

        double y_dist = METERS_PER_DEGREE_LATITUDE * (lat1 - lat2);
        double x_dist = METERS_PER_DEGREE_LONGITUDE * (lon1 - lon2);
    
        return Math.sqrt((y_dist * y_dist) + (x_dist * x_dist));
    }
}