/*
 * @file TrackNet.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wile.redstonepen.blocks.track;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

public class TrackNet
{
  final List<BlockPos> neighbour_positions;
  final List<Direction> neighbour_sides;
  final List<Direction> internal_sides;
  final List<Direction> power_sides;
  private int power;

  public TrackNet(List<BlockPos> positions, List<Direction> ext_sides, List<Direction> int_sides, List<Direction> pwr_sides)
  { neighbour_positions=positions; neighbour_sides=ext_sides; internal_sides=int_sides; power_sides=pwr_sides; power=0; }

  public TrackNet(List<BlockPos> positions, List<Direction> ext_sides, List<Direction> int_sides, List<Direction> pwr_sides, int power_setval)
  { neighbour_positions=positions; neighbour_sides=ext_sides; internal_sides=int_sides; power_sides=pwr_sides; power=power_setval; }

  int getPower() { return power; }
  void setPower(int p) { power = p; }

  @Override
  public String toString() {
    String s = "NET{";
    s += "p:" + power;
    s += ", intsides:" +  String.join("", internal_sides.stream().map(TrackBlockEntity::dirstr).toList());
    s += ", pwrsides:" +  String.join("", power_sides.stream().map(TrackBlockEntity::dirstr).toList());
    s += ", nbsides:" +  String.join("", neighbour_sides.stream().map(TrackBlockEntity::dirstr).toList());
    s += ", nbpos:" +  String.join(",", neighbour_positions.stream().map(TrackBlockEntity::posstr).toList());
    return s + "}";
  }
}
