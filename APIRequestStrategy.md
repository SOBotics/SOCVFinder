# APIRequestStrategy

<h2>Base for calculation</h2>

<ul>
<li>API Quota 10000, every API call 100 questions (1 request every 10s)</li>
<li>Major tag gets approx 1000 question / day.</li>
<li>Typical close vote and possible duplicates in major tag DUP=20 CV1=60 CV2=40 CV3=20 CV4=10 = 150 questions/days</li>
<li>Tracking cv / pd questions for 20 days (maybe can be reduced to 15)</li>
<li>Tracking 20 major tags</tag>
<li>100 Newest question in tag approx 1h-2h time span (verify high traffic periods)</li>
<li>100 Newest question no tag approx 10 time span (verify high traffic periods)</li>
</ul>

<h2>New questions</h2>

<b>Strategy 1: </b> execute api call for each tag use first page (2h of questions)

<b>Strategy 2: </b> execute api (2-4 pages) call often with no filter on tag. (if tracking many tags probably best)


<h2>Question in database</h2>

<code>20x20x150 = 60000</code>, 600 api calls.

New question tracking will remove some api calls (hence some question <code>150x20=3000</code>, 30 api call)

Every request done to bot for cherry picking removes 1 api call (4 users a tag --> <code>4x20=</code> 80 api calls)

Total amount approx <b>500 api</b> calls to update remaining.

<h2>Initial startegy</h2>

<ul>
<li>New questions, strategy 2 every 10 minutes --> <code>20x(24x6)=</code><b>3000 api</b> calls</li>

<li>Updating old questions cv count every 2 hours --> <code>500x12=</code><b>6000 api</b> calls</li>
</ul>

Total <b>9000</b> API calls

<h2>Conclusion</h2>

<h3>Thread 1: New questions</h3>
Running on 10 min interupt with a throttle of 1 api call every 2s (1 min to update, 9 min sleep).

<h3>Thread 2: DB update</h3>
Querying for 100 questions with oldest update date and executing api every 15s (hence approx 5760 calls)

<h2>Final consideration</h2>
For questions older then 7 days data.stackexchange.com could be used to update database, this would save 3000-4000 api calls or allow to double major tags that are tracked.
