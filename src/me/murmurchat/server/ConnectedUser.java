package me.murmurchat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

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
	byte[] keyBytes;

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
		keyBytes = new byte[294];
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
			keyBytes = publicKey.getEncoded();
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
			displayMessage(secretMessage);
			
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
		catch (BadPaddingException | IllegalBlockSizeException e)
		{
			displayMessage("Error generating secret message.");
			e.printStackTrace();
			disconnect();
		}

		byte[] secretReply = new byte[secretMessage.getBytes().length];

		try
		{
			in.read(secretReply);
		}
		catch (IOException e)
		{
			displayMessage("Error reading secret message.");
			disconnect();
		}

		if (!(new String(secretReply).equals(secretMessage)))
		{
			displayMessage("Client sent incorrect secret key, disconnecting");
			disconnect();
			return;
		}

		displayMessage("User authenticated.");

		File f = new File("res/users/", FileNames.getFilename(publicKey.getEncoded()));
		if (f.exists())
		{
			try
			{
				out.write(0);
				sendUserAccountDatabase(out);
			}
			catch (IOException e)
			{
				displayMessage("Error sending user file");
				disconnect();
				return;
			}
		}
		else
		{
			try
			{
				out.write(1);
				readAccountDatabase(in);
			}
			catch (IOException e)
			{
				displayMessage("Error reading user file");
				disconnect();
				return;
			}
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
				case 2:
					readAccountDatabase(in);
					break;
				case 8:
					displayMessage("Sending message");
					byte[] receiver = Util.readPublicKey(in);
					byte[] msg = Util.readPrefixedBytes(in);

					ConnectedUser receiverUser = MurmurServer.getUser(receiver);
					if (receiverUser != null)
					{
						displayMessage("Found user");
						receiverUser.out.write(8);
						receiverUser.out.write(keyBytes);
						receiverUser.out.writeInt(msg.length);
						receiverUser.out.write(msg);
					}
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
		}
	}

	void readAccountDatabase(DataInputStream in) throws IOException
	{
		displayMessage("User updating profile");
		int numBytes = in.readInt();
		byte[] file = new byte[numBytes];
		in.read(file);

		File userFile = new File("res/users/", FileNames.getFilename(publicKey.getEncoded()));
		DataOutputStream fOut = new DataOutputStream(new FileOutputStream(userFile));
		fOut.write(file);
		fOut.close();
		displayMessage("Done updating");
	}

	void sendUserAccountDatabase(DataOutputStream out) throws IOException
	{
		DataInputStream fileIn = null;
		try
		{
			fileIn = new DataInputStream(new FileInputStream(new File("res/users/", FileNames.getFilename(publicKey.getEncoded()))));
		}
		catch (FileNotFoundException e)
		{
			displayMessage("User does not have a database, disconnecting");
			disconnect();
			return;
		}

		ArrayList<Byte> bytes = new ArrayList<Byte>();

		int curByte = -1;
		while ((curByte = fileIn.read()) != -1)
			bytes.add((byte) curByte);
		fileIn.close();

		out.writeInt(bytes.size());
		out.write(Util.toByteArray(bytes));
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
