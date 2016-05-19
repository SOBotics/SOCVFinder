$(document).ready(function () {
    $(".switchToLight").click(function () {
        $("body").addClass("bodyLight").removeClass("bodyDark");
        localStorage["bodyClass"] = "bodyLight";
        initChart();
    });

    $(".switchToDark").click(function () {
        $("body").addClass("bodyDark").removeClass("bodyLight");
        localStorage["bodyClass"] = "bodyDark";
        initChart();
    });
    $("body").addClass(localStorage["bodyClass"] || "bodyDark");
});