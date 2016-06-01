$(document).ready(function () {
    $(".switchToLight").click(function () {
        $("body").addClass("bodyLight").removeClass("bodyDark");
        localStorage["bodyClass"] = "bodyLight";
    });

    $(".switchToDark").click(function () {
        $("body").addClass("bodyDark").removeClass("bodyLight");
        localStorage["bodyClass"] = "bodyDark";
    });

    $("#openAllReports").click(function() {
        $(".reportTitle a").each(function() {
            window.open($(this).attr("href"));
        });
    });

    $("#sortBy").change(function() {
        switch (this.value) {
            case "Age":
                SortByAge();
                break;
            case "Answers":
                SortByAnswers();
                break;
            case "Close votes":
                SortByCloseVotes();
                break;
            case "Score":
                SortByScore();
                break;
            case "Views":
                SortByViews();
                break;
        }
    });

    if (localStorage["bodyClass"] === "bodyLight") {
        $("body").addClass("bodyLight");
    }
    else {
        $("body").addClass("bodyDark");
    }

    $(".postTime").each(function() {
        var postTime = new Date(+$(this)[0].dataset.unixtime * 1000);
        var milliDelta = Date.now() - postTime;

        if (milliDelta < 5000) {
            $(this).text("a few seconds");
        }
        else if (milliDelta < 1000 * 59) {
            var secs = Math.round(milliDelta / 1000);
            $(this).text(secs + " second" + (secs == 1 ? "" : "s"));
        }
        else if (milliDelta < 1000 * 60 * 59) {
            var mins = Math.round(milliDelta / 1000 / 60);
            $(this).text(mins + " minute" + (mins == 1 ? "" : "s"));
        }
        else if (milliDelta < 1000 * 60 * 60 * 23) {
            var hours = Math.round(milliDelta / 1000 / 60 / 60);
            $(this).text(hours + " hour" + (hours == 1 ? "" : "s"));
        }
        else if (milliDelta < 1000 * 60 * 60 * 24 * 6) {
            var days = Math.round(milliDelta / 1000 / 60 / 60 / 24);
            $(this).text(days + " day" + (days == 1 ? "" : "s"));
        }
        else if (milliDelta < 1000 * 60 * 60 * 24 * 7 * 30) {
            var weeks = Math.round(milliDelta / 1000 / 60 / 60 / 24 / 7);
            $(this).text(weeks + " week" + (weeks == 1 ? "" : "s"));
        }
        else {
            $(this).text("on " + postTime.toDateString());
            return;
        }

        $(this).text($(this).text() + " ago");
    });
});

function SortByAge() {
    $(".report").sort(function(a, b) {
        var aTime = $(".postTime", a)[0].dataset.unixtime;
        var bTime = $(".postTime", b)[0].dataset.unixtime;
        return bTime - aTime;
    })
    .appendTo($("#main div")[0]);
}

function SortByAnswers() {
    $(".report").sort(function(a, b) {
        var aAns = $(".answerCount", a).text().slice(9).trim();
        var bAns = $(".answerCount", b).text().slice(9).trim();
        return aAns - bAns;
    })
    .appendTo($("#main div")[0]);
}

function SortByCloseVotes() {
    $(".report").sort(function(a, b) {
        var aCVs = $(".closeVotes", a).text().slice(13).trim();
        var bCVs = $(".closeVotes", b).text().slice(13).trim();
        return bCVs - aCVs;
    })
    .appendTo($("#main div")[0]);
}

function SortByViews() {
    $(".report").sort(function(a, b) {
        var aViews = $(".viewCount", a).text().slice(7).trim();
        var bViews = $(".viewCount", b).text().slice(7).trim();
        return aViews - bViews;
    })
    .appendTo($("#main div")[0]);
}

function SortByScore() {
    $(".report").sort(function(a, b) {
        var aScore = $(".questionScore", a).first().text();
        var bScore = $(".questionScore", b).first().text();
        return aScore - bScore;
    })
    .appendTo($("#main div")[0]);
}
