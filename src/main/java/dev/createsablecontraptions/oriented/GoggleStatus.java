package dev.createsablecontraptions.oriented;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import dev.createsablecontraptions.elevator.*;
import dev.createsablecontraptions.physics.StructureStatus;

public interface GoggleStatus {
    int csc$status();
    java.util.Optional<net.minecraft.core.BlockPos> csc$collisionTarget();
    static AbstractContraptionEntity root(AbstractContraptionEntity e) {
        while(e.getVehicle() instanceof AbstractContraptionEntity parent && ElevatorLink.managed(parent.getContraption()))e=parent;
        return e;
    }
    static int compute(AbstractContraptionEntity entity) {
        var root=entity;
        while(root.getVehicle() instanceof AbstractContraptionEntity parent && ElevatorLink.managed(parent.getContraption()))root=parent;
        var sub=ElevatorActors.sub(root);
        boolean moving=sub instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel server
                && (server.latestLinearVelocity.lengthSquared()>1e-8 || server.latestAngularVelocity.lengthSquared()>1e-8);
        int status=StructureStatus.choose(blocked(root),actor(root),root.isStalled(),moving);
        var hit=dev.createsablecontraptions.physics.CollisionState.get(root);
        if(status==StructureStatus.COLLISION)return hit==null?StructureStatus.UNAVAILABLE:hit.reason();
        return status;
    }
    private static boolean blocked(AbstractContraptionEntity e) {
        if(((ElevatorLink)e.getContraption()).csc$isBlocked())return true;
        for(var p:e.getPassengers())if(p instanceof AbstractContraptionEntity child && ElevatorLink.managed(child.getContraption()) && blocked(child))return true;
        return false;
    }
    private static int actor(AbstractContraptionEntity e) {
        int result=0;
        for(var pair:e.getContraption().getActors()) {
            if(pair.right==null || !pair.right.stall || pair.right.disabled)continue;
            if(MovementBehaviour.REGISTRY.get(pair.left.state()) instanceof PortableStorageInterfaceMovement)return StructureStatus.TRANSFER;
            if(AllBlocks.MECHANICAL_DRILL.has(pair.left.state()))result=StructureStatus.DRILL;
            else if(AllBlocks.DEPLOYER.has(pair.left.state()) && result!=StructureStatus.DRILL)result=StructureStatus.DEPLOYER;
            else if(result==0)result=StructureStatus.ACTOR;
        }
        for(var p:e.getPassengers())if(p instanceof AbstractContraptionEntity child && ElevatorLink.managed(child.getContraption())) {
            int childResult=actor(child);
            if(childResult==StructureStatus.TRANSFER)return childResult;
            if(result==0 || result==StructureStatus.ACTOR)result=childResult==0?result:childResult;
        }
        return result;
    }
}
