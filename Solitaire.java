/**********************************************************************
* Solitaire: A program for playing all possible moves of solitaire
* Copyright (C) 2014 Jonathan Richards
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
* 
* Written by Jonathan Richards, jonrds@gmail.com
**********************************************************************/

import java.util.*;

class Solitaire {
	//represents a single card
	//do not duplicate ()
	private static class Card {
		int suit;
		int number;
		public Card(int s, int n) {
			suit = s;
			number = n;
		}
		//can this card be stacked on the card above it on the playing table?
		public boolean stackable(Card upper) {
			return (this.suit % 2 != upper.suit % 2 
				&& this.number == upper.number - 1);
		}
		//can this card be placed on the card below it in the finished columns?
		public boolean can_place(Card lower) {
			return (this.suit == lower.suit
				&& this.number == lower.number + 1);
		}
	}
	
	//A single possible state of the game. Hashable
	private class State {
		Vector<Card> deck; //deck of extra cards
		List<Vector<Card>> finished; //finished cards, starting from aces
		List<Vector<Card>> up_cards; //face up cards on table
		List<Vector<Card>> down_cards; //face down cards on table

		public State(Vector<Card> d, List<Vector<Card>> f,
			List<Vector<Card>> up, List<Vector<Card>> down) {
			deck = d;
			finished = f;
			up_cards = up;
			down_cards = down;
		}

		@Override
		public int hashCode() {
			int result = 17;
			result = 37*result + deck.hashCode();
			result = 37*result + finished.hashCode();
			result = 37*result + up_cards.hashCode();
			result = 37*result + down_cards.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof State)) return false;
			if (o == this) return true;

