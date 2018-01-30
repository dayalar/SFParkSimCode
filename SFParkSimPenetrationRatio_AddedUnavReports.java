import java.sql.Timestamp;
import java.util.*;
import java.io.*;


public class SFParkSimPenetrationRatio_AddedUnavReports
{
    // Algorithms to test.
    private final int GREEDY = 1;
    private final int DM_GPA = 2;
    private final int GREEDY_ALL_DATA = 3;
    private final int DM_GPA_ALL_DATA = 4;
    private final int AVP_LAMBDA_3 = 5;
    private final int AVP_LAMBDA_12 = 6;
    private final int DM_GPA_ONLY_AV = 7;
    private final int DM_GPA_ONLY_AV_WITH_AGING = 8;
    private final int GREEDY_ONLY_AV = 9;
    private final int RANDOM_WALK = 10;
    private final int JOSSE = 13;
    private final int GUO = 14;
    private final int JOSSE2 = 15;
    private final int EndOfSearch = -2;

    //private double SP_Josse[][] = new double[N_NODES][N_NODES];        // Will contain the shortest path distance between node i and node j.
    //private int next_Josse[][] = new int[N_NODES][N_NODES];            // Used by the Floyd-Warshall algorithm to reconstruct the shortest paths.
    //private int SP_direction_Josse[][] = new int[N_NODES][N_NODES];    // Will contain the first node to move towards in the shortest path between node i and node j.

    //private final int SYSTEM_OPT = 11;
    //private final int EASY_NE = 12;

    private int n = SFParkRoadNetworkCreator.N_NODES;
    private int nodeIdStart = SFParkNetwork.nodeIdStart;
    private int nBlocks = 66;

    // Variables from QING GUO simulation system
    private ArrayList<ArrayList<String>> runningOptPaths;
    private ArrayList<ArrayList<Integer>> runningOptPathsPM;
    //private ArrayList<String> runningOptPath = new ArrayList<String>();
    private ArrayList<ArrayList<ArrayList<String>>> optimalPathsWDest = new ArrayList<ArrayList<ArrayList<String>>>();
    private ArrayList<ArrayList<Integer>> optimalPathsPM = new ArrayList<ArrayList<Integer>>();
    private int destinations[];
    private int k_m = 30; //=30 // Maximum number of edges.
    private int tau = 120;
    private int h = (int) Math.ceil(tau / 21.0);  // min cost = 21 sec. avg cost = 59 sec.
    private int k_tau = h;
    private double velocityWalking = Double.POSITIVE_INFINITY; // Equivalent to not adding cost of walking to final destination.
    private double [][] edgeCosts = new double[n][n];
    private ArrayList<ArrayList<Integer>> adjNodes = new ArrayList<ArrayList<Integer>>();
    private int beta = 60*60;
    private ArrayList<ArrayList<Integer>> adjList= new ArrayList<ArrayList<Integer>>(n);
    private double [][] probability;
    private double [][] avail;
    private double [][] var; // variance
    private double [][] probByExp; // estimated probabilities from mean and variance.
    private int probDurationInMin = 60;
    private int initYear = 2012;
    private int initMonth = 5;
    private int initDate = 4;
    private int initHour1 = 20;
    private int initHour2 = 21;
    private int initMinute = 0;
    private int initSecond = 0;


    //*****
    // Data files
    // NOTE: You have to make sure that this second data file (parkDepark) is generated based on the first one...
    //*****
    //private String parkingDataFile = "dbProjection_5_6_12_avRducedBy0.7.csv";
    //private String parkDeparkDataFile = "dbProjection_5_6_12_parkDepark.csv";
    private String parkingDataFile = "dbProjection_4_6_12.csv";
    private String parkingDataFileQing = "sfpark.FishermansWharf_compLevel_1.0.csv";
    private String parkDeparkDataFile = "dbProjection_4_6_12_parkDepark.csv";
    private String timeFile = "times.txt";
    private String edgesFile = "SFPark_edges_FishermansWharf.csv";

    private double intersectionDelay = 0; // Seconds of delay at each intersection
    private double currentDelay[];
    private double velocity = 8.9408/4.0; // 8.9408 m/s = 20 mph
    private double intersectionDelayInMeters = intersectionDelay*velocity;
    private int secondsPastEmpty = 300; // 300s = 5min
    private int numVehs[] = {1,5,10,15,20,25,30,35,40,45,50,55,60}; //{5,10,15,20,25};
    private int numSims = 10000; //40;  // Should be a multiple of numTimes
    private int numRepeatPenetrationTests = 1;    // This is the number of times I randomize the experiment.
    private int numTimes = 20;  
    private int totalSims = numSims*numRepeatPenetrationTests;

    SFParkRoadNetworkCreator creator;

    private SFParkNetwork road = new SFParkNetwork(n);
    private HashMap<Integer,SFParkNode> nodes;
    private SFParkEdge edges[][];
    private double edgeWeights[][];
    private SFParkEdge edgeList[];

    private double SP[][];           // Will contain the shortest path distance between node i and node j.
    private int SP_direction[][];    // Will contain the first node to move towards in the shortest path between node i and node j.

    private HashMap<Integer,int[]> blockIdMap = new HashMap<Integer,int[]>();
    private int blockIds[] = new int[nBlocks];

    private ParkingData groundTruthData = new ParkingData(parkingDataFile);
    private ParkingData groundTruthDataQing = new ParkingData(parkingDataFileQing);

    private ParkingDataWithPenetration userViewOfData = new ParkingDataWithPenetration(parkDeparkDataFile);

    private HashMap<Integer,Integer> foundEmpty;
    private HashMap<Integer,Timestamp> foundEmptyTime;
    private HashMap<Integer,Timestamp> foundEmptyTimeT;
    private SFParkLocation currentLocations[];

    private double rho = 0.9; // Threshold for the Josse algorithm
    
    private PreprocessingForParkingDeparking preprocessPD = new PreprocessingForParkingDeparking();
    
	//private ArrayList<Integer> runningOptPaths[];

    private int numVeh = 1;

    Timestamp times[] = new Timestamp[numTimes];
	
    private float penetrations[] = {(float)0.1,(float)0.2,(float)0.3,(float)0.4,(float)0.5,(float)0.6,(float)0.7,(float)0.8,(float)0.9,(float)0.95,(float)1.0};
    private float penetration;

    Random rEdge = new Random();
    Random rDirection = new Random();
    Random rOffset = new Random();

    private boolean visOutput = false;
     
    private int parkingChanges[][] = new int[n][n];

    //private double SALT_inSeconds = 1287.9026737967915;

    //Timestamp initialProbabilityTime = new Timestamp(112,4,1,0,0,0,0);   // 2012-05-01 00:00:00.000
    //Timestamp dayOfSim = new Timestamp(112,3,6,0,0,0,0); // 2012-04-06 00:00:00.000

    boolean debugGreedyMovement = false;

    Timestamp t_m_startingTime[];

    int T = 5; // in minutes
    int Ts[] = {120};//{1,5,15,30,45,60,75,90,105,120};
    

