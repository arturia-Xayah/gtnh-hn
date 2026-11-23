package dev.aeoptimizer.dispatch;

/** A transaction that can defer emergency overflow until every ordinary destination was tried. */
public interface OverflowRoute {

    boolean modernization$overflowEnabled();

    boolean modernization$normalSpace();
}
