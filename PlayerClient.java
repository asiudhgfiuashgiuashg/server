import java.net.*;
import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.LinkedList;
import org.json.simple.JSONObject;

public class PlayerClient {
    float charX;
    float charY;
    String username;
    SocketChannel socketChannel;
    Queue<JSONObject> messageInQueue;
    boolean ready; //to start the game

    public PlayerClient(SocketChannel socketChannel) {
    	this.socketChannel = socketChannel;
        messageInQueue =  new LinkedList<>();
        ready = false;
    }
}