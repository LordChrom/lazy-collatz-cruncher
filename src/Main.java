import java.math.BigInteger;
import java.text.NumberFormat;

public class Main {
	public static void main(String[] args) {
		if(args.length==0){
			timedSolve("10^(10^6)"); //17,805,058, 0.34sec
			timedSolve("3^10^7<<3*9^9"); //1,276,807,624, 3.42sec   (canonically 8^9^9*3^10^7)

//			timedSolve("2^2^24-1"); //225,782,649, 8.97sec
//			timedSolve("2^2^20+1"); //7,601,041, 34.6sec

//			timedSolve("(2^2^30-1)<<(3*10^11)"); //514,448,571,769, 6572sec

		}

		for(String s : args){
			timedSolve(s);
		}
	}


	public static void timedSolve(String s){
		System.out.println("\nNumber:\n"+s);
		long startTime = System.currentTimeMillis();
		BigIntParse n = BigIntParse.parse(s);
		System.out.println("Parse time: "+(System.currentTimeMillis()-startTime)*0.001d+"s");

		timedSolve(n);
	}

	public static void timedSolve(BigInteger n) {
		timedSolve(new BigIntParse(n));
	}

	public static void timedSolve(BigIntParse p){
		NumberFormat nf = NumberFormat.getIntegerInstance();

		BigInteger n = p.value;
		long preShift = p.lShift;
		if(preShift>0)
			System.out.println("Pre-factored PowOf2 steps: "+nf.format(p.lShift)); //keeps memory limitations down


		long bitlen = n.bitLength();
		String gbstr = ""+(bitlen / 8000000);
		gbstr=gbstr.substring(0,Math.min(5,gbstr.length()));
		System.out.println("Bits: "+nf.format(bitlen)+" (" + gbstr + " MB)");

		if(n.bitLength()<100000)
			System.out.println(n);

		long startTime = System.currentTimeMillis();

		LazyCollatz.solve(n);

		LazyCollatz.evenSteps+=preShift;

		long totalLen = LazyCollatz.oddSteps+LazyCollatz.evenSteps;
		System.out.println("Path Len: "+nf.format(totalLen+1));
		System.out.println("Even: "+nf.format(LazyCollatz.evenSteps)+"    Odd: "+nf.format(LazyCollatz.oddSteps));
		System.out.println("Even percentage: "+(100*(double)LazyCollatz.evenSteps/totalLen));
		System.out.println("Solve Time:"+(System.currentTimeMillis()-startTime)*0.001d+"s");
	}
}