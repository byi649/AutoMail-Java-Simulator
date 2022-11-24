// Group 7
package automail;

public class MailTube implements Holdable {

    private int numCurrHolding;
    private MailItem item;

    public MailTube(){
        this.numCurrHolding = 0;
    }

    public void addItem(MailItem item) {
        this.item = item;
        numCurrHolding = 1;
    }

    public void removeItem() {
        this.item = null;
        numCurrHolding = 0;
    }

    public int getNumHolding() {
        return numCurrHolding;
    }

    public MailItem getItem() {
        return item;
    }


}
