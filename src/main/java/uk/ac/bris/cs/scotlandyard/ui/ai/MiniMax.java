package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.ai.ResourceProvider;
import uk.ac.bris.cs.scotlandyard.ai.Visualiser;
import uk.ac.bris.cs.scotlandyard.model.*;

import static uk.ac.bris.cs.scotlandyard.model.Colour.*;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;

/**
 * MyMrX is an AI that choose the best move for MrX using minimax with alpha-beta pruning that scores according to Dijkstra algorithm.
 */

@ManagedAI("MiniMax")
public class MiniMax implements PlayerFactory {


	@Override
	public Player createPlayer(Colour colour) {

		return new MyPlayer();
	}


	private static class MyPlayer implements Player {

		private final Random random = new Random();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {


			GameAI currentState = new GameAI(view, location);
			Set<Move> goodMoves = new HashSet<>();
			ArrayList<Integer> scoreList = new ArrayList<>();
			HashMap<Integer, GameAI> statesWithScores = new HashMap<>();

			for (Move move : moves) {

				GameAI nextState = new GameAI(currentState, move);
				int gameStateScore = minimax(nextState, view.getCurrentPlayer(), 2 ,Integer.MIN_VALUE, Integer.MAX_VALUE);

				scoreList.add(gameStateScore);
				statesWithScores.put(gameStateScore, nextState);
				GameAI bestState = statesWithScores.get(Collections.max(scoreList));
				if (nextState.equals(bestState)) goodMoves.add(move);
			}
			callback.accept(new ArrayList<>(goodMoves).get(random.nextInt(goodMoves.size())));
		}

		//scoring using Dijkstra's Algorithm
		private int score(GameAI state, Colour player) {

			int score = 0;

			Dijkstra dijkstra = new Dijkstra(state.getGraph());

			//For MrX, getting away from the detectives is wanted
			if (player.isMrX()) {
				dijkstra.execute(new Node<>(state.getMrXlocation().location));

				for (PlayerAI detective : state.getxDetectives()) {
					score += dijkstra.getPathDistance(new Node<>(detective.location));
				}
			}
			//For the detectives, getting closer to MrX is wanted
			else {
				dijkstra.execute(new Node<>(state.getPlayer(player).location));
				score -= dijkstra.getPathDistance(new Node<>(state.getMrXlocation().location));
			}
			return score;
		}


		private int minimax (GameAI state, Colour player, int depth, int alpha, int beta) {


			Set<Move> validMoves = getValidMoves(state, state.getPlayer(player));
			ArrayList<Integer> alphaList = new ArrayList<>();
			ArrayList<Integer> betaList = new ArrayList<>();
			int score;

			if (depth == 0) {
				score = score(state, player);
				return score;

			} else {
				if (player == BLACK) { //max
					betaList.clear();

					for (Move move : validMoves) {

						GameAI nextState = new GameAI(state,move);
						ArrayList<Integer> scoreList = new ArrayList<>();

						for(PlayerAI detective : nextState.getxDetectives()) {

							score = minimax(nextState, detective.colour, depth - 1, alpha, beta);
							scoreList.add(score);
						}
						score = Collections.max(scoreList);
						if (score > alpha) {
							alpha = score;
							alphaList.add(alpha);
						}
						if (alpha <= beta) break;
					}
					return Collections.max(alphaList);

				} else { //min
					alphaList.clear();

					for (Move move : validMoves) {

						GameAI nextState = new GameAI(state,move);

						score = minimax(nextState, BLACK, depth - 1, alpha, beta);
						if (score < beta) {
							beta = score;
							betaList.add(beta);
						}
						if (alpha >= beta) break;
					}
					return Collections.min(betaList);
				}
			}
		}

		//returns a set of valid moves
		private Set<Move> getValidMoves (GameAI state, PlayerAI player) {
			Set<Move> moves = new HashSet<>();

			ArrayList<Integer> takenLocations = state.getTakenLocations();//gives me a list of locations that are taken by the detectives
			Collection<Edge<Integer, Transport>> possibleMoves = state.getGraph().getEdgesFrom(state.getGraph().getNode(player.location)); //gives me edges for all possible moves from the current player's location

			// for all players including Black
			for(Edge<Integer, Transport> possibleMove : possibleMoves) {

				Integer destination = possibleMove.destination().value();
				Ticket ticket = fromTransport(possibleMove.data());

				if(!takenLocations.contains(destination)){ //first check if the destination is not already taken by a detective
					if(player.hasTickets(ticket, 1)) moves.add(new TicketMove(player.colour, ticket, destination));//only for normal tickets i.e. TAXI, BUS, UNDERGROUND
					if(player.hasTickets(SECRET, 1)) moves.add(new TicketMove(player.colour, SECRET, destination));//only for SECRET
				}
			}

			// for Black
			if (player.hasTickets(DOUBLE, 1)) {
				Set<Move> doubleMoves = new HashSet<>();//we need new set for doubleMoves as there was some merging problem idk why

				if (state.getCurrentRound() < state.getRounds().size() - 1) {//DOUBLE move cannot be played if its the last round(notice that current round will increment after mrX makes the move)

					for (Move move : moves) {

						TicketMove firstMove = (TicketMove) move;
						Collection<Edge<Integer, Transport>> possibleSecondMoves = state.getGraph().getEdgesFrom(state.getGraph().getNode(firstMove.destination()));

						for (Edge<Integer, Transport> possibleSecondMove : possibleSecondMoves) {

							Integer secondDestination = possibleSecondMove.destination().value();
							Ticket secondTicket = fromTransport(possibleSecondMove.data());

							if (!takenLocations.contains(secondDestination) || secondDestination == player.location) {

								if (secondTicket == firstMove.ticket()) {
									if (player.hasTickets(secondTicket, 2))
										doubleMoves.add(new DoubleMove(BLACK, firstMove, new TicketMove(player.colour, secondTicket, secondDestination)));

								} else if (player.hasTickets(secondTicket, 1))
									doubleMoves.add(new DoubleMove(BLACK, firstMove, new TicketMove(player.colour, secondTicket, secondDestination)));

								if (player.hasTickets(SECRET, 2))
									doubleMoves.add(new DoubleMove(BLACK, firstMove, new TicketMove(player.colour, SECRET, secondDestination)));
							}
						}
					}
				}
				moves.addAll(doubleMoves);//combining normal moves with doubleMoves
			}

			if(moves.isEmpty() && player.colour != BLACK) {
				//if there are no moves we have to add a PassMove
				moves.add(new PassMove(player.colour));
			}
			return Collections.unmodifiableSet(moves);
		}

	}

}

