/**
 * 
 */
package epidemiology;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import repast.simphony.context.Context;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Recovered {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;

	public Recovered(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	@Watch(watcheeClassName = "epidemiology.Infected", watcheeFieldNames = "moved", 
			query = "within_vn 1", whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void run() {
		// get the grid location of this Human
		GridPoint pt = grid.getLocation(this);

		// use the GridCellNgh class to create GridCells for
		// the surrounding neighborhood.
		GridCellNgh<Infected> nghCreator = new GridCellNgh<Infected>(grid, pt,
				Infected.class, 1, 1);
		List<GridCell<Infected>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

		GridPoint pointWithMostInfected = null;
		int maxCount = -1;
		for (GridCell<Infected> cell : gridCells) {
			if (cell.size() > maxCount) {
				pointWithMostInfected = cell.getPoint();
				maxCount = cell.size();
			}
		}
		
		moveTowards(pointWithMostInfected);
		recover();
	}
	
	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 2, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
			//energy--;
		}
	}
	
	public void recover() {
		GridPoint pt = grid.getLocation(this);
		List<Object> infected = new ArrayList<Object>();
		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
			if (obj instanceof Infected) {
				infected.add(obj);
			}
		}

		double gamma = 0.5;
		long noOfSusceptibleToRecover = (long) Math.ceil(gamma * infected.size());
		List<Object> susceptibleToRecover = infected.stream().limit(noOfSusceptibleToRecover).collect(Collectors.toList());
		susceptibleToRecover.stream().forEach(obj -> {
			NdPoint spacePt = space.getLocation(obj);
			Context<Object> context = ContextUtils.getContext(obj);
			context.remove(obj);
			Recovered recovered = new Recovered(space, grid);
			context.add(recovered);
			space.moveTo(recovered, spacePt.getX(), spacePt.getY());
			grid.moveTo(recovered, pt.getX(), pt.getY());

			Network<Object> net = (Network<Object>) context.getProjection("infection network");
			net.addEdge(this, recovered);
		});

	}
	
}
