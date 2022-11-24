// Group 7
package automail;

public class Hands implements Holdable {

    private int numCurrHolding;
    private MailItem item;
    public Hands(){
        numCurrHolding = 0;
    }

    public void addItem(MailItem item){
        numCurrHolding = 1;
        this.item = item;
    }

    public void removeItem(){
        numCurrHolding = 0;
        this.item = null;
    }

    public MailItem getItem(){
        return item;
    }

    public int getNumHolding(){
        return numCurrHolding;
    }


}
