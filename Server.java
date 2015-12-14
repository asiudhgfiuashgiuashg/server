import java.net.*;
import java.io.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.util.List;
import java.util.ArrayList;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Queue;
import java.util.LinkedList;

public class Server {
	private static final int MAX_CLIENTS = 2;
	private static List<PlayerClient> clients;
    public static void main(String[] args) throws IOException {	
	    clients = new ArrayList<>();
	    ServerSocketChannel serverSocketChannel;
	    int numClientsConnected = 0;
	    
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }
        
        int portNumber = Integer.parseInt(args[0]);
        
        try {
            //ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[0]));

            serverSocketChannel =  ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(Integer.parseInt(args[0]))); //give it da port
            serverSocketChannel.configureBlocking(false);


            //main server loop
            while (true) {
            	SocketChannel socketChannel = null;
            	if (numClientsConnected < MAX_CLIENTS) {
            		socketChannel =  serverSocketChannel.accept(); //see if a connection is immediatly available, will return null if one isnt (non-blocking I/O)

            		if (socketChannel != null) { //got a connection
		        		//Socket clientSocket = socketChannel.socket();
                 
		        		PlayerClient playerClient = new PlayerClient(socketChannel);
		        		socketChannel.configureBlocking(false);
		        		playerClient.socketChannel = socketChannel;
		        		clients.add(playerClient);

		        		System.out.println("client number " + String.valueOf(numClientsConnected) + " connected");
		        		numClientsConnected++;
		        		

		        		if (MAX_CLIENTS == numClientsConnected) { // send game start signal to everyone
		        			JSONObject readyObject = new JSONObject();
		        			readyObject.put("type", "gameStartSignal");
		        			for (PlayerClient client: clients) {
		        				System.out.println("sending");
					        	sendJSONOnSocketChannel(readyObject, client.socketChannel);
					    	}

		        		}
		        	}
            	}
            	
            	for (int i = 0; i < clients.size(); i++)  {
            		attemptReadFrom(clients.get(i));
            		handleMessageFrom(clients.get(i));
            	}
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
    * remember there is one socketChannel per client
    */
    public static void sendJSONOnSocketChannel(JSONObject jsonObj, SocketChannel socketChannel) {
    	String stringToSend = jsonObj.toString() + '\n';
    	ByteBuffer toSend = ByteBuffer.allocate(stringToSend.length());	
		toSend.clear();
		try {
			toSend.put(stringToSend.getBytes("ASCII"));
			toSend.flip();
			while (toSend.hasRemaining()) {
				socketChannel.write(toSend);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
    }

    public static void attemptReadFrom(PlayerClient client) {
    	JSONObject receiveTo = new JSONObject();
    	try {
    		ByteBuffer byteBuffer = ByteBuffer.allocate(1000);
			int numBytesRead = client.socketChannel.read(byteBuffer);
			if (numBytesRead > 0) {
				String receivedStr = new String(byteBuffer.array(), 0, numBytesRead, "ASCII");
				String lines[] = receivedStr.split("\\r?\\n");
				for (String line: lines) {
					System.out.println("receivedStr: " + receivedStr);
					receiveTo = (JSONObject) JSONValue.parse(receivedStr);
					System.out.println("received json value: " + receiveTo);
					client.messageInQueue.add(receiveTo);
				}
			}
    	} catch (Exception e) {
    		e.printStackTrace();
    		System.exit(-1);
    	}
    }

    public static void handleMessageFrom(PlayerClient client) {
    	PlayerClient receiveFromClient = client;
        if (client.messageInQueue.peek() != null) { //something in queue
        	JSONObject received = client.messageInQueue.remove();
			System.out.println("handling: " + received.toString());
			//position messages
			if (received.get("type").equals("position")) {
				//record position
				receiveFromClient.charX = ((Number) received.get("charX")).floatValue();
				receiveFromClient.charY = ((Number) received.get("charY")).floatValue();
			
				System.out.println(received);
	    		// send coordinates to other clients
	    		sendToAllFrom(received, receiveFromClient);

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
					animationObj.put("animationName", "idle");
				}
				sendToAllFrom(animationObj, receiveFromClient);
			}
		}
    }

    public static void sendToAllFrom(JSONObject toSend, PlayerClient from) {
    	for (PlayerClient client: clients) {
			if (client != from) {
        		sendJSONOnSocketChannel(toSend, client.socketChannel);
        	}
    	}
    }
}
