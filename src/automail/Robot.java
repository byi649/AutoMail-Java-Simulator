// Group 7
package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import simulation.Building;
import simulation.Clock;
import simulation.IMailDelivery;

import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public class Robot {

    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;

    IMailDelivery delivery;
    protected final String id;
    /** Possible states the robot can be in */
    public enum RobotState { DELIVERING, WAITING, RETURNING, HEATING } //HEATING
    // CASE HEATING. Heat counter ++, opeartor is called once each and a case in time
    private int heatTime = 0;
    private static final int DONE_HEATING = 5;

    public RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private MailPool mailPool;
    private boolean receivedDispatch;
    private Building building;

    private MailTube mailTube = null;
    private FoodTube foodTube = null;
    private Hands hands = null;

    private int deliveryCounter;

    private int regularItemDelivered;
    private int foodItemDelivered;
    private int totalRegularItemWeight;
    private int totalFoodItemWeight;

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     * @param number the number assigned as part of robot ID
     * @param building the building the robots operate in
     */
    public Robot(IMailDelivery delivery, MailPool mailPool, int number, Building building){
    	this.id = "R" + number;
    	current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
        this.regularItemDelivered = 0;
        this.foodItemDelivered = 0;
        this.totalRegularItemWeight = 0;
        this.totalFoodItemWeight = 0;
        this.building = building;

        this.foodTube = new FoodTube();
        this.mailTube = new MailTube();
        this.hands = new Hands();
    }
    
    /**
     * This is called when a robot is assigned the mail items and ready to dispatch for the delivery 
     */
    public void dispatch() { receivedDispatch = true; }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void operate() throws ExcessiveDeliveryException {   
    	switch(current_state) {
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION){
                    if (hands.getNumHolding() > 0) {
                        mailPool.addToPool(hands.getItem());
                        System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), hands.getItem().toString());
                        hands.removeItem();
                    }
                	if (mailTube.getNumHolding() > 0) {
                		mailPool.addToPool(mailTube.getItem());
                        System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), mailTube.getItem().toString());
                        mailTube.removeItem();
                	}
                	if (foodTube.getNumHolding() > 0) {
                	    for(int i = foodTube.getNumHolding(); i > 0; i--) {
                            mailPool.addToPool(foodTube.getItem());
                            System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), foodTube.getItem().toString());
                            foodTube.removeItem();
                	    }
                    }
        			/** Tell the sorter the robot is ready */
        			mailPool.registerWaiting(this);
                	changeState(RobotState.WAITING);
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if(!isEmpty() && receivedDispatch){
                	receivedDispatch = false;
                	deliveryCounter = 0; // reset delivery counter
                	setDestination();
                	if(foodTube.getNumHolding() > 0){
                	    changeState(RobotState.HEATING);
                    } else {
                        changeState(RobotState.DELIVERING);
                    }
                }
                break;
    		case DELIVERING:
    			if(current_floor == destination_floor){ // If already here drop off either way
                    /** Delivery complete, report this to the simulator! */
                    if(foodTube.getNumHolding() > 0){
                        delivery.deliver(foodTube.getItem());
                        building.freeFloor(destination_floor);
                        updateStatistics();
                        foodTube.removeItem();
                    } else {
                        delivery.deliver(hands.getItem());
                        updateStatistics();
                        hands.removeItem();
                    }
                    deliveryCounter++;
                    if(deliveryCounter > 3){  // Implies a simulation bug
                    	throw new ExcessiveDeliveryException();
                    }
                    /** Check if want to return, i.e. if there is no item in the tube*/
                    if(isEmpty()) {
                        changeState(RobotState.RETURNING);
                    }
                    else{
                        if(mailTube.getNumHolding() > 0) {
                            /** If there is another mail item, set the robot's route to the location to deliver the item */
                            hands.addItem(mailTube.getItem());
                            mailTube.removeItem();
                        }
                        setDestination();
                        changeState(RobotState.DELIVERING);
                    }

    			} else {
                    if(foodTube.getNumHolding() > 0 && !building.isReserved(destination_floor)) {
                        building.reserveFloor(destination_floor, this.id);
                    }
	        		/** The robot is not at the destination yet, move towards it! */
                    if(!building.isReserved(destination_floor) || building.reservedBy(destination_floor).equals(id)) {
                        moveTowards(destination_floor);
                    }
    			}
                break;
            case HEATING:
                if(heatTime == 4){
                    changeState(RobotState.DELIVERING);
                    heatTime = 0;
                } else {
                    heatTime += 1;
                }
                break;
    	}
    }

    /**
     * Sets the route for the robot
     */
    private void setDestination() {
        /** Set the destination floor */
        if(foodTube.getNumHolding() > 0) {
            destination_floor = foodTube.getItem().getDestFloor();
            if(!building.isReserved(destination_floor)) {
                building.reserveFloor(destination_floor, this.id);
            }
        } else if (hands.getNumHolding() > 0) {
            destination_floor = hands.getItem().getDestFloor();
        }
    }

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination) {
        if(current_floor < destination){
            current_floor++;
        } else {
            current_floor--;
        }
    }
    
    private String getIdTube() {
        int itemInTube;
        if(foodTube.getNumHolding() > 0){
            itemInTube = foodTube.getNumHolding();
        } else if(mailTube.getNumHolding() > 0){
            itemInTube = mailTube.getNumHolding();
        } else{
            itemInTube = 0;
        }
    	return String.format("%s(%1d)", this.id, itemInTube);
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState){
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	if(nextState == RobotState.DELIVERING){
    	    if((foodTube != null) && (foodTube.getNumHolding() != 0)){
                System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), foodTube.getItem().toString());
            } else {
                System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), hands.getItem().toString());
            }
    	}
    }
    
	static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

//	@Override
//	public int hashCode() {
//		Integer hash0 = super.hashCode();
//		Integer hash = hashMap.get(hash0);
//		if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
//		return hash;
//	}

	public boolean isEmpty() {
		return ((hands.getNumHolding() == 0) && (mailTube.getNumHolding() == 0) && (foodTube.getNumHolding() == 0));
	}

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException {
        hands.addItem(mailItem);
		if (hands.getItem().weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}

    /**
     * Adds the item into it's proper tube
     * @param mailItem the item to be added
     * @param isFoodTube determines which tube the item is inserted to
     */
	public void addToTube(MailItem mailItem, Boolean isFoodTube) throws ItemTooHeavyException {
        if (isFoodTube) {
            foodTube.addItem(mailItem);
            if (mailItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
        } else {
            mailTube.addItem(mailItem);
            if (mailTube.getItem().weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
        }
    }

	/**Increments the statistics corresponding to the mail item delivered**/
	private void updateStatistics(){
	    if(foodTube.getNumHolding() == 0) {
            if(hands.getNumHolding() > 0) {
                regularItemDelivered++;
                totalRegularItemWeight += hands.getItem().getWeight();
            } else {
                regularItemDelivered++;
                totalRegularItemWeight += mailTube.getItem().getWeight();
            }
        } else {
            foodItemDelivered++;
            totalFoodItemWeight += foodTube.getItem().getWeight();
        }
	}

    //a bunch of getters :o
    public int getRegularItemDelivered(){ return regularItemDelivered; }
    public int getFoodItemDelivered(){ return foodItemDelivered; }
    public int getTotalRegularItemWeight(){ return totalRegularItemWeight; }
    public int getTotalFoodItemWeight(){ return totalFoodItemWeight; }
}