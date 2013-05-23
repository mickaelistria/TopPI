package fr.liglab.lcm.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import fr.liglab.lcm.internals.TransactionReader;
import fr.liglab.lcm.io.FileReader;
import fr.liglab.lcm.tests.stubs.StubPatternsCollector;

/**
 * Its special feature is providing FileReaders and StubPatternsCollectors
 * on datasets in test/resources
 */
public class FileReaderTest {
	
	/**
	 * made for minsup=2
	 */
	public static final String PATH_MICRO = "target/test-classes/micro.dat";
	
	/**
	 * minsup=2
	 */
	public static StubPatternsCollector getMicroReaderPatterns() {
		StubPatternsCollector patterns = new StubPatternsCollector();
		patterns.expectCollect(3, 	1, 6, 5, 3);
		patterns.expectCollect(2, 	1, 6, 5, 3, 7);
		patterns.expectCollect(2, 	2, 3);
		patterns.expectCollect(4, 	3);
		patterns.expectCollect(3, 	3, 7);
		patterns.expectCollect(4, 	5);
		patterns.expectCollect(3, 	5, 7);
		patterns.expectCollect(4, 	7);
		return patterns;
	}
	
	/**
	 * made for minsup=2
	 */
	public static final String PATH_GLOBAL_CLOSURE = "target/test-classes/globalclosure.dat";
	
	/**
	 * minsup=2
	 */
	public static StubPatternsCollector getGlobalClosurePatterns() {
		StubPatternsCollector patterns = new StubPatternsCollector();
		patterns.expectCollect(5, 	1);
		patterns.expectCollect(3, 	1, 2);
		return patterns;
	}
	
	/**
	 * made for minsup=2
	 */
	public static final String PATH_FAKE_GLOBAL_CLOSURE = "target/test-classes/fakeglobalclosure.dat";
	
	/**
	 * minsup=2
	 */
	public static StubPatternsCollector getFakeGlobalClosurePatterns() {
		StubPatternsCollector patterns = new StubPatternsCollector();
		patterns.expectCollect(4, 	1);
		patterns.expectCollect(3, 	1, 2);
		return patterns;
	}
	
	/**
	 * recommanded minsup = 4
	 * First 50 lines of retail.dat
	 */
	public static final String PATH_50_RETAIL = "target/test-classes/50retail.dat";
	
	/**
	 * minsup = 4
	 */
	public static StubPatternsCollector get50RetailPatterns() {
		StubPatternsCollector patterns = new StubPatternsCollector();
		patterns.expectCollect(5, 	32);
		patterns.expectCollect(12, 	38);
		patterns.expectCollect(6, 	38, 36);
		patterns.expectCollect(32, 	39);
		patterns.expectCollect(11, 	39, 38);
		patterns.expectCollect(5, 	39, 38, 36);
		patterns.expectCollect(11, 	41);
		patterns.expectCollect(4, 	41, 38);
		patterns.expectCollect(8, 	41, 39);
		patterns.expectCollect(23, 	48);
		patterns.expectCollect(7, 	48, 38);
		patterns.expectCollect(18, 	48, 39);
		patterns.expectCollect(6, 	48, 39, 38);
		patterns.expectCollect(6, 	48, 41);
		patterns.expectCollect(5, 	48, 41, 39);
		return patterns;
	}
	
	/**
	 * made for minsup=2
	 * In order to ease testing, each item in this file is equal to its support count
	 */
	public static final String PATH_REBASING = "target/test-classes/rebasing.dat";
	
	/**
	 * To be wrapped in a RebaserCollector !!
	 * minsup = 2
	 */
	public static StubPatternsCollector getRebasingPatterns() {
		StubPatternsCollector patterns = new StubPatternsCollector();
		patterns.expectCollect(5, 	5);
		patterns.expectCollect(4, 	4);
		patterns.expectCollect(3, 	4, 5);
		patterns.expectCollect(3, 	3, 5);
		patterns.expectCollect(2, 	2, 3, 5);
		return patterns;
	}
	
	/**
	 * made for minsup=1, k=2
	 */
	public static final String PATH_TEST_EXPLORE = "target/test-classes/testExplore.dat";
	
	/**
	 * minsup=1, k=2
	 */
	public static StubPatternsCollector getTestExplorePatternsK2() {
		StubPatternsCollector patterns = new StubPatternsCollector();
		patterns.expectCollect(8, 	1);
		patterns.expectCollect(5, 	2);
		patterns.expectCollect(3, 	2, 1);
		patterns.expectCollect(4, 	3);
		patterns.expectCollect(3, 	3, 1);
		return patterns;
	}
	
	/**
	 * made for 1 < minsup < 12
	 */
	public static final String PATH_TEST_UNFILTERING = "target/test-classes/testUnfiltering.txt";
	
	public static StubPatternsCollector getTestUnfilteringPatterns() {
		StubPatternsCollector patterns = new StubPatternsCollector();
		patterns.expectCollect(11, 	0);
		patterns.expectCollect(10, 	1);
		patterns.expectCollect(8, 	2, 0);
		patterns.expectCollect(6, 	0, 1, 2);
		return patterns;
	}
	
	
	
	
	
	@Test
	/**
	 * note : the empty line in micro.dat *is intentional*
	 */
	public void testMicroLoading() {
		FileReader reader = new FileReader(PATH_MICRO);
		assertTrue(reader.hasNext());
		readLine(reader.next(), 5,3,1,6,7);
		readLine(reader.next(), 5,3,1,2,6);
		readLine(reader.next(), 5,7);
		readLine(reader.next(), 3,2,7);
		readLine(reader.next(), 5,3,1,6,7);
		assertFalse(reader.hasNext());
		
		reader.close();
		
		assertTrue(reader.hasNext());
		readLine(reader.next(), 5,3,1,6,7);
		readLine(reader.next(), 5,3,1,2,6);
		readLine(reader.next(), 5,7);
		readLine(reader.next(), 3,2,7);
		readLine(reader.next(), 5,3,1,6,7);
		assertFalse(reader.hasNext());
	}
	
	@Test
	public void testMicroRenaming() {
		FileReader reader = new FileReader(PATH_MICRO);
		while (reader.hasNext()) {
			TransactionReader transaction = reader.next();
			while (transaction.hasNext()) {
				transaction.next();
			}
		}
		
		//    original names :  0    1   2  3  4   5  6   7
		reader.close(new int[] {-1, -1, -1, 0, -1, 1, -1, 2});
		
		assertTrue(reader.hasNext());
		readLine(reader.next(), 0, 1, 2);
		readLine(reader.next(), 0, 1);
		readLine(reader.next(), 1, 2);
		readLine(reader.next(), 0, 2);
		readLine(reader.next(), 0, 1, 2);
		assertFalse(reader.hasNext());
	}
	
	private void readLine(TransactionReader lineReader, int... items) {
		for (int item: items) {
			assertTrue(lineReader.hasNext());
			assertEquals(item, lineReader.next());
		}
	}

}
