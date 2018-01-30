public class SFParkEdge
{
    private SFParkNode node1, node2;
    private int nBlocks;
    private int blockId1,blockId2;
    private int total1,total2;

    public SFParkEdge(SFParkNode n1, SFParkNode n2, int nB, int bId1, int bId2, int tot1, int tot2)
    {
        node1 = n1;
        node2 = n2;

        if (nB == 0)
	    {
            nBlocks = nB;
            if ( (bId1 == -1) && (bId2 == -1) )
	        {
                blockId1 = bId1;
                blockId2 = bId2;
                total1 = tot1;
                total2 = tot2;
            }
            else
	        {
                System.out.println("SFParkEdge should have had blockId's of -1 because it has 0 SFPark blocks.");
                System.exit(1);
            }
        }
        else if (nB == 1)
	    {
            nBlocks = nB;
            if ( (bId1 != -1) && (bId2 == -1) )
	        {
                blockId1 = bId1;
                blockId2 = bId2;
                total1 = tot1;
                total2 = tot2;
            }
            else
	        {
                System.out.println("SFParkEdge should have had blockId2 of -1 because it has only 1 SFPark block.");
                System.exit(1);
            }
        }
        else if (nB == 2)
	    {
            nBlocks = nB;
            if ( (bId1 != -1) && (bId2 != -1) )
	        {
                blockId1 = bId1;
                blockId2 = bId2;
                total1 = tot1;
                total2 = tot2;
            }
            else
	        {
                System.out.println("SFParkEdge should have had blockId's not equal to -1.  They both should be a valid id.");
                System.exit(1);
            }
        }
        else
	    {
            // nB should have been 0, 1, or 2. Raise an ERROR!!
            System.out.println("nBlocks can only have a value of 0, 1, or 2.");
            System.exit(1);
        }
    }

    public SFParkNode getNode1()
    {
        //System.out.println("Hey guys 1!!");
        return node1;
    }

    public SFParkNode getNode2()
    {
        //System.out.println("Hey guys 2!!");
        return node2;
    }

    public int getNBlocks()
    {
        return nBlocks;
    }

    public int getBlockId1()
    {
        return blockId1;
    }

    public int getBlockId2()
    {
        return blockId2;
    }

    public int getTotal1()
    {
        return total1;
    }

    public int getTotal2()
    {
        return total2;
    }


    public String toString()
    {
        String s = "\tEdge: \n\tnode1 = " + node1.toString() + "\n\tnode2 = " + node2.toString() + "\n\tnBlocks = " + nBlocks + ", blockId1 = " + blockId1 + ", total1 = " + total1 + ", blockId2 = " + blockId2 + ", total2 = " + total2 + "\n";
        return s;  
    }
}
