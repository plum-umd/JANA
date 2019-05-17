import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.math.BigInteger;
import java.text.DecimalFormat;

class Polynomial implements Comparable<Polynomial> {
  static boolean showAll = false;
  Double constant;
  public Map<Term, Double> terms;  // maps terms to coefficients

  public Polynomial(BigInteger constant) {
    this.terms = new HashMap<Term, Double>();
    this.constant = constant.doubleValue();
  }

  public Polynomial(Double constant) {
    this.terms = new HashMap<Term, Double>();
    this.constant = constant;
  }

  public Polynomial(BigInteger varnum, BigInteger constant) {
    this.terms = new HashMap<Term, Double>();
    this.terms.put(new Term(new VarFactor(varnum)), Double.valueOf(1));
    this.constant = constant.doubleValue();
  }

  public Polynomial(String label, BigInteger constant) {
    this.terms = new HashMap<Term, Double>();
    this.terms.put(new Term(new LabelFactor(label)), Double.valueOf(1));
    this.constant = constant.doubleValue();
  }

  public Polynomial(Factor factor) {
    this.terms = new HashMap<Term, Double>();
    this.terms.put(new Term(factor), Double.valueOf(1));
    this.constant = Double.valueOf(0);
  }

  public Polynomial(Map<Term, Double> terms, Double constant) {
    this.terms = terms;
    this.constant = constant;
  }

  abstract static class Factor {
    abstract public boolean isVar();
    abstract public int printCompareTo(Factor f);
    abstract public int degree();
  }

  static class VarFactor extends Factor {
    public BigInteger varnum;
    
    public VarFactor(BigInteger varnum) {
      this.varnum = varnum;
    }

    public String toString() {
      return "v" + varnum;
    }

    public boolean isVar() { return true; }

    public int degree() { return 1; }

    public int printCompareTo(Factor f) {
      if (f instanceof LabelFactor) {
        return -1;
      } else if (f instanceof VarFactor) {
        return this.varnum.compareTo(((VarFactor) f).varnum);
      } else if (f instanceof InverseFactor) {
        return 1;
      } else {
        throw new RuntimeException("unsupported factor comparison");
      }
    }

    public int hashCode() {
      return this.varnum.hashCode();
    }

    public boolean equals(Object that) {
      if (that instanceof VarFactor) {
        return this.varnum.equals(((VarFactor) that).varnum);
      }

      return false;
    }
  }

  static class LabelFactor extends Factor {
    public String label;
    
    public LabelFactor(String label) {
      this.label = label;
    }

    public String toString() {
      return label;
    }

    public boolean isVar() { return true; }

    public int degree() { return 1; }

    public int printCompareTo(Factor f) {
      if (f instanceof LabelFactor) {
        return this.label.compareTo(((LabelFactor) f).label);
      } else if (f instanceof VarFactor) {
        return 1;
      } else if (f instanceof InverseFactor) {
        return 1;
      } else {
        throw new RuntimeException("unsupported factor comparison");
      }
    }

    public int hashCode() {
      return this.label.hashCode();
    }

    public boolean equals(Object that) {
      if (that instanceof LabelFactor) {
        return this.label.equals(((LabelFactor) that).label);
      }

      return false;
    }
  }

  static class InverseFactor extends Factor {
    public Polynomial polynomial;

    public InverseFactor(Polynomial polynomial) {
      this.polynomial = polynomial;
    }

    public String toString() {
      return "(1 / (" + polynomial + "))";
    }

    public boolean isVar() { return false; }

    public int degree() {
      return -1 * polynomial.degree();
    }

    public int printCompareTo(Factor f) {
      if (f instanceof LabelFactor) {
        return -1;
      } else if (f instanceof VarFactor) {
        return -1;
      } else if (f instanceof InverseFactor) {
        return this.toString().compareTo(((InverseFactor) f).toString());
      } else {
        throw new RuntimeException("unsupported factor comparison");
      }
    }

    public int hashCode() {
      // TODO use sorted maps for consistent ordering
      return toString().hashCode();
    }

    public boolean equals(Object o) {
      if (! (o instanceof InverseFactor)) return false;

      InverseFactor that = (InverseFactor) o;
      return this.polynomial.equals(that.polynomial);
    }
  }

  static class Term {
    /** maps factors to their exponents */
    public Map<Factor, Integer> factors;

    public Term(Factor factor) {
      this.factors = new HashMap<Factor, Integer>();
      this.factors.put(factor, 1);
    }

    public Term(Map<Factor, Integer> factors) {
      this.factors = factors;
    }

