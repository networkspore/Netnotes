
Creating a Launcher
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
