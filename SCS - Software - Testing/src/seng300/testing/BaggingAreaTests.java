package seng300.testing;

import static org.junit.Assert.*;
import org.junit.*;
import java.util.Random;
import org.lsmr.selfcheckout.*;
import org.lsmr.selfcheckout.devices.*;
//import org.lsmr.selfcheckout.devices.SimulationException;
import org.lsmr.selfcheckout.devices.observers.*;
import org.lsmr.selfcheckout.products.BarcodedProduct;

import seng300.software.ProductDatabaseLogic;
import seng300.software.SelfCheckoutSystemLogic;
import seng300.software.exceptions.ProductNotFoundException;

import java.math.*;
import java.util.*;


public class BaggingAreaTests {
	
	//declare testing variables and objects	
	SelfCheckoutStation scs;
	int bval1 = 1;
	int[] bdenom_array = {bval1};
	
	BigDecimal cval1 = new BigDecimal(0.25);
	BigDecimal[] cdenom_array = {cval1};
	
	Currency defcur = Currency.getInstance("CAD");

	int scaleMaximumWeight = 15;
	int scaleSensitivity = 3;
	
	BigDecimal pval1 = new BigDecimal("1.25");
	BigDecimal pval2 = new BigDecimal("3.00");
	BigDecimal pval3 = new BigDecimal("10.00");
	BigDecimal pval4 = new BigDecimal("2.00");
	BigDecimal pval5 = new BigDecimal("8.00");
	BigDecimal pval6 = new BigDecimal("2.00");

	BarcodedItem it1;
	BarcodedItem it2;
	BarcodedItem it3;
	BarcodedItem it4;
	BarcodedItem it5;
	BarcodedItem it6;
	BarcodedItem it7;
	
	//values
	boolean expected = true;
	boolean actual = true;

	Map<Barcode, BarcodedProduct> bprods;
	Map<Barcode, BarcodedItem> bitems;

	ProductDatabaseLogic db;
	SelfCheckoutSystemLogic checkoutControl;
	
	@Before
	//runs before each test
	public void setUp() throws ProductNotFoundException {
		//this is taken from the selfcheckout class. just setting everything up
		//scs = new SelfCheckoutStation(defcur, bdenom_array, cdenom_array, scaleMaximumWeight, scaleSensitivity);
		db = new ProductDatabaseLogic(7, scaleMaximumWeight);
		
		int counter = 1;
		double changedWeight = 0;
		Random rand = new Random();
		for (Barcode b : this.db.getProducts().keySet()) {
			switch(counter) {
				case 1:
					changedWeight = rand.nextDouble() + 1;
					it1 = new BarcodedItem(b, changedWeight);
					break;
				case 2:
					changedWeight = scaleSensitivity;
					it2 = new BarcodedItem(b, changedWeight);
					break;
				case 3:
					changedWeight = rand.nextDouble() + 5;
					it3 = new BarcodedItem(b, changedWeight);
					break;
				case 4:
					changedWeight = rand.nextDouble() + 10;
					it4 = new BarcodedItem(b, changedWeight);
					break;
				case 5:
					changedWeight = scaleMaximumWeight;
					it5 = new BarcodedItem(b, changedWeight);
					break;
				case 6:
					changedWeight = rand.nextDouble() + scaleMaximumWeight;
					it6 = new BarcodedItem(b, changedWeight);
					break;
				case 7:
					changedWeight = rand.nextDouble() + 6;
					it7 = new BarcodedItem(b, changedWeight);
					break;
			}
			BarcodedProduct changingTheProduct = new BarcodedProduct (b, this.db.getProduct(b).getDescription(), 
			this.db.getProduct(b).getPrice(), changedWeight);
			
			this.db.getProducts().replace(b, this.db.getProduct(b), changingTheProduct);
			
			counter++;
		}
		
		scs = new SelfCheckoutStation(defcur, bdenom_array, cdenom_array, scaleMaximumWeight, scaleSensitivity);
		checkoutControl = new SelfCheckoutSystemLogic(scs, db);
				
	}

	@After
	public void tearDown() {
		// 
	}
	
	//tests
	
	//=================================================
	// Testing that when an item is added to the scale
	// then the bagging area reacts accordingly.
	//=================================================
	
	//=================================================
	// Testing single items
	//=================================================

