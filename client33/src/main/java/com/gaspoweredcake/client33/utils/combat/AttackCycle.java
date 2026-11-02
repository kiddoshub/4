package com.gaspoweredcake.client33.utils.combat;

/** Invalidates queued work and reserves attacks before callbacks can reenter. */
public final class AttackCycle {
    private long generation;
    private boolean consumed;

    public long begin() { consumed = false; return ++generation; }
    public void invalidate() { generation++; consumed = true; }
    public boolean valid(long token) { return token == generation; }
    public boolean claim(long token, boolean multiple) {
        if (!valid(token) || (consumed && !multiple)) return false;
        consumed = true;
        return true;
    }
}
