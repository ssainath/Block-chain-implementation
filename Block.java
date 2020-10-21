package Task0;
/* Shreya Sainathan created on 3/5/2020 inside the package - Task0 */

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.HashMap;
import org.json.JSONObject;


public class Block {

    //position of block on the chain
    private int index;
    //time the block was created
    private java.sql.Timestamp timestamp;
    //transaction details to be stored in the block
    private String data;
    //hash of previous block
    private String previousHash;
    //value determined by proof of work done
    private BigInteger nonce;
    //specifies the exact number of left most hex digits needed by a proper hash
    private int difficulty;

    /**
     * constructor
     * @param index
     * @param timestamp
     * @param data
     * @param difficulty
     */
    Block(int index, java.sql.Timestamp timestamp,String data, int difficulty)
    {
        this.index=index;
        this.timestamp=timestamp;
        this.data=data;
        this.difficulty=difficulty;
    }

    /**
     * computes a hash of the concatenation of the index, timestamp, data, previousHash, nonce, and difficulty
     * @return  hash in a String holding Hexadecimal characters
     */
    public String calculateHash()
    {
        //concatenate index, timestamp, data, previoushash, nonce and difficulty
        String input = String.valueOf(index) + timestamp + data+ previousHash+ nonce+ difficulty;
        String hash = new String();
        byte[] mdArray = null;
        try {
            //Digesting the input
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            mdArray = md.digest(input.getBytes());
        }//catching exception for getInstance method
        catch ( NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //converting byte array to hexadecimal
        hash = javax.xml.bind.DatatypeConverter.printHexBinary(mdArray);
        return hash;
    }

    /**
     * Getter method for data
     * @return data field
     */
    public String getData()
    {
        return this.data;
    }

    /**
     * Getter method for difficulty
     * @return difficulty field
     */
    public int getDifficulty()
    {
        return this.difficulty;
    }

    /**
     * Getter method for index
     * @return index field
     */
    public int getIndex()
    {
        return this.index;
    }

    /**
     * Getter method for nonce
     * @return nonce field
     */
    public BigInteger getNonce()
    {
        return this.nonce;
    }

    /**
     * Getter method for previous hash
     * @return previousHash field
     */
    public java.lang.String getPreviousHash()
    {
       return this.previousHash;
    }

    /**
     * Getter method for time stamp
     * @return timestamp field
     */
    public java.sql.Timestamp getTimestamp()
    {
        return this.timestamp;
    }

    /**
     * @param args
     */
    public static void main(java.lang.String[] args)
    {

    }

    /**
     * Calls the calculateHash method
     * It increments the nonce until it produces a good hash.
     * A good hash is the one with leading zeros same as difficulty
     * @return the good hash
     */
    public String proofOfWork()
    {
        this.nonce=BigInteger.ZERO;
        //calculating hash first time
        String hash = this.calculateHash();
        while(!isGoodHash(hash))   //for every time the hash isn't good, loop again
        {
            nonce = nonce.add(BigInteger.ONE);
            hash= this.calculateHash();
        }
        return hash;
    }

    /**
     * Takes a substring of the first characters, the length corresponding to difficulty
     * checks if the characters are all zero, returns true if yes. If not, false
     * @param hashValue the hash string
     * @return true if hash has the correct number of leading zeros, false if not
     */
    private boolean isGoodHash(String hashValue)
    {
        String lead = hashValue.substring(0, this.difficulty+1);
        for(char c: lead.toCharArray())
        {
            if(c!='0')
            {
                return false;
            }
        }
        return true;
    }

    /**
     * sets the Data member
     * @param data value to set to the member data
     */
    public void setData(String data)
    {
        this.data = data;
    }

    /**
     * sets the difficulty member
     * @param difficulty value to set to the member difficulty
     */
    public void setDifficulty(int difficulty)
    {
        this.difficulty = difficulty;
    }

    /**
     * sets the Index member
     * @param index value to set to the member Index
     */
    public void setIndex(int index)
    {
        this.index = index;
    }

    /**
     * sets the previousHash member
     * @param previousHash value to set to the member previousHash
     */
    public void setPreviousHash(String previousHash)
    {
        this.previousHash = previousHash;
    }

    /**
     * sets the timestamp member
     * @param timestamp value to set to the member timestamp
     */
    public void setTimestamp(Timestamp timestamp)
    {
        this.timestamp = timestamp;
    }

    /**
     *  overrides Java's toString method
     * @return JSON representation of block's data
     */
    @Override
    public java.lang.String toString()
    {
        //Adding to map
        HashMap<String,String> map = new HashMap<>();
        map.put("index", String.valueOf(this.index).trim());
        map.put("timestamp", this.timestamp.toString());
        map.put("data", this.data);
        map.put("previousHash", this.previousHash);
        map.put("nonce", this.nonce.toString());
        map.put("difficulty", String.valueOf(this.difficulty));
        //Converting contents of map to json string
        String jsonString = new JSONObject(map).toString();
        return jsonString;
    }

}
