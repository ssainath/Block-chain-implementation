package Task1;
/* Shreya Sainathan created on 2/19/2020 inside the package - task5 */


import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class BlockClientTCP {
    static Socket clientSocket = null;
    static PrintWriter out;
    static BufferedReader in;

    /**
     * This method signs the message using d
     * @param inString  message to sign
     * @param d component of key
     * @return sign
     */
    private static String sign(String inString, String d) {
        // compute the digest with SHA-256
        byte[] bytesOfMessage = new byte[0];
        try {
            bytesOfMessage = inString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] bigDigest = md.digest(bytesOfMessage);

        // we only want two bytes of the hash for BabySign
        // we add a 0 byte as the most significant byte to keep
        // the value to be signed non-negative.
        byte[] messageDigest = new byte[bigDigest.length + 1];
        int j = 0;
        messageDigest[j++] = 0;   // most significant set to 0
//        messageDigest[1] = bigDigest[0]; // take a byte from SHA-256
//        messageDigest[2] = bigDigest[1]; // take a byte from SHA-256

        for (int i = 0; i < bigDigest.length; i++) {
            messageDigest[j++] = bigDigest[i];
        }

        // The message digest now has three bytes. Two from SHA-256
        // and one is 0.

        // From the digest, create a BigInteger
        BigInteger m = new BigInteger(messageDigest);

        //Retrieving n and d
        BigInteger dInt = new BigInteger(d);
        String[] str = inString.split("\\*");
        BigInteger n = new BigInteger(str[2]);

        // encrypt the digest with the private key
        BigInteger c = m.modPow(dInt, n);

        // return this as a big integer string
        return c.toString();
    }


    /**
     * This method starts the program by creating connections to server
     * @param args
     */
    public static void main(String args[]) {
        System.out.println("Client running");
        try {
            int serverPort = 7777;  //port number for server connection
            clientSocket = new Socket("localhost", serverPort); //connect to localhost on 7777

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));  //To get stream from server
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));  //to send stream to server

            Scanner in = new Scanner(System.in);    //To read input from console
            //Splitting the result into e,d,n to print the public and private keys
            String[] keys = generateID().split("\\*");
            //keys[0]=e , keys[1]= d, keys[2]=n
            System.out.println("Private key formed by ordered pair of " + keys[1] + " and " + keys[2]);
            System.out.println("Public key formed by ordered pair of " + keys[0] + " and " + keys[2]);

            //Concatenating e and n to compute hash
            String pubKey = keys[0] + keys[2];
            //leastSigTwenty extracts the first 40 nibbles for making ID
            String id = leastSigTwenty(pubKey);
            while (true) {
                //Accept operation from user
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
                    case 0: //View block chain status, send id, e,n,choice to server, d for signature
                    case 3: //View the chain, send id, e,n,choice to server, d for signature
                    {
                        send(id, keys[0], keys[2], choice, keys[1]);
                        break;
                    }
                    case 1:
                    {   //Adding a transaction
                        //Send id, e, n, choice, difficulty , data to sever, d for sign
                        long start = System.currentTimeMillis();
                        System.out.println("Enter difficulty>0");   //difficulty
                        int difficulty = Integer.parseInt(in.nextLine().trim());
                        System.out.println("Enter transaction");       //data
                        String txn = in.nextLine();
                        send(id,keys[0],keys[2],choice,keys[1], difficulty,txn);
                        long end = System.currentTimeMillis();
                        System.out.println("Total execution time to add this block was "+ (end - start) +" milliseconds");
                        break;
                    }
                    case 2: //Verify the chain
                    {   //Send id, e,n,choice to server, d for signature
                        long start = System.currentTimeMillis();
                        send(id, keys[0], keys[2], choice, keys[1]);
                        long end = System.currentTimeMillis();
                        System.out.println("Total time to verify chain was "+ (end - start) +" milliseconds");
                        break;
                    }
                    case 4: //Corrupt the chain
                    {    //Send id, e, n, choice, difficulty , data to sever, d for sign
                        System.out.println("Corrupt the Blockchain");
                        System.out.print("Enter block ID of block to corrupt - ");
                        int blockId = Integer.parseInt(in.nextLine().trim());   //id to corrupt
                        System.out.println("Enter new data for block "+blockId);
                        String data = in.nextLine();    //data to fill
                        send(id,keys[0],keys[2],choice,keys[1], blockId,data);
                        break;
                    }
                    case 5: //Chain repair
                    { //Send id, e, n, choice, difficulty , data to sever, d for sign
                        long start = System.currentTimeMillis();
                        send(id, keys[0], keys[2], choice, keys[1]);
                        long end = System.currentTimeMillis();
                        System.out.println("Total execution time required to repair the chain was "+(end-start)+" milliseconds");
                        break;
                    }
                    case 6:
                    {   //Exit, close socket
                        clientSocket.close();
                        System.out.println("Thank you. Program exiting");
                        return;
                    }
                    default:    //random input- anything but 0 to 6
                    {
                        System.out.println("Enter valid number between 0 to 6.");
                        break;
                    }
                }
            }
        }
        //handling exception
        catch (IOException e) {
            System.out.println("IO Exception:" + e.getMessage());
            e.printStackTrace();
        } finally {
            try {//closing socket if open
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * method to send request for choices 1 and 4
     * @param id ID for the client session
     * @param e component of key
     * @param n component of key
     * @param choice choice of user
     * @param d component of key
     * @param difficultyOrIndex difficulty if choice is 1 and index if choice is 4
     * @param txn data
     */
    private static void send(String id, String e, String n, int choice, String d, int difficultyOrIndex, String txn) {
        HashMap<String,String> map = new HashMap<>();
        //Adding id, e,n, choice, difficultyOrIndex, data to map
        map.put("id",id);
        map.put("e",e);
        map.put("n",n);
        map.put("choice",String.valueOf(choice));
        map.put("difficultyOrIndex", String.valueOf(difficultyOrIndex));
        map.put("data", String.valueOf(txn.toLowerCase()));
        //Constructing message for sign
        String inString = id + "*" + e + "*" + n + "*" + String.valueOf(choice) + "*" + difficultyOrIndex +"*"+txn;
        inString = inString.toLowerCase();
        String sign = sign(inString, d);
        //Adding sign to map
        map.put("sign",sign);
        //Sending the JSON string to server side
        out.println(new JSONObject(map).toString());
        out.flush();
        String data = null; // read a line of data from the stream from server
        try {
            data = in.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //To extract response from server
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonOb= (JSONObject)(parser.parse(data));
            String response = (String)jsonOb.get("response");
            if(!response.isBlank()) {
                System.out.println(response);
            }//Print response
        } catch (ParseException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * for sending request for choices 0,2,3,5
     * @param id
     * @param e
     * @param n
     * @param choice
     * @param d
     */
    private static void send(String id, String e, String n, int choice, String d) {
        HashMap<String,String> map = new HashMap<>();
        //Adding id, e, n and choice to map
        map.put("id",id);
        map.put("e",e);
        map.put("n",n);
        map.put("choice",String.valueOf(choice));
        //Constructine message for signing
        String inString = id + "*" + e + "*" + n + "*" + choice;
        inString = inString.toLowerCase();
        String sign = sign(inString, d);
        //Adding sign to map
        map.put("sign",sign);
        //Sending message to server
        out.println(new JSONObject(map).toString());
        out.flush();
        String data = null; // read a line of data from the stream
        try {
            data = in.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //To extract response from server
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonOb= (JSONObject)(parser.parse(data));
            String response = (String)jsonOb.get("response");
            if(!response.isBlank())
            System.out.println(response);   //Print response from server
        } catch (ParseException ex) {
            ex.printStackTrace();
        }



    }

    /**
     * This method computes the least significnt 20 bytes from the SHA-256 hash of concatenation of e and n
     *
     * @param pubKey
     * @return the user id for each session
     */
    private static String leastSigTwenty(String pubKey) {

        String hashKey = hash(pubKey);
        int length = hashKey.length();
        return hashKey.substring(length - 40, length);
    }

    /**
     * @param pubKey concatenation of e and n
     * @return hash string of the input(sha-256)
     */
    private static String hash(String pubKey) {
        try {
            // Create a SHA256 digest
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");
            // allocate room for the result of the hash
            byte[] hashBytes;
            // perform the hash
            digest.update(pubKey.getBytes("UTF-8"), 0, pubKey.length());
            // collect result
            hashBytes = digest.digest();
            return convertToHex(hashBytes);
        } catch (NoSuchAlgorithmException nsa) {
            System.out.println("No such algorithm exception thrown " + nsa);
        } catch (UnsupportedEncodingException uee) {
            System.out.println("Unsupported encoding exception thrown " + uee);
        }
        return null;
    }

    /**
     * this method converts the input byte array formed by message digest of sha-256 into a hexadecimal string
     *
     * @param hashBytes
     * @return hexadecimal string
     */
    private static String convertToHex(byte[] hashBytes) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < hashBytes.length; i++) {
            int halfbyte = (hashBytes[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = hashBytes[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * This method computes the RSA keys
     *
     * @return Generates a string which comprises of e,d, n delimited by *
     */
    private static String generateID() {
        // Each public and private key consists of an exponent and a modulus
        BigInteger n; // n is the modulus for both the private and public keys
        BigInteger e; // e is the exponent of the public key
        BigInteger d; // d is the exponent of the private key

        Random rnd = new Random();

        // Step 1: Generate two large random primes.
        // We use 400 bits here, but best practice for security is 2048 bits.
        // Change 400 to 2048, recompile, and run the program again and you will
        // notice it takes much longer to do the math with that many bits.
        BigInteger p = new BigInteger(400, 100, rnd);
        BigInteger q = new BigInteger(400, 100, rnd);

        // Step 2: Compute n by the equation n = p * q.
        n = p.multiply(q);

        // Step 3: Compute phi(n) = (p-1) * (q-1)
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        // Step 4: Select a small odd integer e that is relatively prime to phi(n).
        // By convention the prime 65537 is used as the public exponent.
        e = new BigInteger("65537");

        // Step 5: Compute d as the multiplicative inverse of e modulo phi(n).
        d = e.modInverse(phi);

        //Returns e,d,n by delimiting with *
        return e + "*" + d + "*" + n;
    }
}

