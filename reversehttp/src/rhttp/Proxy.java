package rhttp;

import java.io.*;
import java.net.*;
import java.util.*;

/*
 * A class describing a generic proxy server. The proxy takes the
 " incoming bytes from the client-socket, processes it, and then
 " passes it along to the server-socket
 * 10
 * author <a href ="mailto: esitmit. edu" >Eric Sit</a>
 * version %I%, %G%
 * (c) Eric Sit 1999*/

public class Proxy extends Thread {

	Thread t;
	InputStream from_client;
	OutputStream to_client;
	InputStream from_server;
	OutputStream to_server;
	HttpInputStream http_from_client;
	HttpOutputStream http_to_client;
	HttpInputStream http_from_server;
	HttpOutputStream http_to_server;
	Socket client_socket, server_socket;

	SocketList browserHash;
	/*
	 * The stored browser ID.
	 */

	public String bID;
	String preprocessType = "";
	/*
	 * The contentLength of the HTTP body.
	 */

	private int contentLength = -1;
	/*
	 * If true, data has been sent sucessfully, otherwise false.
	 */

	public boolean dataSent = false;
	/*
	 * If a serversocket was passed in during creation, isServerSocket is true,
	 * otherwise false.
	 */

	public boolean isServerSocket = false;
	ServerSocket ss;
	/*
	 * if false, Proxy will close data to outgoing connection after sending
	 * data.
	 */

	boolean persistent = true;

	/*
	 * Creates a proxy to copy data from the client-socket to the server-socket.
	 * If persistent, connection to the client-socket will remain open after
	 * proxying data.
	 */

	public Proxy(Socket client_socket, Socket server_socket, boolean persistent) {

		t = new Thread(this, "Proxy Thread");
		System.out.println("Child thread: " + t);
		this.server_socket = server_socket;
		this.client_socket = client_socket;
		this.persistent = persistent;
	}

	/**
	 * Creates a proxy to copy data from the socket that ss accepts to the
	 * server-socket. BrowserHash maintains a hash between the browserID and the
	 * accepted socket. If persistent, connection to the accepted socket will
	 * remain open after proxying data.
	 */
	public Proxy(ServerSocket ss, Socket server_socket, SocketList browserHash,
			boolean persistent) {
		t = new Thread(this, "Proxy Thread");
		System.out.println("Child thread: " + t);
		this.ss = ss;
		isServerSocket = true;
		this.persistent = persistent;
		this.server_socket = server_socket;
		this.browserHash = browserHash;
	}

	/*
	 * Main execution of Proxy thread.
	 */

	public void run() {
		System.out.println(t + " of preprocessType: " + preprocessType
				+ " has started.");
		if (isServerSocket) {
			try {
				client_socket = ss.accept();
				System.out.println("Connection accepted in Proxy!");
				// save the client socket in hashtable. The hashtable will
				// notify.
				bID = client_socket.toString();

				if (browserHash != null)
					browserHash.put(bID, client_socket);
			} catch (IOException e) {
				System.err.println("Could not accept client socket for proxy");
				if (!getPreprocess().equals("decodeFromRequest")) // mostly
																	// normal
					e.printStackTrace();
				dataSent = false;
				System.out.println(t + " of preprocessType: " + preprocessType
						+ " has returned with exception.");
				return;
			}
		}
		try {
			createStreams(client_socket, server_socket);
			process();
		} catch (IOException e) {
			// problem. firewall sent error, so there's no Browser ID?
			if (!getPreprocess().equals("decodeFromRequest")) // mostly normal
				e.printStackTrace();
			dataSent = false;
			System.out.println(t + " of preprocessType: " + preprocessType
					+ " has returned with exception.");
			return;
		}

		try {
			toSocket();
		} catch (IOException e) {
			if (!getPreprocess().equals("decodeFromRequest")) // mostly normal
				e.printStackTrace();
			dataSent = false;
			System.out.println(t + " of preprocessType: " + preprocessType
					+ " has returned with exception.");
			return;
		}

		dataSent = true;
		System.out.println(t + " of preprocessType: " + preprocessType
				+ " has returned.");
	}

