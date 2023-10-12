package com.toa.mining.puzzle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.lang3.tuple.ImmutablePair;

@Slf4j
@PluginDescriptor(
	name = "TOA Mining Puzzle",
	description = "",
	tags = {"TOA Mining Puzzle"}
)
public class ToaMiningPuzzle extends Plugin
{


	private final int HET_SEAL_NPC_ID = 11706;

	// GameObject annotations
	private final int EMITTER = 45_486;
	private final int COLLECTOR = 45485;
	private final int WALL_SOLID_CENTER = 45458;
	private final int WALL_SOLID_EDGE = 45460;
	private final int WALL_MINABLE_CENTER = 45462;
	private final int WALL_MINABLE_EDGE = 45464;
	private final int USER_MIRRORS = 45455;
	private final int STATIC_MIRRORS = 45456;
	private final int PICKAXE_STORAGE = 45468;
	private final int DOORS = 45135;
	private final int[] allObjects = {
		EMITTER,
		COLLECTOR,
		WALL_SOLID_CENTER,
		WALL_SOLID_EDGE,
		WALL_MINABLE_CENTER,
		WALL_MINABLE_EDGE,
		USER_MIRRORS,
		STATIC_MIRRORS,
		PICKAXE_STORAGE,
		DOORS
	};

	@Inject Client client;
	@Override
	protected void startUp()
	{

	}

	@Override
	protected void shutDown() throws Exception
	{

	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		Optional<NPC> seal = client.getNpcs().stream().filter((npc -> npc.getId() == HET_SEAL_NPC_ID)).findFirst();
		if(!seal.isPresent()) {
			return;
		}
		Point sealCenter = new Point(seal.get().getLocalLocation().getSceneX(), seal.get().getLocalLocation().getSceneY());

		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();

		Point emitterPoint = null;
		Set<Mirror> mirrors = new HashSet<Mirror>();
		Set<Point> solids = new HashSet<Point>();
		Set<Point> collector = new HashSet<Point>();

		for(int x = sealCenter.getX() - Facts.MAX_WEST_FROM_CENTER; x <= sealCenter.getX() + Facts.MAX_EAST_FROM_CENTER; x++) {
			for(int y = sealCenter.getY() - Facts.MAX_SOUTH_FROM_CENTER; y <= sealCenter.getY() + Facts.MAX_NORTH_FROM_CENTER; y++) {
				Tile tile = tiles[client.getPlane()][x][y];

				WallObject wallObject = tile.getWallObject();
				if(wallObject != null) {
					solids.add(new Point(x, y));
				}
				GameObject[] gameObjects = tile.getGameObjects();
				for(GameObject gameObject : gameObjects) {
					if(gameObject == null) {
						continue;
					}
					if(gameObject.getId() == EMITTER && emitterPoint == null) {
						emitterPoint = new Point(
							gameObject.getSceneMinLocation().getX(),
							gameObject.getSceneMinLocation().getY() + 1
						);
					}
					// If this is something that is solid
					if(Arrays.asList(EMITTER, WALL_SOLID_CENTER, WALL_SOLID_EDGE, PICKAXE_STORAGE).contains(gameObject.getId()) ) {
						solids.addAll(getSceneTiles(gameObject));
					}
					else if(COLLECTOR == gameObject.getId()) {
						collector.addAll(getSceneTiles(gameObject));
					}
					else if(gameObject.getId() == STATIC_MIRRORS) {
						mirrors.add(new Mirror(
							gameObject.getSceneMinLocation(),
							getMirrorDirection(gameObject))
						);
					}
				}
			}
		}


		for(int x = sealCenter.getX() - 1; x <= sealCenter.getX() + 1; x++) {
			for(int y = sealCenter.getY() - 1; y <= sealCenter.getY() + 1; y++) {
				solids.add(new Point(x, y));
			}
		}
		if(emitterPoint != null) {
			State state = new State(
				sealCenter,
				emitterPoint,
				mirrors,
				new HashSet<>(),
				solids,
				collector
			);
			state.printLayout();
			for(ImmutablePair<Point, Direction> pair : state.getBeamLocations()) {
				System.out.println(pair.left);
			}
			State.getSolution(state);
		}
	}

	private Set<Point> getSceneTiles(GameObject gameObject) {
		Set<Point> points = new HashSet<Point>();
		for(int x = gameObject.getSceneMinLocation().getX(); x <= gameObject.getSceneMaxLocation().getX(); x++) {
			for(int y = gameObject.getSceneMinLocation().getY(); y <= gameObject.getSceneMaxLocation().getY(); y++) {
				points.add(new Point(x, y));
			}
		}
		return points;
	}

	private Direction getMirrorDirection(GameObject gameObject) {
		switch ((gameObject.getConfig() & 0b011000000) >> 6) {
			case 0b00:
				return Direction.NORTH;
			case 0b01:
				return Direction.EAST;
			case 0b10:
				return Direction.SOUTH;
			case 0b11:
				return Direction.WEST;
		}
		return Direction.NORTH;
	}


	private boolean isGameObjectToaObject(int id) {
		for(int toaId : allObjects) {
			if(id == toaId) {
				return true;
			}
		}
		return false;
	}

}
