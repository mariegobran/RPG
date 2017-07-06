import java.util.*;
import java.util.concurrent.LinkedTransferQueue;



public class GameEngine {
	
	public static AIPlayer autoPlayer = new AIPlayer();
	public static HumanPlayer player = new HumanPlayer();

	private static Player currentPlayer = player;
	private static Player winner = null;
	
	public static MainWindow w;
	private static Object lock = new Object();
    public static Queue<Message> queue = new LinkedTransferQueue<>();

	public static void main(String[] args) {
		//create and display the main game window
		MainWindow.lock = lock;
		MainWindow.queue = queue;
		w = new MainWindow(autoPlayer, player);
		w.display();

		//give Ability class access to card managers
		Ability.playerCardManager = player.cardManager;
		Ability.AICardManager = autoPlayer.cardManager;

		handleMulligans(); //ensures each player has at least one pokemon

		setupPhase(); //players choose their initial active and benched pokemon

		rollForFirstTurn(); //determines which player gets to start

		//play game until there is a winner
		while(true){

		    //check win (lose) condition of having no cards to draw
		    if(currentPlayer.getDeck().size() == 0){
		        if(currentPlayer == player){
		            declareWinner(Ability.Player.PLAYER);
                }
                else{
		            declareWinner(Ability.Player.AI);
                }
                break;
            }

			currentPlayer.playTurn();

			if(winnerFound()){ break; }

			//TODO updateStatusEffects(); //e.g.; burns do damage between turns; sleeping pokémon have chance to wake up
            checkForKnockouts();
            if(winnerFound()){ break; }

			switchTurn();
		}


		//TODO showWinScreen();
        System.out.println("end");
    }

	private static void switchTurn(){
		if(currentPlayer == player){
			currentPlayer = autoPlayer;
		}
		else{
			currentPlayer = player;
		}
	}

	private static void setupPhase(){
		player.setup();
		autoPlayer.setup();
	}

	private static void handleMulligans(){
		//player mulligans
		while(player.cardManager.getFirstPokemon() == null){
			//shuffle hand into deck
			player.cardManager.shuffleHandIntoDeck();

			//draw new hand
			for(int i = 0; i < CardManager.STARTING_HAND_SIZE; i++){
				player.drawCard();
			}

			//opponent draws 1 card
			autoPlayer.drawCard();
		}

		//ai mulligans
		while(autoPlayer.cardManager.getFirstPokemon() == null){
			//shuffle hand into deck
			autoPlayer.cardManager.shuffleHandIntoDeck();

			//draw new hand
			for(int i = 0; i < CardManager.STARTING_HAND_SIZE; i++){
				autoPlayer.drawCard();
			}

			//opponent draws 1 card
			player.drawCard();
		}
	}

	private static void rollForFirstTurn(){
		if(RandomNumberGenerator.flipACoin()){
			currentPlayer = player;
		}
		else{
			currentPlayer = autoPlayer;
		}
	}

