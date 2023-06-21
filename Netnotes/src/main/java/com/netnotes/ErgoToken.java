package com.netnotes;

import java.io.File;

import com.google.gson.JsonObject;

public class ErgoToken {

    // private File logFile = new File("currencies-log.txt");
    private String m_tokenId = null;
    private String m_name = null;
    private File m_imageFile = null;

    public ErgoToken(String key, File imageFile) {
        m_imageFile = imageFile;

        setDefaults(key);
    }

    public ErgoToken(String tokenId, String name, String imageLocation) {

        m_tokenId = tokenId;
        m_name = name;
        m_imageFile = new File(imageLocation);
    }

    public JsonObject getJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", m_tokenId);
        jsonObject.addProperty("name", m_name);
        jsonObject.addProperty("imageFile", m_imageFile.getAbsolutePath());
        return jsonObject;
    }

    public void setDefaults(String key) {

        switch (key) {
            case "aht":
                m_name = "Ergo Auction House";
                m_tokenId = "18c938e1924fc3eadc266e75ec02d81fe73b56e4e9f4e268dffffcb30387c42d";
                break;
            case "comet":
                m_name = "Comet Memecoin";
                m_tokenId = "0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b";
                break;
            case "cypx":
                m_name = "CyberVerse";
                m_tokenId = "01dce8a5632d19799950ff90bca3b5d0ca3ebfa8aaafd06f0cc6dd1e97150e7f";
                break;
            case "egio":
                m_name = "ErgoGames.io";
                m_tokenId = "00b1e236b60b95c2c6f8007a9d89bc460fc9e78f98b09faec9449007b40bccf3";
                break;
            case "epos":
                m_name = "ErgoPOS";
                m_tokenId = "00bd762484086cf560d3127eb53f0769d76244d9737636b2699d55c56cd470bf";
                break;
            case "erdoge":
                m_name = "Erdoge Memecoin";
                m_tokenId = "36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1";
                break;
            case "ergold":
                m_name = "Ergold";
                m_tokenId = "e91cbc48016eb390f8f872aa2962772863e2e840708517d1ab85e57451f91bed";
                break;
            case "ergone":
                m_name = "ErgOne NFT";
                m_tokenId = "fcfca7654fb0da57ecf9a3f489bcbeb1d43b56dce7e73b352f7bc6f2561d2a1b";
                break;
            case "ergopad":
                m_name = "ErgoPad";
                m_tokenId = "d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413";
                break;
            case "ermoon":
                m_name = "ErMoon";
                m_tokenId = "9dbc8dd9d7ea75e38ef43cf3c0ffde2c55fd74d58ac7fc0489ec8ffee082991b";
                break;
            case "exle":
                m_name = "Ergo-Lend";
                m_tokenId = "007fd64d1ee54d78dd269c8930a38286caa28d3f29d27cadcb796418ab15c283";
                break;
            case "flux":
                m_name = "Flux";
                m_tokenId = "e8b20745ee9d18817305f32eb21015831a48f02d40980de6e849f886dca7f807";
                break;
            case "getblock":
                m_name = "GetBlock.io";
                m_tokenId = "4f5c05967a2a68d5fe0cdd7a688289f5b1a8aef7d24cab71c20ab8896068e0a8";
                break;
            case "kushti":
                m_name = "Kushti Memecoin";
                m_tokenId = "fbbaac7337d051c10fc3da0ccb864f4d32d40027551e1c3ea3ce361f39b91e40";
                break;
            case "love":
                m_name = "Love NFT";
                m_tokenId = "3405d8f709a19479839597f9a22a7553bdfc1a590a427572787d7c44a88b6386";
                break;
            case "lunadog":
                m_name = "LunaDog NFT";
                m_tokenId = "5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e";
                break;
            case "migoreng":
                m_name = "Mi Goreng";
                m_tokenId = "0779ec04f2fae64e87418a1ad917639d4668f78484f45df962b0dec14a2591d2";
                break;
            case "neta":
                m_name = "anetaBTC";
                m_tokenId = "472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8";
                break;
            case "obsidian":
                m_name = "Adventurers DAO";
                m_tokenId = "2a51396e09ad9eca60b1bdafd365416beae155efce64fc3deb0d1b3580127b8f";
                break;
            case "paideia":
                m_name = "Paideia DAO";
                m_tokenId = "1fd6e032e8476c4aa54c18c1a308dce83940e8f4a28f576440513ed7326ad489";
                break;
            case "proxie":
                m_name = "Proxies NFT";
                m_tokenId = "01ddcc3d0205c2da8a067ffe047a2ccfc3e8241bc3fcc6f6ebc96b7f7363bb36";
                break;
            case "quacks":
                m_name = "duckpools.io";
                m_tokenId = "089990451bb430f05a85f4ef3bcb6ebf852b3d6ee68d86d78658b9ccef20074f";
                break;
            case "sigrsv":
                m_name = "Sigma Reserve";
                m_tokenId = "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0";
                break;
            case "sigusd":
                m_name = "Sigma USD";
                m_tokenId = "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04";
                break;
            case "spf":
                m_name = "Spectrum Finanace";
                m_tokenId = "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d";
                break;
            case "terahertz":
                m_name = "Swamp Audio";
                m_tokenId = "02f31739e2e4937bb9afb552943753d1e3e9cdd1a5e5661949cb0cef93f907ea";
                break;
            case "walrus":
                m_name = "Walrus Dao";
                m_tokenId = "59ee24951ce668f0ed32bdb2e2e5731b6c36128748a3b23c28407c5f8ccbf0f6";
                break;
            case "woodennickels":
                m_name = "Wooden Nickles";
                m_tokenId = "4c8ac00a28b198219042af9c03937eecb422b34490d55537366dc9245e85d4e1";
                break;
        }

    }

    public String getName() {
        return m_name;
    }

    public File getImageFile() {
        return m_imageFile;
    }

    public String getTokenId() {
        return m_tokenId;
    }

}
