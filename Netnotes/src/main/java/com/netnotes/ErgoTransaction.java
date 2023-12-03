package com.netnotes;

import org.ergoplatform.appkit.ErgoClient;
import org.ergoplatform.appkit.ErgoToken;
import org.ergoplatform.appkit.InputBoxesSelectionException;
import org.ergoplatform.appkit.NetworkType;
import org.ergoplatform.appkit.RestApiErgoClient;
import org.ergoplatform.appkit.SignedTransaction;
import org.ergoplatform.appkit.UnsignedTransaction;

import com.satergo.Wallet;
import com.satergo.WalletKey;
import com.satergo.ergo.ErgoInterface;

public class ErgoTransaction {

    private String m_txId = null;
    private String m_insuffientFunds = null;
    private String m_keyException = null;
    private String m_explorerUrl;
    private String m_nodeUrl;
    private NetworkType m_networkType;
    private long m_nanoErg;
    private AddressInformation m_receipientAddress;
    private String m_senderAddress;
    private long m_fee;
    private long m_created;
    private AmountConfirmBox[] m_ergoTokens;

    
    public static String transact(ErgoClient ergoClient, SignedTransaction signedTx) {
        return ergoClient.execute(ctx -> {
            String quoted = ctx.sendTransaction(signedTx);
            return quoted.substring(1, quoted.length() - 1);
        });
    }
 

    public ErgoTransaction(AddressData addressData, Wallet wallet, long nanoErg, AddressInformation receipientAddress, String nodeApiAddress, String apiKey, String explorerUrl, long fee, AmountBox[] tokenBoxes){
        m_explorerUrl = explorerUrl;
        m_nodeUrl = nodeApiAddress;
        m_nanoErg = nanoErg;
        m_receipientAddress = receipientAddress;
        m_senderAddress = addressData.getAddress().toString();
        m_networkType = addressData.getNetworkType();
        
        int amountOfTokens = tokenBoxes != null && tokenBoxes.length > 0 ? tokenBoxes.length : 0;
        ErgoToken[] tokenArray = new ErgoToken[amountOfTokens] ;
        m_ergoTokens = new AmountConfirmBox[amountOfTokens];                        
        
        if(amountOfTokens > 0 && tokenBoxes != null && tokenArray != null){
            for(int i = 0; i < amountOfTokens ; i++){
                AmountBox box = tokenBoxes[i];
                if(box != null && box instanceof AmountConfirmBox){
                    AmountConfirmBox confirmBox = (AmountConfirmBox) box;
                    m_ergoTokens[i] = confirmBox;
                        
                        tokenArray[i] = confirmBox.getErgoToken();
                    
                }
            }
                
        }

        
        m_fee = fee;

        try {
            ErgoClient ergoClient = RestApiErgoClient.create(m_nodeUrl, m_networkType, apiKey, m_explorerUrl);

            UnsignedTransaction unsignedTx = ErgoInterface.createUnsignedTransaction(ergoClient,
                        wallet.addressStream(m_networkType).toList(),
                    receipientAddress.getAddress(), m_nanoErg, m_fee, addressData.getAddress(), tokenArray);

            String txId = transact(ergoClient, ergoClient.execute(ctx -> {
                try {
                    return wallet.key().sign(ctx, unsignedTx, wallet.myAddresses.keySet());
                } catch (WalletKey.Failure ex) {
                    setKeyException(ex.toString());
                    return null;
                }
            }));
            
            setTxtId(txId);
            
        } catch (InputBoxesSelectionException ibsEx) {
            setInsuffientFunds(ibsEx.toString());
            
        }
           
        m_created = System.currentTimeMillis();
    }

    public long getTimeStamp(){
        return m_created;
    }

    public String getSenderAddress(){
        return m_senderAddress;
    }

    public long getFee(){
        return m_fee;
    }

    public long getNanoErgs(){
        return m_nanoErg;
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

    private void setTxtId(String txId){
        m_txId = txId;
        
    }

    public boolean isSent(){
        return m_txId != null;
    }

    private void setInsuffientFunds(String ibsEx){
        m_insuffientFunds = ibsEx;
    }

    public String getInsuffientFunds(){
        return m_insuffientFunds;
    }

    private void setKeyException(String keyException){
        m_keyException = keyException;
    }

    public boolean isUnauthorized(){
        return m_keyException != null;
    }

    public boolean isInsufficientFunds(){
        return m_insuffientFunds != null;
    }

 
    
}
