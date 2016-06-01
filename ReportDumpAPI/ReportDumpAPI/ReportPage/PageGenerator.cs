using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;
using Jil;

namespace ReportDumpAPI.ReportPage
{
    public static class PageGenerator
    {
        private static readonly string template;



        static PageGenerator()
        {
            template = File.ReadAllText(Path.Combine(Config.ContentDir, "report-template.html"));
            template = PatchReportMaxLife(template);
            template = PatchVersion(template);
        }



        public static string GenerateReportPage(string jsonStr)
        {
            var json = JSON.Deserialize<Dictionary<string, object>>(jsonStr);
            var isDupeReport = false;
            var html = template;

            html = PatchReportType(html, json, out isDupeReport);
            html = PatchReportRoomID(html, json);
            html = PatchReportID(html, json);
            html = PatchReportSearchTag(html, json);
            html = PatchReportContent(html, json, isDupeReport);

            return html;
        }



        private static string PatchReportMaxLife(string html)
        {
            return html.Replace("$REPORT_MAX_LIFE_DAYS$", Config.ReportLifeMaxDays.ToString());
        }

        private static string PatchVersion(string html)
        {
            var ver = ThisAssembly.Git.Sha.ToUpperInvariant();

            if (ver.Length > 5)
            {
                ver = ver.Substring(0, 5);
            }

            var link = $"https://github.com/jdd-software/SOCVFinder/commit/{ThisAssembly.Git.Sha}";
            var a = $"<a href=\"{link}\">{ver}</a>";

            return html.Replace("$SERVER_VERSION$", a);
        }

        private static string PatchReportType(string html, Dictionary<string, object> json, out bool dupes)
        {
            var isDupesStr = json["is_filtered_duplicates"].ToString();
            var isDupes = bool.Parse(isDupesStr);
            dupes = isDupes;
            return html.Replace("$REPORT_TYPE$", isDupes ? "Duplicate" : "Cherry-pick");
        }

        private static string PatchReportID(string html, Dictionary<string, object> json)
        {
            return html.Replace("$REPORT_ID$", json["batch_nr"].ToString());
        }

        private static string PatchReportRoomID(string html, Dictionary<string, object> json)
        {
            return html.Replace("$REPORT_ROOM_ID$", json["room_id"].ToString());
        }

        private static string PatchReportSearchTag(string html, Dictionary<string, object> json)
        {
            var tagsStr = json["search_tag"].ToString();
            tagsStr = tagsStr.Remove(0, 1).Substring(0, tagsStr.Length - 2);

            var tagSplit = tagsStr.Split(';');

            for (var i = 0; i < tagSplit.Length; i++)
            {
                tagSplit[i] = $"<span class=\"reportTitleTag\">{tagSplit[i]}</span>";
            }

            var tags = "";

            if (tagSplit.Length == 1)
            {
                tags = tagSplit[0];
            }
            else if (tagSplit.Length == 2)
            {
                tags = tagSplit[0] + " & " + tagSplit[1];
            }
            else
            {
                for (var i = 0; i < tagSplit.Length - 2; i++)
                {
                    tags += tagSplit[i] + ", ";
                }

                tags += tagSplit[tagSplit.Length - 2] + " & " + tagSplit[tagSplit.Length - 1];
            }

            return html.Replace("$REPORT_TAG$", tags);
        }

        private static string PatchReportContent(string html, Dictionary<string, object> json, bool dupes)
        {
            var qsJson = json["questions"].ToString();
            var qs = JSON.Deserialize<Dictionary<string, object>[]>(qsJson);
            var list = new StringBuilder("<div>");

            // Start of open all/sort by functions.
            list.AppendLine("<div class=\"reportHeaderFunctions\">");

            // Open all button.
            list.AppendLine("<span id=\"openAllReports\">Open all</span>");

            // Sort by select.
            list.AppendLine("<span>");
            list.AppendLine("Sort by:");

            list.AppendLine("<select id=\"sortBy\">");
            list.AppendLine("<option>Age</option>");
            list.AppendLine("<option>Answers</option>");
            list.AppendLine("<option selected>Close votes</option>");
            list.AppendLine("<option>Score</option>");
            list.AppendLine("<option>Views</option>");
            list.AppendLine("</select>");

            // End of sort by select.
            list.AppendLine("</span>");

            // End of Report functions.
            list.AppendLine("</div>");

            foreach (var q in qs)
            {
                var qHtml = GetQuestionHtml(q, dupes);
                list.AppendLine(qHtml);
            }

            list.AppendLine("</div>");

            return html.Replace("$REPORT_CONTENT$", list.ToString());
        }

