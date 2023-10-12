package com.toa.mining.puzzle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import net.runelite.api.Point;
import net.runelite.api.coords.Direction;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class State
{
	Point centroid;
	Point emitterStart;
	Set<Mirror> staticMirrors;
	Set<Mirror> deployedMirrors;
	Set<Point> solids;
	Set<Point> collector;

	public State(Point centroid, Point emitterStart, Set<Mirror> staticMirrors, Set<Mirror> deployedMirrors, Set<Point> solids, Set<Point> collector) {
		this.centroid = centroid;
		this.emitterStart = emitterStart;
		this.staticMirrors = staticMirrors;
		this.deployedMirrors = deployedMirrors;
		this.solids = solids;
		this.collector = collector;

	}

	public State withMirrors(Set<Mirror> deployedMirrors) {
		return new State(centroid, emitterStart, staticMirrors, deployedMirrors, solids, collector);
	}

	private static Point getNextPoint(Point point, Direction direction) {
		switch(direction) {
			case NORTH:
				return new Point(point.getX(), point.getY() + 1);
			case EAST:
				return new Point(point.getX() + 1, point.getY());
			case SOUTH:
				return new Point(point.getX(), point.getY() - 1);
			case WEST:
				return new Point(point.getX() - 1, point.getY());
		}
		return point;
	}

	Direction getMirroredDirection(Direction direction, Direction mirrorDirection) {
		switch(mirrorDirection) {
			case NORTH:
				if(direction == Direction.WEST) {
					return Direction.NORTH;
				} else {
					return null;
				}
			case EAST:
				if(direction == Direction.NORTH) {
					return Direction.EAST;
				} else {
					return null;
				}
			case SOUTH:
				if(direction == Direction.EAST) {
					return Direction.SOUTH;
				} else {
					return null;
				}
			case WEST:
				if(direction == Direction.SOUTH) {
					return Direction.WEST;
				} else {
					return null;
				}
		}
		return null;
	}

	public Set<ImmutablePair<Point, Direction>> getBeamLocations() {
		Direction activeDirection = Direction.WEST;
		Point activePoint = emitterStart;
		activePoint = getNextPoint(activePoint, activeDirection);
		Set<ImmutablePair<Point, Direction>> visitedPoints = new HashSet<ImmutablePair<Point, Direction>>();
		visitedPoints.add(new ImmutablePair(activePoint, activeDirection));

		Set<Point> endPoints = new HashSet<Point>();
		endPoints.addAll(collector);
		endPoints.addAll(solids);

		while(
			(activeDirection != null) &&
			(!endPoints.contains(activePoint)))
		{
			boolean hitAMirror = false;

			Point finalActivePoint = activePoint;
			Optional<Mirror> staticMirror = staticMirrors.stream().filter(mirror -> mirror.getLocation().equals(finalActivePoint)).findFirst();
			if(staticMirror.isPresent()) {
				activeDirection = getMirroredDirection(activeDirection, staticMirror.get().getDirection());
				hitAMirror = true;
			}

			if(!hitAMirror) {
				Optional<Mirror> deployedMirror = deployedMirrors.stream().filter(mirror -> mirror.getLocation().equals(finalActivePoint)).findFirst();
				if(deployedMirror.isPresent()) {
					activeDirection = getMirroredDirection(activeDirection, deployedMirror.get().getDirection());
					hitAMirror = true;
				}
			}

			if(!hitAMirror) {
				visitedPoints.add(new ImmutablePair(activePoint, activeDirection));
			}

			if(activeDirection != null) {
				activePoint = getNextPoint(activePoint, activeDirection);
			}

		}
		return visitedPoints;
	}

	public boolean isSolution() {
		Direction activeDirection = Direction.WEST;
		Point activePoint = emitterStart;
		activePoint = getNextPoint(activePoint, activeDirection);

		while(
			(activeDirection != null) &&
				(!solids.contains(activePoint)))
		{
			boolean hitAMirror = false;

			Point finalActivePoint = activePoint;
			Optional<Mirror> staticMirror = staticMirrors.stream().filter(mirror -> mirror.getLocation().equals(finalActivePoint)).findFirst();
			if(staticMirror.isPresent()) {
				activeDirection = getMirroredDirection(activeDirection, staticMirror.get().getDirection());
				hitAMirror = true;
			}

			if(!hitAMirror) {
				Optional<Mirror> deployedMirror = deployedMirrors.stream().filter(mirror -> mirror.getLocation().equals(finalActivePoint)).findFirst();
				if(deployedMirror.isPresent()) {
					activeDirection = getMirroredDirection(activeDirection, deployedMirror.get().getDirection());
					hitAMirror = true;
				}
			}

			if(activeDirection != null) {
				activePoint = getNextPoint(activePoint, activeDirection);
			}


			if(collector.contains(activePoint)) {
				return true;
			}
		}
		return false;
	}


	public static Set<Mirror> getSolution(State startState) {
		State strippedState = startState.withMirrors(new HashSet<>());
		Queue<State> solutionQueue = new ArrayDeque<>();
		solutionQueue.offer(strippedState);

		while(solutionQueue.size() > 0 && !solutionQueue.peek().isSolution()) {
			State solution = solutionQueue.poll();
			if(solution.deployedMirrors.size() > 3) {
				continue;
			}

			for(ImmutablePair<Point, Direction> pair : solution.getBeamLocations()) {
				for(Direction direction : Direction.values()) {
					Set<Mirror> newMirrors = new HashSet<Mirror>(solution.deployedMirrors);
					newMirrors.add(new Mirror(pair.left, direction));
					solutionQueue.add(solution.withMirrors(newMirrors));
				}
			}
		}

		if(solutionQueue.size() > 0) {
			System.out.println("Found solution");
			solutionQueue.peek().printLayout();
		} else {
			System.out.println("No solution");
		}
		return solutionQueue.peek().deployedMirrors;
	}

	public String stringLayout() {
		StringBuilder strb = new StringBuilder();
		for(int y = centroid.getY() + Facts.MAX_NORTH_FROM_CENTER; y >= centroid.getY() - Facts.MAX_SOUTH_FROM_CENTER; y--) {
			for(int x = centroid.getX() - Facts.MAX_WEST_FROM_CENTER; x <= centroid.getX() + Facts.MAX_EAST_FROM_CENTER; x++) {
				Point point = new Point(x, y);
				Optional<Mirror> staticMirror = staticMirrors.stream().filter(mirror -> mirror.getLocation().equals(point)).findFirst();
				Optional<Mirror> deployedMirror = deployedMirrors.stream().filter(mirror -> mirror.getLocation().equals(point)).findFirst();

				if(emitterStart.equals(point)) {
					strb.append("E");
				}
				else if(solids.contains(point)) {
					strb.append("X");
				}
				else if(collector.contains(point)) {
					strb.append("C");
				}
				else if(staticMirror.isPresent()) {
					switch(staticMirror.get().getDirection()) {
						case NORTH:
							strb.append("M");
							break;
						case EAST:
							strb.append("M");
							break;
						case SOUTH:
							strb.append("M");
							break;
						case WEST:
							strb.append("M");
							break;
					}
				}
				else if(deployedMirror.isPresent()) {
					switch(deployedMirror.get().getDirection()) {
						case NORTH:
							strb.append("n");
							break;
						case EAST:
							strb.append("e");
							break;
						case SOUTH:
							strb.append("s");
							break;
						case WEST:
							strb.append("w");
							break;
					}
				}
				else {
					strb.append(" ");
				}
			}
			strb.append("\n");
		}
		return strb.toString();
	}

	public void printLayout() {
		System.out.println(stringLayout());
	}
}
