package TKC;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {

		String TaxonomyPath = "C:\\Users\\LaptopAZ.vn\\eclipse-workspace\\TKC_E-Evaluation-master\\Dataset\\taxonomy_Test.txt";
		String inputPath = "C:\\Users\\LaptopAZ.vn\\eclipse-workspace\\TKC_E-Evaluation-master\\Dataset\\transaction_Test.txt";
		// String inputPath = "liquor.txt";
		TKC TKC = new TKC();
		int k = 5;
		// CLH_MinerTestP cl = new CLH_MinerTestP();
		 //pCLH_Miner cl = new pCLH_Miner();
		for (int i = 0; i < 1; i++) {
			System.gc();
			TKC.runAlgorithm(k, inputPath, "", TaxonomyPath);
			TKC.printStats();
			//k-=10;
			
		}
	}
}
