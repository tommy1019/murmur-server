package me.murmurchat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AcceptThread extends Thread
{
	public static final int PORT = 21212;

	ServerSocket serverSocket;

	public AcceptThread()
	{
		this.setName("Accept Thread");
		
		try
		{
			serverSocket = new ServerSocket(PORT);
			serverSocket.setSoTimeout(1000);
		}
		catch (IOException e)
		{
			System.err.println("Error creating server.");
			System.exit(1);
		}
	}

	public void run()
	{
		System.out.println("Waiting for connections...");
		while (!this.isInterrupted())
		{
			try
			{
				Socket s = serverSocket.accept();
				ConnectedUser user = new ConnectedUser(s);

				MurmurServer.instance.connectedUsers.add(user);
			}
			catch (IOException e)
			{
			}
		}
	}
}
