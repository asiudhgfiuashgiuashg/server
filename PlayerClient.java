import java.net.*;
import java.io.*;

public class PlayerClient {
	Socket clientSocket;
    PrintWriter clientOut;               
    BufferedReader clientIn;
    float charX;
    float charY;
    String username;

    public PlayerClient(Socket clientSocket, PrintWriter clientOut, BufferedReader clientIn) {
    	this.clientSocket = clientSocket;
    	this.clientOut = clientOut;
    	this.clientIn = clientIn;
    }
}