	/*
	 * Creates the necessary streams from the passed in sockets.
	 */
	void createStreams(Socket client_socket, Socket server_socket)
			throws IOException {
		// create the streams
		try {
			from_server = server_socket.getInputStream();
			http_from_server = new HttpInputStream(from_server);
			to_server = server_socket.getOutputStream();
			http_to_server = new HttpOutputStream(to_server, http_from_server);
		} catch (IOException e) {
			System.err.println("System could not connect to server.");
			e.printStackTrace();
			throw e;
		}
		try {
			from_client = client_socket.getInputStream();
			http_from_client = new HttpInputStream(from_client);
			// ENABLE the following in order to LOG socket data

			// if (preprocessType.equals ("decodeFromResponse"))

			// http_from_client = new LoggedInputStream(from_client,
			// HTTP.LOG_BIN+"RHAin.txt");
			// else
			// http_from_client = new LoggedInputStream(from_client,
			// HTTP.LOG_BIN+ "RHPin.txt");

			to_client = client_socket.getOutputStream();
			http_to_client = new HttpOutputStream(to_client, http_from_client);
		} catch (IOException e) {
			System.err.println("System could not connect to client.");
			System.err.println(e.toString());
			throw e;
		}
	}

	/*
	 * Processes bytes depending on processType before proxying the data along.
	 */

	void process() throws IOException {

		if (preprocessType.equals("decodeFromResponse")) {
			try {
				// TEST code
				// System.out.printin(httpfrom-client.bytesAvailable() +
				// " bytes availble to be read.");

				http_from_client.readHeaders();
				// TEST code
				// System.out.printin(http-from-client.bytesAvailable() +
				// " bytes availble to be read."); 200
				bID = http_from_client.getHeader("Browser- ID");
				if (bID != null)
					System.out
							.println("The browser ID read from RHP is " + bID);
				else {
					// there must have been an error! firewall is probably
					// throwing error message
					throw new IOException(
							"BID is null. Firewall sending error message?");
				}
				String clStr = http_from_client.getHeader("Content-length");
				if (clStr != null)
					setContentLength(Integer.parseInt(clStr));
				else
					setContentLength(-1);
			} catch (IOException e) {
				System.err.println("Couldn't read headers in preprocessing");
				e.printStackTrace();
				throw e;
			}
		} else if (preprocessType.equals("decodeFromRequest")) {
			// do nothing
		}
	}

	/*
	 * Proxies the data from one socket to another.
	 */

	void toSocket() throws IOException {
		// start proxying
		int bytes_read;
		byte[] buffer;
		// check to see if we have a content-length defined. If we do, we want
		// to
		// stop when we have proxied reached contentLength.
		if (getContentLength() != -1) {
			if (getContentLength() < 4096)
				buffer = new byte[getContentLength()];
			else
				buffer = new byte[4096];
			int bytes_read_total = 0;
			try {
				while ((bytes_read = http_from_client.read(buffer)) != -1) {
					http_to_server.write(buffer, 0, bytes_read);
					http_to_server.flush();
					bytes_read_total += bytes_read;
					if (bytes_read_total >= getContentLength())
						break;
				}
			} catch (IOException e) {
				System.out
						.println("IOException in proxying. Socket seems dead.");
				// e.printStackTrace(; 260
				throw e;
			}
		} else {
			buffer = new byte[4096];
			try {
				while ((bytes_read = http_from_client.read(buffer)) != -1) {
					to_server.write(buffer, 0, bytes_read);
					to_server.flush();
				}
			} catch (IOException e) {
				System.out
						.println("IOException in proxying. Socket to WWW server seems dead.");
				// e.printStackTrace(;
				throw e;
			}
		}
		// if a non-persistent proxy was requested, then we must
		// close the streams

		if (!persistent) {
			try {
				to_server.close();
			} catch (IOException e) {
			}
		}
	}

	/*
	 * Sets the preprocess type which affects what kind of preprocessing needs
	 * to be done. 290
	 */

	public void setPreprocess(String preprocessType) {
		this.preprocessType = preprocessType;
	}

	/*
	 * Gets the preprocess type which affects what kind of preprocessing needs
	 * to be done.
	 */

	public String getPreprocess() {
		return preprocessType;
	}

	/*
	 * Sets the contentLength of the body. This forces the class to only proxy
	 * this many bytes of data.
	 */

	public void setContentLength(int size) {
		this.contentLength = size;
	}

	/*
	 * Gets the contentLength of the body. This forces the class to only proxy
	 * this many bytes of data.
	 */

	public int getContentLength() {
		return contentLength;
	}/*
	 * Gets the stored browserID 320
	 */

	public String getBrowserID() {
		return bID;
	}
}