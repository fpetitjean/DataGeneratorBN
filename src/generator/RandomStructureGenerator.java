package generator;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.FastMath;
import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.util.BayesNodeUtil;

public class RandomStructureGenerator {

	int nVariables;
	int nDataPoints;
	double alphaDirichlet;
	int maxNParents;
	int maxNValuesPerNode;

	RandomDataGenerator r;
	BayesNet bn;
	BayesNode[] nodes;
	//eliminationOrdering
	int[]eo;
	
	/**
	 * Creates the generator
	 * @param nVariables number of nodes in the BN
	 * @param maxNParents Maximum number of parents per nodes - sampled from U(0,maxNParents) 
	 * @param maxNValuesPerNode Maximum number of outcome per nodes - sampled from U(2,maxNValuesPerNode)
	 * @param nDataPoints Number of samples to generate
	 * @param alphaDirichlet will sample each multinomial in the CPT (ie line) from Dir(alphaDirichlet). The highest value of alpha, the harder it will be to detect the correlations. 
	 * @param seed Seed to pass to one of the random generator
	 */
	public RandomStructureGenerator(int nVariables, int maxNParents,int maxNValuesPerNode,int nDataPoints, double alphaDirichlet, long seed) {
		this.nVariables = nVariables;
		this.nDataPoints = nDataPoints;
		this.maxNParents = maxNParents;
		this.maxNValuesPerNode = maxNValuesPerNode;
		this.alphaDirichlet = alphaDirichlet; 

		RandomGenerator rg = new JDKRandomGenerator();
		rg.setSeed(seed);
		this.r = new RandomDataGenerator(rg);
		this.bn = null;
	}

	public void generateRandomStructure() {
		RandomDataGenerator rdg = new RandomDataGenerator();

		bn = new BayesNet();
		nodes = new BayesNode[nVariables];
		for (int i = 0; i < nVariables; i++) {
			nodes[i] = bn.createNode("n"+(i+1));
			int nValues = r.nextInt(2, maxNValuesPerNode);
			for (int j = 0; j < nValues; j++) {
				nodes[i].addOutcome("s"+j);
			}
		}
		
		eo = rdg.nextPermutation(nVariables, nVariables);

		for (int i = 1; i < eo.length; i++) {
			
			BayesNode child = nodes[eo[i]];
			// looking for the parents of node eo[i]
			int nParents = r.nextInt(0, FastMath.min(i, maxNParents));
			if(nParents>0){
				int[] parentsIDsInEO = r.nextPermutation(i, nParents);
				
				ArrayList<BayesNode>parents = new ArrayList<>();
				for(int eoID:parentsIDsInEO){
					BayesNode parent = nodes[eo[eoID]];
					parents.add(parent);
				}
				child.setParents(parents);
			}
		}
		System.out.println("Generated BN structure as follows:");
		System.out.println(toString());
	}

	public String toString() {
		String str = "BN with "+nVariables+" nodes\nnode_name:\n"
				+ "\toutcome_1,...,outcome_v\n"
				+ "\tparent_1,...,parent_k\n";
		
		for (int eoIndex:eo) {
			BayesNode child = nodes[eo[eoIndex]];
			str+=child.getName()+":\n";
			str+="\toutcomes: "+child.getOutcomes().toString()+"\n";
			str+="\tparents: ";
			List<BayesNode> parents = child.getParents();
			if(parents.size()>0){
				str+=parents.toString()+"\n";
			}else{
				str+="no parents\n";
			}
		}
		return str;
	}

