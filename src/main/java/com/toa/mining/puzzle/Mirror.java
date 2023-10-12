package com.toa.mining.puzzle;

import net.runelite.api.Point;
import net.runelite.api.coords.Direction;

public class Mirror
{
	private Point location;
	private Direction direction;

	public Mirror(Point location, Direction direction) {
		this.location = location;
		this.direction = direction;
	}

	public Point getLocation() {
		return location;
	}

	public Direction getDirection() {
		return direction;
	}
}
