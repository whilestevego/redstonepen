package wile.redstonepen.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTestHelper;
import wile.redstonepen.ModConstants;
import wile.redstonepen.gametestcommon.RelayTests;

@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public class RelayGameTests
{
  private static final String EMPTY_RELAY_TEMPLATE = "relay_activates_from_redstone";

  public RelayGameTests() {}

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayActivatesFromRedstone(GameTestHelper helper)
  { RelayTests.relayActivatesFromRedstone(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void invertedRelayRegistersPoweredInput(GameTestHelper helper)
  { RelayTests.invertedRelayRegistersPoweredInput(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void pulseRelayClearsItsPulseAfterTick(GameTestHelper helper)
  { RelayTests.pulseRelayClearsItsPulseAfterTick(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void pulseRelaySetsPoweredFalseWhenInputRemoved(GameTestHelper helper)
  { RelayTests.pulseRelaySetsPoweredFalseWhenInputRemoved(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void bistableRelayLatchesOnFirstRisingEdge(GameTestHelper helper)
  { RelayTests.bistableRelayLatchesOnFirstRisingEdge(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayDownFacingRotation0ActivatesFromEast(GameTestHelper helper)
  { RelayTests.relayDownFacingRotation0ActivatesFromEast(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayDownFacingRotation0DoesNotActivateFromNorth(GameTestHelper helper)
  { RelayTests.relayDownFacingRotation0DoesNotActivateFromNorth(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayDownFacingRotation1ShiftsOutputToEast(GameTestHelper helper)
  { RelayTests.relayDownFacingRotation1ShiftsOutputToEast(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void relayDownFacingRotation1ActivatesFromNorth(GameTestHelper helper)
  { RelayTests.relayDownFacingRotation1ActivatesFromNorth(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation0FrontIsNorth(GameTestHelper helper)
  { RelayTests.downFacingRotation0FrontIsNorth(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation1FrontIsEast(GameTestHelper helper)
  { RelayTests.downFacingRotation1FrontIsEast(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation2FrontIsSouth(GameTestHelper helper)
  { RelayTests.downFacingRotation2FrontIsSouth(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingRotation3FrontIsWest(GameTestHelper helper)
  { RelayTests.downFacingRotation3FrontIsWest(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void upFacingIsOppositeOfFacingForAllDirections(GameTestHelper helper)
  { RelayTests.upFacingIsOppositeOfFacingForAllDirections(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void downFacingMethodEqualsFacingPropertyForAllDirections(GameTestHelper helper)
  { RelayTests.downFacingMethodEqualsFacingPropertyForAllDirections(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void rightFacingIsRotationPlusOneRelativeToFront(GameTestHelper helper)
  { RelayTests.rightFacingIsRotationPlusOneRelativeToFront(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void backFacingIsRotationPlusTwoRelativeToFront(GameTestHelper helper)
  { RelayTests.backFacingIsRotationPlusTwoRelativeToFront(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void leftFacingIsRotationPlusThreeRelativeToFront(GameTestHelper helper)
  { RelayTests.leftFacingIsRotationPlusThreeRelativeToFront(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void allSixMappedDirectionsAreDistinctForEveryState(GameTestHelper helper)
  { RelayTests.allSixMappedDirectionsAreDistinctForEveryState(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void forwardThenReverseStateMappedFacingIsIdentity(GameTestHelper helper)
  { RelayTests.forwardThenReverseStateMappedFacingIsIdentity(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementDownFaceRotation0(GameTestHelper helper)
  { RelayTests.getStateForPlacementDownFaceRotation0(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementDownFaceRotation1(GameTestHelper helper)
  { RelayTests.getStateForPlacementDownFaceRotation1(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementDownFaceRotation2(GameTestHelper helper)
  { RelayTests.getStateForPlacementDownFaceRotation2(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementDownFaceRotation3(GameTestHelper helper)
  { RelayTests.getStateForPlacementDownFaceRotation3(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementNorthFaceRotation0(GameTestHelper helper)
  { RelayTests.getStateForPlacementNorthFaceRotation0(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementNorthFaceRotation1(GameTestHelper helper)
  { RelayTests.getStateForPlacementNorthFaceRotation1(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementNorthFaceRotation2(GameTestHelper helper)
  { RelayTests.getStateForPlacementNorthFaceRotation2(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementEastFaceRotation1(GameTestHelper helper)
  { RelayTests.getStateForPlacementEastFaceRotation1(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementEastFaceRotation2(GameTestHelper helper)
  { RelayTests.getStateForPlacementEastFaceRotation2(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementSouthFaceRotation1(GameTestHelper helper)
  { RelayTests.getStateForPlacementSouthFaceRotation1(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementSouthFaceRotation3(GameTestHelper helper)
  { RelayTests.getStateForPlacementSouthFaceRotation3(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementWestFaceRotation1(GameTestHelper helper)
  { RelayTests.getStateForPlacementWestFaceRotation1(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementWestFaceRotation3(GameTestHelper helper)
  { RelayTests.getStateForPlacementWestFaceRotation3(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void getStateForPlacementReturnsNullWhenNoSurface(GameTestHelper helper)
  { RelayTests.getStateForPlacementReturnsNullWhenNoSurface(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 10)
  public static void relayDropsWhenSupportBlockRemoved(GameTestHelper helper)
  { RelayTests.relayDropsWhenSupportBlockRemoved(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
  public static void relayDeactivatesWhenPowerRemovedViaTick(GameTestHelper helper)
  { RelayTests.relayDeactivatesWhenPowerRemovedViaTick(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 30)
  public static void bistableRelayDeactivatesOnFallingEdge(GameTestHelper helper)
  { RelayTests.bistableRelayDeactivatesOnFallingEdge(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 20)
  public static void invertedRelayDeactivatesWhenPowerRemoved(GameTestHelper helper)
  { RelayTests.invertedRelayDeactivatesWhenPowerRemoved(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void bridgeRelayPlacedAndGetSignalDoesNotThrow(GameTestHelper helper)
  { RelayTests.bridgeRelayPlacedAndGetSignalDoesNotThrow(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void bridgeRelayGetInputPowerFromRedstoneWire(GameTestHelper helper)
  { RelayTests.bridgeRelayGetInputPowerFromRedstoneWire(helper); }

  @GameTest(template = EMPTY_RELAY_TEMPLATE, timeoutTicks = 5)
  public static void bridgeRelayGetSignalFromLeftWireConnection(GameTestHelper helper)
  { RelayTests.bridgeRelayGetSignalFromLeftWireConnection(helper); }
}