    // nondestructive
    public Term times(Term with) {
      Map<Factor, Integer> left = this.factors;
      Map<Factor, Integer> right = with.factors;
      Map<Factor, Integer> newfactors = new HashMap<Factor, Integer>();

      // System.out.println("terms: " + this + " times " + right);
      for (Factor f : left.keySet()) {
        if (right.containsKey(f)) {
          // System.out.println("contains");
          int newexp = left.get(f) + right.get(f);

          // System.out.println("newexp: " + newexp);
          
          if (0 != newexp) {
            // only add the factor if the exponent is not 0
            newfactors.put(f, newexp);
          }
        } else {
          // System.out.println("not contains");
          newfactors.put(f, left.get(f));
        }
      }

      for (Factor f : right.keySet()) {
        if (! left.containsKey(f)) {
          newfactors.put(f, right.get(f));
        }
      }

      // System.out.println("new factors: " + new Term(newfactors));

      return new Term(newfactors);
    }

    /**
     * Compute degree by summing the exponents of each factor.
     */
    public int degree() {
      int degree = 0;
      for (Factor factor : factors.keySet()) {
        degree += factor.degree() * factors.get(factor);
      }
      return degree;
    }

    public int hashCode() {
      // TODO order the factors for consistency
      return toString().hashCode();
    }

    public boolean equals(Object o) {
      if (! (o instanceof Term)) return false;

      // System.out.println("t1");
      Term that = (Term) o;

      // System.out.println("t2");
      if (this.factors.size() != that.factors.size()) return false;

      // System.out.println("t3");
      for (Factor f : this.factors.keySet()) {
        // System.out.println("t4");
        if (! that.factors.containsKey(f)) return false;
        if (! this.factors.get(f).equals(that.factors.get(f))) return false;
      }
      // System.out.println("t5");

      return true;
    }

    public String toString() {
      List<String> fstrings = new ArrayList<String>();
      Comparator<Factor> factorComparator
        = new Comparator<Factor>() {
            public int compareTo(Factor a, Factor b) {
              return compare(a, b);
            }

            public int compare(Factor a, Factor b) {
              return a.printCompareTo(b);
            }
          };
      List<Factor> sortedFactors = new ArrayList<Factor>(factors.keySet());
      Collections.sort(sortedFactors, factorComparator);

      for (Factor factor : sortedFactors) {
        StringBuilder sb = new StringBuilder();
        Integer exponent = factors.get(factor);
        
        sb.append(factor.toString());
        if ((1 != exponent) || showAll) {
          sb.append("^");
          sb.append(exponent);
        }
        fstrings.add(sb.toString());
      }
      
      StringBuilder sb = new StringBuilder();
      String delim = "";

      for (String factor : fstrings) {
        sb.append(delim);
        sb.append(factor);
        delim = " * ";
      }

      return sb.toString();
    }
  }

  // destructive
  public Polynomial plus(Polynomial with) {
    for (Term term : with.terms.keySet()) {
      if (this.terms.containsKey(term)) {
        this.terms.put(term, this.terms.get(term) + with.terms.get(term));
      } else {
        this.terms.put(term, with.terms.get(term));
      }
    }

    return new Polynomial(this.terms, this.constant + with.constant);
  }

  // nondestructive
  public Polynomial times(Polynomial with) {
    /** maps terms to their coefficients */
    Map<Term, Double> newterms = new HashMap<Term, Double>();

    for (Term lterm : this.terms.keySet()) {
      newterms.put(lterm, this.terms.get(lterm) * with.constant);
    }

    for (Term rterm : with.terms.keySet()) {
      newterms.put(rterm, with.terms.get(rterm) * this.constant);
    }

    Double newconst = this.constant * with.constant;
    for (Term lterm : this.terms.keySet()) {
      for (Term rterm : with.terms.keySet()) {
        Term newterm = lterm.times(rterm);
        Double newcoeff =
          this.terms.get(lterm) * with.terms.get(rterm);

        if (newterm.factors.size() == 0) {
          // if the factors are now 1 (due to division), coeff is a
          // constant
          newconst = newconst + newcoeff;
        } else {
          newterms.put(newterm, newcoeff);
        }
      }
    }
    
    return new Polynomial(newterms, newconst);
  }

  // destructive
  public Polynomial negate() {
    for (Term term : this.terms.keySet()) {
      this.terms.put(term, Double.valueOf(-1) * this.terms.get(term));
    }
    
    this.constant = this.constant * Double.valueOf(-1);

    return this;
  }

