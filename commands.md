# Commands

##Moljnir notification

###Registrer to notification

`@que opt-in <tag> <algo-type>`

 - **`tag`**: The tag in which the duplicate search will be carried out.

 - **`algo-type`**: Duplicate post search algorithm type. Possible values: `pd` = "Possible duplicate" comment search, `sa` = Sam's [UniStack](https://github.com/ArcticEcho/UniStack) algorithm.

####Example:

`@que opt-in [java] pd`

###Remove for notification

`@que opt-out [java] pd`

###Report result of notification

Users who have opted-in for said tag can reply to the message with:

 - **`k`**: Confirms that the report is indeed a duplicate.

 - **`f`**: Marks the report as a false positive (it will be removed from the duplicate search).



##Dupe hunt and Cherry pick

##Command

`@que <max-questions> <tag>* "dupes" <cv-count>cv <q-score>s <answers> <age>d`

 - **`max-questions`**: The maximum number of questions to return (default 10).

 - **`tag`**: The tag on which the search will be based, supports multiple tags in and

 - **`cv-count`**: The question's close vote count; supports basic mathematical comparison operators: `>`, `<`, and `=`. (default `=4CV`?).

 - **`q-score`**: The question's vote count; supports basic mathematical comparison operators: `>`, `<`, and `=`. (Default `<1`?)

 - **`answers`**: Specifies whether the question should have (accepted) answers; possible values are: 
  - `nr`: No roomba.
  - `a` Has answer.
  - `aa` Has accepted answers.
  - `na` No answers; set this as the default value?
  - `naa` No accepted answer.

 -  **`age`**: Returns questions posted with the specified number of days > (in days) < (before days).

<b>Example</b>

`@que [java] dupes` - search for dupes in java

`@que [c#]` - get cherry picked gestions in c#

`@que 5 [php] [css] >2cv` - get max 5 questions with tags php and css that has more then 2 close vote

`@que 10 [python] =3cv <=0s nr` - get max 10 questions with tags python that has 3 close vote, questions score <=0 and it will not be roomba
