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
			int score = 0;
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
			ArrayList<Integer> scores = new ArrayList<>();
			for(Move move : moves){
				scores.add(score(view, move));
			}
			Object[] movesArray = moves.toArray();
			Integer max = Collections.max(scores);
			int[] myArray = new int[scores.size()];
			Iterator<Integer> iterator = scores.iterator();
			for (int i = 0; i < myArray.length; i++) {
				myArray[i] = iterator.next().intValue();
			}
			int index = 0;
			for(int n=0; n < movesArray.length; n++){
				if(myArray[n] == max){
					index = n;
					break;
				}
			}
			System.out.println(movesArray);
			System.out.println(scores);
			System.out.println(max);
			System.out.println(index);

			return (Move)movesArray[index];
		}


		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
							 Consumer<Move> callback) {


			bestMove = bestFromMoves(view, moves);

			callback.accept(bestMove);
		}
	}
}