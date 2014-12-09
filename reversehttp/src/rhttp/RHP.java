 
package rhttp;
import java.io.*;
import java.net.*;
import java.util.Vector;
/*
* A class containing describing the Reverse HTTP Proxy
* author <a href="mailto:esitemit.edu">Eric Sit</a>
* version %I%, %G% 10
* (c) Eric Sit 1999*/

public class RHP implements Runnable {
 
boolean listening = true;
InputStreamReader from_client = null;
OutputStreamWriter to_client = null;
InputStreamReader from_server = null;
OutputStreamWriter to_server = null;  
RHPWeb web;
/*
* A vector containing all information about RHP hosts that class knows about.*/

SocketList socketList = new SocketList();
SocketList browserHash = new SocketList();
 /*
* Creates a reverse HTTP object.*/
public static void main(String[] args) throws IOException {
RHP thisRHP = new RHP();
thisRHP.run();
}
/*
* Handle incoming responses and make the request to the web server*/

public void run() {
// figure out the localhost
String localHost;
String localIP;
try { 
localHost = InetAddress.getLocalHost ().getHostName ();
localIP = InetAddress.getLocalHost ().getHostAddress();
} catch (UnknownHostException ex) {
localHost = "localhost";
localIP = "127.0.0.1";
}
System.out.println("This machine is called "+localHost+" with an IP of "+localIP);
 addWebServer();
while (true) {
try {
web.join();
} catch (InterruptedException e) {
System.err.println("web service interrupted!");
e.printStackTrace();
}
}
}
/*
* Creates web server to list all servers available*/

public void addWebServer() {
ServerSocket webServerSocket = null;
try {
webServerSocket = new ServerSocket(HTTP.PORT);
} catch (IOException e) {
System.err.println("Could not start web server on port: "+ HTTP.PORT);
System.exit(-1);
}
web = new RHPWeb(webServerSocket, socketList, browserHash);
web.start();
}
}