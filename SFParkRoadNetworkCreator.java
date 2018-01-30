// Class that takes data from SFPark about their nodes and edges and creates a graph from it

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.ArrayList;

public class SFParkRoadNetworkCreator
{
    public static final int N_NODES = 40;
    public int N_EDGES = 63;
    private String nodeFilename = "SFPark_nodes_FishermansWharf.csv";
    private FileReader node_fr;
    private BufferedReader node_br;
    private String edgeFilename = "SFPark_edges_FishermansWharf2.csv";
    private FileReader edge_fr;
    private BufferedReader edge_br;

    private SFParkNetwork road = new SFParkNetwork(N_NODES);
    private HashMap<Integer,SFParkNode> nodes;
    private SFParkEdge edges[][];
    private double edgeWeights[][];
    private SFParkEdge edgeList[] = new SFParkEdge[N_EDGES];

    private double SP[][] = new double[N_NODES][N_NODES];        // Will contain the shortest path distance between node i and node j.
    private int next[][] = new int[N_NODES][N_NODES];            // Used by the Floyd-Warshall algorithm to reconstruct the shortest paths.
    private int SP_direction[][] = new int[N_NODES][N_NODES];    // Will contain the first node to move towards in the shortest path between node i and node j.

    public SFParkRoadNetworkCreator()
    {
        try
	    {
            node_fr = new FileReader(nodeFilename);
            node_br = new BufferedReader(node_fr);
            edge_fr = new FileReader(edgeFilename);
            edge_br = new BufferedReader(edge_fr);
        }
	    catch (Exception e)
	    {
            e.printStackTrace();
        }
        readNodes();
        nodes = road.getNodes();
        readEdges();
        edges = road.getEdges();
        edgeWeights = road.getEdgeWeights();
        //edgeList = road.getEdgeList();
        //SFParkEdge e = edges[6][11];
        //System.out.println(e.getBlockId1() + " " + e.getBlockId2());
        floydWarshall();

        /*for ( int i = 0; i < N_NODES; i++ )
	    {
            for ( int j = 0; j < N_NODES; j++ )
	        {
                System.out.println("SP between node " + (i+7001) + " and node " + (j+7001) + " goes to node " + (SP_direction[i][j]+7001) + ".");
            }
	    }*/
    }

    public int[][] getSP_direction()
    {
        return SP_direction;
    }
    
    public double[][] getShortestPaths()
    {
        return SP;
    }

    public double[][] getEdgeWeights()
    {
        return edgeWeights;
    }
    
    public SFParkEdge[][] getEdges()
    {
        return edges;
    }

    public SFParkNetwork getRoad()
    {
        return road;
    }

    public SFParkEdge[] getEdgeList()
    {
        return edgeList;
    }

    public HashMap<Integer,SFParkNode> getNodes()
    {
        return nodes;
    }

    public void floydWarshall()
    {
        // This function will use the Floyd-Warshall algorithm to compute the shortest paths between all nodes
        // and the first edge to take for each of those shortest paths.

        for ( int i = 0; i < N_NODES; i++ )
	    {
            SP[i][i] = 0.0;
        }

        for ( int i = 0; i < N_NODES; i++ )
	    {
            for ( int j = 0; j < N_NODES; j++ )
	        {
                if ( i != j )
		        {
                    SP[i][j] = edgeWeights[i][j];
                }
            }
        }

        for ( int i = 0; i < N_NODES; i++ )
	    {
            for ( int j = 0; j < N_NODES; j++ )
	        {
                next[i][j] = -1;
            }
        }

        for ( int k = 0; k < N_NODES; k++ )
	    {
            for ( int i = 0; i < N_NODES; i++ )
	        {
                for ( int j = 0; j < N_NODES; j++ )
		        {
                    if ((SP[i][k]+SP[k][j]) < SP[i][j] )
		            {
                        SP[i][j] = SP[i][k]+SP[k][j];
                        next[i][j] = k;
                    }
                }
            }
        }

        /*ArrayList<Integer> shortestPaths[][] = new ArrayList<Integer>[N_NODES][N_NODES];
        for ( int i = 0; i < N_NODES; i++ )
	    {
            for ( int j = 0; j < N_NODES; j++ )
	        {
                shortestPaths[i][j] = reconstructPath(i,j);
            }
	    }*/

        //System.out.println("SP value of node 0 and 2 is: " + SP[0][2]);
        for ( int i = 0; i < N_NODES; i++ )
	    {
            for ( int j = 0; j < N_NODES; j++ )
	        {
                //System.out.println(i+" "+j);
                ArrayList<Integer> sp = reconstructPath(i,j);
                SP_direction[i][j] = ((Integer)sp.get(1)).intValue(); // The second element is the next node to visit.
                //System.out.println("SP_direction is " + SP_direction[i][j]);
            }
        }
    }

