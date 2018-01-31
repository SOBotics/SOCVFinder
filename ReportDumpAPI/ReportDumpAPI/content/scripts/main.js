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
        $(".reportField a").each(function() {
            var url = $(this).attr("href");
            window.open(url);
        });
    });

    if (localStorage["bodyClass"] === "bodyLight") {
        $("body").addClass("bodyLight");
    }
    else {
        $("body").addClass("bodyDark");
    }

    $(".timestamp").each(function() {
        var timestamp = new Date(+$(this)[0].dataset.unixtime * 1000);
        var secAge = (Date.now() - timestamp) / 1000;

        $(this).attr("title", timestamp.toISOString().replace("T", " ").substr(0, 19) + "Z");

        if (secAge < 5) {
            $(this).text("a few seconds");
        }
        else if (secAge < 59) {
            var secs = Math.round(secAge);
            $(this).text(secs + " second" + (secs == 1 ? "" : "s"));
        }
        else if (secAge < 60 * 59) {
            var mins = Math.round(secAge / 60);
            $(this).text(mins + " minute" + (mins == 1 ? "" : "s"));
        }
        else if (secAge < 60 * 60 * 23) {
            var hours = Math.round(secAge / 60 / 60);
            $(this).text(hours + " hour" + (hours == 1 ? "" : "s"));
        }
        else if (secAge < 60 * 60 * 24 * 6) {
            var days = Math.round(secAge / 60 / 60 / 24);
            $(this).text(days + " day" + (days == 1 ? "" : "s"));
        }
        else if (secAge < 60 * 60 * 24 * 7 * 4) {
            var weeks = Math.round(secAge / 60 / 60 / 24 / 7);
            $(this).text(weeks + " week" + (weeks == 1 ? "" : "s"));
        }
        else {
            var md = timestamp.toDateString().slice(4, 15);
            var y = "'" + md.split(" ")[2].slice(2, 4)
            var mdy = md.slice(0, 7) + y;
            $(this).text("on " + mdy);
            return;
        }

        $(this).text($(this).text() + " ago");
    });
});