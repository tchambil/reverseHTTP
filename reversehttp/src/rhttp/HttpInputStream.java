 
package rhttp;
import java.io.*;
import java.net.*;
import java.util.*;
/*
* An inputstream with special routines for HTTP requests.
*
* author <a href="mailto: esitDmit. edu" >Eric Sit</a>
* version %I%, %G% 10
* (c) Eric Sit 1999*/

public class HttpInputStream extends BufferedInputStream {
	 
/*
* Creates an HttpInputStream from an InputStream.
* */
	
public HttpInputStream (InputStream in) {
super (in,1);
}  
 
protected String method, path, queryString;
protected float version;
protected Hashtable headers = new Hashtable();
/**
* Parses an HTTP request, storing the method, request, and version in the object.
* */

public void readRequest () throws IOException {
String request = readLine (); 
if (request == null)
throw new HttpException (HTTP.STATUS_BAD_REQUEST, "Null query");
StringTokenizer parts = new StringTokenizer (request);
try {
parseMethod (parts.nextToken ());
parseRequest (parts.nextToken ());
} catch (NoSuchElementException ex) {
throw new HttpException (HTTP.STATUS_BAD_REQUEST, request);
}
if (parts.hasMoreTokens ())  
parseVersion (parts.nextToken());
else
version = 0.9f;
if ((version < 1.0f) && (method == HTTP.METHOD_HEAD))
throw new HttpException (HTTP.STATUS_NOT_ALLOWED, method);
if (version >= 1.0f)
readHeaders();
}
 /**
* Parses an HTTP method portion of the HTTP request.
* */

protected void parseMethod (String method) throws HttpException {
if (method.equals (HTTP.METHOD_GET))
	this.method = HTTP.METHOD_GET;
else if (method.equals (HTTP.METHOD_POST))
	this.method = HTTP.METHOD_POST;
else if (method.equals (HTTP.METHOD_HEAD))
	this.method = HTTP.METHOD_HEAD;
else  
throw new HttpException (HTTP.STATUS_NOT_IMPLEMENTED, method);
}
/**
* Parses the request portion of the HTTP request
*/

protected void parseRequest (String request) throws HttpException {
System.out.println("Parsing request: " + request);  
if (request.startsWith("http://")) {
// we want to remove the http://hostname part
String relPath; // result after we remove http://
int pathIdx = request.indexOf('/', 7); // the slash after the hostname
if (pathIdx == request.length()-1)
relPath = "/";
else  
relPath = request.substring(pathIdx);
// now do the normal stuff
path = HTTP.canonicalizePath (relPath);
queryString ="";
}
else if (request.startsWith ("/")) {
 
path = HTTP.canonicalizePath (request);
queryString ="";
} else
throw new HttpException (HTTP.STATUS_BAD_REQUEST, request);
}
/**
* Parses the version part in the HTTP request. 100
* */

protected void parseVersion (String verStr) throws HttpException {
if (!verStr.startsWith ("HTTP/"))
throw new HttpException (HTTP.STATUS_BAD_REQUEST, verStr);
try {
version = Float.valueOf (verStr.substring (5)).floatValue();
} catch (NumberFormatException ex) {
throw new HttpException (HTTP.STATUS_BAD_REQUEST, verStr);
}
}  
 /**
* Reads through all the headers in the HTTP request.
* */

public void readHeaders() throws IOException {
String header;
while (((header = readLine ()) != null) && !header.equals ("")) {
// TEST
// System.out.printin("readHeaders: "+header); 120
int colonldx = header.indexOf (':');
if (colonldx != -1) {
String name = header.substring (0, colonldx);
String value = header.substring (colonldx + 1);
headers.put (name.toLowerCase (), value.trim ());
}
}
} 
/**
* Reads a line and returns a string.
* */

public String readLine () throws IOException {
StringBuffer line = new StringBuffer ();
int c;
while (((c = read ()) != -1) && (c!='\n') && (c !='\r'))
line.append ((char) c); 
if ((c == '\r') && ((c = read ()) != '\n') && (c!= -1))
-- pos;
return ((c == -1) && (line.length() == 0)) ? null: line.toString();
}
/*
* Returns the request's method.
150
*/

public String getMethod () {
return method;
}
/*
* Returns the request's path.
83
*/
public String getPath() {
return path;
}
/*
* Returns the request's query.
* */
public String getQueryString () {
return queryString;
}
/*
* Returns the request's version.
* */
public float getVersion () {
return version; 
}/*
* Returns the request's header.
* */
public String getHeader (String name) {
return (String) headers.get (name.toLowerCase ());
}
/**
* Returns an Enumeration of all the headers in the request.
* */
public Enumeration getHeaderNames() {
return headers.keys ();
}/*
* Erases all the object's stored headers from the request.
* */
public void resetHeaders() {  
headers = new Hashtable();
}
/*
* Returns the number of bytes available in stream.
* */
public int bytesAvailable() {
int bytes_available;
try {
bytes_available = this.available(); 
} catch (IOException e) {

e.printStackTrace();
bytes_available = -1;
}
return bytes_available;
}
}