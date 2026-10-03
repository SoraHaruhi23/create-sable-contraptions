package dev.createsablecontraptions.compat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarInputStream;
import static org.junit.jupiter.api.Assertions.*;

/** Checks pinned dependency bytecode without loading Minecraft, Mixins or native physics. */
class MixinTargetsTest {
    @Test void physicalTanksBypassActorTickCancellationIncludingCreativeTanks() throws Exception {
        String tank="com/simibubi/create/content/fluids/tank/FluidTankBlockEntity";
        var hook=compiled("mixin/PhysicalActorTickerMixin").methods.stream()
                .filter(m->m.name.equals("csc$oneWorkExecutor")).findFirst().orElseThrow();
        var instructions=hook.instructions;
        boolean checked=false;
        for(var instruction:instructions) {
            if (!(instruction instanceof org.objectweb.asm.tree.TypeInsnNode type)
                    || type.getOpcode()!=org.objectweb.asm.Opcodes.INSTANCEOF || !type.desc.equals(tank)) continue;
            var next=instruction.getNext();
            while(next!=null && next.getOpcode()<0)next=next.getNext();
            assertInstanceOf(org.objectweb.asm.tree.JumpInsnNode.class,next);
            var branch=(org.objectweb.asm.tree.JumpInsnNode)next;
            assertEquals(org.objectweb.asm.Opcodes.IFNE,branch.getOpcode());
            assertTrue(instructions.indexOf(branch.label)>callIndex(hook,"cancel"),
                    "Tank path must skip cancellation, not merely mention its type");
            checked=true;
        }
        assertTrue(checked);
        assertEquals(tank,dependency("com/simibubi/create/content/fluids/tank/CreativeFluidTankBlockEntity").superName);
        var tick=dependency(tank).methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        for(var name:List.of("sendData","onPositionChanged","refreshCapability","updateConnectivity"))
            assertTrue(callIndex(tick,name)>=0,"Real tank maintenance requires tick: "+name);
    }

