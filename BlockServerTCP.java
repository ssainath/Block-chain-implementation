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
import java.util.Scanner;

public class BlockServerTCP {

    public static void main(String args[]) {
        System.out.println("Server running");
        BlockChain bc = new BlockChain();   //block chain object
        //Creating genesis block and adding it to the chian
        Block genesisBlock = new Block(0, bc.getTime(), "Genesis", 2);
        bc.addBlock(genesisBlock);
        //map for json related processes
        HashMap<String, String> map = new HashMap<>();
        Socket clientSocket = null;
        try {
            int serverPort = 7777; // the server port we are using

            // Create a new server socket
            ServerSocket listenSocket = new ServerSocket(serverPort);


            //This while loop makes sure the server listens when the client is restarted
            while (true) {
                /*
                 * Block waiting for a new connection request from a client.
                 * When the request is received, "accept" it, and the rest
                 * the tcp protocol handshake will then take place, making
                 * the socket ready for reading and writing.
                 */
                clientSocket = listenSocket.accept();
                // If we get here, then we are now connected to a client.

                // Set up "in" to read from the client socket
                Scanner in;
                in = new Scanner(clientSocket.getInputStream());

                // Set up "out" to write to the client socket
                PrintWriter out;
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));

                /*
                 * Forever,
                 *   read a line from the socket, perform verifications, if passed,
                 *   parse it to find the operation to be performed
                 *  return the result of the operation(add, subtract, view)
                 */
                while (true) {
                    if (in.hasNext()) {
                        //request from client
                        String requestString = in.nextLine();
                        requestString = requestString.trim();
                        //To parse the request string
                        JSONParser parser = new JSONParser();
                        String sign = "";
                        String id = "";
                        String e = "";
                        String n = "";
                        String choice = "";
                        String data = "";
                        String diffOrIndex = "";
                        String message = "";
                        try {
                            //JSON Object to parse the request from client
                            JSONObject jsonOb =
                                (JSONObject)parser.parse(requestString);
                            //Extracting sign, id, e, n , choice
                            sign = (String) jsonOb.get("sign");
                            id = (String) jsonOb.get("id");
                            e = (String) jsonOb.get("e");
                            n = (String) jsonOb.get("n");
                            choice = (String) jsonOb.get("choice");
                            //Constructing message
                            message = id + "*" + e + "*" + n + "*" + choice;
                            //Message needs to have data and difficulty/id if the choice was to add to or corrupt chain
                            if (Integer.parseInt(choice) == 1 || Integer.parseInt(choice) == 4) {
                                data = (String) jsonOb.get("data");
                                diffOrIndex = (String) jsonOb.get("difficultyOrIndex");
                                message = message + "*" + diffOrIndex + "*" + data;
                            }
                        } //catching exception
                        catch (ParseException ex) {
                            ex.printStackTrace();
                        }

                        //Verifying if the sign matches
                        boolean verify1 = verify(message, sign);
                        //verifying if the id matches public key sent
                        boolean verify2 = verifyId(message);
                        String response = "";
                        //if both tests pass, process the request
                        if (verify1 && verify2) {
                            switch (Integer.valueOf(choice)) {
                                case 0: {   //Status of chain
                                    response = "Current size of chain: " + bc.getChainSize() +
                                            "\nCurrent hashes per second by this machine: " + bc.hashesPerSecond() +
                                            "\nDifficulty of most recent block: " + bc.getLatestBlock().getDifficulty() +
                                            "\nNonce for most recent block: " + bc.getLatestBlock().getNonce() +
                                            "\nChain hash: " + bc.getRecentHash();
                                    break;
                                }
                                case 1: {   //Adding new block
                                    bc.addBlock(new Block(bc.getChainSize(), bc.getTime(), data, Integer.parseInt(diffOrIndex)));
                                    break;
                                }
                                case 2: {   //Verifying chain
                                    if(bc.isChainValid()) { //if true, pass the verification result
                                        response = "Verifying entire chain" +
                                                "\nChain verification: " + bc.isChainValid();
                                    }
                                    else
                                    {//If false, pass the verification message, along with the error prompt
                                        response = "Verifying entire chain" + "\n" + bc.errorPrompt+
                                                "\nChain verification: " + bc.isChainValid();
                                    }
                                    break;
                                }
                                case 3:     //View the chain
                                {
                                    response = bc.toString();
                                    break;
                                }
                                case 4: //Corrupting chain
                                {
                                    bc.blockChain.get(Integer.parseInt(diffOrIndex)).setData(data);
                                   response = "Block "+diffOrIndex +" now holds "+data;
                                    break;
                                }
                                case 5: //Repair chain
                                {
                                    response = "Repairing the entire chain";
                                    bc.repairChain();
                                    break;
                                }
                            }
                        } else {    //Error in request if the verification fails
                            response = "Error in request";
                        }
                        //Sending response back to server
                        map.put("response",response);
                         out.println( new JSONObject(map).toString());
                        out.flush();
                    } else {       //break out of while loop if nothing is received from client
                        break;
                    }
                }
            } // Handle exceptions
        } catch (IOException e) {
            System.out.println("IO Exception:" + e.getMessage());

            //clean up sockets if open
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                // ignore exception on close
            }
        }
    }

    /**
     * Verifying id by forming the id from public key, and matching it with the key sent in the request
     *
     * @param message contains key, e and n
     * @return true/false if test passes/not passes
     */
    private static boolean verifyId(String message) {
        //parsing message to get id, e , n
        String[] s = message.split("\\*");
        String pubKey = s[1] + s[2];  //e+n
        String identity = s[0];
        String hashKey = hash(pubKey);   //hashing e+n
        int length = hashKey.length();
        String id = hashKey.substring(length - 40, length);    //Extracting last 20 bytes(40 nibbles in hexadecimal string)
        if (id.equals(identity)) //test passed if both ids match, else return false
        {
            return true;
        }
        return false;
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
     * Verify if the message sign is authentic
     * @param message message received
     * @param sign received
     * @return true if sign is valid, false if sign is invalid
     */
    private static boolean verify(String message, String sign) {
        // Take the encrypted string and make it a big integer
        BigInteger encryptedHash = new BigInteger(sign);
        // Decrypt it
        String[] s = message.split("\\*");
        BigInteger e = new BigInteger(s[1]);
        BigInteger n = new BigInteger(s[2]);
        BigInteger decryptedHash = encryptedHash.modPow(e, n);

        // Get the bytes from messageToCheck
        byte[] bytesOfMessageToCheck = new byte[0];
        try {
            bytesOfMessageToCheck = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        // compute the digest of the message with SHA-256
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }

        byte[] messageToCheckDigest = md.digest(bytesOfMessageToCheck);

        // messageToCheckDigest is a full SHA-256 digest
        // take two bytes from SHA-256 and add a zero byte
        byte[] extraByte = new byte[messageToCheckDigest.length + 1];
        extraByte[0] = 0;
        int j = 1;
        for (int i = 0; i < messageToCheckDigest.length; i++) {
            extraByte[j++] = messageToCheckDigest[i];
        }

        // Make it a big int
        BigInteger bigIntegerToCheck = new BigInteger(extraByte);
        // inform the client on how the two compare
        if (bigIntegerToCheck.compareTo(decryptedHash) == 0) {
            return true;
        } else {
            return false;
        }

    }
}

