#SOCVDBService

This is planned to be a service that fills/updates a database with question, since api can not be filtered on close votes / flags.

Currently its a test implementation of the api, to understand what can be achieved.

The main class is CloseVoteFinder, it currently has a static API THROTTLE set to 1 seconds and MAX_PAGES=20

To test bot this code can be used:


<code>Properties properties = new Properties();</code>

<code>properties.load(new FileInputStream("ini/SOCVService.properties"));</code>

<code>CloseVoteFinder.initInstance(properties);</code>

<code>String tag = "php";</code>

<code>BotCommands botCmd = new BotCommands();</code>

<code>//For cherry</code>

<code>CherryPickResult resultCherryPick = botCmd.getCherryPickBatch(0L, 0L, tag, 10, null, null, null, null); </code>

<code>String htmlCherry = resultCherryPick.getHTML();</code>

<code>//For possibile dupes</code>

<code>CherryPickResult resultPossibileDupes = botCmd.getPossibileDuplicatesBatch(0, 0, tag, 10); </code>

<code>String htmlPDupes = resultPossibileDupes.getHTML();</code>

 
There is also a temporary (only for testing) Swing application CVAppSwing.java executed by the SOCVDBService.jar (executable jar)
