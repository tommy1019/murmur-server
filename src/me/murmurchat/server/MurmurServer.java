package me.murmurchat.server;

public class MurmurServer
{
	public static void main(String[] args)
	{
		AcceptThread thread = new AcceptThread();
		thread.start();
	}
}
