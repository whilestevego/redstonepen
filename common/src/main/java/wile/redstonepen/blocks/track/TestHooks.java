/*
 * @file TestHooks.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 */
package wile.redstonepen.blocks.track;

import net.minecraft.core.Direction;
import wile.redstonepen.blocks.track.RedstoneTrackDefs.connections;

public final class TestHooks
{
  private long state = 0L;

  public long getState() { return state; }
  public void setState(long s) { state = s; }

  public int getWireFlags()
  { return (int)((state & RedstoneTrackDefs.STATE_FLAG_WIR_MASK) >> RedstoneTrackDefs.STATE_FLAG_WIR_POS); }

  public boolean getWireFlag(int index)
  { return (state & (1L << (RedstoneTrackDefs.STATE_FLAG_WIR_POS + index))) != 0; }

  public int getWireFlagCount()
  { return RedstoneTrackDefs.STATE_FLAG_WIR_COUNT; }

  public int getConnectionFlags()
  { return (int)((state & RedstoneTrackDefs.STATE_FLAG_CON_MASK) >> RedstoneTrackDefs.STATE_FLAG_CON_POS); }

  public boolean getConnectionFlag(int index)
  { return (state & (1L << (RedstoneTrackDefs.STATE_FLAG_CON_POS + index))) != 0; }

  public int getConnectionFlagCount()
  { return RedstoneTrackDefs.STATE_FLAG_CON_COUNT; }

  public int getSidePower(Direction side)
  {
    final int shift = RedstoneTrackDefs.STATE_FLAG_PWR_POS + 4 * connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0);
    return (int)((state >> shift) & 0xf);
  }

  public void setSidePower(Direction side, int p)
  {
    final int shift = RedstoneTrackDefs.STATE_FLAG_PWR_POS + 4 * connections.CONNECTION_BIT_ORDER_REV.getOrDefault(side, 0);
    state = (state & ~(((long)(0xf)) << shift)) | (((long)(p & 0xf)) << shift);
  }

  public int addWireFlags(long flags)
  {
    int n_added = 0;
    for(int i = 0; i < getWireFlagCount(); ++i) {
      long mask = 1L << i;
      if(((flags & mask) != 0) && ((state & mask) == 0)) {
        state |= mask;
        ++n_added;
      }
    }
    return n_added;
  }

  public int getRedstoneDustCount()
  {
    int n = 0;
    int rem = getWireFlags();
    for(int i = 0; (rem != 0) && (i < RedstoneTrackDefs.STATE_FLAG_WIR_COUNT); ++i) {
      if((rem & 1) != 0) ++n;
      rem >>= 1;
    }
    rem = getConnectionFlags();
    for(int i = 0; (rem != 0) && (i < RedstoneTrackDefs.STATE_FLAG_CON_COUNT); ++i) {
      if((rem & 1) != 0) ++n;
      rem >>= 1;
    }
    return n;
  }
}