	public static void waitForInput(){
        try{
            synchronized(lock){
                lock.wait();
            }
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
	
	public static PokemonCard getChoiceOfCard(Ability.Target target){
		Message msg = null;
		PokemonCard cardToReturn = null;

		switch(target){
			case OPPONENT:
				//
				break;
			case OPPONENT_ACTIVE:
				cardToReturn = autoPlayer.getActivePokemon();
				break;
			case OPPONENT_BENCH:
				GameEngine.w.updateInstructions("Select a Pokémon on your opponent's bench.");
				waitForInput();
				msg = queue.remove();
				if(msg.getSide() == Message.Side.AI && msg.getType() == Message.ButtonType.BENCH){
					cardToReturn = autoPlayer.getBench().get(msg.getIndex());
				}
				break;
			case OPPONENT_POKEMON:
				GameEngine.w.updateInstructions("Select a Pokémon from your opponent's bench or active slot.");
				waitForInput();
				msg = queue.remove();
				if(msg.getSide() == Message.Side.AI && msg.getType() == Message.ButtonType.BENCH){
					cardToReturn = autoPlayer.getBench().get(msg.getIndex());
				}
				else if(msg.getSide() == Message.Side.AI && msg.getType() == Message.ButtonType.ACTIVE){
					cardToReturn = autoPlayer.getActivePokemon();
				}
				break;
			case YOU:
				//
				break;
			case YOUR_ACTIVE:
				cardToReturn = player.getActivePokemon();
				break;
			case YOUR_BENCH:
				GameEngine.w.updateInstructions("Select a Pokémon on your bench.");
				waitForInput();
				msg = queue.remove();
				if(msg.getSide() == Message.Side.PLAYER && msg.getType() == Message.ButtonType.BENCH){
					cardToReturn = player.getBench().get(msg.getIndex());
				}
				break;
			case YOUR_POKEMON:
				GameEngine.w.updateInstructions("Select a Pokémon from your bench or active slot.");
				waitForInput();
				msg = queue.remove();
				if(msg.getSide() == Message.Side.PLAYER && msg.getType() == Message.ButtonType.BENCH){
					cardToReturn = player.getBench().get(msg.getIndex());
				}
				else if(msg.getSide() == Message.Side.PLAYER && msg.getType() == Message.ButtonType.ACTIVE){
					cardToReturn = player.getActivePokemon();
				}
				break;
		}

		
		return cardToReturn;
	}

	public static PokemonCard choosePokemonCard(Player p, Ability.Target target){
		PokemonCard cardToReturn;
		
		if (p == player){
			cardToReturn = getChoiceOfCard(target);
			while (cardToReturn == null) {
				cardToReturn = getChoiceOfCard(target);
			}
		} else {
			if (target == Ability.Target.OPPONENT_BENCH){
				cardToReturn = player.cardManager.getFirstCardOfBench();
			} else if (target == Ability.Target.YOUR_BENCH){
				cardToReturn = autoPlayer.cardManager.getFirstCardOfBench();
			} else if (target == Ability.Target.OPPONENT_POKEMON){
				cardToReturn = player.cardManager.getActivePokemon();
			} else {
				cardToReturn = autoPlayer.cardManager.getFirstCardOfBench();
			}
		}
		return cardToReturn;
	}

	public static void checkForKnockouts(){
		//check AI bench
		for(PokemonCard p : autoPlayer.cardManager.getBench()){
			if(p.getCurrentHP() <= 0){
				autoPlayer.cardManager.getBench().remove(p);
				autoPlayer.cardManager.addPokemonCardToDiscard(p);

				if(player.getPrizeCards().size() == 1){
					declareWinner(Ability.Player.PLAYER);
					return;
				}
				else{
					player.cardManager.drawPrizeCard();
				}
			}
		}

		//check player bench
		for(PokemonCard p : player.cardManager.getBench()){
			if(p.getCurrentHP() <= 0){
				player.cardManager.getBench().remove(p);
				player.cardManager.addPokemonCardToDiscard(p);

				if(autoPlayer.getPrizeCards().size() == 1){
					declareWinner(Ability.Player.AI);
					return;
				}
				else{
					autoPlayer.cardManager.drawPrizeCard();
				}
			}
		}

		//check AI active
		if(autoPlayer.cardManager.getActivePokemon().getCurrentHP() <= 0){
			autoPlayer.cardManager.addPokemonCardToDiscard(autoPlayer.getActivePokemon());
			autoPlayer.cardManager.removeActivePokemon();

			if(autoPlayer.getPrizeCards().size() == 1){
				declareWinner(Ability.Player.PLAYER);
				return;
			}
			else{
				player.cardManager.drawPrizeCard();
			}

			if(! autoPlayer.chooseNewActivePokemon()){
				declareWinner(Ability.Player.PLAYER);
				return;
			}
		}

		//check player active
		if(player.cardManager.getActivePokemon().getCurrentHP() <= 0){
			player.cardManager.addPokemonCardToDiscard(player.getActivePokemon());
			player.cardManager.removeActivePokemon();

			if(autoPlayer.getPrizeCards().size() == 1){
				declareWinner(Ability.Player.AI);
			}
			else{
				autoPlayer.cardManager.drawPrizeCard();
			}

			if(! player.chooseNewActivePokemon()){
				declareWinner(Ability.Player.AI);
				return;
			}
		}
	}

	public static void declareWinner(Ability.Player p){
		switch(p){
			case AI:
				winner = autoPlayer;
				break;
			case PLAYER:
				winner = player;
				break;
		}
	}

	public static boolean winnerFound(){
	    return winner != null;
    }
}