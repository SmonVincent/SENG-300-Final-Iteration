package seng300.software;


import java.math.BigDecimal;
import java.util.Currency;

import org.lsmr.selfcheckout.Banknote;
import org.lsmr.selfcheckout.Coin;
import org.lsmr.selfcheckout.SimulationException;
import org.lsmr.selfcheckout.devices.AbstractDevice;
import org.lsmr.selfcheckout.devices.Keyboard;
import org.lsmr.selfcheckout.devices.OverloadException;
import org.lsmr.selfcheckout.devices.SelfCheckoutStation;
import org.lsmr.selfcheckout.devices.SupervisionStation;
import org.lsmr.selfcheckout.devices.observers.AbstractDeviceObserver;
import org.lsmr.selfcheckout.devices.observers.KeyboardObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lsmr.selfcheckout.BarcodedItem;
import org.lsmr.selfcheckout.Item;
import org.lsmr.selfcheckout.PLUCodedItem;
import org.lsmr.selfcheckout.PriceLookupCode;
import org.lsmr.selfcheckout.devices.SelfCheckoutStation;
import org.lsmr.selfcheckout.devices.SupervisionStation;
import org.lsmr.selfcheckout.external.ProductDatabases;
import org.lsmr.selfcheckout.products.BarcodedProduct;
import org.lsmr.selfcheckout.products.PLUCodedProduct;
import org.lsmr.selfcheckout.products.Product;

import seng300.software.exceptions.ValidationException;

import seng300.software.exceptions.ProductNotFoundException;

public class AttendantLogic implements KeyboardObserver {

	public static boolean loggedIn;
	private static String userInput;
	private static String inputtedID;
	private static String inputtedPassword;
	private static String attendantPassword;
	private static String attendantID;
	
	public static boolean isIDEntered = false;
	public boolean enabledTrue = false;
	public boolean disabledTrue = false;
  
  public static SupervisionStation ss;
  
  private static volatile AttendantLogic instance = null;

  
	private Currency currency = Currency.getInstance("CAD");
	
	private SelfCheckoutSystemLogic scsLogic1;
	private SelfCheckoutSystemLogic scsLogic2;
	private SelfCheckoutSystemLogic scsLogic3;
	private SelfCheckoutSystemLogic scsLogic4;
	private SelfCheckoutSystemLogic scsLogic5;
	private SelfCheckoutSystemLogic scsLogic6;
	
	// Private for security reasons.
	private BigDecimal coin1 = new BigDecimal("0.05");
	private BigDecimal coin2 = new BigDecimal("0.10");
	private BigDecimal coin3 = new BigDecimal("0.25");
	private BigDecimal coin4 = new BigDecimal("1.00");
	private BigDecimal coin5 = new BigDecimal("2.00");
	
	private Coin dime = new Coin(currency, coin2);
	private Coin loonie = new Coin(currency, coin4);
	private Coin nickle = new Coin(currency, coin1);
	private Coin quarter = new Coin(currency, coin3);
	private Coin twoonie = new Coin(currency, coin5);
	
	private Banknote note1 = new Banknote(currency, 1);
	private Banknote note2 = new Banknote(currency, 5);
	private Banknote note3 = new Banknote(currency, 10);
	private Banknote note4 = new Banknote(currency, 20);
	private Banknote note5 = new Banknote(currency, 50);
	private Banknote note6 = new Banknote(currency, 100);

	private int[] bankNoteDenominations = {note1.getValue(), note2.getValue(), note3.getValue(), note4.getValue(), note5.getValue(), note6.getValue()};
	private Banknote[] banknoteArray = {note1, note2, note3, note4, note5, note6};
	
	private BigDecimal[] coinDenominations = {nickle.getValue(), dime.getValue(), quarter.getValue(), loonie.getValue(), twoonie.getValue()};
	private Coin[] coinArray = {nickle, dime, quarter, loonie, twoonie};
	
	private int scaleMaxWeight = 15;
	private int scaleSensitivity = 3;
		
