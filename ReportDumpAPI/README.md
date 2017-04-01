# Usage

Send a POST request to `{host domain/IP}/api/create-report` with the following JSON data:

 - `botName` *string*: the name of your chatbot.<sup>*</sup>
 - `tag` *string*: a common tag which all the posts have.
 - `posts` *object*: an array of `post` objects. <sup>*</sup>

A `post` object is an array of sub-objects called `fields`. A `field` represents a single piece of data (with a name of the data). A `post` can contain mutiple `field`s (there is a limit though).

`field`s must contain the following:

 - `id` *string*: a unique string which identifies this field from others. This field's value must not change if you want a constant report layout.<sup>*</sup>
 - `name` *string*: the name of the field. This text will be displayed as-is to the end-user.<sup>*</sup>
 - `value`: the data associated with the field. All primitive types are supported.<sup>*</sup>
 - `specialType` *string*: the name of the special type this field represents.

The `specialType` field tells the API that the `field` object contains special data which needs additional processing before being displayed in the end report. The supported values are currently: "answers", "link", and "date".

 - "answers" type `field`s require the `value` field to be of type *string*. The data must start with an `a` (case-insensitive) to indicate that an answer has been accepted followed by the total number of answers. Example: `A5` tells us there are 5 answers, one of which is accepted. `2` indicates that there are 2 answers in total, neither are accepted.

 - "date" type `field`s require the `value` field to be of type *int*. The data must be in unix epoch form.

 - "link" type `field`s require the `value` field to be of type *string*. The data must be a URL.

Example:

```json
{
    "botName": "Botty McBotface",
    "tag": "jon-skeet",
    "posts": [
        [{
            "id": "title",
            "name": "How do I foo the bazzed bars?",
            "value": "https://stackoverflow.com/questions/123456789",
            "specialType": "link"
        }, {
            "id": "score",
            "name": "",
            "value": "-3"
        }, {
            "id": "postage",
            "name": "Posted",
            "value": 1289792601,
            "specialType": "date"
        }, {
            "id": "answercount",
            "name": "Answers",
            "value": "A4",
            "specialType": "answers"
        }],
        [{
            "id": "title",
            "name": "Barring a baz made a foo, why?",
            "value": "https://stackoverflow.com/questions/1122334455",
            "specialType": "link"
        }, {
            "id": "score",
            "name": "",
            "value": "5"
        }, {
            "id": "postage",
            "name": "Posted",
            "value": 2683722661,
            "specialType": "date"
        }, {
            "id": "answercount",
            "name": "Answers",
            "value": "2",
            "specialType": "answers"
        }]
    ]
}
```

If the request is processed successfuly a URL (plain string) of the report will be returned. Example:

```
http://reports.socvr.org/jS0xsN
```
 
 -----
 
 <sup>*</sup> This field is required.
