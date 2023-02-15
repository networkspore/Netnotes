Getting Started
-------------

If your new to the Java environment there are a lot of things that need to be setup before you can make an app that interfaces with the <a href="https://github.com/ergoplatform/ergo-appkit">ergo app-kit</a>.

If your a complete beginner in java, the first hurdle to cross is the development directory. Java has a directory structure which it requires, rather than suggests, and for a barebones setup you can basically have everything tree off of the src folder:

appdirectory -> src -> main -> java -> groupID eg="com" -> artifactID eg: netnotes

Before we go much further I guess I should discuss the dev environment. I used VScode so that's what I will explain. Using VScode you can install the maven, or gradle package extension, which is needed to install build plugins (which are also needed). You can also install the graalvm, and java language exensions using vscode. 

As for the builder, I looked into gradle a little bit and still have it installed but it seems pretty advanced, so as this is a bare-bones setup I'm goign to explain things from the maven point of view.

You will want to place a main class in the artifactID folder if you intend to build a JavaFx (windowed) project. Lets build an app that will give you a new wallet Mneumonic!:

First your 'Main.java' class:

```
package groupid.artifactid;

import javafx.application.Application;

public class Main {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
```
Then your App.java class:
```
package groupid.artifactid;

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
        
        //Use this to set the icon that windows uses when the program is running
        //place icon.png beneath the /src/main/resources folder 
        //stage.getIcons().add(new Image("/icon.png"));
        
        stage.setScene(scene);
        stage.show();
    }
}
```


Note:

When you create a .java file the package declartion at the top will correspond to this directory path, starting after the java directory:

package groupid.artifactid

imports (another) groupid.artifactid.ClassName;

Probably the step in the process that can be the most frustrating is figuring out the maven or gradle java build system. Using maven you will want to create a pom.xml file which contains the following (I'm targeting java 17, because of the gluonfx plugin which will be discussed later:

```

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

```

This pom file covers a lot of things at once. 

You need the openFx dependancies to show window frames and then there's the javafx-maven-plugin, which is also required, (if you go with gradle you'll need that plugin). You'll also need the maven-shade-plugin. (gradle uses a shadowJar plugin) Maven installs all of these dependancies, and plugins when you save the pom file (if you have the VS code extension installed), so no further downloading is required.

All those dependancies in the 'dependancy managment' section, are their because of the differences between the environment and the environment that the app-kit was compiled with. To make sure you have the right dependencies look in the dependencies section of maven in vscode. You might have to expand the appkit icon, in the maven dependencies section to see any dependency conflicts. 

If the shade plugin is working you should now be able to compile a jar using:

mvn package
    
you can use a terminal, that's in your projects root directory.

This will create a target folder at your project root and than build a .jar file and place it in that target folder. (if all goes well)

To run this jar, you will need to use the absolute path (C:\project\target\filename.jar), or be in the target directory with the terminal. You can execute the line:

java -jar filename.jar

To run your app!

Making a Launcher
----
For myself using a command line doesn't seem like the right way to be launching an application. So, in order to make a launcher, I chose to get the <a href="https://github.com/gluonhq/graal/releases">graalvm from Gluon</a> which is meant for building javaFX into an exe using graalvm's native-image function. To do that you'll need a new plugin <a href="https://github.com/gluonhq/gluonfx-maven-plugin">gluonfx plugin</a> which can be installed in maven. (they also have a gradle plugin if that's what your into).

I haven't been able to get the gluonfx to build an app-kit project, and I assume that it might be possible, after you use the shade program, to make a jar, but I don't mind having a laucher application, because this can always help for updating the jar file to new versions later.

The bare bones of a launcher class would be : 

````
package com.launcher;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().exec("cmd.exe /C  javaw -jar filename.jar");
        } catch (IOException e) {

        }

    }
}
````

You will see that I created a side folder called launcher and placed the Main.java class in it, with my launcher code.

If you want your application to use a custom icon, rather than the gluon icon,

create an icon.ico file and place it in the yourproject/src/Windows/ directory for the gluonfx plugin to catch.
 
 Now you can add the gluonfx plugin to your your pom file:

<plugin>
  <groupId>com.gluonhq</groupId>
  <artifactId>gluonfx-maven-plugin</artifactId>
  <version>1.0.16</version>
  <configuration>
    <mainClass>com.launcher.Main</mainClass>
   </configuration>
</plugin>

When you run mvn gluonfx:build it will then build an exe of the mainClass that you set.

However, you can't just build gluonfx files.... (sorry)... It's hard to get gluon to actually work, it might keep giving you errors that it can't find the cl.exe

cl.exe is the name of the C compiler that it wants to use and can't find.

You'll need to install visual studio, (community edition) and the .net developent package, in order to make sure you have the windows compiler, so that you change the jar into an exe.

You'll also need the gluonfx version of graalvm (you don't need to change your other graalvm), but rather just download and install the <a href="https://github.com/gluonhq/graal/releases">gluon graalvm</a> into a new directory. You'll have to then use the x64 Native Tools Command Prompt and set a GRAALVM_HOME variable to the directory (type: set GRAALVM_HOME=C:/dev/gluongraal). You can check out <a href="https://docs.gluonhq.com/#_create_run_a_native_image_of_the_application"> this page </a> for more info. There are also tutorials online for this if you run into snags. 

You may also alter the settings in VSCode to set the terminal to use the native tools one. change Shell path from cmd.exe to
```
cmd.exe /k "C:\\Program Files (x86)\\Microsoft Visual Studio\\2019\\Community\\VC\\Auxiliary\\Build\\vcvars64.bat"
```
If all goes well you now have a launcher exe that will open your ergo-appkit enabled jar file!
  
And now you can get started making your ergo-appkit project!
