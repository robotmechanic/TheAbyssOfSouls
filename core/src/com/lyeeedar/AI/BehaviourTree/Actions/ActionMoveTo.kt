package com.lyeeedar.AI.BehaviourTree.Actions

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.XmlReader
import com.lyeeedar.AI.BehaviourTree.ExecutionState
import com.lyeeedar.Components.Mappers
import com.lyeeedar.Components.TaskMove
import com.lyeeedar.Enums
import com.lyeeedar.GlobalData
import com.lyeeedar.Level.Tile
import com.lyeeedar.Pathfinding.Pathfinder
import com.lyeeedar.Util.Point

/**
 * Created by Philip on 21-Mar-16.
 */

class ActionMoveTo(): AbstractAction()
{
	var dst: Int = 0
	var towards: Boolean = true
	lateinit var key: String

	override fun evaluate(entity: Entity): ExecutionState
	{
		val target = getData( key, null ) as? Point;
		val posData = Mappers.position.get(entity)
		val taskData = Mappers.task.get(entity)
		val tile = posData.position as? Tile

		// doesnt have all the needed data, fail
		if ( target == null || posData == null || tile == null || taskData == null )
		{
			state = ExecutionState.FAILED;
			return state;
		}

		// if we arrived at our target, succeed
		if ( (towards && tile.taxiDist(target) <= dst) || (!towards && tile.taxiDist(target) >= dst) )
		{
			state = ExecutionState.COMPLETED;
			return state;
		}

		val pathFinder = Pathfinder(tile.level.grid, tile.x, tile.y, target.x, target.y, GlobalData.Global.canMoveDiagonal, posData.size, entity);
		val path = pathFinder.getPath( posData.slot );

		// if couldnt find a valid path, fail
		if ( path.size < 2 )
		{
			Point.freeAll(path)
			state = ExecutionState.FAILED;
			return state;
		}

		var nextTile = tile.level.getTile( path.get( 1 ) );

		// if next step is impassable then fail
		if ( ! ( nextTile?.getPassable( posData.slot, entity ) ?: false ) )
		{
			Point.freeAll(path)
			state = ExecutionState.FAILED;
			return state;
		}

		var offset = path.get( 1 ) - path.get( 0 );

		// if moving towards path to the object
		if ( towards )
		{
			if ( path.size - 1 <= dst || offset == Point.ZERO )
			{
				Point.freeAll(path)
				offset.free()
				state = ExecutionState.COMPLETED;
				return state;
			}

			taskData.tasks.add(TaskMove(Enums.Direction.getDirection(offset)));
		}
		// if moving away then just run directly away
		else
		{
			if ( path.size - 1 >= dst || offset == Point.ZERO )
			{
				Point.freeAll(path)
				offset.free()
				state = ExecutionState.COMPLETED;
				return state;
			}

			offset *= -1
			taskData.tasks.add(TaskMove(Enums.Direction.getDirection(offset)));
		}

		Point.freeAll(path)
		offset.free()
		state = ExecutionState.RUNNING;
		return state;
	}

	override fun cancel()
	{

	}

	override fun parse( xml: XmlReader.Element)
	{
		dst = Integer.parseInt( xml.getAttribute( "Distance", "0" ) );
		towards = xml.getAttribute( "Towards", "true" ).toBoolean();
		key = xml.getAttribute( "Key" );
	}
}