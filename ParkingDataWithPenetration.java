import java.io.BufferedReader;
import java.io.FileReader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
//import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Random;
import java.sql.Timestamp;


public class ParkingDataWithPenetration
{
    private static String fileName;
    private Random r = new Random();

    private int PARK = PreprocessingForParkingDeparking.PARK;
    private int DEPARK = PreprocessingForParkingDeparking.DEPARK;

    private ArrayList<Integer> blockList = new ArrayList<Integer>();
    private ArrayList<Timestamp> timeList = new ArrayList<Timestamp>();
    private ArrayList<Integer> parkDeparkList = new ArrayList<Integer>();

    private ArrayList<Integer> blockListPenetrated = new ArrayList<Integer>();
    private ArrayList<Timestamp> timeListPenetrated = new ArrayList<Timestamp>();
    private ArrayList<Integer> parkDeparkListPenetrated = new ArrayList<Integer>();

    private HashMap<Integer,ArrayList<Timestamp>> parkingMap;
    private HashMap<Integer,ArrayList<Timestamp>> avOnlyParkingMap;
    private HashMap<Integer,ArrayList<Timestamp>> initialParkingMap;
    private HashMap<Integer,ArrayList<Timestamp>> initialAvOnlyParkingMap;
    private int lastReportProcessed;
    private int initialLastReportProcessed;


    private Timestamp currentTime;
    private Timestamp initialCurrentTime;


    private int disregardSALTReportSeconds = 2*60*60; // After two hours

    BufferedReader readFile;

    public ParkingDataWithPenetration(String fN)
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
        readParkDeparksIntoMemory();

        //penetrateTheData((float)1.0);
        //System.out.println("blockList.size() = " + blockList.size() + ", blockListPenetrated.size() = " + blockListPenetrated.size());
        //System.out.println(computeSALT(new Timestamp(112,4,6,23,59,59,0)));
 
