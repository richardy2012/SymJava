package lambdacloud.core;

import java.util.Map;

import lambdacloud.core.graph.GraphBuilder;
import lambdacloud.core.graph.Node;
import symjava.symbolic.Expr;
import symjava.symbolic.Matrix;
import symjava.symbolic.Vector;
import symjava.symbolic.utils.Utils;

public class Session {
	public double run(Expr expr, Map<String, Double> dict) {
		CloudConfig.setGlobalTarget("job_local.conf");
		Node n = GraphBuilder.build(expr);
		return run(n, dict);
	}
	
	public CloudSD runVec(Node root, Map<String, double[]> dict) {
		int nArgs = root.args.size();
		CloudSD[] inputs = new CloudSD[nArgs];
		for(int i=0; i<nArgs; i++) {
			Expr arg = root.args.get(i);
			double[] d = dict.get(arg.toString());
			if(d == null && arg.getParent() != null) {
				d = dict.get(arg.getParent().toString());
				if(d != null) {
					if(arg instanceof Matrix) {
						//extract sub-matrix from parent matrix
						Matrix m = (Matrix)arg;
						Matrix p = (Matrix)arg.getParent();
						Jama.Matrix mat = new Jama.Matrix(d, p.nRow);
						d = mat.getMatrix(m.nRowStart, m.nRowStart+m.nRow-1, m.nColStart, m.nColStart+m.nCol-1).getColumnPackedCopy();
					} else if (arg instanceof Vector) {
						//extract sub-vector from parent vector
						Vector m = (Vector)arg;
						Vector p = (Vector)arg.getParent();
						Jama.Matrix mat = new Jama.Matrix(d, p.nDim);
						d = mat.getMatrix(m.nStart, m.nStart+m.nDim-1, 0, 0).getColumnPackedCopy();
					}
				}
			}
			if(d == null) {
				Node child = root.children.get(root.args.get(i).toString());
				CloudSD ret = runVec(child, dict);
				inputs[i] = ret;
			} else {
				inputs[i] = new CloudSD(arg.toString()).init(d);
			}
		}
		CloudSD output = new CloudSD();//"output").resize(4); //TODO

		root.cfunc.apply(output, inputs);
		System.out.println("run: "+root);
		if(output.fetchToLocal()) {
			for(double d : output.getData()) {
				System.out.println(d);
			}
		}
		return output;
	}
	
	/**
	 * TODO
	 * run() return a double
	 * run() could return an array of Tensor
	 * 
	 * @param root
	 * @param dict
	 * @return
	 */
	public double run(Node root, Map<String, Double> dict) {
		double[] args = new double[root.args.size()];
		for(int i=0; i<root.args.size(); i++) {
			Double d = dict.get(root.args.get(i).toString());
			if(d == null) {
				Node child = root.children.get(root.args.get(i).toString());
				args[i] = run(child, dict);
			} else {
				args[i] = d;
			}
		}
		CloudSD input = new CloudSD("input").init(args);
		CloudSD output = new CloudSD("output").resize(1);
		root.cfunc.apply(output, input);
		if(output.fetchToLocal()) {
			for(double d : output.getData()) {
				System.out.println(d);
			}
		}
		
		return output.getData(0);
	}
	
	public double runLocal(Expr expr, Map<String, Double> dict) {
		Node n = GraphBuilder.build(expr);
		return runLocal(n, dict);
	}
	
	public double runLocal(Node root, Map<String, Double> dict) {
		double[] args = new double[root.args.size()];
		for(int i=0; i<root.args.size(); i++) {
			Double d = dict.get(root.args.get(i).toString());
			if(d == null) {
				Node child = root.children.get(root.args.get(i).toString());
				args[i] = run(child, dict);
			} else {
				args[i] = d;
			}
		}
		return root.func.apply(args);
	}
}