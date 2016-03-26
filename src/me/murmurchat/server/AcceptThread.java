package me.murmurchat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class AcceptThread extends Thread
{
	public static final int PORT = 21212;

	ServerSocket serverSocket;

	ArrayList<ConnectedUser> connectedUsers = new ArrayList<ConnectedUser>();

	Thread userRemover;

	public AcceptThread()
	{
		try
		{
			serverSocket = new ServerSocket(PORT);
		}
		catch (IOException e)
		{
			System.err.println("Error creating server.");
			System.exit(1);
		}

		userRemover = new Thread(new Runnable()
		{
			public void run()
			{
				while (true)
					for (int i = 0; i < connectedUsers.size(); i++)
						if (!connectedUsers.get(i).connected)
							connectedUsers.remove(i);
			}
		});
		userRemover.start();
	}

	public void run()
	{
		System.out.println("Waiting for connections...");
		while (true)
		{
			try
			{
				Socket s = serverSocket.accept();
				
				System.out.println("User connected");
				ConnectedUser user = new ConnectedUser(s);

				connectedUsers.add(user);
				
				System.out.println("Added user");
			}
			catch (IOException e)
			{
				System.err.println("Error accepting client.");
			}
		}
	}
}
