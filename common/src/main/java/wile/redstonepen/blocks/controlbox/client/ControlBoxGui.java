package wile.redstonepen.blocks.controlbox.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import wile.redstonepen.ModContent;
import wile.redstonepen.blocks.controlbox.Defs;
import wile.redstonepen.blocks.controlbox.ControlBoxUiContainer;
import wile.redstonepen.libmc.Auxiliaries;
import wile.redstonepen.libmc.Guis;
import wile.redstonepen.libmc.GuiTextEditing;
import wile.redstonepen.libmc.TooltipDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Environment(EnvType.CLIENT)
public class ControlBoxGui extends Guis.ContainerGui<ControlBoxUiContainer>
{
  private final int VALUE_UPDATE_INTERVAL = 2;
  private final String tooltip_prefix = ModContent.references.CONTROLBOX_BLOCK.getDescriptionId();
  private final GuiTextEditing.MultiLineTextBox textbox;
  private final Guis.CheckBox start_stop;
  private final Guis.ImageButton cb_copy_all;
  private final Guis.ImageButton cb_paste_all;
  private final Guis.Image cb_error_indicator;
  private final Guis.Image rca_enabled_indicator;
  private final List<Guis.TextBox> port_stati;
  private final List<Guis.Image> port_stati_i_indicators;
  private final List<Guis.Image> port_stati_o_indicators;
  private final Map<String, Integer> symbols_ = new HashMap<>();
  private final List<Tuple<Integer, String>> errors_ = new ArrayList<>();
  private int update_counter_ = 0;
  private boolean focus_editor_ = false;
  private boolean debug_enabled_ = false;
  private boolean code_requested_ = false;
  private Component activating_player_ = Component.empty();

  public ControlBoxGui(ControlBoxUiContainer container, Inventory player_inventory, Component title)
  {
    super(container, player_inventory, title,"textures/gui/control_box_gui.png", 238, 206);
    titleLabelX = 17; titleLabelY = -10;
    start_stop = new Guis.CheckBox(getBackgroundImage(), 12, 12, Guis.Coord2d.of(15,213), Guis.Coord2d.of(28,213));
    cb_copy_all = new Guis.ImageButton(getBackgroundImage(), 12, 12, Guis.Coord2d.of(41,213));
    cb_paste_all = new Guis.ImageButton(getBackgroundImage(), 12, 12, Guis.Coord2d.of(54,213));
    cb_error_indicator = new Guis.Image(getBackgroundImage(), 5, 2, Guis.Coord2d.of(68,213));
    rca_enabled_indicator = new Guis.Image(getBackgroundImage(), 7, 7, Guis.Coord2d.of(90,215));
    textbox = new GuiTextEditing.MultiLineTextBox(29, 12, 156, 170, Component.literal("Code"));
    port_stati = new ArrayList<>();
    port_stati_i_indicators = new ArrayList<>();
    port_stati_o_indicators = new ArrayList<>();
  }

