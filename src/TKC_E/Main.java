package TKC_E;

import java.io.IOException;


public class Main {

	public static void main(String[] args) throws IOException {

//		String TaxonomyPath = "connectTaxonomy.txt";
//		String inputPath = "connect.txt";

		String TaxonomyPath = "C:\\Users\\LaptopAZ.vn\\eclipse-workspace\\TKC_E-Evaluation-master\\Dataset\\fruithutlTaxonomy.txt";
		String inputPath = "C:\\Users\\LaptopAZ.vn\\eclipse-workspace\\TKC_E-Evaluation-master\\Dataset\\fruithut.txt";
		TKC_E TKC_E = new TKC_E();
		// CLH_MinerTestP cl = new CLH_MinerTestP();
		// pCLH_Miner cl = new pCLH_Miner();
		int k = 200;
		// CLH_MinerTestP cl = new CLH_MinerTestP();
		 //pCLH_Miner cl = new pCLH_Miner();
		for (int i = 0; i < 1; i++) {
			System.gc();
			TKC_E.runAlgorithm(k, inputPath, "", TaxonomyPath, Integer.MAX_VALUE);
			TKC_E.printStats();
//			k-=50;
			
		}

//2088282/2150177
	}
}
