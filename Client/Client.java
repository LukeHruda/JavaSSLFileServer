import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client {
    public static void main(String[] args) throws IOException{
        
        //Set the group password based off the keystore file
        System.setProperty("javax.net.ssl.trustStore","group.18");
        Socket socket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket("localhost",4444);

        //generate the socket writer and reader, along with command line reader
        BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        //ask the user if they would like to login or create
        System.out.println("Login or Create?");
        String response = "";
        boolean valid = false;
        boolean login = false;
        String hashPassword = "";
        while(!valid)
        {
            //chech the user prompt
            response = userInput.readLine();
            if(response.equals("login"))
            {
                //tell the server someone is logging in
                printWriter.println(response);
                while(!login)
                {
                    //get the user to enter their username and password
                    System.out.println("Please enter your username");
                    String userName = "";
                    String password = "";
                    userName = userInput.readLine();
                    System.out.println("Please enter your Password");
                    password = userInput.readLine();
                    //hash the password, immedately drop the plaintext password from memory
                    hashPassword = sha512(password);
                    password = null;
                    //send the server the username and hashed password
                    printWriter.println(userName);
                    printWriter.println(hashPassword);

                    response = socketBufferedReader.readLine();

                    if(response.equals("success"))
                    {
                        System.out.println("Login Successful");
                        valid = true;
                        login = true;
                    }
                    else
                    {
                        System.out.println("Login Failed, incorrect username or password");
                    }
                }
                
            }
            //user would like to create a new account
            else if(response.equals("create"))
            {
                //tell the server a new account is incoming
                printWriter.println(response);
                //user enters a username and a password with confirmation
                System.out.println("Please enter a username");
                String userName = "";
                String password = "";
                String passwordCon = "";
                userName = userInput.readLine();
                System.out.println("Please enter your Password");
                password = userInput.readLine();
                System.out.println("Please confirm your Password");
                passwordCon = userInput.readLine();
                while(!password.equals(passwordCon)){
                    System.out.println("Passwords do not match.\nEnter try again.");
                    passwordCon = userInput.readLine();
                }
                //hash the user password, send username and hash to server for storage
                hashPassword = sha512(password);
                printWriter.println(userName);
                printWriter.println(hashPassword);
                password = null;
                valid = true;
            }
            else{
                System.out.println("Command not recognized");
            } 
        }
        
        String message = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        OutputStream out = socket.getOutputStream();
        while(true){
            //ask the user to enter a command
            System.out.println("Please enter a command.");
            message = userInput.readLine().toLowerCase();
            //if the command is quit
            if(message.equals("quit")){
                //tell the server we are leaving, close all data streams and close the socket
                printWriter.println(message);
                out.close();
                socket.close();
                bis.close();
                fis.close();
                System.exit(0);
            }
            //upload file code
            else if(message.equals("upload")){
                //tell the server we want to upload a file and whether it will be general or private encryption
                printWriter.println(message);
                System.out.println("general or private?");
                message = userInput.readLine();
                printWriter.println(message);

                //get the file name from the user, tell the server the file name and size
                String fileName = userInput.readLine();
                printWriter.println(fileName);
                File myFile = new File (fileName);
                printWriter.println(myFile.length());
                byte [] mybytearray  = new byte [(int)myFile.length()];
                fis = new FileInputStream(myFile);
                bis = new BufferedInputStream(fis);
                bis.read(mybytearray,0,mybytearray.length);

                //generate the initialization vector for CFB-512 bit encryption
                byte[] IV = new byte[64];
                //if it is a private file we generate the IV off the 512 bit hash of the user password so that only this user can decrypt it
                if(message.equals("private"))
                {
                    for (int i = 0; i < 128; i += 2) {
                        IV[i / 2] = (byte) ((Character.digit(hashPassword.charAt(i), 16) << 4) + Character.digit(hashPassword.charAt(i+1), 16));
                    }
                }
                //if it is a general file we generate the IV off the 512 bit hash of the word general so that any user can decrypt it
                else if(message.equals("general"))
                {
                    String generalHash = sha512("general");
                    for (int i = 0; i < 128; i += 2) {
                        IV[i / 2] = (byte) ((Character.digit(generalHash.charAt(i), 16) << 4) + Character.digit(generalHash.charAt(i+1), 16));
                    }
                }
                //generate the key based on the 512 hash of the file name
                String hashFile = sha512(fileName);
                byte [] key  = new byte [64];
                for (int i = 0; i < 128; i += 2) {
                    key[i / 2] = (byte) ((Character.digit(hashFile.charAt(i), 16) << 4) + Character.digit(hashFile.charAt(i+1), 16));
                }

                //generate the block cipher by doing the XOR of the IV and KEY 
                byte [] blockCypher  = new byte [64];
                for(int i = 0; i<64; i++)
                {
                    blockCypher[i] = (byte) (key[i]^IV[i]);
                }
                System.out.println("Initialization Vector:");
                for(int i = 0; i<64; i++)
                {
                    System.out.print(Byte.toUnsignedInt(IV[i])+" ");
                }
                System.out.println();
                System.out.println("Key:");
                for(int i = 0; i<64; i++)
                {
                    System.out.print(Byte.toUnsignedInt(key[i])+" ");
                }
                System.out.println();
                System.out.println("Block Cipher:");
                for(int i = 0; i<64; i++)
                {
                    System.out.print(Byte.toUnsignedInt(blockCypher[i])+" ");
                }
                System.out.println();
                //create arrays for storage
                byte [] finalarray  = new byte [(int)myFile.length()];
                byte [] cipherarray  = new byte [64];

                //calc # of 64 byte chunks
                int chunks = (int)Math.ceil(myFile.length()/64);
                int count = 0;
                while(count < chunks){
                    //encrypt 64 bytes at a time and append them to the final array
                    for(int i = 0; i<64;i++)
                    {
                        cipherarray[i] = (byte) (mybytearray[i+64*count]^blockCypher[i]);
                        finalarray[i+64*count] = cipherarray[i];
                    }
                    //reset the block cipher by XOR the cipher text with the key again to create new block cipher
                    for(int i = 0; i<64;i++)
                    {
                        blockCypher[i]=(byte)(cipherarray[i]^key[i]);
                    }
                    //reset the cipher array and incrememnt loop counter
                    cipherarray  = new byte [64];
                    count++;
                }
                //create output stream
                os = socket.getOutputStream();
                System.out.println("Sending " + fileName + "(" + mybytearray.length + " bytes)");
                //write the file and flush the output stream
                os.write(finalarray,0,mybytearray.length);
                os.flush();
                os.close();
                System.out.println("Done.");
                
            }
            //user would like to download a file
            else if(message.equals("download")){
                //tell the server we want to upload a file and whether it will be general or private encryption
                printWriter.println(message);
                System.out.println("general or private?");
                message = userInput.readLine();
                printWriter.println(message);
                //get the file name from the user, tell the server the file name
                String fileName = userInput.readLine();
                printWriter.println(fileName);
                //get the file size expected from the server
                String fileBytes = socketBufferedReader.readLine();
                int fileSize = Integer.parseInt(fileBytes);
                int bytesRead;
                int current = 0;
                FileOutputStream fos = null;
                // receive file into a temporary array
                byte [] mybytearray  = new byte [fileSize];
                InputStream is = socket.getInputStream();
                fos = new FileOutputStream(fileName);
                bytesRead = is.read(mybytearray,0,mybytearray.length);
                current = bytesRead;
                for(int i = fileSize; i >=0; i--)
                {
                    bytesRead = is.read(mybytearray, current, (mybytearray.length-current));
                    if(bytesRead >= 0) current += bytesRead;
                }
                //generate the initialization vector for CFB-512 bit encryption
                byte[] IV = new byte[64];
                //if it is a private file we generate the IV off the 512 bit hash of the user password so that only this user can decrypt it
                if(message.equals("private"))
                {
                    for (int i = 0; i < 128; i += 2) {
                        IV[i / 2] = (byte) ((Character.digit(hashPassword.charAt(i), 16) << 4) + Character.digit(hashPassword.charAt(i+1), 16));
                    }
                }
                //if it is a general file we generate the IV off the 512 bit hash of the word general so that any user can decrypt it
                else if(message.equals("general"))
                {
                    String generalHash = sha512("general");
                    for (int i = 0; i < 128; i += 2) {
                        IV[i / 2] = (byte) ((Character.digit(generalHash.charAt(i), 16) << 4) + Character.digit(generalHash.charAt(i+1), 16));
                    }
                }
                //generate the key based on the 512 hash of the file name
                String hashFile = sha512(fileName);
                byte [] key  = new byte [64];
                for (int i = 0; i < 128; i += 2) {
                    key[i / 2] = (byte) ((Character.digit(hashFile.charAt(i), 16) << 4) + Character.digit(hashFile.charAt(i+1), 16));
                }

                //generate the block cipher by doing the XOR of the IV and KEY 
                byte [] blockCypher  = new byte [64];
                for(int i = 0; i<64; i++)
                {
                    blockCypher[i] = (byte) (key[i]^IV[i]);
                }
                //create arrays for storage
                byte [] finalarray  = new byte [(int)fileSize];
                byte[] plainText  = new byte [64];
                 //calc # of 64 byte chunks
                int chunks = (int)Math.ceil(fileSize/64);
                int count = 0;
                while(count < chunks){
                     //decrypt 64 bytes at a time and append them to the final array
                    for(int i = 0; i<64;i++)
                    {
                        plainText[i] = (byte) (mybytearray[i+64*count]^blockCypher[i]);
                        finalarray[i+64*count] = plainText[i];
                    }
                    //reset the block cipher by XOR the cipher text with the key again to create new block cipher
                    for(int i = 0; i<64;i++)
                    {
                        blockCypher[i]=(byte)(mybytearray[i+64*count]^key[i]);
                    }
                    count++;
                }
                //write the file and close
                fos.write(finalarray);
                fos.close();
                System.out.println("File " + fileName + " downloaded (" + current + " bytes read)");
            }
            //user wants to list all the files
            else if(message.equals("list")){
                //tell the server we want to see the files
                printWriter.println(message);
                
                //get the list of general access files
                response = socketBufferedReader.readLine();
                String formatString = response.replace(" ", "\n");
                System.out.println("General Files:");
                System.out.print(formatString);

                //get the list of private files
                response = socketBufferedReader.readLine();
                formatString = response.replace(" ", "\n");
                System.out.println("Your Private Files:");
                System.out.print(formatString);
                
            }
            else{
                System.out.println("Error Command Not recognized.\nOptions are: List, Upload and Download");
            }
            
        }
    }
    private static String sha512(String text)
    {
        String hashtext;
        hashtext = text;
        try{
            // getInstance() method is called with algorithm SHA-512
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(text.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            hashtext = no.toString(16);

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // return the HashText
        return hashtext;
    }
}