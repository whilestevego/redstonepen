package wile.redstonepen.blocks.controlbox;

import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

class ControlBoxLogic
{
  static class Logic
  {
    public int input_mask  = 0x00000000;  // 24bit, direction ordinal nibbles
    public int input_data  = 0x00000000;
    public int output_mask = 0x00000000;
    public int output_data = 0x00000000;
    public int intr_redges = 0x00000000;  // Rising edges seen between logic ticks
    public int intr_fedges = 0x00000000;  // Falling edges seen between logic ticks
    public long rca_input_mask  = 0;      // 64bit, direction ordinal nibbles
    public long rca_input_data  = 0;
    public long rca_output_mask = 0;
    public long rca_output_data = 0;

    private static int counter_function(String sym, MathExpr.Expr[] x, Map<String, Integer> m)
    {
      final int nargs = x.length;
      if(nargs <= 0) return 0;
      int q = m.getOrDefault(sym,0);
      if(nargs >= 5 && x[4].calc(m)>0) {
        q = 0;
      } else if(nargs == 1) {
        if(x[0].calc(m) > 0) ++q;
      } else {
        int x0 = x[0].calc(m);
        int x1 = x[1].calc(m);
        if((x0>0) && (x1<=0)) { ++q; } else if((x0<=0) && (x1>0)) { --q; }
      }
      if(nargs >= 4) {
        q = Mth.clamp(q, x[2].calc(m), x[3].calc(m));
      } else if(nargs >= 3) {
        q = Mth.clamp(q, 0, x[2].calc(m));
      } else {
        q = Mth.clamp(q, 0, 0x7fffffff);
      }
      m.put(sym, q);
      return q;
    }

    private static int timer_on_function(String sym, MathExpr.Expr[] x, Map<String, Integer> m)
    {
      if(x.length != 2) { m.remove("." + sym + ".clk"); m.remove(sym + ".et"); m.remove(sym + ".pt"); return 0; } // Invalid.
      final int in = x[0].calc(m);
      final int pt = x[1].calc(m);
      if(in <= 0) {
        // Signal 0.
        m.remove("." + sym + ".clk");
        m.put(sym + ".et", 0);
        return MathExpr.Expr.bool_false();
      } else if(pt <= 0) {
        // No time defined or changed.
        return MathExpr.Expr.bool_true(); // return (in>0) ? MathExpr.Expr.bool_true() : MathExpr.Expr.bool_false();
      } else {
        final int now = m.getOrDefault(".clock", 0);
        int et = m.getOrDefault(sym + ".et", 0);
        if(et >= pt) {
          return MathExpr.Expr.bool_true();
        } else if(et <= 0) {
          m.put("." + sym + ".clk", now);
          m.put(sym + ".et", 1);
          m.put(sym + ".pt", pt);
          m.put(".deadline", Math.min(m.getOrDefault(".deadline", 20), pt));
          return MathExpr.Expr.bool_false();
        } else {
          et = Math.min(now-m.getOrDefault("." + sym + ".clk", now), pt);
          m.put(sym + ".et", et);
          if(et >= pt) {
            m.remove("." + sym + ".clk");
            return MathExpr.Expr.bool_true();
          } else {
            m.put(".deadline", Math.min(m.getOrDefault(".deadline", 20), pt-et));
            return MathExpr.Expr.bool_false();
          }
        }
      }
    }

    private static int timer_off_function(String sym, MathExpr.Expr[] x, Map<String, Integer> m)
    {
      if(x.length != 2) { m.remove("." + sym + ".clk"); m.remove(sym + ".et"); m.remove(sym + ".pt"); return 0; } // Invalid.
      final int in = x[0].calc(m);
      final int pt = x[1].calc(m);
      if(in > 0) {
        // Signal not false.
        m.remove("." + sym + ".clk");
        m.put(sym + ".et", 0);
        return MathExpr.Expr.bool_true();
      } else if(pt <= 0) {
        // No time defined or changed.
        return MathExpr.Expr.bool_true(); // return (in<=0) ? MathExpr.Expr.bool_true() : MathExpr.Expr.bool_false();
      } else {
        final int now = m.getOrDefault(".clock", 0);
        int et = m.getOrDefault(sym + ".et", 0);
        if(et >= pt) {
          return MathExpr.Expr.bool_false();
        } else if(et <= 0) {
          m.put("." + sym + ".clk", now);
          m.put(sym + ".et", 1);
          m.put(sym + ".pt", pt);
          m.put(".deadline", Math.min(m.getOrDefault(".deadline", 20), pt));
          return MathExpr.Expr.bool_true();
        } else {
          et = Math.min(now-m.getOrDefault("." + sym + ".clk", now), pt);
          m.put(sym + ".et", et);
          if(et >= pt) {
            m.remove("." + sym + ".clk");
            return MathExpr.Expr.bool_false();
          } else {
            m.put(".deadline", Math.min(m.getOrDefault(".deadline", 20), pt-et));
            return MathExpr.Expr.bool_true();
          }
        }
      }
    }

