import java.math.BigInteger;
import java.util.ArrayList;

public class BigIntParse {

	enum TokenType {plus,minus,mult,pow,parenOpen,parenClosed,num, shl,factorial}

	TokenType type;
	BigInteger value;
	long lShift = 0;

	private BigIntParse(TokenType t){
		this.type=t;
	}

	public BigIntParse(BigInteger value){
		this.type=TokenType.num;
		this.value=value;
	}

	public BigIntParse(int value){
		this(BigInteger.valueOf(value));
	}

	public static BigIntParse parse(String s){
		return resolve(tokenize(s));
	}

	private static ArrayList<BigIntParse> tokenize(String s){
		ArrayList<BigIntParse> ret = new ArrayList<>();
		for(char c : s.toCharArray()){
			switch (c){
				case '+':
					ret.add(new BigIntParse(TokenType.plus));
					break;
				case '-':
					ret.add(new BigIntParse(TokenType.minus));
					break;
				case '*':
					ret.add(new BigIntParse(TokenType.mult));
					break;
				case '^':
					ret.add(new BigIntParse(TokenType.pow));
					break;
				case '(':
					ret.add(new BigIntParse(TokenType.parenOpen));
					break;
				case ')':
					ret.add(new BigIntParse(TokenType.parenClosed));
					break;
				case '<':
					if (!ret.isEmpty()) {
						BigIntParse prev = ret.getLast();
						if (prev.type == TokenType.shl) {
							break;
						}
					}
					ret.add(new BigIntParse(TokenType.shl));
					break;
				case '!':
					ret.add(new BigIntParse(TokenType.factorial));
					break;
				default:
					if(Character.isDigit(c)) {
						int val = Integer.parseInt(String.valueOf(c));


						if (!ret.isEmpty()) {
							BigIntParse prev = ret.getLast();
							if (prev.type == TokenType.num) {
								prev.value = prev.value.multiply(BigInteger.TEN).add(BigInteger.valueOf(val));
								break;
							}
						}

						ret.add(new BigIntParse(val));
						break;
					}
			}
		}

		return ret;
	}

	public static BigIntParse resolve(ArrayList<BigIntParse> tokens){
		ArrayList<BigIntParse> subList = new ArrayList<>();

		for (int i = 0; i < tokens.size(); i++) {
			BigIntParse tok = tokens.get(i);
			if(tok.type==TokenType.parenOpen){
				tokens.remove(i);
				int depth = 1;
				while(true){
					if(i>= tokens.size())	throw new RuntimeException("paren not terminated");

					BigIntParse subExprToken = tokens.remove(i);
					if(subExprToken.type==TokenType.parenOpen)
						depth++;
					else if(subExprToken.type==TokenType.parenClosed)
						depth--;

					if(depth<=0)
						break;
					else
						subList.add(subExprToken);
				}
				tokens.add(i,resolve(subList));
				subList.clear();
			}
		}

		for (int i = tokens.size()-2; i>=1; i--) {
			BigIntParse tok = tokens.get(i);
			if(tok.type==TokenType.pow){
				BigIntParse R = tokens.remove(i+1);
				tokens.remove(i);
				BigIntParse L = tokens.get(i-1);
				if(R.type!=TokenType.num || L.type!=TokenType.num)	throw new RuntimeException("Weird pow args");

				L.value=bigIntPow(L.getValue(),R.getValue());
			}
		}

		for (int i = tokens.size()-1; i>=1; i--) {
			BigIntParse tok = tokens.get(i);
			if(tok.type==TokenType.factorial){
				tokens.remove(i);
				BigIntParse L = tokens.get(i-1);
				if(L.type!=TokenType.num)	throw new RuntimeException("Weird factorial args");

				L.value=factorial(L.getValue());
			}
		}

		for (int i = 1; i < tokens.size()-1;) {
			BigIntParse tok = tokens.get(i);
			if(tok.type==TokenType.mult){
				BigIntParse R = tokens.remove(i+1);
				tokens.remove(i);
				BigIntParse L = tokens.get(i-1);
				if(R.type!=TokenType.num || L.type!=TokenType.num)	throw new RuntimeException("Weird mult args");

				L.value=L.getValue().parallelMultiply(R.getValue());
			}else {
				i++;

			}
		}

		for (int i = 1; i < tokens.size()-1;) {
			BigIntParse tok = tokens.get(i);
			if(tok.type==TokenType.plus || tok.type==TokenType.minus){
				BigIntParse R = tokens.remove(i+1);
				tokens.remove(i);
				BigIntParse L = tokens.get(i-1);
				if(R.type!=TokenType.num || L.type!=TokenType.num)	throw new RuntimeException("Weird add/sub args");

				if(tok.type==TokenType.plus)
					L.value=L.getValue().add(R.getValue());
				else
					L.value=L.getValue().subtract(R.getValue());
			}else {
				i++;

			}
		}

		for (int i = tokens.size()-2; i>=1; i--) {
			BigIntParse tok = tokens.get(i);
			if(tok.type==TokenType.shl){
				BigIntParse R = tokens.remove(i+1);
				tokens.remove(i);
				BigIntParse L = tokens.get(i-1);
				if(R.type!=TokenType.num || L.type!=TokenType.num)	throw new RuntimeException("Weird shift left args");
				L.lShift+=R.getValue().longValueExact();
			}
		}

		if (tokens.size()!=1)
			throw new RuntimeException("Expression not fully resolved, "+tokens.size()+" terms present");

		return tokens.getFirst();
	}

	private static BigInteger bigIntPow(BigInteger l, BigInteger r){
		if(l.bitLength()<1000){
			if(l.bitCount()==1){
				//we're a power of two, yay
				int e = l.getLowestSetBit();
				// our output is of the form (2^e)^r or 1<<(e*r)
				int shiftr=r.parallelMultiply(BigInteger.valueOf(e)).intValueExact();

				return BigInteger.ONE.shiftLeft(shiftr);
			}
		}

		try {
			int pow = r.intValueExact();
			return l.pow(pow);
		}catch (ArithmeticException e){
			throw new RuntimeException("power too big");
		}
	}

	private static BigInteger factorial(BigInteger num){
		return factorial(num,BigInteger.TWO);
	}

	private static BigInteger factorial(BigInteger upper,BigInteger lower) {
		BigInteger diff = upper.subtract(lower);
		if(diff.bitLength()<5) {
			long u = upper.longValueExact();
			long l = lower.longValueExact();
			BigInteger ret = lower;

			while (u > l) {
				ret = ret.multiply(BigInteger.valueOf(u));
				u--;
			}
			return ret;

		}

		BigInteger split = upper.add(lower).shiftRight(1);
		return factorial(upper, split.add(BigInteger.ONE)).multiply(factorial(split, lower));
	}

	private BigInteger getValue(){
		if(lShift>Integer.MAX_VALUE)
			throw new RuntimeException("Shift too big");
		if(lShift>0)
			value=value.shiftLeft((int)lShift);
		lShift=0;
		return value;
	}
}