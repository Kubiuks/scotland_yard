package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.ai.ResourceProvider;
import uk.ac.bris.cs.scotlandyard.ai.Visualiser;
import uk.ac.bris.cs.scotlandyard.model.*;

import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;

@ManagedAI("RandomAI")
public class RandomAI implements PlayerFactory {

	private ArrayList<Spectator> xSpectators = new ArrayList<>();

	@Override
	public List<Spectator> createSpectators(ScotlandYardView view){
		if(xSpectators.size() == 0) throw new IllegalArgumentException("Spectators cannot be empty!");
		return Collections.unmodifiableList(this.xSpectators);
	}

	@Override
	public void ready(Visualiser visualiser, ResourceProvider provider) {
	}

	@Override
	public void finish() {

	}

	private int score(ScotlandYardView view){
		int score = 0;
		return score;
	}


	// TODO create a new player here
	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	// TODO A sample player that selects a random move
	private static class MyPlayer implements Player {

		private final Random random = new Random();
		private List<Boolean> xRounds;
		private int xCurrentRound = 0;
		private Graph<Integer, Transport> xGraph;
		private ArrayList<ScotlandYardPlayer> xPlayers = new ArrayList<>();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
							 Consumer<Move> callback) {


			// TODO do something interesting here; find the best move
			// picks a random move
			callback.accept(new ArrayList<>(moves).get(random.nextInt(moves.size())));

		}

		private Set<Move> getValidMoves (ScotlandYardPlayer player) {
			Set<Move> moves = new HashSet<>();
			ArrayList<Integer> takenLocations = getTakenLocations();//gives me a list of locations that are taken by the detectives
			Collection<Edge<Integer, Transport>> possibleMoves = getGraph().getEdgesFrom(getGraph().getNode(player.location())); //gives me edges for all possible moves from the current player's location
			for(Edge<Integer, Transport> possibleMove : possibleMoves) {
				Integer destination = possibleMove.destination().value();
				Ticket ticket = fromTransport(possibleMove.data());
				if(!takenLocations.contains(destination)){ //first check if the destination is not already taken by a detective
					if(player.hasTickets(ticket, 1)) moves.add(new TicketMove(player.colour(), ticket, destination));//only for normal tickets i.e. TAXI, BUS, UNDERGROUND
					if(player.hasTickets(SECRET, 1)) moves.add(new TicketMove(player.colour(), SECRET, destination));//only for SECRET
				}
			}
			Set<Move> doubleMoves = new HashSet<>();//we need new set for doubleMoves as there was some merging problem idk why
			if(getCurrentRound() < getRounds().size() - 1 && player.hasTickets(DOUBLE, 1)){//DOUBLE move cannot be played if its the last round(notice that current round will increment after mrX makes the move)
				for(Move move : moves) {
					TicketMove firstMove = (TicketMove)move;
					Collection<Edge<Integer, Transport>> possibleSecondMoves = getGraph().getEdgesFrom(getGraph().getNode(firstMove.destination()));
					for(Edge<Integer, Transport> possibleSecondMove : possibleSecondMoves) {
						Integer secondDestination = possibleSecondMove.destination().value();
						Ticket secondTicket = fromTransport(possibleSecondMove.data());
						if(!takenLocations.contains(secondDestination) || secondDestination == player.location()) {
							if(secondTicket == firstMove.ticket()){
								if(player.hasTickets(secondTicket, 2)) doubleMoves.add(new DoubleMove(player.colour(), firstMove, new TicketMove(player.colour(), secondTicket, secondDestination)));
							}
							else if(player.hasTickets(secondTicket, 1)) doubleMoves.add(new DoubleMove(player.colour(), firstMove, new TicketMove(player.colour(), secondTicket, secondDestination)));
							if(player.hasTickets(SECRET, 2)) doubleMoves.add(new DoubleMove(player.colour(), firstMove, new TicketMove(player.colour(), SECRET, secondDestination)));
						}
					}
				}
			}
			moves.addAll(doubleMoves);//combining normal moves with doubleMoves
			// TODO: added check for Black
			if(moves.isEmpty() && player.colour()!= BLACK){//if there are no moves we have to add a PassMove
				moves.add(new PassMove(player.colour()));
			}
			return Collections.unmodifiableSet(moves);
		}
		public int getCurrentRound() {
			return this.xCurrentRound;
		}

		public List<Boolean> getRounds() {
			return Collections.unmodifiableList(xRounds);
		}

		public Graph<Integer, Transport> getGraph() {
			return new ImmutableGraph<>(xGraph);
		}

		private ArrayList<Integer> getTakenLocations (){
			ArrayList<Integer> locations = new ArrayList<>();
			for(ScotlandYardPlayer player : getxPlayers()){
				if(player.isDetective()){
					locations.add(player.location());
				}
			}
			return locations;
		}

		private ArrayList<ScotlandYardPlayer> getxPlayers() {
			return xPlayers;
		}

	}


}