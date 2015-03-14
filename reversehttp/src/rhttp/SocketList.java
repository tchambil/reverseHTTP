package rhttp;

import java.io.*;
import java.net.*;
import java.util.*;

/*
 * A class containing a table of connections to servers
 *
 * author <a href="mailto: esitemit. edu" >Eric Sit</a>
 * version %I%, %G% 10
 * (c) Eric Sit 1999*/

public class SocketList extends Hashtable {
	public void SocketList() {
	}

	public synchronized Object ask(Object key) {
		try {
			wait();
		} catch (InterruptedException e) {
			System.err.println("Interrupted Exception caught");
		}
		return get(key);

	}

	public synchronized Object deliver(Object key, Object value) {
		Object obj = put(key, value);
		notify();
		return obj;
	}

	public String listAll() {
		return this.toString();
	}
}