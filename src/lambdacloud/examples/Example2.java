package lambdacloud.examples;

import static symjava.symbolic.Symbol.*;
import static symjava.math.SymMath.*;
import lambdacloud.core.CloudConfig;
import lambdacloud.core.CloudFunc;
import lambdacloud.core.CloudSD;
import symjava.symbolic.Expr;

/**
 * In this example, we define a function on the fly.
 * 
 * double fun(double x, double y) {
 * 	return Math.sqrt(x*x + y*y);
 * }
 * 
 * It will be pushed to the cloud after initializing of the instance.
 * 
 * A cloud shared data 'input' is defined to be used as 
 * the parameter of the function. 
 * 
 * The newly defined function is called from local machine and
 * the return value is stored on the cloud named as 'output'.
 * We fetch the data 'output' and print it. 
 * 
 */
public class Example2 {

	public static void main(String[] args) {
		CloudConfig.setTarget("server");
		
		Expr expr = sqrt(x*x + y*y);
		CloudFunc f = new CloudFunc(new Expr[]{x, y}, expr);
		
		CloudSD input = new CloudSD("input").init(new double[]{3, 4});
		
		CloudSD output = new CloudSD("output").resize(1);
		f.apply(output, input);
		
		if(output.fetchToLocal()) {
			for(double d : output.getData()) {
				System.out.println(d);
			}
		}
	}
}
