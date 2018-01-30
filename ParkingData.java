import java.io.BufferedReader;
import java.io.FileReader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.sql.Timestamp;


public class ParkingData
{
    private static String fileName;

    private ArrayList<Integer> blockList = new ArrayList<Integer>();
    private ArrayList<Timestamp> timeList = new ArrayList<Timestamp>();
    private ArrayList<Integer> availabilityList = new ArrayList<Integer>();

    private HashMap<Integer,Integer> parkingMap;
    private HashMap<Integer,Integer> initialParkingMap;
    private int lastReportProcessed;
    private int initialLastReportProcessed;

    BufferedReader readFile;

    public ParkingData(String fN)
    {
        fileName = fN;
        try
	{
	    readFile = new BufferedReader(new FileReader(fileName));
        }
        catch (Exception e)
	{
            e.printStackTrace();
        }
        readDataIntoMemory();

        //initiateTheParkingMap(new Timestamp(112,3,6,17,45,24,0));
        //System.out.println(getAvailability(847032));
        //advanceToTime(new Timestamp(112,3,6,19,50,0,0));
        //System.out.println(getAvailability(847032));
        //advanceToTime(new Timestamp(112,3,6,23,30,0,0));
        //System.out.println(getAvailability(847032));
        //restartTheParkingMap();
        //System.out.println(getAvailability(847032));
        //advanceToTime(new Timestamp(112,3,6,19,50,0,0));
        //System.out.println(getAvailability(847032));
        //advanceToTime(new Timestamp(112,3,6,23,30,0,0));
        //System.out.println(getAvailability(847032));
    }

    public int getAvailability(int b)
    {
        // Will return the availability for the given block at the time to which the profile has been updated.
        if ( parkingMap.containsKey(b) )
        {
            return parkingMap.get(b).intValue();
        }
        else
        {
            return 0;
        }
    }

    public void advanceToTime(Timestamp t)
    {
        // This function assumes that the function will be called with progressively larger timestamps...

        int i = lastReportProcessed+1;
        int blockId = ((Integer)blockList.get(i)).intValue();
        Timestamp time = (Timestamp)timeList.get(i);
        int availability = ((Integer)availabilityList.get(i)).intValue();
        
        while ( time.before(t) )
	{
            parkingMap.put(blockId,availability);

            i = i+1;

            if ( i >= blockList.size() )
	    {
                break;
            }

            blockId = ((Integer)blockList.get(i)).intValue();
            time = (Timestamp)timeList.get(i);
            availability = ((Integer)availabilityList.get(i)).intValue();
        }

        lastReportProcessed = i-1;
    }

    public void initiateTheParkingMap(Timestamp t)
    {
        // It will scan the data and give the availability according to the last report for each block.

        parkingMap = new HashMap<Integer,Integer>();
        initialParkingMap = new HashMap<Integer,Integer>();

        int i = 0;        
        int blockId = ((Integer)blockList.get(i)).intValue();
        Timestamp time = (Timestamp)timeList.get(i);
        int availability = ((Integer)availabilityList.get(i)).intValue();
        
        while ( time.before(t) )
	{
            parkingMap.put(blockId,availability);
            initialParkingMap.put(blockId,availability);

            i = i+1;

            if ( i >= blockList.size() )
	    {
                break;
            }

            blockId = ((Integer)blockList.get(i)).intValue();
            time = (Timestamp)timeList.get(i);
            availability = ((Integer)availabilityList.get(i)).intValue();
        }

        lastReportProcessed = i-1;
        initialLastReportProcessed = lastReportProcessed;
    }

    public void restartTheParkingMap()
    {
        // This function will restore to the initialParkingMap so that when testing another algorithm the same conditions will apply.

        lastReportProcessed = initialLastReportProcessed;

        parkingMap = new HashMap<Integer,Integer>();

        Object keys[] = initialParkingMap.keySet().toArray();
        for (int k = 0; k < keys.length; k++ )
	{
            int currentKey = ((Integer)keys[k]).intValue();
            int availability = ((Integer)initialParkingMap.get(currentKey)).intValue();
            parkingMap.put(currentKey,availability);
        }
    }

    private void readDataIntoMemory()
    {
        int blockId = 0;
        String text = "";
        int availability = -1;

        try 
        {
            String line = readFile.readLine();

	    line = readFile.readLine();// read the line after the header
	    while (line != null) 
            {
                String fileData[] = line.split(",");
                blockId = Integer.parseInt(fileData[0]);
                text = fileData[2].substring(0, fileData[2].indexOf(".")).replace(" ", ":");
                DateFormat df = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
                Timestamp timestamp = new Timestamp(df.parse(text.replace("-", ":")).getTime());
                availability = Integer.parseInt(fileData[1]);

                blockList.add(blockId);
                availabilityList.add(availability);
                timeList.add(timestamp);
                line = readFile.readLine();
	    }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String args[])
    {
        ParkingData pd = new ParkingData("dbProjection_4_6_12.csv"); 
    }

}
