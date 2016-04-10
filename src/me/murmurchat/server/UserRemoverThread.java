package me.murmurchat.server;

public class UserRemoverThread extends Thread
{
	public UserRemoverThread()
	{
		this.setName("User Remover Thread");
	}
	
	public void run()
	{		
		while (!this.isInterrupted())
		{
			for (int i = 0; i < MurmurServer.instance.connectedUsers.size(); i++)
				if (!MurmurServer.instance.connectedUsers.get(i).connected)
					MurmurServer.instance.connectedUsers.remove(i);
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				this.interrupt();
			}
		}
	}
}
