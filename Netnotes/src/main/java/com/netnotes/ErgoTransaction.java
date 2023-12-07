package com.netnotes;

import org.ergoplatform.appkit.NetworkType;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class ErgoTransaction {

    public final static PriceAmount UNKNOWN_PRICE_AMOUNT = new PriceAmount(0, new PriceCurrency("unknown","unknown","unknown",0,"unknown","unknown",null,"unknown",""));

    private AddressInformation m_senderAddress;
    private AddressInformation m_receipientAddress;
    private String m_txId = null;
    private String m_explorerUrl = null;
    private String m_nodeUrl;
    private NetworkType m_networkType;
    private PriceAmount m_ergoAmount;
    private PriceAmount m_feeAmount;
    private long m_created;
    private PriceAmount[] m_tokens;


    public ErgoTransaction(String txId, AddressInformation senderAddress, AddressInformation receipientAddress, PriceAmount ergoAmount,PriceAmount feeAmount, PriceAmount[] tokens, String nodeUrl,  String explorerUrl) throws NullPointerException{
        
        m_txId = txId;
        m_senderAddress = senderAddress;
        m_ergoAmount = ergoAmount;
        m_receipientAddress = receipientAddress;
        m_feeAmount = feeAmount;
        m_tokens = tokens; 
        m_nodeUrl = nodeUrl;

        m_created = System.currentTimeMillis();

    }

    public ErgoTransaction(JsonObject json) throws Exception{
        JsonElement txIdElement = json.get("txId");
        JsonElement ergoAmountElement = json.get("ergoAmount");
        JsonElement feeAmountElement = json.get("feeAmount");
        JsonElement tokensElement = json.get("tokens");
        JsonElement isExplorerElement = json.get("isExplorer");
        JsonElement explorerUrlElement = json.get("explorerUrl");
        JsonElement nodeUrlElement = json.get("nodeUrl");

        if(txIdElement == null || ergoAmountElement == null || feeAmountElement == null){
            throw new Exception("Invalid arguments");
        }

        m_txId = txIdElement.getAsString();
        m_ergoAmount = new PriceAmount(ergoAmountElement.getAsJsonObject());
        m_feeAmount = new PriceAmount(feeAmountElement.getAsJsonObject());
        boolean isJsonExplorer = isExplorerElement != null && isExplorerElement.isJsonPrimitive() ? isExplorerElement.getAsBoolean() : false;
        m_explorerUrl = isJsonExplorer && explorerUrlElement != null && explorerUrlElement.isJsonPrimitive() ? explorerUrlElement.getAsString() : null;
        m_nodeUrl = nodeUrlElement != null && nodeUrlElement.isJsonPrimitive() ? nodeUrlElement.getAsString() : "";
        JsonArray tokensArray = tokensElement != null && tokensElement.isJsonArray() ? tokensElement.getAsJsonArray() : new JsonArray();
        int tokensArrayLength = tokensArray.size();

        PriceAmount[] tokenAmounts = new PriceAmount[tokensArrayLength];
        for(int i = 0; i < tokensArrayLength ; i ++ ){
            JsonElement tokenElement = tokensArray.get(i);
            
            tokenAmounts[i] = tokenElement != null && tokenElement.isJsonObject() ? new PriceAmount(tokenElement.getAsJsonObject()) : UNKNOWN_PRICE_AMOUNT;
        }
        m_tokens = tokenAmounts;
    }

    public PriceAmount[] getTokens(){
        return m_tokens;
    }

    public long getTimeStamp(){
        return m_created;
    }

    public AddressInformation getSenderAddress(){
        return m_senderAddress;
    }

    public PriceAmount getFeeAmount(){
        return m_feeAmount;
    }

    public PriceAmount getErgoAmount(){
        return m_ergoAmount;
    }

    public AddressInformation getReceipientAddressInfo(){
        return m_receipientAddress;
    }

    public NetworkType getNetworkType(){
        return m_networkType;
    }

    public String getExplorerUrl(){
        return m_explorerUrl;
    }

    public String getNodeUrl(){
        return m_nodeUrl;
    }

    public String txId(){
        return m_txId;
    }

    public boolean isSent(){
        return m_txId != null;
    }

    public boolean isExplorer(){
        return m_explorerUrl != null;
    }


    public JsonArray getTokenJsonArray(){
        JsonArray jsonArray = new JsonArray();
        for(int i = 0; i < m_tokens.length ; i++){
            jsonArray.add(m_tokens[i].getJsonObject());
        }
        return jsonArray;
    }

    public JsonObject getJsonObject(){
        JsonObject json = new JsonObject();
      
        json.addProperty("txtId", m_txId);
        json.addProperty("isExplorer", isExplorer());

        if(isExplorer()){
            json.addProperty("explorerUrl", m_explorerUrl);
        }
       
        json.addProperty("nodeUrl", m_nodeUrl);
        json.add("feeAmount", m_feeAmount.getJsonObject());
        json.add("ergoAmount", m_ergoAmount.getJsonObject());
        json.add("tokens", getTokenJsonArray());

        return json;
    }
    
    
}
