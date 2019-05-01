package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Ticket;

import java.util.HashMap;
import java.util.Map;

public class PlayerAI {

    Colour colour;
    int location;
    Map<Ticket, Integer> tickets;

    public PlayerAI(Colour colour, int location, Map<Ticket, Integer> tickets) {

        this.colour = colour;
        this.location = location;
        this.tickets = tickets;
    }

    public boolean hasTickets(Ticket ticket) {
        return (Integer)this.tickets.get(ticket) != 0;
    }

    public boolean hasTickets(Ticket ticket, int quantityInclusive) {
        return (Integer)this.tickets.get(ticket) >= quantityInclusive;
    }


}
