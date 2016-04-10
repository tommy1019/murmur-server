package me.murmurchat.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class MurmurServer
{
	public static MurmurServer instance;

	ArrayList<ConnectedUser> connectedUsers;

	AcceptThread acceptThread;
	HeartbeatThread heartbeatThread;
	UserRemoverThread userRemover;

	public MurmurServer()
	{
		connectedUsers = new ArrayList<ConnectedUser>();

		acceptThread = new AcceptThread();
		heartbeatThread = new HeartbeatThread();
		userRemover = new UserRemoverThread();

		FileNames.loadFromFile();
	}

	public void start()
	{
		acceptThread.start();
		heartbeatThread.start();
		userRemover.start();

		handelInput();

		heartbeatThread.interrupt();
		userRemover.interrupt();
		acceptThread.interrupt();

		for (ConnectedUser u : connectedUsers)
		{
			u.interrupt();
			u.disconnect();
		}

		System.out.println("Requested closing of all threads.");
	}

	public void handelInput()
	{
		Scanner input = new Scanner(System.in);
		boolean running = true;

		while (running)
		{
			String line = input.nextLine();

			switch (line)
			{
			case "stop":
				running = false;
				break;
			case "list":
				System.out.println("There are " + connectedUsers.size() + " users online.");
				break;
			}
		}

		input.close();
	}

	public static ConnectedUser getUser(byte[] key)
	{
		for (ConnectedUser u : instance.connectedUsers)
			if (Arrays.equals(key, u.keyBytes))
				return u;

		return null;
	}

	public static void main(String[] args)
	{
		instance = new MurmurServer();
		instance.start();
	}
}
