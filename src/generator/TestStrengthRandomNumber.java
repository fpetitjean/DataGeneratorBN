package generator;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.inference.ChiSquareTest;

public class TestStrengthRandomNumber {

	public static void main(String[] args) throws NoSuchAlgorithmException {
//		Generating data from 2 boolean variable then checking if passes independence test

		final int N = 100;
		int N_EXP = 1000000;

		System.out.println("Mersenne Twister");
		MersenneTwister rand = new MersenneTwister();
		int nErrors = 0;
		ChiSquareTest test = new ChiSquareTest();
		double[] expected = new double[] { N / 4.0, N / 4.0 , N / 4.0, N / 4.0  };
		long[] observed = new long[4];// A x B
		for (int e = 0; e < N_EXP; e++) {

			Arrays.fill(observed, 0);
			for (int i = 0; i < N; i++) {
				int A = (rand.nextBoolean()) ? 1 : 0;
				int B = (rand.nextBoolean()) ? 1 : 0;
				observed[A*2+B]++;
			}

			double p = test.chiSquareTest(expected, observed);
//			System.out.println("p = "+p);
			if (p <= .05) {
				nErrors++;
			}
		}
		double fwer = 1.0 * nErrors / N_EXP;
		System.out.println("FWER= " + fwer);

		System.out.println("SHA1PRNG");
		SecureRandom srg = SecureRandom.getInstance("SHA1PRNG");
		nErrors = 0;
		for (int e = 0; e < N_EXP; e++) {

			Arrays.fill(observed, 0);
			for (int i = 0; i < N; i++) {
				int A = (srg.nextBoolean()) ? 1 : 0;
				int B = (srg.nextBoolean()) ? 1 : 0;
				observed[A*2+B]++;
			}

			double p = test.chiSquareTest(expected, observed);
			if (p <= .05) {
				nErrors++;
			}
		}
		fwer = 1.0 * nErrors / N_EXP;
		System.out.println("FWER= " + fwer);

	}

}
