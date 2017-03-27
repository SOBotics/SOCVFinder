using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using Jil;

namespace ReportDumpAPI.ReportPage
{
    public static class PageGenerator
    {
        private class Field
        {
            public string Id { get; set; }
            public string Name { get; set; }
            public string Html { get; set; }
            public int Length { get; set; }
            public string SpecType { get; set; }
            public double AvgLength { get; set; }
        }

        private const string validIdChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        private const int idLength = 6;
        private const int maxVisCharsPerReportContainer = 50;
        private static RandomNumberGenerator rng = RNGCryptoServiceProvider.Create();
        private static readonly string template;



        static PageGenerator()
        {
            template = File.ReadAllText(Path.Combine(Config.ContentDir, "report-template.html"));
            template = PatchReportMaxLife(template);
            template = PatchVersion(template);
        }



        public static string GenerateReportPage(string jsonStr, out string reportId)
        {
            var json = JSON.Deserialize<Dictionary<string, object>>(jsonStr);
            var id = GenerateId();
            var html = template;

            html = PatchBotName(html, json);
            html = PatchReportID(html, id);
            html = PatchReportTag(html, json);
            html = PatchReportContent(html, json);

            reportId = id;

            return html;
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

        private static string PatchReportMaxLife(string html)
        {
            return html.Replace("$REPORT_MAX_LIFE_DAYS$", Config.ReportLifeMaxDays.ToString());
        }

        private static string GenerateId()
        {
            var id = "";
            for (var i = 0; i < idLength; i++)
            {
                var b = new byte[4];
                rng.GetBytes(b);
                var bInt = Math.Abs(BitConverter.ToInt32(b, 0));
                var charIndex = bInt % validIdChars.Length;
                id += validIdChars[charIndex];
            }

            return id;
        }

        private static string PatchBotName(string html, Dictionary<string, object> json)
        {
            var name = json["botName"].ToString();
            name = name.Substring(1, name.Length - 2);
            return html.Replace("$BOT_NAME$", name);
        }

        private static string PatchReportID(string html, string id)
        {
            return html.Replace("$REPORT_ID$", $"({id})");
        }

        private static string PatchReportTag(string html, Dictionary<string, object> json)
        {
            if (!json.ContainsKey("tag"))
            {
                return html.Replace("$REPORT_TAG$", "");
            }

            var tagsStr = json["tag"].ToString();
            
            tagsStr = tagsStr.Remove(0, 1).Substring(0, tagsStr.Length - 2);

            tagsStr = $": <span class=\"reportTitleTag\">{tagsStr}</span>";

            return html.Replace("$REPORT_TAG$", tagsStr);
        }

        private static string PatchReportContent(string html, Dictionary<string, object> json)
        {
            var postsJson = json["posts"].ToString();
            var posts = JSON.Deserialize<object[]>(postsJson);
            var htmlBuilder = new StringBuilder("<div>");

            // Start of open all/sort by functions.
            htmlBuilder.AppendLine("<div class=\"reportHeaderFunctions\">");

            // Open all button.
            htmlBuilder.AppendLine("<span id=\"openAllReports\">Open all</span>");

            // Sort by select.
            htmlBuilder.AppendLine("<span>");
            htmlBuilder.AppendLine("Sort by:");

            htmlBuilder.AppendLine("<select id=\"sortBy\">");
            htmlBuilder.AppendLine("<option>Age</option>");
            htmlBuilder.AppendLine("<option>Answers</option>");
            htmlBuilder.AppendLine("<option selected>Close votes</option>");
            htmlBuilder.AppendLine("<option>Score</option>");
            htmlBuilder.AppendLine("<option>Views</option>");
            htmlBuilder.AppendLine("</select>");

            // End of sort by select.
            htmlBuilder.AppendLine("</span>");

            // End of Report functions.
            htmlBuilder.AppendLine("</div>");

            var reports = new List<List<Field>>();
            foreach (var p in posts)
            {
                var fields = GetPostReportFieldsHtml(p.ToString());
                reports.Add(fields);
            }
            var reportsHtml = MergeAllReportHtmlFields(reports);
            htmlBuilder.AppendLine(reportsHtml);

            htmlBuilder.AppendLine("</div>");

            return html.Replace("$REPORT_CONTENT$", htmlBuilder.ToString());
        }

        private static List<List<KeyValuePair<string, double>>> GenerateFieldLayout(List<List<Field>> reports, out List<List<Field>> reportsWithAvg)
        {
            var avgVisCharLengths = new Dictionary<string, double>();

            foreach (var field in reports[0])
            {
                var avg = reports.Average(r => r.Single(f => f.Id == field.Id).Length);
                avgVisCharLengths[field.Id] = avg;
            }

            var processedFields = new List<string>();
            var fieldLayout = new List<List<KeyValuePair<string, double>>>();

            while (processedFields.Count < avgVisCharLengths.Count)
            {
                var currentLayerFields = new List<KeyValuePair<string, double>>();

                var biggestField = avgVisCharLengths
                    .Where(kv => !processedFields.Contains(kv.Key))
                    .OrderByDescending(kv => kv.Value)
                    .First();
                var totalLen = biggestField.Value;

                currentLayerFields.Add(new KeyValuePair<string, double>(biggestField.Key, biggestField.Value));
                processedFields.Add(biggestField.Key);

                while (true)
                {
                    var nextField = avgVisCharLengths
                        .Where(kv => !processedFields.Contains(kv.Key) && totalLen + kv.Value + 3 < maxVisCharsPerReportContainer)
                        .OrderByDescending(kv => kv.Value)
                        .FirstOrDefault();

                    if (nextField.Key == null) break;

                    currentLayerFields.Add(new KeyValuePair<string, double>(nextField.Key, nextField.Value));
                    processedFields.Add(nextField.Key);
                    totalLen += nextField.Value;
                }

                fieldLayout.Add(currentLayerFields);
            }

            var outReports = reports;

            for (var i = 0; i < reports.Count; i++)
            for (var j = 0; j < reports[i].Count; j++)
            {
                outReports[i][j].AvgLength = avgVisCharLengths[reports[i][j].Id];
            }

            reportsWithAvg = outReports;

            return fieldLayout;
        }

        private static string MergeAllReportHtmlFields(List<List<Field>> reports)
        {
            var html = new StringBuilder();
            var updatedReports = new List<List<Field>>();
            var layout = GenerateFieldLayout(reports, out updatedReports);

            foreach (var r in reports)
            {
                html.AppendLine("<div class=\"report\">");

                foreach (var layer in layout)
                {
                    html.AppendLine("<div class=\"reportSubContainer\">");

                    foreach (var layoutField in layer)
                    {
                        var field = r.Single(f => f.Id == layoutField.Key);
                        // Add 5 for extra padding.
                        var widthPercent = field.AvgLength / layer.Sum(x => x.Value + 5);
                        // 900px is the width of a report div.
                        var width = Math.Round(900 * widthPercent);
                        var style = "";

                        if (field.SpecType == "answers")
                        {
                            style = $"width:{width}px;";
                        }
                        else
                        {
                            style = $"style=\"width:{width}px;";
                        }

                        if (layer.Count == 1)
                        {
                            style += "text-align:left;\"";
                        }
                        else
                        {
                            style += "\"";
                        }

                        var fieldHtml = field.Html.Replace("$STYLE$", style);

                        html.AppendLine(fieldHtml);
                    }

                    html.AppendLine("</div>");
                }

                html.AppendLine("</div>");
            }

            return html.ToString();
        }

        private static List<Field> GetPostReportFieldsHtml(string json)
        {
            var jsonFields = JSON.Deserialize<Dictionary<string, object>[]>(json);
            var fields = new List<Field>();

            foreach (var f in jsonFields)
            {
                var html = new StringBuilder();
                var fieldData = f["value"].ToString();
                var fieldName = f["name"].ToString();
                var fieldId = f["id"].ToString();
                var field = new Field
                {
                    Id = fieldId,
                    Name = fieldName
                };

                fieldId = fieldId.Substring(1, fieldId.Length - 2);
                fieldName = fieldName.Substring(1, fieldName.Length - 2);

                if (fieldData.StartsWith("\"") && fieldData.EndsWith("\""))
                {
                    fieldData = fieldData.Substring(1, fieldData.Length - 2);
                }

                if (f.ContainsKey("specialType"))
                {
                    var type = f["specialType"].ToString();
                    type = type.Substring(1, type.Length - 2);

                    switch (type)
                    {
                        case "link":
                        {
                            html.AppendLine("<h3 class=\"reportField\" $STYLE$>");
                            html.Append($"<a target=\"_blank\" href=\"{fieldData}\">");
                            html.Append(fieldName);
                            html.AppendLine("</a>");
                            html.AppendLine("</h3>");
                            field.Length = fieldName.Length;
                            field.SpecType = "link";
                            break;
                        }
                        case "date":
                        {
                            html.AppendLine("<div class=\"reportField\" $STYLE$>");
                            html.AppendLine($"<span class=\"valueName\">{fieldName}</span>: ");
                            html.AppendLine($"<span class=\"timestamp\" data-unixtime=\"{fieldData}\"></span>");
                            html.AppendLine("</div>");
                            // 8 chars is the avg length of the friendly representation of a timestamp.
                            field.Length = fieldName.Length + 8;
                            field.SpecType = "date";
                            break;
                        }
                        case "answers":
                        {
                            if (fieldData.ToUpperInvariant().StartsWith("A"))
                            {
                                html.AppendLine("<div class=\"reportField\" style=\"color:#00AB0B;$STYLE$\" title=\"This question has an accepted answer\">");
                                fieldData = fieldData.Remove(0, 1);
                            }
                            else
                            {
                                html.AppendLine("<div class=\"reportField\" style=\"$STYLE$>");
                            }
                            html.Append($"<span class=\"valueName\">{fieldName}</span>: ");
                            html.AppendLine(fieldData);
                            html.AppendLine("</div>");
                            field.Length = fieldName.Length + fieldData.Length;
                            field.SpecType = "answers";
                            break;
                        }
                    }
                }
                else
                {
                    html.AppendLine("<div class=\"reportField\" $STYLE$>");
                    if (!string.IsNullOrWhiteSpace(fieldName))
                    {
                        html.Append($"<span class=\"valueName\">{fieldName}</span>: ");
                    }
                    html.AppendLine(fieldData);
                    html.AppendLine("</div>");
                    field.Length = fieldName.Length + fieldData.Length;
                }

                field.Html = html.ToString();

                fields.Add(field);
            }

            return fields;
        }
    }
}
