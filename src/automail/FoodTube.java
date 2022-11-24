// Group 7
package automail;

public class FoodTube implements Holdable {

    private static final int HOLD_LIMIT = 3;
    private int numCurrHolding = 0;
    private MailItem[] items = new MailItem[HOLD_LIMIT];

    public FoodTube(){
        // count to 5 units of time

    }

    public void addItem(MailItem item) {
        // Add a new item to list if there's less than 3 items
        if(numCurrHolding < HOLD_LIMIT){
            items[numCurrHolding] = item;
            numCurrHolding += 1;
        }
    }

    public void removeItem() {
        items[numCurrHolding - 1] = null;
        numCurrHolding = numCurrHolding - 1;
    }

    public MailItem getItem() {
        return items[numCurrHolding - 1];
    }

    public int getNumHolding() {
        return numCurrHolding;
    }

}
