Creating runtime image (first navigate into the "jre" folder)

C:\jdk\jdk-17.0.2\bin\jlink --module-path C:\jdk\jdk-17.0.2\jmods --add-modules java.desktop,java.sql,java.xml,jdk.unsupported,jdk.accessibility --output jre-17 --no-header-files --no-man-pages --strip-debug --compress=2

The "--no-header-files" and "no-man-pages" are very safe to use.
The "--strip-debug" option is safe, but keep in mind that it might prevent debugging via the IDE.
The "--compress=2" is a bit obscure, hopefully that won't have any bad side effects.

When a new version of Java is available, re-create the runtime image (the java version appears twice in the command!), test it and push the changes.

JDK is taken from https://adoptium.net/
