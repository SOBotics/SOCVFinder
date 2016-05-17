#Software specifications

<h2>Major issue</h2>

Impossibile to filter API  on <code>close_vote_count</code> and <code>close_flag_count</code>, this force the need for a service that fills a database with question of interest

No information available if question is in review or is closed flag. This makes it impossibile to include a reverse function, flag checking function, that could remove questions from review que if incorrectly flagged, by editing. Futhermore we can not link to the review interface (hence user need to vote from question page)

<h2>Software structure</h2>

<h3>Database Amazone RDS</h3>
Database is need to store questions with close votes/duplicate request and registrer user activity for statistics

<h4>Major tables</h4>

<b>TODO:</b> Define the correct database structure and relative columns

<b>Users</b>
<table>
<tr>
<td>id_user</td>
<td>user_name</td>
<td>acces_level</td>
</tr>
</table>

<b>Questions</b>
<table>
<tr>
<td>question_id</td>
<td>creation_date</td>
<td>title</td>
<td>tag</td>
<td>close_vote_count</td>
<td>score</td>
<td>view_count</td>
<td>answer_count</td>
<td>is_answer_accepted</td>
<td>is_possibile_duplicate</td>
<td>id_user_ignore</td>
</tr>
</table>

<b>Batch served</b>
<table>
<tr>
<td>user_id</td>
<td>batch_nr</td>
<td>question_id</td>
<td>batch_date_start</td>
<td>cv_count_before</td>
<td>batch_date_end</td>
<td>cv_count_after</td>
<td>is_closed</td>
</tr>
</table>

<h3>Database fill service (since no filtering in api)</h3>

Rough numbers: monitor 40 high traffic tags with 1000 question x day each tag.

The service will execute similar API call: https://api.stackexchange.com/docs/questions#page=4&pagesize=100&fromdate=2016-05-04&todate=2016-05-05&order=desc&sort=activity&tagged=ios&filter=!)5IW.LolAne7)nV0)jqvenrWbHDZ&site=stackoverflow&run=true

Fill the database with selected questions and then monitor with predefined strategy the questions.

To find possibile duplicates use comments example

<code>
"comments": [
        {
          "score": 0,
          "comment_id": 61589692,
          "body": "I found the answer <a href=\"http://stackoverflow.com/a/21850538/1960169\">here</a>. Adding it to the window solved my problem."
        },
        {
          "score": 1,
          "comment_id": 61590323,
          "body": "Possible duplicate of <a href=\"http://stackoverflow.com/questions/21850436/add-a-uiview-above-all-even-the-navigation-bar\">Add a UIView above all, even the navigation bar</a>"
        }
      ],
      </code>

A test application is under development here <a href="https://github.com/jdd-software/SOCVFinder/tree/master/SOCVDBService">SOCVDBService</a>

<h2>BOT Function</h2>

<h3>Notification</h3>

Monitor database for latest possibile duplicate and notify user in relative room

<h3>Question search function</h3>

<ol>
<li>
On request query database on <code>tag</code> to get LIMIT 100 question_id with order as in request command
</li>
<li>
Execute api request with question_id's (https://api.stackexchange.com/docs/questions/questionid1;questionId2) and filter as request
</li>
<li>
Update batch table with user, room, batch number and questions 
</li>
<li>
Generate response file and respond with location to user
</li>
<li>
On batch response command <code>done</code> execute api request with question ids and update batch table
</li>
</ol>

Note: If tag is not supported by database, the api is queried for latest X questions based on activity date.