  // nondestructive
  public Polynomial invert() {
    if (isZero(this.constant) &&
        this.terms.size() == 1) {
      // invert by making the exponent negative when there is only one
      // term, no constant, and all factors are variables
      Map<Term, Double> newterms = new HashMap<Term, Double>();
      for (Term term : this.terms.keySet()) {
        boolean allVars = false;
        Map<Factor, Integer> inverseFactors = new HashMap<Factor, Integer>();

        // check that all factors are var factors and invert them
        for (Factor factor : term.factors.keySet()) {
          if (factor.isVar()) {
            inverseFactors.put(factor, -1 * term.factors.get(factor));
            allVars = true;
          } else {
            allVars = false;
            break;
          }
        }
        if (allVars) {
          newterms.put(new Term(inverseFactors),
                       Double.valueOf(1) / this.terms.get(term));
          return new Polynomial(newterms, Double.valueOf(0));
        }

        // there is only one term
        break;
      }
      // fall-through
    } else if (! isZero(this.constant) &&
               this.terms.size() == 0) {
      // if the polynomial is just a constant, invert the integer
      // TODO support Double instead of doing int arithmetic
      return new Polynomial(Double.valueOf(1) / this.constant);
    }


    // invert by creating an inverted polynomial factor
    Map<Term, Double> newterms = new HashMap<Term, Double>();
    newterms.put(new Term(new InverseFactor(this)), Double.valueOf(1));
    return new Polynomial(newterms, Double.valueOf(0));
  }

  static DecimalFormat df = new DecimalFormat("#.##");

  public String toString() {
    List<String> tstrings = new ArrayList<String>();
    Comparator<Term> termComparator = new Comparator<Term>() {
        public int compareTo(Term a, Term b) {
          return compare(a, b);
        }

        public int compare(Term a, Term b) {
          int adeg = a.degree();
          int bdeg = b.degree();
          
          if (adeg == bdeg) {
            return b.toString().compareTo(a.toString());
          }
          
          return adeg - bdeg;
        }
      };
    List<Term> sortedTerms = new ArrayList<Term>(terms.keySet());
    Collections.sort(sortedTerms, termComparator);
    Collections.reverse(sortedTerms);

    for (Term term : sortedTerms) {
      Double coeff = terms.get(term);

      if (! isZero(coeff) || showAll) {
        StringBuilder sb = new StringBuilder();        

        if (! Double.valueOf(1).equals(coeff) || showAll) {
          sb.append(df.format(coeff));
          sb.append(" * ");
        }
        sb.append(term.toString());

        tstrings.add(sb.toString());
      }
    }

    StringBuilder sb = new StringBuilder();
    String delim = "";
    for (String term : tstrings) {
      sb.append(delim);
      sb.append(term);
      delim = "  +  ";
    }

    if (! isZero(constant) || tstrings.size() == 0 || showAll) {
      if (tstrings.size() > 0) {
        sb.append("  +  ");
      }
      sb.append(df.format(constant));
    }
    
    return sb.toString();
  }

  public Term maxTerm() {
    Term max_term = null;
    int max = 0;
    boolean isSet = false;
    for (Term term : terms.keySet()) {
      int term_degree = term.degree();
      if (! isSet || term_degree > max) {
        max_term = term;
        max = term_degree;
      }
    }
    return max_term;
  }
  
  public int degree() {
    Term term = maxTerm();
    if (null == term) {
      return 0;
    } else {
      return term.degree();
    }
  }

  public int compareTo(Polynomial with) {
    int adeg = this.degree();
    int bdeg = with.degree();
          
    if (adeg == bdeg) {
      if (0 == adeg) {
        return this.constant.compareTo(with.constant);
      } else {
        Term amax = this.maxTerm();
        Term bmax = with.maxTerm();
        return this.terms.get(amax).compareTo(with.terms.get(bmax));
      }
    }

    return adeg - bdeg;
  }

  public int hashCode() {
    // TODO ordered maps for consistent strings
    return toString().hashCode();
  }

  public boolean equals(Object o) {
    if (! (o instanceof Polynomial)) return false;

    // System.out.println("p1");
    Polynomial that = (Polynomial) o;
    // System.out.println("p2");
    if (! this.constant.equals(that.constant)) return false;
    // System.out.println("p3");
    if (this.terms.size() != that.terms.size()) return false;
    // System.out.println("p4");

    for (Term term : this.terms.keySet()) {
      // System.out.println("p5");
      if (! that.terms.containsKey(term)) return false;
      if (! this.terms.get(term).equals(that.terms.get(term))) return false;
    }
    // System.out.println("p6");

    return true;
  }

  public boolean isConstant() {
    return this.terms.keySet().size() == 0;
  }

  public boolean isZero() {
    return isConstant() && isZero(this.constant);
  }

  static boolean isZero(Double d) {
    return d == 0.0 || d == -0.0;
  }
}
