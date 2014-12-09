package rhttp;
import java.io.*;
import java.net.*;
import java.util.*;
/**
* The HttpProcessor that handles requests to to initiate and reinitiate the control connection.
*
* author <a href="mailto: es itemit. edu" >Eric Sit</a>
* version %I%, %G% 10
* (c) Eric Sit 1999*/

public class RhttpControl implements HttpProcessor {
protected File file;
SocketList socketList;
SocketList browserHash;
Hashtable controlHash;
Socket client;
HttpInputStream in;

public boolean keepAlive = false;

public RhttpControl (HttpInputStream in, SocketList socketList,
SocketList browserHash, Hashtable controlHash,
Socket client) throws IOException {
	

this.socketList= socketList;
this.browserHash= browserHash;
this.controlHash = controlHash;
this.client = client;

this.in = in;
if (in.getMethod () == HTTP.METHOD_POST) {
// get the output stream
OutputStream o = client.getOutputStream();
HttpOutputStream out = new HttpOutputStream (o, in);
// send an OK back
out.write("HTTP/1.0 "+HTTP.STATUS_OKAY+" Ok\r\n");
out.write("\r\n"); 
out.flush();
keepAlive = true;
// Find the agent ID.
String aID = in.getHeader("Agent-ID");
ServerSocket ss = null;
 
if (!socketList.containsKey(aID)) {
// we are adding a new server
ss = addServer(aID);
// start proxy
ProxyFile toControl = new ProxyFile(ss, client, browserHash, true);
toControl.setPreprocess("encodeAsResponse");
toControl.start();
controlHash.put(aID, toControl);  
} else {
// we are trying to reinit a dead connection
System.out.println("Attempting to reinitialize.");
ss = (ServerSocket) socketList.get(aID);
ProxyFile newToControl = (ProxyFile) controlHash.get(aID);
newToControl.setOutSocket(client);
}  
};
}
public void processRequest (HttpOutputStream out) throws IOException {
 
}
ServerSocket addServer(String aID) {
// now we want to create a serverSocket to handle requests for this server
//ff/i rst pick a port to use 80
boolean unique = false;
ServerSocket ss=null;
while (!unique) {
try {
ss = new ServerSocket(0);
unique = true;
System.out.println("Serversocket at "+ss.getLocalPort());
} catch (IOException e) {
System.err.println("Could not start server on port: "+ ss.getLocalPort());  
System.out.println("Trying a different port.");
//I/try to get a different port
}
}
// store association between new server socket to the connected client socket
socketList.put(aID,ss);
System.out.println("Associating RHP port "+ss.getLocalPort()+" with client.");
 
return ss;
}
}