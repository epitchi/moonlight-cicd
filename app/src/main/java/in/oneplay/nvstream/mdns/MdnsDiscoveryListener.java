package in.oneplay.nvstream.mdns;

public interface MdnsDiscoveryListener {
    void notifyComputerAdded(MdnsComputer computer);
    void notifyComputerRemoved(MdnsComputer computer);
    void notifyDiscoveryFailure(Exception e);
}