  @Override
  public void init()
  {
    super.init();
    {
      textbox.init(this, Guis.Coord2d.of(29, 12)).setFontColor(0xdddddd).setCursorColor(0xdddddd).setLineHeight(7).onValueChanged((tb)->push_code(textbox.getValue()));
      addRenderableWidget(textbox);
      start_stop.init(this, Guis.Coord2d.of(196, 14)).tooltip(Auxiliaries.localizable(tooltip_prefix+".tooltips.runstop"));
      start_stop.onclick((cb)->{
        final net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        {
          final wile.api.rca.RedstoneClientAdapter rca = wile.api.rca.FmmRedstoneClientAdapter.Adapter.instance();
          if(rca != null && rca.isOpen()) nbt.putBoolean("withrca", true);
        }
        getMenu().onGuiAction("enabled", nbt);
        focus_editor_=true;
      });
      addRenderableWidget(start_stop);
      cb_copy_all.init(this, Guis.Coord2d.of(212, 14)).tooltip(Auxiliaries.localizable(tooltip_prefix+".tooltips.copyall"));
      cb_copy_all.onclick((cb)->{Auxiliaries.setClipboard(textbox.getValue()); focus_editor_=true; });
      cb_copy_all.visible = false;
      addRenderableWidget(cb_copy_all);
      cb_paste_all.init(this, Guis.Coord2d.of(212, 14)).tooltip(Auxiliaries.localizable(tooltip_prefix+".tooltips.pasteall"));
      cb_paste_all.onclick((cb)->{textbox.setValue(Auxiliaries.getClipboard().orElse("")); push_code(textbox.getValue()); focus_editor_=true; });
      cb_paste_all.visible = false;
      addRenderableWidget(cb_paste_all);
      cb_error_indicator.init(this, Guis.Coord2d.of(230, 14));
      cb_error_indicator.visible = false;
      addRenderableWidget(cb_error_indicator);
      rca_enabled_indicator.init(this, Guis.Coord2d.of(194, 40));
      rca_enabled_indicator.visible = false;
      rca_enabled_indicator.tooltip((rcae)->Auxiliaries.localizable(tooltip_prefix+".tooltips.rcaplayer", activating_player_));
      addRenderableWidget(rca_enabled_indicator);
    }
    {
      int ygap=12, x0=getGuiLeft()+205, y0=getGuiTop()+56;
      int[] liney_map = { (5*ygap), (4*ygap), (0), (2*ygap), (3*ygap), (ygap) };
      port_stati.clear();
      port_stati_i_indicators.clear();
      port_stati_o_indicators.clear();
      port_stati.add(new Guis.TextBox(x0, y0+liney_map[0], 30,10, Component.literal("down"), font));   // DOWN
      port_stati.add(new Guis.TextBox(x0, y0+liney_map[1], 30,10, Component.literal("up"), font));     // UP
      port_stati.add(new Guis.TextBox(x0, y0+liney_map[2], 30,10, Component.literal("red"), font));    // NORTH
      port_stati.add(new Guis.TextBox(x0, y0+liney_map[3], 30,10, Component.literal("yellow"), font)); // SOUTH
      port_stati.add(new Guis.TextBox(x0, y0+liney_map[4], 30,10, Component.literal("green"), font));  // WEST
      port_stati.add(new Guis.TextBox(x0, y0+liney_map[5], 30,10, Component.literal("blue"), font));   // EAST
      for(int i=0; i<port_stati.size(); ++i) {
        {
          final Guis.TextBox tb = port_stati.get(i);
          tb.setEditable(false);
          tb.setBordered(false);
          tb.setTextColor(0xffdddddd);
          tb.setTextColorUneditable(0xffdddddd);
          tb.setValue(String.format("%1s=00", Defs.PORT_NAMES.get(i).toUpperCase()));
          addRenderableWidget(tb);
        }
        {
          final Guis.Image img = new Guis.Image(getBackgroundImage(), 3, 6, Guis.Coord2d.of(78, 215));
          img.init(this, Guis.Coord2d.of(191, 56+liney_map[i]));
          port_stati_i_indicators.add(img);
          addRenderableWidget(img);
        }
        {
          final Guis.Image img = new Guis.Image(getBackgroundImage(), 3, 6, Guis.Coord2d.of(84, 215));
          img.init(this, Guis.Coord2d.of(189, 56+liney_map[i]));
          port_stati_o_indicators.add(img);
          addRenderableWidget(img);
        }
      }
    }
    {
      final List<TooltipDisplay.TipRange> tooltips = new ArrayList<>();
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+200,getGuiTop()+36, 36, 16, ()->{
        final Component c = Component.literal("");
        symbols_.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach((kv)->{
          final String k = kv.getKey();
          if((!debug_enabled_) && (k.startsWith(".") || Defs.PORT_NAMES.contains(k) || k.endsWith(".re") || k.endsWith(".fe"))) return;
          final String lf = (c.getSiblings().isEmpty()) ? "" : "\n"; // bah, can't do Component.join(separator)
          c.getSiblings().add(Component.literal(String.format("%s%s = %d", lf, k.toUpperCase(), kv.getValue())));
        });
        return c;
      }));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+196,getGuiTop()+14, 16, 16, ()->
        (errors_.isEmpty()) ? (Component.empty()) : (Auxiliaries.localizable(tooltip_prefix+".error."+errors_.get(0).getB()))
      ));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+12, 5, 8, Auxiliaries.localizable(tooltip_prefix+".help.1")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+22, 5, 3, Auxiliaries.localizable(tooltip_prefix+".help.2")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+27, 5, 5, Auxiliaries.localizable(tooltip_prefix+".help.3")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+34, 5, 5, Auxiliaries.localizable(tooltip_prefix+".help.4")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+41, 5, 6, Auxiliaries.localizable(tooltip_prefix+".help.5")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+49, 5, 4, Auxiliaries.localizable(tooltip_prefix+".help.6")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+55, 5, 5, Auxiliaries.localizable(tooltip_prefix+".help.7")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+62, 5, 3, Auxiliaries.localizable(tooltip_prefix+".help.8")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+67, 5, 7, Auxiliaries.localizable(tooltip_prefix+".help.9")));
      tooltips.add(new TooltipDisplay.TipRange(getGuiLeft()+18,getGuiTop()+76, 5, 3, Auxiliaries.localizable(tooltip_prefix+".help.10")));
      tooltip_.init(tooltips).delay(50);
    }
    setInitialFocus(textbox);
    setFocused(textbox);
    textbox.active = false;
    getMenu().onGuiAction("serverdata");
  }

  @Override
  protected void containerTick()
  {
    // Received server data.
    {
      final net.minecraft.nbt.CompoundTag nbt = getMenu().fetchReceivedServerData();
      if(!nbt.isEmpty()) {
        if(nbt.contains("ports")) {
          final int mask = (nbt.getInt("inputs")|nbt.getInt("outputs"));
          final int io = nbt.getInt("ports");
          for(int i=0; i<Defs.PORT_NAMES.size(); ++i) {
            if((mask & (0xf<<(4*i))) == 0) continue;
            port_stati.get(i).setValue(String.format("%1s=%02d", Defs.PORT_NAMES.get(i).toUpperCase(), (io>>(4*i)) & 0xf));
          }
        }
        if(nbt.contains("code")) {
          textbox.setValue(nbt.getString("code"));
          focus_editor_ = true;
        }
        if(nbt.contains("enabled")) {
          start_stop.checked(nbt.getBoolean("enabled"));
          focus_editor_ = true;
        }
        if(nbt.contains("debug")) {
          debug_enabled_ = nbt.getBoolean("debug");
        }
        if(nbt.contains("inputs")) {
          int mask = nbt.getInt("inputs");
          for(int i=0; i<Defs.PORT_NAMES.size(); ++i) {
            port_stati_i_indicators.get(i).visible = ((mask & 0xf) != 0);
            mask >>=4;
          }
        }
        if(nbt.contains("outputs")) {
          int mask = nbt.getInt("outputs");
          for(int i=0; i<Defs.PORT_NAMES.size(); ++i) {
            port_stati_o_indicators.get(i).visible = ((mask & 0xf) != 0);
            mask >>=4;
          }
        }
        if(nbt.contains("symbols", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
          net.minecraft.nbt.CompoundTag sym_nbt = nbt.getCompound("symbols");
          symbols_.clear();
          sym_nbt.getAllKeys().forEach(k->symbols_.put(k, sym_nbt.getInt(k)));
        }
        if(nbt.contains("errors", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
          net.minecraft.nbt.CompoundTag err_nbt = nbt.getCompound("errors");
          errors_.clear();
          err_nbt.getAllKeys().forEach(k->{ try { errors_.add(new Tuple<>(Integer.parseInt(k), err_nbt.getString(k))); } catch(Throwable ignored) {} });
          if(errors_.isEmpty()) {
            cb_error_indicator.visible = false;
            cb_error_indicator.setX(0);
            cb_error_indicator.setY(0);
            cb_error_indicator.tooltip(Component.empty());
          } else {
            Guis.Coord2d exy = textbox.getCoordinatesAtIndex(errors_.get(0).getA());
            cb_error_indicator.tooltip(Auxiliaries.localizable(tooltip_prefix+".error."+errors_.get(0).getB()));
            cb_error_indicator.visible = true;
            cb_error_indicator.setX(exy.x);
            cb_error_indicator.setY(exy.y + textbox.getLineHeight());
          }
        }
        if(nbt.contains("player", net.minecraft.nbt.Tag.TAG_STRING)) {
          final String player_name = nbt.getString("player");
          if(player_name.isEmpty()) {
            activating_player_ = Component.empty();
            rca_enabled_indicator.visible = false;
            rca_enabled_indicator.active = false;
          } else {
            activating_player_ = Component.literal(player_name);
            rca_enabled_indicator.visible = true;
            rca_enabled_indicator.active = true;
          }
        }
      } else if(--update_counter_ <= 0) {
        update_counter_ = VALUE_UPDATE_INTERVAL;
        if(!code_requested_) {
          code_requested_ = true;
          getMenu().onGuiAction("serverdata");
        } else {
          getMenu().onGuiAction("servervalues");
        }
      }
    }
    // UI update
    {
      start_stop.active = errors_.isEmpty();
      if(!start_stop.active) { start_stop.checked(false); } else { cb_error_indicator.visible = false; }
      textbox.active = !start_stop.checked();
      textbox.setFontColor(textbox.active ? 0xeeeeee : 0x999999);
      cb_paste_all.visible = textbox.active && textbox.getValue().trim().isEmpty();
      cb_copy_all.visible = !cb_paste_all.visible;
      if(focus_editor_) {
        focus_editor_ = false;
        if(!isDragging() && !textbox.isFocused()) {
          children().forEach(child->{
            if((child != textbox) && (child instanceof AbstractWidget wg)) {
              wg.setFocused(false);
            }
          });
          setFocused(textbox);
        }
      }
    }
  }

  @Override
  public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks)
  { super.render(gg, mouseX, mouseY, partialTicks); }

  @Override
  protected void renderLabels(GuiGraphics gg, int x, int y)
  {
    gg.drawString(font, title, titleLabelX+1, titleLabelY+1, 0x303030);
    gg.drawString(font, title, titleLabelX, titleLabelY, 0x707070);
  }

  @Override
  protected void slotClicked(Slot hoveredSlot, int hoveredIndex, int no, ClickType clickType)
  {}

  private void push_code(String text)
  {
    final net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
    nbt.putString("code", text);
    getMenu().onGuiAction("codeupdate", nbt);
  }

}
