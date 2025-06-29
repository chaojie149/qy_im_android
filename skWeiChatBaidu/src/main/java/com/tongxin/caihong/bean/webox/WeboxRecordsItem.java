package com.tongxin.caihong.bean.webox;

public class WeboxRecordsItem {
	private int amount;
	private String serialNumber;
	private int balance;
	private String requestId;
	private String currency;
	private String tradeType;
	private String createDateTime;
	private String direction;

	public void setAmount(int amount){
		this.amount = amount;
	}

	public int getAmount(){
		return amount;
	}

	public void setSerialNumber(String serialNumber){
		this.serialNumber = serialNumber;
	}

	public String getSerialNumber(){
		return serialNumber;
	}

	public void setBalance(int balance){
		this.balance = balance;
	}

	public int getBalance(){
		return balance;
	}

	public void setRequestId(String requestId){
		this.requestId = requestId;
	}

	public String getRequestId(){
		return requestId;
	}

	public void setCurrency(String currency){
		this.currency = currency;
	}

	public String getCurrency(){
		return currency;
	}

	public void setTradeType(String tradeType){
		this.tradeType = tradeType;
	}

	public String getTradeType(){
		return tradeType;
	}

	public void setCreateDateTime(String createDateTime){
		this.createDateTime = createDateTime;
	}

	public String getCreateDateTime(){
		return createDateTime;
	}

	public void setDirection(String direction){
		this.direction = direction;
	}

	public String getDirection(){
		return direction;
	}

	@Override
 	public String toString(){
		return 
			"WeboxRecordsItem{" +
			"amount = '" + amount + '\'' + 
			",serialNumber = '" + serialNumber + '\'' + 
			",balance = '" + balance + '\'' + 
			",requestId = '" + requestId + '\'' + 
			",currency = '" + currency + '\'' + 
			",tradeType = '" + tradeType + '\'' + 
			",createDateTime = '" + createDateTime + '\'' + 
			",direction = '" + direction + '\'' + 
			"}";
		}
}
