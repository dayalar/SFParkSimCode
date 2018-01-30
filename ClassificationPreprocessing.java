import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.sql.Timestamp;

public class ClassificationPreprocessing
{
    private String dataFile = "dbProjection_4_6_12_ver2.csv";
    private String outputFile = "preProcessedSFParkData.csv";
    private int secondsInInterval = 60;
    private int inactivityIntervalInHours = 12;

    public ClassificationPreprocessing()
    {
        try
	{
            FileReader fr = new FileReader(dataFile);
            BufferedReader br = new BufferedReader(fr);

            FileWriter fw = new FileWriter(outputFile);
            BufferedWriter bw = new BufferedWriter(fw);

            String line = "";
            line = br.readLine();
            line = br.readLine();

            String fileData[] = line.split(",");
            int blockId = Integer.parseInt(fileData[0]);
            int available = Integer.parseInt(fileData[1]);
            String text = "";
	    text = fileData[2].substring(0, fileData[2].indexOf(".")).replace(" ", ":");
	    DateFormat df = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
	    Timestamp timestamp = new Timestamp(df.parse(text.replace("-", ":")).getTime());

            Timestamp currentTime = timestamp;
            Timestamp nextTimeInterval = addTimes(currentTime,secondsInInterval);

            HashMap<Integer,Integer> activeBlocks = new HashMap<Integer,Integer>();
            HashMap<Integer,Timestamp> lastReportTime = new HashMap<Integer,Timestamp>();
            HashSet<Integer> currentNoAvailBlocks = new HashSet<Integer>();

            while (line != null)
	    {
                fileData = line.split(",");
                blockId = Integer.parseInt(fileData[0]);
                available = Integer.parseInt(fileData[1]);
                text = "";
	        text = fileData[2].substring(0, fileData[2].indexOf(".")).replace(" ", ":");
	        df = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
	        timestamp = new Timestamp(df.parse(text.replace("-", ":")).getTime());

                if ( timestamp.after(nextTimeInterval) )
		{
                    // Current Interval is over.  Print the available classification reports to a file.
                    // DUMP THE DATA... ie. FOR EACH ACTIVE BLOCK OUTPUT AN AVAILABILITY REPORT
                    Object activeBlockIds[] = activeBlocks.keySet().toArray();
                    for ( int i = 0; i < activeBlockIds.length; i++ )
		    {
                        int currentBlock = ((Integer)activeBlockIds[i]).intValue();
                        int availability = (Integer)activeBlocks.get(currentBlock);
                        if ( currentNoAvailBlocks.contains(currentBlock) )
			{
                            // Print a NO availability report.
                            fw.write(currentBlock+","+currentTime+",N,"+availability+"\n");
                        }
                        else
			{
                            // Print a YES availability report.
                            fw.write(currentBlock+","+currentTime+",Y,"+availability+"\n");
                        }

                        if ( inactivityIntervalPassed(currentTime,lastReportTime.get(currentBlock)) )
			{
                            activeBlocks.remove(currentBlock);
                        }
                    }

                    currentTime = new Timestamp(nextTimeInterval.getTime());     
                    nextTimeInterval = addTimes(currentTime,secondsInInterval);   
                }

                if ( !activeBlocks.containsKey(blockId) )
		{
                    // Then this is the first time we see a report for this block.
                    activeBlocks.put(blockId,available);
                    lastReportTime.put(blockId,currentTime);
                }

                if ( available == 0 )
		{
                    // Add the blockId to currentNoAvailBlocks
                    currentNoAvailBlocks.add(blockId);
                    activeBlocks.put(blockId,available);
                    lastReportTime.put(blockId,currentTime);
                }
                else 
		{
                    // There is some availability
                    if ( currentNoAvailBlocks.contains(blockId) )
		    {
                        // Add it to be taken away later because now there's availability.
                        currentNoAvailBlocks.remove(blockId);
                        activeBlocks.put(blockId,available);
                        lastReportTime.put(blockId,currentTime);
                    }
                    else
		    {
                        activeBlocks.put(blockId,available);
                        lastReportTime.put(blockId,currentTime);
                    }
                    // else // Nothing to do...
                }

                line = br.readLine();
            }
        }
        catch (Exception e)
	{
            e.printStackTrace();
        }
    }

    public Timestamp addTimes(Timestamp t, double s)
    {
        return new Timestamp(t.getTime() + ((long)(s*1000)));
    }

    public boolean inactivityIntervalPassed(Timestamp current,Timestamp last)
    {
        double secondsInInactivityInterval = inactivityIntervalInHours*60*60;
        Timestamp inactivityPlusLast = addTimes(last,secondsInInactivityInterval);
        return current.after(inactivityPlusLast); 
    }

    public static void main(String args[])
    {
        ClassificationPreprocessing cp = new ClassificationPreprocessing();
    }

}
