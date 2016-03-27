package me.murmurchat.server;

import java.util.HashMap;

public class FileNames
{
	public static HashMap<String, String> nameMap = new HashMap<String, String>();

	static int counter = 0;

	public static String getFilename(byte[] key)
	{
		if (!nameMap.containsKey(new String(key)))
			nameMap.put(new String(key), "" + counter++);

		return nameMap.get(new String(key));
	}
}
