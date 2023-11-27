package com.netnotes;

import java.time.LocalDateTime;
import java.util.ArrayList;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

public class AmountMenuButton extends BufferedMenuButton {

    private final SimpleObjectProperty<AddressData> m_selectedAddressData = new SimpleObjectProperty<>(null);
   
    private final SimpleObjectProperty< AmountBoxes> m_amountBoxes = new SimpleObjectProperty<>(null);
    private boolean m_nullDisable = false;
    

    public AmountMenuButton(){
        super();
        setupListeners();
    }
    public AmountMenuButton(String urlString,  String text){
        super(text, urlString);
        setupListeners();
    }

    public AmountMenuButton( String urlString, double imageWidth){
        super(urlString, imageWidth);

        setupListeners();
    }


    private void setupListeners(){
        Runnable updateList = ()->{
            AddressData addressData = m_selectedAddressData.get();
            AmountBoxes amountBoxes = m_amountBoxes.get();
            if(addressData != null){
                ArrayList<PriceAmount> tokenList = addressData.getConfirmedTokenList();
                long timeStamp = System.currentTimeMillis();

                for(int i = 0; i < tokenList.size() ; i ++){
                    PriceAmount tokenAmount = tokenList.get(i);
                    String tokenId = tokenAmount.getTokenId();

                    boolean isOmitted =  amountBoxes == null ? false : (amountBoxes.getAmountBox(tokenId) != null ? true : false);

                    AmountMenuItem amountMenuItem = getAmountMenuItem(tokenId);

                    if(amountMenuItem != null){
                        if(isOmitted){
                            removeAmountMenuItem(tokenId);
                        }else{
                            amountMenuItem.priceAmountProperty().set(tokenAmount);
                            amountMenuItem.setTimeStamp(timeStamp);
                            
                            amountMenuItem.setOnAction(e->{
                                AmountBoxes currentAmountBoxes = m_amountBoxes.get();
                                if(currentAmountBoxes != null){
                                    PriceAmount currentPriceAmount = amountMenuItem.priceAmountProperty().get();
                                    AmountSendBox newAmountSendBox = new AmountSendBox(new PriceAmount(0, currentPriceAmount.getCurrency()), getScene(), true);
                                    newAmountSendBox.balanceAmountProperty().set(currentPriceAmount);
                                    currentAmountBoxes.add(newAmountSendBox);
                                }
                            });
                            
                        }
                    }else{
                        if(!isOmitted){
                            AmountMenuItem newAmountMenuItem = new AmountMenuItem(tokenAmount);
                            newAmountMenuItem.setTimeStamp(timeStamp);
                            getItems().add(newAmountMenuItem);
                            newAmountMenuItem.setOnAction(e->{
                                AmountBoxes currentAmountBoxes = m_amountBoxes.get();
                                if(currentAmountBoxes != null){
                                  //  removeAmountMenuItem(newAmountMenuItem.getTokenId());
                                    PriceAmount currentPriceAmount = newAmountMenuItem.priceAmountProperty().get();
                                    AmountSendBox newAmountSendBox = new AmountSendBox(new PriceAmount(0, currentPriceAmount.getCurrency()), getScene(), true);
                                    newAmountSendBox.balanceAmountProperty().set(currentPriceAmount);
                                    currentAmountBoxes.add(newAmountSendBox);
                                }
                            });
                        }
                    }
                }

                removeOld(timeStamp);
            }else{
                getItems().clear();
            }

        };
        
        ChangeListener<? super LocalDateTime> selectedAddressChangeListener = (obs, oldVal, newVal) -> updateList.run();

        m_selectedAddressData.addListener((obs, oldval, newval) ->{ 
            getItems().clear();
            updateList.run();
            if(oldval != null){
                oldval.getLastUpdated().removeListener(selectedAddressChangeListener);
            }
            if(newval != null){
                if(m_nullDisable){
                    setDisable(false);
                }
                newval.getLastUpdated().addListener(selectedAddressChangeListener);
            }else{
                if(m_nullDisable){
                    setDisable(true);
                }
            }
        });
    
        ListChangeListener<AmountBox> amountBoxListener = new ListChangeListener<AmountBox>() {
            public void onChanged(Change<? extends AmountBox> c) {
                updateList.run();
            }
        };
    

        m_amountBoxes.addListener((obs,oldval, newval)->{
            if(oldval != null){
                oldval.amountsList().removeListener(amountBoxListener);
            } 
            if(newval != null){
                newval.amountsList().addListener(amountBoxListener);
                
            }
        });

    }

