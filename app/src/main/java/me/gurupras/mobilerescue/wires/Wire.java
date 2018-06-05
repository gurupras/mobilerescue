package me.gurupras.mobilerescue.wires;

import java.util.HashSet;

public abstract class Wire implements IWire {
    protected HashSet<IWireEvents> listeners;

    public Wire() {
        listeners = new HashSet<IWireEvents>();
    }

    public void addListener(IWireEvents listener) {
        this.listeners.add(listener);
    }

    public void removeListener(IWireEvents listener) {
        this.listeners.remove(listener);
    }
}
