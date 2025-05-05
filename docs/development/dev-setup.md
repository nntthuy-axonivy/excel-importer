# Dev Environment Setup

The excel-importer is an Eclipse RCP extension, contributing to the Axon Ivy Designer featureset. This guide explains the steps to setup the development environment for it.

It was written for LTS12 and EclipseRCP 2024-03.

### Eclipse RCP

To start with, we need an Eclipse IDE for development.

1. Download the latest EclipseRCP IDE from the official site. 
2. Unpack the ZIP to a location of your choice
3. Run the Eclipse binary in it; and pick a yet empty workspace for development

### Workspace

Now you are ready to import the "excel-importer" extension

1. Click on the Menu "File" -> "Import" -> "General" > Existing projects into workspace"
2. Select the "excel-importer-modules" and "excel-importer" project within your local clone of this repo.
3. The extension will not yet compile; until we have a valid target-platform

### Target Platform

Here's how you can setup your eclipseRCP to build upon the AxonIvyDesigner platform.

1. Navigate to "Window" > "Preferences" > "Plug-in Development" > "Target Platform"  Select the 'ivy.target' file from your local workspace. 
2. Press "Apply and Close" ... wait for the platform fetching (south east progress bar of eclipse main windows) 
3. Compile the 'excel-connector' extension. You may have to do this manually after loading the platform: Menu "Project" > "Clean" 
4. Now all projects in the workspace should be without errors.

![platform-target-lts.png](platform-target-lts.png)

### Launch Config

Now we are building a launcher to start the Designer from the Dev environment.

1. Click on the Menu "Run" > "Run Configurations ..."
2. Right click on "Eclipse Application" and opt for "New Configuration"
3. Give the Configuration a name e.g. "DevDesigner"
4. In the Main tab, select the product to run to be "ch.ivyteam.ivy.designer.branding.product" 
5. In the Arguments tab, in the Working Directory section, choose "Other" and pass a file path to an official AxonIvyDesigner, matching your development train.
6. In the "VM arguments" section: add the following:  
   
   ```
   -Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true
   -Xmx2g
   --add-modules=ALL-SYSTEM
   --add-exports=java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED
   --add-opens=java.base/java.io=ALL-UNNAMED
   --add-opens=java.base/java.lang=ALL-UNNAMED
   --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
   --add-opens=java.base/java.time=ALL-UNNAMED
   --add-opens=java.base/java.text=ALL-UNNAMED
   --add-opens=java.base/java.util=ALL-UNNAMED
   --add-opens=java.base/java.net=ALL-UNNAMED
   --add-opens=java.desktop/java.awt.font=ALL-UNNAMED
   --add-exports=java.xml/com.sun.org.apache.xpath.internal=ALL-UNNAMED
   --add-exports=java.xml/com.sun.org.apache.xpath.internal.objects=ALL-UNNAMED
   --add-exports=java.xml/com.sun.org.apache.xml.internal.utils=ALL-UNNAMED
   --add-exports=java.base/sun.security.provider.certpath=ALL-UNNAMED
   -Dosgi.requiredJavaVersion=17 
   ```
7. Hit the "Run" button ... and the Designer should start

![launch-create.png](launch-create.png)

![launch-designer-branding.png](launch-designer-branding.png)

![launch-workdir-designer.png](launch-workdir-designer.png)

### Designer

In the Dev-Designer you can access the extension.

1. Create an empty playground Project as your "target" for demo imports.
2. Run the extension via Menu "File" -> "Import". If you run it, you will execute the code in your Eclipse RCP environment.

![run-extension-inDevDesigner.png](run-extension-inDevDesigner.png)

### Debugging

You can also debug through the extension code; though most of the code life in the consumed Ivy Platform.

1. Close any running DevDesigner instances
2. Click on the "Debug" (bug icon) in the Eclipse RCP main toolbar
3. Select the "DevDesigner" launch config
4. Add debug breakpoints into the "excel-importer" extension code
5. Trigger the import in the DevDesigner, and the execution will halt on breakpoints.

### Testing

The most important transformations when importing Excel sheets to Dialogs are tested with classical @IvyTest cases. Here's how to use them:

1. Import all projects of your local 'excel-importer' clone into the Designer workspace (yes, also the extension itself)
   1. you'll probably see two false-positive compile errors; just delete them in the Problem section.
2. Go into the Test project and select a Unit Test of your choice
3. Right click and Run it to verify your code.

These tests are of course also executed in the actions build pipeline, using Maven.

![run-tests-inDesigner.png](run-tests-inDesigner.png)

### Database

The supported databases of this connector can be started with Docker Compose.
Once you have Docker installed, you can simply run a test-db like this.

```bash
# run all DBs
docker compose up

# run Postgres
docker compose up postgres

# ... or any other
docker compose up mariadb
docker compose up mysql
docker compose up mssql
```