    private static int timer_pulse_function(String sym, MathExpr.Expr[] x, Map<String, Integer> m)
    {
      if(x.length != 2) { m.remove("." + sym + ".clk"); m.remove(sym + ".et"); m.remove(sym + ".pt"); return 0; } // Invalid.
      final int in = x[0].calc(m);
      final int pt = x[1].calc(m);
      if(pt <= 0) return (in>0) ? MathExpr.Expr.bool_true() : MathExpr.Expr.bool_false();
      int et = m.getOrDefault(sym + ".et", 0);
      if(et > 0) {
        if(et >= pt) {
          // Timer expired, waiting for falling input edge.
          if(in<=0) m.put(sym + ".et", 0);
          return MathExpr.Expr.bool_false();
        } else {
          // Timer running, output constant during that time.
          final int now = m.getOrDefault(".clock", 0);
          et = Math.min(now-m.getOrDefault("." + sym + ".clk", now), pt);
          m.put(sym + ".et", et);
          if(et >= pt) {
            m.remove("." + sym + ".clk");
            return MathExpr.Expr.bool_false();
          } else {
            m.put(".deadline", Math.min(m.getOrDefault(".deadline", 20), pt-et));
            return MathExpr.Expr.bool_true();
          }
        }
      } else if(in > 0) {
        // Input rising edge or initial signal.
        m.put("." + sym + ".clk", m.getOrDefault(".clock", 0));
        m.put(sym + ".et", 1);
        m.put(sym + ".pt", pt);
        m.put(".deadline", Math.min(m.getOrDefault(".deadline", 20), pt));
        return MathExpr.Expr.bool_true();
      } else {
        // Signal 0, not started.
        return MathExpr.Expr.bool_false();
      }
    }

    private static int timer_interval_function(String sym, MathExpr.Expr[] x, Map<String, Integer> m)
    {
      if(x.length < 1 || x.length > 2) { m.remove(sym + ".clk"); return 0; } // Invalid.
      final int en = (x.length < 2) ? 15 : x[1].calc(m);
      if(en <= 0) { m.remove(sym + ".clk"); return 0; } // Disabled by enable signal argument.
      final int pt = x[0].calc(m);
      if(pt <= 2) return MathExpr.Expr.bool_false();
      final int now = m.getOrDefault(".clock", 0);
      final int clk = m.getOrDefault(sym + ".clk", now-pt);
      if(Math.abs(now-clk) >= pt) {
        m.put(sym + ".clk", now);
        m.put(".deadline", 1);
        return MathExpr.Expr.bool_true();
      } else {
        m.put(".deadline", Math.min(m.getOrDefault(".deadline", 20), clk-now+pt));
        return MathExpr.Expr.bool_false();
      }
    }

