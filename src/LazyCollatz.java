import java.io.*;
import java.math.BigInteger;
import java.text.NumberFormat;

public class LazyCollatz {
	private boolean topInstance = false;
	private BigInteger n;
	//any number of collatz steps can be combined into a single function of the form (x*mult+add) >> shf
	//these constants represent those values for this particular sub operation
	private BigInteger mult;
	private BigInteger add;
	private int shr;

	public long oddSteps, evenSteps;

	private int bitBudget; //number of valid bits for the sub operation. If we shift right too far, we'll need the higher bits of the parent up

	public LazyCollatz(BigInteger n, int bitBudget){
		this.n=n;
		this.mult = BigInteger.ONE;
		this.add = BigInteger.ZERO;
		this.shr = 0;
		this.oddSteps = 0;
		this.evenSteps = 0;
		this.bitBudget=bitBudget;
	}

	//public facing method
    public static LazyCollatz solve(BigInteger num) {
		LazyCollatz ret = new LazyCollatz(num,Integer.MAX_VALUE);
		ret.topInstance=true;
		ret.topSolve();
		return ret;
	}

	private void topSolve() {
		long statusUpdatePeriod = 15000;
		long startTime = System.currentTimeMillis();
		long reportTime = startTime+statusUpdatePeriod;

		while(!n.equals(BigInteger.ONE)) {
			long time = System.currentTimeMillis();
			if(time>reportTime){
				statusUpdatePeriod+=1000;
				reportTime=time+statusUpdatePeriod;
				System.out.println(" - solving... t="+(time-startTime)/1000+"s. Bits: "
						+ NumberFormat.getIntegerInstance().format(n.bitLength())+
						"  Steps: "+NumberFormat.getIntegerInstance().format(oddSteps+evenSteps));
			}
			partialSolve();
		}
	}

	private void solve(){
		while(bitBudget>0&&!n.equals(BigInteger.ONE))
			partialSolve();
	}

	//solves by choosing a strategy
	private void partialSolve(){
		if(n.equals(BigInteger.ZERO)){
			shr+=bitBudget;
			evenSteps+=bitBudget;
			bitBudget=0;
			return;
		}

		int x = Math.min(n.getLowestSetBit(),bitBudget); //trailing zeroes

		if(x>0) {
			n = n.shiftRight(x);
			if(!topInstance)
				bitBudget -= x;
			shr += x;
			evenSteps += x;

		}

		if(bitBudget==0)
			return;


		if(bitBudget>700 && n.bitLength()>1000) //tune this
			partialSolveBig();
		else
			partialSolveSmall();

	}

	//solves by solving a subproblem large enough to need chopping
	private void partialSolveBig(){
		BigInteger subN = chop();
		while(subN.equals(BigInteger.ONE)){
			if(n.equals(BigInteger.ONE))
				break;
			incorporate(3,1,0,1,0);
			subN = chop();
		}

		LazyCollatz ret = new LazyCollatz(subN,Math.min(subN.bitLength(),bitBudget));
		ret.solve();
		incorporate(ret);
	}

	private static final int smallBitSize = 38;
	private static final long smallBitMask = (1L<<smallBitSize)-1;

	//solves by solving a subproblem small enough to do iteratively
	private void partialSolveSmall(){
		while(!n.equals(BigInteger.ONE) && bitBudget>0) {
			long x = n.longValue() & smallBitMask;

			long add = 0;
			long mult = 1;
			int shr = 0;
			int oddSteps = 0;
			int limit = Math.min(bitBudget, smallBitSize);
			if(x==0)
				shr=limit;
			while ((shr < limit) && (add<=0x3fffffffffffffffL) && (x > 0L) ){
				if ((x & 1) == 0) {
					x >>= 1;
					shr++;
				} else {
					if (x == 1L)
						if (topInstance)
							break;
					x = x * 3 + 1;
					mult *= 3;
					add = add * 3 + (1L << shr);
					oddSteps++;
				}
			}

			incorporate(mult, add, shr, oddSteps, shr);
		}
	}

	//splits off an appropriate amount of the lower bits of a number for smaller computation
	private BigInteger chop(){
		byte[] bytes = n.toByteArray();
		int len = bytes.length;

		int partialLen; //Bunch of magic heuristics to determine how to scale the recursion
		if(len<3000) {
			partialLen = (int)Math.sqrt(len);
		}else{
			float k = ((float) Math.log(len))*.3f-0.15f;
			k=Math.min(k,0.75f);

			if(topInstance) {
				if (len > 30000)
					k *= 0.3f;
				else k*=0.8f;
			}

			partialLen=(int)(len*k);
		}

		byte[] choppedBytes = new byte[partialLen+1];
		System.arraycopy(bytes,bytes.length-partialLen,choppedBytes,1,partialLen);
		choppedBytes[0]=0;
		return new BigInteger(choppedBytes);
	}


	//incorporates the operations from a recursive call into this instance
	private void incorporate(LazyCollatz sub){
		if(!topInstance) {
			add = add.parallelMultiply(sub.mult).add(sub.add.shiftLeft(shr));
			mult = mult.parallelMultiply(sub.mult);
			shr += sub.shr;
			bitBudget-=sub.shr;

		}
		oddSteps+=sub.oddSteps;
		evenSteps+=sub.evenSteps;
		if(bitBudget>0)
			n=n.parallelMultiply(sub.mult).add(sub.add).shiftRight(sub.shr);;
	}

	//same thing but it takes the parameters directly
	private void incorporate(long imult, long iadd, int ishr, int odd, int even){
		BigInteger bigimul = BigInteger.valueOf(imult);
		BigInteger bigiadd = BigInteger.valueOf(iadd);
		if(!topInstance) {
			add = add.multiply(bigimul).add(bigiadd.shiftLeft(shr));
			mult = mult.multiply(bigimul);
			shr += ishr;
			bitBudget-=ishr;
		}
		oddSteps+=odd;
		evenSteps+=even;

		if(bitBudget>0)
			n = n.multiply(bigimul).add(bigiadd).shiftRight(ishr);
	}
}