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

    public static void solve(BigInteger num) {
		LazyCollatz ret = new LazyCollatz(num,Integer.MAX_VALUE);
		ret.topInstance=true;
		ret.mult=BigInteger.ZERO; //dont need to keep track of upstream changes for top level, saves a LOT of cycles
		ret.topSolve();
	}

	private void topSolve() {
		oddSteps=evenSteps=0;

		long statusUpdatePeriod = 15000;
		long startTime = System.currentTimeMillis();
		long reportTime = startTime+statusUpdatePeriod;

		evenSteps = n.getLowestSetBit(); //initial trailing zeroes
		if(evenSteps>0)
			n = n.shiftRight((int)evenSteps);

		while(!n.equals(BigInteger.ONE)) {
			//reporting
			long time = System.currentTimeMillis();
			if(time>reportTime){
				statusUpdatePeriod+=1000;
				reportTime=time+statusUpdatePeriod;
				System.out.println(" - solving... t="+(time-startTime)/1000+"s. Bits: "
						+ NumberFormat.getIntegerInstance().format(n.bitLength())+
						"  Steps: "+NumberFormat.getIntegerInstance().format(oddSteps+evenSteps));
			}

			bitBudget=Integer.MAX_VALUE;

			//the actual solve
			if(n.bitLength()>5000) //tune this
				partialSolveBig();
			else
				partialSolveSmall();
		}
	}

	private void subSolve(){
		while(bitBudget>0) {
			if(n.equals(BigInteger.ONE)){
				LazyCollatz manualStep = new LazyCollatz(BigInteger.ONE,0);
				manualStep.mult=BigInteger.valueOf(3);
				oddSteps++;
				incorporate(manualStep);
			}

			if (bitBudget > 700) //tune this
				partialSolveBig();
			else
				partialSolveSmall();
		}
	}


	//solves by solving a subproblem large enough to need chopping
	private void partialSolveBig(){
		BigInteger subN = chop();

		LazyCollatz ret = new LazyCollatz(subN,Math.min(lowerBytes*8,bitBudget));
		ret.subSolve();
		incorporate(ret);
	}

	//constants for how many bits to use in the small solver
	private static final int smallBitSize = 32; //max 39 so add doesn't overflow
	private static final long smallBitMask = (1L<<smallBitSize)-1;

	//solves by solving a subproblem small enough to do iteratively
	private void partialSolveSmall(){
		while(!(n.equals(BigInteger.ONE)&&topInstance) && bitBudget>0) {
			long x = n.longValue() & smallBitMask;

			long add = 0, mult = 1;
			int shr = 0;
			int limit = Math.min(bitBudget, smallBitSize);

			while (x > 0L && (shr<limit) && !(x == 1L && topInstance)){
				//we always have an odd step to start with
				x = x * 3 + 1;
				mult *= 3;
				add = add * 3 + (1L << shr);
				oddSteps++;

				//then some number of even steps
				int trail = Math.min(limit - shr, Long.numberOfTrailingZeros(x));

				x >>= trail;
				shr += trail;
			}


			//shorter version of the incorporate function, with special stuff to auto align to the right
			BigInteger bigmul = BigInteger.valueOf(mult);
			this.mult = this.mult.multiply(bigmul);
			n = n.multiply(bigmul).add(BigInteger.valueOf(add));

			int trail = Math.min(n.getLowestSetBit(),bitBudget); //trailing zeroes (also handles the rsh)
			evenSteps+=trail;
			bitBudget-=trail;
			this.shr += trail;
			n=n.shiftRight(trail);
		}
	}

	//splits off an appropriate amount of the lower bits of a number for smaller computation
	private BigInteger chop(){
		byte[] srcBytes = n.toByteArray();
		int len = srcBytes.length;
		
		lowerBytes=(int)(len*0.45f); //tune this

		byte[] choppedBytes = new byte[lowerBytes +1];
		byte[] remainingBytes = new byte[len- lowerBytes];

		System.arraycopy(srcBytes,srcBytes.length- lowerBytes,choppedBytes,1, lowerBytes);
		System.arraycopy(srcBytes,0,remainingBytes,0,len- lowerBytes);


		n = new BigInteger(remainingBytes);
		return new BigInteger(choppedBytes);
	}


	//incorporates the deferred operations from a recursive call into this instance
	private void incorporate(LazyCollatz sub){
		mult = mult.parallelMultiply(sub.mult);
		bitBudget-=sub.shr;
		shr += sub.shr;

		n=n.parallelMultiply(sub.mult).shiftLeft(lowerBytes *8-sub.shr).add(sub.n);

		int trail = Math.min(n.getLowestSetBit(),bitBudget); //trailing zeroes
		if(trail==0) return;

		n = n.shiftRight(trail);
		this.shr += trail;
		evenSteps += trail;

		if(!topInstance)
			bitBudget -= trail;
	}
}