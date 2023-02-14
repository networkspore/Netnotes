Getting Started
-------------

If your new to the Java environment there are a lot of things that need to be setup before you can make an app that interfaces with the <a href="https://github.com/ergoplatform/ergo-appkit">ergo app-kit</a>.

If your a complete beginner in java, the first hurdle to cross is the development directory. Java has a directory structure which it requires, rather than suggests, and for a barebones setup you can basically have everything tree off of the src folder:

appdirectory -> src -> main -> java -> groupID eg="com" -> artifactID eg: netnotes

Before we go much further I guess I should discuss the dev environment. I used VScode so that's what I will explain. Using VScode you can install the maven, or gradle package extension, which is needed to install build plugins (which are also needed). You can also install the graalvm, and java language exensions using vscode. 

As for the builder, I looked into gradle a little bit and still have it installed but it seems pretty advanced, so as this is a bare-bones setup I'm goign to explain things from the maven point of view.

You will want to place a main class in the artifactID folder if you intend to build a JavaFx (windowed) project. Lets build an app that will give you a new wallet Mneumonic!:

First your 'Main.java' class:


package groupID.artifactID;

import javafx.application.Application;

public class Main {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}

Then your App.java class:

package yourgroupid.yourartifactid;

import org.ergoplatform.appkit.Mnemonic;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.stage.Stage;


public class App extends Application {

    @Override
    public void start(Stage stage) {
    
        VBox layout = new VBox();
        Scene scene = new Scene(layout, 600, 425);

        String mneumonicString =  Mnemonic.generateEnglishMnemonic();

        Label mneumonicText = new Label(mneumonicString);
        mneumonicText.setWrapText(true);
        layout.getChildren().add(mneumonicText);
        
        stage.setScene(scene);
        stage.show();
    }
}

If you wanted to include any sesources in your app (like images, or css files) you can then place them in the src/main/resources folder

appdirectory -> src -> main -> resources 

Note:

When you create a .java file the package declartion at the top will correspond to this directory path, starting after the java directory:

package groupID.artifactID

imports (another) groupID.artifactID.ClassName;

