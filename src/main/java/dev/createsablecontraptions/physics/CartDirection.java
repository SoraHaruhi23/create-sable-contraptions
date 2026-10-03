package dev.createsablecontraptions.physics;

public final class CartDirection {
    private CartDirection() {}
    public static boolean reversed(double x,double y,double z,double oldX,double oldY,double oldZ) {
        return x*x+y*y+z*z>1e-16 && x*oldX+y*oldY+z*oldZ<0;
    }
}
