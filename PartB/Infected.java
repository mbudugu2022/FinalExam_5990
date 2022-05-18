package epidemiology;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
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

public class Infected {

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private boolean moved;

	public Infected(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		// get the grid location of this Infected
		GridPoint pt = grid.getLocation(this);

		// use the GridCellNgh class to create GridCells for
		// the surrounding neighborhood.
		GridCellNgh<Susceptible> nghCreator = new GridCellNgh<Susceptible>(grid, pt, Susceptible.class, 1, 1);
		List<GridCell<Susceptible>> gridCells = nghCreator.getNeighborhood(true);
		SimUtilities.shuffle(gridCells, RandomHelper.getUniform());

		GridPoint pointWithMostSusceptible = null;
		int maxCount = -1;
		for (GridCell<Susceptible> cell : gridCells) {
			if (cell.size() > maxCount) {
				pointWithMostSusceptible = cell.getPoint();
				maxCount = cell.size();
			}
		}
		moveTowards(pointWithMostSusceptible);
		if(maxCount > 0) {
			infect();
		}
		
	}

	public void moveTowards(GridPoint pt) {
		// only move if we are not already in this grid location
		if (!pt.equals(grid.getLocation(this))) {
			NdPoint myPoint = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
			moved = true;
		}
	}

	public void infect() {
		GridPoint pt = grid.getLocation(this);
		List<Object> susceptible = new ArrayList<Object>();
		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
			if (obj instanceof Susceptible) {
				susceptible.add(obj);
			}
		}

		double beta = 1;
		long susceptibleToIinfect = (long) Math.ceil(beta * susceptible.size());
		List<Object> susceptibleToInfect = susceptible.stream().limit(susceptibleToIinfect).collect(Collectors.toList());
		susceptibleToInfect.stream().forEach(obj -> {
			NdPoint spacePt = space.getLocation(obj);
			Context<Object> context = ContextUtils.getContext(obj);
			context.remove(obj);
			Infected infected = new Infected(space, grid);
			context.add(infected);
			space.moveTo(infected, spacePt.getX(), spacePt.getY());
			grid.moveTo(infected, pt.getX(), pt.getY());

			Network<Object> net = (Network<Object>) context.getProjection("infection network");
			net.addEdge(this, infected);
		});

	}
	
}
