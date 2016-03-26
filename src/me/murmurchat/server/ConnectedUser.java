package me.murmurchat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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

	Socket socket;

	DataOutputStream out = null;
	DataInputStream in = null;

	boolean connected = true;

	PublicKey publicKey;

	String secretMessage;

	public ConnectedUser(Socket socket)
	{
		this.socket = socket;

		try
		{
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
		}
		catch (IOException e)
		{
			System.err.println("IOError connecting client");
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
			System.err.println("IOError connecting client");
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
			System.out.println("Client sent malformed public key, disconnecting.");
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
				out.write(c.doFinal(secretMessage.getBytes()));
			}
			catch (IOException e)
			{
				System.err.println("IOError connecting client");
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
			System.err.println("Client sent invalid public key, disconnecting.");
			e.printStackTrace();
			disconnect();
		}
		catch (IllegalBlockSizeException e)
		{
			System.err.println("Error generating secret message");
			e.printStackTrace();
			disconnect();
		}
		catch (BadPaddingException e)
		{
			System.err.println("Error generating secret message");
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
			System.err.println("IOError connecting client");
			disconnect();
		}

		if (!(new String(secretReply).equals(secretMessage)))
		{
			disconnect();
			return;
		}

		while (true)
		{
			System.out.println("Running!");
		}
	}

	void disconnect()
	{
		try
		{
			socket.close();
		}
		catch (IOException e)
		{
			System.err.println("Failed to close thread for client.");
			return;
		}

		connected = false;
		this.interrupt();
	}
}
