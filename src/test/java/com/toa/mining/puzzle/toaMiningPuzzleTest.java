package com.toa.mining.puzzle;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class toaMiningPuzzleTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ToaMiningPuzzle.class);
		RuneLite.main(args);
	}
}