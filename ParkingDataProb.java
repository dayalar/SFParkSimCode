import java.io.BufferedReader;
import java.io.FileReader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.sql.Timestamp;


public class ParkingDataProb
{
    private static String fileName;

    private ArrayList<Integer> blockList = new ArrayList<Integer>();
    private ArrayList<Timestamp> timeList = new ArrayList<Timestamp>();
    private ArrayList<Double> probabilityList = new ArrayList<Double>();

    private HashMap<Integer,Double> parkingMap;
    private HashMap<Integer,Double> initialParkingMap;
    private int lastReportProcessed;
    private int initialLastReportProcessed;

    BufferedReader readFile;

    public ParkingDataProb(String fN)
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

        initiateTheParkingMap(new Timestamp(112,6,1,17,45,24,0));
        System.out.println(getProbability(847032));
        advanceToTime(new Timestamp(112,6,1,22,50,0,0));
        System.out.println(getProbability(847032));
        advanceToTime(new Timestamp(112,6,1,23,30,0,0));
        System.out.println(getProbability(847032));
        /*restartTheParkingMap();
        System.out.println(getProbability(847032));
        advanceToTime(new Timestamp(112,6,1,19,50,0,0));
        System.out.println(getProbability(847032));
        advanceToTime(new Timestamp(112,6,1,23,30,0,0));
        System.out.println(getProbability(847032));*/
    }

    public double getProbability(int b)
    {
        // Will return the probability for the given block at the time to which the profile has been updated.
        if ( parkingMap.containsKey(b) )
        {
            return parkingMap.get(b).doubleValue();
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
        double probability = ((Double)probabilityList.get(i)).doubleValue();
        
        while ( time.before(t) )
	{
            parkingMap.put(blockId,probability);

            i = i+1;

            if ( i >= blockList.size() )
	    {
                break;
            }

            blockId = ((Integer)blockList.get(i)).intValue();
            time = (Timestamp)timeList.get(i);
            probability = ((Double)probabilityList.get(i)).doubleValue();
        }

        lastReportProcessed = i-1;
    }

    public void initiateTheParkingMap(Timestamp t)
    {
        // It will scan the data and give the probability according to the last report for each block.

        parkingMap = new HashMap<Integer,Double>();
        initialParkingMap = new HashMap<Integer,Double>();

        int i = 0;        
        int blockId = ((Integer)blockList.get(i)).intValue();
        Timestamp time = (Timestamp)timeList.get(i);
        double probability = ((Double)probabilityList.get(i)).doubleValue();
        
        while ( time.before(t) )
	{
            parkingMap.put(blockId,probability);
            initialParkingMap.put(blockId,probability);

            i = i+1;

            if ( i >= blockList.size() )
	    {
                break;
            }

            blockId = ((Integer)blockList.get(i)).intValue();
            time = (Timestamp)timeList.get(i);
            probability = ((Double)probabilityList.get(i)).doubleValue();
        }

        lastReportProcessed = i-1;
        initialLastReportProcessed = lastReportProcessed;
    }

    public void restartTheParkingMap()
    {
        // This function will restore to the initialParkingMap so that when testing another algorithm the same conditions will apply.

        lastReportProcessed = initialLastReportProcessed;

        parkingMap = new HashMap<Integer,Double>();

        Object keys[] = initialParkingMap.keySet().toArray();
        for (int k = 0; k < keys.length; k++ )
	{
            int currentKey = ((Integer)keys[k]).intValue();
            double probability = ((Double)initialParkingMap.get(currentKey)).doubleValue();
            parkingMap.put(currentKey,probability);
        }
    }

    private void readDataIntoMemory()
    {
        int blockId = 0;
        String text = "";
        double probability = -1;
        try 
        {
            //readFile.readLine();
            String line = readFile.readLine();

	    // line = readFile.readLine();// read the line after the header
	    while (line != null) 
            {
                String fileData[] = line.split(",");
                blockId = Integer.parseInt(fileData[0]);
                text = fileData[2].substring(0, fileData[2].indexOf(".")).replace(" ", ":");
                DateFormat df = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
                Timestamp timestamp = new Timestamp(df.parse(text.replace("-", ":")).getTime());
                probability = Double.parseDouble(fileData[1]);

                blockList.add(blockId);
                probabilityList.add(probability);
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
        ParkingDataProb pd = new ParkingDataProb("avProbsEvery60Minutes_june2012_avRducedBy0.7.csv"); 
    }

}
