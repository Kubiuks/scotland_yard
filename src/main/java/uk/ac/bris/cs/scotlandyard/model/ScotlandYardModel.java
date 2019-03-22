package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.*;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Graph;

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	private List<Boolean> xRounds;
	private Graph<Integer, Transport> xGraph;
	private ArrayList<ScotlandYardPlayer> xPlayers = new ArrayList<>();
	private int xCurrentRound = NOT_STARTED;
	private int xAllPlayers	= 0;
	private int xCurrentPlayer = 0; //increment whenever we change the player, initially 0 for mrX
	private int xLastMrXlocation = 0;


	private void validateRounds(List<Boolean> rounds) {
		requireNonNull(rounds);

		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}

		this.xRounds = rounds;
	}

	private void validateGraph(Graph<Integer, Transport> graph) {
		requireNonNull(graph);

		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}

		this.xGraph = graph;
	}

	private ArrayList<PlayerConfiguration> configurePlayers(PlayerConfiguration mrX, PlayerConfiguration firstDetective, PlayerConfiguration... restOfTheDetectives) {
		if (mrX.colour != BLACK)
			throw new IllegalArgumentException("MrX should be Black");

		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		configurations.add(requireNonNull(mrX));
		configurations.add(requireNonNull(firstDetective));

		for (PlayerConfiguration config : restOfTheDetectives) {
			configurations.add(config);
		}

		Set<Integer> set = new HashSet<>();
		Set<Colour> xColour = new HashSet<>();

		for (PlayerConfiguration configuration : configurations) {

			if (set.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			set.add(configuration.location);
			if (xColour.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");
			xColour.add(configuration.colour);
			if (!configuration.tickets.containsKey(BUS))
				 throw new IllegalArgumentException("no BUS");
			if (!configuration.tickets.containsKey(TAXI))
				 throw new IllegalArgumentException("no TAXI");
			if (!configuration.tickets.containsKey(UNDERGROUND))
				 throw new IllegalArgumentException("no UNDERGROUND");
			if (!configuration.tickets.containsKey(DOUBLE))
				 throw new IllegalArgumentException("no DOUBLE");
			if (!configuration.tickets.containsKey(SECRET))
				 throw new IllegalArgumentException("no SECRET");
			if (configuration != mrX && !(configuration.tickets.get(DOUBLE) == 0))
			 	throw  new IllegalArgumentException("no DOUBLE allowed for detectives");
			if (configuration != mrX && !(configuration.tickets.get(SECRET) == 0))
				 throw  new IllegalArgumentException("no SECRET allowed for detectives");
		 }


		return configurations;
	}


//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

				validateGraph(graph);
				validateRounds(rounds);
				ArrayList<PlayerConfiguration> configurations = configurePlayers(mrX, firstDetective, restOfTheDetectives);
				for(PlayerConfiguration configuration : configurations){//uses the given configurations to create real ScotlandYardPlayers
				xPlayers.add(new ScotlandYardPlayer(configuration.player,
												   configuration.colour,
												   configuration.location,
												   configuration.tickets));
				this.xAllPlayers += 1;
				}

			}

	@Override
	public void startRotate() {
		ScotlandYardPlayer player = getxPlayerbyColour(getCurrentPlayer());
		player.player().makeMove(this, player.location(), getValidMoves(player), this);//needs accept method, which is below
	}

	private Set<Move> getValidMoves (ScotlandYardPlayer player) {
		Set<Move> moves = new HashSet<>();
		ArrayList<Integer> takenLocations = getUnavailableLocations(); //gives me a list of locations that are taken by the detectives
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
		if(xCurrentRound < xRounds.size() - 1 && player.hasTickets(DOUBLE, 1)){//DOUBLE move cannot be played if its the last round(notice that current round will increment after mrX makes the move)
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
		if(moves.isEmpty()){//if there are no moves we have to add a PassMove
			moves.add(new PassMove(player.colour()));
		}
		return Collections.unmodifiableSet(moves);
	}

	@Override
	public void accept(Move move){
		requireNonNull(move);
		ScotlandYardPlayer p = getxPlayerbyColour(move.colour());
		if(getValidMoves(p).contains(move)) makeMove(p, move); //move requested must be in the set of valid moves
		else throw new IllegalArgumentException("Move in not possible");
		setNextPlayer();
	}

	private void setNextPlayer(){
		xCurrentPlayer += 1;
		if(xCurrentPlayer == xAllPlayers){
			xCurrentPlayer = 0; //when all players made move the round is over so reset the current player to mrX
		}
		else startRotate();
	}

	private void makeMove(ScotlandYardPlayer player, Move move){

	}

	private ArrayList<Integer> getUnavailableLocations (){
			ArrayList<Integer> locations = new ArrayList<>();
			for(ScotlandYardPlayer player : getxPlayers()){
				if(player.isDetective()){
					locations.add(player.location());
				}
			}
			return locations;
	}

	@Override
	public List<Colour> getPlayers() {
		ArrayList<Colour> cPlayers = new ArrayList<>();
		for (ScotlandYardPlayer xPlayer : getxPlayers()) {
			cPlayers.add(xPlayer.colour());
		}
		return Collections.unmodifiableList(cPlayers);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		   Set<Colour> xWinningPlayers = new HashSet<>();
		   if(isGameOver()){
		   	 //TODO
		   }
		   return Collections.unmodifiableSet(xWinningPlayers);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		   ScotlandYardPlayer player = getxPlayerbyColour(colour);
		   if(player == null) { return Optional.empty(); }
		   Integer location = player.location();
		   if(player.isMrX()){
		   	//TODO
			   if(getCurrentRound() == NOT_STARTED){
			   	return Optional.of(0);
			   }

		   }
		   return Optional.of(location);
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		ScotlandYardPlayer player = getxPlayerbyColour(colour);
		if(player == null) return Optional.empty();
		return Optional.of(player.tickets().getOrDefault(ticket, 0));
	}

	@Override
	public boolean isGameOver() {
		// TODO
		return false;
	}

	private ArrayList<ScotlandYardPlayer> getxPlayers() {
		return xPlayers;
	}

	private ScotlandYardPlayer getxPlayerbyColour(Colour colour){
		for(ScotlandYardPlayer player : getxPlayers()) {
			if (player.colour() == colour){
				return player;
			}
		}
		return null;
	}


	@Override
	public Colour getCurrentPlayer() {
		return this.xPlayers.get(xCurrentPlayer).colour();
	}

	@Override
	public int getCurrentRound() {
		return this.xCurrentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(xRounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(xGraph);
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

}
