import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Random;
import java.sql.Timestamp;



public class AverageAvailabilityComputation2
{
    private int intervalMinutes = 60;
    private int minutesInOneDay = 24*60;
    private int discardThresholdInHours = 24;

    private String nodesFile = "SFPark_edges_FishermansWharf2.csv";
    //private String dataFile = "completeDbProjectionApril2012.csv";
    //private String outputFile = "averagesEvery" + intervalMinutes + ".csv";
    private String dataFile = "dbProjection_4_6_12-5_5_12_avRducedBy0.7_w_errorRate0.1f.csv";


    private ParkingSelection ps = new ParkingSelection(dataFile);

    private Timestamp initialTime = new Timestamp(112,3,6,0,0,0,0);   // 2012-04-06 00:00:00.000		
    private Timestamp lastTime = new Timestamp(112,4,5,23,59,59,999);   // 2012-5-5 23:59:59.999

    private ArrayList<Integer> blockList = new ArrayList<Integer>();

    HashMap<Integer,Integer> totalAvailabilityBlocks = new HashMap<Integer,Integer>();
    HashMap<Integer,Integer> dataPointsBlocks = new HashMap<Integer,Integer>();

    public AverageAvailabilityComputation2()
    {
        //System.out.print("Reading Availability File into Memory...");
        ps.readAvailabilityFileIntoMemory();
        //System.out.println("DONE");

        try
	{
            //System.out.print("Reading block information into memory...");
            readBlocks();
            //System.out.println("DONE");

            //FileWriter fw = new FileWriter(outputFile);
            //BufferedWriter bw = new BufferedWriter(fw);

            Timestamp currentTime = (Timestamp)initialTime.clone();

            int totalIntervals = (int)Math.floor((double)minutesInOneDay/(double)intervalMinutes);
        
            for ( int i = 0; i < totalIntervals; i++ )
	    {
                //System.out.println("Interval " + i );
                // For this interval, will do all days until you reach the lastTime
                for ( int j = 0; j < blockList.size(); j++ )
		{
		    int currentBlock = blockList.get(j);
                    int currentTotal = 0;
                    int currentCount = 0;
                    int minutesToAdd = i*intervalMinutes;
                    currentTime = addTimes(initialTime,minutesToAdd*60);

		    while (currentTime.compareTo(lastTime) < 0)
		    {
                        long availabilityData[] = (long[])ps.computeAvailabilityAtTimeTFromMemory(currentTime).get(currentBlock);
                        //System.out.println(currentBlock + "," + currentTime);
                        if ( !tooLongAgo(new Timestamp(availabilityData[1]),currentTime) )
			{
                            if ( availabilityData[0] > 0 )
			    {
                                currentTotal++; //+= availabilityData[0];
                            }
                            currentCount++;
                        }
                        currentTime = addTimes(currentTime,24*60*60); // To check the same time but the next day.
                    }

                    double avgAvailability;
                    if ( currentCount == 0 )
		    {
                        avgAvailability = 0;
                    }
                    else
		    {
                        avgAvailability = (double)currentTotal/(double)currentCount;
                    }
  
                    //bw.write(currentBlock + "," + avgAvailability + "," + currentTime + "," + currentTotal + "," + currentCount + "\n");
                    System.out.println(currentBlock + "," + avgAvailability + "," + currentTime + "," + currentTotal + "," + currentCount);
                }
                //int minutesToAdd = (i+1)*intervalMinutes;
                //currentTime = addTimes(initialTime,minutesToAdd*60);
            }
        }
        catch (Exception e)
	{
            e.printStackTrace();
        }               
    }

    public boolean tooLongAgo(Timestamp dataPoint, Timestamp curr)
    {
        Timestamp dataPointAndDiscardThres = addTimes(dataPoint,discardThresholdInHours*60*60);
        return dataPointAndDiscardThres.before(curr);
    }

    public Timestamp addTimes(Timestamp t, double s)
    {
        return new Timestamp(t.getTime() + ((long)(s*1000)));
    }

    public void readBlocks()
    {
        try
	{
            FileReader fr = new FileReader(nodesFile);
            BufferedReader br = new BufferedReader(fr);

            String line = br.readLine();
            while ( line != null )
	    {
                String fileData[] = line.split(",");
                int blockId = Integer.parseInt(fileData[0]);
                if (blockId == -1)
		{
                    break;
                }
                //System.out.println(blockId);
                blockList.add(blockId);

                line = br.readLine();
            }
        }
        catch (Exception e)
	{
            e.printStackTrace();
        }
    }

    public static void main (String args[])
    {
        AverageAvailabilityComputation2 aac2 = new AverageAvailabilityComputation2();
    }
}
