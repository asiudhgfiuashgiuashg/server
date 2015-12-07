import java.net.*;
import java.io.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Server {
    public static void main(String[] args) throws IOException {
        float char0X;
	    float char0Y;
	    float char1X;
	    float char1Y;
	    
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }
        
        int portNumber = Integer.parseInt(args[0]);
        
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));

            /**client 0 setup**/
            Socket client0Socket = serverSocket.accept();
            PrintWriter client0out = new PrintWriter(client0Socket.getOutputStream(), true);                   
            BufferedReader client0in = new BufferedReader(new InputStreamReader(client0Socket.getInputStream()));
            System.out.println("*****************CLIENT 0 CONNECTED******************");
            /**client 1 setup**/
            Socket client1Socket = serverSocket.accept();
            PrintWriter client1out = new PrintWriter(client1Socket.getOutputStream(), true);                   
            BufferedReader client1in = new BufferedReader(new InputStreamReader(client1Socket.getInputStream()));
            System.out.println("*****************CLIENT 1 CONNECTED******************");

            JSONObject readyObject = new JSONObject();
            readyObject.put("type", "gameStartSignal");
            client0out.println(readyObject);
            client1out.println(readyObject);

            String inputLine0;
            String inputLine1;
            JSONObject received0;
            JSONObject received1;
            while (true) {
            	if (client0in.ready()) {
            		inputLine0 = client0in.readLine();
            		received0 = (JSONObject) JSONValue.parse(inputLine0);
            		System.out.println("received from client 0: " + received0.toString());
            		if (received0.get("type").equals("position")) {
            			//record position
            			char0X = ((Number) received0.get("charX")).floatValue();
            			char0Y = ((Number) received0.get("charY")).floatValue();
            		
	            		// send coordinates to other client
		                client1out.println(received0);
            		}
            		
            	}
            	if (client1in.ready()) {
            		inputLine1 = client1in.readLine();
            		received1 = (JSONObject) JSONValue.parse(inputLine1);
            		System.out.println("received from client 1: " + received1.toString());
            		if (received1.get("type").equals("position")) {
            			//record position
	            		char1X = ((Number) received1.get("charX")).floatValue();
	            		char1Y = ((Number) received1.get("charY")).floatValue();

	            		// send coordinates to other client
		                client0out.println(received1);
		            }
            	}
            }
            //System.out.println("*****************ONE OR MORE CLIENTS DISCONNECTED******************");
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}
