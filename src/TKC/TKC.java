package TKC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import TKC_E.Itemset;

public class TKC {
	double minUtil;
	List<UtilityList> ListUls;
	PriorityQueue<Itemset> Q;
	Comparator<Itemset> customComparator = new Comparator<Itemset>() {
		@Override
		public int compare(Itemset I1, Itemset I2) {
			return I1.getUtility() > I2.getUtility() ? 1 : -1;

		}
	};
	private int[] temp = new int[5000];
	int itemCount = 0;
	int giCount = 0;
	int taxDepth = 0;
	double maxMemory = 0;
	public static Map<Integer, UtilityList> mapItemToUtilityList;
	public long startTimestamp = 0;
	Map<Integer, Double> mapItemToTWU;
	/** the time at which the algorithm ended */
	public long endTimestamp = 0;
	TaxonomyTree taxonomy;
	// private int[] itemsetBuffer = null;
	List<Pair> revisedTransaction;
	List<List<Pair>> datasetAfterRemove;
	int countHUI;
	int candidate;

	class Pair {
		int item = 0;
		double utility = 0;
	}

	int timeProject = 0;

	public void runAlgorithm(int k, String inputPath, String outputPath, String TaxonomyPath) throws IOException {

		this.minUtil = 0.0;
		candidate = 0;
		maxMemory = 0;
		startTimestamp = System.currentTimeMillis();
		Q = new PriorityQueue<Itemset>(customComparator);
		mapItemToTWU = new HashMap<Integer, Double>();
		taxonomy = new TaxonomyTree();
		taxonomy.ReadDataFromPath(TaxonomyPath);
		BufferedReader myInput = null;
		// itemsetBuffer = new int[500];
		datasetAfterRemove = new ArrayList<List<Pair>>();
		countHUI = 0;
		Set<Integer> itemInDB = new HashSet<Integer>();
		String thisLine;
		try {
			// prepare the object for reading the file
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputPath))));
			// for each line (transaction) until the end of file
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is a comment, is empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}

				// split the transaction according to the : separator
				String split[] = thisLine.split(":");
				// the first part is the list of items
				String items[] = split[0].split(" ");
				// the second part is the transaction utility
				double transactionUtility = Double.parseDouble(split[1]);
				HashSet<Integer> setParent = new HashSet<Integer>();
				// for each item, we add the transaction utility to its TWU
				for (int i = 0; i < items.length; i++) {
					// convert item to integer
					Integer item = Integer.parseInt(items[i]);
					itemInDB.add(item);
					if (taxonomy.mapItemToTaxonomyNode.get(item) == null) {
						TaxonomyNode newNode = new TaxonomyNode(item);
						taxonomy.mapItemToTaxonomyNode.get(-1).addChildren(newNode);
						taxonomy.mapItemToTaxonomyNode.put(item, newNode);
					} else {
						TaxonomyNode parentNode = taxonomy.mapItemToTaxonomyNode.get(item).getParent();
						while (parentNode.getData() != -1) {
							setParent.add(parentNode.getData());
							parentNode = parentNode.getParent();
						}
					}

					// get the current TWU of that item
					Double twu = mapItemToTWU.get(item);
					// add the utility of the item in the current transaction to its twu
					twu = (twu == null) ? transactionUtility : twu + transactionUtility;
					mapItemToTWU.put(item, twu);
				}
				for (Integer parentItemInTransaction : setParent) {
					Double twu = mapItemToTWU.get(parentItemInTransaction);
					twu = (twu == null) ? transactionUtility : twu + transactionUtility;
					mapItemToTWU.put(parentItemInTransaction, twu);
				}
				itemCount = itemInDB.size();
				giCount = taxonomy.getGI() - 1;
				taxDepth = taxonomy.getMaxLevel();
			}
		} catch (Exception e) {
			// catches exception if error while reading the input file
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}
		List<UtilityList> listOfUtilityLists = new ArrayList<UtilityList>();
		mapItemToUtilityList = new HashMap<Integer, UtilityList>();

		// For each item
		for (Integer item : mapItemToTWU.keySet()) {
			// if the item is promising (TWU >= minutility)
			if (mapItemToTWU.get(item) > minUtil) {
				// create an empty Utility List that we will fill later.
				UtilityList uList = new UtilityList(item);
				mapItemToUtilityList.put(item, uList);
				// add the item to the list of high TWU items
				listOfUtilityLists.add(uList);

			}
		}
		
		Collections.sort(listOfUtilityLists, new Comparator<UtilityList>() {
			public int compare(UtilityList o1, UtilityList o2) {
				// compare the TWU of the items
				return compareItems(o1.item, o2.item);
			}
		});
		
		try {
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputPath))));
			int tid = 0;
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is a comment, is empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}
				String split[] = thisLine.split(":");
				// get the list of items
				String items[] = split[0].split(" ");
				// get the list of utility values corresponding to each item
				// for that transaction
				String utilityValues[] = split[2].split(" ");

				// Copy the transaction into lists but
				// without items with TWU < minutility

				// long newTWU = 0; // NEW OPTIMIZATION
				double TU = Double.parseDouble(split[1]);
				// Create a list to store items
				List<Pair> revisedTransaction = new ArrayList<Pair>();
				// for each item
				double remainingUtility = 0;
				HashMap<Integer, Double> mapParentToUtility = new HashMap<Integer, Double>();

				for (int i = 0; i < items.length; i++) {
					Double Utiliy = Double.parseDouble(utilityValues[i]);
					int item = Integer.parseInt(items[i]);
					TaxonomyNode nodeParent = taxonomy.mapItemToTaxonomyNode.get(item).getParent();
					while (nodeParent.getData() != -1) {
						Double utilityOfParent = mapParentToUtility.get(nodeParent.getData());
						if (utilityOfParent != null) {
							mapParentToUtility.put(nodeParent.getData(), utilityOfParent + Utiliy);
						} else {
							mapParentToUtility.put(nodeParent.getData(), Utiliy);
						}
						nodeParent = nodeParent.getParent();
					}
					Pair pair = new Pair();
					pair.item = item;
					pair.utility = Utiliy;
					if (mapItemToTWU.get(pair.item) >= minUtil) {
						revisedTransaction.add(pair);
						// remainingUtility += pair.utility;
						// newTWU+=pair.utility;
					}
				}
				Collections.sort(revisedTransaction, new Comparator<Pair>() {
					public int compare(Pair o1, Pair o2) {
						return compareItems(o1.item, o2.item);
					}
				});

				for (int i = 0; i < revisedTransaction.size(); i++) {
					Pair pair = revisedTransaction.get(i);
					remainingUtility = remainingUtility - pair.utility;
					UtilityList utilityListOfItem = mapItemToUtilityList.get(pair.item);
					Element element = new Element(tid, pair.utility, remainingUtility, TU);
					utilityListOfItem.addElement(element);
				}
				double CountUtility = TU;
				for (Integer itemParent : mapParentToUtility.keySet()) {
					double CountUtilityOfEachItem = CountUtility;
					for (int i = 0; i < revisedTransaction.size(); i++) {
						Pair CurrentItem = revisedTransaction.get(i);
						if (compareItems(itemParent, CurrentItem.item) > 0) {
							CountUtilityOfEachItem -= CurrentItem.utility;
						}
					}
					CountUtilityOfEachItem -= mapParentToUtility.get(itemParent);
					UtilityList utilityListOfItem = mapItemToUtilityList.get(itemParent);
					if (utilityListOfItem != null) {
						Element element = new Element(tid, mapParentToUtility.get(itemParent), CountUtilityOfEachItem,
								TU);
						utilityListOfItem.addElement(element);
					}

				}
				datasetAfterRemove.add(revisedTransaction);
				tid++;
			}

			List<UtilityList> listUtilityLevel1 = new ArrayList<UtilityList>();
			for (UtilityList ul1 : listOfUtilityLists) {
				if (taxonomy.getMapItemToTaxonomyNode().get(ul1.item).getLevel() == 1) {
					listUtilityLevel1.add(ul1);
				}
				if (taxonomy.getMapItemToTaxonomyNode().get(ul1.item).getLevel() > 1) {
					break;
				}
			}

			/*
			 * for (Integer utilityList : mapItemToUtilityList.keySet()) {
			 * System.out.println(utilityList+"-"+
			 * mapItemToUtilityList.get(utilityList).elements.size()+
			 * "-"+mapItemToUtilityList.get(utilityList).GWU+
			 * "-"+mapItemToUtilityList.get(utilityList).sumIutils+
			 * "-"+mapItemToUtilityList.get(utilityList).sumRutils); }
			 */
			itemCount = itemInDB.size();
			giCount = taxonomy.getGI() - 1;
			taxDepth = taxonomy.getMaxLevel();

			SearchTree(new int[0], 0, null, listUtilityLevel1, k, Q);
		} catch (Exception e) {
			throw e;
		}
		checkMemory();
		endTimestamp = System.currentTimeMillis();
		myInput.close();

	}

	private void SearchTree(int[] prefix, int prefixLength, UtilityList pUL, List<UtilityList> ULs, int k,
			PriorityQueue<Itemset> Q) {
		for (int i = 0; i < ULs.size(); i++) {
			if (ULs.get(i).GWU > minUtil) {
				UtilityList X = ULs.get(i);
				candidate++;
				if (X.sumIutils > minUtil) {
					countHUI++;
					StringBuffer buffer = new StringBuffer();
					for (int j = 0; j < prefixLength; j++) {
						buffer.append(prefix[j]);
						buffer.append(' ');
					} 
					buffer.append(X.item);
					if (Q.size() < k) {
						Q.offer(new Itemset(X.sumIutils,buffer.toString()));
					} else {
						Q.offer(new Itemset(X.sumIutils,buffer.toString()));
						Q.poll();
						minUtil = Q.peek().getUtility();
					}
//					if (Q.size() < k) {
//						Q.add(X);
//					} else {
//						Q.add(X);
//						Q.poll();
//						minUtil = Q.peek().sumIutils;
//					}
//				for (int j = 0; j < prefixLength; j++) {
//					System.out.print(prefix[j] + " ");
//				}
//				System.out.print(X.item + " ");
//
//				System.out.println("  #UTIL: " + X.sumIutils);

				}
				List<UtilityList> exULs = new ArrayList<UtilityList>();
				for (int j = i + 1; j < ULs.size(); j++) {

					UtilityList Y = ULs.get(j);

					if (!CheckParent(Y.item, X.item) && Y.item != X.item) {
						UtilityList exULBuild = construct(pUL, X, Y, prefix);
						if (exULBuild.GWU >  minUtil) {
							exULs.add(exULBuild);
						}
					}
				}
				if (X.sumIutils + X.sumRutils > minUtil) {
					TaxonomyNode taxonomyNodeX = taxonomy.getMapItemToTaxonomyNode().get(X.item);
					List<TaxonomyNode> childOfX = taxonomyNodeX.getChildren();

					for (TaxonomyNode taxonomyNode : childOfX) {
						int Child = taxonomyNode.getData();
						UtilityList ULofChild = mapItemToUtilityList.get(Child);
						if (ULofChild != null) {
							UtilityList exULBuild = constructTax(pUL, ULofChild, prefix);
							X.AddChild(exULBuild);
						}
					}

					for (UtilityList childULs : X.getChild()) {
						if (childULs.GWU > minUtil) {
							ULs.add(childULs);
						}
					}
				}
				int[] newPrefix = new int[prefix.length + 1];
				System.arraycopy(prefix, 0, newPrefix, 0, prefix.length);
				newPrefix[prefix.length] = X.item;
				SearchTree(newPrefix, prefixLength + 1, X, exULs, k, Q);
			}
		}
	}

	private UtilityList constructTax(UtilityList P, UtilityList Child, int[] prefix) {

		if (P == null) {
			return Child;
		} else {
			UtilityList newULs = new UtilityList(Child.item);
			for (Element PElment : P.getElement()) {

				Element UnionChild = findElementWithTID(Child, PElment.tid);
				if (UnionChild != null) {
					List<Pair> trans = datasetAfterRemove.get(UnionChild.tid);
					double remainUtility = UnionChild.TU;

					for (int i = 0; i < trans.size(); i++) {
						Integer currentItem = trans.get(i).item;
						if (compareItems(currentItem, Child.item) < 0
								&& compareItems(currentItem, prefix[prefix.length - 1]) > 0
								&& (!CheckParent(Child.item, currentItem))
								&& (!CheckParentWithPrefix(currentItem, prefix))) {
							remainUtility -= trans.get(i).utility;
						}
					}
					remainUtility -= (PElment.iutils + UnionChild.iutils);
					// Create new element
					Element newElment = new Element(UnionChild.tid, PElment.iutils + UnionChild.iutils, remainUtility,
							UnionChild.TU);
					// add the new element to the utility list of pXY
					newULs.addElement(newElment);
				}
			}
			// return the utility list of pXY.
			return newULs;
		}
	}

	private UtilityList construct(UtilityList P, UtilityList px, UtilityList py, int[] prefix) {
		UtilityList pxyUL = new UtilityList(py.item);

		// for each element in the utility list of pX
		for (Element ex : px.elements) {
			// do a binary search to find element ey in py with tid = ex.tid
			Element ey = findElementWithTID(py, ex.tid);
			if (ey == null) {
				continue;
			}
			// if the prefix p is null
			if (P == null) {
				// Create the new element
				List<Pair> trans = datasetAfterRemove.get(ex.tid);
				double remainUtility = ey.TU;
				for (int i = 0; i < trans.size(); i++) {
					Integer currentItem = trans.get(i).item;
					if (compareItems(currentItem, py.item) < 0 && compareItems(currentItem, px.item) > 0
							&& (!CheckParent(px.item, currentItem)) && (!CheckParent(py.item, currentItem))) {
						remainUtility -= trans.get(i).utility;
					}
				}
				remainUtility -= (ex.iutils + ey.iutils);
				Element eXY = new Element(ex.tid, ex.iutils + ey.iutils, remainUtility, ey.TU);
				// add the new element to the utility list of pXY
				pxyUL.addElement(eXY);

			} else {
				// find the element in the utility list of p wih the same tid
				Element e = findElementWithTID(P, ex.tid);
				if (e != null) {
					List<Pair> trans = datasetAfterRemove.get(e.tid);
					double remainUtility = ey.TU;
					for (int i = 0; i < trans.size(); i++) {
						Integer currentItem = trans.get(i).item;
						if (compareItems(currentItem, py.item) < 0 && compareItems(currentItem, px.item) > 0
								&& (!CheckParentWithPrefix(currentItem, prefix)) && (!CheckParent(px.item, currentItem))
								&& (!CheckParent(py.item, currentItem))) {
							remainUtility -= trans.get(i).utility;
						}
					}
					remainUtility -= (ex.iutils + ey.iutils - e.iutils);
					Element eXY = new Element(ex.tid, ex.iutils + ey.iutils - e.iutils, remainUtility, ey.TU);
					// add the new element to the utility list of pXY
					pxyUL.addElement(eXY);
				}
			}
		}
		// return the utility list of pXY.
		return pxyUL;
	}

	private boolean CheckParentWithPrefix(int item, int[] prefix) {
		for (int i = 0; i < prefix.length; i++) {
			if (CheckParent(item, prefix[i])) {
				return true;
			}
		}
		return false;
	}

	private Element findElementWithTID(UtilityList ulist, int tid) {
		List<Element> list = ulist.elements;

		// perform a binary search to check if the subset appears in level k-1.
		int first = 0;
		int last = list.size() - 1;

		// the binary search
		while (first <= last) {
			int middle = (first + last) >>> 1; // divide by 2

			if (list.get(middle).tid < tid) {
				first = middle + 1; // the itemset compared is larger than the subset according to the lexical order
			} else if (list.get(middle).tid > tid) {
				last = middle - 1; // the itemset compared is smaller than the subset is smaller according to the
									// lexical order
			} else {
				return list.get(middle);
			}
		}
		return null;
	}

	private int compareItems(int item1, int item2) {
		int levelOfItem1 = taxonomy.getMapItemToTaxonomyNode().get(item1).getLevel();
		int levelOfItem2 = taxonomy.getMapItemToTaxonomyNode().get(item2).getLevel();
		if (levelOfItem1 == levelOfItem2) {
			int compare = (int) (mapItemToTWU.get(item1) - mapItemToTWU.get(item2));
			// if the same, use the lexical order otherwise use the TWU
			return (compare == 0) ? item1 - item2 : compare;
		} else {
			return levelOfItem1 - levelOfItem2;
		}
	}

	private boolean CheckParent(int item1, int item2) {
		TaxonomyNode nodeItem1 = taxonomy.getMapItemToTaxonomyNode().get(item1);
		TaxonomyNode nodeItem2 = taxonomy.getMapItemToTaxonomyNode().get(item2);
		int levelOfItem1 = nodeItem1.getLevel();
		int levelOfItem2 = nodeItem2.getLevel();
		if (levelOfItem1 == levelOfItem2) {
			return false;
		} else {
			if (levelOfItem1 > levelOfItem2) {
				TaxonomyNode parentItem1 = nodeItem1.getParent();
				while (parentItem1.getData() != -1) {
					if (parentItem1.getData() == nodeItem2.getData()) {
						return true;
					}
					parentItem1 = parentItem1.getParent();
				}
				return false;
			} else {
				TaxonomyNode parentItem2 = nodeItem2.getParent();
				while (parentItem2.getData() != -1) {
					if (parentItem2.getData() == nodeItem1.getData()) {
						return true;
					}
					parentItem2 = parentItem2.getParent();
				}
				return false;
			}
		}
	}

	public void printStats() throws IOException {
		System.out.println("=============  TKC =============");
//		System.out.println(" |I|              : " + itemCount);
//		System.out.println(" |GI|             : " + giCount);
//		System.out.println(" Depth            : " + taxDepth);
		System.out.println(" minUtil = " + minUtil);
		System.out.println(" Total time ~: " + (endTimestamp - startTimestamp) + " ms");
		System.out.println(" Memory ~ " + maxMemory + " MB");
		System.out.println("======================================");
		System.out.println(" List of Top k High Utility Itemset:");
		while (!Q.isEmpty()) {
			System.out.println(Q.poll().toString());
		}
		System.out.println("======================================");
	}

	private void checkMemory() {
		// get the current memory usage
		double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
		// if higher than the maximum until now
		if (currentMemory > maxMemory) {
			// replace the maximum with the current memory usage
			maxMemory = currentMemory;
		}
	}
}