	private AttendantLogic(SupervisionStation supervisionStation)
	{
		AttendantLogic.ss = supervisionStation;

		loggedIn = false;
		userInput = "";
		attendantPassword = "12345678";
		attendantID = "87654321";
		
	  	SelfCheckoutSystemLogic scsLogic1 = new SelfCheckoutSystemLogic(new SelfCheckoutStation(currency, bankNoteDenominations, coinDenominations, scaleMaxWeight, scaleSensitivity));
	  	SelfCheckoutSystemLogic scsLogic2 = new SelfCheckoutSystemLogic(new SelfCheckoutStation(currency, bankNoteDenominations, coinDenominations, scaleMaxWeight, scaleSensitivity));
	  	SelfCheckoutSystemLogic scsLogic3 = new SelfCheckoutSystemLogic(new SelfCheckoutStation(currency, bankNoteDenominations, coinDenominations, scaleMaxWeight, scaleSensitivity));
	  	SelfCheckoutSystemLogic scsLogic4 = new SelfCheckoutSystemLogic(new SelfCheckoutStation(currency, bankNoteDenominations, coinDenominations, scaleMaxWeight, scaleSensitivity));
	  	SelfCheckoutSystemLogic scsLogic5 = new SelfCheckoutSystemLogic(new SelfCheckoutStation(currency, bankNoteDenominations, coinDenominations, scaleMaxWeight, scaleSensitivity));
	  	SelfCheckoutSystemLogic scsLogic6 = new SelfCheckoutSystemLogic(new SelfCheckoutStation(currency, bankNoteDenominations, coinDenominations, scaleMaxWeight, scaleSensitivity));
		
		// Adds all the stations to the list of supervised stations.
		ss.add(scsLogic1.station);
		ss.add(scsLogic2.station);
		ss.add(scsLogic3.station);
		ss.add(scsLogic4.station);
		ss.add(scsLogic5.station);
		ss.add(scsLogic6.station);
	}
	
	public static AttendantLogic getInstance() {
	
		if (instance == null)
		{
			instance = new AttendantLogic(new SupervisionStation());
		}
		return instance;
	}
	
	// Removes all coins from the CoinStorageUnit
	public void emptyCoinStorageUnit(SelfCheckoutStation sc) throws ValidationException
	{
		if(loggedIn && ss.supervisedStations().contains(sc)) {
			sc.coinStorage.unload();
		} else {
			throw new ValidationException();
		}
	}
	
	// Removes all banknotes from the BanknoteStorageUnit
	public void emptyBanknoteStorageUnit(SelfCheckoutStation sc) throws ValidationException
	{
		if(loggedIn && ss.supervisedStations().contains(sc)) {
			sc.banknoteStorage.unload();
		} else {
			throw new ValidationException();
		}
	}
	
	//Refills each coin dispenser of each coin denomination to its maximum capacity.
	public void refillsCoinDispenser(SelfCheckoutStation sc) throws SimulationException, OverloadException
	{
		if (loggedIn && ss.supervisedStations().contains(sc)) {
			// Iterates over different denominations in the hashmap; 1 dispenser per denomination.
			for (int i = 0; i < sc.coinDenominations.size(); i++) {
				
				int loadedCoins = sc.coinDispensers.get(sc.coinDenominations.get(i)).size();
				int dispenserCapacity = sc.coinDispensers.get(sc.coinDenominations.get(i)).getCapacity();
				int coinsToAdd = dispenserCapacity - loadedCoins;
				// Adds coins 1 by 1 in the software, until the maximum capacity is reached.
				for (int j = 0; j < coinsToAdd; j++) {	
					sc.coinDispensers.get(sc.coinDenominations.get(i)).load(new Coin(Currency.getInstance("CAD"), sc.coinDenominations.get(i)));
				}
			}
		}
	}
	
	//Refills each banknote dispenser of each banknote denomination to its maximum capacity.
	public void refillsBanknoteDispenser(SelfCheckoutStation sc) throws OverloadException
	{
		if (loggedIn && ss.supervisedStations().contains(sc)) {
			// Iterates over different denominations in the hashmap; 1 dispenser per denomination.
			for (int i = 0; i < sc.banknoteDenominations.length; i++) {
				
				int loadedBanknotes = sc.banknoteDispensers.get(sc.banknoteDenominations[i]).size();
				int dispenserCapacity = sc.banknoteDispensers.get(sc.banknoteDenominations[i]).getCapacity();
				int banknotesToAdd = dispenserCapacity - loadedBanknotes;
				// Adds coins 1 by 1 in the software, until the maximum capacity is reached.
				for (int j = 0; j < banknotesToAdd; j++) {
					sc.banknoteDispensers.get(sc.banknoteDenominations[i]).load(new Banknote(Currency.getInstance("CAD"), sc.banknoteDenominations[i]));
				}
			}
		}
	}
	@Override
	public void enabled(AbstractDevice<? extends AbstractDeviceObserver> device) {
		enabledTrue = true;
		
	}

	@Override
	public void disabled(AbstractDevice<? extends AbstractDeviceObserver> device) {
		disabledTrue = true;
		
	}

	@Override
	public void keyPressed(Keyboard k, char c) {
		// TODO Auto-generated method stub
		userInput += c;
		
		if (userInput.length() == attendantID.length() && isIDEntered == false) {
			inputtedID = userInput;
			userInput = "";
			isIDEntered = true;
		} 
	}
	