    public void setAmountBoxes(AmountBoxes amountBoxes){
        m_amountBoxes.set(amountBoxes);
    }

    public AmountBoxes getAmountBoxes(){
        return m_amountBoxes.get();
    }
    
    public void setNullDisable(boolean nullDisable){
        m_nullDisable = nullDisable;
        if(m_selectedAddressData.get() == null){
            setDisable(nullDisable);
        }

        if(!nullDisable){
            setDisable(false);
        }
    }
    
    public SimpleObjectProperty<AddressData> addressDataProperty(){
        return m_selectedAddressData;
    }

    /*public ObservableList<String> omittedTokenIds(){
        return m_omittedTokenIds;
    }*/

    public void removeOld(long timeStamp){
        ArrayList<String> removeList = new ArrayList<>();

        for(int i = 0; i < getItems().size() ; i++){
            MenuItem menuItem = getItems().get(i);
            
            if(menuItem != null && menuItem instanceof AmountMenuItem){
                AmountMenuItem amountMenuItem = (AmountMenuItem) menuItem;
                if(amountMenuItem.getTimeStamp() < timeStamp){
                    removeList.add(amountMenuItem.getTokenId());        
                }
            }
        }

        for(String tokenId : removeList){
            removeAmountMenuItem(tokenId);
        }
    }



    /*public boolean getIsOmitted(String tokenId){
        if(tokenId == null){
            return true;
        }
        for(int i = 0; i < m_omittedTokenIds.size() ; i++){
            String id = m_omittedTokenIds.get(i);
            if(id.equals(tokenId)){
                return true;
            }
        }
        
        return false;
    }*/

    public AmountMenuItem getAmountMenuItem(String tokenId){
        if(tokenId != null){
            for(int i = 0; i < getItems().size() ; i++){
                MenuItem menuItem = getItems().get(i);
                
                if(menuItem != null && menuItem instanceof AmountMenuItem){
                    AmountMenuItem amountMenuItem = (AmountMenuItem) menuItem;
                    
                    if(amountMenuItem.getTokenId().equals(tokenId)){
                        return amountMenuItem;
                    }
                }
            }
        }

        return null;
    }

    public void removeAmountMenuItem(String tokenId){
        if(tokenId != null){
            for(int i = 0; i < getItems().size() ; i++){
                MenuItem menuItem = getItems().get(i);
                
                if(menuItem != null && menuItem instanceof AmountMenuItem){
                    AmountMenuItem amountMenuItem = (AmountMenuItem) menuItem;
                    
                    if(amountMenuItem.getTokenId().equals(tokenId)){
                        getItems().remove(i);
                        break;
                    }
                }
            }
        }
    }

     public AmountMenuItem getAmountMenuItemByFriendlyId(String friendlyId){
        if(friendlyId != null){
            for(int i = 0; i < getItems().size() ; i++){
                MenuItem menuItem = getItems().get(i);
                
                if(menuItem != null && menuItem instanceof AmountMenuItem){
                    AmountMenuItem amountMenuItem = (AmountMenuItem) menuItem;
                    
                    if(amountMenuItem.getFriendlyId().equals(friendlyId)){
                        return amountMenuItem;
                    }
                }
            }
        }

        return null;
    }

    public void removeAmountMenuItemByFriendlyId(String friendlyId){
        if(friendlyId != null){
            for(int i = 0; i < getItems().size() ; i++){
                MenuItem menuItem = getItems().get(i);
                
                if(menuItem != null && menuItem instanceof AmountMenuItem){
                    AmountMenuItem amountMenuItem = (AmountMenuItem) menuItem;
                    
                    if(amountMenuItem.getFriendlyId().equals(friendlyId)){
                        getItems().remove(i);
                        break;
                    }
                }
            }
        }
    }
}
