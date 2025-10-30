import java.math.BigInteger;
import java.text.NumberFormat;

public class LazyCollatz {
	public static long oddSteps, evenSteps;

	//any number of collatz steps can be combined into a single function of the form (x*mult+add) >> shf
	//the "add" factor can be found directly from the result of the sub calculation
	//these constants represent those values for this particular sub operation
	private BigInteger n, mult;
	private int shr;

	private int bitBudget; //number of valid bits for the sub operation. If we shift right too far, we'll need the higher bits of the parent up
	private int lowerBytes; //number of bits trimmed off for lower operations
	private boolean topInstance = false;



	public LazyCollatz(BigInteger n, int bitBudget){
		this.n=n;
		this.bitBudget=bitBudget;
		mult = BigInteger.ONE;
		shr = 0;
		lowerBytes = 0;
	}

    public static LazyCollatz solve(BigInteger num) {
		LazyCollatz ret = new LazyCollatz(num,Integer.MAX_VALUE);
		ret.topInstance=true;
		ret.mult=BigInteger.ZERO; //dont need to keep track of upstream changes for top leve, saves lotsa perf
		ret.topSolve();
		return ret;
	}

	private void topSolve() {
		oddSteps=evenSteps=0;

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
			bitBudget=Integer.MAX_VALUE;
			partialSolve();
		}
	}

	private void solve(){
		while(bitBudget>0&&!n.equals(BigInteger.ONE))
			partialSolve();
	}

	//solves by choosing a strategy
	private void partialSolve(){
		if(n.equals(BigInteger.ZERO)){ //if lower bites are all zero, shift right for whole budget
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

			if(bitBudget==0) return;
		}

		if(bitBudget>700 && n.bitLength()>700) //tune this
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

			LazyCollatz manualStep = new LazyCollatz(BigInteger.ONE,0);
			manualStep.mult=BigInteger.valueOf(3);
			oddSteps++;
			incorporate(manualStep);
			subN = chop();
		}

		LazyCollatz ret = new LazyCollatz(subN,Math.min(subN.bitLength(),bitBudget));
		ret.solve();
		incorporate(ret);
	}

	//constants for how many bits to use in the small solver
	private static final int smallBitSize = 38;
	private static final long smallBitMask = (1L<<smallBitSize)-1;

	//solves by solving a subproblem small enough to do iteratively
	private void partialSolveSmall(){
		while(!n.equals(BigInteger.ONE) && bitBudget>0) {
			long x = n.longValue() & smallBitMask;

			long add = 0, mult = 1;
			int shr = 0;
			int limit = Math.min(bitBudget, smallBitSize);
			if(x==0)
				shr=limit;
			while ((shr < limit) && (add<=0x3fffffffffffffffL) && (x > 0L) ){
				if ((x & 1) == 0) {
					x >>= 1;
					shr++;
				} else {
					if (x == 1L && topInstance)
						break;
					x = x * 3 + 1;
					mult *= 3;
					add = add * 3 + (1L << shr);
					oddSteps++;
				}
			}

			evenSteps+=shr;

			//shorter version of the incorporate function
			BigInteger bigimul = BigInteger.valueOf(mult);
			this.mult = this.mult.multiply(bigimul);
			bitBudget-=shr;
			this.shr += shr;
			n = n.multiply(bigimul).add(BigInteger.valueOf(add)).shiftRight(shr);
		}
	}

	//splits off an appropriate amount of the lower bits of a number for smaller computation
	private BigInteger chop(){
		byte[] bytes = n.toByteArray();
		int len = bytes.length;
		
		lowerBytes =(int)(len*0.45f);

		byte[] choppedBytes = new byte[lowerBytes +1];
		byte[] remainingBytes = new byte[len- lowerBytes];
		
		System.arraycopy(bytes,bytes.length- lowerBytes,choppedBytes,1, lowerBytes);
		System.arraycopy(bytes,0,remainingBytes,0,len- lowerBytes);

		choppedBytes[0]=0;

		n = new BigInteger(remainingBytes);
		return new BigInteger(choppedBytes);
	}


	//incorporates the deferred operations from a recursive call into this instance
	private void incorporate(LazyCollatz sub){
		mult = mult.parallelMultiply(sub.mult);
		bitBudget-=sub.shr;
		shr += sub.shr;

		n=n.parallelMultiply(sub.mult).shiftLeft(lowerBytes *8-sub.shr).add(sub.n);
	}
}