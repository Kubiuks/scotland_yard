package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;

@ManagedAI("SimpleAI")


public class SimpleAI implements PlayerFactory {

	static Move bestMove;






	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}
	private static class MyPlayer implements Player {


		private Integer score(ScotlandYardView view, Move move1){
			Integer score = 0;
			if(move1 instanceof TicketMove) {
				TicketMove move = (TicketMove) move1;
				Integer destination = move.destination();
				Ticket ticket = move.ticket();
				for (Edge edge : view.getGraph().getEdgesFrom(new Node<>(destination))) {
					boolean ok = false;
					for (Colour player : view.getPlayers()) {
						if (player.isDetective()) {
							if (edge.destination().value() == view.getPlayerLocation(player)) ok = true;
						}
					}
					if(!ok){
						score ++;
					}
				}
			}
			return score;
		}

		private Move bestFromMoves(ScotlandYardView view, Set<Move> moves){
			HashMap<Move, Integer> scores = new HashMap<>();
			for(Move move : moves){
				scores.put(move, score(view, move));
			}
			Integer max = Collections.max(scores.values());
			Move bestMove = null;
			for (Map.Entry<Move, Integer> entry : scores.entrySet()){
				if (entry.getValue()==max) {
					bestMove = entry.getKey();

				}
			}
			System.out.println(max);
			System.out.println(scores);

			return bestMove;
		}


		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
							 Consumer<Move> callback) {


			bestMove = bestFromMoves(view, moves);

			callback.accept(bestMove);
		}
	}
}