package rhttp;
 
import java.io.*;
import java.net.*;
import java.util.*;
/**
* A class describing the RHP web server.
*
53
60
70
80
90
* author <a href= "mailto: esitemit.edu" >Eric Sit</a> 10
" version %I%, %G%
* (c) Eric Sit 1999*/

public class RHPWeb extends Thread {
Thread t;
private ServerSocket ss = null;
private Socket client = null;
 
SocketList socketList;
SocketList browserHash;
Hashtable controlHash;
public RHPWeb(ServerSocket socket, SocketList socketList, SocketList browserHash) {
this.socketList = socketList;
this.browserHash = browserHash;
t = new Thread(this, "RHPWeb Thread");
System.out.println("Child thread: " + t);
this.ss = socket;  
controlHash = new Hashtable();
}
public void run(){
try {
for (;;) {
 
// Wait for client to connect. The method will block, and when it
//returns the socket will be already connected to the client
client = ss.accept();
InputStream in;
HttpInputStream httpIn;
HttpProcessor processor;
OutputStream out;
HttpOutputStream httpOut;
boolean keepAlive = false;  
try {
// get the inputstream
 
in = client.getInputStream ();
httpIn = new HttpInputStream (in);
// figure out the right processor to use for this stream
processor = getProcessor (httpIn);
 
if (processor instanceof RhttpControl) {
RhttpControl subProcessor = (RhttpControl) processor;
keepAlive = subProcessor.keepAlive;
}
else if (processor instanceof RhttpData) {
RhttpData subProcessor = (RhttpData) processor;
keepAlive = subProcessor.keepAlive;
}
 
// get the output stream
out = client.getOutputStream ();
httpOut = new HttpOutputStream(out,httpIn);
//process it
processor.processRequest (httpOut);
httpOut.flush ();
} catch (IOException e) {  
e.printStackTrace();
} finally {
if (!keepAlive) {
try {
client.close();
} catch (IOException ignored) {}
}
}
} // Loop again, waiting for next connection 90
} catch (IOException e) {
e.printStackTrace();
}
}
protected HttpProcessor getProcessor (HttpInputStream httpIn) {
try {
	httpIn.readRequest();
 
if (httpIn.getPath ().startsWith (HTTP.SERVERS_BIN))  
return new RhttpServers (httpIn, socketList);
else if (httpIn.getPath ().startsWith (HTTP.CONTROL_BIN))
return new RhttpControl (httpIn, socketList, browserHash, controlHash, client);
else if (httpIn.getPath ().startsWith (HTTP.DATA_BIN))
return new RhttpData (httpIn, socketList, browserHash, client);
else if (httpIn.getPath ().startsWith (HTTP.PAC_BIN))
return new RhttpPac (httpIn, socketList);
else if (httpIn.getPath ().startsWith (HTTP.CLOSE_BIN))
return new RhttpClose(httpIn, socketList, controlHash, client);
else  
return new HttpFile (httpIn);
} catch (HttpException ex) {
return ex;
} catch (Exception ex) {
StringWriter trace = new StringWriter();
ex.printStackTrace (new PrintWriter (trace, true));
return new HttpException (HTTP.STATUS_INTERNAL_ERROR,
"<PRE>" + trace + "</PRE>");
}
}
}
