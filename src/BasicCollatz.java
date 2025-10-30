import java.math.BigInteger;

public class BasicCollatz {

	public static void solve(BigInteger n){
		long startTime = System.currentTimeMillis();

		int oddSteps = 0;
		int evenSteps = 0;
		while (!n.equals(BigInteger.ONE)){
			long x = n.longValue()&0x00000000FFFFFFFFL;
			int mult = 1;
			int add = 0;
			int shr = 0;
			while ((shr<19) && ((add&0xe000000000000000L)==0) && ((x&0xe000000000000000L)==0) && x!=1L){
				if((x&1)==0){
					x>>=1;
					shr++;
					evenSteps++;
				}else{
					x = x*3+1;
					mult*=3;
					add=add*3+(1<<shr);
					oddSteps++;
				}
			}
			n=n.multiply(BigInteger.valueOf(mult)).add(BigInteger.valueOf(add)).shiftRight(shr);
		}
		System.out.println("Steps: "+(oddSteps+evenSteps));
		System.out.println("Even percentage: "+(100*(double)evenSteps/(evenSteps+oddSteps)));
		System.out.println("Time Taken: "+(System.currentTimeMillis()-startTime)+"ms");
	}
}