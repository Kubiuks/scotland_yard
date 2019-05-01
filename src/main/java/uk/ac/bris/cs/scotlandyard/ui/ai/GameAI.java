
package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

public class GameAI {
    private List<Colour> xPlayers;
    private List<PlayerAI> xDetectives;
    private PlayerAI mrX;
    private Graph<Integer, Transport> xGraph;
    private List<Boolean> xRounds;
    private int xCurrentRound;
    private List<GameAI> xNodes;

    private final Ticket[] xArr = {BUS, TAXI, UNDERGROUND, SECRET, DOUBLE};
    private final ArrayList<Ticket> xTicketTypes = new ArrayList<>(Arrays.asList(xArr)); // created array list of tickets to loop for the mrX tickets and use in if statement

    public GameAI(ScotlandYardView view, int location) { // constructor for the initial tree

        this.xPlayers = view.getPlayers();
        this.xDetectives = new ArrayList<>();

        Map<Ticket, Integer> mrXTickets = new HashMap<>();

        if (view.getCurrentPlayer().isMrX()) {
            for (Ticket ticket : xTicketTypes) {
                mrXTickets.put(ticket, view.getPlayerTickets(view.getCurrentPlayer(), ticket).get());
            }
            this.mrX = new PlayerAI(view.getCurrentPlayer(), location, mrXTickets);

        }

        for (Colour colour : this.xPlayers) {
            if (!colour.equals(Colour.BLACK)) {
                Map<Ticket, Integer> detectiveTickets = new HashMap<>();

                for (Ticket ticket : xTicketTypes) {
                    if (ticket != DOUBLE && ticket != SECRET) {
                        detectiveTickets.put(ticket, view.getPlayerTickets(colour, ticket).get());
                    } else {
                        detectiveTickets.put(ticket, 0);
                    }
                }

                PlayerAI xDetective = new PlayerAI(colour, view.getPlayerLocation(colour).get(), detectiveTickets);

                this.xDetectives.add(xDetective);
            }
        }
        this.xGraph = view.getGraph();
        this.xRounds = view.getRounds();
        this.xCurrentRound = view.getCurrentRound();
        this.xNodes = new ArrayList<>();
    }

    public GameAI(GameAI game, Move move) { // constructor for the nodes of the tree for mrX

        this.xPlayers = game.xPlayers;
        this.mrX = game.getMrXlocation();
        this.xDetectives = game.getxDetectives();

        if (move.colour().isMrX()) {
            Map<Ticket, Integer> mrXTickets = new HashMap<>();

            for (Ticket ticket : xTicketTypes) {
                if (ticketTypeOfMove(move, ticket)) mrXTickets.put(ticket, game.mrX.tickets.get(ticket) - 1);
                else mrXTickets.put(ticket, game.mrX.tickets.get(ticket));
            }
            this.mrX = new PlayerAI(game.mrX.colour, game.mrX.location, mrXTickets);
        } else if (!move.colour().isMrX()) {
            this.xDetectives = new ArrayList<>();

            for (PlayerAI detective : game.xDetectives) {
                Map<Ticket, Integer> detectiveTickets = new HashMap<>();

                for (Ticket ticket : xTicketTypes) {
                    if (ticket != DOUBLE && ticket != SECRET) {
                        if (ticketTypeOfMove(move, ticket))
                            detectiveTickets.put(ticket, detective.tickets.get(ticket) - 1);
                        else detectiveTickets.put(ticket, detective.tickets.get(ticket));
                    } else {
                        detectiveTickets.put(ticket, 0);
                    }
                }

                PlayerAI xDetective = new PlayerAI(detective.colour, getXlocation(move), detectiveTickets);
                this.xDetectives.add(xDetective);
            }
        }

        game.xNodes.add(this);
        this.xGraph = game.xGraph;
        this.xNodes = new ArrayList<>();
        this.xRounds = game.getRounds();
        this.xCurrentRound = game.getCurrentRound() + 1; // dont forget to increment round after moves
    }

    public PlayerAI getPlayer(Colour colour) { // taken from model, gets the player

        ArrayList<PlayerAI> allPlayers = new ArrayList<>();
        allPlayers.add(this.getMrXlocation());
        allPlayers.addAll(this.getxDetectives());
        PlayerAI xPlayer = null;
        for (PlayerAI aPlayer : allPlayers) {
            if (colour.equals(aPlayer.colour)) {
                xPlayer = aPlayer;
            }
        }
        return xPlayer;
    }

    private boolean ticketTypeOfMove(Move move, Ticket ticket) { // checks which ticket type was assigned to the move
        boolean xType = false;

        if (move instanceof DoubleMove) {
            if ((((DoubleMove) move).firstMove().ticket().equals(ticket)) || ((DoubleMove) move).secondMove().ticket().equals(ticket) || ticket.equals(Ticket.DOUBLE)) {
                xType = true;
            }
        }
        if (move instanceof TicketMove) {
            if (((TicketMove) move).ticket().equals(ticket)) {
                xType = true;
            }
        }
        return xType;
    }

    private int getXlocation(Move move) { // gets the final location of the move
        int xLocation = 0;
        if (move instanceof TicketMove) {
            xLocation = ((TicketMove) move).destination();
        } else if (move instanceof DoubleMove) {
            xLocation = ((DoubleMove) move).finalDestination();
        }
        return xLocation;
    }

    public ArrayList<Integer> getTakenLocations() { // taken from model, used for getvalidmoves
        ArrayList<Integer> locations = new ArrayList<>();
        for (Colour player : xPlayers) {
            if (player.isDetective()) {
                locations.add(getPlayer(player).location);
            }
        }
        return locations;
    }

    public Graph<Integer, Transport> getGraph() {
        return this.xGraph;
    }

    public List<PlayerAI> getxDetectives() {
        return this.xDetectives;
    }

    public PlayerAI getMrXlocation() {
        return this.mrX;
    }

    public List<Boolean> getRounds() {
        return this.xRounds;
    }

    public int getCurrentRound() {
        return this.xCurrentRound;
    }

    public void addNode(GameAI state) {
        this.xNodes.add(state);
    }

    public List<GameAI> getxNodes() {
        return this.xNodes;
    }

    public List<Colour> getPlayers() {
        return this.xPlayers;
    }
}