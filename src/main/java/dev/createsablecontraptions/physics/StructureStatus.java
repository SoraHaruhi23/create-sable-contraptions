package dev.createsablecontraptions.physics;

/** Small synchronized status code. No world references or diagnostic strings travel in metadata. */
public final class StructureStatus {
    public static final int UNKNOWN=0,MOVING=1,IDLE=2,COLLISION=3,DRILL=4,DEPLOYER=5,TRANSFER=6,ACTOR=7,STALLED=8;
    public static final int UNLOADED=9,BUDGET=10,BOUNDARY=11,UNAVAILABLE=12;
    private StructureStatus() {}
    public static int choose(boolean blocked,int actor,boolean stalled,boolean moving) {
        if(actor==DRILL || actor==DEPLOYER || actor==TRANSFER)return actor;
        if(blocked)return COLLISION;
        if(actor==ACTOR)return ACTOR;
        if(stalled)return STALLED;
        return moving?MOVING:IDLE;
    }
    public static String stateKey(int code) {
        return code==MOVING?"moving":code==IDLE?"idle":code>=COLLISION && code<=UNAVAILABLE?"stopped":"waiting";
    }
    public static String reasonKey(int code) {
        return switch(code) {
            case COLLISION->"collision";case DRILL->"drill";case DEPLOYER->"deployer";
            case TRANSFER->"transfer";case ACTOR->"actor";case STALLED->"unspecified";
            case UNLOADED->"unloaded";case BUDGET->"budget";case BOUNDARY->"boundary";case UNAVAILABLE->"unavailable";default->"";
        };
    }
}
