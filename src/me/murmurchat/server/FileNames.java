package me.murmurchat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class FileNames
{
	public static final String DATABASE_FILENAME = "res/users.db";

	public static HashMap<String, String> nameMap = new HashMap<String, String>();

	static int counter = 0;

	static void loadFromFile()
	{
		File f = new File(DATABASE_FILENAME);

		if (f.exists())
		{
			try
			{
				DataInputStream in = new DataInputStream(new FileInputStream(f));

				while (true)
				{
					byte[] curKey = new byte[294];
					if (in.read(curKey) != 294)
						break;
					int id = in.readInt();
					
					counter = id + 1;
					nameMap.put(new String(curKey), "" + id);
				}

				in.close();
			}
			catch (FileNotFoundException e)
			{
				System.out.println("Error finding user database.");
				e.printStackTrace();
			}
			catch (IOException e)
			{
				System.out.println("Error reading from user database.");
				e.printStackTrace();
			}
		}
		
		System.out.println("Loaded " + counter + " users.");
	}

	public static String getFilename(byte[] key)
	{
		if (!nameMap.containsKey(new String(key)))
		{
			nameMap.put(new String(key), "" + counter);

			try
			{
				DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(DATABASE_FILENAME), true));

				out.write(key);
				out.writeInt(counter);

				out.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			counter++;
		}

		return nameMap.get(new String(key));
	}
}
