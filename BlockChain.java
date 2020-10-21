package Task0;
/* Shreya Sainathan created on 3/5/2020 inside the package - Task0 */

import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.sql.Timestamp;

public class BlockChain {

    //list to hold blocks
    private List<Block> blockChain;
    //hash of most recent block
    private String recentHash;

    /**
     * constructor for blockchain class
     */
    BlockChain()
    {
        blockChain = new ArrayList<>();
        recentHash = new String();
    }

    /**
     * Sets the previous hash of the new block to the recent hash of block chain object
     * Add a new block to the block chain
     * call the proof of work method for the new block
     * @param newBlock
     */
    public void addBlock(Block newBlock)
    {
        newBlock.setPreviousHash(this.recentHash);
        this.recentHash=newBlock.proofOfWork();
        blockChain.add(newBlock);
    }

    /**
     * @return the size of the block chain
     */
    public int getChainSize()
    {
        return this.blockChain.size();
    }

    /**
     * @return the last block of the chain
     */
    public Block getLatestBlock()
    {
        return this.blockChain.get(this.getChainSize()-1);
    }

    /**
     * @return the current system time
     */
    public java.sql.Timestamp getTime()
    {
        //Object of Date class
        Date date= new Date();
        //Current time in milliseconds
        long time = date.getTime();
        return new Timestamp(time);
    }

