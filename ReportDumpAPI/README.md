*Under construction...*

# Usage

Send a POST request to `{host domain/IP}/api/create-report` with the following JSON data:

 - `botName` *string*: the name of your chatbot.<sup>*</sup>
 - `tag` *string*: a common tag which all the posts have.
 - `posts` *object*: an array of `post` objects. <sup>*</sup>

A `post` object is an array of sub-objects called `fields`. A `field` represents a single piece of data (with a name of the data). A `post` can contain mutiple `field`s is any order you wish. There is a limit to the number of `field`s a single `post` can contain though.

`field`s must contain the following:

 - `name` *string*: the name of the field. This text will be displayed as-is to the end-user.<sup>*</sup>
 - `value`: the data associated with the field. All primitive types are supported.<sup>*</sup>
 - `specialType` *string*: the name of the special type this field represents.

The `specialType` field tells the API that the `field` object contains special data which needs additional processing before being displayed in the end report. The supported values are currently: "answers" and "date".

"answers" type `field`s require the `value` field to be of type *string*. The data must start with an `a` (case-insensitive) to indicate that an answer has been accepted followed by the total number of answers. Example: `A5` tells us there are 5 answers, one of which is accepted. `2` indicated there are 2 answers in total, neither are accepted.

"date" type `field`s require the `value` field to be of type *int*. The data must be in unix epoch form.

Example:

```json
{
  "botName": "Botty McBotface",
  "tag": "jon-skeet",
  "posts":
  [
    [
      {
        "name": "How do I foo the bazzed bars?",
        "value": "https://stackoverflow.com/questions/123456789"
      },
      {
        "name": "",
        "value": "-3"
      },
      {
        "name": "Posted On",
        "value": 1289792601,
        "specialType": "date"
      },
      {
        "name": "Answers",
        "value": "A4",
        "specialType": "answers"
      }
    ],
    [
      {
        "name": "Barring a baz made a foo, why?",
        "value": "https://stackoverflow.com/questions/1122334455"
      },
      {
        "name": "",
        "value": "5"
      },
      {
        "name": "Posted On",
        "value": 2683722661,
        "specialType": "date"
      },
      {
        "name": "Answers",
        "value": "2",
        "specialType": "answers"
      }
    ]
  ]
}
```
 
 -----
 
 <sup>*</sup> This field is required.
