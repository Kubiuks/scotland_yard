package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;

@ManagedAI("RandomAI")
public class RandomAI implements PlayerFactory {

    @Override
    public Player createPlayer(Colour colour) {
        return new MyPlayer();
    }

    private static class MyPlayer implements Player {

        private final Random random = new Random();

        @Override
        public void makeMove(ScotlandYardView view, int location, Set<Move> moves, Consumer<Move> callback) {

            callback.accept(new ArrayList<>(moves).get(random.nextInt(moves.size())));
        }
    }
}