    //@SuppressWarnings("deprecation")
    public SFParkSimPenetrationRatio_AddedUnavReports()
    {
        if (h <6) { h = 6; } // System.out.println("h = " + h); QING Variable

        readTimes();
        
        creator = new SFParkRoadNetworkCreator();
        System.out.println("penetrationRatio\t" + 
                           "numVeh\t" +
        		   "T\t" +
        		   "timeToParkGreedy\t" + 
        		   "timeToParkGPA_alldirs\t" + 
        		   "timeToParkGreedy_allData\t" + 
        		   "timeToParkGPA_allData\t" + 
        		   "timeToParkGPA_onlyAv\t" + 
        		   "timeToParkGPA_onlyAv_withAging\t" + 
        		   "timeToParkGreedy_onlyAv\t" + 
        		   "timeToParkRandomWalk\t" + 
        		   "timeToParkSysOpt\t" +
        		   "timeToParkEasyNE\t" +
			   "timeToParkJosse\t" +
			   "timeToParkGuo");
        
        road = creator.getRoad();
        nodes = creator.getNodes();
        edges = creator.getEdges();
        edgeWeights = creator.getEdgeWeights();
        SP = creator.getShortestPaths();
        SP_direction = creator.getSP_direction();
        edgeList = creator.getEdgeList();
	computeAdjList();  // QING CODE

        
        // Compute edgeCosts and adjNodes:
	for ( int i = 0; i < n; i++ )  // QING CODE
	{
	    ArrayList<Integer> adjNodesForI = new ArrayList<Integer>();
	    for ( int j = 0; j < n; j++ )
	    {
		if ( edges[i][j] != null )
		{
		    adjNodesForI.add(j);
		    edgeCosts[i][j] = edgeWeights[i][j]/velocity;
		}
	    }
	    adjNodes.add(i, adjNodesForI);
	}

	
        createBlockIdMap();
        double timeToParkGreedy = 0.0;
        double timeToParkGPA_alldirs = 0.0;
        double timeToParkGreedy_allData = 0.0;
        double timeToParkGPA_alldirs_allData = 0.0;
        //double timeToParkAVP_lambda3 = 0.0;
        //double timeToParkAVP_lambda12 = 0.0;
        double timeToParkGPA_onlyAv = 0.0;
        double timeToParkGPA_onlyAv_withAging = 0.0;
        double timeToParkGreedy_onlyAv = 0.0;
        double timeToParkRandomWalk = 0.0;
        double timeToParkSysOpt = 0.0;
        double timeToParkEasyNE = 0.0;
	double timeToParkJosse = 0.0;
	double timeToParkGuo = 0.0;

	//System.out.println("QingDebug: Initiating History Paths");
        initiateHistoryPaths();
	//System.out.println("QingDebug: Initiating History Paths...DONE");

	// ALL THIS IS QING CODE
	groundTruthDataQing.initiateTheParkingMap(new Timestamp(2012-1900, 4-1, 6, 0, 0, 0, 0));
	probability = new double[n][n]; // Probabilities need to be re-initialized every time. QING CODE
	avail = new double[n][n]; // Availability need to be re-initialized every time.
	var = new double[n][n]; // Variance need to be re-initialized every time.
	probByExp = new double[n][n]; // Prob need to be re-initialized every time.
        
	System.out.println("QingDebug: Computing ProbAvail_randomscan");
	computeProbAvail_randomScan();
	optimalPathsWDest = new ArrayList<ArrayList<ArrayList<String>>>();
	optimalPathsPM = new ArrayList<ArrayList<Integer>>();
	System.out.println("QingDebug: ComputingOptPaths");
        for (int dest = 0; dest < n; dest++)
        {
 	    System.out.println("\tQingDebug: ComputingOptPaths for dest = " + dest);
            optimalPathsWDest.add(optAlgrorithmFinite(dest));
 	    System.out.println("\tQingDebug: ComputingOptPaths for dest = " + dest + " DONE");
	}
	System.out.println("QingDebug: ComputingOptPaths DONE");

	//System.out.println("QingDebug: Computing ProbAvail_randomscan DONE");
        // END THE QING CODE

        for ( int vehs = 0; vehs < numVehs.length; vehs++ )
        {
            numVeh = numVehs[vehs];
	    runningOptPaths = new ArrayList<ArrayList<String>>(); // Qing variable
	    runningOptPathsPM = new ArrayList<ArrayList<Integer>>();
            destinations = new int[numVeh]; // Qing variable
	    
            for ( int ts = 0; ts < Ts.length; ts++ )
	    {
                T = Ts[ts];
                //secondsPastEmpty = T*60;
                currentDelay = new double[numVeh];

                for ( int pens = 0; pens < penetrations.length; pens++ )
                {
                    penetration = penetrations[pens];
                    //System.out.println("Penetration Ratio is " + penetration + ".");

                    timeToParkGreedy = 0.0;
                    timeToParkGPA_alldirs = 0.0;
                    timeToParkGreedy_allData = 0.0;
                    timeToParkGPA_alldirs_allData = 0.0;
                    //timeToParkAVP_lambda3 = 0.0;
                    //timeToParkAVP_lambda12 = 0.0;
                    timeToParkGPA_onlyAv = 0.0;
                    timeToParkGPA_onlyAv_withAging = 0.0;
                    timeToParkGreedy_onlyAv = 0.0;
                    timeToParkRandomWalk = 0.0;
                    timeToParkSysOpt = 0.0;
                    timeToParkEasyNE = 0.0;
		    timeToParkJosse = 0.0;
		    timeToParkGuo = 0.0;
                    
                    for (int l = 0; l < (int)(Math.ceil((double)numSims/(double)times.length)); l++ )
    	            {
                        for ( int i = 0; i < times.length; i++ )
	                {
                            Timestamp initialTime = times[i];
                            groundTruthData.initiateTheParkingMap(initialTime);
			    
			    for ( int penTimes = 0; penTimes < numRepeatPenetrationTests; penTimes++ )
		            {
                                runningOptPaths.clear();  // Qing variable
				runningOptPathsPM.clear();
                                userViewOfData.initiateThePenetratedProfile(penetration,initialTime);
                                SFParkLocation initialLocations[] = generateInitialLocations();
                                for ( int j = 0; j < parkingChanges.length; j++ )
    	                        {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
		                    {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                        {
                                    currentDelay[j] = 0.0;
                                }
                                groundTruthData.restartTheParkingMap();
                                //System.out.print("\tRunning Greedy, ");
                                //timeToParkGreedy += runSim(GREEDY,initialLocations,initialTime);
                                //System.out.println(" and now total is " + timeToParkGreedy);
			                    //System.out.println("--------------------------------------");
                                // Restart the parkingData
                                groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                        {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
    		                    {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                        {
                                    currentDelay[j] = 0.0;
                                }
                                /*// Run algorithm 2.
                                //System.out.print("Running GPA, ");
                                timeToParkGPA_alldirs += runSim(DM_GPA,initialLocations,initialTime);

                                groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                            {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
    		                        {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                            {
                                    currentDelay[j] = 0.0;
                                }
                                // Run algorithm 3.
                                //System.out.print("Running Greedy_allData, ");
                                timeToParkGreedy_allData += runSim(GREEDY_ALL_DATA,initialLocations,initialTime);

                                groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                            {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
    		                        {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                            {
                                    currentDelay[j] = 0.0;
                                }
                                // Run algorithm 4.
                                //System.out.print("Running GPA_allData, ");
                                timeToParkGPA_alldirs_allData += runSim(DM_GPA_ALL_DATA,initialLocations,initialTime);

                                groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                            {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
    		                        {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                            {
                                    currentDelay[j] = 0.0;
                                }
                                // Run algorithm 5.
                                //System.out.print("Running DM_GPA_ONLY_AV, ");
                                timeToParkGPA_onlyAv += runSim(DM_GPA_ONLY_AV,initialLocations,initialTime);

                                groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
  	                            {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
		                            {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                            {
                                    currentDelay[j] = 0.0;
                                }
                                // Run algorithm 6.
                                //System.out.println("Running DM_GPA_ONLY_AV_WITH_AGING");
                                timeToParkGPA_onlyAv_withAging += runSim(DM_GPA_ONLY_AV_WITH_AGING,initialLocations,initialTime);


                                groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                            {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
		                            {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                            {
                                    currentDelay[j] = 0.0;
                                }
                                // Run algorithm 7.
                                //System.out.println("Running GREEDY_ONLY_AV");
                                timeToParkGreedy_onlyAv += runSim(GREEDY_ONLY_AV,initialLocations,initialTime);

                                groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                            {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
		                            {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                            {
                                    currentDelay[j] = 0.0;
                                }
                                // Run algorithm 10.
                                //System.out.println("Running RANDOM_WALK");
                                timeToParkRandomWalk += runSim(RANDOM_WALK,initialLocations,initialTime);
                                */
                                groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                        {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
		                    {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                        {
                                    currentDelay[j] = 0.0;
                                }
                                //runningOptPaths = new ArrayList<Integer>[numVeh];
                                //for ( int j = 0; j < currentDelay.length; j++ )
	                            //{
                                    //runningOptPaths[j] = new ArrayList<Integer>();
                                //}                                
                                // Run algorithm 10.
                                //System.out.println("Running SYSTEM_OPT");
                                //timeToParkSysOpt += (systemOptimalTotalCost(initialLocations,initialTime) / velocity);
                                //timeToParkEasyNE += (easyNETotalCost(initialLocations,initialTime) / velocity);
                                //System.out.println(timeToParkSysOpt);
                                //runSim(SYSTEM_OPT,initialLocations,initialTime);

				
				groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                        {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
		                    {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                        {
                                    currentDelay[j] = 0.0;
                                }
                                // Run algorithm Josse.
                                //System.out.println("Running JOSSE");
                                timeToParkJosse += runSim(JOSSE,initialLocations,initialTime);

				// This is the formulation implemented by Qing of the PM algorithm
				// Run algorithm for Josse
				//System.out.println("\tQingDebug: ComputingOptPaths for PM algorithm");
			        //optimalPathsPM = probMaxAlgFinite(initialTime); // This is for the Josse algorithm
  	                        //System.out.println("\tQingDebug: ComputingOptPaths for PM algorithm DONE");

				//for (int vId = 0; vId < numVeh; vId++)
				//{
				//runningOptPathsPM.add(new ArrayList<Integer>(optimalPathsPM.get(initialLocations[vId].getDirection())));
				//}
				//timeToParkJosse += runSim(JOSSE,initialLocations,initialTime);

				groundTruthData.restartTheParkingMap();
                                // Restart the Penetrated Parking Map
                                userViewOfData.restartTheParkingMap();
                                // Restart the data that needs to be restarted
                                for ( int j = 0; j < parkingChanges.length; j++ )
	                        {
                                    for ( int k = 0; k < parkingChanges[j].length; k++ )
		                    {
                                        parkingChanges[j][k] = 0;
                                    }
                                }
                                for ( int j = 0; j < currentDelay.length; j++ )
	                        {
                                    currentDelay[j] = 0.0;
                                }
				
                                // Run algorithm Guo.
                                //System.out.println("Running Guo");
				for (int vId = 0; vId < numVeh; vId++)
				{
                                    destinations[vId] = initialLocations[vId].getDirection();
				    //runningOptPaths.add(new ArrayList<String>());
				}
				for (int vId = 0; vId < numVeh; vId++)
				{
                                    runningOptPaths.add(new ArrayList<String>(optimalPathsWDest.get(destinations[vId]).get(initialLocations[vId].getDirection())));
				}
				timeToParkGuo += runSim(GUO,initialLocations,initialTime);
                            }
		        }
                    }

                    timeToParkGreedy = timeToParkGreedy / numVeh;
                    timeToParkGPA_alldirs = timeToParkGPA_alldirs / numVeh;
                    timeToParkGreedy_allData = timeToParkGreedy_allData / numVeh;
                    timeToParkGPA_alldirs_allData = timeToParkGPA_alldirs_allData / numVeh;
                    //timeToParkAVP_lambda3 = timeToParkAVP_lambda3 / numVeh;
                    //timeToParkAVP_lambda12 = timeToParkAVP_lambda12 / numVeh;
                    timeToParkGPA_onlyAv = timeToParkGPA_onlyAv / numVeh;
                    timeToParkGPA_onlyAv_withAging = timeToParkGPA_onlyAv_withAging / numVeh;
                    timeToParkGreedy_onlyAv = timeToParkGreedy_onlyAv / numVeh;
                    timeToParkRandomWalk = timeToParkRandomWalk / numVeh;
                    timeToParkSysOpt = timeToParkSysOpt / numVeh;
                    timeToParkEasyNE = timeToParkEasyNE / numVeh;
		    timeToParkJosse = timeToParkJosse / numVeh;
		    timeToParkGuo = timeToParkGuo / numVeh;
                
                    System.out.println(penetration + "\t" + numVeh + "\t" + T + "\t" + 
                                       (timeToParkGreedy/totalSims) + "\t" + 
                		       (timeToParkGPA_alldirs/totalSims) + "\t" + 
                                       (timeToParkGreedy_allData/totalSims) + "\t" + 
                		       (timeToParkGPA_alldirs_allData/totalSims) + "\t" + 
                                       (timeToParkGPA_onlyAv/totalSims) + "\t" + 
                		       (timeToParkGPA_onlyAv_withAging/totalSims) + "\t" + 
                                       (timeToParkGreedy_onlyAv/totalSims) + "\t" + 
                	    	       (timeToParkRandomWalk/totalSims) + "\t" +
                                       (timeToParkSysOpt/totalSims) + "\t" +
                	    	       (timeToParkEasyNE/totalSims) + "\t" +
				       (timeToParkJosse/totalSims) + "\t" +
			               (timeToParkGuo/totalSims));
                }
            }
        }
    }

    public SFParkLocation[] generateInitialLocations()
    {
        SFParkLocation initLocs[] = new SFParkLocation[numVeh];
        for ( int i = 0; i < initLocs.length; i++ )
	    {
            initLocs[i] = generateRandomLocation();
        }
        return initLocs;
    }

    public SFParkLocation generateRandomLocation()
    {
        int newEdge = rEdge.nextInt(edgeList.length);
        SFParkEdge edge = edgeList[newEdge];

        int node1 = edge.getNode1().getId()-7001;
        int node2 = edge.getNode2().getId()-7001;
        //System.out.println("Generating a new location at edge #" + newEdge + ". NODE1 = " + node1 + ", NODE2 = " + node2 + ".");

        int newDir = rDirection.nextInt(2);
        if ( newDir == 0 )
	{
            newDir = node1;
        }
        else
	{
            newDir = node2;
        }

        double dist = edgeWeights[node1][node2];
        double newOffset = rOffset.nextDouble()*dist;
    
        return (new SFParkLocation(edge,newDir,newOffset));
    }

    public boolean willPark(SFParkEdge e, Timestamp t)
    {
        int nBlocks = e.getNBlocks();
        int node1 = e.getNode1().getId()-7001;
        int node2 = e.getNode2().getId()-7001;

        if ( nBlocks == 0 )
        {
            if (debugGreedyMovement)
	    {
                System.out.println("\t\tAvailability is 0, block has no parking zone");
            }

            return false;
        }
        else if ( nBlocks == 1 )
	{
            int blockId = e.getBlockId1();
            int availability = groundTruthData.getAvailability(blockId) + parkingChanges[node1][node2];// + (map.get(blockId)[0]-initMap.get(blockId)[0]); 

            if (debugGreedyMovement)
	    {
                System.out.println("\t\tAvailability is " + availability + ", blockId is: " + blockId + ", and time is:" + t);
            }

            if ( availability > 0 )
	    {
                return true;
            }
            else
	    {
                return false;
            }
        }
        else // nBlocks == 2
        { 
            int blockId1 = e.getBlockId1();
            int blockId2 = e.getBlockId2();
            int availability = groundTruthData.getAvailability(blockId1) + groundTruthData.getAvailability(blockId2) + parkingChanges[node1][node2]; 

            if (debugGreedyMovement)
	    {
                System.out.println("\t\tAvailability is " + availability + ", blockId is: " + blockId1 + ", and time is:" + t);
            }

            //System.out.println("Block id's are: " + blockId1 + " and " + blockId2 + ". Current availability is " + availability );
            if ( availability > 0 )
	    {
                return true;
            }
            else
	    {
                return false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    public Timestamp getTimestampFromText(String timeText) 
    {
        timeText = timeText.replace(" ", ":").replaceAll("-", ":");

        return new Timestamp(Integer.parseInt(timeText.split(":")[0])-1900,
                             Integer.parseInt(timeText.split(":")[1])-1,
                             Integer.parseInt(timeText.split(":")[2]), 
                             Integer.parseInt(timeText.split(":")[3]), 
                             Integer.parseInt(timeText.split(":")[4]), 
                             Integer.parseInt(timeText.split(":")[5]),0);
    }

    public int totalParking(int blockId)
    {
        int nodesInCBlock[] = blockIdMap.get(blockId);
        SFParkEdge e = edges[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001];
        int totalAv = 0;
        if ( e.getBlockId1() == blockId )
	{
            totalAv = e.getTotal1();
        }
        else if ( e.getBlockId2() == blockId )
	{
            totalAv = e.getTotal2();
        }
        else
	{
            System.out.println("Big Mistake in totalParking.  BlockId is unrecognized based on edges mapping.");
            System.exit(-1);
        }

        return totalAv;
    }

    public int DM_GPA(HashMap<Integer,Double> gs)
    {
        // gs will have the magnitudes of the direction vectors.  This function will return the largest one.

        Object keys[] = gs.keySet().toArray();

        double currentMax = -1.0;
        int maxNode = -1;

        for (int i = 0; i < keys.length; i++ )
	{
            double currentMagnitude = gs.get((Integer)keys[i]).doubleValue();
            if ( currentMax < currentMagnitude )
	    {
                currentMax = currentMagnitude;
                maxNode = (Integer)keys[i];
            }
        }

        return maxNode;
    }

    public int getFarthestNodeInBlock(int blockId, int currentNode)
    {
        // Return given the blockId the closest node of the block to go to from the current node.

        double dist1,dist2;
        int nodes[] = blockIdMap.get(blockId);
        dist1 = SP[currentNode][nodes[0]-7001];
        dist2 = SP[currentNode][nodes[1]-7001];

        if ( dist1 > dist2 )
	{
            return nodes[0]-7001;
        }
        else
	{
            return nodes[1]-7001;
        }
    }

    public int getClosestNodeInBlock(int blockId, int currentNode)
    {
        // Return given the blockId the closest node of the block to go to from the current node.

        double dist1,dist2;
        int nodes[] = blockIdMap.get(blockId);
        dist1 = SP[currentNode][nodes[0]-7001];
        dist2 = SP[currentNode][nodes[1]-7001];

        if ( dist1 < dist2 )
	{
            return nodes[0]-7001;
        }
        else
	{
            return nodes[1]-7001;
        }
    }

    public Timestamp addTimes(Timestamp t, double s)
    {
        return new Timestamp(t.getTime() + ((long)(s*1000)));
    }

    public void createBlockIdMap()
    {
        // This function creates the HashMap that given a blockId will give its connecting nodes. Also
        // will create a function of the blockId's in the array blockIds.

        FileReader fr = null;// = new FileReader();
        BufferedReader br;//= new BufferedReader();
    
        try
	{
            fr = new FileReader(edgesFile);
        }
        catch (Exception e)
	{
            e.printStackTrace();
        }

        br = new BufferedReader(fr);

        for ( int i = 0; i < nBlocks; i++ )
	{
            String line = "";
            try
	    {
                line = br.readLine();
            }
            catch (Exception e)
	    {
                e.printStackTrace();
            }

            String fileData[] = line.split(",");
            int blockId = Integer.parseInt(fileData[0]);
            int nodeId1 = Integer.parseInt(fileData[6]);
            int nodeId2 = Integer.parseInt(fileData[7]);

            int connectingNodes[] = new int[2];
            connectingNodes[0] = nodeId1;
            connectingNodes[1] = nodeId2;

            blockIds[i] = blockId;
            blockIdMap.put(blockId,connectingNodes);
        }
    }

    //@SuppressWarnings("deprecation")
    public boolean secondsHavePassed(int seconds, Timestamp pastTime, Timestamp currentTime)
    {
        double secondsInInactivityInterval = seconds;
        Timestamp inactivityPlusLast = addTimes(pastTime,secondsInInactivityInterval);
        return currentTime.after(inactivityPlusLast); 
    }


    public void outputVis(int id,SFParkLocation loc)
    {
        SFParkEdge e = loc.getEdge();
        int direction = loc.getDirection();
        double offset = loc.getOffset();

        SFParkNode node1 = e.getNode1();
        SFParkNode node2 = e.getNode2();
        int node1Id = node1.getId()-7001;
        int node2Id = node2.getId()-7001;

        SFParkNode start = null;
        SFParkNode end = null;
        int startId = -1;
        int endId = -1;

        if (node1Id == direction)
	{
            start = node2;
            startId = node2Id;
            end = node1;
            endId = node1Id;
        }
        else if (node2Id == direction)
	{
            start = node1;
            startId = node1Id;
            end = node2;
            endId = node2Id;
        }
        else
	{
            System.out.println("ERROR!!");
            System.exit(-1);
        }

        double startLat = start.getLatitude();
        double startLon = start.getLongitude();
        double endLat = end.getLatitude();
        double endLon = end.getLongitude();
        double dist = edgeWeights[startId][endId];
        
        double ratio = offset/dist;

        double lat = startLat + (endLat-startLat)*ratio;
        double lon = startLon + (endLon-startLon)*ratio;
        System.out.println(id+","+lat+","+lon);
    }
 
    public void readTimes()
    {
        try
	{
            FileReader fr = new FileReader(timeFile);
            BufferedReader br = new BufferedReader(fr);
        
            String line = br.readLine();
            int i = 0;
            while ((line != null) && (i < numTimes))
  	    {
                times[i] = getTimestampFromText(line);
                //System.out.println(times[i]);
                i++;
                line = br.readLine(); 
            }
        }
        catch ( Exception e )
	{
            e.printStackTrace();
        }
    }

    public double runSim(int algorithm, SFParkLocation il[], Timestamp it)
    {
        int nParked = 0;
        boolean parked[] = new boolean[numVeh];
        currentLocations = new SFParkLocation[numVeh];        

        foundEmpty = new HashMap<Integer,Integer>();
        /*for ( int i = 0; i < foundEmpty.length; i++ )
	{
            foundEmpty[i] = new HashMap<Integer,Integer>();
        }*/

        foundEmptyTime = new HashMap<Integer,Timestamp>();
        foundEmptyTimeT = new HashMap<Integer,Timestamp>();
        /*for ( int i = 0; i < foundEmptyTime.length; i++ )
	    {
            foundEmptyTime[i] = new HashMap<Integer,Timestamp>();  // Time that a given block was found empty.
        }*/

        for ( int i = 0; i < currentLocations.length; i++ )
	{
            currentLocations[i] = new SFParkLocation(il[i].getEdge(),il[i].getDirection(),il[i].getOffset());
        }

        Timestamp currentTime = new Timestamp(it.getTime());
        double timeToPark = 0.0;

	for ( int i = 0; i < parked.length; i++ )
	{
            parked[i] = false;
        }

        while (nParked < numVeh)
	{
            int closestVeh = -1;
            double closestDistance = 99999999.0;
            boolean movingTowardsParking = false;

            //System.out.println("nParked = " + nParked + ".");
            for ( int i = 0; i < numVeh; i++ )
	    {
                if ( !parked[i] )
		{
                    // Check if offset is past the midpoint of the block.  If so then he will just travel to next node.
                    SFParkEdge currentEdge = currentLocations[i].getEdge();
                    //int nextNode = currentLocations[i].getDirection();
                    double offset = currentLocations[i].getOffset();
                    //int nodesInCurrentBlock[] = blockIdMap.get(currentBlock);
                    int node1 = currentEdge.getNode1().getId();
                    int node2 = currentEdge.getNode2().getId();
                    double blockDistance = edgeWeights[node1-7001][node2-7001];
                    //System.out.println("blockDistance = " + blockDistance + " and offset = " + offset);

                    if ( offset > (blockDistance/2.0) )
    		    {
                        // Car is moving towards the next node.
                        double distanceToNextNode = blockDistance-offset+currentDelay[i];
                        if ( distanceToNextNode < closestDistance )
		        {
                            movingTowardsParking = false;
                            closestVeh = i;
                            closestDistance = distanceToNextNode;
                        }
                    }
                    else
		    {
                        // Car is moving towards potential parking (mid-point of the block)
                        
                        double distanceToParking = (blockDistance/2.0)-offset+currentDelay[i];
                        if ( distanceToParking < closestDistance )
		        {
                            movingTowardsParking = true;
                            closestVeh = i;
                            closestDistance = distanceToParking;
                        }
                    }
                }
            }

            //System.out.println("closestDistance = " + closestDistance + "m");
            currentTime = addTimes(currentTime,closestDistance/velocity);
            groundTruthData.advanceToTime(currentTime);
            userViewOfData.advanceToTime(currentTime);
            //System.out.println(currentTime);

            for ( int i = 0; i < numVeh; i++ )
	    {
                // For each car, will move them along closestDistance units. 
                // If it's the closest vehicle, will try to park him or get him to the next node.

                if (!parked[i])
		{
                    SFParkEdge currentEdge = currentLocations[i].getEdge();
                    //int nextNode = currentLocations[i].getDirection();
                    double offset = currentLocations[i].getOffset();
                    //int nodesInCurrentBlock[] = blockIdMap.get(currentBlock);
                    //nodesInCurrentBlock[0] = nodesInCurrentBlock[0]-7001;
                    //nodesInCurrentBlock[1] = nodesInCurrentBlock[1]-7001;
                    int node1 = currentEdge.getNode1().getId()-7001;
                    int node2 = currentEdge.getNode2().getId()-7001;

                    if ( closestVeh == i )
		    {
                        if ( movingTowardsParking )
			{
                            // Move vehicle to midpoint of the block.  If there is parking, park.  Otherwise do nothing else.
                            currentLocations[i].setOffset(offset+closestDistance);
                            boolean willPark;
                            willPark = willPark(currentEdge,currentTime);

			                // He is the closest vehicle, so then the intersectionDelay can be reset because he will for sure get to the middle of the block.
                            currentDelay[i] = 0.0;

                            if (willPark)
	                    {
                                //System.out.println("Greedy is adding " + (sp_length/velocity));
                                nParked++;
                                parked[i] = true;
                                timeToPark += (((double)currentTime.getTime()-(double)it.getTime())/1000.0); // Time to park in seconds
                                //System.out.println("Just added " + (((double)currentTime.getTime()-(double)it.getTime())/1000.0) + " to the total." );
                                parkingChanges[node1][node2] = parkingChanges[node1][node2]-1;
                                parkingChanges[node2][node1] = parkingChanges[node2][node1]-1;
                                if ( visOutput )
			        {
                                    outputVis(i,currentLocations[i]);
                                }
                                //System.out.println(" " + nParked);
                                if (debugGreedyMovement)
				{
                                    System.out.println("Greedy Parked vehicle #" + i + ". Added " + (((double)currentTime.getTime()-(double)it.getTime())/1000.0) );
                                }
                            }
                            else
                	    {
                                if ( debugGreedyMovement )
				{
                                    System.out.println("HELLO!!! No parking available!!");
                                    System.out.println("NO PARKING AVAILABLE. Veh #" + i + " was the closest.");
                                }
                                currentLocations[i].setOffset(offset+closestDistance+0.0000001);

                                // Update foundEmpty map for this vehicle
                                int nBlocks = currentEdge.getNBlocks();
                                if ( nBlocks > 0 )
				{
                                    // Reporting was erroneous for this block and so we need to save this info.

                                    int blockId = currentEdge.getBlockId1();
                                    int currAv = (int)groundTruthData.getAvailability(blockId);
                                    int blockId2 = -1;

                                    if ( nBlocks == 2 )
				    {
                                        blockId2 = currentEdge.getBlockId2();
                                        //currAv += (int)currMap.get(blockId2)[0];
                                        foundEmpty.put(new Integer(blockId2), new Integer(groundTruthData.getAvailability(blockId2)));
                                        foundEmptyTime.put(new Integer(blockId2), currentTime);
                                        foundEmptyTimeT.put(blockId2, currentTime);
                                    }
                                    foundEmpty.put(new Integer(blockId), new Integer(currAv));
				    foundEmptyTime.put(new Integer(blockId), currentTime);
				    foundEmptyTimeT.put(blockId,currentTime);
                                }

                                if ( visOutput )
	    		        {
                                    outputVis(i,currentLocations[i]);
                                }
                            }
                        }
                        else
			{
                            //****** Moving towards node, move the vehicle to the node and choose the next block to take with offset 0.
                            currentLocations[i].setOffset(0.0);
                            int currentNode = currentLocations[i].getDirection();

                            int newDirection = -1;
                            switch(algorithm)
			    {
			        case GREEDY:
                                    newDirection = greedyAlgorithm(i,currentTime);
                                    break;
			        case DM_GPA:
                                    newDirection = DM_GPA_Algorithm(i,currentTime);
                                    break;
                                case GREEDY_ALL_DATA:
                                    newDirection = greedyAllDataAlgorithm(i,currentTime);
                                    break;
                                case DM_GPA_ALL_DATA:
                                    newDirection = DM_GPA_allDataAlgorithm(i,currentTime);
                                    break;
                                case AVP_LAMBDA_3:
                                    newDirection = AVP_algorithm(i,currentTime,3.0);
                                    break;
                                case AVP_LAMBDA_12:
                                    newDirection = AVP_algorithm(i,currentTime,12.0);
                                    break;
			        case DM_GPA_ONLY_AV:
                                    newDirection = DM_GPA_OnlyAv_Algorithm(i,currentTime);
                                    break;
			        case DM_GPA_ONLY_AV_WITH_AGING:
                                    newDirection = DM_GPA_OnlyAv_WithAging_Algorithm(i,currentTime);
                                    break;
			        case GREEDY_ONLY_AV:
                                    newDirection = greedy_OnlyAv_Algorithm(i,currentTime);
                                    break;
			        case RANDOM_WALK:
			            newDirection = randomWalk_algorithm(i,currentTime);
			            break;
			        case JOSSE:
				    newDirection = Josse_algorithm(i,currentTime);
				    break;
			        case GUO:
				    String theEntry[] = optAlgorithmSingleStep(i).split(",");
				    newDirection = Integer.parseInt(theEntry[0]);
				    if (newDirection == EndOfSearch)
				    {
					// We have traversed over k_m edges so recompute a new path.
			                runningOptPaths.set(i,new ArrayList<String>(optimalPathsWDest.get(destinations[i]).get(currentLocations[i].getDirection())));
					theEntry = optAlgorithmSingleStep(i).split(",");
                                        newDirection = Integer.parseInt(theEntry[0]);//randomWalk_algorithm(i,currentTime);
				    }
				    //costOfContinueSearch = Double.parseDouble(theEntry[1]);
				    break;
			        default:
                                    System.out.println("Undefined algorithm in function runSim.");
                                    System.exit(-1);
                            }

                            currentLocations[i].setEdge(edges[currentNode][newDirection]);
                            currentLocations[i].setDirection(newDirection);

  			    // He is the closest vehicle, he will get to the intersection.  Then he has to wait in the intersection.
                            currentDelay[i] = intersectionDelayInMeters;

                            if ( visOutput )
			                {
                                outputVis(i,currentLocations[i]);
                            }
                        }
                    }
                    else
		    {
                        // He is not the closest vehicle, so then change the offset closestDistance units.
                        //double currentOffset = currentLocations[i].getOffset();
                        if ( closestDistance <= currentDelay[i] )
			{
                            // He can just wait at the intersection.
                            currentDelay[i] = currentDelay[i]-closestDistance;
                        }
                        else
			{
                            // He will move ahead and maybe wait whatever is left at the intersection.
                            currentLocations[i].setOffset(offset+closestDistance-currentDelay[i]);
                            currentDelay[i] = 0.0;
                        }

                        if ( visOutput )
			{
                            outputVis(i,currentLocations[i]);
                        }
                    }
                }
            }
            //System.out.println("------------------------------------");
        }

        return timeToPark;
    }

    public int greedyAlgorithm(int vehId, Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();

        //int closestNodeToVisit = -1;
        double sp_length = Double.MAX_VALUE;
        double sp_metric = Double.MAX_VALUE;
        int blockToVisit = -1;
        //double halfBlockDist = -1.0;
        int spDir = -1;

        for ( int j = 0; j < nBlocks; j++ )
    	{
            int cBlock = blockIds[j];
            int reportedAvailability = userViewOfData.getAvailability(cBlock,T,foundEmptyTimeT);
            int closestNodeInBlock = getClosestNodeInBlock(cBlock,currentNode);  // Decide whether to go to node1 or node2 of cBlock.
            int nodesInCBlock[] = blockIdMap.get(cBlock);
            //availability += parkingChanges[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]; //SHOULD USERS KNOW OF NEW PARKING UPDATES???
            // Probably updates should be based on penetration ratio'

            if ( foundEmpty.containsKey(cBlock) )
	    {
                Timestamp pastTime = (Timestamp)(foundEmptyTime.get(cBlock));
                if ( secondsHavePassed(secondsPastEmpty,pastTime,t) )
                {
                    // Remove block from the foundEmpty maps
                    foundEmpty.remove(cBlock);
                    foundEmptyTime.remove(cBlock);
                }
                else
                {
                    reportedAvailability = 0;
                }
            }
            double dist = SP[currentNode][closestNodeInBlock]+(edgeWeights[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]/2);
            double metric = dist;// /reportedAvailability;                            

            if ((metric < sp_metric) && (reportedAvailability > 0))
            {
                sp_length = dist;
                //closestNodeToVisit = closestNodeInBlock;
                blockToVisit = cBlock;
                sp_metric = metric;
                //halfBlockDist = edgeWeights[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]/2;
                spDir = SP_direction[currentNode][closestNodeInBlock];
            }
        }

        // Set newDirection to spDir and set the newEdge.
        if ( currentNode == spDir )
        {
            spDir = getFarthestNodeInBlock(blockToVisit,currentNode);
        }

        if (spDir == -1)
        {
        	//System.out.println("WHAAATTT!!!! There are no available spots!!!!\n");
        	return this.randomWalk_algorithm(vehId, t);
        }
        return spDir;
    }

    public int randomWalk_algorithm(int vehId, Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();
        ArrayList<Integer> adjNodesR = adjacentNodes(currentNode);
        int randomIndex = (int)Math.ceil(adjNodesR.size()*Math.random())-1;
        return adjNodesR.get(randomIndex);  
    }

    /*public int randomWalkNoReturn_algorithm(int vehId, Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();
        SFParkEdge e = currentLocations[vehId].getEdge();
        int node1 = e.getNode1().getId() - 7001;
        int node2 = e.getNode2().getId() - 7001;
       
        ArrayList<Integer> adjNodesR = new ArrayList<Integer>();
        for ( int j = 0; j < n; j++ )
        {
            if ( (node1 != j) && (node2 != j) )  // This is done to check that he doesn't return to the previous edge.
	    {
                if ( edges[currentNode][j] != null )
                {
                    // Then j is adjacent to the currentLocation
                    adjNodesR.add(new Integer(j));
                }
            }
        }

        if ( adjNodesR.size() == 0 ) // Just in case adjNodesR is empty
	{
            return randomWalk_algorithm(vehId, t);
        }

        double randomStep = 1.0 / adjNodesR.size();
        double r = Math.random();

        int i;
        for ( i = 0; i < adjNodesR.size()-1; i++ )
	{
            if ( r <= (i+1)*randomStep )
	    {
                return adjNodesR.get(i).intValue(); 
            }
        }

        return adjNodesR.get(i).intValue();
    }*/
    
    public int DM_GPA_Algorithm(int vehId,Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();

        HashMap<Integer,Double> gVectors = new HashMap<Integer,Double>();
        for ( int j = 0; j < n; j++ )
        {
            if ( edges[currentNode][j] != null )
            {
                // Then j is adjacent to the currentLocation
                gVectors.put(new Integer(j),new Double(0.0));
            }
        }

        for ( int j = 0; j < nBlocks; j++ )
        {
            // Compute gravity value for this block.
            int currentBlock = blockIds[j];
            int availability = userViewOfData.getAvailability(currentBlock,T,foundEmptyTimeT);
            int closestNodeInBlock = getClosestNodeInBlock(currentBlock,currentNode);  // Decide whether to go to node1 or node2 of currentBlock.
            int nodesInCurrentBlock[] = blockIdMap.get(currentBlock);
            availability += (parkingChanges[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]);
            //availability += (currMap.get(currentBlock)[0]-initMap.get(currentBlock)[0]);
            //double avProb = 0.0; // THIS SHOULD BE FIXED TO COMPUTE THE NEW TYPE OF PROBABILITY per blockId and per currentTime!!!!!!!
            if ( foundEmpty.containsKey(currentBlock) )
            {
                Timestamp pastTime = (Timestamp)(foundEmptyTime.get(currentBlock));
                if ( secondsHavePassed(secondsPastEmpty,pastTime,t) )
                {
                    // Remove block from the foundEmpty maps
                    foundEmpty.remove(currentBlock);
                    foundEmptyTime.remove(currentBlock);
                }
                else
                {
                    /*if ( (availability - (parkingChanges[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001])) <= 
                                                            ((HashMap<Integer,Integer>)foundEmpty[i]).get(currentBlock).intValue() )
                    {
                        avProb = 0.0;
                    }*/
                    availability = 0;
                }
            }
            if ( availability < 0 )
            {
                availability = 0;
            }

            Object keys[] = gVectors.keySet().toArray();
            for (int k = 0; k < keys.length; k++ )
            {
                int currentKey = ((Integer)keys[k]).intValue(); // Current key is one of the directions to consider.
                double dist;// = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                double g;// = availability / (dist*dist);
                int directionToTake;
                if ( currentNode != closestNodeInBlock )
                {
                    double adjBlockDist = edgeWeights[currentNode][currentKey];
                    double pathDist = SP[currentKey][closestNodeInBlock];
                    dist = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                }
                else
                {
                    // This means that there's availability in an incident block to the currentLocation (node).
                    int farthestNodeInBlock = getFarthestNodeInBlock(currentBlock,currentNode);
                    if ( farthestNodeInBlock == currentKey )
                    {
                        // He will just move to half the block and try to park.
                        dist = (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                    }
                    else
                    {
                        double adjBlockDist = edgeWeights[currentNode][currentKey];
                        double pathDist = SP[currentKey][closestNodeInBlock];
                        dist = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                    }
                }
 
                g = (availability) / (dist*dist);
                directionToTake = currentKey; //SP_direction[currentNode][closestNodeInBlock];

                double previousG = ((Double)gVectors.get(directionToTake)).doubleValue();
                double newG = previousG + g;
                gVectors.put(new Integer(directionToTake),new Double(newG));
            }
        }

        // Now all gVectors are set.  Then choose the highest one (DM-GPA).
        return DM_GPA(gVectors);
         
    }

    public int greedyAllDataAlgorithm(int vehId, Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();

        //int closestNodeToVisit = -1;
        double sp_length = 999999999.0;
        double sp_metric = 999999999.0;
        int blockToVisit = -1;
        //double halfBlockDist = -1.0;
        int spDir = -1;

        for ( int j = 0; j < nBlocks; j++ )
    	{
            int cBlock = blockIds[j];
            int reportedAvailability = groundTruthData.getAvailability(cBlock);
            int closestNodeInBlock = getClosestNodeInBlock(cBlock,currentNode);  // Decide whether to go to node1 or node2 of cBlock.
            int nodesInCBlock[] = blockIdMap.get(cBlock);
            //availability += parkingChanges[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]; //SHOULD USERS KNOW OF NEW PARKING UPDATES???
            // Probably updates should be based on penetration ratio'

            if ( foundEmpty.containsKey(cBlock) )
	    {
                Timestamp pastTime = (Timestamp)(foundEmptyTime.get(cBlock));
                if ( secondsHavePassed(secondsPastEmpty,pastTime,t) )
                {
                    // Remove block from the foundEmpty maps
                    foundEmpty.remove(cBlock);
                    foundEmptyTime.remove(cBlock);
                }
                else
                {
                    reportedAvailability = 0;
                }
            }
            double dist = SP[currentNode][closestNodeInBlock]+(edgeWeights[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]/2);
            double metric = dist;// /reportedAvailability;                            

            if ((metric < sp_metric) && (reportedAvailability > 0))
            {
                sp_length = dist;
                //closestNodeToVisit = closestNodeInBlock;
                blockToVisit = cBlock;
                sp_metric = metric;
                //halfBlockDist = edgeWeights[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]/2;
                spDir = SP_direction[currentNode][closestNodeInBlock];
            }
        }

        // Set newDirection to spDir and set the newEdge.
        if ( currentNode == spDir )
        {
            spDir = getFarthestNodeInBlock(blockToVisit,currentNode);
        }

        return spDir;
    }

    public int DM_GPA_allDataAlgorithm(int vehId,Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();

        HashMap<Integer,Double> gVectors = new HashMap<Integer,Double>();
        for ( int j = 0; j < n; j++ )
        {
            if ( edges[currentNode][j] != null )
            {
                // Then j is adjacent to the currentLocation
                gVectors.put(new Integer(j),new Double(0.0));
            }
        }

        for ( int j = 0; j < nBlocks; j++ )
        {
            // Compute gravity value for this block.
            int currentBlock = blockIds[j];
            int availability = groundTruthData.getAvailability(currentBlock);
            int closestNodeInBlock = getClosestNodeInBlock(currentBlock,currentNode);  // Decide whether to go to node1 or node2 of currentBlock.
            int nodesInCurrentBlock[] = blockIdMap.get(currentBlock);
            availability += (parkingChanges[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]);
            //availability += (currMap.get(currentBlock)[0]-initMap.get(currentBlock)[0]);
            //double avProb = 0.0; // THIS SHOULD BE FIXED TO COMPUTE THE NEW TYPE OF PROBABILITY per blockId and per currentTime!!!!!!!
            if ( foundEmpty.containsKey(currentBlock) )
            {
                Timestamp pastTime = (Timestamp)(foundEmptyTime.get(currentBlock));
                if ( secondsHavePassed(secondsPastEmpty,pastTime,t) )
                {
                    // Remove block from the foundEmpty maps
                    foundEmpty.remove(currentBlock);
                    foundEmptyTime.remove(currentBlock);
                }
                else
                {
                    /*if ( (availability - (parkingChanges[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001])) <= 
                                                            ((HashMap<Integer,Integer>)foundEmpty[i]).get(currentBlock).intValue() )
                    {
                        avProb = 0.0;
                    }*/
                    availability = 0;
                }
            }
            if ( availability < 0 )
            {
                availability = 0;
            }

            Object keys[] = gVectors.keySet().toArray();
            for (int k = 0; k < keys.length; k++ )
            {
                int currentKey = ((Integer)keys[k]).intValue(); // Current key is one of the directions to consider.
                double dist;// = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                double g;// = availability / (dist*dist);
                int directionToTake;
                if ( currentNode != closestNodeInBlock )
                {
                    double adjBlockDist = edgeWeights[currentNode][currentKey];
                    double pathDist = SP[currentKey][closestNodeInBlock];
                    dist = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                }
                else
                {
                    // This means that there's availability in an incident block to the currentLocation (node).
                    int farthestNodeInBlock = getFarthestNodeInBlock(currentBlock,currentNode);
                    if ( farthestNodeInBlock == currentKey )
                    {
                        // He will just move to half the block and try to park.
                        dist = (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                    }
                    else
                    {
                        double adjBlockDist = edgeWeights[currentNode][currentKey];
                        double pathDist = SP[currentKey][closestNodeInBlock];
                        dist = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                    }
                }
 
                g = (availability) / (dist*dist);
                directionToTake = currentKey; //SP_direction[currentNode][closestNodeInBlock];

                double previousG = ((Double)gVectors.get(directionToTake)).doubleValue();
                double newG = previousG + g;
                gVectors.put(new Integer(directionToTake),new Double(newG));
            }
        }

        // Now all gVectors are set.  Then choose the highest one (DM-GPA).
        return DM_GPA(gVectors);
         
    }

    public int AVP_algorithm(int vehId, Timestamp t, double lambda)
    {
        // This algorithm returns the next intersection to take according to the AVP algorithm (as specified
        // in the ACM GIS 2013 paper by Josse, Schubert, and Kriegel.

        int currentNode = currentLocations[vehId].getDirection();
        return AVP_computeNextNode(vehId,currentNode,t,lambda);
    }

    public int AVP_computeNextNode(int vehId, int startNode, Timestamp currentTime, double lambda)
    {
        // Will compute next node to take according to AVP probabilities (ACM GIS 2013 paper).

        ArrayList<Integer> adjNodesAVP = adjacentNodes(startNode);
        double bestP_vac = -2.0;
        int bestNode = -1;
        for ( int i = 0; i < adjNodesAVP.size(); i++ )
        {
            int adjNode = (int)adjNodesAVP.get(i);

            SFParkEdge currentEdge = edges[startNode][adjNode];
            int nBlocks = currentEdge.getNBlocks();
            double currentP_vac = -3.0;

            if ( nBlocks == 0 )
	    {
                currentP_vac = -1.0;
            }
            else if ( nBlocks == 1 )
	    {
                int blockId = currentEdge.getBlockId1();

                // Check if this block has been foundEmpty recently.
                if ( foundEmpty.containsKey(blockId) )
                {
                    Timestamp pastTime = (Timestamp)(foundEmptyTime.get(blockId));
                    if ( secondsHavePassed(secondsPastEmpty,pastTime,currentTime) )
                    {
                        // Remove block from the foundEmpty maps
                        foundEmpty.remove(blockId);
                        foundEmptyTime.remove(blockId);
                        currentP_vac = p_vac(vehId,startNode,adjNode,currentTime,lambda);
                    }
                    else
                    {
                        // This block was foundEmpty too recently.
                        currentP_vac = -1.5;
                    }
                }
                else
	     	{
                    currentP_vac = p_vac(vehId,startNode,adjNode,currentTime,lambda);
                }
            }
            else if (nBlocks == 2 )
	    {
                int blockId1 = currentEdge.getBlockId1();
                int blockId2 = currentEdge.getBlockId2();

                // Check if these blocks have been foundEmpty recently.
                if ( foundEmpty.containsKey(blockId1) )
                {
                    Timestamp pastTime = (Timestamp)(foundEmptyTime.get(blockId1));
                    if ( secondsHavePassed(secondsPastEmpty,pastTime,currentTime) )
                    {
                        // Remove block from the foundEmpty maps
                        foundEmpty.remove(blockId1);
                        foundEmptyTime.remove(blockId1);
                        foundEmpty.remove(blockId2);
                        foundEmptyTime.remove(blockId2);
                        currentP_vac = p_vac(vehId,startNode,adjNode,currentTime,lambda);
                    }
                    else
                    {
                        // This block was foundEmpty too recently.
                        currentP_vac = -1.5;
                    }
                }
                else
		{
                    currentP_vac = p_vac(vehId,startNode,adjNode,currentTime,lambda);
                }

            }
            else
	    {
                System.out.println("Number of edges is wrong in function computeNextNode.");
                System.exit(-1);
            }

            if ( currentP_vac >= bestP_vac )
	    {
                bestP_vac = currentP_vac;
                bestNode = adjNode;
            }

        }
        //System.out.println("Next node is " + bestNode);
        return bestNode;
    }

    public double p_vac(int vehId, int currentNode, int adjNode, Timestamp currentTime, double lambda)
    {
        // Computes the probability of vaccancy in the block, according to the method presented in ACM GIS 2013
        // paper by Josse, Schubert, and Kriegel.

        ArrayList<Timestamp> avReports;
        ArrayList<Timestamp> avReports2;
        avReports = new ArrayList<Timestamp>();

        SFParkEdge currentEdge = edges[currentNode][adjNode];
        int nBlocks = currentEdge.getNBlocks();
        if ( nBlocks == 0 )
	{
            return 0.0;
        }
        else if ( nBlocks == 1 )
	{
            int blockId = currentEdge.getBlockId1();
            avReports = (ArrayList<Timestamp>)(userViewOfData.getAvailabilityReports(blockId));
        }
        else if (nBlocks == 2 )
	{
            int blockId1 = currentEdge.getBlockId1();
            int blockId2 = currentEdge.getBlockId2();

            avReports = (ArrayList<Timestamp>)(userViewOfData.getAvailabilityReports(blockId1));
            avReports2 = (ArrayList<Timestamp>)(userViewOfData.getAvailabilityReports(blockId2));
            for ( int i = 0; i < ((ArrayList<Timestamp>)avReports2).size(); i++ )
	    {
                avReports.add((Timestamp)(avReports2.get(i)));
            }
        }
        else
	{
            System.out.println("Number of edges is wrong in function p_vac.");
            System.exit(-1);
        }

        double p_vacs[] = new double[((ArrayList<Timestamp>)avReports).size()];
        System.out.println("\tp_vacs.length == " + p_vacs.length );
        
        // Compute p_vac for each report.
        for ( int i = 0; i < p_vacs.length; i++ )
	{
            Timestamp t_v = (Timestamp)(avReports.get(i));
            double timeDiffInMinutes = (((double)(currentTime.getTime()-t_v.getTime()))/1000.0)/60.0;
            System.out.println("\t\t\tcurrentTime == " + currentTime);
            System.out.println("\t\t\tt_v == " + t_v);
            System.out.println("\t\ttimeDiffInMinutes == " + timeDiffInMinutes);
            double exponent = (-1)*lambda*(timeDiffInMinutes);
            System.out.println("\t\tp_vacs[i] == " + exponent);
            p_vacs[i] = Math.exp(exponent);
        }

        // Now we have a p_vac value for each parking report.  So we then combine them into one p_vac value.
        double probabilityOfZeroParking = 1;
        for ( int i = 0; i < p_vacs.length; i++ )
	{
            probabilityOfZeroParking = probabilityOfZeroParking * (1-p_vacs[i]);
        }

        System.out.println("Probality of parking is " + (1-probabilityOfZeroParking));
        return (1 - probabilityOfZeroParking);
    }

    public ArrayList<Integer> adjacentNodes( int node )
    {
        ArrayList<Integer> adjNodesAN = new ArrayList<Integer>();

        for ( int j = 0; j < n; j++ )
        {
            if ( edges[node][j] != null )
            {
                // Then j is adjacent to the node
                adjNodesAN.add(j);
            }
        }

        return adjNodesAN;
    }

    public int DM_GPA_OnlyAv_Algorithm(int vehId,Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();

        HashMap<Integer,Double> gVectors = new HashMap<Integer,Double>();
        for ( int j = 0; j < n; j++ )
        {
            if ( edges[currentNode][j] != null )
            {
                // Then j is adjacent to the currentLocation
                gVectors.put(new Integer(j),new Double(0.0));
            }
        }

        for ( int j = 0; j < nBlocks; j++ )
        {
            // Compute gravity value for this block.
            int currentBlock = blockIds[j];
            int availability = userViewOfData.getOnlyAvReportsTotal(currentBlock,T);
            int closestNodeInBlock = getClosestNodeInBlock(currentBlock,currentNode);  // Decide whether to go to node1 or node2 of currentBlock.
            int nodesInCurrentBlock[] = blockIdMap.get(currentBlock);
            availability += (parkingChanges[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]);
            //availability += (currMap.get(currentBlock)[0]-initMap.get(currentBlock)[0]);
            //double avProb = 0.0; // THIS SHOULD BE FIXED TO COMPUTE THE NEW TYPE OF PROBABILITY per blockId and per currentTime!!!!!!!
            if ( foundEmpty.containsKey(currentBlock) )
            {
                Timestamp pastTime = (Timestamp)(foundEmptyTime.get(currentBlock));
                if ( secondsHavePassed(secondsPastEmpty,pastTime,t) )
                {
                    // Remove block from the foundEmpty maps
                    foundEmpty.remove(currentBlock);
                    foundEmptyTime.remove(currentBlock);
                }
                else
                {
                    /*if ( (availability - (parkingChanges[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001])) <= 
                                                            ((HashMap<Integer,Integer>)foundEmpty[i]).get(currentBlock).intValue() )
                    {
                        avProb = 0.0;
                    }*/
                    availability = 0;
                }
            }
            if ( availability < 0 )
            {
                availability = 0;
            }

            Object keys[] = gVectors.keySet().toArray();
            for (int k = 0; k < keys.length; k++ )
            {
                int currentKey = ((Integer)keys[k]).intValue(); // Current key is one of the directions to consider.
                double dist;// = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                double g;// = availability / (dist*dist);
                int directionToTake;
                if ( currentNode != closestNodeInBlock )
                {
                    double adjBlockDist = edgeWeights[currentNode][currentKey];
                    double pathDist = SP[currentKey][closestNodeInBlock];
                    dist = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                }
                else
                {
                    // This means that there's availability in an incident block to the currentLocation (node).
                    int farthestNodeInBlock = getFarthestNodeInBlock(currentBlock,currentNode);
                    if ( farthestNodeInBlock == currentKey )
                    {
                        // He will just move to half the block and try to park.
                        dist = (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                    }
                    else
                    {
                        double adjBlockDist = edgeWeights[currentNode][currentKey];
                        double pathDist = SP[currentKey][closestNodeInBlock];
                        dist = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                    }
                }
 
                g = (availability) / (dist*dist);
                directionToTake = currentKey; //SP_direction[currentNode][closestNodeInBlock];

                double previousG = ((Double)gVectors.get(directionToTake)).doubleValue();
                double newG = previousG + g;
                gVectors.put(new Integer(directionToTake),new Double(newG));
            }
        }

        // Now all gVectors are set.  Then choose the highest one (DM-GPA).
        return DM_GPA(gVectors);
         
    }

    public int DM_GPA_OnlyAv_WithAging_Algorithm(int vehId,Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();

        HashMap<Integer,Double> gVectors = new HashMap<Integer,Double>();
        for ( int j = 0; j < n; j++ )
        {
            if ( edges[currentNode][j] != null )
            {
                // Then j is adjacent to the currentLocation
                gVectors.put(new Integer(j),new Double(0.0));
            }
        }

        for ( int j = 0; j < nBlocks; j++ )
        {
            // Compute gravity value for this block.
            int currentBlock = blockIds[j];
            double availability = userViewOfData.getAgingScoreOnlyAv(currentBlock,T);
            int closestNodeInBlock = getClosestNodeInBlock(currentBlock,currentNode);  // Decide whether to go to node1 or node2 of currentBlock.
            int nodesInCurrentBlock[] = blockIdMap.get(currentBlock);
            availability += (parkingChanges[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]);
            //availability += (currMap.get(currentBlock)[0]-initMap.get(currentBlock)[0]);
            //double avProb = 0.0; // THIS SHOULD BE FIXED TO COMPUTE THE NEW TYPE OF PROBABILITY per blockId and per currentTime!!!!!!!
            if ( foundEmpty.containsKey(currentBlock) )
            {
                Timestamp pastTime = (Timestamp)(foundEmptyTime.get(currentBlock));
                if ( secondsHavePassed(secondsPastEmpty,pastTime,t) )
                {
                    // Remove block from the foundEmpty maps
                    foundEmpty.remove(currentBlock);
                    foundEmptyTime.remove(currentBlock);
                }
                else
                {
                    /*if ( (availability - (parkingChanges[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001])) <= 
                                                            ((HashMap<Integer,Integer>)foundEmpty[i]).get(currentBlock).intValue() )
                    {
                        avProb = 0.0;
                    }*/
                    availability = 0.0;
                }
            }
            if ( availability < 0.0 )
            {
                availability = 0.0;
            }

            Object keys[] = gVectors.keySet().toArray();
            for (int k = 0; k < keys.length; k++ )
            {
                int currentKey = ((Integer)keys[k]).intValue(); // Current key is one of the directions to consider.
                double dist;// = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                double g;// = availability / (dist*dist);
                int directionToTake;
                if ( currentNode != closestNodeInBlock )
                {
                    double adjBlockDist = edgeWeights[currentNode][currentKey];
                    double pathDist = SP[currentKey][closestNodeInBlock];
                    dist = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                }
                else
                {
                    // This means that there's availability in an incident block to the currentLocation (node).
                    int farthestNodeInBlock = getFarthestNodeInBlock(currentBlock,currentNode);
                    if ( farthestNodeInBlock == currentKey )
                    {
                        // He will just move to half the block and try to park.
                        dist = (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                    }
                    else
                    {
                        double adjBlockDist = edgeWeights[currentNode][currentKey];
                        double pathDist = SP[currentKey][closestNodeInBlock];
                        dist = adjBlockDist + pathDist + (edgeWeights[nodesInCurrentBlock[0]-7001][nodesInCurrentBlock[1]-7001]/2);
                    }
                }
 
                g = (availability) / (dist*dist);
                directionToTake = currentKey; //SP_direction[currentNode][closestNodeInBlock];

                double previousG = ((Double)gVectors.get(directionToTake)).doubleValue();
                double newG = previousG + g;
                gVectors.put(new Integer(directionToTake),new Double(newG));
            }
        }

        // Now all gVectors are set.  Then choose the highest one (DM-GPA).
        return DM_GPA(gVectors);
         
    }

    public int greedy_OnlyAv_Algorithm(int vehId, Timestamp t)
    {
        int currentNode = currentLocations[vehId].getDirection();

        //int closestNodeToVisit = -1;
        double sp_length = 999999999.0;
        double sp_metric = 999999999.0;
        int blockToVisit = -1;
        //double halfBlockDist = -1.0;
        int spDir = -1;

        for ( int j = 0; j < nBlocks; j++ )
    	{
            int cBlock = blockIds[j];
            int reportedAvailability = userViewOfData.getOnlyAvReportsTotal(cBlock,T);
            //System.out.println(cBlock + "   " + reportedAvailability);
            int closestNodeInBlock = getClosestNodeInBlock(cBlock,currentNode);  // Decide whether to go to node1 or node2 of cBlock.
            int nodesInCBlock[] = blockIdMap.get(cBlock);
            //availability += parkingChanges[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]; //SHOULD USERS KNOW OF NEW PARKING UPDATES???
            // Probably updates should be based on penetration ratio'

            if ( foundEmpty.containsKey(cBlock) )
	    {
                Timestamp pastTime = (Timestamp)(foundEmptyTime.get(cBlock));
                if ( secondsHavePassed(secondsPastEmpty,pastTime,t) )
                {
                    // Remove block from the foundEmpty maps
                    foundEmpty.remove(cBlock);
                    foundEmptyTime.remove(cBlock);
                }
                else
                {
                    reportedAvailability = 0;
                }
            }
            double dist = SP[currentNode][closestNodeInBlock]+(edgeWeights[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]/2);
            double metric = dist;// /reportedAvailability;                            

            if ((metric < sp_metric) && (reportedAvailability > 0))
            {
                sp_length = dist;
                //closestNodeToVisit = closestNodeInBlock;
                blockToVisit = cBlock;
                sp_metric = metric;
                //halfBlockDist = edgeWeights[nodesInCBlock[0]-7001][nodesInCBlock[1]-7001]/2;
                spDir = SP_direction[currentNode][closestNodeInBlock];
            }
        }

        if ( spDir == -1 )
	{
            // Choose a random direction
            ArrayList<Integer> adjNodesGreedy = adjacentNodes(currentNode);
            int randomIndex = (int)Math.ceil(adjNodesGreedy.size()*Math.random())-1;
            spDir = adjNodesGreedy.get(randomIndex);
        }

        // Set newDirection to spDir and set the newEdge.
        if ( currentNode == spDir )
        {
            spDir = getFarthestNodeInBlock(blockToVisit,currentNode);
        }

        return spDir;
    }

    public double systemOptimalTotalCost(SFParkLocation il[], Timestamp it)
    {
    	// Compute the total number of slots available (including time?) and give them an Identification (SFParkEdge?, offset)
    	ArrayList<SFParkEdge> parkingSlots = getIndividualSlots();
    	int totalAvailableSlots = parkingSlots.size();
        
    	// Create a cost matrix from each vehicle to each available slot
    	double costVehicleToSlots[][] = new double[numVeh][totalAvailableSlots];
    	for ( int i = 0; i < costVehicleToSlots.length; i++ )
    	{
    		for (int j = 0; j < costVehicleToSlots[0].length; j++ )
    		{
    			costVehicleToSlots[i][j] = vehToSlotDistance(il[i],parkingSlots.get(j));
    		}
    	}
    	
    	if (numVeh > totalAvailableSlots)
    	{
    	    System.out.println("numVeh > totalAvailableSlots!!!!!");
    	    System.exit(-1);
    	}
    	
    	// Run HungarianAlgorithm
    	int preSystOptAssignment[][] = new int[numVeh][2];
    	preSystOptAssignment = HungarianAlgorithm.hgAlgorithm(costVehicleToSlots,"min");
    	int systOptAssignment[] = new int[numVeh];
    	for ( int i = 0; i < numVeh; i++ )
    	{
    		systOptAssignment[preSystOptAssignment[i][0]] = preSystOptAssignment[i][1];
    	}
    	
    	// Sum up all costs for the assignment
    	double cost = 0.0;
    	for ( int i = 0; i < systOptAssignment.length; i++ )
    	{
    		cost += costVehicleToSlots[i][systOptAssignment[i]];
    	}
    	return cost;
    }

    public int edgeAvailability(SFParkEdge e)
    {
    	int nBlocks = e.getNBlocks();
    	if ( nBlocks == 2 )
    	{
    		return (groundTruthData.getAvailability(e.getBlockId1()) + groundTruthData.getAvailability(e.getBlockId2()));
    	}
    	else if ( nBlocks == 1 )
    	{
    		return groundTruthData.getAvailability(e.getBlockId1());
    	}
    	else // nBlocks == 0
    	{
    		return 0;
    	}
    }
    
    public ArrayList<SFParkEdge> getIndividualSlots()
    {
        ArrayList<SFParkEdge> parkingSlots = new ArrayList<SFParkEdge>();
    	for ( int i = 0; i < edges.length; i++ )
    	{
    		for ( int j = 0; j < edges[0].length; j++ )
    		{
    			if ( edges[i][j] != null )
    			{
    				SFParkEdge currentEdge = edges[i][j];
    				int edgeAv = edgeAvailability(currentEdge);
    				for ( int k = 0; k < edgeAv; k++ )
    				{
    					parkingSlots.add(currentEdge);
    				}
    			}
    		}
    	}
    	return parkingSlots;
    }
    
    public double vehToSlotDistance(SFParkLocation vehLoc, SFParkEdge e)
    {
    	// Should return the distance between the location of the veh and the midpoint of the edge e.
        int vehNodeId1 = vehLoc.getEdge().getNode1().getId()-7001;
        int vehNodeId2 = vehLoc.getEdge().getNode2().getId()-7001;
        int eNodeId1 = e.getNode1().getId()-7001;
        int eNodeId2 = e.getNode2().getId()-7001;
        
        if ( sameEdge(vehLoc.getEdge(), e) )
        {
        	// vehicle is located in e
        	double halfOfEdge = ((double)SP[vehNodeId1][vehNodeId2]) / 2.0;
        	double offset = vehLoc.getOffset();
        	if ( offset <= halfOfEdge )
        	{
        		return (halfOfEdge-offset);
        	}
        	else
        	{
        	    /*if ((SP[vehNodeId1][vehNodeId2]-offset) + halfOfEdge < 0.0)
        	    {
        		System.out.println("WHAAAAAAAAAATTTTT!!!!!");
        	    }*/
        	    return ((SP[vehNodeId1][vehNodeId2]-offset) + halfOfEdge);
        	}
        }
        else
        {
            int edgeBlockId = e.getBlockId1();
            int closestNodeInEdgeBlock = getClosestNodeInBlock(edgeBlockId,vehLoc.getDirection());  // Decide whether to go to node1 or node2 of currentBlock.        	
            double distance = 0.0;
            
            // distance to directionNode
            distance += (SP[vehNodeId1][vehNodeId2] - vehLoc.getOffset());
            // distance from directionNode to closestNode in edge e
            distance += SP[vehLoc.getDirection()][closestNodeInEdgeBlock];
            // distance to midpoint of edge e
            distance += (((double)SP[eNodeId1][eNodeId2]) / 2.0);
            return distance;
        }
    	
    }
    
    public boolean sameEdge(SFParkEdge e1, SFParkEdge e2)
    {
    	int e1node1 = e1.getNode1().getId();
    	int e1node2 = e1.getNode2().getId();
    	int e2node1 = e2.getNode1().getId();
    	int e2node2 = e2.getNode2().getId();
    	
    	if ( e1node1 == e2node1 )
    	{
    	    if ( e1node2 == e2node2 )
    	    {
    		return true;
    	    }
    	    else
    	    {
    		return false;
    	    }
    	}
    	else if ( e1node1 == e2node2 )
    	{
    	    if ( e1node2 == e2node1 )
    	    {
    		return true;
    	    }
    	    else
    	    {
    		return false;
    	    }
    	}
    	else
    	{
    	    return false;
    	}
    }

    public double easyNETotalCost(SFParkLocation il[], Timestamp it)
    {
    	// Compute the total number of slots available (including time?) and give them an Identification (SFParkEdge?, offset)
    	ArrayList<SFParkEdge> parkingSlots = getIndividualSlots();
    	int totalAvailableSlots = parkingSlots.size();
        
    	boolean localParked[] = new boolean[numVeh];
    	boolean localFilled[] = new boolean[totalAvailableSlots];
    	
    	for ( int i = 0; i < localParked.length; i++ )
    	{
    	    localParked[i] = false;
    	    localFilled[i] = false;
    	}
    	
    	// Create a cost matrix from each vehicle to each available slot
    	double costVehicleToSlots[][] = new double[numVeh][totalAvailableSlots];
    	for ( int i = 0; i < costVehicleToSlots.length; i++ )
    	{
    	    for (int j = 0; j < costVehicleToSlots[0].length; j++ )
    	    {
    		costVehicleToSlots[i][j] = vehToSlotDistance(il[i],parkingSlots.get(j));
    	    }
    	}
    	
       	if (numVeh > totalAvailableSlots)
    	{
    	    System.out.println("numVeh > totalAvailableSlots!!!!!");
    	    System.exit(-1);
    	}
    	
    	// Run an easy Nash Equilibrium algorithm
    	int nParked = 0;
       	int preEasyNEAssignment[][] = new int[numVeh][2];
       	while (nParked < numVeh)
       	{
       	    double minCost = Double.MAX_VALUE;
       	    int minVehicle = -1;
       	    int minParkingSlot = -1;
    	    for ( int i = 0; i < costVehicleToSlots.length; i++ )
    	    {
    	    	if ( !localParked[i] )
    	    	{
    		    for ( int j = 0; j < costVehicleToSlots[0].length; j++ )
    		    {
    			if ( !localFilled[j] )
    			{
    			    if ( costVehicleToSlots[i][j] < minCost )
    			    {
    			        minCost = costVehicleToSlots[i][j];
    			        minVehicle = i;
    			        minParkingSlot = j;
    			    }
    			}
    		    }
    	    	}
    	    }
    	    
    	    preEasyNEAssignment[nParked][0] = minVehicle;
    	    preEasyNEAssignment[nParked][1] = minParkingSlot;
    	    localParked[minVehicle] = true;
    	    localFilled[minParkingSlot] = true;
    	    nParked++;
       	}

    	int easyNEAssignment[] = new int[numVeh];
    	for ( int i = 0; i < numVeh; i++ )
    	{
    	    easyNEAssignment[preEasyNEAssignment[i][0]] = preEasyNEAssignment[i][1];
    	}
    	
    	// Sum up all costs for the assignment
    	double cost = 0.0;
    	for ( int i = 0; i < easyNEAssignment.length; i++ )
    	{
    	    cost += costVehicleToSlots[i][easyNEAssignment[i]];
    	}
    	return cost;
    }

    public int Josse_algorithm(int vehId, Timestamp t)
    {
	// Implements the G1 algorithm from EDBT 2016

        // This algorithm returns the next intersection to take according to the G1 algorithm (as specified
        // in the EDBT 2016 paper by Josse, Schmid, and Schubert.

        int currentNode = currentLocations[vehId].getDirection();
        return Josse_computeNextNode(vehId,currentNode,t);
    }
    
    public int Josse_computeNextNode(int vehId, int startNode, Timestamp currentTime)
    {
        // Will compute next node to take according to G1 probabilities (EDBT 2016 paper).

        //ArrayList<Integer> adjNodes = adjacentNodes(startNode); //THIS IS OLD WAY WITH BEING ABLE TO RETURN TO OLD EDGE

	
        // int currentNode = currentLocation.getDirection();
        SFParkEdge e = currentLocations[vehId].getEdge();
        int node1 = e.getNode1().getId() - 7001;
        int node2 = e.getNode2().getId() - 7001;
       
        ArrayList<Integer> adjNodesJosse = new ArrayList<Integer>();
        for ( int j = 0; j < n; j++ )
        {
            if ( (node1 != j) && (node2 != j) )  // This is done to check that he doesn't return to the previous edge.
	    {
                if ( edges[startNode][j] != null )
                {
                    // Then j is adjacent to the currentLocation
                    adjNodesJosse.add(new Integer(j));
                }
            }
	}

        double bestP_g1 = -2.0;
        int bestNode = -1;
        for ( int i = 0; i < adjNodesJosse.size(); i++ )
        {
            int adjNode = (int)adjNodesJosse.get(i);

            SFParkEdge currentEdge = edges[startNode][adjNode];
            int nBlocks = currentEdge.getNBlocks();
            double currentP_g1 = -1.75;

            if ( nBlocks == 0 )
	    {
                currentP_g1 = -1.0;
            }
            else if ( nBlocks == 1 )
	    {
                int blockId = currentEdge.getBlockId1();

                // Check if this block has been foundEmpty recently.
                if ( foundEmpty.containsKey(blockId) )
                {
                    Timestamp pastTime = (Timestamp)(foundEmptyTime.get(blockId));
                    if ( secondsHavePassed(secondsPastEmpty,pastTime,currentTime) )
                    {
                        // Remove block from the foundEmpty maps
                        foundEmpty.remove(blockId);
                        foundEmptyTime.remove(blockId);
                        currentP_g1 = p_g1(vehId,startNode,adjNode,currentTime);
                    }
                    else
                    {
                        // This block was foundEmpty too recently.
                        currentP_g1 = -1.5;
                    }
                }
                else
	     	{
                    currentP_g1 = p_g1(vehId,startNode,adjNode,currentTime);
                }
            }
            else if (nBlocks == 2 )
	    {
                int blockId1 = currentEdge.getBlockId1();
                int blockId2 = currentEdge.getBlockId2();

                // Check if these blocks have been foundEmpty recently.
                if ( foundEmpty.containsKey(blockId1) )
                {
                    Timestamp pastTime = (Timestamp)(foundEmptyTime.get(blockId1));
                    if ( secondsHavePassed(secondsPastEmpty,pastTime,currentTime) )
                    {
                        // Remove block from the foundEmpty maps
                        foundEmpty.remove(blockId1);
                        foundEmptyTime.remove(blockId1);
                        foundEmpty.remove(blockId2);
                        foundEmptyTime.remove(blockId2);
                        currentP_g1 = p_g1(vehId,startNode,adjNode,currentTime);
                    }
                    else
                    {
                        // This block was foundEmpty too recently.
                        currentP_g1 = -1.5;
                    }
                }
                else
		{
                    currentP_g1 = p_g1(vehId,startNode,adjNode,currentTime);
                }

            }
            else
	    {
                System.out.println("Number of edges is wrong in function computeNextNode.");
                System.exit(-1);
            }

            if ( currentP_g1 >= bestP_g1 )
	    {
                bestP_g1 = currentP_g1;
                bestNode = adjNode;
            }

        }
        //System.out.println("Next node is " + bestNode);
	if (bestNode == -1)
	    return randomWalk_algorithm(vehId,currentTime);

	if (bestP_g1 < rho)
	    // We need to extend the path.  Do we need to do anything here?
	    // We don't really need to extend because after traversing it will extend.
	    ;
	
        return bestNode;
    }
    
    public double p_g1(int vehId, int currentNode, int adjNode, Timestamp currentTime)
    {
        // Computes the probability of vaccancy in the block, according to the G1 method presented in EDBT 2016
        // paper by Josse, Schmid, and Schubert.

        ArrayList<Timestamp> avReports;
        ArrayList<Timestamp> avReports2;
        avReports = new ArrayList<Timestamp>();

	    double lambda = 0.0;
	    double mu = 0.0;
	
        SFParkEdge currentEdge = edges[currentNode][adjNode];
        int nBlocks = currentEdge.getNBlocks();
        if ( nBlocks == 0 )
        {
            return 0.0;
        }
        else if ( nBlocks == 1 )
	    {
            int blockId = currentEdge.getBlockId1();
            avReports = (ArrayList<Timestamp>)(userViewOfData.getAvailabilityReports(blockId));
	        if (!PreprocessingForParkingDeparking.inverseLambda.containsKey(blockId) || !PreprocessingForParkingDeparking.inverseMu.containsKey(blockId))
	        {
                return 0.0;
	        }
	        lambda = 1.0/(PreprocessingForParkingDeparking.inverseLambda.get(blockId));
	        mu = 1.0/(PreprocessingForParkingDeparking.inverseMu.get(blockId));
        }
        else if (nBlocks == 2 )
	    {
            int blockId1 = currentEdge.getBlockId1();
            int blockId2 = currentEdge.getBlockId2();
            //System.out.println("blocks before error are: " + blockId1 + " and " + blockId2); 
            avReports = (ArrayList<Timestamp>)(userViewOfData.getAvailabilityReports(blockId1));
            avReports2 = (ArrayList<Timestamp>)(userViewOfData.getAvailabilityReports(blockId2));

	        if (!PreprocessingForParkingDeparking.inverseLambda.containsKey(blockId1) || !PreprocessingForParkingDeparking.inverseLambda.containsKey(blockId2))
	        {
                return 0.0;
	        }
	        double inverseLambda1 = PreprocessingForParkingDeparking.inverseLambda.get(blockId1);
	        double inverseLambda2 = PreprocessingForParkingDeparking.inverseLambda.get(blockId2);
	        lambda = 1.0/((inverseLambda1+inverseLambda2)/2.0);

	        if (!PreprocessingForParkingDeparking.inverseMu.containsKey(blockId1) || !PreprocessingForParkingDeparking.inverseMu.containsKey(blockId2))
	        {
                return 0.0;
	        }
	        double inverseMu1 = PreprocessingForParkingDeparking.inverseMu.get(blockId1);
	        double inverseMu2 = PreprocessingForParkingDeparking.inverseMu.get(blockId2);
	        mu = 1.0/((inverseMu1+inverseMu2)/2.0);
            for ( int i = 0; i < ((ArrayList<Timestamp>)avReports2).size(); i++ )
	        {
                avReports.add((Timestamp)(avReports2.get(i)));
            }
        }
        else
	    {
            System.out.println("Number of edges is wrong in function p_g1.");
            System.exit(-1);
        }

        double p_g1s[] = new double[((ArrayList<Timestamp>)avReports).size()];
        //System.out.println("\tp_g1s.length == " + p_g1s.length );
        
        // Compute p_g1 for each report.
        for ( int i = 0; i < p_g1s.length; i++ )
	    {
            Timestamp t_v = (Timestamp)(avReports.get(i));
            double timeDiffInMinutes = (((double)(currentTime.getTime()-t_v.getTime()))/1000.0)/60.0;
            //System.out.println("\t\t\tcurrentTime == " + currentTime);
            //System.out.println("\t\t\tt_v == " + t_v);
            //System.out.println("\t\ttimeDiffInMinutes == " + timeDiffInMinutes);
            //double exponent = (-1)*lambda*(timeDiffInMinutes);
            //System.out.println("\t\tp_g1s[i] == " + exponent);
            double exponent = (-1)*(lambda+mu)*timeDiffInMinutes;
	        p_g1s[i] = ((mu)/(lambda+mu)) + ((lambda)/(lambda+mu))*Math.exp(exponent);
        }

        // Now we have a p_g1 value for each parking report.  So we then combine them into one p_vac value.
        double probabilityOfZeroParking = 1;
        for ( int i = 0; i < p_g1s.length; i++ )
	    {
            probabilityOfZeroParking = probabilityOfZeroParking * (1-p_g1s[i]);
        }

        //System.out.println("Probality of parking is " + (1-probabilityOfZeroParking));
        return (1 - probabilityOfZeroParking);
    }

    public String optAlgorithmSingleStep(int vehId)
    {
	    String optStep = (EndOfSearch + "," + 0.0);
        if (!runningOptPaths.get(vehId).isEmpty())
	    {  // if there are still more edges in runningOptPath
	        optStep = runningOptPaths.get(vehId).get(0);
	        runningOptPaths.get(vehId).remove(0);
	    }
	    return optStep;
    }

    public ArrayList<ArrayList<String>> optAlgrorithmFinite(int destination)
    {
        ArrayList<ArrayList<HashMap<ArrayList<Integer>,Double>>> C = new ArrayList<ArrayList<HashMap<ArrayList<Integer>,Double>>>();
	    ArrayList<ArrayList<HashMap<ArrayList<Integer>,Integer>>> NEXT = new ArrayList<ArrayList<HashMap<ArrayList<Integer>,Integer>>>();
	    // Create possible history paths for each node:
		
	    C.add(new ArrayList<HashMap<ArrayList<Integer>,Double>>());
	    NEXT.add(new ArrayList<HashMap<ArrayList<Integer>,Integer>>());
	    for(int i = 0; i < n; i++)
	    {
	        // System.out.println("History path before " + i + ":\n" + historyPathslist.get(i)) ;
	        for (ArrayList path : historyPathslist.get(i))
	        {
	            C.get(0).add(new HashMap<ArrayList<Integer>,Double>());
                NEXT.get(0).add(new HashMap<ArrayList<Integer>,Integer>());
		        C.get(0).get(i).put(path,(double)beta);
		        NEXT.get(0).get(i).put(path,EndOfSearch);
		        // System.out.println(NEXT.get(0).get(i)); //correct
            }
	    }
		
	    for (int k = 1; k <= k_m; k++ )
	    {
	        C.add(new ArrayList<HashMap<ArrayList<Integer>,Double>>());
	        NEXT.add(new ArrayList<HashMap<ArrayList<Integer>,Integer>>());
	        for(int i = 0; i < n; i++)
	        {
                for (ArrayList path : historyPathslist.get(i))
		        {
		            C.get(k).add(new HashMap<ArrayList<Integer>,Double>());
		            NEXT.get(k).add(new HashMap<ArrayList<Integer>,Integer>());
		            C.get(k).get(i).put(path,Double.POSITIVE_INFINITY);
		            NEXT.get(k).get(i).put(path,EndOfSearch);
		            // for (int j = 0; j < n; j++)
		            for (Integer j : adjList.get(i))
		            {
			            // if (edges[i][j] != null)
			            // {
			            ArrayList<Integer> lastHistoryList = new ArrayList<>();
			            for (int idx = 0; idx < path.size(); idx++)
                        {
			                lastHistoryList.add((int)path.get(idx));
			            }
			            lastHistoryList.add(i);
			            lastHistoryList.remove(0);
			            boolean recentlyTraversed = false;
			            double accumulatedTime = 0;
			            for (int pathIdx = 1;  pathIdx < path.size(); pathIdx++)
			            {
			                if (tau == 0)
			                {
				                break;
			                }
			                accumulatedTime = accumulatedTime + edgeCosts[(int)path.get(pathIdx)][(int)path.get(pathIdx-1)];
			                if ((((Integer)path.get(pathIdx)).intValue() == i && path.get(pathIdx-1) == j) ||
				                      (path.get(pathIdx) == j && ((Integer)path.get(pathIdx-1)).intValue() == i))
			                {
				                recentlyTraversed = true;
			                }
			                if (accumulatedTime >= tau)
			                {
				                break;
			                }
			            }
			            double p_ij = recentlyTraversed ? 0 : probability[i][j];
			            double C_cont = C.get(k-1).get(j).get(lastHistoryList);
			            double uc_ij = SP[j][destination]/velocityWalking;
			            double C_ijk;
			            if ( uc_ij > C_cont )
			            {
			                C_ijk = edgeCosts[i][j] + C_cont;
			            }
			            else
			            {
			                C_ijk = edgeCosts[i][j]  + uc_ij * p_ij + (1-p_ij) * C_cont;
			            }
							
			            if(C_ijk < C.get(k).get(i).get(path))
			            {
                            C.get(k).get(i).put(path,C_ijk);
			                NEXT.get(k).get(i).put(path,j);
			            }
		            // }
                    }
		        }
            }
        }
	    // System.out.println(C.get(2));
	    ArrayList<ArrayList<String>> finalPaths = new ArrayList<ArrayList<String>>();
		
	    for ( int i = 0; i < n; i++ )
	    {
	        ArrayList<String> finalPath = new ArrayList<String>(k_m);
	        ArrayList<Integer> runningHistory = new ArrayList<Integer>();
	        finalPath.add(0, (i + "," + 0.0));
	        runningHistory.add(0, i);
	        for (int k = 1; k <= k_m; k++)
	        {
	 	        String theEntry[] = finalPath.get(k-1).split(",");
		        int currentNode = Integer.parseInt(theEntry[0]);
		        if (k > k_tau)
		        {
		            // System.out.println(runningHistory);
		            ArrayList<Integer> runningHistoryArg = new ArrayList<Integer>(runningHistory);
		            int nextNode = NEXT.get(k_m-k).get(currentNode).get(runningHistory.subList(0,(int)runningHistory.size()-1));
		            double nextCost = C.get(k_m-k).get(currentNode).get(runningHistory.subList(0,(int)runningHistory.size()-1));
		            finalPath.add(nextNode + "," + nextCost);
		            runningHistory.add(nextNode);
				
                    runningHistory.remove(0);
                }
		        else
		        {
		            ArrayList<Integer> runningHistoryArg = new ArrayList<Integer>();
		            for (ArrayList path : historyPathslist.get(currentNode))
		            {
			            if(runningHistory.subList(0,(int)runningHistory.size()-1).equals(path.subList(k_tau-k+1, k_tau)))
                        {
			                // System.out.println(runningHistory + "  vs  " + path.subList(k_tau-k, k_tau));
			                for (int idx = 0; idx < path.size(); idx++)
			                {
				                runningHistoryArg.add((int)path.get(idx));
			                }
			                break;
                        }
		            }
		            // System.out.println(runningHistory);
		            int nextNode = NEXT.get(k_m-k).get(currentNode).get(runningHistoryArg);
		            double nextCost = C.get(k_m-k).get(currentNode).get(runningHistoryArg);
		            finalPath.add(nextNode + "," + nextCost);
		            runningHistory.add(nextNode);
		        }
	        }
	        finalPath.remove(0);
			
	        finalPaths.add(i, finalPath);
	    }
	    return finalPaths;
    }

    public void computeAdjList()
    {
	    for(int i = 0; i < n; i++)
	    {
	        ArrayList<Integer> adjNodesForI = new ArrayList<Integer>();
	        for (int j = 0; j < n; j++)
	        {
		        if (edges[i][j] != null)
		        {
		            adjNodesForI.add(j);
		        }
	        }
	        adjList.add(adjNodesForI);
	    }
    }

    private ArrayList<ArrayList<ArrayList<Integer>>> historyPathslist = new ArrayList<ArrayList<ArrayList<Integer>>>();
	
    public void initiateHistoryPaths()
    {	// Create possible history paths for each node:
	    for(int i = 0; i < n; i++)
	    {
	        ArrayList<ArrayList<Integer>> historyPathsForI = new ArrayList<ArrayList<Integer>>();
	        ArrayList<Integer> currentPath = new ArrayList<Integer>();
	        for( int i_1 = 0; i_1 < n; i_1++ )
	        {
		        if( edges[i_1][i] != null )
		        {	// k_tau == 0 is not considered.
		            currentPath.add(0,i_1);
		            DFSforHistoryPaths( historyPathsForI, currentPath, i, 1 );
		            currentPath.remove(0);
		        }
	        }
	        historyPathslist.add(historyPathsForI);
	    }
		// System.out.println(historyPathslist);
    }

    public void DFSforHistoryPaths(ArrayList<ArrayList<Integer>> historyPathsForI, ArrayList<Integer> currentPath, int root, int depth)
    {	
	    if ( depth != k_tau )
	    {
	        int currentNode = currentPath.get(0);
	        for( int i = 0; i < n; i++ )
	        {
		        if( edges[i][currentNode] != null )
		        {
		            currentPath.add(0,i);
		            DFSforHistoryPaths(historyPathsForI, currentPath, root, depth+1 );
		            currentPath.remove(0);
		        }
	        }
	    }
	    else
	    {
	        ArrayList<Integer> finalPath = new ArrayList<Integer>();
	        for (int idx = 0; idx < currentPath.size(); idx++)
	        {
		        finalPath.add((int)currentPath.get(idx));
	        }
	        historyPathsForI.add(finalPath);
	    }
    }

    public void computeProbAvail_randomScan()
    {
	    List<String> dates =  new ArrayList<String>();
	    dates.add("306"); // 4/6/2012
	    dates.add("309");
	    dates.add("310");
	    dates.add("311");
	    dates.add("312");
	    dates.add("313");
	    dates.add("316");
	    dates.add("317");
       	dates.add("318");
	    dates.add("319");
	    dates.add("320");
	    dates.add("323");
	    dates.add("324");
	    dates.add("325");
	    dates.add("326");
	    dates.add("327");
	    dates.add("330");
	    dates.add("401");
	    dates.add("402");
	    dates.add("403");
	    // dates.add("404");
	    double numberOfAtoms = dates.size();
	    Timestamp [] randomTime = new Timestamp[(int)numberOfAtoms];
	    int randomTimeIdx = 0;
	    for (String date : dates)
	    {
	        Timestamp startTime = new Timestamp(initYear-1900, Integer.parseInt(date.substring(0,1)), Integer.parseInt(date.substring(1)), initHour1, initMinute, initSecond, 0); // One millisecond is the atom time.
	        // Timestamp randomTime;
	        randomTime[randomTimeIdx] = generateRandomTime(startTime, probDurationInMin);
	        //System.out.print("randomTime[randomTimeIdx] == " + randomTime[randomTimeIdx]);
	        groundTruthDataQing.advanceToTime(randomTime[randomTimeIdx]);
	        //System.out.println(" ... I'm fine...");
	        accumulatedProbAvail();
	        // accumulatedAvail();
	        randomTimeIdx++;
	    }
	    for ( int i = 0; i < n; i++ )
	    {
	        for ( int j = 0; j < i; j++ )
	        {
		        if ( edges[i][j] != null )
		        {
		            probability[i][j] = probability[i][j] / numberOfAtoms;
		            avail[i][j] = avail[i][j] / numberOfAtoms;
		            // System.out.println(probability[i][j]);
		        }
		        probability[j][i] = probability[i][j];
		        avail[j][i] = avail[i][j];
	        }
	    }

	    randomTimeIdx = 0;
	    groundTruthDataQing.restartTheParkingMap();
		
	    for (String date : dates)
	    {
	        // Timestamp startTime = new Timestamp(initYear-1900, Integer.parseInt(date.substring(0,1)), Integer.parseInt(date.substring(1)), initHour1, initMinute, initSecond, 0); // One millisecond is the atom time.
	        // Timestamp randomTime;
	        // randomTime = generateRandomTime(startTime, probDurationInMin);
	        groundTruthDataQing.advanceToTime(randomTime[randomTimeIdx]);
	        accumulatedVar();
	        randomTimeIdx++;
	    }
		
	    for ( int i = 0; i < n; i++ )
	    {
	        for ( int j = 0; j < i; j++ )
	        {
		        if ( edges[i][j] != null )
		        {
		            var[i][j] = var[i][j] / (numberOfAtoms-1);
		            probByExp[i][j] = 1 - Phi(0.5, avail[i][j], Math.sqrt(var[i][j]));
		            // System.out.println(probability[i][j] + " Est: " + probByExp[i][j]);
		        }
		        var[j][i] = var[i][j];
		        probByExp[j][i] = probByExp[i][j];
	        }
	    }
		
	    groundTruthDataQing.restartTheParkingMap();
    }

    // return Phi(z) = standard Gaussian cdf using Taylor approximation
    public static double Phi(double z)
    {
	    if (z < -8.0) return 0.0;
	    if (z >  8.0) return 1.0;
	    double sum = 0.0, term = z;
	    for (int i = 3; sum + term != sum; i += 2) {
	        sum  = sum + term;
	        term = term * z * z / i;
	    }
	    return 0.5 + sum * phi(z);
    }

    // return Phi(z, mu, sigma) = Gaussian cdf with mean mu and stddev sigma
    public static double Phi(double z, double mu, double sigma)
    {
	    return Phi((z - mu) / sigma);
    } 

    // {	// Computing the Gaussian probabilities:
	// Source: http://introcs.cs.princeton.edu/java/22library/Gaussian.java.html
	// return phi(x) = standard Gaussian pdf
    public static double phi(double x)
    {
    	return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
    }

    // return phi(x, mu, signma) = Gaussian pdf with mean mu and stddev sigma
    public static double phi(double x, double mu, double sigma)
    {
	    return phi((x - mu) / sigma) / sigma;
    }

    public Timestamp generateRandomTime(Timestamp startingTimeInterval, long timeDurationInMin)
    {
	    long startInMilli = startingTimeInterval.getTime();
	    long durationInMilli = timeDurationInMin * 60 * 1000;
	    long randomTimeInMilli = startInMilli + (long)(Math.random() * (durationInMilli + 1));
	    return new Timestamp(randomTimeInMilli);
    }

    public void accumulatedProbAvail()
    {   // With anomalous blocks removed.
        // Timestamp currentTime = new Timestamp(startTime.getTime());
		
        for ( int i = 0; i < n; i++ )
	    {
	        for ( int j = 0; j < i; j++ )
	        {	// To prevent duplicates!
		        if ( edges[i][j] != null )
		        {
		            // groundTruthDataQing.advanceToTime(currentTime);
					
		            int nBlocks = edges[i][j].getNBlocks();
		            if ( nBlocks == 0 )
		            {
			            probability[i][j] = 0;
			            avail[i][j] += 0;
		            }
		            else if (nBlocks == 1 )
		            {
			            int blockId1 = edges[i][j].getBlockId1();
			            if (blockId1 != 326061 && blockId1 != 546282 && groundTruthDataQing.getAvailability(blockId1) > 0)
			            {
			                probability[i][j] ++;
			            }
			            if (blockId1 != 326061 && blockId1 != 546282)
			            {
			                avail[i][j] += groundTruthDataQing.getAvailability(blockId1);
			            }
			            // System.out.println(blockId1 + ": " + groundTruthDataQing.getAvailability(blockId1));
			            // currentTime = addTimes(currentTime, edgeCosts[i][j]);
		            }
		            else // nBlocks == 2
		            {
			            int blockId1 = edges[i][j].getBlockId1();
			            int blockId2 = edges[i][j].getBlockId2();
			            double avail1 = (blockId1 == 326061 || blockId1 == 546282) ? 0 : groundTruthDataQing.getAvailability(blockId1);
			            double avail2 = (blockId2 == 326061 || blockId2 == 546282) ? 0 : groundTruthDataQing.getAvailability(blockId2);
			            if ( (avail1 + avail2) > 0)
			            {
			                probability[i][j] ++;
			            }
			            avail[i][j] += avail1 + avail2;
						
			            // System.out.println(blockId1 + ": " + groundTruthDataQing.getAvailability(blockId1));
			            // System.out.println(blockId2 + ": " + groundTruthDataQing.getAvailability(blockId2));
						
			            // currentTime = addTimes(currentTime, edgeCosts[i][j]);

		            }
		        }
		        else
		        {
		            probability[i][j] = 0;
		            avail[i][j] += 0;
                }
	        }
	    }
    }

    public void accumulatedVar()
    { // With anomalous blocks removed.
	    // Timestamp currentTime = new Timestamp(startTime.getTime());
		
	    for ( int i = 0; i < n; i++ )
	    {
	        for ( int j = 0; j < i; j++ )
	        {
		        if ( edges[i][j] != null )
		        {
		            // groundTruthDataQing.advanceToTime(currentTime);
					
		            int nBlocks = edges[i][j].getNBlocks();
		            if ( nBlocks == 0 )
		            {
			            var[i][j] += 0;
		            }
		            else if (nBlocks == 1 )
		            {
			            int blockId1 = edges[i][j].getBlockId1();
			            if (blockId1 != 326061 && blockId1 != 546282)
			            {
			                double number = groundTruthDataQing.getAvailability(blockId1);
			                var[i][j] += (avail[i][j] - number) * (avail[i][j] - number);
			            }
						
			            // currentTime = addTimes(currentTime, edgeCosts[i][j]);
		            }
		            else // nBlocks == 2
		            {
			            int blockId1 = edges[i][j].getBlockId1();
			            int blockId2 = edges[i][j].getBlockId2();
			            double avail1 = (blockId1 == 326061 || blockId1 == 546282) ? 0 : groundTruthDataQing.getAvailability(blockId1);
			            double avail2 = (blockId2 == 326061 || blockId2 == 546282) ? 0 : groundTruthDataQing.getAvailability(blockId2);
			            double number = avail1 + avail2;
			            var[i][j] += (avail[i][j] - number) * (avail[i][j] - number);
						
			            // currentTime = addTimes(currentTime, edgeCosts[i][j]);
		            }
		        }
		        else
		        {
		            var[i][j] += 0;
		        }
	        }
	    }
    }

    
    public ArrayList<ArrayList<Integer>> probMaxAlgFinite(Timestamp startTime)
    {
	    // Computing the optimal path for the PM algorithm.  This is taken from Qing's code
	
	    ArrayList<ArrayList<HashMap<ArrayList<Integer>,Double>>> C = new ArrayList<ArrayList<HashMap<ArrayList<Integer>,Double>>>();
	    ArrayList<ArrayList<HashMap<ArrayList<Integer>,Integer>>> NEXT = new ArrayList<ArrayList<HashMap<ArrayList<Integer>,Integer>>>();
	    // Create possible history paths for each node:
		
	    C.add(new ArrayList<HashMap<ArrayList<Integer>,Double>>());
	    NEXT.add(new ArrayList<HashMap<ArrayList<Integer>,Integer>>());
	    for(int i = 0; i < n; i++)
	    {
	        // System.out.println("History path before " + i + ":\n" + historyPathslist.get(i)) ;
	        for (ArrayList path : historyPathslist.get(i))
	        {
		        C.get(0).add(new HashMap<ArrayList<Integer>,Double>());
		        NEXT.get(0).add(new HashMap<ArrayList<Integer>,Integer>());
		        C.get(0).get(i).put(path,1.0);
		        NEXT.get(0).get(i).put(path,EndOfSearch);
		        // System.out.println(NEXT.get(0).get(i)); //correct
	        }
	    }
		
	    for (int k = 1; k <= k_m; k++ )
	    {
	        C.add(new ArrayList<HashMap<ArrayList<Integer>,Double>>());
	        NEXT.add(new ArrayList<HashMap<ArrayList<Integer>,Integer>>());
	        for(int i = 0; i < n; i++)
	        {
		        for (ArrayList path : historyPathslist.get(i))
		        {
		            C.get(k).add(new HashMap<ArrayList<Integer>,Double>());
		            NEXT.get(k).add(new HashMap<ArrayList<Integer>,Integer>());
		            C.get(k).get(i).put(path,1.0);
		            NEXT.get(k).get(i).put(path,EndOfSearch);
		            for (Integer j : adjList.get(i))
		            {
			            ArrayList<Integer> lastHistoryList = new ArrayList<>();
			            for (int idx = 0; idx < path.size(); idx++)
			            {
			                lastHistoryList.add((int)path.get(idx));
			            }
			            lastHistoryList.add(i);
			            lastHistoryList.remove(0);
			            boolean recentlyTraversed = false;
			            double accumulatedTime = 0;
			            for (int pathIdx = 1;  pathIdx < path.size(); pathIdx++)
			            {
			                if (tau == 0)
			                {
				                break;
			                }
			                accumulatedTime = accumulatedTime + edgeCosts[(int)path.get(pathIdx)][(int)path.get(pathIdx-1)];
			                if ((((Integer)path.get(pathIdx)).intValue() == i && path.get(pathIdx-1) == j) || (path.get(pathIdx) == j && ((Integer)path.get(pathIdx-1)).intValue() == i))
			                {
				                recentlyTraversed = true;
			                }
			                if (accumulatedTime >= tau)
			                {
				                break;
			                }
			            }
			            double p_ij = recentlyTraversed ? 0 : p_g1(-1,i,j,startTime);
			            double C_ijk = (1-p_ij) * C.get(k-1).get(j).get(lastHistoryList);
							
			            if(C_ijk < C.get(k).get(i).get(path))
			            {
			                C.get(k).get(i).put(path,C_ijk);
			                NEXT.get(k).get(i).put(path,j);
		            	}
		      
				
		            }
		        }
	        }
	    }

	    ArrayList<ArrayList<Integer>> finalPaths = new ArrayList<ArrayList<Integer>>();
		
	    for ( int i = 0; i < n; i++ )
	    {
	        ArrayList<Integer> finalPath = new ArrayList<Integer>(k_m);
	        ArrayList<Integer> runningHistory = new ArrayList<Integer>();
	        finalPath.add(0, i);
	        runningHistory.add(0, i);
	        for (int k = 1; k <= k_m; k++)
	        {
		        int currentNode = finalPath.get(k-1);
		        if (k > k_tau)
		        {
		            // System.out.println(runningHistory);
		            ArrayList<Integer> runningHistoryArg = new ArrayList<Integer>(runningHistory);
		            int nextNode = NEXT.get(k_m-k).get(currentNode).get(runningHistory.subList(0,(int)runningHistory.size()-1));
		            finalPath.add(nextNode);
		            runningHistory.add(nextNode);
				
		            runningHistory.remove(0);
		        }
		        else
		        {
		            ArrayList<Integer> runningHistoryArg = new ArrayList<Integer>();
		            for (ArrayList path : historyPathslist.get(currentNode))
		            {
			            if(runningHistory.subList(0,(int)runningHistory.size()-1).equals(path.subList(k_tau-k+1, k_tau)))
			            {
			                // System.out.println(runningHistory + "  vs  " + path.subList(k_tau-k, k_tau));
                            for (int idx = 0; idx < path.size(); idx++)
			                {
				                runningHistoryArg.add((int)path.get(idx));
			                }
			                break;
			            }
		            }
		            // System.out.println(runningHistory);
		            int nextNode = NEXT.get(k_m-k).get(currentNode).get(runningHistoryArg);
		            finalPath.add(nextNode);
		            runningHistory.add(nextNode);
		        }
            }
	        finalPath.remove(0);
			
	        finalPaths.add(i, finalPath);
	    }

        return finalPaths;
    }

    public int probMaxAlgSingleStep(int vId)
    {
	    int optStep = EndOfSearch;
	    if (!runningOptPathsPM.get(vId).isEmpty())
	    {   // if there are still more edges in runningOptPathsPM.get(vId)
	        optStep = runningOptPathsPM.get(vId).get(0);
	        runningOptPathsPM.get(vId).remove(0);
	    }
	    return optStep;
    }

    public static void main(String args[])
    {
        SFParkSimPenetrationRatio_AddedUnavReports s = new SFParkSimPenetrationRatio_AddedUnavReports();
    }
}