    public ArrayList<Integer> reconstructPath(int i, int j)
    {
        // Assumes that the next matrix has been set by the FW algorithm.

        if ( SP[i][j] == Double.POSITIVE_INFINITY )
	    {
            return null;
        }

        int intermediate = next[i][j];
        if ( intermediate == -1 )
	    {
            // The edge between them is their shortest path.
            ArrayList<Integer> sp = new ArrayList<Integer>();
            sp.add(new Integer(i));
            sp.add(new Integer(j));
            return sp;
        }
        else
	    {
            ArrayList<Integer> leftSP = reconstructPath(i,intermediate);
            ArrayList<Integer> rightSP = reconstructPath(intermediate,j);

            for ( int k = 1; k < rightSP.size(); k++ )
	        {
                leftSP.add((Integer)rightSP.get(k));
            }
            return leftSP;
        }
    }

    public void readEdges()
    {
        String line = "";
        int nlines = 0;
        try
	    {
            line = edge_br.readLine();
        }
        catch (Exception e)
	    {
            e.printStackTrace();
        }
        while (line != null)
	    {
            String fileData[] = line.split(",");
            int blockId = Integer.parseInt(fileData[0]);
            int nodeId1 = Integer.parseInt(fileData[6]);
            int nodeId2 = Integer.parseInt(fileData[7]);
            int nBlocks = Integer.parseInt(fileData[8]);
            int nTotal = Integer.parseInt(fileData[9]);

            if (nBlocks == 0)
	        {
                // It has no SFPark blocks.
                SFParkEdge e = new SFParkEdge(nodes.get(new Integer(nodeId1)),nodes.get(new Integer(nodeId2)),nBlocks,-1,-1,0,0);
                road.addEdge(e);
                edgeList[nlines] = e;
                //System.out.println("Added edge <" + nodeId1 +","+ nodeId2 +","+ nBlocks +",-1,-1>" + nlines);
            }
            else if (nBlocks == 1)
	        {
                // This edge has only one side of the street with SFPark sensors.
                SFParkEdge e = new SFParkEdge(nodes.get(new Integer(nodeId1)),nodes.get(new Integer(nodeId2)),nBlocks,blockId,-1,nTotal,0);
                road.addEdge(e);
                if (e == null)
	   	        {
                    System.out.println("There's a problem here AAAAAAAHHHHH!!!!!!!");
                }
                edgeList[nlines] = e;
                //System.out.println("Added edge <" + nodeId1 +","+ nodeId2 +","+ nBlocks +","+ blockId +",-1>");
            }
            else if (nBlocks == 2)
	        {
                // Both sides of the street have SFPark sensors.  We need to read the next block to get that info as well.
                String line2 = "";
                try
		        {
                    line2 = edge_br.readLine();
                }
                catch (Exception e)
		        {
                    e.printStackTrace();
                }
                String fileData2[] = line2.split(",");
                int blockId2 = Integer.parseInt(fileData2[0]);
                int nTotal2 = Integer.parseInt(fileData2[9]);
                SFParkEdge e = new SFParkEdge(nodes.get(new Integer(nodeId1)),nodes.get(new Integer(nodeId2)),nBlocks,blockId,blockId2,nTotal,nTotal2);
                road.addEdge(e);
                if (e == null)
	        	{
                    System.out.println("There's a problem here AAAAAAAHHHHH!!!!!!!");
                }
                edgeList[nlines] = e;
                //System.out.println("Added edge <" + nodeId1 +","+ nodeId2 +","+ nBlocks +","+ blockId +","+ blockId2 + ">");
            }
            else
	        {
	            // ERROR: nBlocks can only be 0,1, or 2.
                System.out.println("ERROR: nBlocks can only be 0, 1, or 2. Read wrong value from file.");
                System.exit(1);
            }
            
            nlines++;

            try
	        {
                line = edge_br.readLine();
            }
            catch (Exception e)
	        {
                e.printStackTrace();
            }
        }    
    }

    public void readNodes()
    {
        String line = "";
        try
	    {
            line = node_br.readLine();
        }
        catch (Exception e)
	    {
            e.printStackTrace();
        }
        while (line != null)
	    {
            String fileData[] = line.split(",");
            int nodeId = Integer.parseInt(fileData[0]);
            double latitude = Double.parseDouble(fileData[1]);
            double longitude = Double.parseDouble(fileData[2]);
            road.addNode(new SFParkNode(nodeId,latitude,longitude));            

            try
  	        {
                line = node_br.readLine();
            }
            catch (Exception e)
	        {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[])
    {
        SFParkRoadNetworkCreator s = new SFParkRoadNetworkCreator();
    }
}
