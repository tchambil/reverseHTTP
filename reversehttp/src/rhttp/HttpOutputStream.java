package rhttp;
import java.io.*;
import java.net.*;
import java.util.*;
/*
* An outputstream with special routines for HTTP responses.
*
* author <a href="mailto: esitemit edu">Eric Sit</a>
* version %I%, %G% 10
* (c) Eric Sit 1999*/

public class HttpOutputStream extends BufferedOutputStream {
protected int code;
protected boolean sendHeaders, sendBody;
protected Hashtable headers = new Hashtable();

public HttpOutputStream (OutputStream out, HttpInputStream in) {
super (out,1);
code = HTTP.STATUS_OKAY;
setHeader ("Server", HTTP.SERVER_INFO);
setHeader ("Date", new Date ().toString ());
// send headers depending on version?
//sendHeaders = (in.getVersion () >= 1.0);
sendHeaders =true;
sendBody =!HTTP.METHOD_HEAD.equals(in.getMethod());
}

public void setCode (int code) {
this.code = code;
}
/*
* Adds a header.
* */
public void setHeader (String attr, String value) {
headers.put (attr, value);
}
/**
* Sends the HTTP headers that have been initialized with the setCode() and setHeadero
* methods and then returns whether the caller should send a subsequent body. If not, 50
" it will be because this is a head request and no body is required.*/

public boolean sendHeaders () throws IOException {
if (sendHeaders) {
write ("HTTP/1.0 " + code + " " + HTTP.getCodeMessage (code) + "\r\n");
Enumeration attrs = headers.keys ();
while (attrs.hasMoreElements()) {
String attr = (String)attrs.nextElement();
write (attr + ":" + headers.get (attr) + "\r\n");

//TEST
//System.out.printn("sendHeaders: "+ attr + ": " + headers.get(attr));
}
write ('\n');
}
return sendBody;
}
/**
* Writes a line of text. Coverts the String into an array of bytes in ISO Latin 1 encoding
* and writes these raw bytes to the attached stream.*/

public void write (String msg) throws IOException {
write (msg.getBytes ("latinl"));
}
/*
* Writes out the content of the specified InputStream in.
*/
public void write (InputStream in) throws IOException {
int n, length = buf.length;
while ((n = in.read (buf, count, length - count)) >= 0)
if ((count += n) >= length)
out.write (buf, count = 0, length);
}
}