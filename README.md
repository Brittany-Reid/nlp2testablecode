# NLP2TestableCode
NLP2TestableCode is a plug-in for the Eclipse IDE that uses natural language tasks to search for relevent Java Stack Overflow snippets, corrects compiler errors, integrates code snippets by making changes based on existing source code and helps developers test code snippets.

## Plugin Installation Instructions:

> [!NOTE]
> 13/06/2025: NLP2TestableCode was developed on an older version of Eclipse. The 09/18/2019 release [here](https://www.eclipse.org/downloads/packages/release/2019-09/r/eclipse-ide-rcp-and-rap-developers-includes-incubating-components) is confirmed to work.

To install the plugin for development:
 1. Download and install the Java Development Kit (JDK)
 2. Download and install Eclipse IDE for RCP and RAP Developers from the Eclipse Project website.
 3. File -> Import -> Git -> Projects from Git -> Clone URI.
 4. Copy and paste the .git URI from the NLP2TestableCode GitHub.
 5. Press Next until you get to the project import wizard. Choose "Import exisiting Eclipse projects" and press Next and Finish.
 6. Download CoreNLP (https://stanfordnlp.github.io/CoreNLP/) and extract into /lib
 7. Download the SO dataset (http://doi.org/10.5281/zenodo.3752789) and extract into /data
 8. You can now run the plugin by right-clicking launches/NLP2TestableCode.launch and selecting Run As... > NLP2TestableCode. If you recieve an error about the JRE, open 'Run Configurations...' in the Run menu and Eclipse should automatically update the JRE to your default.

To install the plugin on your regular Eclipse environment (e.g. for personal use), you will need to package the plugin so it can be installed via the Eclipse Install New Software tool. Since this repository is purely for the development of the tool, there is currently no support in this repository for packaging the plugin for installation.

## Important Plugin Configuration Settings:

### Content Assist:

To get the most out of the plugin, it is strongly recommended that you go to Window -> Preferences -> Java -> Editor -> Content Assist and:
- Turn off 'Insert single proposals automatically' so single type suggestions do not automatically trigger.
- Turn on 'Disable insertion triggers besides 'Enter''.
- Add a content assist binder to trigger the NLP2Code task content assist window by adding a '?' symbol to the set of symbols that trigger content assist.

### Required Libraries:

You will need to download Stanford CoreNLP (https://stanfordnlp.github.io/CoreNLP/) and extract the folder (stanford-corenlp-full-2018-10-05) into /lib.

### Stack Overflow Data:

NLP2TestableCode uses an offline database of SO posts, you will need to download the pre-filtered xml files from http://doi.org/10.5281/zenodo.3752789 and extract them into /data.

### Testing
To prevent the execution of arbirary code from within  the SO database, NLP2TestableCode has testing disabled by default. We do not recommend turning testing on without running the plug-in within a Virtual Machine. If you would like to turn testing on, the setting can be changed in Activator.java.

## How to use the plugin:

When you first open the editor, you will need to invoke the plug-in before setup can begin. You can do this by pressing ctrl+space. Loading will be visible in the eclipse progress window.

### Search for Code Snippets usig Natural Language Tasks
Queries can be invoked in three ways:
 1. Write your query on a line (e.g. sort an array) and press the Stack Overflow button on the toolbar (or pressing ctrl+6 as a hotkey).
 2. Construct a natural language query of characters and spaces with a question mark at the end (e.g. sort an array?).
 3. Press ctrl+6 to open the nlp2code content assist. You can also cycle through content assist suppliers until you get to nlp2code.contentassist by pressing ctrl+space.
 
Once a query is invoked, the plugin will take some time to process, evaluate and correct code snippets related to your query. During this process, the progress window will show progress. As soon as a compiling snippet is found, it will be inserted.

If you find that the first result isn't helpful, you can cycle through all of the retrieved code snippets by pressing ctrl+` (ctrl + tilde/backtick key), or by pressing the stack overflow button with the blue arrow on the toolbar.

tl;dr:
Conduct a query by:
 1. highlighting the text and pressing ctrl+6 (or pressing the stack overflow button).
 2. writing a query comprised of letters and spaces (no other characters accepted). e.g. sort an array?
 3. selecting a task in the content assist that suits what you are looking for.

After a query, cycle through possible solutions with ctrl + `.
After you select a snippet, you will be prompted for feedback if feedback has been enabled in the preferences.txt

### Testing Code Snippets
**Warning: We highly recommend running any testing within a VM. The testing process will run arbitrary code from Stack Overflow on your machine. This can be potentionally dangerous. By default, testing is disabled. You will need to enable it within Activator.java by setting the value `testing` to true.**

When snippets have finished being evaluated, you can then begin the testing process.

#### Argument and Return Types
Press ctrl+Alt+T to see argument and return type suggestions based on the retrieved code snippets. When you select a type suggestion, or write your own, the plug-in will then build a skeleton JUnit test case for you.

#### Customizing the JUnit test case and running tests
When you are finished customizing the JUnit test case, press ctrl+Alt+D to run the test. When the test is finished, the inserted snippet will update with the new best.

## How to contribute:

Pull requests are most welcome!

## References:

For more information see:

NLP2TestableCode: Optimising the Fit of Stack Overflow Code Snippets intoExisting Code - https://arxiv.org/pdf/2004.07663.pdf

NLP2Code: Code Snippet Content Assistvia Natural Language Tasks - http://cs.adelaide.edu.au/~christoph/icsme17c.pdf
