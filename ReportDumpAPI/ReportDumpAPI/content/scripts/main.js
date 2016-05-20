$(document).ready(function () {
    $(".switchToLight").click(function () {
        $("body").addClass("bodyLight").removeClass("bodyDark");
        localStorage["bodyClass"] = "bodyLight";
    });

    $(".switchToDark").click(function () {
        $("body").addClass("bodyDark").removeClass("bodyLight");
        localStorage["bodyClass"] = "bodyDark";
    });

    if (localStorage["bodyClass"] === "bodyLight") {
        $("body").addClass("bodyLight");
    }
    else {
        $("body").addClass("bodyDark");
    }

    $(".postTime").each(function() {
        var postTime = new Date(Number.parseInt($(this).text()) * 1000);
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
            var weeks = Math.round(milliDelta / 1000 / 60 / 60 / 60 / 7);
            $(this).text(weeks + " week" + (weeks == 1 ? "" : "s"));
        }
        else {
            $(this).text("on " + postTime.toDateString());
            return;
        }

        $(this).text($(this).text() + " ago");
    });
});