	public void generateDataset(File arffFile) {
		if(bn==null){
			generateRandomStructure();
		}

		try {
			generateRandomCPTs(bn, alphaDirichlet);
			if (arffFile.exists()) {
				arffFile.delete();
			}
			generateSecureRandomCasesFileArff(arffFile);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

	}
	
	public void generateRandomCPTs(BayesNet net, double hardness){
		List<BayesNode> allNodes = net.getNodes();
		// ~ learning the CPT of node i
		for (int i = 0; i < net.getNodes().size(); i++) {
			BayesNode n = allNodes.get(i);
			List<BayesNode> parents = n.getParents();
			int nbParents = parents.size();

			// ~ CPT = cartesian product (sizes multiplied)
			int[] sizes = new int[nbParents];
			int nbRowsInCPT = 1;
			for (int parent = 0; parent < parents.size(); parent++) {
				sizes[parent] = parents.get(parent).getOutcomeCount();
				nbRowsInCPT *= sizes[parent];
			}

			double[][] probas = new double[nbRowsInCPT][n.getOutcomeCount()];

			// random CPTs
			for (int row = 0; row < nbRowsInCPT; row++) {
				
				//sample from Dirichlet(alphaDirichlet,...,alphaDirichlet)
				//to assign probabilities of multinomial
				double sum = 0.0;
				for (int s = 0; s < n.getOutcomeCount(); s++) {
					probas[row][s]=this.r.nextGamma(hardness, 1.0);
					sum+=probas[row][s];
				}
				for (int s = 0; s < n.getOutcomeCount(); s++) {
					probas[row][s]/=sum;
				}
			}
			double[] probas1D = new double[nbRowsInCPT * n.getOutcomeCount()];
			for (int row = 0; row < nbRowsInCPT; row++) {
				int offset = n.getOutcomeCount() * row;
				for (int s = 0; s < n.getOutcomeCount(); s++) {
					int index = offset+s;
					probas1D[index] = probas[row][s];
				}
			}
			
//			System.out.println(Arrays.toString(probas1D));
			n.setProbabilities(probas1D);

		}

	}
	
	public void generateSecureRandomCasesFileArff(File file) throws FileNotFoundException,
			NoSuchAlgorithmException {
		if (file.exists()) {
			file.delete();
		}
		PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file), 10 * 1024 * 1024));
		System.out.println("Writing case file: "+file.getAbsolutePath());

		SecureRandom secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");

		BayesNode n;

		out.println("@relation " + file.getName());
		out.println();

		for (int j = 0; j < eo.length; j++) {
			n = nodes[eo[j]];
			out.print("@attribute " + n.getName() + " {");
			out.print(n.getOutcomeName(0));
			for (int s = 1; s < n.getOutcomeCount(); s++) {
				out.print("," + n.getOutcomeName(s));
			}
			out.println("}");
		}

		out.println();
		out.println("@data");
		out.println();
		HashMap<BayesNode, String> evidence = new HashMap<>();
		for (int i = 0; i < nDataPoints; i++) {
			String str = "";
			for (int j = 0; j < eo.length; j++) {
				n = nodes[eo[j]];
				double[]probas = BayesNodeUtil.getSubCpt(n, evidence);
				double rand = secureRandomGenerator.nextDouble();
				int chosenValue = 0;
				double sumProba = probas[chosenValue];
				while (rand > sumProba) {
					chosenValue++;
					assert(chosenValue<probas.length);
					sumProba += probas[chosenValue];
				}
				String outcome = n.getOutcomeName(chosenValue);
				evidence.put(n, outcome);
				str += outcome;
				if (j < eo.length - 1) {
					str += ",";
				}
			}
			out.println(str);
			evidence.clear();
		}

		out.flush();
		out.close();

	}

	public static void main(String[] args) throws IOException {

		String rootFolder = "/tmp/data/";
		if (!new File(rootFolder).exists()) {
			new File(rootFolder).mkdirs();
		}

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);

		// Default values
		int nVariables = 100;
		int maxNParents = 5;
		int maxNValues= 5;
		int nDataPoints = 50000;
		double alphaDirichlet = 10.0;
		long seed = 3071980L;

		RandomStructureGenerator gen = new RandomStructureGenerator(nVariables, maxNParents,maxNValues,nDataPoints,alphaDirichlet, seed);
		
		File rep = new File(rootFolder + "/");
		if (!rep.exists()) {
			rep.mkdirs();
		}

		File arff = new File(rootFolder +"/data.arff");
		if (!arff.exists()) {
			new FileOutputStream(arff).close();
		}

		gen.generateDataset(arff);

	}
}
