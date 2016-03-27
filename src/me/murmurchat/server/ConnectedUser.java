package me.murmurchat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ConnectedUser extends Thread
{
	public static final int KEY_SIZE = 294;

	static int userIdCount = 0;
	
	Socket socket;

	DataOutputStream out = null;
	DataInputStream in = null;

	boolean connected = true;
	boolean hasHeartbeat = true;

	PublicKey publicKey;

	int userId = ++userIdCount;
	
	String secretMessage;

	public ConnectedUser(Socket socket)
	{
		displayMessage("User connected.");
		this.socket = socket;

		try
		{
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		}
		catch (IOException e)
		{
			displayMessage("Error creating data streams.");
			disconnect();
			return;
		}

		this.start();
	}

	public void run()
	{
		// Read the 294 bytes of the 2048 bit RSA public key.
		byte[] keyBytes = new byte[294];
		try
		{
			in.readFully(keyBytes);
		}
		catch (IOException e)
		{
			displayMessage("Error reading public key.");
			disconnect();
		}

		KeyFactory keyFactory = null;
		try
		{
			keyFactory = KeyFactory.getInstance("RSA");
		}
		catch (NoSuchAlgorithmException e)
		{
			System.err.println("Error no RSA key factory found program must exit.");
			System.exit(1);
		}

		try
		{
			publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
		}
		catch (InvalidKeySpecException e)
		{
			displayMessage("Client sent malformed public key.");
			disconnect();

			return;
		}

		try
		{
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.ENCRYPT_MODE, publicKey);

			SecureRandom random = new SecureRandom();
			StringBuilder strBuilder = new StringBuilder();
			for (int i = 0; i < 32; i++)
				strBuilder.append(random.nextInt(256));

			secretMessage = strBuilder.toString();

			try
			{
				byte[] msg = c.doFinal(secretMessage.getBytes());
				out.writeInt(msg.length);
				out.write(msg);
			}
			catch (IOException e)
			{
				displayMessage("Error sending secret message.");
				disconnect();
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			System.err.println("Error no RSA key factory found program must exit.");
			System.exit(1);
		}
		catch (NoSuchPaddingException e)
		{
			e.printStackTrace();
			disconnect();
		}
		catch (InvalidKeyException e)
		{
			displayMessage("Client sent invalid public key.");
			e.printStackTrace();
			disconnect();
		}
		catch (IllegalBlockSizeException e)
		{
			displayMessage("Error generating secret message.");
			e.printStackTrace();
			disconnect();
		}
		catch (BadPaddingException e)
		{
			displayMessage("Error generating secret message.");
			e.printStackTrace();
			disconnect();
		}

		byte[] secretReply = new byte[secretMessage.getBytes().length];

		try
		{
			in.readFully(secretReply);
		}
		catch (IOException e)
		{
			displayMessage("Error reading secret message.");
			disconnect();
		}

		if (!(new String(secretReply).equals(secretMessage)))
		{
			disconnect();
			return;
		}

		displayMessage("User authenticated.");

		File f = new File("res/users/", publicKey.getEncoded().toString());
		try
		{
			f.createNewFile();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			disconnect();
			return;
		}

		try
		{
			DataInputStream fileIn = new DataInputStream(new FileInputStream(f));

			int curByte = -1;
			while ((curByte = fileIn.read()) != -1)
				out.write((byte) curByte);

			fileIn.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			disconnect();
			return;
		}

		int packetType = -1;
		try
		{
			while ((packetType = in.read()) != -1)
			{
				switch (packetType)
				{
				case 1:
					hasHeartbeat = true;
					break;
				default:
					displayMessage(("Client sent unknown packet type " + packetType));
					break;
				}
			}
		}
		catch (IOException e)
		{
			displayMessage("Error reading from client.");
			e.printStackTrace();
		}
	}

	void disconnect()
	{
		displayMessage("Disconnecting client.");
		try
		{
			socket.close();
		}
		catch (IOException e)
		{
			displayMessage("Error closing socket for client.");
			return;
		}

		connected = false;
		this.interrupt();
	}
	
	void displayMessage(String msg)
	{
		System.out.println("[" + userId + "] " + msg);
	}
}