        private static string GetQuestionHtml(Dictionary<string, object> json, bool dupes)
        {
            var html = new StringBuilder();
            var qID = json["question_id"];
            var link = $"//stackoverflow.com/q/{qID}";
            var title = json["title"].ToString();
            title = title.Remove(0, 1).Substring(0, title.Length - 2);
            var score = json["score"].ToString();
            score = score.StartsWith("-") || score == "0" ? score : $"+{score}";
            var age = json["creation_date"].ToString();
            var views = json["view_count"].ToString();
            var answerCount = json["answer_count"].ToString();
            var accecptedAnsID = json["accepted_answer_id"].ToString();
            var cvCount = json["close_vote_count"].ToString();

            // Start of report.
            html.AppendLine("<div class=\"report\">");

            // Start of report header.
            html.AppendLine("<div class=\"reportSubContainer\">");

            // Report title.
            html.AppendLine("<h3 class=\"reportTitle\">");
            html.AppendLine($"<a target=\"_blank\" href=\"{link}\">");
            html.AppendLine(title);
            html.AppendLine("</a>");
            html.AppendLine("</h3>");

            // Report score.
            html.AppendLine("<div class=\"questionScore\">");
            html.AppendLine(score);
            html.AppendLine("</div>");

            // End of report header.
            html.AppendLine("</div>");

            // Start of report stats.
            html.AppendLine("<div class=\"reportSubContainer\">");

            // Question age.
            html.AppendLine("<div class=\"postAge\">");
            html.AppendLine("<span class=\"valueName\">Posted:</span> ");
            html.AppendLine($"<span class=\"postTime\" data-unixtime=\"{age}\"><span>");
            html.AppendLine("</div>");

            // View count.
            html.AppendLine("<div class=\"viewCount\">");
            html.AppendLine($"<span class=\"valueName\">Views:</span> ");
            html.AppendLine(views);
            html.AppendLine("</div>");

            // Answer count.
            if (accecptedAnsID == "0")
            {
                html.AppendLine("<div class=\"answerCount\">");
            }
            else
            {
                html.AppendLine("<div class=\"answerCount\" style=\"color:#00AB0B\" title=\"This question has an accepted answer\">");
            }
            html.AppendLine("<span class=\"valueName\">Answers:</span> ");
            html.AppendLine(answerCount);
            html.AppendLine("</div>");

            // Close vote count.
            html.AppendLine("<div class=\"closeVotes\">");
            html.AppendLine("<span class=\"valueName\">Close votes:</span> ");
            html.AppendLine(cvCount);
            html.AppendLine("</div>");

            // End of report stats.
            html.AppendLine("</div>");

            if (dupes)
            {
                var comments = JSON.Deserialize<Dictionary<string, object>[]>(json["comments"].ToString());

                if (comments.Length > 0 &&
                    comments[0].ContainsKey("duplicated_target_id") &&
                    comments[0].ContainsKey("duplicated_target_title") &&
                    comments[0].ContainsKey("duplicated_target_score"))
                {
                    var targetID = comments[0]["duplicated_target_id"];
                    var dupeLink = $"//stackoverflow.com/q/{targetID}";
                    var dupeTitle = comments[0]["duplicated_target_title"].ToString();
                    dupeTitle = dupeTitle.Remove(0, 1).Substring(0, dupeTitle.Length - 2);
                    var dupeScore = comments[0]["duplicated_target_score"].ToString();
                    dupeScore = dupeScore.StartsWith("-") || dupeScore == "0" ? dupeScore : $"+{dupeScore}";

                    // Start of dupe target details.
                    html.AppendLine("<div class=\"reportSubContainer\">");

                    // Dupe target link.
                    html.AppendLine("<div class=\"dupeTargetLink\">");
                    html.AppendLine("<span class=\"valueName\">Duplicate of:</span> ");
                    html.AppendLine($"<a target=\"_blank\" href=\"{dupeLink}\">");
                    html.AppendLine(dupeTitle);
                    html.AppendLine("</a>");
                    html.AppendLine("</div>");

                    // Dupe target score.
                    html.AppendLine("<div class=\"questionScore\">");
                    html.AppendLine(dupeScore);
                    html.AppendLine("</div>");

                    // End of dupe details.
                    html.AppendLine("</div>");

                    if (comments.Length == 2)
                    {
                        var opComment = comments[1]["body"].ToString();

                        // OP response.
                        html.AppendLine("<div class=\"opResponseContainer\">");
                        html.AppendLine("<span class=\"valueName\">Response from OP:</span> ");
                        html.AppendLine($"<span class=\"opResponseComment\">{opComment}</span>");
                        html.AppendLine("</div>");
                    }
                }
            }

            // End of report.
            html.AppendLine("</div>");

            return html.ToString();
        }
    }
}
