package edu.yale.network;

import edu.yale.network.Util.Monitor;

public class DummyMonitor implements Monitor {
    public boolean overload() {
        return false;
    }
}