			State state2 = (State) o;
			return (deck.equals(state2.deck) && finished.equals(state2.finished)
				&& up_cards.equals(state2.up_cards) && down_cards.equals(state2.down_cards));
		}

		//make all possible types of moves
		public int play() {
			return (hand_to_table_or_finished() + table_to_table() 
				+ table_to_finished());
		}

		//make a move playing from the hand holding the deck
		public int hand_to_table_or_finished() {
			int deck_size = deck.size();
			int changes = 0;
			for (int i=2; i<deck_size; i+=3) {
				changes += hand_to_table_helper(i);
				changes += hand_to_finished_helper(i);
			}
			changes += hand_to_table_helper(deck_size-1);
			changes += hand_to_finished_helper(deck_size-1);
			return changes;
		}

		//make a move playing from the hand to the table
		//called by hand_to_table_or_finished
		public int hand_to_table_helper(int i) {
			int new_states = 0;
			Card take = deck.get(i);
			for (int j=0; j<7; j++) {
				Vector<Card> put_stack = up_cards.get(j);
				if ((!put_stack.isEmpty() && take.stackable(put_stack.lastElement())) 
					|| (put_stack.isEmpty() && take.number == 13)) {
					Vector<Card> new_deck = new Vector<Card>(deck);
					List<Vector<Card>> new_up_cards = new ArrayList<Vector<Card>>(up_cards);
					Vector<Card> new_put_stack = new Vector<Card>(put_stack);
					new_put_stack.add(new_deck.remove(i));
					new_up_cards.set(j, new_put_stack);
					addState(new State(new_deck, finished, new_up_cards, down_cards));
					new_states++;
				}
			}
			return new_states;
		}

		//make a move playing from the hand to the finished stacks
		//called by hand_to_table_or_finished
		public int hand_to_finished_helper(int i) {
			int new_states = 0;
			Card take = deck.get(i);
			Vector<Card> put_stack = finished.get(take.suit);
			if ((!put_stack.isEmpty() && take.can_place(put_stack.lastElement()))
				|| (put_stack.isEmpty() && take.number == 1)) {
				Vector<Card> new_deck = new Vector<Card>(deck);
				List<Vector<Card>> new_finished = new ArrayList<Vector<Card>>(finished);
				Vector<Card> new_put_stack = new Vector<Card>(put_stack);
				new_put_stack.add(new_deck.remove(i));
				new_finished.set(take.suit, new_put_stack);
				addState(new State(new_deck, new_finished, up_cards, down_cards));
				new_states++;
			}
			return new_states;
		}
	
		//make a move on the table
		public int table_to_table() {
			int new_states = 0;
			Vector<Card> take_stack;
			Vector<Card> new_take_stack;
			Card take;
			Vector<Card> put_stack;
			Vector<Card> new_put_stack;
			List<Vector<Card>> new_up_cards;
			List<Vector<Card>> new_down_cards;
			Vector<Card> new_down_stack;
			
			//iterate through stacks to take from
			for (int i=0; i<7; i++) {
				take_stack = up_cards.get(i);
				if (take_stack.isEmpty()) continue;

				//iterate through cards in stack to take from
				int take_stack_size = take_stack.size();
				for (int k=0; k < take_stack_size; k++) {
					take = take_stack.get(k);
				
					//iterate through stacks to put onto
					for (int j=0; j<7; j++) {
						put_stack = up_cards.get(j);
						
						if ((!put_stack.isEmpty() && take.stackable(put_stack.lastElement()))
							|| (put_stack.isEmpty() && take.number == 13)) {
							//shallow copy list
							new_up_cards = new ArrayList<Vector<Card>>(up_cards);
							
							//shallow copy individual stacks
							new_take_stack = new Vector<Card>(take_stack);
							new_put_stack = new Vector<Card>(put_stack);
							
							//stack cards on other stack
							for (Card putting : new_take_stack.subList(k, take_stack_size)) {
								new_put_stack.add(putting);
							}
							new_take_stack.subList(k, take_stack_size).clear();
							
							
							//set new up cards
							new_up_cards.set(i, new_take_stack);
							new_up_cards.set(j, new_put_stack);

							//System.out.format("%d, %d to %d, %d\n", take.suit, take.number, put.suit, put.number);
							//turn a card face up
							if (new_up_cards.get(i).isEmpty() && !down_cards.get(i).isEmpty()) {
								//shallow copy down cards
								new_down_cards = new ArrayList<Vector<Card>>(down_cards);
								new_down_stack = new Vector<Card>(down_cards.get(i));
								
								//move down card to up card
								new_take_stack.add(new_down_stack.remove(new_down_stack.size()-1));

								//set new down cards
								new_down_cards.set(i, new_down_stack);
							} else {
								new_down_cards = down_cards;
							}
							if (addState(new State(deck, finished, new_up_cards, new_down_cards))) {
								new_states++;
							}
						}
					}
				}
			}
			return new_states;
		}

		//move a card from the table to the finished stacks
		public int table_to_finished() {
			int new_states = 0;
			Vector<Card> take_stack;
			Vector<Card> new_take_stack;
			Card take;
			Vector<Card> put_stack;
			List<Vector<Card>> new_up_cards;
			List<Vector<Card>> new_down_cards;
			List<Vector<Card>> new_finished;
			Vector<Card> new_down_stack;
			Vector<Card> new_finished_stack;

			for (int i=0; i<7; i++) {
				take_stack = up_cards.get(i);
				if (take_stack.isEmpty()) continue;
				//take = take_stack.get(take_stack.size()-1);
				take = take_stack.lastElement();
			
				put_stack = finished.get(take.suit);
				if ((!put_stack.isEmpty() && take.can_place(put_stack.lastElement()))
					|| (put_stack.isEmpty() && take.number == 1)) {
					//shallow copy list
					new_up_cards = new ArrayList<Vector<Card>>(up_cards);
					new_finished = new ArrayList<Vector<Card>>(finished);
						
					//shallow copy individual stacks
					new_take_stack = new Vector<Card>(take_stack);
					new_finished_stack = new Vector<Card>(put_stack);

					new_finished_stack.add(new_take_stack.remove(new_take_stack.size()-1));
					
					new_up_cards.set(i, new_take_stack);
					new_finished.set(take.suit, new_finished_stack);

					if (new_up_cards.get(i).isEmpty() && !down_cards.get(i).isEmpty()) {
						//shallow copy down cards
						new_down_cards = new ArrayList<Vector<Card>>(down_cards);
						new_down_stack = new Vector<Card>(down_cards.get(i));
						
						//move down card to up card
						new_take_stack.add(new_down_stack.remove(new_down_stack.size()-1));

						//set new down cards
						new_down_cards.set(i, new_down_stack);
					} else {
						new_down_cards = down_cards;
					}
					if (addState(new State(deck, new_finished, new_up_cards, new_down_cards))) {
						new_states++;
					}
				}
				
			}
			return new_states;
		}

		//printable representation of the state of the game
		public void print_state() {
			for (Vector<Card> l : finished) {
				
				for (Card c : l) {
					System.out.format("%d, %d\t", c.suit, c.number);
				}
				System.out.println();
			}
			//System.out.println();

			for (Card c : deck) {
	    		System.out.format("%d, %d\t", c.suit, c.number);
	    	}

	    	for (Vector<Card> l : down_cards) {
				System.out.println();
	    		for (Card c : l) {
	    			System.out.format("%d, %d\t", c.suit, c.number);
	    		}
	    	}
	    	System.out.println();
	    	for (Vector<Card> l : up_cards) {
	    		System.out.println("");
	    		for (Card c : l) {
	    			System.out.format("%d, %d\t", c.suit, c.number);
	    		}
	    	}
	    	System.out.println();
		}
	}

	Queue<State> states = new LinkedList<State>();
	HashSet<State> done_states = new HashSet<State>();

	//constructor
	public Solitaire() {
		//create shuffled deck
		//need an arraylist first to allow for Collections.shuffle
		List<Card> cs = new ArrayList<Card>();
		for (int i=0; i < 4; i++) {
			for (int j=1; j <= 13; j++) {
				cs.add(new Card(i, j));
			}
		}
		Collections.shuffle(cs);

		Vector<Card> cards = new Vector<Card>();
		for (Card c : cs) {
			cards.add(c);
		}

		//create empty finished stacks
		List<Vector<Card>> finished = new ArrayList<Vector<Card>>();
		for (int i=0; i < 4; i++) {
			finished.add(new Vector<Card>());
		}

		//deal out table
		List<Vector<Card>> up_cards = new ArrayList<Vector<Card>>();
		List<Vector<Card>> down_cards = new ArrayList<Vector<Card>>();	
		for (int i=0; i < 7; i++) {
			
			down_cards.add(new Vector<Card>());
			for (int j=0; j<i; j++) {
				down_cards.get(i).add(cards.remove(cards.size()-1));
			}

			up_cards.add(new Vector<Card>());
			up_cards.get(i).add(cards.remove(cards.size()-1));
		}

		State state = new State(cards, finished, up_cards, down_cards);

		states.add(state);
		done_states.add(state);
	}
	
	//adds a state if it hasn't been added already
	public boolean addState(State s) {
		if (done_states.contains(s)) {
			System.out.println("Duplicate state");
			s.print_state();
			return false;
		} else {
			states.add(s);
			done_states.add(s);
			return true;			
		}
	}
	
    public static void main(String[] args) {
    	Solitaire s = new Solitaire();
    	State state = s.states.poll();

    	//for creating test cases
		Card c1 = new Card(1, 1);
		Card c2 = new Card(2, 2);
		Card c3 = new Card(1, 7);
		Card c4 = new Card(2, 8);
		Card c5 = new Card(1, 3);
		Card c6 = new Card(2, 13);
		Card c7 = c5;
		Vector<Card> d1 = new Vector<Card>();
		Vector<Card> d2 = new Vector<Card>();
		Vector<Card> d3 = new Vector<Card>();
		Vector<Card> d4 = new Vector<Card>();
		Vector<Card> d5 = new Vector<Card>();
		Vector<Card> d6 = new Vector<Card>();
		Vector<Card> d7 = new Vector<Card>();
		List<Vector<Card>> list = new ArrayList<Vector<Card>>();
		d1.add(c2);
		d1.add(c1);
		d2.add(c6);
		d3.add(c4);
		d4.add(c4);
		d5.add(c4);
		d6.add(c6);
		d7.add(c7);
		list.add(d1);
		list.add(d2);
		list.add(d3);
		list.add(d4);
		list.add(d5);
		list.add(d6);
		list.add(d7);
		state.up_cards = list;


    	state.print_state();
    	System.out.format("%d new states\n", state.play());

    	while ((state = s.states.poll()) != null) {
	    	state.print_state();
    	}

    	/*
    	System.out.println("ROUND 2");

    	Queue<State> round2 = new LinkedList<State>();

    	while ((state = s.states.poll()) != null) {
	    	round2.add(state);
    	}

    	while ((state = round2.poll()) != null) {
	    	System.out.println();
	    	state.print_state();
	    	System.out.format("%d new states\n", state.play());
    	}
    	/*

    	System.out.println("ROUND 3");

    	while ((state = s.states.poll()) != null) {
	    	System.out.println();
	    	state.print_state();
    	}
    	*/
    }
}