Probably the step in the process that can be the most frustrating is figuring out the maven or gradle java build system. Using maven you will want to create a pom.xml file which contains the following (I'm targeting java 17, because of the gluonfx plugin which will be discussed later:

-------

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>groupid</groupId>
    <artifactId>artifactid</artifactId>
    <version>0.0.1</version>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <exec.mainClass>roupid.artifactid.Main</exec.mainClass>
    </properties>
    <dependencies>

        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>17</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-base</artifactId>
            <version>17</version>
        </dependency>
        
     <dependency>
            <groupId>org.ergoplatform</groupId>
            <artifactId>ergo-appkit_2.12</artifactId>
            <version>5.0.0</version>
        </dependency>
     
    </dependencies>
    
    
        <build>
            <plugins>
                    
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <version>0.0.8</version>
                <configuration>
                <mainClass>groupid.artifactid.Main</mainClass>
                </configuration>
            </plugin>

            <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.4.1</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
             <configuration>
                    <transformers>
                        <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                            <mainClass>groupid.artifactid.Main</mainClass>
                        </transformer>
                    </transformers>
                     <filters>
                        <filter>
                        <artifact>*:*</artifact>
                        <excludes>
                            <exclude>META-INF/*.SF</exclude>
                            <exclude>META-INF/*.DSA</exclude>
                            <exclude>META-INF/*.RSA</exclude>
                        </excludes>
                        </filter>
                    </filters>
                </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
   </plugins>
        </build>


    <dependencyManagement>
    

        <dependencies>

            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-api</artifactId>
                <version>1.7.36</version>
            </dependency>


            <dependency>
                <groupId>org.typelevel</groupId>
                <artifactId>cats-kernel_2.12</artifactId>
                <version>0.9.0</version>
            </dependency>


            


            <dependency>
                <groupId>org.bouncycastle</groupId>
                <artifactId>bcprov-jdk15on</artifactId>
                <version>1.66</version>
            </dependency>


            <dependency>
                <groupId>com.google.guava</groupId>
                <artifactId>guava</artifactId>
                <version>21.0</version>
            </dependency>


            <dependency>
                <groupId>org.scala-lang</groupId>
                <artifactId>scala-reflect</artifactId>
                <version>2.12.2</version>
            </dependency>


            <dependency>
                <groupId>org.scorexfoundation</groupId>
                <artifactId>sigma-state_2.12</artifactId>
                <version>5.0.1</version>
            </dependency>


            <dependency>
                <groupId>org.scala-lang</groupId>
                <artifactId>scala-library</artifactId>
                <version>2.12.6</version>
            </dependency>

        

            <dependency>
                <groupId>com.google.code.gson</groupId>
                <artifactId>gson</artifactId>
                <version>2.8.5</version>
            </dependency>

        </dependencies>

    </dependencyManagement>
</project>

------

This pom file covers a lot of things at once. 

You need the openFx dependancies to show window frames, then there's the javafx-maven-plugin, which is also required, (if you go with gradle you'll need that plugin). You'll also need the maven-shade-plugin. (gradle uses a shadow plugin) Maven installs all of these dependancies, and plugins when you save the pom file, so no further downloading is required.

If you don't install the shade plugin, you'll need to add all those dependancies to the 'dependancy managment' section, in order to get maven to say that everything is ok. The shade plugin is designed to package all the dependancies into one executable jar, but it can't package certain secure files, so you need to add those dependancies to the 'excludes' section. Someone on stack overflow suggested the:
 <excludes>
    <exclude>META-INF/*.SF</exclude>
    <exclude>META-INF/*.DSA</exclude>
    <exclude>META-INF/*.RSA</exclude>
</excludes>

So that the shade plugin will skip all of the secure files. 

In order to build your jar you should now be able to run from the command line, 

mvn package

when you are at the root of your project directory.

This will build a .jar file and place it in a target folder at the root of your project. (if all goes well)

To run this jar, you can execute the line:

java -jar filename.jar


However, for myself this doesn't seem like the right way to be launching this application. So, in order to make a launcher, using javaFX, you'll want to get the <a href="https://github.com/gluonhq/graal/releases">graalvm from Gluon</a> which is meant for building javaFX into an exe and then you'll need a new plugin <a href="https://github.com/gluonhq/gluonfx-maven-plugin">gluonfx plugin</a> can then be installed in maven.

To do this create a different folder, called launcher in your groupID folder and then copy the main class you created ealier into it and change the code to this:

package com.launcher;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().exec("cmd.exe /C  javaw -jar artifactID.jar");
        } catch (IOException e) {

        }

    }
}

And then in your perviously created App.java add:

stage.getIcons().add(new Image("/icon.png"));

And place "icon.png" in the /src/main/resources folder.

You can also convert the icon.png to icon.ico (using an online file converter, or your app of choice)

 Then place icon.ico file in a new /src/Windows/ directory.
 
 Now you can add the gluonfx plugin to the plugins section of your pom file:

<plugin>
  <groupId>com.gluonhq</groupId>
  <artifactId>gluonfx-maven-plugin</artifactId>
  <version>1.0.16</version>
  <configuration>
    <mainClass>com.launcher.Main</mainClass>
   </configuration>
</plugin>

However, you can't just build gluonfx files.... (sorry)

You'll need to install visual studio, (community edition) and the .net developent package, in order to make sure you have cli so that gluon can use graalvm to change the jar into an exe.

You'll also need the gluonfx version of graalvm (you don't need to change your other graalvm), but rather just download and install the <a href="https://github.com/gluonhq/graal/releases">gluon graalvm</> into a new directory. You'll have to then use the x64 Native Tools Command Prompt and set a GRAALVM_HOME variable to the directory (type: set GRAALVM_HOME=C:/dev/gluongraal). You can check out <a href="https://docs.gluonhq.com/#_create_run_a_native_image_of_the_application"> this page </a> for more info. There are also tutorials online for this if you run into snags. 

You may also alter the settings in VSCode to set the terminal to use the native tools one. (File→Settings…→Tools→Terminal, and change Shell path from cmd.exe to cmd.exe /k "<path to VS2019>\VC\Auxiliary\Build\vcvars64.bat
  
If all goes well (and it probably won't) you now have a launcher exe that will open your ergo-appkit enabled jar file!
  
And now you can get started making your ergo-appkit project!
