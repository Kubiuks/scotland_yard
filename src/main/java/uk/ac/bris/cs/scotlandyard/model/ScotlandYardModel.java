
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
		import java.util.stream.Collectors;

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
	private int xCurrentPlayer = 0; //increment whenever we change the player, initially 0 for mrX
	private int xLastKnownMrXLocation = 0;
	private Set<Colour> xWinningPlayers = new HashSet<>();
	private Set<Colour> xDetectives = new HashSet<>();
	private ArrayList<Spectator> xSpectators = new ArrayList<>();
	private boolean mrXWon = false;
	private boolean detectivesWon = false;

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

	private ArrayList<PlayerConfiguration> configurePlayers(PlayerConfiguration mrX,
															PlayerConfiguration firstDetective,
															PlayerConfiguration... restOfTheDetectives) {
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
			xPlayers.add(new ScotlandYardPlayer(
					configuration.player,
					configuration.colour,
					configuration.location,
					configuration.tickets
			));
			if(!(configuration.colour == BLACK)) this.xDetectives.add(configuration.colour);
		}

	}

	@Override
	public void startRotate() {
		if(isGameOver()) throw new IllegalStateException("game is over");
		nextMove();
	}

	private void nextMove() {
		ScotlandYardPlayer player = xPlayers.get(xCurrentPlayer);
		Set<Move> validMoves = getValidMoves(player);
		player.player().makeMove(this, player.location(), validMoves, this);
	}

	private Set<Move> getValidMoves (ScotlandYardPlayer player) {
		Set<Move> moves = new HashSet<>();

		ArrayList<Integer> takenLocations = getTakenLocations();//gives me a list of locations that are taken by the detectives
		Collection<Edge<Integer, Transport>> possibleMoves = getGraph().getEdgesFrom(getGraph().getNode(player.location())); //gives me edges for all possible moves from the current player's location

		// for all players including Black
		for(Edge<Integer, Transport> possibleMove : possibleMoves) {

			Integer destination = possibleMove.destination().value();
			Ticket ticket = fromTransport(possibleMove.data());

			if(!takenLocations.contains(destination)){ //first check if the destination is not already taken by a detective
				if(player.hasTickets(ticket, 1)) moves.add(new TicketMove(player.colour(), ticket, destination));//only for normal tickets i.e. TAXI, BUS, UNDERGROUND
				if(player.hasTickets(SECRET, 1)) moves.add(new TicketMove(player.colour(), SECRET, destination));//only for SECRET
			}
		}

		// for Black
		if (player.hasTickets(DOUBLE, 1)) {
			Set<Move> doubleMoves = new HashSet<>();//we need new set for doubleMoves as there was some merging problem idk why

			if (getCurrentRound() < getRounds().size() - 1) {//DOUBLE move cannot be played if its the last round(notice that current round will increment after mrX makes the move)

				for (Move move : moves) {

					TicketMove firstMove = (TicketMove) move;
					Collection<Edge<Integer, Transport>> possibleSecondMoves = getGraph().getEdgesFrom(getGraph().getNode(firstMove.destination()));

					for (Edge<Integer, Transport> possibleSecondMove : possibleSecondMoves) {

						Integer secondDestination = possibleSecondMove.destination().value();
						Ticket secondTicket = fromTransport(possibleSecondMove.data());

						if (!takenLocations.contains(secondDestination) || secondDestination == player.location()) {

							if (secondTicket == firstMove.ticket()) {

								if (player.hasTickets(secondTicket, 2))
									doubleMoves.add(new DoubleMove(BLACK, firstMove, new TicketMove(player.colour(), secondTicket, secondDestination)));

							} else if (player.hasTickets(secondTicket, 1))

								doubleMoves.add(new DoubleMove(BLACK, firstMove, new TicketMove(player.colour(), secondTicket, secondDestination)));

							if (player.hasTickets(SECRET, 2))

								doubleMoves.add(new DoubleMove(BLACK, firstMove, new TicketMove(player.colour(), SECRET, secondDestination)));

						}
					}
				}
			}

			moves.addAll(doubleMoves);//combining normal moves with doubleMoves
		}

		// added check for Black, because BLACK can't have a PassMove
		if(moves.isEmpty() && player.colour()!= BLACK) {
			//if there are no moves we have to add a PassMove
			moves.add(new PassMove(player.colour()));
		}
		return Collections.unmodifiableSet(moves);
	}

	@Override
	public void accept(Move move) {
		requireNonNull(move);
		ScotlandYardPlayer player = getXPlayerbyColour(move.colour());
		if (!getValidMoves(player).contains(move)) throw new IllegalArgumentException("Move in not valid");//move requested must be in the set of valid moves
		makeXMove(player, move);
	}

	private void makeXMove(ScotlandYardPlayer player, Move move) {

		//Taken Reveal Round and Location logic out to corresponding methods
		//Added quite a lot of logic for setting a new player and setting next move to accommodate a couple of tests

		move.visit( new MoveVisitor() {
			@Override
			public void visit(PassMove move) {

				player.location(player.location());

				boolean isRotationCompleted = setNextPlayer(false);

				notifySpectatorsAboutMoveMade(move);

				setNextMove(isRotationCompleted);
			}

			// 1. remove ticket
			// 2. change location to destination
			// 3. increment player
			// 4. (notify round started) + notify move made
			// 5. (notify Game Over) or (notify rotation complete or set next move)

			@Override
			public void visit(TicketMove move) {

				Ticket ticket = move.ticket();
				int destination = move.destination();

				player.removeTicket(ticket);
				player.location(destination);

				boolean isRotationCompleted = setNextPlayer(false);

				if (player.isDetective()) {

					getXPlayerbyColour(BLACK).addTicket(ticket);
					notifySpectatorsAboutMoveMade(move, ticket, destination);

				} else {

					xCurrentRound += 1;
					notifySpectatorsAboutRoundStarted();

					if (isRoundReveal(getCurrentRound())) {
						notifySpectatorsAboutMoveMade(move, ticket, destination);
					} else {
						notifySpectatorsAboutMoveMade(move, ticket, xLastKnownMrXLocation);
					}
				}

				setNextMove(isRotationCompleted);

			}

			// 1. increment player
			// 2. remove Double ticket
			// 3. notify Double move made
			// 4. change location to destination 1
			// 5. remove Ticket 1
			// 6. increment round and notify about round started
			// 7. notify Move 1 made
			// 8. change location to destination 2
			// 9. remove Ticket 2
			// 10. increment round and notify about round started
			// 11. notify Move 2 made
			// 12. (notify Game Over) or (notify rotation complete or set next move)

			@Override
			public void visit(DoubleMove move) {

				Ticket first = move.firstMove().ticket();
				Ticket second = move.secondMove().ticket();

				int destination1 = move.firstMove().destination();
				int destination2 = move.finalDestination();

				boolean isRotationCompleted = setNextPlayer(false);

				//added check for all possible reveal rounds

				// Remove Double move
				player.removeTicket(DOUBLE);

				// Notify about DOUBLE move made
				if (isRoundReveal(getCurrentRound() + 1)) { // check if next round is reveal
					if (isRoundReveal(getCurrentRound() + 2)) { // check if next after next round is reveal
						// if both rounds are reveal, show both destinations
						notifySpectatorsAboutMoveMade(move, first, destination1, second, destination2);
					} else {
						// if first is reveal and second isn't, show destination1 in both
						notifySpectatorsAboutMoveMade(move, first, destination1, second, destination1);
					}
				}
				else {
					if (isRoundReveal(getCurrentRound() + 2)) {
						// if first isn't reveal and second is reveal, show xLastKnownMrXLocation for first and destination2 for second
						notifySpectatorsAboutMoveMade(move, first, xLastKnownMrXLocation, second, destination2);
					} else {
						// if both aren't reveal, show xLastKnownMrXLocation for both
						notifySpectatorsAboutMoveMade(move, first, xLastKnownMrXLocation, second, xLastKnownMrXLocation); // if both aren't reveal, show both xLastKnownMrXLocation
					}
				}
				// Then, change location to destination1
				// And, remove First ticket
				player.location(destination1);
				player.removeTicket(first);

				xCurrentRound += 1;
				notifySpectatorsAboutRoundStarted();

				// Then, notify about First move
				if (isRoundReveal(getCurrentRound())) {
					// if current round is reveal, show destination1 for first move
					notifySpectatorsAboutMoveMade(move, first, destination1);
				} else {
					// if current round isn't reveal, show xLastKnownMrXLocation for first move
					notifySpectatorsAboutMoveMade(move, first, xLastKnownMrXLocation);
				}


				// Then, change location to destination2
				// And, remove Second ticket
				player.location(destination2);
				player.removeTicket(second);

				xCurrentRound += 1;
				notifySpectatorsAboutRoundStarted();

				// Then, notify about Second move
				if (isRoundReveal(getCurrentRound())) {
					// if current round is reveal, show destination2 for second move
					notifySpectatorsAboutMoveMade(move, second, destination2);
				} else {
					if (isRoundReveal(getCurrentRound()-1)) {
						// if current round isn't reveal, but previous is, show destination1 for second move
						notifySpectatorsAboutMoveMade(move, second, destination1);
					} else {
						// if current and previous rounds aren't reveal, show xLastKnownMrXLocation for second move
						notifySpectatorsAboutMoveMade(move, second, xLastKnownMrXLocation);
					}

				}

				// this was added, because we should remove ticket -> change player -> notify move made -> check is Game Over -> check is Rotation Completed
				setNextMove(isRotationCompleted);

			}
		});
	}


	private boolean setNextPlayer(boolean isRotationCompleted){

		if (xCurrentPlayer == xPlayers.size() - 1) {
			isRotationCompleted = true;
			xCurrentPlayer = 0;
		} else {
			xCurrentPlayer += 1;
		}

		return isRotationCompleted;
	}

	private void setNextMove(boolean isRotationCompleted){

		if (isGameOver()){
			notifySpectatorsAboutGameOver();
		} else {
			if(isRotationCompleted) {
				notifySpectatorsAboutRotationComplete();
			} else {
				nextMove();
			}
		}
	}

	private ArrayList<Integer> getTakenLocations () {
		ArrayList<Integer> locations = new ArrayList<>();
		for(ScotlandYardPlayer player : xPlayers) {
			if(player.isDetective()) {
				locations.add(player.location());
			}
		}
		return locations;
	}


	// --- Methods for Game Over ---
	@Override
	public boolean isGameOver() {
		if(areDetectivesTicketless() || allDetectivesDontHaveValidMoves() || noRoundsLeft()){
			mrXWon = true;
			return true;
		}
		else if(isMrXcaptured() || isMrXStuck() || isMrXCornered()){
			detectivesWon = true;
			return true;
		}
		else return false;
	}


	private boolean isMrXcaptured() {
		for(ScotlandYardPlayer player : xPlayers) {
			if(player.isDetective() && player.location() == getXPlayerbyColour(BLACK).location()) return true;
		}
		return false;
	}

	private boolean areDetectivesTicketless() {
		for(ScotlandYardPlayer player : xPlayers) {
			if(player.isDetective()){
				if(player.hasTickets(BUS) || player.hasTickets(UNDERGROUND) || player.hasTickets(TAXI)) return false;
			}

		}
		return true;
	}

	private boolean allDetectivesDontHaveValidMoves() {

		boolean ok = true;
		for(ScotlandYardPlayer player : xPlayers) {
			if(player.isDetective()) {
				ok &= getValidMoves(player).contains(new PassMove(player.colour()));
			}
		}
		return ok;
	}

	private boolean noRoundsLeft() {
		//  added check for the last player on this round, because you can't finish the game until the last player has played
		if(getCurrentRound() == xRounds.size() && this.xCurrentPlayer == 0) return true;
		return false;
	}


	private boolean isMrXStuck(){

		//Check whether mrX is stuck or mrX was freed before next rotation
		Set<Move> possibleMoves = getValidMoves(getXPlayerbyColour(BLACK));
		//added check if after the last player's move MrX is stuck (if he is stuck, he can't make a Pass Move)
		if (possibleMoves.isEmpty() && this.xCurrentPlayer == 0) return true;
		else return false;
	}

	private boolean isMrXCornered() {
		int isCornered = 0;
		//added check if all mrX's edges are taken by detectives
		Collection<Edge<Integer, Transport>> edgesToMrX = getGraph().getEdgesTo(getGraph().getNode(xPlayers.get(0).location()));
		for (Edge<Integer, Transport> edge : edgesToMrX) {
			for (ScotlandYardPlayer player : xPlayers) {
				if (player.isDetective() && player.location() == edge.destination().value()) {
					isCornered++;
				}
			}
		}

		if (isCornered == edgesToMrX.size()) return true;
		else return false;
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		if(isGameOver()) {
			if(mrXWon) xWinningPlayers.add(BLACK);
			if(detectivesWon) xWinningPlayers = xDetectives;
			return Collections.unmodifiableSet(xWinningPlayers);
		} else {
			return Collections.unmodifiableSet(emptySet());
		}
	}

	// ---  ---


	@Override
	public List<Colour> getPlayers() {
		ArrayList<Colour> cPlayers = new ArrayList<>();
		for (ScotlandYardPlayer xPlayer : xPlayers) {
			cPlayers.add(xPlayer.colour());
		}
		return Collections.unmodifiableList(cPlayers);
	}

	// if you need a real location, return player.location
	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {

		ScotlandYardPlayer player = getXPlayerbyColour(colour);
		if(player == null) return Optional.empty();

		if(player.isMrX() && isRoundReveal(getCurrentRound())) {
			xLastKnownMrXLocation = player.location();
			return Optional.of(player.location());

		} else if (player.isMrX() && !isRoundReveal(getCurrentRound())) {
			//MrX's location is hidden
			return Optional.of(xLastKnownMrXLocation);

		} else return Optional.of(player.location());
	}

	private boolean isRoundReveal(int roundToCheck) {

		if (roundToCheck - 1 < 0) return false;
		return this.xRounds.get(roundToCheck - 1);
	}


	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		ScotlandYardPlayer player = getXPlayerbyColour(colour);
		if(player == null) return Optional.empty();
		return Optional.of(player.tickets().getOrDefault(ticket, 0));
	}


	private ScotlandYardPlayer getXPlayerbyColour(Colour colour){
		for(ScotlandYardPlayer player : xPlayers) {
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

	private void notifySpectatorsAboutRotationComplete() {
		for (Spectator spectator : getSpectators()) {
			spectator.onRotationComplete(this);
		}
	}

	private void notifySpectatorsAboutRoundStarted(){
		for(Spectator spectator : getSpectators()){
			spectator.onRoundStarted(this, getCurrentRound());
		}
	}

	//Overloaded for Pass move
	private void notifySpectatorsAboutMoveMade(Move move) {
		for (Spectator spectator : xSpectators) {
			spectator.onMoveMade(this, move);
		}
	}

	//Overloaded for Normal move
	private void notifySpectatorsAboutMoveMade(Move move, Ticket ticket, int destination) {
		for (Spectator spectator : xSpectators) {
			spectator.onMoveMade(this, new TicketMove(move.colour(), ticket, destination));
		}
	}

	//Overloaded for DoubleMove
	private void notifySpectatorsAboutMoveMade(Move move, Ticket ticket1, int destination1, Ticket ticket2, int destination2) {
		for (Spectator spectator : xSpectators) {
			spectator.onMoveMade(
							this, new DoubleMove(move.colour(),
							new TicketMove(move.colour(), ticket1, destination1),
							new TicketMove(move.colour(), ticket2, destination2))
			);
		}
	}

	private void notifySpectatorsAboutGameOver() {
		for(Spectator spectator : getSpectators()) {
			spectator.onGameOver(this, getWinningPlayers());
		}
	}

}