    private static List<MathExpr.ExprFuncDef> make_functions()
    {
      return Arrays.asList(
        new MathExpr.ExprFuncDef("inv", 1, (x,m)->Math.min(15, Math.max(0, 15-x[0].calc(m)))),
        new MathExpr.ExprFuncDef("max", -1, (x,m)->(Arrays.stream(x).mapToInt(e->e.calc(m)).max().orElse(0))),
        new MathExpr.ExprFuncDef("min", -1, (x,m)->(Arrays.stream(x).mapToInt(e->e.calc(m)).min().orElse(0))),
        new MathExpr.ExprFuncDef("lim", -1, (x,m)->switch(x.length) { case 0->0; case 1->Math.min(15,Math.max(0,x[0].calc(m))); case 2->Math.min(x[1].calc(m),Math.max(0,x[0].calc(m))); default->Math.min(x[2].calc(m),Math.max(x[1].calc(m),x[0].calc(m))); }),
        new MathExpr.ExprFuncDef("if", -1, (x,m)->switch(x.length) { case 0->0; case 1->((x[0].calc(m)>0)?15:0); case 2->((x[0].calc(m)>0) ? x[1].calc(m) : 0); default->((x[0].calc(m)>0) ? x[1].calc(m) : x[2].calc(m)); }),
        new MathExpr.ExprFuncDef("mean", -1, (x,m)->((x.length==0)?(0):(Arrays.stream(x).mapToInt(e->e.calc(m)).sum()/x.length))),
        new MathExpr.ExprFuncDef("rnd",  0, (x,m)->((int)(Math.random()*16.0))),
        new MathExpr.ExprFuncDef("clock",  0, (x,m)->m.getOrDefault(".clock", 0)),
        new MathExpr.ExprFuncDef("time",  0, (x,m)->m.getOrDefault(".time", 0)),
        new MathExpr.ExprFuncDef("tiv1", -1, (x,m)->timer_interval_function(".tiv1", x, m)),
        new MathExpr.ExprFuncDef("tiv2", -1, (x,m)->timer_interval_function(".tiv2", x, m)),
        new MathExpr.ExprFuncDef("tiv3", -1, (x,m)->timer_interval_function(".tiv3", x, m)),
        new MathExpr.ExprFuncDef("cnt1", -1, (x,m)->counter_function(".cnt1", x, m)),
        new MathExpr.ExprFuncDef("cnt2", -1, (x,m)->counter_function(".cnt2", x, m)),
        new MathExpr.ExprFuncDef("cnt3", -1, (x,m)->counter_function(".cnt3", x, m)),
        new MathExpr.ExprFuncDef("cnt4", -1, (x,m)->counter_function(".cnt4", x, m)),
        new MathExpr.ExprFuncDef("cnt5", -1, (x,m)->counter_function(".cnt5", x, m)),
        new MathExpr.ExprFuncDef("ton1", 2, (x,m)->timer_on_function("ton1", x, m)),
        new MathExpr.ExprFuncDef("ton2", 2, (x,m)->timer_on_function("ton2", x, m)),
        new MathExpr.ExprFuncDef("ton3", 2, (x,m)->timer_on_function("ton3", x, m)),
        new MathExpr.ExprFuncDef("ton4", 2, (x,m)->timer_on_function("ton4", x, m)),
        new MathExpr.ExprFuncDef("ton5", 2, (x,m)->timer_on_function("ton5", x, m)),
        new MathExpr.ExprFuncDef("tof1", 2, (x,m)->timer_off_function("tof1", x, m)),
        new MathExpr.ExprFuncDef("tof2", 2, (x,m)->timer_off_function("tof2", x, m)),
        new MathExpr.ExprFuncDef("tof3", 2, (x,m)->timer_off_function("tof3", x, m)),
        new MathExpr.ExprFuncDef("tof4", 2, (x,m)->timer_off_function("tof4", x, m)),
        new MathExpr.ExprFuncDef("tof5", 2, (x,m)->timer_off_function("tof5", x, m)),
        new MathExpr.ExprFuncDef("tp1", 2, (x,m)->timer_pulse_function("tp1", x, m)),
        new MathExpr.ExprFuncDef("tp2", 2, (x,m)->timer_pulse_function("tp2", x, m)),
        new MathExpr.ExprFuncDef("tp3", 2, (x,m)->timer_pulse_function("tp3", x, m)),
        new MathExpr.ExprFuncDef("tp4", 2, (x,m)->timer_pulse_function("tp4", x, m)),
        new MathExpr.ExprFuncDef("tp5", 2, (x,m)->timer_pulse_function("tp5", x, m))
      );
    }

    private static final List<MathExpr.ExprFuncDef> functions_ = make_functions();
    Map<String,Integer> symbols_ = new HashMap<>();
    private MultiLineMathExpr expressions_ = MultiLineMathExpr.EMPTY;
    private String code_ = "";

    public boolean valid()
    { return expressions_.invalid_entries.isEmpty(); }

    public MultiLineMathExpr expressions()
    { return expressions_; }

    public Map<Integer,String> errors()
    { return expressions_.invalid_entries.stream().collect(Collectors.toMap(e->(e.offset+e.parsed.pe), e->(e.parsed.error))); }

    public void symbol(String key, int value)
    { symbols_.put(key.toLowerCase(), value); }

    public int symbol(String key)
    { return symbols_.getOrDefault(key.toLowerCase(), 0); }

    public Map<String,Integer> symbols()
    { return symbols_; }

    public String code()
    { return code_; }

