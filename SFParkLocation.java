public class SFParkLocation
{
    private SFParkEdge edge;
    private int direction;
    private double offset;

    public SFParkLocation(SFParkEdge e, int d, double o)
    {
        edge = e;
        direction = d;
        offset = o;
    }

    public SFParkEdge getEdge()
    {
        return edge;
    }

    public int getDirection()
    {
        return direction;
    }

    public double getOffset()
    {
        return offset;
    }

    public void setEdge(SFParkEdge e)
    {
        edge = e;
    }

    public void setDirection(int d)
    {
        direction = d;
    }

    public void setOffset(double o)
    {
        offset = o;
    }

    public String toString()
    {
        String s = "Location: \n" + edge.toString() + "\tdirection = " + direction + ", offset = " + offset;
        return s;      
    }
}