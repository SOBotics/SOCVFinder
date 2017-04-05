using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using Jil;
using ReportDumpAPI.ReportPage;

namespace ReportDumpAPI.ApiServer
{
    public static class RequestHandler
    {
        public static string HandleReportRequest(byte[] body, bool zipped, out int statusCode)
        {
            var html = "";
            var reportId = "";

            try
            {
                var json = "";

                if (zipped)
                {
                    json = body.Decompress();
                }
                else
                {
                    json = Encoding.UTF8.GetString(body);
                }

                // Remove BOM char if present.
                if (json[0] == 65279)
                {
                    json = json.Remove(0, 1);
                }

                html = PageGenerator.GenerateReportPage(json, out reportId);
            }
            catch (PageGenerator.ParsingException ex)
            {
                statusCode = 400;
                return ex.Message;
            }
            catch (Exception)
            {
                statusCode = 400;
                return "Invalid report JSON received.";
            }

            try
            {
                var dataDir = Path.Combine(Config.ContentDir, "reports");

                if (!Directory.Exists(dataDir))
                {
                    Directory.CreateDirectory(dataDir);
                }

                var file = Path.Combine(dataDir, reportId + ".html");

                File.WriteAllText(file, html);

                statusCode = 200;
                return $"http://{Config.FQD}/{reportId}";
            }
            catch (Exception)
            {
                statusCode = 500;
                return $"An unknown error occurred while processing report {reportId}.";
            }
        }
    }
}
