import java.util.*;

public class CardManager {

	public static final int STARTING_HAND_SIZE = 7;

	private Deck deck;
	private ArrayList<Card> hand;
	private ArrayList<PokemonCard> bench;
	private ArrayList<Card> prizeCards;
	private ArrayList<Card> discardPile;
	private PokemonCard activePokemon;

	//Constructor and helper methods
	public CardManager(){
		buildDeck();
		selectHand();
		selectPrizeCards();
		discardPile = new ArrayList<Card>();
		bench = new ArrayList<PokemonCard>();
	}
	private void buildDeck(){
		deck = new Deck();
		
		ArrayList<Integer> cardNumbers = Parser.readInDeck("deck1.txt");
		
		for (Integer num : cardNumbers){
			Card card = Parser.cards.get(num - 1).shallowCopy();
			if (card != null)
				deck.push(card);
		}
		
		deck.shuffle();
	}
	private void selectHand(){
		hand = new ArrayList<Card>();
		
		for (int i = 0; i < 7; i++){
			Card card = deck.pop();
			hand.add(card);
		}
		
		while (getFirstPokemon() == null){
			Iterator<Card> it = hand.iterator();
			
			while (it.hasNext()){
				Card card = it.next();
				deck.push(card);
				it.remove();
			}
			
			deck.shuffle();
			for (int i = 0; i < 7; i++){
				Card card = deck.pop();
				hand.add(card);
			}
		}
	}
	private void selectPrizeCards(){
		prizeCards = new ArrayList<Card>(6);
		
		for (int i = 0; i < 6; i++){
			Card card = deck.pop();
			prizeCards.add(card);
		}
	}

	
	public void setActivePokemon(PokemonCard pokemon){
		activePokemon = pokemon;

		if(hand.contains(pokemon)){
			hand.remove(pokemon);
		}
		else if(bench.contains(pokemon)){
			bench.remove(pokemon);
		}
	}
	
	public void removeActivePokemon(){
		discardActivePokemon();
		activePokemon = null;
	}
	
	public void discardActivePokemon(){
		ArrayList<EnergyCard> energy = activePokemon.getEnergy();
		for (EnergyCard card : energy){
			discardPile.add(card);
			activePokemon.removeEnergy(card);
		}
		discardPile.add(activePokemon);
	}

	public void attachEnergy(EnergyCard energy, PokemonCard pokemon){
		pokemon.attachEnergy(energy);
		hand.remove(energy);
	}
	
	public void addCardToHandFromDeck(int index){
		Card card = deck.getCardAtIndex(index);
		hand.add(card);
		deck.removeCardAtIndex(index);
	}

	public boolean movePokemonToBench(PokemonCard pokemon){
		if (bench.size() < 5){
			bench.add(pokemon);
			hand.remove(pokemon);
			return true;
		}
		return false;
	}

	public void drawPrizeCard(){
		hand.add(prizeCards.get(0));
		prizeCards.remove(prizeCards.get(0));
	}

	public void addToDiscard(Card card){
		//NOTE: DOES NOT REMOVE CARD FROM ANYTHING
		discardPile.add(card);
	}

	public void addPokemonCardToDiscard(PokemonCard card){
		//TODO update with evolution
		//Does not remove card from anything
		while(card.energy.size() > 0){
			EnergyCard discard = card.energy.remove(0);
			addToDiscard(discard);
		}

		discardPile.add(card);
	}

	public void shuffleHandIntoDeck(){
		for(Card c : hand){
			deck.push(c);
		}
		deck.shuffle();
	}

	public void retreatPokemon(PokemonCard cardToSwapWith){
		int amountToRemove = activePokemon.getEnergyToRetreat();

		if(amountToRemove <= activePokemon.energy.size()){
			for(int i = 0; i < amountToRemove; i++){
				EnergyCard e = activePokemon.energy.remove(0);
				discardPile.add(e);
			}

			int index = bench.indexOf(cardToSwapWith);
			PokemonCard temp = activePokemon;
			setActivePokemon(cardToSwapWith);
			bench.add(index, temp);
		}
		else{
			System.out.println("bork");
		}
	}

	//Getters
	public ArrayList<Card> getHand(){
		return this.hand;
	}
	public ArrayList<PokemonCard> getBench(){
		return this.bench;
	}
	public PokemonCard getActivePokemon(){
		return this.activePokemon;
	}
	public ArrayList<Card> getDiscard(){
		return this.discardPile;
	}
	public ArrayList<Card> getDeck(){
		return this.deck.getCards();
	}
	public ArrayList<Card> getPrizeCards(){
		return this.prizeCards;
	}

	//Special Getters
	public PokemonCard getFirstCardOfBench(){
		return this.bench.get(0);
	}
	public PokemonCard getNextPokemon(int index){
		for (; index < hand.size(); index++){
			Card card = hand.get(index);
			if (card instanceof PokemonCard) return (PokemonCard) card;
		}
		return null;
	}
	public EnergyCard getFirstEnergy(){
		for (Card card : hand){
			if (card instanceof EnergyCard) return (EnergyCard) card;
		}
		return null;
	}
	public PokemonCard getFirstPokemon(){
		for (Card card : hand){
			if (card instanceof PokemonCard){
				PokemonCard pc = (PokemonCard) card;
				if(pc.getCat() == PokemonCard.Category.BASIC){
					return pc;
				}
			}

		}
		return null;
	}
}