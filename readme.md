#SOCVFinder

##Background

This bot was developed to address the following issues:

1. The problem of duplicated questions, that not only confuse search engines but also encourage rep hunts by copying and pasting answers. SE specifically introduced the [the mighty Mjölnir](http://meta.stackexchange.com/questions/230865/increase-close-vote-weight-for-gold-tag-badge-holders/231212#231212) to address this problem, but too often it is not used for various reasons.

2. The inefficiency of current close review queue. There are too many questions and too few reviewers; the end result is that the few reviewers often see all their work [age away](http://meta.stackoverflow.com/questions/252584/enough-fuzzying-lets-let-everything-into-the-close-queue-and-age-out-questions), especially if they filter on low-traffic tags.

## Objectives

 1. To provide a means and aids to users having a gold-badge in a tag in finding duplicates faster.

 2. To help users easily identify (and review) questions of certain (customizable) characteristics. A.K.A., "cherry-picking", "filtering", etc. so that they feel they are making a difference helping the community.

## 1. Dupe hammer aids

### 1.1 Live duplicate notifications

Allow users with a gold badge to opt-in for real-time notifications of possible duplicates. Those notifications are messages sent to certain configurable chat rooms.

Example:
>[tag:possible-duplicate] [tag:python] [tag:mysql] [tag:django] [tag:django-1.7] [Link to a question](http://stackoverflow.com/questions/1) @User1 @User2

The users will only be pinged if they're present in the room. It is possible to give feedback on a duplicate notification (true positive; false positive).

### 1.2 Duplication search

Allow users to search for questions with duplicate flags or close-votes as they wish, possibly indicating filter options, like a max number of questions to review, a date span, a score count etc. This is similar to the tag filter in the `/review` interface but provides more filtering options and abstracts itself from the current review interface.

Screen-shot of an example of iOS duplicate search:

[![enter image description here][1]][1]

The bot won't return previously reviewed questions.

## 2. Enjoyable and targeted reviewing

**Make your reviews count!**

Leverage the community's work, don't let it go to waste and have some fun doing it.

## 2.1 What can make reviewing enjoyable?

 - Knowing that your vote counts;
 - Cherry-picking questions;
 - Getting statistics on reviewing (users/tags/rooms).

## 2.2 How can you make your vote count?
 
 - Review questions that already have high or low close vote count.
 - Review questions that will not go to the roomba.
 - Review questions together with others

## 2.3 Cherry-pick your questions

Query the bot for the desired number of questions in tags of choice, with the possibility to specify a desired CV count, question score, if not roomba, if has answers, if has accepted answer etc. The bot won't return questions already reviewed.

Screen-shot of an example of a cherry-pick request with no filters in the Java tag:

[![enter image description here][2]][2]

##2.4 Enjoyable statistics

Query the bot for some statistics so you can enjoy your efforts and see the status of your favorite tags.

Examples:

    This is your effort that I have registred all time
       nr [tag]             Reviews  CV virt.  CV count    Closed
    -----------------------------------------------------------------
       1. java                  370       221       216        87
       2. blinking               45        22        22         4
       3. javascript             25         8         8         3
       4. c                      11         2         2         2
       5. c#                      1         1         1         1
       6. maven                   1         1         1         0
       7. c++                     1         1         1         1
       8. python                  1         1         1         1
       9. swift                   2         0         0         0
    -----------------------------------------------------------------
          TOTAL                 457       257       252        99

<!-- -->

    Tag statistics all time
       nr [tag]             Reviews  CV virt.  CV count    Closed
    -----------------------------------------------------------------
       1. python                839       814       672       286
       2. java                  779       721       537       272
       3. c#                    272       185       185        69
       4. php                   111        98        98        89
       5. ios                   160        93        93        33
       6. c                     122        84        83        42

## 3. Limitations

- The bot only uses the Stack Exchange API and does not do any "screen scrapping".
- The API can not filter on close votes, hence all questions need to be scanned; to reduce API calls, an index system is implemented.
- The information of a question currently being the close-vote queue is not available in the API. As such, we cannot redirect user to the `/review` interface.

## 4. Commands

For full command specification based on privilege level see [Quick guide](https://github.com/jdd-software/SOCVFinder/blob/master/quickGuide.md).

## 5. Accounts

This bot is using the [Queen](http://stackoverflow.com/users/6294609/queen) account and is also a registered stack app. The test is currently made in [SOCVFinder](http://chat.stackoverflow.com/rooms/111347/socvfinder) chat room.


  [1]: http://i.stack.imgur.com/MrN50.png
  [2]: http://i.stack.imgur.com/X4gI8.png
