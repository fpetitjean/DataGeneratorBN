package generator;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.inference.ChiSquareTest;

public class TestStrengthRandomNumber {

	public static void main(String[] args) throws NoSuchAlgorithmException {
//		Generating data from 2 boolean variables then checking correlation
		
		final int N = 10000;
		int N_EXP = 100000;
		
		System.out.println("Mersenne Twister");
		MersenneTwister rand = new MersenneTwister();
		int nErrors = 0;
		for (int e = 0; e < N_EXP; e++) {
			
			long[][] contingency = new long[2][2];//A x B
			
			for (int i = 0; i < N; i++) {
				int A = (rand.nextBoolean())?1:0;
				int B = (rand.nextBoolean())?1:0;
				contingency[A][B]++;
				
			}
			
//			for (int i = 0; i < contingency.length; i++) {
//				for (int j = 0; j < contingency[i].length; j++) {
//					System.out.print(contingency[i][j]+"\t");
//				}
//				System.out.println();
//			}
//			
			ChiSquareTest test = new ChiSquareTest();
			double p  = test.chiSquareTest(contingency);
//			System.out.println("p = "+p);
			if(p<=.05) {
				nErrors ++;
			}
		}
		double fwer = 1.0*nErrors/N_EXP;
		System.out.println("FWER= "+fwer);
		
		System.out.println("SHA1PRNG");
		SecureRandom secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
		nErrors = 0;
		for (int e = 0; e < N_EXP; e++) {
			
			long[][] contingency = new long[2][2];//A x B
			
			for (int i = 0; i < N; i++) {
				int A = (secureRandomGenerator.nextBoolean())?1:0;
				int B = (secureRandomGenerator.nextBoolean())?1:0;
				contingency[A][B]++;
				
			}
			
			ChiSquareTest test = new ChiSquareTest();
			double p  = test.chiSquareTest(contingency);
			if(p<=.05) {
				nErrors ++;
			}
		}
		fwer = 1.0*nErrors/N_EXP;
		System.out.println("FWER= "+fwer);

	}

}
