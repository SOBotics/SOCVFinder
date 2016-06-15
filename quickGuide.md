# QUEEN'S QUICK GUIDE

> Note in specific rooms allowed commands can be reduced according to RO's specifications, use `@Queen commands`, for commands available in room.

##Reviewer

###Cherry pick

The process of reviewing is **asking for batch** in desired tag, **review** and send **done** command to notify Queen that the questions have been reviewed to avoid that questions are included in next (10) batches.

####Work flow cherry pick on close votes

- `@Queen [java]` - get 20 cherry picked questions in java

- **Review the questions**

- `@Queen done` - report to Queen that you have reviewed the question to avoid them to show up in future batches, note you can see them again with the -all parameter see full commands

####Work flow cherry pick on possible duplicates

- `@Queen [c#] dupes` - get 20 possible duplicates in C#

- **Review the questions**

- `@Queen done` - report to Queen that you have reviewed the question to avoid them to show up in future batches.

####Additional filters

These are the filters reviewer can apply to the cherry pick


    @Queen <max-questions> <tag>* "dupes" <cv-count>cv <q-score>s <answers> <age>d -all

 - **`max-questions`**: The maximum number of questions to return (default 20).

 - **`tag`**: The tag on which the search will be based, supports multiple tags in and

 - **`cv-count`**: The question's close vote count; supports basic mathematical comparison operators: `>`, `<`, and `=`.

 - **`q-score`**: The question's vote count; supports basic mathematical comparison operators: `>`, `<`, and `=`.

 - **`answers`**: Specifies whether the question should have (accepted) answers; possible values are: 
  - `nr`: No roomba.
  - `a` Has answer.
  - `aa` Has accepted answers.
  - `na` No answers.
  - `naa` No accepted answer.
  
 -  **`age`**: Returns questions posted with the specified number of days > (in days) < (before days).
 
 - **`-all`** : Included also question previous reviewed and confirmed with done

**Examples**

`@Queen 5 [php] [css] >2cv <=0s na <=2d -all` - get max 5 questions with tags php and css that has more then 2 close vote, the question score is 0 or less, there is no answer and it was posted 2 days ago, include also questions previously reviewed

`@Queen 10 [java] dupes nr >2d` - return max 10 questions that are possible duplicate, will not go to roomba and where posted in last two days.

**Note** 

It is important to have space between filters, but not inside and include letters so that queen can understand on what attribute the filter is to be applied, see >2**cv**, <=0**s** <2**d**

####Ignore / Delete the batch
If you do not have time to review or you where only testing you can delete the batch by replying to batch with ignore

`@Queen ignore` - The batch will be delete, questions released for other users and questions will show up in future batch.


###Statistics

The statistics are gather when the reviewer sends **done** command an api call is made to check close vote count and number of closed question. Naturally the Queen can not be sure that reviewer closed the question so if multiple people review same batch the **cv virtual count** reflects all closed votes counted (for hammer this can be 5 for 1 question) and **cv count**, is the maximum 1 vote per question count.

#### My statistics

    @Queen stats me <today|week>
 
  - `today`: Show only today
  - `week` Show this week
  - `` Show all time

#### Tag statistics

    @Queen stats tags <today|week>
 
  - `today`: Show only today
  - `week` Show this week
  - `` Show all time

#### Room statistics

    @Queen stats rooms <today|week>
 
  - `today`: Show only today
  - `week` Show this week
  - `` Show all time

#### Remove message

`@Queen remove` - Remove last message or message replied to.


##Hammer/Moljnir

As user opt in for notification in a tag a background thread starts that scans SO on latest question for possible duplications and output these in room where the user has opted in. User will only be pinged if **present in room**, hence leaving the room the notification will continue to stream to room but no ping will be sent.

###Register to notification

    @Queen opt-in <tag> <algo-type>

 - **`tag`**: The tag in which the duplicate search will be carried out.

 - **`algo-type`**: Duplicate post search algorithm type. Possible values: `pd` = "Possible duplicate" comment search, `sa` = Sam's [UniStack](https://github.com/ArcticEcho/UniStack) algorithm.

*Note: currently only pd is supported*

    @Queen opt-in [java] pd

###Respond to notifications

If no other user in room have opt-in and the room is displaying only opt-in notifications the duplicate notifications stream will stop.

    @Queen opt-out [java] pd

###Report result of notification

User with hammer privilege can reply to notification with (either by reply to message or if last message reply to Queen)

 - **`k`**: Confirms that the report is indeed a duplicate and if possible (within 115s) edit message.

 - **`f <wl>`**: Indicate as false positive (if wl is added question is white listed, hence it will not be showed in batches anymore) and if possible edit message.
 

###List available tags for opt-in
In certain rooms only specific tags can be allowed, this command lists available tags for opt-in.

    @Queen list tags


##Whitelist

The whitelist gives the possibility to exclude one or multiple questions from future batches (all users).

- **`wl <question_id>*`**: Exclude question(s) from all future batches

**Example**

- `wl 36306170 35127956`


##Tag Owner

###Manage users and set access level

Users above >3K are automatically added to the Reviewer privilege level if not present. Room owner can add or change the level for a single user by executing the add user command

    @Queen add user <id_user> <Display name> <access_level>

 - **`id_user`**: The users id

 - **`Display Name`**: The users display name

 - **`access_level`**: The access level of different commands
  - `0`: Guest.
  - `1` Reviewer.
  - `2` Hammer
  - `3` Tag owner.
  - `4` Bot owner. (you need to be bot owner to set this level)

There is also an actual room owner level but this level can not be set, it depends on chat room settings.

**Example**

- `add users 5292302 Petter Friberg 2`  - Add user 5292302 with hammer privilege 

###Index tags

The Queen to cherry pick uses api calls (around 10/20) to scan questions, in high traffic tags this means that only last 2 days of questions are covered. Indexing a tag force the the queen to scan all question last 20 days (hence after this most close vote have age away) and saves to database all question that have at least 3 close votes. After scanning the questions she will also output statistics (not including delete questions) on closed questions and distribution of close vote count.

Note: Normally there is no need to index a tag more then 1/2 times a day

    @Queen index <tag>

 - **`tag`**: The tag to scan, it need to be included in our monitored tags

**Example**

- `@Queen index [java]`  - Index the java tag


##Actual Room Owner and moderator
The queen can be configured to allow only a sub set of commands in specific room, futhermore RO's can configure to allow duplicate notifications in only a subset of tags (only in these tag you may opt-in)

###Manage tags in room

    @Queen add <tag>

 - **`tag`**: The tag will be added to monitored tags and room users may opt-in for notifications.


    @Queen remove <tag>

 - **`tag`**: The tag will be removed from monitored tags and room users may not opt-in for notifications.
 
 