	// @ TESTING-TEAM I changed the instance of the barcode scanner from 'scanner' to 'mainScanner' to match hardware-v2. -Kevin
	// @ TESTING-TEAM I changed the instance of the ElectronicScale from 'scale' to 'scanningArea' to match hardware-v2. -Kevin
	// List of new things to test
	// - There is a mainScanner and handheldScanner
	//    - Need more info, but assumed hanheldScanner will be used for items 
	//      that're too heavy for scanning area but enough for bagging area
	//    - Will mainScanner be disabled if hanheld is used?
	//    - both scanners report to BarcodeScannerObserver
	// - How to test timer (timer is advanced, can do without)
	//    - Assumption, timer starts when scanner observer detects a new item
	//    - Test the expected, if item is placed before timer runs out
	//      - bagging area observer
	//      - How would this change with the handheldScanner?
	//    - Test after timer has run out
	// - How to test attendant verification?
	// - Own Bags
	//    - Test the flags, notification and system block
	//    - Does only need to be tested once? Case of multiple bags?
	// 
	// replaced all scanning areas with bagging areas
	@Test
	public void testAddItemUnderSensitivity() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it1); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		
		//bagging area should be happy
		scs.baggingArea.add(it1); // notify weightchanged was not called
		//expected weight
		expected = false;
		actual = checkoutControl.isBlocked();
		assertEquals("item was less than sensitivity.",
				expected, actual);	
	}
	
	@Test // Should have similar reaction to items below sensitivity
	public void testAddItemEqualSensitivity() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it2); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		//bagging area shouldn't know/care
		scs.baggingArea.add(it2);
		//expected weight
		expected = false;
		actual = checkoutControl.isBlocked();
		assertEquals("item is equal to sensitivity.",
				expected, actual);	
	}
	
	@Test
	public void testAddItemAboveSensitivity() throws InterruptedException {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		//bagging area should know/care
		scs.baggingArea.add(it3);
		Thread.sleep(500); // Used so check bag thread can pick up results
		//expected weight
		expected = false;
		actual = checkoutControl.isBlocked();
		assertEquals("item is above the sensitivity.",
				expected, actual);	
	}
	
	@Test
	public void testAddItemEqualWeightLim() throws InterruptedException {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it5); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		//bagging area should not notify overload
		scs.baggingArea.add(it5);
		Thread.sleep(500); // Used so check bag thread can pick up results
		//expected weight
		expected = false;
		actual = checkoutControl.isBlocked();
		assertEquals("item is equal to the limit.",
				expected, actual);	
	}
	
	@Test
	public void testAddItemAboveWeightLim() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it6); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		//bagging area should notify overload
		scs.baggingArea.add(it6);
		//expected weight
		expected = true;
		actual = checkoutControl.isBlocked();
		assertEquals("item is above the limit.",
				expected, actual);		
	}
	
	@Test
	public void testScanItemButDontPlace() throws InterruptedException {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it4); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		//expected weight
		Thread.sleep(6000);
		expected = true;
		actual = scs.mainScanner.isDisabled(); // Not blocked but scanner is disabled
		assertEquals("item is not placed on the scale.",
				expected, actual);		
		actual = checkoutControl.isBlocked();
		assertEquals("item is not placed on the scale.",
				expected, actual);
	}
	
	@Test //(expected = NullPointerException.class)
	public void testAddItemWithoutScan() throws InterruptedException {
		scs.baggingArea.add(it4); 
		//expected weight
		expected = true;
		actual = checkoutControl.isBlocked();
		assertEquals("item is above the limit.",
				expected, actual);		
	}
	
	//=================================================
	// Testing multiple items
	//=================================================
	
	@Test
	public void testScanSecondItemUnderSensitivity() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it1); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		//bagging area shouldn't care
		//bagging area shouldn't care	
		previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it2); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		
		//expected value
		expected = false;
		actual = scs.mainScanner.isDisabled();
		assertEquals("items are under sensitivity.",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("items are under sensitivity.",
				expected, actual);	
	}
	
	@Test
	public void testAddSecondItemUnderSensitivity() {
		//bagging area shouldn't know/care
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it2); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		scs.baggingArea.add(it2);
		//bagging area should know/care
		previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		scs.baggingArea.add(it3);

		//expected value
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("it2 not added but it3 is",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("it2 not added but it3 is",
				expected, actual);
	}
	
	@Test 
	public void testAddItemsAboveSens() throws ProductNotFoundException, InterruptedException {
		//bagging area should know/care
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		scs.baggingArea.add(it3);
		
		previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it7); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		scs.baggingArea.add(it7); 
		
		Thread.sleep(500); // Used so check bag thread can pick up results
		//expected weight
		//expected value
		expected = false;
		actual = scs.mainScanner.isDisabled();
		assertEquals("Case passes",
						expected, actual);
		actual = checkoutControl.isBlocked();
		assertEquals("Case passes",
				expected, actual);
	}
	
	@Test
	public void testAddItemsPastWeightLim() { // BUG IN CODE, FIXED LATER
		//bagging area should be fine
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		scs.baggingArea.add(it3);
		//adding item1 should make the scale notify overload
		previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it4); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		scs.baggingArea.add(it4);
		//expected value
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("Case passes",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("Case passes",
				expected, actual);	
	}
	
	
	@Test
	public void testAddItemInOverload1() {
		//bagging area should be in overload after item6
		scs.mainScanner.scan(it6); 
		scs.baggingArea.add(it6);
		//this should notify overload again?

		scs.mainScanner.scan(it2); 
		scs.baggingArea.add(it2); // Works only because it2 weight = sensitivity & weightChanged is skipped
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("Case passes",
				expected, actual);	
	}
	
	@Test
	public void testAddItemInOverload2() {
		//bagging area should be in overload after item6

		scs.mainScanner.scan(it6); 
		scs.baggingArea.add(it6);
		//this should notify overload again?
		scs.mainScanner.scan(it3); 	
		scs.baggingArea.add(it3);
		//expected value 
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("Case passes",
				expected, actual);	
	}
	
	@Test
	public void testScanItemButDontPlaceTwice() throws InterruptedException {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it7); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		do {
			scs.mainScanner.scan(it3);
		} while (scs.mainScanner.isDisabled() == false);
		//expected weight
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item is not placed on the scale.",
				expected, actual);	
	}
	
	@Test 
	public void testAddItemWithoutScanTwice() throws InterruptedException {
		scs.baggingArea.add(it7); 	
		scs.baggingArea.add(it3); 
		//expected weight
		Thread.sleep(6000);
		expected = true;
		actual = checkoutControl.isBlocked();
		assertEquals("item is above the limit.",
				expected, actual);	
		actual = scs.mainScanner.isDisabled();
		assertEquals("item isn't scanned",
				expected, actual);	
	}
	


	//=================================================
	// Testing no scan
	//=================================================
	
	@Test 
	public void testNoScanUnderSensitivity() {
		//bagging area should be happy
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it1); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		//expected weight
		expected = false;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item was less than sensitivity.",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("item was less than sensitivity.",
				expected, actual);	
	}
	
	@Test 
	public void testNoScanEqualSensitivity() {
		//bagging area shouldn't know/care
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it2); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		//expected weight
		expected = false;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item is equal to sensitivity.",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("item was less than sensitivity.",
				expected, actual);	
	}
	
	@Test 
	public void testNoScanAboveSensitivity() {
		//bagging area should know/care
		scs.baggingArea.add(it3);
		//expected weight
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item is above the sensitivity but not scanned.",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("item is above the sensitivity but not scanned.",
				expected, actual);
	}
	
	@Test 
	public void testNoScanEqualWeightLim() {
		//bagging area should not notify overload
		scs.baggingArea.add(it5);
		//expected weight
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item is equal to the limit.",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("item is equal to the limit.",
				expected, actual);	
	}
	
	@Test 
	public void testNoScanAboveWeightLim() {
		//bagging area should notify overload
		scs.baggingArea.add(it6); 
		//expected weight
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item is equal to the limit.",
				expected, actual);
		actual = checkoutControl.isBlocked();
		assertEquals("item is equal to the limit.",
				expected, actual);
	
	}
	

	//=================================================
	// Testing no add
	//=================================================

	@Test
	public void testNoAddUnderSensitivity() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it1); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		//bagging area should be happy
		//expected weight
		expected = false;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item was less than sensitivity.",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("item was less than sensitivity.",
				expected, actual);
	}
	
	@Test
	public void testNoAddEqualSensitivity() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it2); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		//bagging area shouldn't know/care
		//expected weight
		expected = false;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item is equal to sensitivity.",
				expected, actual);	
		actual = checkoutControl.isBlocked();
		assertEquals("item is equal to sensitivity.",
				expected, actual);	
	}
	
	@Test
	public void testNoAddAboveSensitivity() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		//bagging area should know/care
		//expected weight
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item is above the sensitivity.",
				expected, actual);	
	}
	
	@Test
	public void testNoAddEqualWeightLim() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it5); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		//bagging area should not notify overload
		//expected weight
		expected = true;
		actual = scs.mainScanner.isDisabled();
		assertEquals("item is equal to the limit.",
				expected, actual);	
	}
	
	@Test
	public void testNoAddAboveWeightLim() throws InterruptedException {
		//bagging area should notify overload
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it6); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);

		expected = true; // should be false, wait for fix
		actual = scs.mainScanner.isDisabled();
		assertEquals("item was above weight limit.",
				expected, actual);	
	}
	
	// New Tests section
	// Put in bagging area alert and check own bags
	
	// First cause bardCodeScanned event
	// Uses notifyItemAdded in BaggingAreaObserver
	// Note this method isn't connected to ElectronicScaleObserver
	// Called in addToCart in SelfCheckoutLogic
	// To simulate placing item would have to notifyWeightChanged 
	// Forcefully add an item using ElectronicScale
	 
	@Test //(timeout = 100) // 5000ms == 5s
	public void timerTestIfSystemBlockAfter5() throws InterruptedException {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		
		 Thread.sleep(7000);
		 expected = true;
		 actual = checkoutControl.isBlocked();
		 assertEquals("Item not in bagging after 5s",
				 expected, actual);
	}
	
	@Test 
	public void timerTestIfSystemUnblockAfter5() throws InterruptedException {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		
		 Thread.sleep(7000);
		 expected = true;
		 actual = checkoutControl.isBlocked();
		 assertEquals("Item not in bagging after 5s",
				 expected, actual);
		 scs.baggingArea.add(it3);
		 expected = false; // scanner enabled but not unblocked BUG?
		 actual = scs.mainScanner.isDisabled();//checkoutControl.isBlocked();
		 assertEquals("Item in bagging after 5s",
				 expected, actual);
	}
	
	@Test
	public void timerTestUnblockBefore5() throws InterruptedException {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		
		 scs.baggingArea.add(it3);
		 Thread.sleep(500); // Used so check bag thread can pick up results
		 expected = false;
		 actual = checkoutControl.isBlocked();
		 assertEquals("Item in bagging before 5s", 
				 expected, actual);
	}
	// Tests weight change after partial payment??
	
	//blocked = false when system is blocked
	//blocked = true when system isn't blocked
	//When adding your own bags system is blocked and won't detect changes
	//Attendant will manually unblock system
	
	@Test
	public void useOwnBagTestSystemBlock() {
		 checkoutControl.useOwnBags();
		 expected = true;
		 actual = checkoutControl.isBlocked();
		 assertEquals("System block",
				 expected, actual);	
	}
	
	@Test
	public void useOwnBagTestSystemUnblock() {
		 checkoutControl.useOwnBags();
		 checkoutControl.unblock();
		 expected = false;
		 actual = checkoutControl.isBlocked();
		 assertEquals("System block", 
				 expected, actual);	
	}
	
	@Test //(expected = NullPointerException.class)
	public void itemNotListedAfterScanTest() {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		
		scs.baggingArea.add(it4);
		expected = true;
		actual = checkoutControl.isBlocked();
		assertEquals("System block after add",
				expected, actual);
	}
	
	@Test // place unknown item
	public void unknownItemPlacedTest() {
		scs.baggingArea.add(it3);
		expected = true;
		actual = checkoutControl.isBlocked();
		assertEquals("System block after add",
				expected, actual);
	}

	// Test product exception
	@Test (expected = InvalidArgumentSimulationException.class)
	public void addTwoOfSameItemTest() throws InterruptedException {
		int previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		
		scs.baggingArea.add(it3);
		Thread.sleep(500); // Used so check bag thread can pick up results
		
		previousNumOfProducts = checkoutControl.getCart().getProducts().size();
		
		do {
			scs.mainScanner.scan(it3); 
		} while(checkoutControl.getCart().getProducts().size() == previousNumOfProducts);
		scs.baggingArea.add(it3);
	}
}


