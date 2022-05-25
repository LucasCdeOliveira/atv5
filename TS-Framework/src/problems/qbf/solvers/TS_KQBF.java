package problems.qbf.solvers;

import metaheuristics.tabusearch.AbstractTS;
import problems.qbf.KQBF;
import problems.qbf.KQBF_Inverse;
import problems.qbf.QBF_Inverse;
import solutions.Solution;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;


/**
 * Metaheuristic TS (Tabu Search) for obtaining an optimal solution to a QBF
 * (Quadractive Binary Function -- {@link #QuadracticBinaryFunction}).
 * Since by default this TS considers minimization problems, an inverse QBF
 *  function is adopted.
 * 
 * @author ccavellucci, fusberti
 */
public class TS_KQBF extends AbstractTS<Integer> {

	private final Integer fake = new Integer(-1);

	/**
	 * Constructor for the TS_QBF class. An inverse QBF objective function is
	 * passed as argument for the superclass constructor.
	 *
	 * @param tenure
	 *            The Tabu tenure parameter.
	 * @param iterations
	 *            The number of iterations which the TS will be executed.
	 * @param filename
	 *            Name of the file for which the objective function parameters
	 *            should be read.
	 * @throws IOException
	 *             necessary for I/O operations.
	 */
	public TS_KQBF(Integer tenure, Integer iterations, String filename) throws IOException {
		super(new KQBF_Inverse(filename), tenure, iterations);
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeCL()
	 */
	@Override
	public ArrayList<Integer> makeCL() {

		ArrayList<Integer> _CL = new ArrayList<Integer>();
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer cand = new Integer(i);
			_CL.add(cand);
		}

		return _CL;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeRCL()
	 */
	@Override
	public ArrayList<Integer> makeRCL() {

		ArrayList<Integer> _RCL = new ArrayList<Integer>();

		return _RCL;

	}
	
	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeTL()
	 */
	@Override
	public ArrayDeque<Integer> makeTL() {

		ArrayDeque<Integer> _TS = new ArrayDeque<Integer>(2*tenure);
		for (int i=0; i<2*tenure; i++) {
			_TS.add(fake);
		}

		return _TS;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#updateCL()
	 */
	@Override
	public void updateCL() {

		// do nothing

	}

	/**
	 * {@inheritDoc}
	 * 
	 * This createEmptySol instantiates an empty solution and it attributes a
	 * zero cost, since it is known that a QBF solution with all variables set
	 * to zero has also zero cost.
	 */
	@Override
	public Solution<Integer> createEmptySol() {
		Solution<Integer> sol = new Solution<Integer>();
		sol.cost = 0.0;
		return sol;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The local search operator developed for the QBF objective function is
	 * composed by the neighborhood moves Insertion, Removal and 2-Exchange.
	 */
	@Override
	public Solution<Integer> neighborhoodMove() {

		Double minDeltaCost;
		Integer firstCandIn = null, firstCandOut = null;
		// Since we are using break in the for loop we created this variable to control if we need to update
		// the candidate list and the tabulist
		Boolean needRemoveCandIn = false;

		minDeltaCost = Double.POSITIVE_INFINITY;
		updateCL();
		// Evaluate insertions
		for (Integer candIn : CL) {
			Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, sol);
			if (!TL.contains(candIn) || sol.cost+deltaCost < bestSol.cost) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					firstCandIn = candIn;
					firstCandOut = null;
					if(ObjFunction.shouldInsert(firstCandIn, sol)) {
						break;
					}
					else {
						needRemoveCandIn = true;
						break;
					}
				}
			}
		}
		if(firstCandIn != null) {
			if(!needRemoveCandIn) {
				sol.add(firstCandIn);
				CL.remove(firstCandIn);
				TL.add(firstCandIn);
				return null;
			}
			CL.remove(firstCandIn);
			TL.add(firstCandIn);
			needRemoveCandIn = false;
		}
		// Evaluate removals
		for (Integer candOut : sol) {
			Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, sol);
			if (!TL.contains(candOut) || sol.cost+deltaCost < bestSol.cost) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					firstCandIn = null;
					firstCandOut = candOut;
					break;
				}
			}
		}
		if(firstCandOut != null) {
			sol.remove(firstCandOut);
			CL.add(firstCandOut);
			return null;
		}
		// Evaluate exchanges
		for (Integer candIn : CL) {
			boolean found = false;
			for (Integer candOut : sol) {
				Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
				if ((!TL.contains(candIn) && !TL.contains(candOut)) || sol.cost+deltaCost < bestSol.cost) {
					if (deltaCost < minDeltaCost) {
						minDeltaCost = deltaCost;
						firstCandIn = candIn;
						firstCandOut = candOut;
						found = true;
						break;
					}
				}
			}
		}
		// Implement the best non-tabu move
		TL.poll();
		//Here only the exchange matter, if I added or removed I already returned from the function
		if(firstCandIn != null && firstCandOut != null) {
			//Remove to evaluate the new weight with the new element that will enter
			sol.remove(firstCandOut);
			if (ObjFunction.shouldInsert(firstCandIn, sol)) {
				sol.add(firstCandIn);
				CL.remove(firstCandIn);
				TL.add(firstCandIn);
				CL.add(firstCandOut);
				return null;
			} else {
				//If I can't insert I just return that element to the solution
				sol.add(firstCandOut);
			}
		}
		ObjFunction.evaluate(sol);
		
		return null;
	}

	/**
	 * A main method used for testing the GRASP metaheuristic.
	 *
	 */
	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();
		TS_KQBF tabusearch = new TS_KQBF(20, 1000, "instances/kqbf/kqbf100");
		Solution<Integer> bestSol = tabusearch.solve();
		System.out.println("maxVal = " + bestSol);
		KQBF evaluateCost = new KQBF("instances/kqbf/kqbf100");
		System.out.println("weight of solution = " + evaluateCost.evaluateWeight(bestSol));
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = "+(double)totalTime/(double)1000+" seg");

	}

}