    /**
     * uses 00000000 string to see the hashes computed by the system per second
     * @return the number of hashes per second computed by the system
     */
    public int hashesPerSecond()
    {
        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();

        String input = "00000000";
        String hash;
        byte[] mdArray = null;
        int count = 0;
        try {
            //Digesting the input
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            while(endTime - startTime < 1000)
            {
                mdArray = md.digest(input.getBytes());
                count++;
                endTime = System.currentTimeMillis();
            }
                    }//catching exception for getInstance method
        catch ( NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //converting byte array to hexadecimal
        hash = javax.xml.bind.DatatypeConverter.printHexBinary(mdArray);
        //returning the number of hashes per second
        return (int) (count/(endTime-startTime));
    }

    /**
     * for each block it checks if the hash is same as the previous hash of next block,
     * for last blocks it checks if the hash is same as the chain hash
     * It also checks if the number of leading zeroes in the hash is same as the
     * difficulty of the block
     * For a chain with only genesis block, it checks if the hash of the block is same as the chain hash
     * @return true if chain is valie, false if invalid
     */
    public boolean isChainValid()
    {
        if(this.blockChain.size()==0)
        {
            return false;
        }
        //For every block in the list, do the check
         for(int i=0;i<blockChain.size();i++)
        {
            //the hash of the block, this hash will be the final hash after the Proof of work is done
            //proof of work is done while the block is added to the chain
            String currentHash = blockChain.get(i).calculateHash();
            //Getting the zeroes string required to be at the front
            StringBuilder zeroPad = new StringBuilder();
            for(int x=0;x<blockChain.get(i).getDifficulty();x++)
            {
                zeroPad.append(0);
            }
            //Check the number of zeroes in the block
            //return false if the number of zeroes dont match
            for(int j=0;j<blockChain.get(i).getDifficulty();j++)
            {
                if(currentHash.charAt(j)!='0')
                {
                    System.out.println("..Improper hash on node "+i+ " Does not begin with "+zeroPad);
                    return false;
                }
            }

            //if the block is last, check with hash with the recent hash of the chain
            if(i==blockChain.size()-1)
            {
                if(!currentHash.equals(this.recentHash))
                {
                    System.out.println("..Improper hash referenced to node "+i+ " Does not begin with "+zeroPad);
                    return false;
                }
            }
            else    //if the block is not the last, check the hash with the previous hash field of the next block
            {
                if(!currentHash.equals(blockChain.get(i+1).getPreviousHash()))
                {
                    return false;
                }
            }
        }
         //if false is not sent for any of the block, the method returns true, meaning the block chain is valid
        return true;
    }

    /**
     * Repairs a chain which is corrupted
     * Identifies at which node the chain is breaking, ad fixes the chain from there till the end
     */
    public void repairChain()
    {
        int faultyBlock=-1;
        //For every block in the list, do the check
        for(int i=0;i<blockChain.size();i++)
        {
            //the hash of the block, this hash will be the final hash after the Proof of work is done
            //proof of work is done while the block is added to the chain
            String currentHash = blockChain.get(i).calculateHash();
            //Check the number of zeroes in the block
            //return false if the number of zeroes dont match
            boolean mismatch=false;
            for(int j=0;j<blockChain.get(i).getDifficulty();j++)
            {
                if(currentHash.charAt(j)!='0')
                {
                    mismatch=true;
                    break;
                }
            }
            if(mismatch)
            {
                faultyBlock=i;
                break;
            }
            //if the block is last, check with hash with the recent hash of the chain
            if(i==blockChain.size()-1)
            {
                if(!currentHash.equals(this.recentHash))
                {
                    faultyBlock=i;
                    break;
                }
            }
            else    //if the block is not the last, check the hash with the previous hash field of the next block
            {
                if(!currentHash.equals(blockChain.get(i+1).getPreviousHash()))
                {
                    faultyBlock=i;
                    break;
                }
            }
        }
        //If a faulty block was identified, then repair the chain from that block till the end
        if(faultyBlock>=0)
        {
            for(int i=faultyBlock;i<this.blockChain.size();i++)
            {
                //compute proof of work, and assign the result hash to currentHash
                String currentHash = blockChain.get(i).proofOfWork();
                //if block is last, assign the currentHash to the recentHash of the blockchain class
                if(i==blockChain.size()-1)
                {
                    this.recentHash = currentHash;
                }
                //if block is not last, assign currentHash to the previous hash of next block
                else
                {
                    this.blockChain.get(i+1).setPreviousHash(currentHash);
                }
            }
        }
    }

    /**
     * concatenates the result of toString from each block, concatenates them for the ds_chain
     * @return the json format of the chain
     */
    @Override
    public String toString()
    {
        //map to hold the details for json output
        HashMap<String,String> map = new LinkedHashMap<>();
        //building the value for ds_chain
        StringBuilder ds_chain = new StringBuilder("[");
        for(int i=0;i<this.blockChain.size();i++)
        {
            ds_chain.append(this.blockChain.get(i).toString()); //concatenate the toString of each block
            if(i==blockChain.size()-1)  //append ] for last block
            {
                ds_chain.append("]");
            }
            else        //append , for other blocks
            {
                ds_chain.append(",");
            }
        }
        map.put("chainHash", this.recentHash);
        map.put("ds_chain", ds_chain.toString());

        //Converting contents of map to json string
        String jsonString = new JSONObject(map).toString();
        return jsonString.replaceAll("\\\\", "");
    }


    public static void main(java.lang.String[] args)
    {
        //blockchain object
        BlockChain bc = new BlockChain();

        //Adding Genesis block
        Block genesisBlock = new Block(0, bc.getTime(), "Genesis", 2);
        bc.addBlock(genesisBlock);

        Scanner in = new Scanner(System.in);
        while(true)
        {
            //options 0 to 6
            System.out.println("0. View basic blockchain status.");
            System.out.println("1. Add a transaction to the blockchain.");
            System.out.println("2. Verify the blockchain.");
            System.out.println("3. View the blockchain.");
            System.out.println("4. Corrupt the chain.");
            System.out.println("5. Hide the Corruption by recomputing hashes.");
            System.out.println("6. Exit.");
            int choice = Integer.parseInt(in.nextLine().trim());
            switch(choice)
            {
                case 0:     //View status
                {
                   System.out.println("Current size of chain: "+ bc.getChainSize());
                   System.out.println("Current hashes per second by this machine: "+bc.hashesPerSecond());
                   System.out.println("Difficulty of most recent block: "+bc.getLatestBlock().getDifficulty());
                   System.out.println("Nonce for most recent block: "+bc.getLatestBlock().getNonce());
                   System.out.println("Chain hash: "+bc.recentHash);
                   break;
                }
                case 1: //add a transaction
                {
                    long start = System.currentTimeMillis();
                    System.out.println("Enter difficulty>0");   //difficulty
                    int difficulty = Integer.parseInt(in.nextLine().trim());
                    System.out.println("Enter transaction");    //transaction/data
                    String txn = in.nextLine();
                    bc.addBlock(new Block(bc.getChainSize(), bc.getTime(), txn, difficulty));
                    long end = System.currentTimeMillis();
                    System.out.println("Total execution time to add this block was "+ (end - start) +" milliseconds");
                    break;
                }
                case 2: //Verifying chain
                {
                    System.out.println("Verifying entire chain");
                    long start = System.currentTimeMillis();
                    System.out.println("Chain verification: "+ bc.isChainValid());
                    long end = System.currentTimeMillis();
                    System.out.println("Total time to verify chain was "+ (end - start) +" milliseconds");
                    break;
                }
                case 3:     //View chain
                {
                    System.out.println(bc.toString());
                    break;
                }
                case 4:     //Corrupt the chain
                {
                    System.out.println("Corrupt the Blockchain");
                    System.out.print("Enter block ID of block to corrupt - ");  //index to corrupt
                    int id = Integer.parseInt(in.nextLine().trim());
                    System.out.println("Enter new data for block "+id);     //new data
                    String data = in.nextLine();
                    bc.blockChain.get(id).setData(data);        //setting data
                    System.out.println("Block "+id +" now holds "+data);
                    break;
                }
                case 5: //Repair chain
                {
                    System.out.println("Repairing the entire chain");
                    long start = System.currentTimeMillis();
                    bc.repairChain();   //calling the repair chain method
                    long end = System.currentTimeMillis();
                    System.out.println("Total execution time required to repair the chain was "+(end-start)+" milliseconds");
                    break;
                }
                case 6:     //Exit
                {
                    return;
                }
                default:    //default
                {
                    System.out.println("Enter valid number between 0 to 6.");
                    break;
                }
            }


        }

    }



}
