import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Scanner;

public class Serverthread extends Thread {
    //create the socket, declare the max file size
    Socket socket;
    public final static int FILE_SIZE = 6022386;
    Serverthread(Socket socket)
    {
        this.socket = socket;
    }
    public void run(){
        try {
            //Create writer and reader to write messages across the socket
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String userName = "";
            String login = bufferedReader.readLine();
            if(login.equals("login"))
            {
                //Read the users.txt file and create a hashmap to look up the username values
                String user;
                boolean loginValid = false;
                Scanner sc = new Scanner(new File("users.txt"));
                Hashtable<String, String> users = new Hashtable<String, String>();
                while (sc.hasNextLine()) {
                    user = sc.nextLine();
                    String strar[] = user.split(",");
                    users.put(strar[0],strar[1]);
                }
                while(!loginValid)
                {
                    //get the Username and password from the client
                    userName = bufferedReader.readLine();
                    String password = bufferedReader.readLine();
                    System.out.println("Attempted connection from: "+userName);
                    //System.out.println("password: '"+password);

                    //Check if the user exists and the password matches
                    if(users.containsKey(userName))
                    {
                        if(users.get(userName).equals(password))
                        {
                            System.out.println("password matches");
                            loginValid = true;
                            printWriter.println("success");
                        }
                        else
                        {
                            System.out.println("Login Failed, incorrect password");
                            printWriter.println("fail");
                        }
                    }
                    else
                    {
                        System.out.println("Login Failed, user does not exist");
                        printWriter.println("fail");
                    }
                    
                }      
            }
            else if(login.equals("create"))
            {
                userName = bufferedReader.readLine();
                String password = bufferedReader.readLine();
                try
                {
                    FileWriter fw = new FileWriter("users.txt", true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.newLine();
                    bw.write(userName+","+password);
                    System.out.println("New User created: "+userName);
                    bw.close();
                    fw.close();
                } catch (IOException e) {
                    //exception handling left as an exercise for the reader
                }
                
            }
            System.out.println("User: '"+userName+"' is now connected.");

            //create the message and File IO variables
            String message;
            FileInputStream fis = null;
            OutputStream os = null;

            while (true) {
                //read the message request from the client and print it
                message = bufferedReader.readLine();
                System.out.println(message);

                //The client would like to upload a file
                if(message.equals("upload"))
                {
                    try{
                        String uploadType = bufferedReader.readLine();
                        //get the file name and its size in bytes, parse to integer
                        String fileName = bufferedReader.readLine();
                        String fileBytes = bufferedReader.readLine();
                        int fileSize = Integer.parseInt(fileBytes);
                        int bytesRead;
                        File myFile;
                        int current = 0;
                        FileOutputStream fos = null;
                        // receive file
                        byte [] mybytearray  = new byte [fileSize];
                        InputStream is = socket.getInputStream();
                        if(uploadType.equals("private"))
                        {
                            new File ("files\\"+userName).mkdirs();
                            myFile = new File ("files\\"+userName+"\\"+fileName);
                        }
                        else
                        {
                            myFile = new File ("files\\general\\"+fileName);
                        }
                        fos = new FileOutputStream(myFile);
                        //read fthe file byte by byte from the socket input stream
                        bytesRead = is.read(mybytearray,0,mybytearray.length);
                        current = bytesRead;
                        for(int i = fileSize; i >=0; i--)
                        {
                            bytesRead = is.read(mybytearray, current, (mybytearray.length-current));
                            if(bytesRead >= 0) current += bytesRead;
                        }
                        //write the byte array to the file output stream
                        fos.write(mybytearray);
                        fos.close();
                        System.out.println("File " + fileName + " downloaded (" + current + " bytes read)");
                    }catch (Exception e) {
                        //TODO: handle exception
                    }
                }
                //client requests to download a file from the server
                else if(message.equals("download"))
                {
                    try{
                        //read the file name from the client
                        String fileName = bufferedReader.readLine();
                        String downloadType = bufferedReader.readLine();
                        System.out.println("Client requests "+downloadType+" file: "+fileName);
                        File myFile;
                        if(downloadType.equals("private"))
                        {
                            new File ("files\\"+userName).mkdirs();
                            myFile = new File ("files\\"+userName+"\\"+fileName);
                        }
                        else
                        {
                            myFile = new File ("files\\general\\"+fileName);
                        }
                        //wirte the file size back to the client
                        printWriter.println(myFile.length());
                        //read the file into a byte array using a File input stream
                        byte [] mybytearray  = new byte [(int)myFile.length()];
                        fis = new FileInputStream(myFile);
                        fis.read(mybytearray);
                        os = socket.getOutputStream();
                        System.out.println("Sending " + fileName + "(" + mybytearray.length + " bytes)");
                        //Write the byte array and close the file
                        os.write(mybytearray,0,mybytearray.length);
                        os.flush();
                        fis.close();
                        System.out.println("Done.");
                    }catch (Exception e) {
                        //TODO: handle exception
                    }
                }
                else if(message.equals("list"))
                {
                    // creates a file object
                    File file = new File(System.getProperty("user.dir")+"\\files\\general");

                    // returns an array of all files
                    String[] fileList = file.list();
                    String response = "";
                    if(fileList.length != 0){
                        for(String str : fileList) {
                            response += str+" ";
                        }
                    }
                    else{
                        response = "Public Directory Empty";
                    }
                    printWriter.println(response);

                    // creates a file object
                    new File ("files\\"+userName).mkdirs();
                    file = new File(System.getProperty("user.dir")+"\\files\\"+userName);

                    // returns an array of all files
                    fileList = file.list();
                    response = "";
                    if(fileList.length != 0){
                        for(String str : fileList) {
                            response += str+" ";
                        }
                    }
                    else{
                        response = "Private-Directory-Empty";
                    }
                    
                    printWriter.println(response);
                }
                else
                {
                    printWriter.println(message + " echo");
                }
                
            }
        } catch (Exception e) {
            //TODO: handle exception
        }
        
    }
    
}