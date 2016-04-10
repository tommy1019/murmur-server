package me.murmurchat.server;

import java.io.IOException;

public class HeartbeatThread extends Thread
{
	public HeartbeatThread()
	{
		this.setName("Heartbeat Thread");
	}
	
	public void run()
	{
		while (!this.isInterrupted())
		{
			for (int i = 0; i < MurmurServer.instance.connectedUsers.size(); i++)
			{
				ConnectedUser user = MurmurServer.instance.connectedUsers.get(i);

				if (user.hasHeartbeat)
				{
					try
					{
						user.out.write(1);
					}
					catch (IOException e)
					{
						System.out.println("Error sending hearbeat.");
					}
					user.hasHeartbeat = false;
				}
				else
				{
					user.disconnect();
				}
			}

			try
			{
				Thread.sleep(10000);
			}
			catch (InterruptedException e)
			{
				this.interrupt();
			}
		}
	}
}