    public boolean code(String new_code)
    {
      if(code_.equals(new_code) && !expressions_.isEmpty()) return expressions_.invalid_entries.isEmpty();
      code_ = new_code;
      expressions_ = MultiLineMathExpr.of(code_, "", functions_);
      input_mask = 0;
      output_mask = 0;
      rca_input_mask = 0;
      rca_output_mask = 0;
      symbols_.clear();
      for(int i=0; i<Defs.PORT_NAMES.size(); ++i ) {
        final String port = Defs.PORT_NAMES.get(i);
        if(expressions_.symbols.contains(port+".co.re") || expressions_.symbols.contains(port+".co.fe")) {
          expressions_.symbols.add(port+".co");
        }
        if(expressions_.assignments.contains(port)) {
          output_mask |= 0xf<<(4*i);
        } else if(Arrays.stream(MultiLineMathExpr.VALID_SYMBOL_SUFFIXES).map(s->port+s).anyMatch(expressions_.symbols::contains)) {
          input_mask |= 0xf<<(4*i);
        }
      }
      expressions_.symbols.forEach((esym)->{
        if(esym.matches("^d[io][1]?[\\d][\\d]?$")) {
          final int channel = Integer.parseInt(esym.substring(2));
          if(channel > 15) return;
          if(esym.charAt(1) == 'i') {
            rca_input_mask |= 0xfL<<(channel*4);
          } else {
            rca_output_mask |= 0xfL<<(channel*4);
          }
        }
      });
      rca_input_data &= rca_input_mask;
      rca_output_data &= rca_output_mask;
      output_data &= output_mask;
      input_data &= input_mask;
      return expressions_.invalid_entries.isEmpty();
    }

    public void tick()
    {
      // Input symbols
      for(int i=0; i<Defs.PORT_NAMES.size(); ++i ) {
        if((input_mask & (0xf<<(4*i))) != 0) symbol(Defs.PORT_NAMES.get(i), (input_data>>(4*i)) & 0xf);
      }
      if(rca_input_mask != 0) {
        for(int i=0; i<16; ++i) {
          if((rca_input_mask & (0xfL<<(4*i))) != 0) symbol("di"+i, (int)((rca_input_data>>(4*i)) & 0xfL));
        }
      }
      // Edge detection
      expressions_.symbols.forEach((esym)->{
        if(!esym.contains(".")) return;
        final boolean isrising = esym.endsWith(".re");
        if(isrising || esym.endsWith(".fe")) {
          final Map<String, Integer> syms = symbols();
          final String symref = esym.substring(0, esym.length()-3);
          if(!syms.containsKey(symref) && (!Defs.PORT_NAMES.contains(symref))) return;
          final String symlast = "."+esym+".d";
          final int q1 = (syms.getOrDefault(symref,0)>0) ? 15:0;
          boolean edge = false;
          if(syms.containsKey(symlast)) {
            final int q0 = (syms.getOrDefault(symlast,0)>0) ? 15:0;
            edge = (isrising) ? ((q0<=0) && (q1>0)) : ((q0>0) && (q1<=0));
          }
          symbol(esym, edge ? 15 : 0);
          symbol(symlast, q1);
        }
      });
      for(int i=0; i<Defs.PORT_NAMES.size(); ++i ) {
        int port_mask = 0xf<<(4*i);
        if((intr_redges & port_mask) != 0) symbol(Defs.PORT_NAMES.get(i)+".re", 15);
        if((intr_fedges & port_mask) != 0) symbol(Defs.PORT_NAMES.get(i)+".fe", 15);
      }
      intr_redges = 0;
      intr_fedges = 0;

      // Calculation
      symbol(".deadline", 40);
      final Map<String,Integer> assigned = expressions_.recalculate(symbols_, (entry, mem)->
        switch(entry.parsed.assignment_symbol) {
          case "r","b","y","g","u","d" -> (Math.max(0, Math.min(15, entry.last_result)));
          default -> entry.last_result;
        }
      );
      // Assign outputs and update mem.
      assigned.forEach(this::symbol);
      output_data = 0;
      for(int i=0; i<Defs.PORT_NAMES.size(); ++i) output_data |= (symbol(Defs.PORT_NAMES.get(i)) & 0xf)<<(4*i);
      output_data &= output_mask;
      if(rca_output_mask != 0) {
        rca_output_data = 0;
        for(int i=0; i<16; ++i) {
          if((rca_output_mask & (0xfL<<(4*i))) != 0) {
            rca_output_data |= ((long)Math.min(15, Math.max(0, symbol("do"+i))))<<(4*i);
          }
        }
      }
      rca_output_data &= rca_output_mask;
    }
  }

  public static class MultiLineMathExpr
  {
    public static final MultiLineMathExpr EMPTY = new MultiLineMathExpr();
    static final String[] VALID_SYMBOL_SUFFIXES = { "", ".re", ".fe", ".co", ".co.re", ".co.fe", ".pt", ".et" }; // comparator override, edges, timers

    public static class Entry
    {
      public final int line_index, offset;
      public final MathExpr.ParsedLine parsed;
      public int last_result = 0;

