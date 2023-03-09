package com.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SetupData {

    private String ergoJarHash = "";
    private String ergoJarURL = "";
    private Version ergoVersion = null;

    private String mainJarURL = "";
    private String mainJarHash = "";
    private Version mainJarVersion = null;

    private String launcherURL = "";
    private String launcherHash = "";
    private Version launcherVersion = null;

    private Version javaVersion = null;
    private String javaName = "Java 19 (x64)";
    private String javaURL = "https://download.oracle.com/java/19/latest/jdk-19_windows-x64_bin.exe";
    private String javaHash = "";

    public SetupData(JsonObject jsonObject) throws Exception {

        createFromJson(jsonObject);

    }

    private void createFromJson(JsonObject jsonObject) {
        ergoJarURL = jsonObject.get("ergoJarURL").getAsString();
        ergoJarHash = jsonObject.get("ergoJarHash").getAsString();
        ergoVersion = new Version(jsonObject.get("ergoVersion").getAsString());

        mainJarVersion = new Version(jsonObject.get("mainJarVersion").getAsString());
        mainJarURL = jsonObject.get("mainJarURL").getAsString();
        mainJarHash = jsonObject.get("mainJarHash").getAsString();

        javaName = jsonObject.get("javaName").getAsString();
        javaURL = jsonObject.get("javaURL").getAsString();
        javaVersion = new Version(jsonObject.get("javaVersion").getAsString());
        mainJarHash = jsonObject.get("javaHash").getAsString();

        launcherURL = jsonObject.get("launcherURL").getAsString();
        launcherHash = jsonObject.get("launcherHash").getAsString();
        launcherVersion = new Version(jsonObject.get("laucherVersion").getAsString());
    }

    public String getMainJarURL() {
        return mainJarURL;
    }

    public String getMainJarHash() {
        return mainJarHash;
    }

    public Version getMainVersion() {
        return mainJarVersion;
    }

    public Version getErgoVersion() {
        return ergoVersion;
    }

    public String getErgoJarHash() {
        return ergoJarHash;
    }

    public String getErgoJarURL() {
        return ergoJarURL;
    }

    public String getJavaHash() {
        return javaHash;
    }

    public Version getJavaVersion() {
        return javaVersion;
    }

    public String getJavaName() {
        return javaName;
    }

    public String getJavaURL() {
        return javaURL;
    }

    private String getJsonString() {

        return getJsonObject().toString();

    }

    private JsonObject getJsonObject() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("ergoJarHash", ergoJarHash);
        jsonObj.addProperty("ergoJarURL", ergoJarURL);
        jsonObj.addProperty("ergoVersion", ergoVersion.get());

        jsonObj.addProperty("mainJarURL", mainJarURL);
        jsonObj.addProperty("mainJarHash", mainJarHash);
        jsonObj.addProperty("mainJarVersion", mainJarVersion.get());

        jsonObj.addProperty("launcherURL", launcherURL);
        jsonObj.addProperty("launcherHash", launcherHash);
        jsonObj.addProperty("mainJarVersion", launcherVersion.get());

        jsonObj.addProperty("javaURL", javaURL);
        jsonObj.addProperty("javaHash", launcherHash);
        jsonObj.addProperty("javaVersion", javaVersion.get());
        jsonObj.addProperty("javaName", javaName);

        return jsonObj;
    }

    public void save(Path savePath) throws IOException {

        Files.writeString(savePath, getJsonString());
    }

}
