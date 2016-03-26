package symjava.symbolic;

import java.util.List;

import com.sun.org.apache.bcel.internal.generic.ClassGen;

import symjava.bytecode.BConstant;
import symjava.bytecode.BytecodeFunc;
import symjava.symbolic.arity.NaryOp;
import symjava.symbolic.utils.BytecodeUtils;
import symjava.symbolic.utils.FuncClassLoader;
import symjava.symbolic.utils.Utils;

public class Func extends NaryOp {
	protected Expr expr;

	/**
	 * Construct an abstract function
	 * Note: For an abstract function with only one argument, define it by
	 * passing an array for the second parameter
	 * 
	 *   Func f = new Func("F", new Expr[]{Symbol.x}); // F(x)
	 *   
	 * @param name
	 * @param args
	 */
	public Func(String name, Expr ...args) {
		super(args);
		this.expr = null;
		this.label = name;
		this.sortKey = label;
	}
	
	/**
	 * Construct a function with expression expr
	 * For example: 
	 *    Func f = new Func("F", Symbol.x); // F(x)=x
	 * @param name
	 * @param expr
	 */
	public Func(String name, Expr expr) {
		super(new Expr[] {expr});
		//Extract free variables from expr
		this.expr = expr;
		this.args = Utils.extractSymbols(expr).toArray(new Expr[0]);
		this.label = name;
		this.sortKey = label;
	}
	
	public Func(String name, Expr expr, Expr[] args) {
		super(args);
		//Extract free variables from expr
		this.expr = expr;
		this.label = name;
		this.sortKey = label;
	}
	
	public String getName() {
		return label;
	}
	
	public Expr getExpr() {
		return this.expr;
	}
	
	@Override
	public boolean isAbstract() {
		return expr == null;
	}
	
	public BytecodeFunc toBytecodeFunc() {
		return toBytecodeFunc(false, false);
	}
	
	public BytecodeFunc toBytecodeFunc(boolean isWriteFile, boolean staticMethod) {
		try {
			if(!isWriteFile) {
				if(this.expr instanceof SymReal<?>) {
					SymReal<?> r = (SymReal<?>)this.expr;
					return new BConstant(r.getValue().doubleValue());
				}
			}
			/**
			 * Return an instance of BytecodeFunc generated by this Func without writing a class file to disk.
			 */
			FuncClassLoader<BytecodeFunc> fcl = new FuncClassLoader<BytecodeFunc>();
			ClassGen genClass = BytecodeUtils.genClassBytecodeFunc(this, isWriteFile, staticMethod);
			return fcl.newInstance(genClass);
			//return (BytecodeFunc)Class.forName("symjava.bytecode."+this.label).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String toString() {
		if(expr != null)
			return expr.toString();
		else
			return getLabel();
	}
	
	public String getLabel() {
		if(args == null || args.length == 0)
			return label;
		else
			return label+"("+Utils.joinLabels(args, ",")+")";
	}

	@Override
	public Expr diff(Expr expr) {
		if(Utils.symCompare(this, expr)) {
			return Symbol.C1;
		} else if(this.containsArg(expr)) {
			if(this.expr != null)
				return this.expr.diff(expr);
			return new Derivative(this, expr);
		} else {
			return Symbol.C0;
		}
	}
	
	public boolean containsArg(Expr arg) {
		if(arg != null) {
			for(Expr e : args) {
				boolean b = Utils.symCompare(e, arg);
				if(b) return true;
			}
		}
		return false;
	}

	@Override
	public Expr subs(Expr from, Expr to) {
		if(Utils.symCompare(this, from))
			return to;
		else if(this.expr != null)
			return new Func(this.label, this.expr.subs(from, to));
		else
			return this;
	}
	
	@Override
	public Expr simplify() {
		if(this.isSimplified)
			return this;
		if(expr != null) {
			Func f = new Func(label, expr.simplify());
			f.args = this.args;
			f.isSimplified = true;
			return f;
		}
		this.isSimplified = true;
		return this;
	}

	@Override
	public boolean symEquals(Expr other) {
		if(other instanceof Func) {
			Func o = (Func)other;
			//TODO support map(expr, [e1,e2,e3])
			if(this.label.equals("_") && other.label.equals("_"))
				return true;
			if(this.label.equals("__") && other.label.equals("__"))
				return true;
			Boolean rlt = Utils.symCompareNull(this.expr, o.expr);
			if(rlt != null && rlt == false)
				return false;
			if(!this.label.equals(o.label))
				return false;
			if(args.length != o.args.length)
				return false;
			for(int i=0; i<args.length; i++) {
				if(!args[i].symEquals(o.args[i]))
					return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public void flattenAdd(List<Expr> outList) {
		if(expr != null)
			expr.flattenAdd(outList);
		else
			outList.add(this);
	}

	@Override
	public void flattenMultiply(List<Expr> outList) {
		if(expr != null)
			expr.flattenMultiply(outList);
		else
			outList.add(this);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return expr.getTypeInfo();
	}

	@Override
	public void updateLabel() {
		// TODO Auto-generated method stub
		
	}

}