      public Entry(int line_index, int offset, MathExpr.ParsedLine parsed)
      { this.line_index=line_index; this.offset=offset; this.parsed=parsed; }
    }

    public static MultiLineMathExpr of(String code)
    { return of(code, "", Collections.emptyList()); }

    public static MultiLineMathExpr of(String code, String assignment_variable)
    { return of(code, assignment_variable, Collections.emptyList()); }

    public static MultiLineMathExpr of(String code, String assignment_variable, Collection<MathExpr.ExprFuncDef> functions)
    {
      if(code.trim().isEmpty()) return EMPTY;
      final List<Entry> entries = new ArrayList<>();
      final String[] lines = code.replace("[\\r\\n\\s]+$", "").split("[\\r]?[\\n]");
      final List<Entry> parse_errors = new ArrayList<>();
      final Set<String> symbols = new HashSet<>();
      final Set<String> assignments = new HashSet<>();
      int line_index=0, offset=0;
      for(String line:lines) {
        if(!line.trim().isEmpty()) {
          final Entry entry = new Entry(line_index, offset, MathExpr.ParsedLine.of(line, assignment_variable, functions));
          if(!entry.parsed.error.isEmpty()) {
            parse_errors.add(entry);
          } else {
            Optional<String> invalid_symbol = entry.parsed.symbols.stream().filter((sym)->{
              final int pos = sym.indexOf('.');
              if(pos < 0) return false;
              if(pos >= sym.length()-1) return true; // ends with dot, suffix missing.
              final String suffix = sym.substring(pos);
              return !Arrays.asList(VALID_SYMBOL_SUFFIXES).contains(suffix);
            }).findFirst();
            if(invalid_symbol.isPresent()) {
              entry.parsed.error = "parse_error";
              entry.parsed.pe = entry.parsed.line.indexOf(invalid_symbol.get());
              if(entry.parsed.pe < 0) entry.parsed.pe = entry.parsed.line.length()-1;
              parse_errors.add(entry);
            } else {
              symbols.addAll(entry.parsed.symbols);
              if(entry.parsed.expression.type == MathExpr.ExprType.ASSIGN) {
                entries.add(entry);
                assignments.add(entry.parsed.assignment_symbol);
              }
            }
          }
        }
        ++line_index;
        offset += 1+line.length();
      }
      return (entries.isEmpty() && parse_errors.isEmpty()) ? EMPTY : (new MultiLineMathExpr(entries, parse_errors, symbols, assignments));
    }

    public MultiLineMathExpr()
    { this(Collections.emptyList(), Collections.emptyList(), Collections.emptySet(), Collections.emptySet()); }

    public MultiLineMathExpr(List<Entry> lines, List<Entry> parse_error_entries, Set<String> symbols, Set<String> assignments)
    { this.entries=lines; this.invalid_entries=parse_error_entries; this.symbols=symbols; this.assignments=assignments; }

    public boolean isEmpty()
    { return entries.isEmpty(); }

    public Map<String,Integer> recalculate(Map<String,Integer> mem, BiFunction<Entry, Map<String,Integer>, Integer> assignment_post_processor)
    {
      final Map<String,Integer> assigned = new HashMap<>();
      for(var entry:entries) {
        entry.last_result = entry.parsed.expression.calc(mem);
        if(!entry.parsed.assignment_symbol.isEmpty()) {
          assigned.put(entry.parsed.assignment_symbol, assignment_post_processor.apply(entry, mem));
        }
      }
      return assigned;
    }

    public final List<Entry> entries;
    public final List<Entry> invalid_entries;
    public final Set<String> symbols;
    public final Set<String> assignments;
  }

  public static class MathExpr
  {
    public enum ExprType { VOID, CONST, VARREF, FUNC, NEG, NOT, MPY, DIV, MOD, ADD, SUB, AND, OR, XOR, NEQ, EQ, LE, GE, LT, GT, ASSIGN }

    public static class Expr
    {
      public static final Expr EMPTY = new Expr(ExprType.VOID, "<EMPTY>");
      protected Expr(ExprType type, String name) { this.type=type; this.name=name; }
      public int calc(Map<String, Integer> mem) { return 0; }
      public String toString() { return "{VOID}"; }
      public final ExprType type;
      public final String name;
      //-------------------------------
      public static int bool_true()  { return 15; }
      public static int bool_false() { return 0; }
      public static int assignment_sanitize(int x) { return x; }
    }

    public static abstract class ExprOp extends Expr
    {
      public ExprOp(ExprType type, List<Expr> args) { super(type, type.toString()); arguments=args; }
      public abstract int calc(Map<String, Integer> mem);
      public String toString() { return name+"{" + arguments.stream().map(Object::toString).collect(Collectors.joining(",")) + "}"; }
      public final List<Expr> arguments;
    }

