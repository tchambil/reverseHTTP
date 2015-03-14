package rhttp;

import java.io.*;
import java.net.*;
import java.util.Date;

/*
 * A class containing describing the Reverse H TTP Agent
 * author <a href='mailto:esittmit.edu">Eric Sit</a>
 * version %I%, %C%
 * (c) Eric Sit 1999*/

public class RHA implements Runnable {
	Thread t;
	String wwwHost;
	int wwwPort;
	static String RHPHost;
	static int RIPPort;
	static int RHPPort;
	Socket control_socket = null;
	Socket data_socket = null;
	Socket server_socket = null;
	InputStream from_control = null;
	HttpInputStream httpFromControl;
	OutputStream to_control = null;
	HttpOutputStream httpToControl;
	InputStream from_server = null;
	HttpInputStream httpFromServer;
	OutputStream to_server = null;
	HttpOutputStream httpToServer;
	Proxy agent;
	ProxyFile responseProxy;
	String bID;
	String aID;
	boolean init = true;

	/*
	 * Creates an reverse HTTP agent object. The agent is for web host wwwHost,
	 * port wunwPort. The Reverse HTTP proxy to be given access is RHPHost, port
	 * RHPPort.
	 */

	public RHA(String wwwHost, int wwwPort, String RHPHost, int RHPPort) {
		this.wwwHost = wwwHost;
		this.wwwPort = wwwPort;
		this.RHPHost = RHPHost;
		this.RHPPort = RHPPort;
		t = new Thread(this, "RHA Thread");
		System.out.println("New thread: " + t);
		t.start();
	}

	/**
	 * Starts the Reverse HITTP Agent. The agent is for web host wwwHost, port
	 * wwwPort. The Reverse HTTP proxy to be given access is pHost, port pPort.
	 * The firewall proxy is at proxyllost, port proryPort. Usage: lava RHA
	 * ccwwwlost> <wwwPort> <pilost> 'ccpPort> <proxyllost> <proxyPort>"
	 */
	public static void main(String[] args) throws IOException {
		String wwwHost;
		int wwwPort;
		String RHPHost;
		int RHPPort;
		try {
			// check arguments
			if (args.length != 6)
				throw new IllegalArgumentException("Wrong number of arguments.");

			// Get command-line arguments.
			wwwHost = args[0];
			wwwPort = Integer.parseInt(args[1]);
			RHPHost = args[2];
			RHPPort = Integer.parseInt(args[3]);
			System.getProperties().put("proxySet", "true");
			System.getProperties().put("proxyHost", args[4]);
			System.getProperties().put("proxyPort", args[5]);
			System.out.println("Starting RHA for web server " + wwwHost + ": "
					+ wwwPort + ", using Proxy "
					+ System.getProperties().getProperty("proxyHost") + ":"
					+ System.getProperties().getProperty("proxyPort"));
			RHA thisRHA = new RHA(wwwHost, wwwPort, RHPHost, RHPPort);
		} catch (Exception e) {
			System.err.println(e);
			System.err
					.println("Usage: java RHA <wwwHost> <wwwPort><pHost> <pPort> <proxyHost> <proxyPort>");
		}
	}

	/**
	 * Handle incoming responses and make the request to the web server
	 * */

	public void run() {
		// figure out the locahost
		String localHost;
		String localIP;
		try {
			localHost = InetAddress.getLocalHost().getHostName();
			localIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException ex) {
			localHost = "localhost";
			localIP = "127.0.0.1";
		}
		System.out.println("This machine is called " + localHost
				+ " with an IP of " + localIP);
		initialize();
		System.out.println("Control connection initialized.");

		while (true) {
			// wait until data is available before opening client to WWW
			waitForData(httpFromControl);

			if (init) {
				// skip over initial Ok header
				try {
					httpFromControl.readHeaders();
					init = false;
				} catch (IOException e) {
					System.err
							.println("Couldn't read initial headers on response");
					e.printStackTrace();
					try {
						httpFromControl.close();
					} catch (IOException f) {
						System.err.println("Couldn't close control.");
						e.printStackTrace();
					}
					continue;
				}
				// we don't care for what we just read, so just dump it.
				httpFromControl.resetHeaders();
				waitForData(httpFromControl);
			}
			// 1 add an agent to handle requests coming in to RHA from RHP
			addAgent();
			// 1/ send the response back to RHP as a request

			try {
				agent.join();
			} catch (InterruptedException e) {
				System.err.println("agent interupted!");
				e.printStackTrace();
			}

			if (!agent.dataSent) {
				System.err.println("Agent Abort !");
				// abort 170
				System.err.println("closing control");
				try {
					control_socket.close();
				} catch (IOException e) {
					System.err.println("Can't close control connection");
				}

				System.err.println("Attempting to reinitialize");
				initialize();
				init = true;
				// skip to end of loop...
				continue;
			} else {
				System.out.println("Agent data sent!");
				System.out.println();
			}

			System.out
					.println("Trying to encode as request and send back to RHP.");
			sendResponse();

			try {
				responseProxy.join();
			} catch (InterruptedException e) {
				System.err.println("response proxy interupted!");
				e.printStackTrace();
			}

			if (!responseProxy.dataSent) {
				System.err.println("Response Abort !");
				// abort
				// we don't care that the data didn't make it to RHP.
				// they will rerequest it.
				continue;

			} else {
				System.out.println("Response proxy data sent!");
				System.out.println();
			}
		}

	}

