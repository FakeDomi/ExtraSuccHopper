package re.domi.extrasucchopper;

public interface HopperAccess
{
    int getItemPickupCooldown();
    void setItemPickupCooldown(int cooldown);
    int getTransferCooldown();
    void setTransferCooldown(int cooldown);
    boolean getIsFull();
    void setLastTickTime(long timestamp);
}