	// Limitation: Attendant have to insert his attendantID first, only then he can enter his password.
	public static void wantsToLogin() {
		inputtedPassword = userInput;
		userInput = "";
		if(attendantPassword.equals(inputtedPassword) && attendantID.equals(inputtedID)) {
			loggedIn = true;
		}
		inputtedPassword = "";
		inputtedID = "";
		isIDEntered = false;
	}
	
	public static void wantsToLogout() {
		loggedIn = false;
	}

		SelfCheckoutStation sc = null;	
		ProductDatabases pd;

	
	
	public void attendantBlock(SelfCheckoutSystemLogic sc)
	{
		sc.manualBlock();
	}
	
	
	public void startUpStation(SelfCheckoutSystemLogic sc)
	{
		sc.turnOnStation();
	}
	
	
	public void shutDownStation(SelfCheckoutSystemLogic sc)
	{
		sc.turnOffStation();
	}
	
	public void attendantAddInk(SelfCheckoutSystemLogic sc)
	{
		sc.manualBlock();
		sc.station.printer.disable();
		
		//attendant physically adds ink
		
	}
	
	public void attendantAddPaper(SelfCheckoutSystemLogic sc)
	{
		sc.manualBlock();
		sc.station.printer.disable();
		
		//attendant physically adds paper

	}
	
	/**
	/**
	 * Attendant removes purchased items from bagging area.
	 */
	public void AttendantRemovePurchasedItem(BarcodedProduct x, SelfCheckoutSystemLogic sc) {
	
		try {
			sc.cart.removeFromCart(x);
		} catch (ProductNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("product was not found!"); //this should be implemented in the GUI
		}
		
		
//		for (BarcodedItem item : this.baggingAreaItems)
//			removeItemBaggingArea(item);
//		for (PLUCodedItem item : this.baggingAreaPluItems)
//			removePluItemBaggingArea(item);
//		return true;
	}
	public void AttendantRemovePurchasedItem(PLUCodedWeightProduct x, SelfCheckoutSystemLogic sc) {
		
		try {
			sc.cart.removeFromCart(x);
		} catch (ProductNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("product was not found!"); //this should be implemented in the GUI
		}
	}

	public void notifyOwnBagBlock(SelfCheckoutSystemLogic stationOfConcern) {
		// GUI INSTANCE POPUP OCCURS
		// NOT DONE!!!!
		
	}
	
	public void notifyWeightDiscBlock(SelfCheckoutSystemLogic stationOfConcern ) {
		// GUI INSTANCE POPUP OCCURS
		// NOT DONE!!!!
	}

	public void notifyRemoveProductBlock(SelfCheckoutSystemLogic selfCheckoutSystemLogic) {
		// GUI INSTANCE POPUP OCCURS
		// NOT DONE!!!!
		
	}
	
	public List<PLUCodedProduct> attendantProductLookUp(String Description) throws ValidationException {
		
		List<PLUCodedProduct> foundItem = new ArrayList<PLUCodedProduct>();
		List<String> foundItemDescrip = new ArrayList<String>();
		List<PLUCodedProduct> sortFoundItem = new ArrayList<PLUCodedProduct>();
		
		String lowDescription = Description.toLowerCase();
		if (loggedIn && ss.supervisedStations().contains(sc)) {
		
		for(Map.Entry<PriceLookupCode, PLUCodedProduct> entry : ProductDatabases.PLU_PRODUCT_DATABASE.entrySet()) {
			String pluLowDescription = entry.getValue().getDescription().toLowerCase();
			if(pluLowDescription.startsWith(lowDescription) == true) {
				foundItem.add(entry.getValue());
				foundItemDescrip.add(pluLowDescription);
			}
		}
		
		Collections.sort(foundItemDescrip);
		
		for (int i = 0; i < foundItem.size(); i++) {
			for (int j = 0; j < foundItemDescrip.size(); j++) {
				  if(foundItem.get(i).getDescription().equalsIgnoreCase(foundItemDescrip.get(j))) {
					  sortFoundItem.add(foundItem.get(j));
				  }
			}
		}
		
		
		return sortFoundItem;
	
	} else {
		throw new ValidationException();
	}

	}
	
	public SelfCheckoutSystemLogic getSCSLogic(int logicNum) {
		SelfCheckoutSystemLogic logic = null;
		
		if (logicNum == 1) {
			logic = this.scsLogic1;
		}
		
		if (logicNum == 2) {
			logic = this.scsLogic2;
		}
		
		if (logicNum == 3) {
			logic = this.scsLogic3;
		}
		
		if (logicNum == 4) {
			logic = this.scsLogic4;
		}
		
		if (logicNum == 5) {
			logic = this.scsLogic5;
		}
		
		if (logicNum == 6) {
			logic = this.scsLogic6;
		}
			
		return logic;
		
	}
}