	/*
	 * * Initializes connection to RHP
	 */
	public void initialize() {
		try {
			control_socket = new Socket(System.getProperties().getProperty(
					"proxyHo st"), Integer.parseInt(System.getProperties()
					.getProperty("proxyPort")));
			from_control = control_socket.getInputStream();
			httpFromControl = new HttpInputStream(from_control);
			// TEST CODE
			// /httpFromControl = new LoggedInputStream(from_control,
			// "RHAin.txt");
			//

			to_control = control_socket.getOutputStream();
			httpToControl = new HttpOutputStream(to_control, httpFromControl);
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: RHP.");
			System.exit(1);

		} catch (IOException e) {
			System.err.println("Couldn't get I/0 for the connection to: RHP.");
			System.exit(1);
		}
		// generate key for agent

		aID = wwwHost;
		try {
			httpToControl.write(HTTP.METHOD_POST + " http://" + RHPHost + ":"
					+ RHPPort + "/" + HTTP.CONTROL_BIN + " HTTP/1.0\r\n");
			httpToControl.write(" Content-length: 0\r\n");
			httpToControl.write("Agent-ID: " + aID + "\r\n");
			httpToControl.write("\r\n");
			httpToControl.flush();
		} catch (IOException e) {
			System.err.println("Could not write to client!");
			System.exit(1);
		}
	}

	/*
	 * Create an agent that makes requests on behalf of the client and sends the
	 * response back to the client.
	 */

	public void addAgent() {
		try {
			server_socket = new Socket(wwwHost, wwwPort);
			from_server = server_socket.getInputStream();
			httpFromServer = new HttpInputStream(from_server);
			to_server = server_socket.getOutputStream();
			httpToServer = new HttpOutputStream(to_server, httpFromServer);
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host:" + wwwHost);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Couldn't get I/0 for the connection to: "
					+ wwwHost + ": " + wwwPort);
			System.exit(1);
		}
		System.out.println("Starting agent");

		// make a proxy to web server. persistent is set to true becuase it is
		// the web server's responsibility to close the connection
		agent = new Proxy(control_socket, server_socket, true);

		agent.setPreprocess("decodeFromResponse");
		agent.start();
	}

	/*
	 * 300 Sends back the response to the RHP request in the form of a request.
	 */
	public void sendResponse() {
		try {
			// First grab the browser ID from the agent and save it.
			bID = agent.bID;
			System.out
					.println("Attempting to setup connection to RHP for response.");

			// Create a brand new socket.
			data_socket = new Socket(System.getProperties().getProperty(
					"proxyHo st"), Integer.parseInt(System.getProperties()
					.getProperty("proxyPort")));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: RHP.");
			System.exit(1);
		} catch (IOException e) {
			System.err
					.println("Couldn't get I/0 for the connection to: RHP. RHP seems to have disappeared.");
			System.exit(1);
		}
		System.out.println("Starting response ProxyFile");
		responseProxy = new ProxyFile(server_socket, data_socket, true);
		responseProxy.setPreprocess("encodeAsRequest");
		responseProxy.bID = bID;
		responseProxy.aID = aID;
		responseProxy.start();
		/*
		 * Waits until data is available in testIn.
		 */
	}

	void waitForData(HttpInputStream testIn) {
		int bytes_available = -1;
		while (true) {
			try {
				bytes_available = testIn.available();

			} catch (IOException e) {
				e.printStackTrace();
			}
			if (bytes_available > 0) {
				System.out.println(bytes_available + " available to be read.");
				break;
			} else {
				t.yield();
			}
		}
	}/*
	 * Returns the port of the reverse HTTP Proxy that the agent is giving
	 * access to.
	 */

	public static int getRHPPort() {
		return RHPPort;
	}

	/*
	 * Returns the hostname of the reverse HTTP Proxy that the agent is giving
	 * access to.
	 */

	public static String getRHPHost() {
		return RHPHost;
	}
}