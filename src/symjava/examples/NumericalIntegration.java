package symjava.examples;

import symjava.symbolic.*;
import static symjava.math.SymMath.*;
import static symjava.symbolic.Symbol.*;
import symjava.bytecode.BytecodeFunc;
import symjava.domains.Domain;
import symjava.domains.Domain2D;
import symjava.domains.Interval;
import symjava.symbolic.utils.JIT;

public class NumericalIntegration {

	public static void main(String[] args) {
		test_1D();
		test_2D();
	}
	
	public static void test_1D() {
		//Define the interal
		Domain I = Interval.apply(-10, 0).setStep(0.01);
		//Define the integral: cumulative distribution function
		Expr cdf = Integrate.apply(exp(-0.5*pow(x,2))/sqrt(2*PI), I);
		System.out.println(cdf); //\int_{-10.0}^{10.0}{1/\sqrt{2*\pi}*e^{-0.5*x^2}}dx
		
		//Compile cdf to perform numerical integration
		BytecodeFunc f = JIT.compile(cdf);
		System.out.println(f.apply()); //1.0
	}
	
	public static void test_2D() {
		//http://turing.une.edu.au/~amth142/Lectures/Lecture_14.pdf
		/*
		Example:
		We will evaluate the integral
			I = \int_{\Omega} sin(sqrt(log(x+y+1))) dxdy
		where \Omega is the disk
			(x-1/2)^2 + (y-1/2)^2 <= 1/4
		Since the disk Ω is contained within the square [0, 1] × [0, 1], we can
		generate x and y as uniform [0, 1] random numbers, and keep those which
		lie in the disk Ω.
		
		Matlab code:
		function ii = monte2da(n)
			k = 0 // count no. of points in disk
			sumf = 0 // keep running sum of function values
			while (k < n) // keep going until we get n points
				x = rand(1,1)
				y = rand(1,1)
				if ((x-0.5)^2 + (y-0.5)^2 <= 0.25) then // (x,y) is in disk
					k = k + 1 // increment count
					sumf = sumf + sin(sqrt(log(x+y+1))) // increment sumf
				end
			end
			ii = (%pi/4)*(sumf/n) // %pi/4 = volume of disk
		endfunction
		-->monte2da(100000)
		ans =
		0.5679196
		*/
		Domain omega = new Domain2D("\\Omega", x,y);
		Expr ii = Integrate.apply(sin(sqrt(log(x+y+1))), omega);
		System.out.println(ii);
		
		
		
	}

}