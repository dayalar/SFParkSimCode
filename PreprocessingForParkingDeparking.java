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

public class PreprocessingForParkingDeparking
{
    //private String dataFile = "dbProjection_5_6_12_avRducedBy0.7_withoutHeader.csv";
    private String dataFile = "dbProjection_4_6_12.csv";
    private String maxAvailabilityData = "maxAvailabilityPerBlock.csv";
    public static final int PARK = 0;
    public static final int DEPARK = 1;
    public static HashMap<Integer,Double> inverseMu = new HashMap<Integer,Double>();
    public static HashMap<Integer,Double> inverseLambda = new HashMap<Integer,Double>();

    public PreprocessingForParkingDeparking()
    {
        try
	{
            FileReader fr = new FileReader(dataFile);
            BufferedReader br = new BufferedReader(fr);

            // Read in the maxAvailabilityData
	    
            String line = "";
            FileReader fr_max = new FileReader(maxAvailabilityData);
            BufferedReader br_max = new BufferedReader(fr_max);
	    HashMap<Integer,Integer> maxAvailability = new HashMap<Integer,Integer>();

            line = br_max.readLine();
            int blockId;
            String fileData[];
            int maxAvail;
	    while (line != null)
	    {
                fileData = line.split(",");
                maxAvailability.put(Integer.parseInt(fileData[0]), Integer.parseInt(fileData[1]));
		line = br_max.readLine();
	    }

	    line = br.readLine();
            line = br.readLine(); // Added because the current file (dbProjection_4_6_12.csv) has a header line.
            int availability;
            String text;
            DateFormat df;
            Timestamp timestamp;

            HashMap<Integer,Integer> previousAvailability = new HashMap<Integer,Integer>();

	    HashMap<Integer,ArrayList<Timestamp>> timestampQueueConsumption = new HashMap<Integer,ArrayList<Timestamp>>();
	    HashMap<Integer,Double> totalConsumptionTime = new HashMap<Integer,Double>();
	    HashMap<Integer,Integer> numberOfDeparks = new HashMap<Integer,Integer>();

	    HashMap<Integer,ArrayList<Timestamp>> timestampQueueAvailability = new HashMap<Integer,ArrayList<Timestamp>>();
	    HashMap<Integer,Double> totalAvailabilityTime = new HashMap<Integer,Double>();
	    HashMap<Integer,Integer> numberOfParks = new HashMap<Integer,Integer>();

            while (line != null)
	    {
                fileData = line.split(",");
                blockId = Integer.parseInt(fileData[0]);
                availability = Integer.parseInt(fileData[1]);
                text = "";
	        text = fileData[2].substring(0, fileData[2].indexOf(".")).replace(" ", ":");
	        df = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
	        timestamp = new Timestamp(df.parse(text.replace("-", ":")).getTime());

                if ( !previousAvailability.containsKey(blockId) )
		{
                    // Then this is the first time we see a report for this block.
                    previousAvailability.put(blockId,availability);
                    ArrayList<Timestamp> tq = new ArrayList<Timestamp>();
		    for (int i = 0; i < availability; i++)
		    {
                        tq.add(timestamp);
		    }
		    timestampQueueConsumption.put(blockId,tq);
		    totalConsumptionTime.put(blockId,0.0);
		    numberOfDeparks.put(blockId,0);

                    tq = new ArrayList<Timestamp>();
		    for (int i = 0; i < (maxAvailability.get(blockId)-availability); i++)
		    {
                        tq.add(timestamp);
		    }
		    timestampQueueAvailability.put(blockId,tq);
		    totalAvailabilityTime.put(blockId,0.0);
		    numberOfParks.put(blockId,0);
		    
                }
                else
		{
                    int prevAvailability = previousAvailability.get(blockId).intValue();
                    if ( availability <= prevAvailability )
		    {
                        // Parking event
                        int numberOfReports = prevAvailability-availability;
                        for ( int i = 0; i < numberOfReports; i++ )
			{
                            //System.out.println(blockId + "," + timestamp + "," + PARK);
			    ((ArrayList<Timestamp>)timestampQueueConsumption.get(blockId)).add(timestamp);
			    Timestamp previousTimestamp;
			    if ( ((ArrayList<Timestamp>)timestampQueueAvailability.get(blockId)).size() > 0 )
			    {
			        previousTimestamp = ((ArrayList<Timestamp>)timestampQueueAvailability.get(blockId)).remove(0);
				long totalMiliseconds = timestamp.getTime()-previousTimestamp.getTime();
				double totalMinutes = (totalMiliseconds/1000.0)/60.0;
				totalAvailabilityTime.put(blockId,(double)totalAvailabilityTime.get(blockId)+totalMinutes);
				numberOfParks.put(blockId,(int)numberOfParks.get(blockId)+1);
				inverseLambda.put(blockId,totalAvailabilityTime.get(blockId)/numberOfParks.get(blockId));
			    }
                        }
                    }
                    else
		    {
                        // Deparking event
                        int numberOfReports = availability-prevAvailability;
                        for ( int i = 0; i < numberOfReports; i++ )
			{
                            //System.out.println(blockId + "," + timestamp + "," + DEPARK + ", " + ((ArrayList<Timestamp>)timestampQueueConsumption.get(blockId)).size());
			    ((ArrayList<Timestamp>)timestampQueueAvailability.get(blockId)).add(timestamp);
			    Timestamp previousTimestamp;
			    if ( ((ArrayList<Timestamp>)timestampQueueConsumption.get(blockId)).size() > 0 )
			    {
			        previousTimestamp = ((ArrayList<Timestamp>)timestampQueueConsumption.get(blockId)).remove(0);
				long totalMiliseconds = timestamp.getTime()-previousTimestamp.getTime();
				double totalMinutes = (totalMiliseconds/1000.0)/60.0;
				totalConsumptionTime.put(blockId,(double)totalConsumptionTime.get(blockId)+totalMinutes);
				numberOfDeparks.put(blockId,(int)numberOfDeparks.get(blockId)+1);
				inverseMu.put(blockId,totalConsumptionTime.get(blockId)/numberOfDeparks.get(blockId));
			    }
                        }
                    }
                    previousAvailability.put(blockId,availability);
                }

                line = br.readLine();
            }
        }
        catch (Exception e)
	{
            e.printStackTrace();
        }
    }

    public static void main(String args[])
    {
        PreprocessingForParkingDeparking cp = new PreprocessingForParkingDeparking();
    }

}
