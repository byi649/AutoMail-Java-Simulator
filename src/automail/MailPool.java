// Group 7
package automail;

import java.util.LinkedList;
import java.util.Comparator;
import java.util.ListIterator;

import exceptions.ItemTooHeavyException;
import simulation.PriorityMailItem;

/**
 * addToPool is called when there are mail items newly arrived at the building to add to the MailPool or
 * if a robot returns with some undelivered items - these are added back to the MailPool.
 * The data structure and algorithms used in the MailPool is your choice.
 * 
 */
public class MailPool {

	private class Item {
		int priority;
		int destination;
		MailItem mailItem;
		boolean IsFoodItem;
		// Use stable sort to keep arrival time relative positions
		
		public Item(MailItem mailItem) {
			priority = (mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1;
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
			this.IsFoodItem = mailItem instanceof FoodItem;
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.priority < i2.priority) {
				order = 1;
			} else if (i1.priority > i2.priority) {
				order = -1;
			} else if (i1.destination < i2.destination) {
				order = 1;
			} else if (i1.destination > i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> regularPool;
	private LinkedList<Item> foodPool;
	private LinkedList<Robot> robots;
	private int timesTubeAttached;

	public MailPool(int nrobots){
		// Start empty
		regularPool = new LinkedList<Item>();
		foodPool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
		timesTubeAttached = 0;
	}

	/**
     * Adds an item to the mail pool
     * @param mailItem the mail item being added.
     */
	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		if (item.IsFoodItem) {
			foodPool.add(item);
			foodPool.sort(new ItemComparator());
		} else {
			regularPool.add(item);
			regularPool.sort(new ItemComparator());
		}
	}
	
	
	
	/**
     * load up any waiting robots with mailItems, if any.
     */
	public void loadItemsToRobot() throws ItemTooHeavyException {
		//List available robots
		ListIterator<Robot> i = robots.listIterator();
		while (i.hasNext()) loadItem(i);
	}
	
	//load items to the robot
	private void loadItem(ListIterator<Robot> i) throws ItemTooHeavyException {
		Robot robot = i.next();
		assert(robot.isEmpty());
		// System.out.printf("P: %3d%n", pool.size());
		// Prioritize regular mail items first (no order mentioned in specification)
		if (regularPool.size() > 0 && (foodPool.size() == 0 || new ItemComparator().compare(regularPool.listIterator().next(), foodPool.listIterator().next()) < 0)) {
			ListIterator<Item> j = regularPool.listIterator();
			if (regularPool.size() > 0) {
				try {
					robot.addToHand(j.next().mailItem); // hand first as we want higher priority delivered first
					j.remove();
					if (regularPool.size() > 0) {
						robot.addToTube(j.next().mailItem, false);
						j.remove();
					}
					robot.dispatch(); // send the robot off if it has any items to deliver
					i.remove();       // remove from mailPool queue
				} catch (Exception e) {
					throw e;
				}
			}
		} else {
			ListIterator<Item> j = foodPool.listIterator();
			if (foodPool.size() > 0) {
				// Food tube can hold 3 items
				try {
					robot.addToTube(j.next().mailItem, true);
					timesTubeAttached++;
					j.remove();

					if (foodPool.size() > 0) {
						robot.addToTube(j.next().mailItem, true);
						j.remove();
					}
					if (foodPool.size() > 0) {
						robot.addToTube(j.next().mailItem, true);
						j.remove();
					}
					robot.dispatch(); // send the robot off if it has any items to deliver
					i.remove();       // remove from mailPool queue
				} catch (Exception e) {
					throw e;
				}
			}
		}

	}

	/**
     * @param robot refers to a robot which has arrived back ready for more mailItems to deliver
     */	
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

	public int getTimesTubeAttached() { return timesTubeAttached;}

}