    @Test void movingFluidOperationsUseLivePhysicalCapabilitiesInsteadOfTankSnapshots() throws Exception {
        var view=compiled("elevator/PhysicalStorage$FluidView");
        for(var method:view.methods) {
            if(!List.of("fill","drain","getFluidInTank","getTankCapacity","getTanks","isFluidValid").contains(method.name))continue;
            assertTrue(callIndex(method,"cap")>=0,method.name);
            assertTrue(callIndex(method,"cap")<callIndex(method,method.name),method.name);
        }
        var cap=view.methods.stream().filter(m->m.name.equals("cap")).findFirst().orElseThrow();
        assertTrue(callIndex(cap,"fluid")>=0);
        var storage=compiled("elevator/PhysicalStorage");
        var fluid=storage.methods.stream().filter(m->m.name.equals("fluid")).findFirst().orElseThrow();
        assertTrue(callIndex(fluid,"physical")>=0 && callIndex(fluid,"getCapability")>callIndex(fluid,"physical"));
        var connection=dependency("com/simibubi/create/content/contraptions/actors/psi/PortableFluidInterfaceBlockEntity")
                .methods.stream().filter(m->m.name.equals("startTransferringTo")).findFirst().orElseThrow();
        assertTrue(callIndex(connection,"getStorage")>=0 && callIndex(connection,"getFluids")>callIndex(connection,"getStorage"));
    }
    @Test void sourceProtectionGuardsNativeSelectionSavedProgressAndFinalDestruction() throws Exception {
        var breaker=compiled("mixin/BlockBreakingMovementMixin");
        for(var name:List.of("csc$excludeOwnPlot","csc$discardSavedSelfTarget","csc$protectPlatform")) {
            var method=breaker.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
            assertTrue(callIndex(method,"allowedTarget")>=0 && callIndex(method,"cancel")>callIndex(method,"allowedTarget"),name);
        }
        var drill=compiled("linear/CollisionDrilling");
        for(var name:List.of("offer","targetInReach","allowedTarget")) {
            var method=drill.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
            assertTrue(callIndex(method,"protects")>=0,name);
            if(name.equals("offer")) assertTrue(callIndex(method,"protects")<callIndex(method,"canBreak"));
        }
    }
    @Test void sourceProtectionIsRestrictedToManagedFamiliesAndUsesPersistentControllers() throws Exception {
        var method=compiled("linear/AssemblySourceProtection").methods.stream().filter(m->m.name.equals("protects")).findFirst().orElseThrow();
        assertTrue(callIndex(method,"managed")>=0 && callIndex(method,"managed")<callIndex(method,"getContaining"));
        assertTrue(callIndex(method,"related")>=0 && callIndex(method,"csc$controllerPos")>=0 && callIndex(method,"getVehicle")>=0);
        var types=new java.util.HashSet<String>();
        for(var instruction:method.instructions) if(instruction instanceof org.objectweb.asm.tree.TypeInsnNode t
                && t.getOpcode()==org.objectweb.asm.Opcodes.INSTANCEOF)types.add(t.desc);
        assertTrue(types.contains("com/simibubi/create/content/contraptions/mounted/CartAssemblerBlock"));
        assertTrue(types.contains("com/simibubi/create/content/contraptions/mounted/MountedContraption"));
        assertTrue(types.contains("com/simibubi/create/content/contraptions/gantry/GantryContraption"));
        assertEquals(-1,callIndex(method,"getChunk"),"Protection must not force-load remote source chunks");
    }
    @Test void invalidSavedBreakerTargetsClearRetryStateAlongBothEntryPaths() throws Exception {
        for(var path:List.of("mixin/BlockBreakingMovementMixin","mixin/CollisionDrillProgressMixin")) {
            var method=compiled(path).methods.stream().filter(m->m.name.equals("csc$discardSavedSelfTarget")
                    || m.name.equals("csc$releaseDistantTarget")).findFirst().orElseThrow();
            assertTrue(callIndex(method,"cancelStall")>=0);
            var keys=new java.util.HashSet<Object>();
            for(var instruction:method.instructions)if(instruction instanceof org.objectweb.asm.tree.LdcInsnNode literal)keys.add(literal.cst);
            assertTrue(keys.containsAll(List.of("WaitingTicks","LastPos","ProjectedPos","CSCCollisionDrill")),path);
        }
    }
    @Test void restoredRootJoinAndNestedChildrenShareOneCommitBoundary() throws Exception {
        var packed=compiled("oriented/PackedStructures");
        var family=packed.methods.stream().filter(m->m.name.equals("restoreFamily")).findFirst().orElseThrow();
        assertTrue(callIndex(family,"run")>=0 && callIndex(family,"commit")>callIndex(family,"run"));
        assertTrue(callIndex(family,"rollback")>=0);
        var body=packed.methods.stream().filter(m->m.name.equals("restoreBody")).findFirst().orElseThrow();
        assertTrue(callIndex(body,"undo")<callIndex(body,"allocateNewSubLevel"));
        assertTrue(callIndex(body,"onCommit")>=0);
        assertTrue(java.util.Arrays.stream(body.instructions.toArray()).noneMatch(i -> i instanceof MethodInsnNode m
                && m.name.equals("csc$packed") && Type.getArgumentTypes(m.desc).length>0));
        var item=compiled("mixin/MinecartItemMixin");
        var hook=item.methods.stream().filter(m->m.name.equals("csc$timePlacement")).findFirst().orElseThrow();
        assertTrue(callIndex(hook,"restoreFamily")>=0);
        var original=dependency("com/simibubi/create/content/contraptions/mounted/MinecartContraptionItem").methods.stream()
                .filter(m->m.name.equals("addContraptionToMinecart")).findFirst().orElseThrow();
        assertEquals(1,java.util.Arrays.stream(original.instructions.toArray()).filter(i->i instanceof MethodInsnNode m
                && m.owner.equals("net/minecraft/world/level/Level") && m.name.equals("addFreshEntity")
                && m.desc.equals("(Lnet/minecraft/world/entity/Entity;)Z")).count());
    }
    @Test void orientationIsCheckedBeforeActorsAndDoesNotSkipStalledWork() throws Exception {
        var sweep=compiled("oriented/ChildMotion").methods.stream().filter(m->m.name.equals("cartTurn")).findFirst().orElseThrow();
        assertTrue(callIndex(sweep,"getAnchorVec")>=0,"Use the same anchor as Create's transform, not the entity position");
        var tick=dependency("com/simibubi/create/content/contraptions/OrientedContraptionEntity").methods.stream()
                .filter(m->m.name.equals("tickContraption")).findFirst().orElseThrow();
        assertTrue(callIndex(tick,"updateOrientation")>=0 && callIndex(tick,"updateOrientation")<callIndex(tick,"tickActors"));
        var hook=compiled("mixin/OrientedEntityMixin").methods.stream().filter(m->m.name.equals("csc$turnSweep")).findFirst().orElseThrow();
        assertTrue(callIndex(hook,"check")<callIndex(hook,"setReturnValue"));
        var valueOf=hook.instructions.get(callIndex(hook,"valueOf"));
        assertEquals(org.objectweb.asm.Opcodes.ICONST_0,valueOf.getPrevious().getOpcode());
    }
    @Test void collisionGameplayStateDoesNotDependOnDiagnosticsConfig() throws Exception {
        for(var path:List.of("oriented/CartStatus","bearing/BearingStatus")) {
            var contact=compiled(path).methods.stream().filter(m->m.name.equals("contact")).findFirst().orElseThrow();
            int state=-1,config=Integer.MAX_VALUE;
            for(int i=0;i<contact.instructions.size();i++) {
                var instruction=contact.instructions.get(i);
                if(instruction instanceof MethodInsnNode m && m.owner.endsWith("/CollisionState") && m.name.equals("contact"))state=i;
                if(instruction instanceof FieldInsnNode f && f.name.equals("DIAGNOSTICS"))config=i;
            }
            assertTrue(state>=0 && state<config,path);
        }
        var sync=compiled("mixin/GoggleStatusMixin").methods.stream().filter(m->m.name.equals("csc$updateStatus")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(sync.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f && f.name.equals("CSC_TARGET")));
    }
    @Test void nativeAssemblyAdvancementAwardsRemainAvailable() throws Exception {
        for(var path:List.of("bearing/MechanicalBearingBlockEntity","bearing/ClockworkBearingBlockEntity",
                "pulley/PulleyBlockEntity","elevator/ElevatorPulleyBlockEntity","piston/MechanicalPistonBlockEntity")) {
            var clazz=dependency("com/simibubi/create/content/contraptions/"+path);
            assertTrue(clazz.methods.stream().anyMatch(m -> callIndex(m,"award")>=0),path);
        }
        // The bridges replace block movement, not the enclosing control blocks' award calls.
        for(var path:List.of("MechanicalBearingMixin","ClockworkBearingMixin","PulleyMixin","ElevatorPulleyMixin","MechanicalPistonMixin"))
            for(var method:compiled("mixin/"+path).methods) {
                var inject=annotation(method.visibleAnnotations,method.invisibleAnnotations,BASE+"injection/Inject");
                if(inject!=null && ((List<?>)value(inject,"method")).contains("assemble"))
                    assertNotEquals(Boolean.TRUE,value(inject,"cancellable"));
            }
    }
    @Test void failedAssemblyCleansItsPublishedTicketAndInventoryCollectionAvoidsDuplicateLookup() throws Exception {
        var assembly=compiled("elevator/ElevatorBridge").methods.stream().filter(m->m.name.equals("assemble")).findFirst().orElseThrow();
        assertTrue(callIndex(assembly,"removeForceLoadTicket")>=0);
        var storage=compiled("elevator/PhysicalStorage");
        var items=storage.methods.stream().filter(m->m.name.equals("getAllItemStorages")).findFirst().orElseThrow();
        assertTrue(callIndex(items,"itemView")>=0);
        assertEquals(-1,callIndex(items,"itemAt"));
        var fluids=storage.methods.stream().filter(m->m.name.equals("getFluids")).findFirst().orElseThrow();
        assertTrue(callIndex(fluids,"fluidView")>=0);
        assertEquals(-1,callIndex(fluids,"fluidAt"));
    }
    @Test void configScreenIsClientOnlyAndGameplaySettingsAreServerOwned() throws Exception {
        var client = compiled("client/CscClient");
        var mod = annotation(client.visibleAnnotations,client.invisibleAnnotations,"net/neoforged/fml/common/Mod");
        assertNotNull(mod);
        assertNotNull(value(mod,"dist"));
        var constructor = client.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        assertTrue(callIndex(constructor,"registerExtensionPoint")>=0);
        var main = compiled("CreateSableContraptions");
        var init = main.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow();
        assertEquals(2,java.util.Arrays.stream(init.instructions.toArray()).filter(i -> i instanceof MethodInsnNode m && m.name.equals("registerConfig")).count());
        assertTrue(java.util.Arrays.stream(init.instructions.toArray()).anyMatch(i -> i instanceof FieldInsnNode f && f.name.equals("SERVER")));
        for (var method : main.methods) for (var i : method.instructions)
            if (i instanceof MethodInsnNode m) assertFalse(m.owner.startsWith("net/minecraft/client/"));
    }
    @Test void bearingDiagnosticsRecordBothKindsOfContactWithoutResweepingOnQuery() throws Exception {
        var motion = compiled("bearing/BearingMotion");
        var check = motion.methods.stream().filter(m -> m.name.equals("check")).findFirst().orElseThrow();
        assertTrue(callIndex(check,"begin") < callIndex(check,"blocked"));
        var scan = motion.methods.stream().filter(m -> m.name.equals("blocked")).findFirst().orElseThrow();
        assertEquals(2, java.util.Arrays.stream(scan.instructions.toArray()).filter(i -> i instanceof MethodInsnNode m
                && m.owner.endsWith("/BearingStatus") && m.name.equals("contact")).count());
        for (var method : compiled("bearing/BearingStatus").methods) {
            if (!method.name.startsWith("lambda$register") && !method.name.equals("register")) continue;
            assertEquals(-1,callIndex(method,"blocked"));
            assertEquals(-1,callIndex(method,"check"));
        }
    }
    @Test void assemblerProtectionUsesPersistentOwnershipAndGuardsDirectActivation() throws Exception {
        var protection = compiled("elevator/AssemblerProtection");
        var managed = protection.methods.stream().filter(m -> m.name.equals("managed")).findFirst().orElseThrow();
        assertTrue(callIndex(managed, "getContaining") >= 0);
        assertTrue(callIndex(managed, "managed") > callIndex(managed, "getContaining"));
        var interact = protection.methods.stream().filter(m -> m.name.equals("interact")).findFirst().orElseThrow();
        assertTrue(callIndex(interact, "managed") < callIndex(interact, "setCanceled"));
        var mixin = compiled("mixin/PhysicsAssemblerProtectionMixin");
        assertNotNull(annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, BASE + "Pseudo"));
        var hook = mixin.methods.stream().filter(m -> m.name.equals("csc$protectStructure")).findFirst().orElseThrow();
        assertTrue(callIndex(hook, "managed") >= 0 && callIndex(hook, "cancel") > callIndex(hook, "managed"));
        var inject = annotation(hook.visibleAnnotations, hook.invisibleAnnotations, BASE + "injection/Inject");
        assertEquals(List.of("assembleOrDisassemble()V"), value(inject, "method"));
        assertEquals(Boolean.TRUE, value(inject, "cancellable"));
    }
    @Test void successfulCartSweepReleasesPredictionButKeepsActorStalls() throws Exception {
        var end=compiled("oriented/CartMotion").methods.stream().filter(m->m.name.equals("end")).findFirst().orElseThrow();
        assertTrue(callIndex(end,"cart")>=0 && callIndex(end,"csc$setBlocked")>callIndex(end,"cart"));
        assertTrue(callIndex(end,"actorStalled")>callIndex(end,"csc$setBlocked"));
        assertTrue(callIndex(end,"accepted")>callIndex(end,"actorStalled"));
        var tick=compiled("oriented/CartDocking").methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        assertTrue(callIndex(tick,"reversed")>=0 && callIndex(tick,"reversed")<callIndex(tick,"cart"));
    }
    @Test void gogglesTitleReservesNativeIconIndentAndDeduplicatesNestedComponents() throws Exception {
        var clazz=compiled("client/StructureGoggles");
        var append=clazz.methods.stream().filter(m->m.name.equals("append")).findFirst().orElseThrow();
        long nativeLines=java.util.Arrays.stream(append.instructions.toArray()).filter(i->i instanceof MethodInsnNode m && m.name.equals("forGoggles")).count();
        assertEquals(3,nativeLines);
        var title=clazz.methods.stream().filter(m->m.name.equals("hasTitle")).findFirst().orElseThrow();
        assertTrue(callIndex(title,"getSiblings")>=0);
    }
    @Test void gogglesUseSyncedStatusAndNativeTooltipProviders() throws Exception {
        var overlay=compiled("mixin/client/StructureGogglesMixin");
        for(String name:List.of("csc$goggles","csc$hover","csc$plainBlock")) {
            var hook=overlay.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
            assertTrue(callIndex(hook,"call")>=0 && callIndex(hook,"append")>callIndex(hook,"call"));
        }
        var tooltip=compiled("client/StructureGoggles").methods.stream().filter(m->m.name.equals("append")).findFirst().orElseThrow();
        assertTrue(callIndex(tooltip,"isWearingGoggles")>=0 && callIndex(tooltip,"managed")>=0);
        assertTrue(callIndex(tooltip,"csc$status")>=0 && callIndex(tooltip,"forGoggles")>=0);
        assertEquals(-1,callIndex(tooltip,"compute"));
        var sync=compiled("mixin/GoggleStatusMixin").methods.stream().filter(m->m.name.equals("csc$updateStatus")).findFirst().orElseThrow();
        assertTrue(callIndex(sync,"compute")>=0 && callIndex(sync,"set")>callIndex(sync,"compute"));
    }
    @Test void packedRestoreWaitsForFinalPlacementAndRecomputesRotationBeforePhysics() throws Exception {
        var early=compiled("mixin/ChildAssemblyMixin").methods.stream().filter(m->m.name.equals("csc$createChildren")).findFirst().orElseThrow();
        assertEquals(-1,callIndex(early,"restore"));
        var join=compiled("oriented/PackedStructures").methods.stream().filter(m->m.name.equals("onJoin")).findFirst().orElseThrow();
        assertTrue(callIndex(join,"restore")>=0);
        var restore=compiled("oriented/PackedStructures").methods.stream().filter(m->m.name.equals("restoreBody")).findFirst().orElseThrow();
        assertTrue(callIndex(restore,"positionRider")<callIndex(restore,"allocateNewSubLevel"));
        assertTrue(callIndex(restore,"target")>callIndex(restore,"csc$setSubLevel"));
        assertTrue(callIndex(restore,"target")<callIndex(restore,"hold"));
        assertTrue(callIndex(restore,"updateLastPose")>callIndex(restore,"hold"));
        assertTrue(callIndex(restore,"updateLastPose")<callIndex(restore,"addForceLoadTicket"));
    }
    @Test void destinationReplacementHonoursHardnessShapesAndNativeDropConfiguration() throws Exception {
        var placement=compiled("bearing/DisassemblyPlacement");
        var check=placement.methods.stream().filter(m->m.name.equals("canReplace")).findFirst().orElseThrow();
        assertTrue(callIndex(check,"getDestroySpeed")>=0 && callIndex(check,"getCollisionShape")>=0);
        var clear=placement.methods.stream().filter(m->m.name.equals("clear")).findFirst().orElseThrow();
        assertTrue(callIndex(clear,"canReplace")<callIndex(clear,"destroyBlock"));
        assertTrue(java.util.Arrays.stream(clear.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f
                && f.name.equals("noDropWhenContraptionReplaceBlocks")));
        var disassemble=compiled("bearing/BearingBridge").methods.stream().filter(m->m.name.equals("disassemble")).findFirst().orElseThrow();
        assertTrue(callIndex(disassemble,"canDisassemble")<callIndex(disassemble,"clear"));
        assertTrue(callIndex(disassemble,"clear")<callIndex(disassemble,"moveBlocks"));
    }
    @Test void assemblerCanFindPhysicalStructureBehindAPlayerPassenger() throws Exception {
        var hook=compiled("mixin/CartAssemblerMixin").methods.stream().filter(m->m.name.equals("csc$findPhysicalPassenger")).findFirst().orElseThrow();
        assertTrue(callIndex(hook,"managed")>=0 && callIndex(hook,"call")>callIndex(hook,"managed"));
        assertTrue(java.util.Arrays.stream(hook.instructions.toArray()).anyMatch(i->i instanceof org.objectweb.asm.tree.TypeInsnNode t
                && t.desc.endsWith("/mounted/MountedContraption")));
        var original=dependency("com/simibubi/create/content/contraptions/mounted/CartAssemblerBlockEntity").methods.stream()
                .filter(m->m.name.equals("disassemble")).findFirst().orElseThrow();
        assertTrue(callIndex(original,"get")>=0);
    }
    @Test void blockedDisassemblyStillChecksOccupancyAndRecordsReasons() throws Exception {
        var check=compiled("bearing/BearingBridge").methods.stream().filter(m->m.name.equals("canDisassemble")).findFirst().orElseThrow();
        assertTrue(callIndex(check,"canReplace")>=0 && callIndex(check,"record")>=0);
        for(var method:compiled("oriented/DisassemblyStatus").methods)
            for(var forbidden:List.of("setBlock","destroyBlock","canDisassemble"))assertEquals(-1,callIndex(method,forbidden));
    }
    @Test void minecartSelfCollisionExcludesOnlyItsPhysicalFamily() throws Exception {
        var hook=compiled("mixin/CartSelfCollisionMixin").methods.stream().filter(m->m.name.equals("csc$excludeCarriedBody")).findFirst().orElseThrow();
        assertTrue(callIndex(hook,"structure")>=0);
        assertTrue(callIndex(hook,"getContaining")>callIndex(hook,"structure"));
        assertTrue(callIndex(hook,"related")>callIndex(hook,"getContaining"));
        assertTrue(callIndex(hook,"empty")>callIndex(hook,"related"));
        assertTrue(java.util.Arrays.stream(hook.instructions.toArray()).anyMatch(i->i instanceof org.objectweb.asm.tree.TypeInsnNode t
                && t.desc.equals("net/minecraft/world/entity/vehicle/AbstractMinecart")));
    }
    @Test void placementTimingWrapsWholeCallAndKeepsInventoryLoadInPlace() throws Exception {
        var hook=compiled("mixin/MinecartItemMixin").methods.stream().filter(m->m.name.equals("csc$timePlacement")).findFirst().orElseThrow();
        assertTrue(callIndex(hook,"begin")<callIndex(hook,"restoreFamily"));
        assertTrue(callIndex(hook,"end")>callIndex(hook,"restoreFamily"));
        assertTrue(hook.tryCatchBlocks.stream().anyMatch(b->b.type==null));
        var restore=compiled("oriented/PackedStructures").methods.stream().filter(m->m.name.equals("restoreBody")).findFirst().orElseThrow();
        assertTrue(callIndex(restore,"loadWithComponents")>=0 && callIndex(restore,"phase")>=0);
        for(var method:compiled("oriented/PlacementTrace").methods)
            for(var forbidden:List.of("copy","loadWithComponents","saveWithFullMetadata","setBlock"))
                assertEquals(-1,callIndex(method,forbidden));
    }
    @Test void cartMountCorrectionIsSharedAndRestrictedToPhysicalMinecarts() throws Exception {
        var method=compiled("mixin/OrientedEntityMixin").methods.stream().filter(m->m.name.equals("csc$gridAlignedCartAttachment")).findFirst().orElseThrow();
        assertTrue(callIndex(method,"managed")>=0);
        assertTrue(callIndex(method,"setReturnValue")>callIndex(method,"managed"));
        assertTrue(java.util.Arrays.stream(method.instructions.toArray()).anyMatch(i->i instanceof org.objectweb.asm.tree.TypeInsnNode t
                && t.desc.endsWith("/mounted/MountedContraption")));
    }
    @Test void packedRestoreAvoidsWholeSnapshotCopyButIsolatesLoadedData() throws Exception {
        var read=compiled("mixin/PhysicalContraptionMixin").methods.stream().filter(m->m.name.equals("csc$read")).findFirst().orElseThrow();
        assertEquals(-1,callIndex(read,"copy"));
        var restore=compiled("oriented/PackedStructures").methods.stream().filter(m->m.name.equals("restoreBody")).findFirst().orElseThrow();
        assertTrue(callIndex(restore,"copy")>=0 && callIndex(restore,"copy")<callIndex(restore,"loadWithComponents"));
        assertTrue(callIndex(restore,"logicalCopy")>=0);
        assertEquals(-1,callIndex(restore,"sendBlockUpdated"));
        assertTrue(callIndex(restore,"setBlock")>=0 && callIndex(restore,"setChanged")>=0);
    }
    @Test void railSweepUsesProjectedTrackPoseBeforeTestingTheBody() throws Exception {
        var reject=compiled("oriented/CartMotion").methods.stream().filter(m->m.name.equals("reject")).findFirst().orElseThrow();
        assertTrue(callIndex(reject,"getPos")>=0 && callIndex(reject,"getPos")<callIndex(reject,"cart"));
        assertTrue(reject.tryCatchBlocks.stream().anyMatch(b->b.type==null));
        var hook=compiled("mixin/MinecartMotionMixin").methods.stream().filter(m->m.name.equals("csc$railMovementFrame")).findFirst().orElseThrow();
        assertTrue(callIndex(hook,"enterRailMove")<callIndex(hook,"call"));
        assertTrue(callIndex(hook,"leaveRailMove")>callIndex(hook,"call"));
        assertTrue(hook.tryCatchBlocks.stream().anyMatch(b->b.type==null));
    }
    private static final String BASE = "org/spongepowered/asm/mixin/";
    private static final java.util.Map<String, ClassNode> CLASSES = new java.util.HashMap<>();

    @Test void cartDiagnosticReadsRecordedChecksWithoutRunningActorsOrEditingTheWorld() throws Exception {
        for(var method:compiled("oriented/CartStatus").methods) {
            for(String mutation:List.of("setBlock","removeBlock","destroyBlock","setDeltaMovement","setPos",
                    "startTransferringTo","visitNewPosition","getInventory","extractItem","insertItem","offer"))
                assertEquals(-1,callIndex(method,mutation),"Diagnostic must be read-only: "+mutation);
            for(var instruction:method.instructions)
                if(instruction instanceof MethodInsnNode call)assertNotEquals("dev/createsablecontraptions/oriented/ChildMotion",call.owner);
        }
    }
    @Test void collisionDiagnosticRecordsActualContactBeforeOfferingItToTheDrill() throws Exception {
        var hit=compiled("oriented/ChildMotion").methods.stream().filter(m->m.name.equals("hit")).findFirst().orElseThrow();
        assertTrue(callIndex(hit,"blocked")>=0 && callIndex(hit,"contact")>callIndex(hit,"blocked"));
        assertTrue(callIndex(hit,"offer")>callIndex(hit,"contact"));
        var query=compiled("oriented/ChildMotion").methods.stream().filter(m->callIndex(m,"collectCartBody")>=0).findFirst().orElseThrow();
        assertTrue(callIndex(query,"begin")>=0);
        assertTrue(callIndex(query,"fail")>=0);
    }

    @Test void cartMoveIsCheckedBeforeVanillaAndIncludesTheRoot() throws Exception {
        var hook=compiled("mixin/CartMoveMixin").methods.stream().filter(m->m.name.equals("csc$wholeStructureSweep")).findFirst().orElseThrow();
        var inject=annotation(hook.visibleAnnotations,hook.invisibleAnnotations,BASE+"injection/Inject");
        var at=(List<?>)value(inject,"at");
        assertEquals("HEAD",value((AnnotationNode)at.getFirst(),"value"));
        assertTrue(callIndex(hook,"reject")>=0 && callIndex(hook,"cancel")>callIndex(hook,"reject"));
        var query=compiled("oriented/ChildMotion").methods.stream().filter(m->callIndex(m,"collectCartBody")>=0).findFirst().orElseThrow();
        assertTrue(callIndex(query,"collect")>=0);
        var end=compiled("oriented/CartMotion").methods.stream().filter(m->m.name.equals("end")).findFirst().orElseThrow();
        assertTrue(callIndex(end,"cart")>=0);
        assertTrue(java.util.Arrays.stream(end.instructions.toArray()).skip(callIndex(end,"cart")+1)
                .anyMatch(i->i instanceof MethodInsnNode c && c.name.equals("setPos")));
    }
    @Test void assemblerUsesItsNativeMinecartCollisionException() throws Exception {
        var query=compiled("oriented/ChildMotion").methods.stream().filter(m->callIndex(m,"collectCartBody")>=0).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(query.instructions.toArray()).anyMatch(i->i instanceof org.objectweb.asm.tree.TypeInsnNode t
                && t.desc.endsWith("/mounted/CartAssemblerBlock")));
        assertTrue(java.util.Arrays.stream(query.instructions.toArray()).anyMatch(i->i instanceof MethodInsnNode c
                && c.owner.equals("net/minecraft/world/phys/shapes/CollisionContext") && c.name.equals("of")));
        var upstream=dependency("com/simibubi/create/content/contraptions/mounted/CartAssemblerBlock");
        var collision=upstream.methods.stream().filter(m->m.name.equals("getCollisionShape")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(collision.instructions.toArray()).anyMatch(i->i instanceof org.objectweb.asm.tree.TypeInsnNode t
                && t.desc.equals("net/minecraft/world/entity/vehicle/AbstractMinecart")));
        assertTrue(callIndex(collision,"empty")>=0);
    }
    @Test void clientCollisionEntityUsesTheSamePoseAsItsCartModel() throws Exception {
        var client=compiled("client/CartClientSync");
        var tick=client.methods.stream().filter(m->m.name.equals("tick")).findFirst().orElseThrow();
        assertTrue(callIndex(tick,"structure")>=0);
        assertTrue(callIndex(tick,"setPos")>=0 && callIndex(tick,"positionRider")>callIndex(tick,"setPos"));
        var position=client.methods.stream().filter(m->m.name.equals("position")).findFirst().orElseThrow();
        assertTrue(callIndex(position,"renderPose")>=0);
        assertTrue(callIndex(position,"transformPosition")>=0);
    }

    @Test void destroyedCartReleasesLiveBodiesWithoutDeletingInventories() throws Exception {
        var method=compiled("oriented/CartLifecycle").methods.stream().filter(m->m.name.equals("release")).findFirst().orElseThrow();
        assertTrue(callIndex(method,"commit")>=0);
        assertTrue(callIndex(method,"release")>=0);
        assertTrue(callIndex(method,"removeForceLoadTicket")>=0);
        assertTrue(callIndex(method,"setUserDataTag")>=0);
        assertTrue(callIndex(method,"discard")>callIndex(method,"commit"));
        assertEquals(-1,callIndex(method,"removeSubLevel"));
        assertEquals(-1,callIndex(method,"setBlock"));
        assertTrue(callIndex(method,"csc$skipActorStop")>=0);
        var cleanup=compiled("oriented/CartLifecycle").methods.stream().filter(m->m.name.equals("removeVirtualAnchors")).findFirst().orElseThrow();
        assertTrue(callIndex(cleanup,"virtualAnchor")>=0 && callIndex(cleanup,"removeBlock")>callIndex(cleanup,"virtualAnchor"));
    }
    @Test void cartRenderAlignmentUsesTheBodyClockAndNeverMovesTheEntity() throws Exception {
        var method=compiled("client/CartRenderAlignment").methods.stream().filter(m->m.name.equals("offset")).findFirst().orElseThrow();
        assertTrue(callIndex(method,"renderPose")>=0);
        assertTrue(callIndex(method,"getPassengerRidingPosition")>=0);
        assertEquals(-1,callIndex(method,"setPos"));
        assertEquals(-1,callIndex(method,"setDeltaMovement"));
    }
    @Test void ridingRequiresAPlainCartWithOnlyProxyPassengers() throws Exception {
        var method=compiled("mixin/MinecartRidingMixin").methods.stream().filter(m->m.name.equals("csc$rideAlongsideProxy")).findFirst().orElseThrow();
        assertTrue(callIndex(method,"structure")>=0);
        assertTrue(callIndex(method,"isSecondaryUseActive")>=0);
        assertTrue(callIndex(method,"getPassengers")>=0);
        assertTrue(callIndex(method,"startRiding")>=0);
    }
    @Test void cartImpulseIsConsumedAtTheValidatedPhysicsEntry() throws Exception {
        var method=compiled("mixin/RapierPipelineMixin").methods.stream().filter(m->m.name.equals("csc$driveMinecart")).findFirst().orElseThrow();
        assertTrue(callIndex(method,"apply")>=0 && callIndex(method,"cancel")>callIndex(method,"apply"));
        var push=compiled("oriented/CartImpulse").methods.stream().filter(m->m.name.equals("push")).findFirst().orElseThrow();
        assertTrue(callIndex(push,"getRailDirection")>=0);
        assertTrue(callIndex(push,"dot")>=0);
        var packet=dependency("dev/ryanhcode/sable/network/packets/tcp/ServerboundPunchSubLevelPacket");
        assertTrue(packet.methods.stream().anyMatch(m->callIndex(m,"tryPunch")>=0));
    }

    @Test void childTargetsAreRefreshedBeforeAnyBodyIsMoved() throws Exception {
        var method = compiled("elevator/ElevatorPhysics").methods.stream().filter(m -> m.name.equals("beforePhysics")).findFirst().orElseThrow();
        assertTrue(callIndex(method,"children") >= 0 && callIndex(method,"children") < callIndex(method,"hold"));
        var target = compiled("oriented/OrientedBridge").methods.stream().filter(m -> m.name.equals("target")).findFirst().orElseThrow();
        assertTrue(callIndex(target,"getBearingPosOf") >= 0);
        assertTrue(callIndex(target,"toGlobalVector") >= 0);
    }
    @Test void childObstaclesParticipateInParentMovementAndReleaseChecks() throws Exception {
        for (String name : List.of("bearing/BearingMotion","elevator/ElevatorCollisions","oriented/CartDocking"))
            assertTrue(compiled(name).methods.stream().flatMap(m -> java.util.Arrays.stream(m.instructions.toArray()))
                    .anyMatch(i -> i instanceof MethodInsnNode c && c.owner.endsWith("/oriented/ChildMotion")),name);
        var query = compiled("oriented/ChildMotion").methods.stream().filter(m -> m.name.equals("check") && callIndex(m,"collectCartBody")>=0).findFirst().orElseThrow();
        assertTrue(callIndex(query,"getAllSubLevels") >= 0);
        assertTrue(callIndex(query,"related") >= 0);
        assertEquals(-1,callIndex(query,"setPos"));
    }
    @Test void portableAnimationMirrorsDockExtensionEvenAfterTransferTimeout() throws Exception {
        var method = compiled("client/PortableAnimation").methods.stream().filter(m -> m.name.equals("tick")).findFirst().orElseThrow();
        assertTrue(callIndex(method,"renderPose") >= 0);
        assertTrue(callIndex(method,"transformPosition") >= 0);
        assertEquals(-1,callIndex(method,"isTransferring"),"Idle but connected docks remain extended");
        assertEquals(-1,callIndex(method,"isPowered"),"Retraction must follow the dock animation, not a binary power flag");
        assertTrue(callIndex(method,"csc$extensionDistance") >= 0);
        assertTrue(callIndex(method,"setValue") > callIndex(method,"csc$extensionDistance"));
        assertTrue(callIndex(method,"tickChaser") >= 0);
        assertEquals(-1,callIndex(method,"startTransferringTo"));
    }
    @Test void bearingHeadsUseBodyAngleInsteadOfNominalSpeedPrediction() throws Exception {
        for (String name : List.of("BearingControllerAngleMixin","ClockworkControllerAngleMixin")) {
            var method = compiled("mixin/client/"+name).methods.stream().filter(m -> m.name.equals("csc$physicalHead")).findFirst().orElseThrow();
            assertTrue(callIndex(method,"getAngle") >= 0);
            assertTrue(callIndex(method,"managed") >= 0);
            assertEquals(-1,callIndex(method,"getAngularSpeed"));
        }
    }

    @Test void cartPickupUsesLiveSnapshotsAndFreshPhysicalIds() throws Exception {
        var packed = compiled("oriented/PackedStructures");
        var snapshot = packed.methods.stream().filter(m -> m.name.equals("snapshot")).findFirst().orElseThrow();
        assertTrue(callIndex(snapshot, "saveWithFullMetadata") >= 0, "Read real inventories, not proxy snapshots");
        var restore = packed.methods.stream().filter(m -> m.name.equals("restoreBody")).findFirst().orElseThrow();
        assertTrue(callIndex(restore, "allocateNewSubLevel") >= 0);
        assertTrue(callIndex(restore, "getUniqueId") >= 0);
        var write = packed.methods.stream().filter(m -> m.name.equals("writeItem")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(write.instructions.toArray()).anyMatch(i -> i instanceof org.objectweb.asm.tree.LdcInsnNode ldc
                && "CSCSubLevel".equals(ldc.cst)), "Remove the live UUID from the item");
    }

    @Test void familyPlacementPreflightsChildrenBeforeTheParentIsPlaced() throws Exception {
        var ready = compiled("oriented/PhysicalFamily").methods.stream().filter(m -> m.name.equals("ready")).findFirst().orElseThrow();
        assertTrue(callIndex(ready, "canDisassemble") >= 0);
        assertTrue(callIndex(ready, "getPassengers") >= 0);
        assertTrue(callIndex(ready, "ready") >= 0, "Recursively check all child destinations in one shared set");
        assertEquals(-1, callIndex(ready, "setBlock"));
        assertEquals(-1, callIndex(ready, "disassemble"));
    }

    @Test void orientedPoseUsesTheCreateBasisAndItsHalfBlockAnchor() throws Exception {
        var target = compiled("oriented/OrientedBridge").methods.stream().filter(m -> m.name.equals("target")).findFirst().orElseThrow();
        assertTrue(callIndex(target, "applyRotation") >= 0);
        assertTrue(callIndex(target, "getAnchorVec") >= 0);
        assertEquals(-1, callIndex(target, "teleport"), "Queue the pose after Sable's previous-frame snapshot");
    }

    @Test void dockingPreviewDoesNotBeginInventoryTransfer() throws Exception {
        for (var method : compiled("docking/DockingMotion").methods) {
            assertEquals(-1, callIndex(method, "startTransferringTo"));
            assertEquals(-1, callIndex(method, "extractItem"));
            assertEquals(-1, callIndex(method, "insertItem"));
        }
    }

    @Test void gantrySurvivesTheTransferBeforeItsWorldShaftIsLinked() throws Exception {
        var bridge = compiled("elevator/ElevatorBridge").methods.stream().filter(m -> m.name.equals("assemble")).findFirst().orElseThrow();
        int begin = callIndex(bridge, "beginTransfer"), move = callIndex(bridge, "assembleBlocks"), end = callIndex(bridge, "endTransfer");
        assertTrue(begin >= 0 && move > begin && end > move, "Neighbour callbacks run inside Sable's transfer before user data is set");
        var mixin = compiled("mixin/GantryCarriageBlockMixin");
        var handler = mixin.methods.stream().filter(m -> m.name.equals("csc$shaftRemainsInWorld")).findFirst().orElseThrow();
        assertTrue(callIndex(handler, "retained") >= 0);
        var inject = annotation(handler.visibleAnnotations, handler.invisibleAnnotations, BASE + "injection/Inject");
        assertEquals(List.of("canSurvive"), value(inject, "method"));
        assertEquals(Boolean.TRUE, value(inject, "cancellable"));
        assertEquals("HEAD", value((AnnotationNode) ((List<?>) value(inject, "at")).getFirst(), "value"));
    }

    @Test void pistonSpeedOverrideCannotBypassTheSharedSweep() throws Exception {
        var speed = compiled("mixin/MechanicalPistonMixin").methods.stream().filter(m -> m.name.equals("csc$sweep")).findFirst().orElseThrow();
        assertTrue(callIndex(speed, "csc$checkSpeed") >= 0);
        for (String type : List.of("mixin/MechanicalPistonMixin", "mixin/PulleyMixin")) {
            var preflight = compiled(type).methods.stream().filter(m -> m.name.equals("csc$preflight")).findFirst().orElseThrow();
            assertTrue(callIndex(preflight, "csc$readyToDisassemble") >= 0, "Check before piston/rope world mutations");
            var inject = annotation(preflight.visibleAnnotations, preflight.invisibleAnnotations, BASE + "injection/Inject");
            assertEquals("HEAD", value((AnnotationNode) ((List<?>) value(inject, "at")).getFirst(), "value"));
            assertEquals(Boolean.TRUE, value(inject, "cancellable"));
        }
    }

    @Test void gantryChecksAfterReadingShaftSpeedAndQueuesItsFinalPosition() throws Exception {
        var mixin = compiled("mixin/GantryEntityMixin");
        for (String name : List.of("csc$checkStep", "csc$follow")) {
            var method = mixin.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            var inject = annotation(method.visibleAnnotations, method.invisibleAnnotations, BASE + "injection/Inject");
            assertEquals("RETURN", value((AnnotationNode) ((List<?>) value(inject, "at")).getFirst(), "value"));
            assertEquals(-1, callIndex(method, "teleport"), "Preserve Sable's previous-pose snapshot");
        }
    }

    @Test void collisionDrillingSelectsOnlyDrillsAndKeepsTheOriginalBreakingPipeline() throws Exception {
        var node = compiled("linear/CollisionDrilling");
        var offer = node.methods.stream().filter(m -> m.name.equals("offer")).findFirst().orElseThrow();
        assertTrue(callIndex(offer, "managed") >= 0);
        assertTrue(callIndex(offer, "canBreak") >= 0, "Retain hardness and track exclusions");
        assertTrue(callIndex(offer, "csc$getSubLevel") >= 0, "Never select this structure's own plot");
        assertEquals(-1, callIndex(offer, "destroyBlock"), "Only select a target; original ticks handle progress and drops");
        assertTrue(java.util.Arrays.stream(offer.instructions.toArray()).anyMatch(i ->
                i instanceof FieldInsnNode f && f.name.equals("MECHANICAL_DRILL")));
        assertTrue(java.util.Arrays.stream(offer.instructions.toArray()).anyMatch(i ->
                i instanceof FieldInsnNode f && f.name.equals("disabled")));
    }

    @Test void newLinearControlsDoNotRequireABearing() throws Exception {
        var resolve = compiled("client/BearingControlsClient").methods.stream().filter(m -> m.name.equals("resolve")).findFirst().orElseThrow();
        assertTrue(callIndex(resolve, "managed") >= 0);
        assertEquals(-1, callIndex(resolve, "bearing"));
    }

    @Test void bothActorBackendsUseThePhysicalRenderPoseWithoutMovingTheProxy() throws Exception {
        var transform = compiled("client/PhysicalActorTransform");
        var apply = transform.methods.stream().filter(m -> m.name.equals("apply")).findFirst().orElseThrow();
        assertTrue(callIndex(apply, "renderPose") >= 0, "Use the same interpolated pose as the real blocks");
        assertTrue(callIndex(apply, "transformPosition") >= 0, "Rotation alone cannot correct elevator translation");
        for (String mutation : List.of("setPos", "setPosRaw", "teleportTo", "applyLocalTransforms"))
            assertEquals(-1, callIndex(apply, mutation), "Rendering must not change gameplay state or reapply proxy transforms");
        for (String backend : List.of("mixin/client/PhysicalActorMatricesMixin", "mixin/client/PhysicalActorVisualMixin")) {
            boolean sharedPose = false;
            for (var method : compiled(backend).methods) for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.owner.equals(transform.name) && call.name.equals("apply"))
                    sharedPose = true;
            assertTrue(sharedPose, "Both ordinary and Flywheel rendering need the physical pose: " + backend);
        }
    }

    @Test void bearingStallParticlesAreClientOnlyAndUseTheOriginalCreateEffect() throws Exception {
        var effects = compiled("client/BearingStallEffects");
        var subscriber = annotation(effects.visibleAnnotations, effects.invisibleAnnotations,
                "net/neoforged/fml/common/EventBusSubscriber");
        assertNotNull(subscriber);
        var sides = (List<?>) value(subscriber, "value");
        assertEquals(1, sides.size());
        assertEquals("CLIENT", ((String[]) sides.getFirst())[1]);
        boolean originalEffect = false, synchronizedStall = false;
        for (var method : effects.methods) for (var instruction : method.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            originalEffect |= call.owner.equals("com/simibubi/create/content/kinetics/base/KineticEffectHandler")
                    && call.name.equals("spawnEffect");
            synchronizedStall |= call.name.equals("isStalled");
            assertNotEquals("setSpeed", call.name, "Particles must not mutate the kinetic network");
        }
        assertTrue(originalEffect);
        assertTrue(synchronizedStall, "Use the entity's server-synchronized stall state");
    }

    @Test void riderDisplacementSurvivesSablesPreviousPoseSnapshot() throws Exception {
        var physics = dependency("dev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem");
        var tick = physics.methods.stream().filter(m -> m.name.equals("tick")).findFirst().orElseThrow();
        int snapshot = callIndex(tick, "updateLastPose"), step = callIndex(tick, "tickPipelinePhysics");
        assertTrue(snapshot >= 0 && step > snapshot, "Sable must snapshot before publishing physics events");
        var ours = compiled("elevator/ElevatorPhysics");
        var target = ours.methods.stream().filter(m -> m.name.equals("target")).findFirst().orElseThrow();
        for (String premature : List.of("hold", "teleport", "logicalPose", "updateLastPose"))
            assertEquals(-1, callIndex(target, premature), "Controller must queue its target until after the snapshot");
        for (String event : List.of("beforePhysics", "afterPhysics")) {
            var handler = ours.methods.stream().filter(m -> m.name.equals(event)).findFirst().orElseThrow();
            assertTrue(callIndex(handler, "hold") >= 0, "Apply/correct the pose only during the physical step");
        }
    }

    @Test void physicalContactTickerRetainsSablesSignalImplementation() throws Exception {
        var contact = dependency("dev/ryanhcode/sable/neoforge/mixinhelper/compatibility/create/redstone_contact/RedstoneContactBlockEntity");
        assertTrue(contact.methods.stream().anyMatch(m -> m.name.equals("tick")), "Runtime must provide native contact ticking");
        var ticker = compiled("mixin/PhysicalActorTickerMixin");
        boolean exemption = false;
        for (var method : ticker.methods) for (var instruction : method.instructions)
            if (instruction instanceof FieldInsnNode field && field.owner.equals("com/simibubi/create/AllBlocks")
                    && field.name.equals("REDSTONE_CONTACT")) exemption = true;
        assertTrue(exemption, "A MovementBehaviour must not suppress the real contact's signal updates");
    }

    @Test void breakerProtectsOwnedPlotAtSelectionResumeAndDestruction() throws Exception {
        var breaker = compiled("mixin/BlockBreakingMovementMixin");
        var selectors = new java.util.HashSet<String>();
        for (var method : breaker.methods) {
            var inject = annotation(method.visibleAnnotations, method.invisibleAnnotations, BASE + "injection/Inject");
            if (inject == null) continue;
            for (Object selector : (List<?>) value(inject, "method")) selectors.add((String) selector);
            assertEquals(Boolean.TRUE, value(inject, "cancellable"));
            assertTrue(callIndex(method, "csc$ownsTarget") >= 0, "Guard every destructive entry point");
            assertTrue(callIndex(method, "cancel") >= 0);
        }
        assertEquals(java.util.Set.of("visitNewPosition", "tickBreaker", "destroyBlock"), selectors);
        var guard = breaker.methods.stream().filter(m -> m.name.equals("csc$ownsTarget")).findFirst().orElseThrow();
        assertTrue(callIndex(guard, "managed") >= 0, "Leave ordinary contraptions and trains unchanged");
        assertTrue(callIndex(guard, "getContaining") >= 0);
        assertTrue(callIndex(guard, "csc$getSubLevel") >= 0, "Exclude by physical UUID, not proxy world coordinates");
    }

    private static ClassNode compiled(String path) throws Exception {
        return read(Files.newInputStream(Path.of("build/classes/java/main/dev/createsablecontraptions/" + path + ".class")));
    }

    private static int callIndex(MethodNode method, String name) {
        for (int i = 0; i < method.instructions.size(); i++)
            if (method.instructions.get(i) instanceof MethodInsnNode call && call.name.equals(name)) return i;
        return -1;
    }

    @Test void conversionsExcludeTrainsAndOnlyOptInPhysicalStabilizedChildren() throws Exception {
        var link = compiled("elevator/ElevatorLink");
        var types = new java.util.HashSet<String>();
        for (var method : link.methods) {
            if (!method.name.equals("linear") && !method.name.equals("bearing") && !method.name.equals("oriented")) continue;
            for (var instruction : method.instructions)
                if (instruction instanceof org.objectweb.asm.tree.TypeInsnNode type && type.getOpcode() == org.objectweb.asm.Opcodes.INSTANCEOF)
                    if (!type.desc.equals("dev/createsablecontraptions/elevator/ElevatorLink")) types.add(type.desc);
        }
        assertEquals(java.util.Set.of("com/simibubi/create/content/contraptions/pulley/PulleyContraption",
                "com/simibubi/create/content/contraptions/piston/PistonContraption",
                "com/simibubi/create/content/contraptions/gantry/GantryContraption",
                "com/simibubi/create/content/contraptions/mounted/MountedContraption",
                "com/simibubi/create/content/contraptions/bearing/StabilizedContraption",
                "com/simibubi/create/content/contraptions/bearing/BearingContraption",
                "com/simibubi/create/content/contraptions/bearing/ClockworkContraption"), types);
        var oriented = link.methods.stream().filter(m -> m.name.equals("oriented")).findFirst().orElseThrow();
        assertTrue(callIndex(oriented, "csc$isPhysicalChild") >= 0, "A train's ordinary stabilized child must not be converted");
    }

    @Test void clockDisassemblyChecksBothHandsBeforeOriginalMutations() throws Exception {
        var mixin = compiled("mixin/ClockworkBearingMixin");
        var method = mixin.methods.stream().filter(m -> m.name.equals("csc$preflightBothHands")).findFirst().orElseThrow();
        var inject = annotation(method.visibleAnnotations, method.invisibleAnnotations, BASE + "injection/Inject");
        assertEquals(Boolean.TRUE, value(inject, "cancellable"));
        var at = (List<?>) value(inject, "at");
        assertEquals("HEAD", value((AnnotationNode) at.getFirst(), "value"));
        int checks = 0;
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.name.equals("ready") && call.owner.endsWith("/BearingBridge")) checks++;
        assertEquals(2, checks, "Never place one clock hand before checking the other hand's destination");
    }

    @Test void physicalControlsRegisterClientInputWithoutProxyRayGates() throws Exception {
        var node = read(Files.newInputStream(Path.of(
                "build/classes/java/main/dev/createsablecontraptions/client/ElevatorControlsClient.class")));
        var subscriber = annotation(node.visibleAnnotations, node.invisibleAnnotations,
                "net/neoforged/fml/common/EventBusSubscriber");
        assertNotNull(subscriber, "Physical controls must register on the event bus");
        var sides = (List<?>) value(subscriber, "value");
        assertEquals(1, sides.size());
        assertEquals("CLIENT", ((String[]) sides.getFirst())[1], "Never load client input code on a dedicated server");
        for (String name : List.of("scroll", "use")) {
            var method = node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            var event = annotation(method.visibleAnnotations, method.invisibleAnnotations,
                    "net/neoforged/bus/api/SubscribeEvent");
            assertNotNull(event, name);
            assertEquals("HIGHEST", ((String[]) value(event, "priority"))[1],
                    "Physical controls must consume input before ordinary block/filter handling");
        }
        boolean movingInteraction = false, physicalRenderer = false;
        for (var method : node.methods) for (var instruction : method.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            assertFalse(call.owner.endsWith("/ContraptionHandlerClient"), "Do not gate physical input behind proxy ray selection");
            assertNotEquals("getBoundingBox", call.name, "A moving proxy box must not reject a real Sable block hit");
            movingInteraction |= call.name.equals("handlePlayerInteraction") && call.owner.endsWith("/AbstractContraptionEntity");
            physicalRenderer |= call.name.equals("renderInContraption") && call.owner.endsWith("/ContraptionControlsRenderer");
        }
        assertTrue(movingInteraction, "Use Create's original moving interaction and floor packet");
        assertTrue(physicalRenderer, "Render original floor labels at the physical control");
    }

    @Test void compiledCallsStillLinkAgainstRuntimeVersions() throws Exception {
        int checked = 0;
        try (var paths = Files.walk(Path.of("build/classes/java/main"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                ClassNode node = read(Files.newInputStream(path));
                for (MethodNode method : node.methods) for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && upstream(call.owner)) {
                        assertTrue(hasMember(call.owner, call.name, call.desc, false, new java.util.HashSet<>()),
                                "Unlinked method from " + node.name + ": " + call.owner + "." + call.name + call.desc);
                        checked++;
                    } else if (instruction instanceof FieldInsnNode field && upstream(field.owner)) {
                        assertTrue(hasMember(field.owner, field.name, field.desc, true, new java.util.HashSet<>()),
                                "Unlinked field from " + node.name + ": " + field.owner + "." + field.name + field.desc);
                        checked++;
                    }
                }
            }
        }
        assertTrue(checked > 50, "The linkage check must inspect real compiled calls");
    }

    private static boolean upstream(String name) {
        return name.startsWith("com/simibubi/create/") || name.startsWith("dev/ryanhcode/sable/");
    }

    private static boolean hasMember(String owner, String name, String descriptor, boolean field,
                                     java.util.Set<String> visited) throws Exception {
        if (owner == null || !visited.add(owner)) return false;
        ClassNode node = dependency(owner);
        if (field ? node.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(descriptor))
                : node.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor))) return true;
        if (name.equals("<init>")) return false;
        if (hasMember(node.superName, name, descriptor, field, visited)) return true;
        for (String iface : node.interfaces) if (hasMember(iface, name, descriptor, field, visited)) return true;
        return false;
    }

    @Test void allRequiredInjectionPointsExistInPublishedJars() throws Exception {
        Path root = Path.of("build/classes/java/main/dev/createsablecontraptions/mixin");
        int checked = 0;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                ClassNode mixin = read(Files.newInputStream(path));
                AnnotationNode annotation = annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, BASE + "Mixin");
                assertNotNull(annotation, path.toString());
                List<?> classes = (List<?>) value(annotation, "value");
                List<?> strings = (List<?>) value(annotation, "targets");
                String target = classes != null ? ((Type) classes.getFirst()).getInternalName()
                        : ((String) strings.getFirst()).replace('.', '/');
                // Simulated is optional and absent from the four pinned dependency matrices.
                // Its ownership/cancellation contract is checked separately above.
                if (target.equals("dev/simulated_team/simulated/content/blocks/physics_assembler/PhysicsAssemblerBlockEntity")) {
                    assertNotNull(annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, BASE + "Pseudo"));
                    continue;
                }
                ClassNode dependency = dependency(target);
                for (var field : mixin.fields) {
                    if (annotation(field.visibleAnnotations, field.invisibleAnnotations, BASE + "Shadow") != null) {
                        assertTrue(dependency.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)),
                                target + " missing shadow field " + field.name);
                        checked++;
                    }
                }
                for (MethodNode handler : mixin.methods) {
                    var wrap = annotation(handler.visibleAnnotations, handler.invisibleAnnotations,
                            "com/llamalad7/mixinextras/injector/wrapmethod/WrapMethod");
                    if (wrap != null) {
                        for (Object selector : (List<?>) value(wrap, "method")) {
                            var targets = dependency.methods.stream().filter(m -> m.name.equals(selector)).toList();
                            assertEquals(1, targets.size(), target + " missing wrapped method " + selector);
                            var method = targets.getFirst();
                            var parameters = Type.getArgumentTypes(method.desc);
                            var actual = Type.getArgumentTypes(handler.desc);
                            assertEquals(parameters.length + 1, actual.length);
                            for (int i = 0; i < parameters.length; i++) assertEquals(parameters[i], actual[i]);
                            assertEquals(method.access & 8, handler.access & 8);
                            checked++;
                        }
                    }
                    var inject = annotation(handler.visibleAnnotations, handler.invisibleAnnotations, BASE + "injection/Inject");
                    if (inject != null) {
                        for (Object selectorObject : (List<?>) value(inject, "method")) {
                            String selector = (String) selectorObject;
                            List<MethodNode> matches = dependency.methods.stream().filter(m -> selector.equals(m.name)
                                    || selector.equals(m.name + m.desc)).toList();
                            assertEquals(1, matches.size(), target + " ambiguous/missing injection " + selector);
                            MethodNode method = matches.getFirst();
                            Type[] callback = Type.getArgumentTypes(handler.desc);
                            Type[] original = Type.getArgumentTypes(method.desc);
                            if (callback.length > 1) {
                                assertEquals(original.length + 1, callback.length, handler.name);
                                for (int i = 0; i < original.length; i++) assertEquals(original[i], callback[i], handler.name);
                            }
                            assertEquals(method.access & 8, handler.access & 8, "static mismatch: " + handler.name);
                            checked++;
                        }
                    }
                    if (annotation(handler.visibleAnnotations, handler.invisibleAnnotations, BASE + "Shadow") != null) {
                        assertTrue(dependency.methods.stream().anyMatch(m -> m.name.equals(handler.name) && m.desc.equals(handler.desc)),
                                target + " missing shadow method " + handler.name);
                        checked++;
                    }
                    var accessor = annotation(handler.visibleAnnotations, handler.invisibleAnnotations, BASE + "gen/Accessor");
                    if (accessor != null) {
                        String name = (String) value(accessor, "value");
                        Type fieldType = Type.getReturnType(handler.desc);
                        if (fieldType.equals(Type.VOID_TYPE)) {
                            assertEquals(1, Type.getArgumentTypes(handler.desc).length, "Accessor setter must have one argument");
                            fieldType = Type.getArgumentTypes(handler.desc)[0];
                        }
                        String descriptor = fieldType.getDescriptor();
                        assertTrue(dependency.fields.stream().anyMatch(f -> f.name.equals(name)
                                        && f.desc.equals(descriptor)), target + " accessor " + name);
                        checked++;
                    }
                    var invoker = annotation(handler.visibleAnnotations, handler.invisibleAnnotations, BASE + "gen/Invoker");
                    if (invoker != null) {
                        String name = (String) value(invoker, "value");
                        assertTrue(dependency.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(handler.desc)),
                                target + " invoker " + name);
                        checked++;
                    }
                }
            }
        }
        assertTrue(checked >= 20, "The compatibility checks must not silently skip mixins");
    }

    private static ClassNode dependency(String name) throws Exception {
        ClassNode cached = CLASSES.get(name);
        if (cached != null) return cached;
        ClassNode result = loadDependency(name);
        CLASSES.put(name, result);
        return result;
    }

    private static ClassNode loadDependency(String name) throws Exception {
        var loader = MixinTargetsTest.class.getClassLoader();
        InputStream direct = loader.getResourceAsStream(name + ".class");
        if (direct != null) return read(direct);
        // ModDev keeps mapped Minecraft off the ordinary JUnit runtime. Read its build artifact
        // as bytes for inherited members, without putting the game on the execution classpath.
        Path mappedGame = Path.of("build/moddev/artifacts/neoforge-21.1.228-merged.jar");
        try (var jar = new java.util.jar.JarFile(mappedGame.toFile())) {
            var entry = jar.getJarEntry(name + ".class");
            if (entry != null) return read(jar.getInputStream(entry));
        }
        // Inspect bundled Rapier/Flywheel bytecode from published metadata, never initialize it.
        var nested = new java.util.LinkedHashSet<String>();
        var manifests = loader.getResources("META-INF/jarjar/metadata.json");
        while (manifests.hasMoreElements()) {
            try (var manifest = manifests.nextElement().openStream()) {
                String json = new String(manifest.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                var matcher = java.util.regex.Pattern.compile("\"path\"\\s*:\\s*\"([^\"]*\\.jar)\"").matcher(json);
                while (matcher.find()) nested.add(matcher.group(1));
            }
        }
        for (String path : nested) try (var input = loader.getResourceAsStream(path)) {
            assertNotNull(input, "Missing target " + name);
            try (var jar = new JarInputStream(input)) {
                for (var entry = jar.getNextJarEntry(); entry != null; entry = jar.getNextJarEntry()) {
                    if (entry.getName().equals(name + ".class")) {
                        ClassNode node = new ClassNode();
                        new ClassReader(jar.readAllBytes()).accept(node, ClassReader.SKIP_CODE);
                        return node;
                    }
                }
            }
        }
        throw new AssertionError("Missing dependency class " + name);
    }
    private static ClassNode read(InputStream input) throws Exception {
        try (input) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }
    private static AnnotationNode annotation(List<AnnotationNode> a, List<AnnotationNode> b, String name) {
        List<AnnotationNode> all = new ArrayList<>();
        if (a != null) all.addAll(a);
        if (b != null) all.addAll(b);
        return all.stream().filter(node -> node.desc.equals("L" + name + ";")).findFirst().orElse(null);
    }
    private static Object value(AnnotationNode node, String key) {
        if (node.values != null) for (int i = 0; i < node.values.size(); i += 2)
            if (node.values.get(i).equals(key)) return node.values.get(i + 1);
        return null;
    }
}
