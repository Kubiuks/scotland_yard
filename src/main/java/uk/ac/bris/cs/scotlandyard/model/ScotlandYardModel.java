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
	private int xLastKnownMrXlocation = 0;
	private Set<Colour> xWinningPlayers = new HashSet<>();
	private Set<Colour> xDetectives = new HashSet<>();
	private ArrayList<Spectator> xSpectators = new ArrayList<>();

	private void validateRounds(List<Boolean> rounds) {
		requireNonNull(rounds);
		if (rounds.isEmpty()) throw new IllegalArgumentException("Empty rounds");
		this.xRounds = rounds;
	}

	private void validateGraph(Graph<Integer, Transport> graph) {
		requireNonNull(graph);
		if (graph.isEmpty()) throw new IllegalArgumentException("Empty graph");
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
				if(!(configuration.colour == BLACK)) this.xDetectives.add(configuration.colour);
				}

			}

	@Override
	public void startRotate() {
		if(isGameOver()) throw new IllegalStateException("game is over");
		ScotlandYardPlayer player = getxPlayerbyColour(getCurrentPlayer());
		player.player().makeMove(this, player.location(), getValidMoves(player), this);//needs accept method, which is below
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
		if(moves.isEmpty()){//if there are no moves we have to add a PassMove
			moves.add(new PassMove(player.colour()));
		}
		return Collections.unmodifiableSet(moves);
	}

	@Override
	public void accept(Move move){
		requireNonNull(move);
		ScotlandYardPlayer p = getxPlayerbyColour(move.colour());
		if(getValidMoves(p).contains(move)) makexMove(p, move); //move requested must be in the set of valid moves
		else throw new IllegalArgumentException("Move in not valid");
		setNextPlayer();
	}

	private void setNextPlayer(){
		xCurrentPlayer += 1;
		if(xCurrentPlayer == xAllPlayers){
			if(getCurrentRound() == getRounds().size()) isGameOver();
			xCurrentPlayer = 0; //when all players made move the round is over so reset the current player to mrX
		}
		else startRotate();
	}


	private void makexMove(ScotlandYardPlayer player, Move move){
		MoveVisitor visitor = new MoveVisitor() {
			@Override
			public void visit(PassMove move) {

			}

			@Override
			public void visit(TicketMove move) {
				player.removeTicket(move.ticket());
				player.location(move.destination());
				if(player.isDetective()) {
					getxPlayerbyColour(BLACK).addTicket(move.ticket());
				}
				else {
					xCurrentRound += 1;
					notifySpectatorsAboutRound();
					if(getRounds().get(getCurrentRound() -1)) { //if true it means its the reveal round
															//-1 because Rounds count start with 0, so our first round is 0th round in getRounds
						xLastKnownMrXlocation = player.location();
					}
				}
			}

			@Override
			public void visit(DoubleMove move) {
				int firstDestination = move.firstMove().destination();
				int finalDestination = move.finalDestination();

				xCurrentRound += 1;
				notifySpectatorsAboutRound();
				player.removeTicket(move.firstMove().ticket());
				player.removeTicket(move.secondMove().ticket());
				player.removeTicket(DOUBLE);
				if(getRounds().get(getCurrentRound() - 1)){//first move is reveal
						xLastKnownMrXlocation = firstDestination;
				}
				if(getRounds().get(getCurrentRound())){//second move is reveal
						xLastKnownMrXlocation = finalDestination;

				}
				xCurrentRound += 1;
				notifySpectatorsAboutRound();
				player.location(finalDestination);
			}
		};
		move.visit(visitor);
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


	@Override
	public boolean isGameOver() {
		boolean winner;
		if(areDetectivesTicketless() || allDetectivesDontHaveValidMoves() || noRoundsLeft()) xWinningPlayers.add(BLACK);
		else if(isMrXcaptured() || isMrXcornered() || isMrXStuck()) xWinningPlayers = xDetectives;
		System.out.println(xWinningPlayers);
		return !xWinningPlayers.isEmpty();
	}

	private boolean isMrXcaptured(){
		for(ScotlandYardPlayer p : getxPlayers()){
			if(p.isDetective() && p.location() == getxPlayerbyColour(BLACK).location()) return true;
		}
		return false;
	}

	private boolean areDetectivesTicketless(){
		for(ScotlandYardPlayer p : getxPlayers()){
			if(p.isDetective()){
				if(p.hasTickets(BUS) || p.hasTickets(UNDERGROUND) || p.hasTickets(TAXI)) return false;
			}

		}
		return true;
	}

	private boolean allDetectivesDontHaveValidMoves(){
		boolean ok = true;
		for(ScotlandYardPlayer p : getxPlayers()) {
			if(p.isDetective()) {
				ok &= getValidMoves(p).contains(new PassMove(p.colour()));
			}
		}
		return ok;
	}

	private boolean noRoundsLeft() {
			return false;
	}

	private boolean isMrXStuck(){
		return false;
	}

	private boolean isMrXcornered(){

		return false;
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		return Collections.unmodifiableSet(xWinningPlayers);
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
	public Optional<Integer> getPlayerLocation(Colour colour) {
		   ScotlandYardPlayer player = getxPlayerbyColour(colour);
		   if(player == null) return Optional.empty();
		   if(player.isMrX()) return Optional.of(xLastKnownMrXlocation);
		   return Optional.of(player.location());
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		ScotlandYardPlayer player = getxPlayerbyColour(colour);
		if(player == null) return Optional.empty();
		return Optional.of(player.tickets().getOrDefault(ticket, 0));
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
		requireNonNull(spectator);
		if(xSpectators.contains(spectator)) throw new IllegalArgumentException("same spectator not allowed");
		else this.xSpectators.add(spectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if(xSpectators.contains(spectator)) xSpectators.remove(spectator);
		else throw  new  IllegalArgumentException("spectator is not registered");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableList(xSpectators);
	}

	private void notifySpectatorsAboutRound(){
		for(Spectator spectator : getSpectators()){
			spectator.onRoundStarted(this, getCurrentRound());
		}
	}

}
