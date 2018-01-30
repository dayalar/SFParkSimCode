import java.util.HashMap;

public class SFParkNetwork
{
    private HashMap<Integer,SFParkNode> nodes;  // Indexed by NodeId 
    private SFParkEdge edges[][];           // Used to save the edges between the nodes.
    private double edgeWeights[][];         // Used to save the distances between the nodes.

    public static int nodeIdStart = 7001;         // Used to convert the nodeId's to start at 0.  
                                            // We assume that the nodes have consecutive id's.

    private int nNodes = 0;
    private int nEdges = 0;

    public SFParkNetwork(int n)
    {
        nodes = new HashMap<Integer,SFParkNode>();
        edges = new SFParkEdge[n][n];
        edgeWeights = new double[n][n];
        for ( int i = 0; i < n; i++ )
	{
            for ( int j = 0; j < n; j++ )
	    {
                edges[i][j] = null;
                edgeWeights[i][j] = Double.POSITIVE_INFINITY;
            }
        }
    }

    public void addNode(SFParkNode node)
    {
        Integer nodeId = new Integer(node.getId());
        nodes.put(nodeId,node);
        nNodes++;
    }
    
    public void addEdge(SFParkEdge edge)
    {
        SFParkNode node1 = edge.getNode1();
        SFParkNode node2 = edge.getNode2();

        int nodeId1 = node1.getId()-nodeIdStart;
        int nodeId2 = node2.getId()-nodeIdStart;

        edges[nodeId1][nodeId2] = edge;
        edges[nodeId2][nodeId1] = edge;
        edgeWeights[nodeId1][nodeId2] = Spatial.distance(node1,node2);
        edgeWeights[nodeId2][nodeId1] = Spatial.distance(node1,node2);
        nEdges++;

        //System.out.println(nodeId1 + " -> " + nodeId2 + " " + edgeWeights[nodeId1][nodeId2]);
    }

    public HashMap<Integer,SFParkNode> getNodes()
    {
        return nodes;
    }

    public SFParkEdge[][] getEdges()
    {
        return edges;
    }

    public double[][] getEdgeWeights()
    {
        return edgeWeights;
    }
}