        //initiateThePenetratedProfile((float)0.1,new Timestamp(112,3,6,17,45,24,0));
        //System.out.println(getAvailability(847001));
        //advanceToTime(new Timestamp(112,3,6,19,50,0,0));
        //System.out.println(getAvailability(847001));
        //advanceToTime(new Timestamp(112,3,6,23,30,0,0));
        //System.out.println(getAvailability(847001));
        //restartTheParkingMap();
        //System.out.println(getAvailability(847001));
        //advanceToTime(new Timestamp(112,3,6,19,50,0,0));
        //System.out.println(getAvailability(847001));
        //advanceToTime(new Timestamp(112,3,6,23,30,0,0));
        //System.out.println(getAvailability(847001));
    }

    public ArrayList<Timestamp> getAvailabilityReports(int blockId)
    {
        ArrayList<Timestamp> avReports = (ArrayList<Timestamp>)(parkingMap.get(blockId));
        //System.out.println("avReports size is " + avReports.size());
        if ( avReports == null )
	    {
            return (new ArrayList<Timestamp>());
        }
        return avReports;
    }

    public void initiateThePenetratedProfile(float pen, Timestamp t)
    {
        penetrateTheData(pen);
        initiateTheParkingMap(t);
    }

    private boolean minutesHavePassed(Timestamp t, int minutes)
    {
        // Will return true if report should be removed because it is too old
        long timePassedInMillis = currentTime.getTime() - t.getTime();
        double minutesPassed = (timePassedInMillis / 1000.0) / 60.0;

        if ( minutesPassed <= minutes )
	    {
            return false;
        }
        else
	    {
            return true;
        }
    }

    public double getAgingScoreOnlyAv(int b, int T)
    {
        // Will return the relevance score for the currentBlock.

        if ( avOnlyParkingMap.containsKey(b) )
	    {
            ArrayList<Timestamp> bList = (ArrayList<Timestamp>)avOnlyParkingMap.get(b);
            if (bList.size() > 0)
	        {
                Timestamp currentTimeReport = (Timestamp)bList.get(0);
                while ( minutesHavePassed(currentTimeReport,T) )
		        {
                    // Current report is stale so then remove it...
                    bList.remove(0);
                    if ( bList.size() > 0 )
		            {
                        currentTimeReport = (Timestamp)bList.get(0);
                    }
                    else
		            {
                        return 0.0;
                    }
                }

                double score = 0.0;
                for ( int i = 0; i < bList.size(); i++ )
		        {
                    score += relevanceScore((Timestamp)bList.get(i), T);
                }
                return score;
            }
            else
	        {
                // No reports on the list
                return 0.0;
            }            
        }
        else
	    {
            return 0.0;
        }
    }

    public double getAgingScoreOnlyAv(int b, int T, HashMap<Integer,Timestamp> foundEmptyTimeT)
    {
        // Will return the relevance score for the currentBlock.
    	Timestamp lastSeenEmptyTime = foundEmptyTimeT.get(b);
    	if ( lastSeenEmptyTime == null )
    	{
    	    return getAgingScoreOnlyAv(b,T);	
    	}
    	else
    	{
            if ( avOnlyParkingMap.containsKey(b) )
	        {
                ArrayList<Timestamp> bList = (ArrayList<Timestamp>)avOnlyParkingMap.get(b);
                if (bList.size() > 0)
	            {
                    Timestamp currentTimeReport = (Timestamp)bList.get(0);
                    while ( minutesHavePassed(currentTimeReport,T) )
		            {
                        // Current report is stale so then remove it...
                        bList.remove(0);
                        if ( bList.size() > 0 )
		                {
                            currentTimeReport = (Timestamp)bList.get(0);
                        }
                        else
		                {
                            return 0.0;
                        }
                    }
                    while ( currentTimeReport.before(lastSeenEmptyTime) )
                    {
                        // Current report is stale so then remove it...
                        bList.remove(0);
                        if ( bList.size() > 0 )
		                {
                            currentTimeReport = (Timestamp)bList.get(0);
                        }
                        else
		                {
                            return 0.0;
                        }                    	
                    }

                    double score = 0.0;
                    for ( int i = 0; i < bList.size(); i++ )
		            {
                        score += relevanceScore((Timestamp)bList.get(i), T);
                    }
                    return score;
                }
                else
	           {
                    // No reports on the list
                    return 0.0;
                }            
            }
            else
	        {
                return 0.0;
            }
    	}
    }

    public double relevanceScore(Timestamp t, int T)
    {
        long ageInMillis = currentTime.getTime()-t.getTime();
        double ageInMinutes = (ageInMillis / 1000.0) / 60.0;
        return (1 - (ageInMinutes / (double)T));
    }

    public int getOnlyAvReportsTotal(int b, int T, HashMap<Integer,Timestamp> foundEmptyTimeT)
    {
        // Will return the number of availability reports (DEPARK) received for this block in the last T minutes.
    	
    	// Here will use the foundEmptyTimeT HashMap, to not consider any report that is before the last time that
    	// the block was found empty.
    	
    	Timestamp lastSeenEmptyTime = foundEmptyTimeT.get(b);
    	
    	if ( lastSeenEmptyTime == null )
    	{
    	    return getOnlyAvReportsTotal(b,T);	
    	}
    	else
    	{
            if ( avOnlyParkingMap.containsKey(b) )
	        {
                ArrayList<Timestamp> bList = (ArrayList<Timestamp>)avOnlyParkingMap.get(b);
                if (bList.size() > 0)
	            {
                    Timestamp currentTimeReport = (Timestamp)bList.get(0);
                    while ( minutesHavePassed(currentTimeReport,T) )
		            {
                        // Current report is stale so then remove it...
                        bList.remove(0);
                        if ( bList.size() > 0 )
		                {
                            currentTimeReport = (Timestamp)bList.get(0);
                        }
                        else
		                {
                            return 0;
                        }
                    }

                    while ( currentTimeReport.before(lastSeenEmptyTime) )
                    {
                        bList.remove(0);
                        if ( bList.size() > 0 )
                        {
                        	currentTimeReport = (Timestamp)bList.get(0);
                        }
                        else
                        {
                        	return 0;
                        }
                    }
                    
                    return bList.size();
                }
                else
	            {
                    // No reports on the list
                    return 0;
                }            
            }
            else
	        {
                return 0;
            }
    	}
    }
    
    public int getOnlyAvReportsTotal(int b, int T)
    {
        // Will return the number of availability reports (DEPARK) received for this block in the last T minutes.
        if ( avOnlyParkingMap.containsKey(b) )
	    {
            ArrayList<Timestamp> bList = (ArrayList<Timestamp>)avOnlyParkingMap.get(b);
            if (bList.size() > 0)
	        {
                Timestamp currentTimeReport = (Timestamp)bList.get(0);
                while ( minutesHavePassed(currentTimeReport,T) )
		        {
                    // Current report is stale so then remove it...
                    bList.remove(0);
                    if ( bList.size() > 0 )
		            {
                        currentTimeReport = (Timestamp)bList.get(0);
                    }
                    else
		            {
                        return 0;
                    }
                }

                return bList.size();
            }
            else
	        {
                // No reports on the list
                return 0;
            }            
        }
        else
	    {
            return 0;
        }
    }

    public int getAvailability(int b, int T)
    {
        // Will return the availability for the given block at the time to which the profile has been updated.
        if ( parkingMap.containsKey(b) )
	    {
            ArrayList<Timestamp> bList = (ArrayList<Timestamp>)parkingMap.get(b);
            if (bList.size() > 0)
	        {
                Timestamp currentTimeReport = (Timestamp)bList.get(0);
                while ( minutesHavePassed(currentTimeReport,T) )
		        {
                    // Current report is stale so then remove it...
                    bList.remove(0);
                    if ( bList.size() > 0 )
		            {
                        currentTimeReport = (Timestamp)bList.get(0);
                    }
                    else
		            {
                        return 0;
                    }
                }

                return bList.size();
            }
            else
	        {
                // No reports on the list
                return 0;
            }            
        }
        else
	    {
            return 0;
        }
    }

    public int getAvailability(int b, int T, HashMap<Integer,Timestamp> foundEmptyTimeT)
    {
    	    	
    	Timestamp lastSeenEmptyTime = foundEmptyTimeT.get(b);
    	
    	if ( lastSeenEmptyTime == null )
    	{
    	    return getAvailability(b,T);	
    	}
    	else
    	{
            if ( parkingMap.containsKey(b) )
	        {
                ArrayList<Timestamp> bList = (ArrayList<Timestamp>)parkingMap.get(b);
                if (bList.size() > 0)
	            {
                    Timestamp currentTimeReport = (Timestamp)bList.get(0);
                    while ( minutesHavePassed(currentTimeReport,T) )
		            {
                        // Current report is stale so then remove it...
                        bList.remove(0);
                        if ( bList.size() > 0 )
		                {
                            currentTimeReport = (Timestamp)bList.get(0);
                        }
                        else
		                {
                            return 0;
                        }
                    }

                    while ( currentTimeReport.before(lastSeenEmptyTime) )
                    {
                        bList.remove(0);
                        if ( bList.size() > 0 )
                        {
                        	currentTimeReport = (Timestamp)bList.get(0);
                        }
                        else
                        {
                        	return 0;
                        }
                    }
                    
                    return bList.size();
                }
                else
	            {
                    // No reports on the list
                    return 0;
                }            
            }
            else
	        {
                return 0;
            }
    	}
    }

    /*public int getAvailability(int b)
    {
        // Will return the availability for the given block at the time to which the profile has been updated.
        if ( parkingMap.containsKey(b) )
        {
            ArrayList<Timestamp> bList = (ArrayList<Timestamp>)parkingMap.get(b);
            return bList.size();        
        }
        else
        {
            return 0;
        }
    }*/

    //public double getAvailabilityProbability(int b)
    //{
    //    // Will return the availability probability for the given block, at the time to which the profile has been updated.
    //    ArrayList<Timestamp> bList = (ArrayList<Timestamp>)parkingMap.get(b);
    //    return 0.0; // NOT IMPLEMENTED YET!!!!!!!!!!
    //}

    public void advanceToTime(Timestamp t)
    {
        // This function assumes that the function will be called with progressively larger timestamps...

        currentTime = t;

        int i = lastReportProcessed+1;
        //System.out.println("Value of i = " + i + ", but size of list is " + blockListPenetrated.size());
        int blockId = ((Integer)blockListPenetrated.get(i)).intValue();
        Timestamp time = (Timestamp)timeListPenetrated.get(i);
        int parkDepark = ((Integer)parkDeparkListPenetrated.get(i)).intValue();
        
        while ( time.before(t) )
	    {
            if ( !parkingMap.containsKey(blockId) )
	        {
                parkingMap.put(blockId,new ArrayList<Timestamp>());
            }
            if ( !avOnlyParkingMap.containsKey(blockId) )
	        {
                avOnlyParkingMap.put(blockId,new ArrayList<Timestamp>());
            }

            // CurrentList will save all DEPARK events.  Except those that are removed by PARK events.
            ArrayList<Timestamp> currentList = (ArrayList<Timestamp>)parkingMap.get(blockId);
            ArrayList<Timestamp> avOnlyCurrentList = (ArrayList<Timestamp>)avOnlyParkingMap.get(blockId);

            if ( parkDepark == PARK )
	        {
                if ( !currentList.isEmpty() )
		        {
                    // Remove the earliest DEPARK event from the list (to match it with this PARK event).
                    currentList.remove(0);
                }
            }
            else if ( parkDepark == DEPARK )
	        {
                currentList.add(time);
                avOnlyCurrentList.add(time);
            }
            else
	        {
                System.out.println("ERROR: Bad value for the PARK/DEPARK entry.");
                System.exit(-1);
            }
            
            parkingMap.put(blockId,currentList);
            avOnlyParkingMap.put(blockId,avOnlyCurrentList);

            i = i+1;

            if ( i >= blockListPenetrated.size() )
	        {
                break;
            }

            blockId = ((Integer)blockListPenetrated.get(i)).intValue();
            time = (Timestamp)timeListPenetrated.get(i);
            parkDepark = ((Integer)parkDeparkListPenetrated.get(i)).intValue();
        }

        lastReportProcessed = i-1;
    }

    public void restartTheParkingMap()
    {
        // This function will restore to the initialParkingMap so that when testing another algorithm the same conditions will apply.

        currentTime = initialCurrentTime;

        lastReportProcessed = initialLastReportProcessed;

        parkingMap = new HashMap<Integer,ArrayList<Timestamp>>();
        avOnlyParkingMap = new HashMap<Integer,ArrayList<Timestamp>>();

        Object keys[] = initialParkingMap.keySet().toArray();
        for (int k = 0; k < keys.length; k++ )
	    {
            int currentKey = ((Integer)keys[k]).intValue();
            ArrayList<Timestamp> initCurrentList = (ArrayList<Timestamp>)initialParkingMap.get(currentKey);
            ArrayList<Timestamp> listToAdd = new ArrayList<Timestamp>();
            ArrayList<Timestamp> initAvOnlyCurrentList = (ArrayList<Timestamp>)initialAvOnlyParkingMap.get(currentKey);
            ArrayList<Timestamp> listToAddAvOnly = new ArrayList<Timestamp>();

            for ( int i = 0; i < initCurrentList.size(); i++ )
	        {
                Timestamp t = new Timestamp(((Timestamp)initCurrentList.get(i)).getTime());
                listToAdd.add(t);
            }
            parkingMap.put(currentKey,listToAdd);

            for ( int i = 0; i < initAvOnlyCurrentList.size(); i++ )
	        {
                Timestamp t = new Timestamp(((Timestamp)initAvOnlyCurrentList.get(i)).getTime());
                listToAddAvOnly.add(t);
            }
            avOnlyParkingMap.put(currentKey,listToAddAvOnly);
        }
    }

    private void initiateTheParkingMap(Timestamp t)
    {
        // Will create a map that has the timestamps of the currently available slots for each blockId
        // It will scan the penetrated data and give a view of how many slots are available according to the penetrated data.

        currentTime = t;
        initialCurrentTime = t;

        parkingMap = new HashMap<Integer,ArrayList<Timestamp>>();
        avOnlyParkingMap = new HashMap<Integer,ArrayList<Timestamp>>();
        initialParkingMap = new HashMap<Integer,ArrayList<Timestamp>>();
        initialAvOnlyParkingMap = new HashMap<Integer,ArrayList<Timestamp>>();

        int i = 0;        
        int blockId = ((Integer)blockListPenetrated.get(i)).intValue();
        Timestamp time = (Timestamp)timeListPenetrated.get(i);
        int parkDepark = ((Integer)parkDeparkListPenetrated.get(i)).intValue();
        
        while ( time.before(t) )
	    {
            if ( !parkingMap.containsKey(blockId) )
	        {
                parkingMap.put(blockId,new ArrayList<Timestamp>());
                initialParkingMap.put(blockId,new ArrayList<Timestamp>());
            }
            if ( !avOnlyParkingMap.containsKey(blockId) )
	        {
                avOnlyParkingMap.put(blockId,new ArrayList<Timestamp>());
                initialAvOnlyParkingMap.put(blockId,new ArrayList<Timestamp>());
            }


            // CurrentList will save all DEPARK events.  Except those that are removed by PARK events.
            ArrayList<Timestamp> currentList = (ArrayList<Timestamp>)parkingMap.get(blockId);
            ArrayList<Timestamp> avOnlyCurrentList = (ArrayList<Timestamp>)avOnlyParkingMap.get(blockId);
            ArrayList<Timestamp> initCurrentList = (ArrayList<Timestamp>)initialParkingMap.get(blockId);
            ArrayList<Timestamp> initAvOnlyCurrentList = (ArrayList<Timestamp>)initialAvOnlyParkingMap.get(blockId);

            if ( parkDepark == PARK )
	        {
                if ( !currentList.isEmpty() )
		        {
                    // Remove the earliest DEPARK event from the list (to match it with this PARK event).
                    currentList.remove(0);
                    initCurrentList.remove(0);
                }
            }
            else if ( parkDepark == DEPARK )
	        {
                currentList.add(time);
                avOnlyCurrentList.add(time);
                initCurrentList.add(time);
                initAvOnlyCurrentList.add(time);
            }
            else
	        {
                System.out.println("ERROR: Bad value for the PARK/DEPARK entry.");
                System.exit(-1);
            }
            
            parkingMap.put(blockId,currentList);
            initialParkingMap.put(blockId,initCurrentList);
            avOnlyParkingMap.put(blockId,avOnlyCurrentList);
            initialAvOnlyParkingMap.put(blockId,initAvOnlyCurrentList);

            i = i+1;

            if ( i >= blockListPenetrated.size() )
	        {
                break;
            }

            blockId = ((Integer)blockListPenetrated.get(i)).intValue();
            time = (Timestamp)timeListPenetrated.get(i);
            parkDepark = ((Integer)parkDeparkListPenetrated.get(i)).intValue();
        }

        lastReportProcessed = i-1;
        initialLastReportProcessed = lastReportProcessed;
    }

    private void penetrateTheData(float penetration)
    {
        // This will generate a new data set that has a level of penetration.  penetration should be a number higher than 0 and no larger than 1.
        blockListPenetrated = new ArrayList<Integer>();
        timeListPenetrated = new ArrayList<Timestamp>();
        parkDeparkListPenetrated = new ArrayList<Integer>();

        for ( int i = 0; i < blockList.size(); i++ )
        {
            double v = r.nextDouble();
            if ( v <= penetration )
            {
                //System.out.println(v);
                int blockId = ((Integer)blockList.get(i)).intValue();
                Timestamp time = (Timestamp)timeList.get(i);
                int parkDepark = ((Integer)parkDeparkList.get(i)).intValue();

                blockListPenetrated.add(blockId);
                timeListPenetrated.add(time);
                parkDeparkListPenetrated.add(parkDepark);
                //System.out.println(blockId + "," + time + "," + parkDepark);  
            }
        }
    }

    private void readParkDeparksIntoMemory()
    {
        int blockId = 0;
        String text = "";
        int parkDepark = -1;

        try 
        {
            String line = readFile.readLine();

	        line = readFile.readLine();// read the line after the header
	        while (line != null) 
            {
                String fileData[] = line.split(",");
                blockId = Integer.parseInt(fileData[0]);
                text = fileData[1].substring(0, fileData[1].indexOf(".")).replace(" ", ":");
                DateFormat df = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
                Timestamp timestamp = new Timestamp(df.parse(text.replace("-", ":")).getTime());
                parkDepark = Integer.parseInt(fileData[2]);

                blockList.add(blockId);
                parkDeparkList.add(parkDepark);
                timeList.add(timestamp);
                line = readFile.readLine();
	        }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public double computeSALT(Timestamp t)
    {

        parkingMap = new HashMap<Integer,ArrayList<Timestamp>>();
        initialParkingMap = new HashMap<Integer,ArrayList<Timestamp>>();
        double totalSeconds = 0.0;
        int totalParks = 0;

        int i = 0;        
        int blockId = ((Integer)blockListPenetrated.get(i)).intValue();
        Timestamp time = (Timestamp)timeListPenetrated.get(i);
        int parkDepark = ((Integer)parkDeparkListPenetrated.get(i)).intValue();
        
        while ( time.before(t) )
	    {
            if ( !parkingMap.containsKey(blockId) )
	        {
                parkingMap.put(blockId,new ArrayList<Timestamp>());
                initialParkingMap.put(blockId,new ArrayList<Timestamp>());
            }

            // CurrentList will save all DEPARK events.  Except those that are removed by PARK events.
            ArrayList<Timestamp> currentList = (ArrayList<Timestamp>)parkingMap.get(blockId);
            ArrayList<Timestamp> initCurrentList = (ArrayList<Timestamp>)initialParkingMap.get(blockId);

            if ( parkDepark == PARK )
	        {
                if ( !currentList.isEmpty() )
		        {
                    // Remove the earliest DEPARK event from the list (to match it with this PARK event).
                    Timestamp pastTime = (Timestamp)currentList.get(currentList.size()-1);
                    currentList.remove(currentList.size()-1);
                    initCurrentList.remove(initCurrentList.size()-1);
                    long timeDiffInMillis = time.getTime() - pastTime.getTime();
                    double seconds = ((double)timeDiffInMillis / 1000.0);
                    if ( seconds <= disregardSALTReportSeconds )
		            {
                        totalSeconds += seconds;
                        totalParks++;
                        System.out.println(totalParks + ": Current time is " + time + " and pastTime is " + pastTime + ". Will add " + seconds + " seconds to block " + blockId + ".");
                    }
                }
            }
            else if ( parkDepark == DEPARK )
	        {
                currentList.add(time);
                initCurrentList.add(time);
            }
            else
	        {
                System.out.println("ERROR: Bad value for the PARK/DEPARK entry.");
                System.exit(-1);
            }
            
            parkingMap.put(blockId,currentList);
            initialParkingMap.put(blockId,initCurrentList);

            i = i+1;

            if ( i >= blockListPenetrated.size() )
	        {
                break;
            }

            blockId = ((Integer)blockListPenetrated.get(i)).intValue();
            time = (Timestamp)timeListPenetrated.get(i);
            parkDepark = ((Integer)parkDeparkListPenetrated.get(i)).intValue();
        }

        lastReportProcessed = i-1;
        initialLastReportProcessed = lastReportProcessed;

        return (totalSeconds / (double)totalParks);
    }

    //public static void main(String args[])
    //{
    //    ParkingDataWithPenetration pdwp = new ParkingDataWithPenetration("dbProjection_5_6_12_parkDepark.csv"); 
    //}
}
