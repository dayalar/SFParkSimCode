public class SFParkNode
{
    private int id;
    private double latitude;
    private double longitude;

    public SFParkNode(int i, double lat, double lon)
    {
        id = i;
        latitude = lat;
        longitude = lon;
    }

    public int getId()
    {
        return id;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public String toString()
    {
        String s = "nodeId = " + id + ", nodeLatitude = " + latitude + ", nodeLongitude = " + longitude;
        return s;
    }
}
