import java.net.*;
import java.io.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.util.List;
import java.util.ArrayList;

public class Server {
	private static final int MAX_CLIENTS = 2;

    public static void main(String[] args) throws IOException {

	    List<PlayerClient> clients = new ArrayList<>();
	    
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }
        
        int portNumber = Integer.parseInt(args[0]);
        
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));

            for (int i = 0; i < MAX_CLIENTS; i++) {
            	Socket clientSocket = serverSocket.accept();
            	PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);                   
            	BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            	clients.add(new PlayerClient(clientSocket, clientOut, clientIn));
            }

            //send game start signal to everyone once everyoen is connected
            JSONObject readyObject = new JSONObject();
            readyObject.put("type", "gameStartSignal");
            for (PlayerClient client: clients) {
            	client.clientOut.println(readyObject);
            }
            
            //main server loop
            while (true) {
            	for (int i = 0; i < clients.size(); i++)  {
            		PlayerClient client = clients.get(i);
            		if (client.clientIn.ready()) {
            			receiveFromSendTo(clients, i, client.clientIn, client.clientOut);
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

    //receive messages from 1 client and pass them on to all the other clients after saving some information into server vars
    public static void receiveFromSendTo(List<PlayerClient> clientList, int receiveFromListPosition, BufferedReader receiveFromReader, PrintWriter sendToWriter) throws IOException {
    	PlayerClient receiveFromClient = clientList.get(receiveFromListPosition);
    	String inputLine;
        JSONObject received;

    	inputLine = receiveFromClient.clientIn.readLine();
		received = (JSONObject) JSONValue.parse(inputLine);
		System.out.println("received from client " + received.toString());
		//position messages
		if (received.get("type").equals("position")) {
			//record position
			receiveFromClient.charX = ((Number) received.get("charX")).floatValue();
			receiveFromClient.charY = ((Number) received.get("charY")).floatValue();
		
    		// send coordinates to other clients
    		for (PlayerClient client: clientList) {
    			if (client != receiveFromClient) {
            		client.clientOut.println(received);
            	}
    		}
		} else if (received.get("type").equals(("direction"))) { //direction updates, need to update animations accordingly
			boolean isMovingLeft = (boolean) received.get("isMovingLeft");
			boolean isMovingRight = (boolean) received.get("isMovingRight");
			boolean isMovingUp = (boolean) received.get("isMovingUp");
			boolean isMovingDown = (boolean) received.get("isMovingDown");

			JSONObject animationObj = new JSONObject(); //represents a message signalling an animation change in the RemotePlayer
			animationObj.put("type", "animation");
			if (isMovingLeft) {
				animationObj.put("animationName", "walkLeft"); // these Animation names are recognized by the setAnimation method of RemotePlayer and signal it what animation to change the remotePlayer to
			} else if (isMovingRight) {
				animationObj.put("animationName", "walkRight");
			} else if (isMovingDown) {
				animationObj.put("animationName", "walkDown");
			} else if (isMovingUp) {
				animationObj.put("animationName", "walkUp");
			} else { //standing still
				animationObj.put("animationName", "notMoving");
			}

			for (PlayerClient client: clientList) {
    			if (client != receiveFromClient) {
            		client.clientOut.println(animationObj);
            	}
    		}
		}
    }
}
