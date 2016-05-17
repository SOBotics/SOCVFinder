# Objectives

 1. To provide a means which aids the user in searching/closing duplicates faster, and to provide a more enjoyable reviewing experience (with the intention of attracting more people to join us and help).

 2. To help users easily identify (and review) questions of certain (customisable) characteristics. A.K.A., "cherry picking", "filtering", etc.

# 1. Dupe hammer aids

## 1.1 Duplicate Notifications

The chat bot will allow users to request it to join any chat room. Upon joining the room, a user with a dupe hammer can request to be notified by executing the following command: 

`@que opt-in <tag> <algo-type>`

 - **`tag`**: The tag in which the duplicate search will be carried out.

 - **`algo-type`**: Duplicate post search algorithm type. Possible values: `pd` = "Possible duplicate" comment search, `sa` = Sam's [UniStack](https://github.com/ArcticEcho/UniStack) algorithm.

###Example:

`@que opt-in [tag:java] pd`

---

If possible duplicates are found, the bot will output it in a chat (pinging the user only if present in room). Example:

<code>[tag:java] <a href="http://stackoverflow.com/questions/37080807" target="_blank">Singleton Design Pattern Implementation</a> possible duplicate of <a href="http://stackoverflow.com/questions/3714971" target="_blank">Difference between singleton class and static class?</a> >> @Petter</code>

Users who have opted-in for said tag can reply to the message with:

 - **`k`**: Confirms that the report is indeed a duplicate.

 - **`f`**: Marks the report as a false positive (it will be removed from the duplicate search).

## 1.2 Duplication search

This function displays possible duplicates.

`@que <max-questions> <tag> dupes`

 - **`max-questions`**: The maximum number of questions to return (optional default 10?).

 - **`tag`**: The target tag.

 

###Example:

`@que 15 [tag:java] dupes`

The bot will also generate a file and respond with file locations:

<code>@Petter <a href="https://github.com/jdd-software/SOCVFinder/blob/master/examplePossibleDuplicateOutput.md">Batch 23: 13 possible duplicates found</a></code> (Follow the link to view an example; currently missing row numbers.)

The users will be able to respond to the above message with:

`done <question-IDs>`

 - **`question-IDs`**: A sequence of question IDs which the user would like to exclude from all future results (any room, any user); note, it's not confirming that the question(s) have been closed.

The bot will query the Stack Exchange API on questions in batch number and update user/room/tag statistics.

# 2. Enjoyable reviewing

## 2.1 What can make reviewing enjoyable?
 - Talking to a bot,
 - Cherry picking questions,
 - Getting statistics on reviewing (users/tags/rooms).

## 2.2 Cherry pick your questions

`@que <max-questions> <tag> <cv-count> <q-score> <answers> <age>`

 - **`max-questions`**: The maximum number of questions to return (default 10?).

 - **`tag`**: The tag on which the search will be based.

 - **`cv-count`**: The question's close vote count; supports basic mathematical comparison operators: `>`, `<`, and `=`. (default `=4CV`?).

 - **`q-score`**: The question's vote count; supports basic mathematical comparison operators: `>`, `<`, and `=`. (Default `<1`?)

 - **`answers`**: Specifies whether the question should have (accepted) answers; possible values are: 
  - `nr`: No roomba.
  - `a` Has answer.
  - `aa` Has accepted answers.
  - `na` No answers; set this as the default value?
  - `naa` No accepted answer.

 -  **`age`**: Returns questions posted with the specified number of days (default 10 days?).

###Examples:

`@que 15 [tag:java] >2CV <0 na 10d`

A maximum of 15 questions in the java tag that have at least 3 or 4 cvs that are scored 0 (or less) and have no answers (also, posted within the last 10 days) will be returned.

`@que [tag:java]`

10 questions in the java tag that have at least 4cv and are scored 0 or less which have been posted within the last 10 days.

The bot will response to user:

<code>@Petter <a href="https://github.com/jdd-software/SOCVFinder/blob/master/exampleCVReviewOutput.md">Java batch 24 generated 14 questions to review</a></code> (Follow the link to view an example, currently missing row numbers.)

Once the user has finished reviewing they can reply to bot with:

`@que done cmd*..`

As for the duplicate search, the API will be called to gather statistics which will allow users to exclude questions from future results.

##2.3 Enjoyable statistics

Web UI displaying statistics on tags, rooms and users with some nice an interactive pie charts.


For more information see <a href="https://github.com/jdd-software/SOCVFinder/blob/master/function_specifications.md">Software specifications</a>.
