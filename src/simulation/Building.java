// Group 7
package simulation;

public class Building {

	
    /** The number of floors in the building **/
    public static int FLOORS;
    
    /** Represents the ground floor location */
    public static final int LOWEST_FLOOR = 1;
    
    /** Represents the mailroom location */
    public static final int MAILROOM_LOCATION = 1;

    //An array representing each floor and it's reservation status
    private String[] floors;

    public Building(){
        //Initialize the floors in the building
        floors = new String[FLOORS+1];
    }

    public void reserveFloor(int floorNumber, String robotID){ floors[floorNumber] = robotID;}

    public void freeFloor(int floorNumber){floors[floorNumber] = null;}

    public boolean isReserved(int floorNumber) {return floors[floorNumber] != null;}

    public String reservedBy(int floorNumber) {return floors[floorNumber];}
}
