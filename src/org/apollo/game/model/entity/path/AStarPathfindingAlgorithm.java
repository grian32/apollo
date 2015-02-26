package org.apollo.game.model.entity.path;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apollo.game.model.Position;

/**
 * A {@link PathfindingAlgorithm} that utilises the A* algorithm to find a solution.
 * <p>
 * This implementation utilises a {@link PriorityQueue} of open {@link Node}s, in addition to the usual {@link HashSet}.
 * This allows for logarithmic-time finding of the cheapest element (as opposed to the linear time associated with
 * iterating over the set), whilst still maintaining the constant time contains and remove of the set.
 * <p>
 * This implementation also avoids the linear-time removal from the queue by polling until the first open node is found
 * when identifying the cheapest node.
 * 
 * @author Major
 */
final class AStarPathfindingAlgorithm extends PathfindingAlgorithm {

	/**
	 * The heuristic.
	 */
	private final Heuristic heuristic;

	/**
	 * Creates the A* pathfinding algorithm with the specified heuristic.
	 *
	 * @param heuristic The heuristic.
	 */
	public AStarPathfindingAlgorithm(Heuristic heuristic) {
		this.heuristic = heuristic;
	}

	@Override
	public Deque<Position> find(Position origin, Position target) {
		Map<Position, Node> nodes = new HashMap<>();
		Node start = new Node(origin), end = new Node(target);
		nodes.put(origin, start);
		nodes.put(target, end);

		Set<Node> open = new HashSet<>();
		Queue<Node> sorted = new PriorityQueue<>();
		open.add(start);
		sorted.add(start);

		do {
			Node active = getCheapest(sorted);
			Position position = active.getPosition();

			if (position.equals(target)) {
				break;
			}

			open.remove(active);
			active.close();

			int x = position.getX(), y = position.getY();
			for (int nextX = x - 1; x <= x + 1; nextX++) {
				for (int nextY = y - 1; y <= y + 1; nextY++) {
					if (nextX == x && nextY == y) {
						continue;
					}

					Position adjacent = new Position(nextX, nextY);
					if (traversable(adjacent)) {
						Node node = createIfAbsent(adjacent, nodes);
						compare(active, node, open, sorted, heuristic);
					}
				}
			}
		} while (!open.isEmpty());

		Deque<Position> shortest = new ArrayDeque<>();
		Node active = end;

		if (active.hasParent()) {
			Position position = active.getPosition();

			while (!origin.equals(position)) {
				shortest.addFirst(position);
				active = active.getParent(); // If the target has a parent then all of the others will.
				position = active.getPosition();
			}
		}

		return shortest;
	}

	/**
	 * Compares the two specified {@link Node}s, adding the other node to the open {@link Set} if the estimation is
	 * cheaper than the current cost.
	 * 
	 * @param active The active node.
	 * @param other The node to compare the active node against.
	 * @param open The set of open nodes.
	 * @param sorted The sorted {@link Queue} of nodes.
	 * @param heuristic The {@link Heuristic} used to estimate the cost of the node.
	 */
	private void compare(Node active, Node other, Set<Node> open, Queue<Node> sorted, Heuristic heuristic) {
		int cost = active.getCost() + heuristic.estimate(active.getPosition(), other.getPosition());

		if (other.getCost() > cost) {
			open.remove(other);
			other.close();
		} else if (other.isOpen() && !open.contains(other)) {
			other.setCost(cost);
			other.setParent(active);
			open.add(other);
			sorted.add(other);
		}
	}

	/**
	 * Creates a {@link Node} and inserts it into the specified {@link Map} if one does not already exist, then returns
	 * that node.
	 *
	 * @param position The {@link Position}.
	 * @param nodes The map of positions to nodes.
	 * @return The node.
	 */
	private Node createIfAbsent(Position position, Map<Position, Node> nodes) {
		Node existing = nodes.get(position);
		if (existing == null) {
			existing = new Node(position);
			nodes.put(position, existing);
		}

		return existing;
	}

	/**
	 * Gets the cheapest open {@link Node} from the {@link Queue}.
	 * 
	 * @param nodes The queue of nodes.
	 * @return The cheapest node.
	 */
	private Node getCheapest(Queue<Node> nodes) {
		Node node = nodes.peek();
		while (!node.isOpen()) {
			nodes.poll();
			node = nodes.peek();
		}

		return node;
	}

}