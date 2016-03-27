package me.murmurchat.server;

import java.util.ArrayList;

public class MurmurServer
{
	public static MurmurServer instance;

	ArrayList<ConnectedUser> connectedUsers;

	AcceptThread acceptThread;
	HeartbeatThread heartbeatThread;
	Thread userRemover;

	public MurmurServer()
	{
		connectedUsers = new ArrayList<ConnectedUser>();
		
		acceptThread = new AcceptThread();
		heartbeatThread = new HeartbeatThread();
		
		userRemover = new Thread(new Runnable()
		{
			public void run()
			{
				while (true)
				{
					for (int i = 0; i < connectedUsers.size(); i++)
						if (!connectedUsers.get(i).connected)
							connectedUsers.remove(i);
					try
					{
						Thread.sleep(10);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void start()
	{
		acceptThread.start();
		heartbeatThread.start();
		userRemover.start();
	}

	public static void main(String[] args)
	{
		instance = new MurmurServer();
		instance.start();
	}
}
