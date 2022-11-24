// Group 7
package automail;

public interface Holdable {

    void addItem(MailItem item);
    MailItem getItem();
    void removeItem();
    int getNumHolding();

}
