package rhttp;
import java.io.*;
import java.net.*;
import java.util.*;
/*
* A class describing a proxy server. The proxy takes the bytes from a socket, writes
* it to a file, and copies the bytes from the file to a socket.
*
* author <a href= "mailto: esitfmit. edu" >Eric Sit</a> 10
* version %I%, %G%
* (c) Eric Sit 1999*/

public class ProxyFile extends Thread {
Thread t;
InputStream from;
HttpInputStream http_from; 
InputStream from_server;
HttpInputStream http_from_server;
OutputStream to_server;
HttpOutputStream http_to_server;
SocketList browserHash;
/*
* The stored browser ID. 30*/

public String bID;
/*
* The stored agent ID.*/

public String aID="";
File file;
/*
* The contentLength of the HTTP body.
64*/

int contentLength;
/*
* If a serversocket was passed in during creation, isServerSocket is true, otherwise false.*/
public boolean isServerSocket = false;
/*
* If true, data has been sent sucessfully, otherwise false. 50*/
public boolean dataSent = false;/*
* If true, the ProxyFile thread will return at the first available moment.
* This usually right after the serversocket accepts.*/
public boolean shutdown = false;
Socket s;  
ServerSocket ss;
Socket outSocket;
String preprocessType="";
/*
* if false, Proxy will close data to outgoing connection after sending data.*/
boolean persistent = false; /*
* Creates a proxy to copy data from the socket to outSocket.
* If persistent, connection to the socket will remain open after proxying data.*/
public ProxyFile(Socket socket, Socket outSocket, boolean persistent) {
t = new Thread(this, "ProxyFile Thread");
System.out.println("Child thread: " + t);
this.s = socket;  
this.outSocket=outSocket;
this.persistent = persistent;
}/*
* Creates a proxy to copy data from the socket accepted by ss to outSocket.
* BrowserHash maintains a hash between the browserID and the accepted socket.
* If persistent, connection to the socket will remain open after proxying data.
65*/
public ProxyFile(ServerSocket ss, Socket outSocket, SocketList browserHash, boolean persistent) {  
t = new Thread(this, "ProxyFile Thread");
System.out.println("Child thread: " + t);
this.ss = ss;
this.outSocket = outSocket;
isServerSocket = true;
this.persistent = persistent;
this.browserHash = browserHash;
}/*
100
* Main execution of Proxy thread.*/
public void run() {
System.out.println(t + " of preprocessType: "+ preprocessType +" has started.");
boolean loop = true;
while (loop) {
 
if (isServerSocket)
try {
getClient();
} catch (IOException ignored) {
continue;
}
if (shutdown)
return;

try {
toFile();
} catch (IOException ignored) {
continue;
}
try {
synchronized(outSocket) {
createStreams();
preProcess(); 
toSocket();
dataSent = true;
System.out.println("Proxyfile has successfully proxied along the data!");
 
System. out.println();
}
} catch (IOException e) {
e.printStackTrace();
dataSent = false;
System.err.println("Proxyf ile failed!");
 
if (getPreprocess().equals("encodeAsResponse")) {
System.err.println("The control connection must be down!!");
try {
System.out.println("Trying to close control connection.");
outSocket.close();
} catch (IOException f) {
System.out.println("Couldn't close control!");
}
 
dataSent= false;
// we should wait until another control connection is established.
}
}
finally {
boolean deleted = file.delete();
if (!deleted)
System.out.println(file + " not deleted!");
}
if (!dataSent)
try 
	{
	wait(1000);
	} 
	catch (InterruptedException e) {
	}
	if (!getPreprocess().equals(" encodeAsRe sponse"))
	loop=false;
 }
System.out.println(t + " of preprocessType: " + preprocessType +" has returned.");
}
/*
* Waits to accept a socket. The browserID is stored in browserffash.
 */
void getClient() throws IOException {
// If its a server socket, we need to wait for client socket 180
try {
s = ss.accept();
if (shutdown) {
System.out.println("Attempting to shutdown control thread!");
try {
ss.close();
} catch (IOException e) {
e.printStackTrace(); 
}
try {
outSocket.close();
} catch (IOException e) {
e.printStackTrace();
}
return;
}
System.out.println("Request received!");  
// generate socket key.
bID = s.getInetAddress().getHostAddress()+" "+String.valueOf(s.getPort());
if (browserHash != null)
browserHash.put(bID, s);
System.out.println("The saved browser ID is: "+bID);
} catch (IOException e) { 
System.err.println("Could not accept client socket for proxy");
e.printStackTrace();
throw e;
}
}
/*
* Copies the data from a socket to a file.*/
void toFile() throws IOException { 
// write to a file
 
String filename = HTTP.TMP_BIN + new Integer(s.getLocalPort()).toString() +".tmp";
file = new File(filename);
FileOutputStream fout;
try {
fout = new FileOutputStream(file); 
} catch (IOException e) {
System.err.println("I0 Exception in creating fileoutputstream for writing");
fout = null;
e.printStackTrace();
throw e;
}
try {
from = s.getInputStream();
} catch (IOException e) { 
System.err.println("could not open file inputstream");
e.printStackTrace();
throw e;
}
if (preprocessType.equals(" encodeAsResponse")) {
// first try to read the request
http_from = new HttpInputStream (from);
 
try {
http_from.readRequest();
} catch (IOException e) {
System.err.println("Could not read request from stream.");
//e.printStackTrace ();
throw e;
}
//if its not a POST, we can just write what we know to disk 260
String requestString = http_from.getMethod() +"1 "+ http_from.getPath() +" "+ "HTTP/"+ String.valueOf(http_from.getVersion())+ "\r\n";
try {
fout.write(stringToBytes(requestString));
} catch (IOException e) {
//System.err.printin("Could not write request to disk.");
e.printStackTrace ();
 
throw e;
}  
// now write the rest of headers
Enumeration headerNames = http_from.getHeaderNames();
while (headerNames.hasMoreElements ()) {
String header = (String) headerNames.nextElement ();
try {
fout.write (stringToBytes(header + ": " + http_from.getHeader(header) + "\r\n"));
} catch (IOException e) {
System.err.println("Could not write the rest of the headers to disk");
e.printStackTrace (); 
throw e;
}
}
try {
fout.write (stringToBytes("\n"));
} catch (IOException e) {
System.err.println("Could not write \n after headers");
e.printStackTrace ();
throw e;
}  
if (http_from.getMethod() == HTTP.METHOD_POST) {
// if it is a POST we need to write the body also.
byte[] buffer;
// first figure out if the request has a content length.
String clStr = http_from.getHeader("Content-length");
if (clStr != null) {  
contentLength = Integer.parseInt(clStr);
buffer = new byte[contentLength];
} else {
contentLength = -1;
buffer = new byte[4096];
}
int bytes_read;
int bytes_read_total = 0;

try {
while((bytes_read = http_from.read(buffer)) != -1) {
fout.write(buffer, 0, bytes_read);
 
fout.flush();
bytes_read_total += bytes_read;
if ((bytes_read_total >= contentLength) && (contentLength != -1))
break;
}  
} catch (IOException e) {
e.printStackTrace();
throw e;
} finally {
try {
fout.close();
} catch (IOException e) {
e.printStackTrace();
}
}  
} else { // any other request other than POST
try {
fout.close();
} catch (IOException e) {
e.printStackTrace();
}
}
} else if (preprocessType.equals("encodeAsRequest")) {
 
// all we need to do is write to disk directly.
// it will stop when the web server closes connection
byte[] buffer = new byte[4096];
int bytes_read;
http_from = new HttpInputStream(from);
try {
while((bytes_read = http_from.read(buffer)) != -1)
if (fout != null) 
fout.write(buffer, 0, bytes_read);
} catch (IOException e) {
System.err.println("I0Exception in write (part of encodeAsRequest)");
e.printStackTrace ();
throw e;
} finally {
if (fout != null)
try {
 
fout.close();
} catch (IOException e) {  
e.printStackTrace();
}
}
}
}/*
* Creates the necessary streams from the sockets.
 */
void createStreams() throws IOException {
try {
from_server = outSocket.getInputStream();
http_from_server = new HttpInputStream (from_server);
to_server = outSocket.getOutputStream();
http_to_server = new HttpOutputStream (to_server, http_from_server);
// ENABLE the following in order to LOG socket data 380
//if (preprocessType. equals("encodeA sResponse"))
//http_to_server = new LoggedOutputStream(to_server, http_from_server, HTTP.LOG_BIN+"RHPout.txt");
//else
//http_to_server = new LoggedOutputStream(to_server, http_from_server, HTTP.LOG_BIN+"RHAout.txt");
} catch (IOException e) {
System.err.println("System could not connect to server.");
e.printStackTrace ();
throw e;  
}
}
/*
* Processes bytes depending on processType before proxying the data along.*/
void preProcess() throws IOException {
if (preprocessType.equals("encodeAsResponse")) {
contentLength = (int) file.length();
// first send additional headers 400
http_to_server.setHeader("Content-Length", String.valueOf(contentLength));
http_to_server.setHeader ("Browser-ID", bID);
try {
 
	http_to_server.sendHeaders ();
	http_to_server.flush();
} catch(IOException e) {
System.err.println("Couldn't set header in preprocessing");
e.printStackTrace ();
throw e;
}  
} else if (preprocessType.equals("encodeAsReque st")) {
contentLength = (int) file.length();
// send
try {
	http_to_server.write(HTTP.METHOD_POST+" http://"+ RHA.getRHPHost() +":"+ RHA.getRHPPort() +"/" +HTTP.DATA_BIN+" HTTP/1.0"+"\r\n");
	http_to_server.write("Agent-ID: "+aID+"\r\n");
if (bID != null) {  
	http_to_server.write("Browser- ID: "+bID+"\r\n");
} else {
System.err.println("Browser ID not set!");
}
http_to_server.write("Content-length: "+ String.valueOf(contentLength) +" \r\n");
http_to_server.write("\n");
http_to_server.flush();
} catch (IOException e) {
System.err.println("Couldn't write to RHP");  
e.printStackTrace ();
throw e;
}
}
}
/*
* Copies the data from the file to the outSocket.*/

void toSocket() throws IOException {  
// start proxying
int bytes_read;
byte[] buffer;
if (contentLength<4096)
buffer = new byte[(int) contentLength];
else
buffer = new byte[4096];
 
FileInputStream from_file;
try {
from_file = new FileInputStream(file);
} catch (FileNotFoundException e) {
System.err.println("File wasn't saved so it couldn't be found.");
throw new IOException(e.toString());
}
try {
while ((bytes_read = from_file.read(buffer))!= -1) { 
http_to_server.write(buffer, 0, bytes_read);
http_to_server.flush();
if (bytes_read >= contentLength)
break;
}
} catch (IOException e) {
System.err.println("We couldn't write to the socket going to server for some reason!");
try {
from_file.close();
} catch (IOException ef) {  
System.err.println("Couldn't close file!");
ef.printStackTrace();
}
e.printStackTrace();
throw new IOException(e.toString() + "Couldn't write to socket going to server for some reason!");
} finally {
try {
from_file.close();
} catch (IOException e) {
System.err.println("Couldn't close file!");  
e.printStackTrace();
}
}
if (!persistent) {
try {
to_server.close();
} catch (IOException e) {
}
}  
}
/*
* Sets the preprocess type which affects what kind of preprocessing needs to be done.*/
public void setPreprocess(String preprocessType) {
this.preprocessType = preprocessType;
}
/*
* Gets the preprocess type which affects what kind of preprocessing needs to be done.*/
public String getPreprocess() {
return preprocessType;
}/*
* Changes the outSocket to a new socket. Useful when a control connection needs
* to be changed to a new socket. 510*/
public synchronized void setOutSocket(Socket s) throws IOException {
outSocket = s;
try {
createStreams();
} catch (IOException e) {
throw e;
}
}
/*
* Returns the target socket where data is being proxied into.*/
public Socket getOutSocket(Socket s) {
return outSocket;
}/*
* Reads a line of text from an inputstream.
530*/
public String readLine (BufferedInputStream in) throws IOException {
StringBuffer line = new StringBuffer ();
int c;
while (((c = in.read ())!= -1) && (c != '\n') && (c !='\r'))
line.append ((char) c);
//if ((c == \r') && ((c = in.read () = '\n') && (c = -1))
// - in.pos;
return ((c == -1) && (line.length () == 0)) ? null : line.toString ();
 
}
/** Returns a String in byte format.*/
public  byte[] stringToBytes (String msg) throws IOException {
return msg.getBytes ("latinl");
}
}