    public static class ExprConst extends Expr
    {
      public ExprConst(int val) { super(ExprType.CONST, "<CONST>"); value=val; }
      public int calc(Map<String, Integer> mem) { return value; }
      public String toString() { return "CONST{"+value+"}"; }
      private final int value;
    }

    public static class ExprVarRef extends Expr
    {
      public ExprVarRef(String ref) { super(ExprType.VARREF, ref); }
      public int calc(Map<String, Integer> mem) { return mem.getOrDefault(name, 0); }
      public String toString() { return "SYM{'"+name+"'}"; }
    }

    public static class ExprFunc extends Expr
    {
      public ExprFunc(String name, int nargs, BiFunction<Expr[], Map<String, Integer>, Integer> fn, List<Expr> args)
      {
        super(ExprType.FUNC, name);
        arguments = args;
        this.func = fn;
        this.num_arguments = nargs;
        if((nargs >= 0) && (nargs != args.size())) throw new RuntimeException("invalid_number_of_arguments");
      }
      public int calc(Map<String, Integer> mem) { return func.apply(arguments.toArray(new Expr[0]), mem); }
      public String toString() { return "FN{'"+name+"'(" + arguments.stream().map(Object::toString).collect(Collectors.joining(",")) + ")}"; }
      public int nargs() { return num_arguments; }
      protected final List<Expr> arguments;
      protected final int num_arguments;
      private final BiFunction<Expr[], Map<String, Integer>, Integer> func;
    }

    public static class ExprNeg extends ExprOp
    {
      public ExprNeg(List<Expr> args) { super(ExprType.NEG, args); }
      public int calc(Map<String, Integer> mem) { return -arguments.get(0).calc(mem); }
    }

    public static class ExprNot extends ExprOp
    {
      public ExprNot(List<Expr> args) { super(ExprType.NOT, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem)==0) ? bool_true() : bool_false(); }
    }

