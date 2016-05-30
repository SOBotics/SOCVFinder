#SOCVFinder

##Background

This bot was develop to address the following issues:

1. The problem of duplicated questions, that not only confuse search engines but also encourage rep hunts with copy and paste answers. SO specifically introduce the [the mighty Mjölnir](http://meta.stackexchange.com/questions/230865/increase-close-vote-weight-for-gold-tag-badge-holders/231212#231212) to address this problem, but to often it is not used for various reasons.

2. The inefficiency of current close review que. Their are to many questions in que and to few reviewers, the end result is that the few reviewers often see all their work [age away](http://meta.stackoverflow.com/questions/252584/enough-fuzzying-lets-let-everything-into-the-close-queue-and-age-out-questions) specially if they filter in on low-traffic tags

## Objectives

 1. To provide a means and aids to hammers in finding duplicates faster.

 2. To help users easily identify (and review) questions of certain (customizable) characteristics. A.K.A., "cherry picking", "filtering", etc.

## 1. Dupe hammer aids

### 1.1 Duplicate Notifications

Allow hammers to opt in for real-time notifications of possible duplicates. The hammer should only be pinged if present in room.


### 1.2 Duplication search

Allow of hammer to search for duplicated questions as they wish indicating max number of question, date span, score count etc. The bot should not return questions in previously reviewed.
  

## 2. Enjoyable and targeted reviewing

Leverage the community's work do not let it go to waste if it is good and have some fun doing it. Make your review count!

## 2.1 What can make reviewing enjoyable?

 - Knowing that your vote counts
 - Talking to a bot,
 - Cherry picking questions,
 - Getting statistics on reviewing (users/tags/rooms).

## 2.2 How can you make your vote count and not let the work of others age away?
 
 - Review questions that already have high close vote count.
 - Review questions that will not go to roomba. 


## 2.3 Cherry pick your questions

Query the bot for the desired number of questions in tag of choice, with the possibility to specify desired cv count, question score, if not roomba, if has answers, if has accepted answer. 


##2.4 Enjoyable statistics

Query the bot for some statistics so you can enjoy your effort and see status of tag

## 3. Limitations

- The bot will only use open SO api and will not do any "screen scrapping".
- The API can not be filtered on close votes, hence all questions need to be scan, to reduce api calls a index system is implemented
- If question is in review que is not avialable in Api, we can not direct user to review interface nor can reversed "Salvage" questions search be made.

## 4. Commands

For full command specification based on privilege level see [Quick guide](https://github.com/jdd-software/SOCVFinder/blob/master/quickGuide.md)

## 5. Accounts

This bot is using the [Queen](http://stackoverflow.com/users/6294609/queen) account and is also a registred stack app. The test is currently made in [SOCVFinder](http://chat.stackoverflow.com/rooms/111347/socvfinder) chat room

