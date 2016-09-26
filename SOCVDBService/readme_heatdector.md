#Heat Dector

Heat detector is a bot, that query [comments api](https://api.stackexchange.com/docs/comments) for all comments on [SO](https://stackoverflow.com/) to catch offensive, rude, snarky comments and comments that leads to abusive content (offensive, spam posts or previous offensive comments)

The comments are output in chat for manual review.

**Example**


To avoid storing comments (deleted by mods) in chat transcript, the comment is automatically removed from chat before edit timeout.

The bot also have both a test function and a report function, that enables possibility to test different comments and train the classifiers.

## Implementation

The comments api is queried every 1,5 min-3 min depending on traffic, the objective of query intervall is to get all last comments within 1 page result (max 100 comments), to reduce api calls to a minimum.

All comments are processed both by regex and multiple machine learning algoritms (NLP), the single system contributes to a final score. If score is above set threshold the comment is outputed to chat.

### Pre-proccesing
Before regex is applied the comment is pre-processed removing username, html tags and repetive characters. For nlp  classification also code blocks, links and non normal ASCII chars are removed.

### Regex
The regex system is currently divided in two text files, high scoring regex and low scoring regex. 

### NLP (Natural language processing)
Currently 3 different NLP systems are used, the different classifiers contributes to final score with different weights.

#### Naive Bayes

This is the primary classifer (currently the best performing classifier) used, contributing most significantly to the final score. 

The classifer runs with:
    minWordFrequency: 1 (comments are to short to use word frequency)
    useWordFrequency: false
    lowercaseTokens: true
    normalizeDocLenght: false
    stemmer: IteratedLovinsStemmer
    Stopword: Standard english stopwords
    Tokenizer: NGramTokenizer (min 1, max 3)
 
 Testing on traning set it has correctly classified instances at 99.15%

#### OpenNLP

Apache Open NLP standard DocumentCategorizerME,  with default settings (cutoff 0, iteration 4000)

#### J48 - Decision three

The feed is filtered to a StringToWordVector and classified with the J48 algoritim, settings are similar as for navive bayes.

## Feed

The classifer has a feed divided in 2 categories, the good feed that is a download of old (>90d) comments from comment api and a bad feed that is a composition of:

1. [Feed provided by SE](http://meta.stackoverflow.com/a/327148/5292302), these comments have been reviwed manully and almost 50% removed as they did not seem to be within scope

2. [Twitter feed](http://meta.stackoverflow.com/a/326617/5292302) provided by [Laurel](http://meta.stackoverflow.com/users/6083675/laurel)

Currently the feed contains 2000 good comments and 2000 bad comments.

## Status

This stackapp is currently in testing phase, to understand correct regex and specially to improve feed, including new bad SO comments replacing the twitter feed comments. The objective is to run on 3000 good/bad only SE comments. 


 



