package symjava.symbolic.arity;

import symjava.symbolic.Expr;

public abstract class UnaryOp extends Expr {
	public Expr arg;
	
	public UnaryOp(Expr arg) {
		this.arg = arg;
		setSimplifyOps(
				arg.getSimplifyOps()
				);
	}

	@Override
	public Expr[] args() {
		return new Expr[] { arg };
	}
}
