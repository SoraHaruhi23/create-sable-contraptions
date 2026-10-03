package dev.createsablecontraptions.oriented;

public final class ChildAssemblyScope {
    private static final ThreadLocal<java.util.ArrayDeque<Boolean>> PARENTS = ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private ChildAssemblyScope() {}
    public static void push(boolean managed) { PARENTS.get().push(managed); }
    public static void pop() { var stack = PARENTS.get(); stack.pop(); if (stack.isEmpty()) PARENTS.remove(); }
    public static boolean physicalParent() { return Boolean.TRUE.equals(PARENTS.get().peek()); }
}