    public static class ExprMpy extends ExprOp
    {
      public ExprMpy(List<Expr> args) { super(ExprType.MPY, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) * arguments.get(1).calc(mem)); }
    }

    public static class ExprDiv extends ExprOp
    {
      public ExprDiv(List<Expr> args) { super(ExprType.DIV, args); }
      public int calc(Map<String, Integer> mem) { int b=arguments.get(1).calc(mem); return (b<=0) ? (0) : (arguments.get(0).calc(mem)/b); }
    }

    public static class ExprMod extends ExprOp
    {
      public ExprMod(List<Expr> args) { super(ExprType.MOD, args); }
      public int calc(Map<String, Integer> mem) { int b=arguments.get(1).calc(mem); return (b<=0) ? (0) : (arguments.get(0).calc(mem)%b); }
    }

    public static class ExprAdd extends ExprOp
    {
      public ExprAdd(List<Expr> args) { super(ExprType.ADD, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) + arguments.get(1).calc(mem)); }
    }

    public static class ExprSub extends ExprOp
    {
      public ExprSub(List<Expr> args) { super(ExprType.SUB, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) - arguments.get(1).calc(mem)); }
    }

    public static class ExprAnd extends ExprOp
    {
      public ExprAnd(List<Expr> args) { super(ExprType.AND, args); }
      public int calc(Map<String, Integer> mem) { return ((arguments.get(0).calc(mem)>0) && (arguments.get(1).calc(mem)>0)) ? bool_true() : bool_false(); }
    }

    public static class ExprOr extends ExprOp
    {
      public ExprOr(List<Expr> args) { super(ExprType.OR, args); }
      public int calc(Map<String, Integer> mem) { return ((arguments.get(0).calc(mem)>0) || (arguments.get(1).calc(mem)>0)) ? bool_true() : bool_false(); }
    }

    public static class ExprXor extends ExprOp
    {
      public ExprXor(List<Expr> args) { super(ExprType.XOR, args); }
      public int calc(Map<String, Integer> mem) { return ((arguments.get(0).calc(mem)>0) ^ (arguments.get(1).calc(mem)>0)) ? bool_true() : bool_false(); }
    }

    public static class ExprNeq extends ExprOp
    {
      public ExprNeq(List<Expr> args) { super(ExprType.NEQ, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) != arguments.get(1).calc(mem)) ? bool_true() : bool_false(); }
    }

    public static class ExprEq extends ExprOp
    {
      public ExprEq(List<Expr> args) { super(ExprType.EQ, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) == arguments.get(1).calc(mem)) ? bool_true() : bool_false(); }
    }

    public static class ExprGe extends ExprOp
    {
      public ExprGe(List<Expr> args) { super(ExprType.GE, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) >= arguments.get(1).calc(mem)) ? bool_true() : bool_false(); }
    }

    public static class ExprLe extends ExprOp
    {
      public ExprLe(List<Expr> args) { super(ExprType.LE, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) <= arguments.get(1).calc(mem)) ? bool_true() : bool_false(); }
    }

    public static class ExprGt extends ExprOp
    {
      public ExprGt(List<Expr> args) { super(ExprType.GT, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) > arguments.get(1).calc(mem)) ? bool_true() : bool_false(); }
    }

    public static class ExprLt extends ExprOp
    {
      public ExprLt(List<Expr> args) { super(ExprType.LT, args); }
      public int calc(Map<String, Integer> mem) { return (arguments.get(0).calc(mem) < arguments.get(1).calc(mem)) ? bool_true() : bool_false(); }
    }

    public static class ExprAssign extends Expr
    {
      public final Expr value;
      public ExprAssign(String ref, Expr value) { super(ExprType.ASSIGN, ref); this.value=value; }
      public int calc(Map<String, Integer> mem) {
        final int res = value.calc(mem);
        if(!name.isEmpty()) { mem.put(name, res); }
        return res;
      }
      public String toString() { return "ASSIGN{'"+name+"'," + value + "}"; }
    }

    public static class ExprFuncDef
    {
      ExprFuncDef(String name, int nargs, BiFunction<Expr[], Map<String, Integer>, Integer> func) { this.name=name; this.func=func; this.nargs=nargs; }
      public final String name;
      public final BiFunction<Expr[], Map<String, Integer>, Integer> func;
      public final int nargs;
    }

    public static class ParsedLine
    {
      public final Expr expression;
      public final String assignment_symbol;
      public final String line;
      public final Set<String> symbols = new HashSet<>();

      public String error;
      public int pe = -1;
      public char c = ' ';
      public final Map<String, ExprFuncDef> functions = new HashMap<>();

      public static ParsedLine of(String line)
      { return of(line, ""); }

      public static ParsedLine of(String line, String default_assignment_variable)
      { return of(line, default_assignment_variable, Collections.emptyList()); }

      public static ParsedLine of(String line, String default_assignment_variable, Collection<ExprFuncDef> functions)
      { return new ParsedLine(line, default_assignment_variable, functions); }

      public String toString()
      {
        return "ParsedLine{"
          + " line:\"" + line.replaceAll("\"", "\\\"") + "\","
          + ((error.isEmpty()) ? "" : (" error:\"" + error.replaceAll("\"", "\\\"") + " @pos=" + pe + "\","))
          + " sym:\"" + String.join(",", symbols) + "\","
          + " expr:\"" + expression + "\" "
          + "}";
      }

      private ParsedLine(String line, String default_assignment_variable, Collection<ExprFuncDef> functions)
      {
        this.line = line;
        Expr exp = Expr.EMPTY;
        String err = "";
        String assign = "";
        functions.forEach((def)->this.functions.put(def.name.toLowerCase(), def));
        if(!line.matches("^[\\s]*#.*")) {
          try {
            adv();
            exp = expr_assign(default_assignment_variable);
            if(pe < line.length()) {
              exp = Expr.EMPTY;
              err = "invalid_character";
            } else {
              assign = exp.name.toLowerCase();
            }
          } catch(Exception e) {
            err = "parse_error";
          }
        }
        this.expression = exp;
        this.assignment_symbol = assign;
        this.error = err;
      }

      private void adv()
      {
        if(++pe >= line.length()) {
          c = '\0';
        } else if((c=='\n') || (c=='\r') || (c=='#')) { // EOL, line comment start
          pe = line.length();
          c = '\0';
        } else {
          final int ci = line.charAt(pe);
          if(ci>127) {
            throw new RuntimeException("invalid_character");
          } else {
            c = Character.toLowerCase((char)ci);
          }
        }
      }

      private boolean adv(char match)
      {
        while((c==' ') || (c=='\t') || (c=='#')) adv();
        if(c != match) return false;
        adv();
        return true;
      }

      private boolean adv(String match)
      {
        while((c==' ') || (c=='\t') || (c=='#')) adv();
        if(!line.regionMatches(true, pe, match, 0, match.length())) return false;
        pe += match.length()-1;
        adv();
        return true;
      }

      private Expr expr_assign(String default_assignment_variable)
      {
        String ref = default_assignment_variable;
        if(line.matches("^[\\s]*[a-zA-Z][\\w.]*[\\s]*[=][^=].*")) {
          ref = const_literal();
          if(!adv('=')) throw new RuntimeException("expected_assignment");
          if(functions.containsKey(ref.toLowerCase())) throw new RuntimeException("symbol_readonly");
        }
        symbols.add(ref);
        return new ExprAssign(ref, expr());
      }

      private Expr expr()
      { return expr_or(); }

      private Expr expr_or()
      {
        Expr x = expr_xor();
        while(true) {
          if(adv("or")) x = new ExprOr(List.of(x, expr_xor()));
          else if(adv("||")) x = new ExprOr(List.of(x, expr_xor()));
          else if(adv('|')) x = new ExprOr(List.of(x, expr_xor()));
          else return x;
        }
      }

      private Expr expr_xor()
      {
        Expr x = expr_and();
        while(true) {
          if(adv("xor")) x = new ExprXor(List.of(x, expr_and()));
          else if(adv('^')) x = new ExprXor(List.of(x, expr_and()));
          else return x;
        }
      }

      private Expr expr_and()
      {
        Expr x = expr_rel();
        while(true) {
          if(adv("and")) x = new ExprAnd(List.of(x, expr_rel()));
          else if(adv("&&")) x = new ExprAnd(List.of(x, expr_rel()));
          else if(adv('&')) x = new ExprAnd(List.of(x, expr_rel()));
          else return x;
        }
      }

      private Expr expr_rel()
      {
        Expr x = arith_add();
        while(true) {
          if(adv("!=")) x = new ExprNeq(List.of(x, arith_add()));
          if(adv("<>")) x = new ExprNeq(List.of(x, arith_add()));
          if(adv("==")) x = new ExprEq(List.of(x, arith_add()));
          if(adv(">=")) x = new ExprGe(List.of(x, arith_add()));
          if(adv("<=")) x = new ExprLe(List.of(x, arith_add()));
          if(adv('>')) x = new ExprGt(List.of(x, arith_add()));
          if(adv('<')) x = new ExprLt(List.of(x, arith_add()));
          else return x;
        }
      }

      private Expr arith_add()
      {
        Expr x = arith_mpy();
        while(true) {
          if(adv('+')) x = new ExprAdd(List.of(x, arith_mpy()));
          else if(adv('-')) x = new ExprSub(List.of(x, arith_mpy()));
          else return x;
        }
      }

      private Expr arith_mpy()
      {
        Expr x = arith_fact();
        while(true) {
          if (adv('*')) x = new ExprMpy(List.of(x, arith_fact()));
          else if (adv('/')) x = new ExprDiv(List.of(x, arith_fact()));
          else if (adv('%')) x = new ExprMod(List.of(x, arith_fact()));
          else return x;
        }
      }

      private Expr arith_fact()
      {
        if(adv('+')) adv();
        if(adv('-') && (!adv('-'))) return new ExprNeg(List.of(arith_fact()));
        if(adv('!')) return new ExprNot(List.of(arith_fact()));
        if(adv('(')) {
          Expr e = expr();
          if(!adv(')')) throw new RuntimeException("missing_closing_parenthesis");
          return e;
        } else if((c>='0') && (c<='9')) {
          return new ExprConst(const_number());
        } else if(c>='a' && c<='z') {
          final String sym = const_literal();
          if(sym.equals("not")) {
            return new ExprNot(List.of(expr()));
          } else if(adv('(')) {
            List<Expr> args = new ArrayList<>();
            if(!adv(')')) {
              args.add(expr());
              while(adv(',')) args.add(expr());
              if(!adv(')')) throw new RuntimeException("missing_closing_function_parenthesis");
            }
            final ExprFuncDef fn = functions.getOrDefault(sym, null);
            if(fn==null) throw new RuntimeException("unknown_function");
            return new ExprFunc(fn.name, fn.nargs, fn.func, args);
          } else {
            if(functions.containsKey(sym)) throw new RuntimeException("missing_function_arguments");
            symbols.add(sym);
            return new ExprVarRef(sym);
          }
        }
        throw new RuntimeException("unexpected_character");
      }

      private String const_literal()
      {
        if(c<'a' || c>'z') return "";
        final int p0 = this.pe;
        while((c>='a' && c<='z') || (c>='0' && c<='9') || (c=='.') || (c=='_')) adv(); // Dot is allowed
        return line.substring(p0, this.pe).toLowerCase();
      }

      private int const_number()
      {
        final int p0 = this.pe;
        while((c>='0') && (c<='9')) adv();
        return Integer.parseInt(line.substring(p0, this.pe));
      }

    }
  }
}
