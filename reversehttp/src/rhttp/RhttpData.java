package rhttp;
import java.io.*;
import java.net.*;
import java.util.*;
/*
* The HttpProcessor that handles incoming data.
* These are the responses to previously made requests.
*
* author <a href="mailto: esit@mit. edu" >Eric Sit</a> 10
* version %I%, %G%
93
* (c) Eric Sit 1999*/

public class RhttpData implements HttpProcessor {
protected File file;
Socket client;
Socket bSocket;
String bID;  
String aID;
SocketList socketList;
SocketList browserHash;
ServerSocket ss;
HttpInputStream in;
OutputStream to_server;  
HttpOutputStream http_to_server;
public boolean keepAlive = false;
// we are guarenteed that client is distinct because we just gota new socket from RHPWeb's accept call
public RhttpData (HttpInputStream in, SocketList socketList,
SocketList browserHash, Socket client) throws IOException {
this.socketList = socketList;  
this.browserHash = browserHash;
this.client = client;
this.in = in;
System.out.println("Begin RhttpData.");
// first create the streams
try {
to_server = client.getOutputStream();
http_to_server= new HttpOutputStream (to_server, in);  
} catch (Exception e) {
System.err.println("System could not connect to server.");
e.printStackTrace();
}
 
if (in.getMethod () == HTTP.METHOD_POST) {
// we should be handling a continuation of a previous communication
System.out.println("RHA POST request should contain DATA.");  
bID = in.getHeader("Browser-ID");
//System.out.printin("The browserID in the incoming request is " + bID);
// using the browser ID coming in, try to match it up with the original browser socket
bSocket = (Socket) browserHash.get(bID);
// determine agent-id
aID = in.getHeader( "Agent-ID");  
System.out.println("The agent ID of this RHA request is "+aID);
// determine appropriate server socket
ss = (ServerSocket) socketList.get(aID);
keepAlive = true;
};
}
public void processRequest (HttpOutputStream out) throws IOException {
// make a proxy to original browser socket
Proxy toBrowser = new Proxy(client, bSocket, false);
toBrowser.setPreprocess("decodeFromRequest");
toBrowser.setContentLength(Integer.parseInt (in.getHeader("Content - length")));
toBrowser.start();
// wait for everything to be sent back before proceeding. 90
try 
{
toBrowser.join();
} 
catch (InterruptedException e) {
System.out.println("Rhttpdata back to browser interrupted!");
e.printStackTrace ();
}
// get rid of the browser socket entry in the hash table since we don't need it anymore.
browserHash.remove(bID);
 
if (!toBrowser.dataSent) {
 
System.err.println("Couldn't send data back to browser!");
// for some reason, we couldn't send the data to the browser.
// A possible reason for this is that the browser has already closed the socket.
//For example, the user clicked on another link before it received a response to an earlier request.
// we don't care about the data anymore. we consider it dead.
// send back an error along the data connection
http_to_server.write("HTTP/1.0 "+HTTP.STATUS_INTERNAL_ERROR+" Ok\r\n");
http_to_server.write(" \r\n");
http_to_server.flush();
 
// we want to close connection to RHA
keepAlive = false;
// Now, the assumption is that the control connection is still up.
// The plan is to ignore this particular bit of data coming in along the data connection.
// We'll just wait till another request comes along the control connection.
} else {
// success
	http_to_server.write("HTTP/1.0 "+HTTP.STATUS_NO_CONTENT+" Ok\r\n");
	http_to_server.write( "\r\n");
	http_to_server.flush();
} 
System.out.println("End RhttpData.");
System.out.println();
}
}