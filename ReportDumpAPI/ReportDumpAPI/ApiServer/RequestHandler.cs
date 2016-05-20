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
            var roomID = "";
            var batchID = "";
            var html = "";

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

                var jObj = JSON.Deserialize<Dictionary<string, object>>(json);
                roomID = jObj["room_id"].ToString();
                batchID = jObj["batch_nr"].ToString();

                html = PageGenerator.GenerateReportPage(json);
            }
            catch (Exception)
            {
                statusCode = 400;
                return "Invalid report JSON received.";
            }

            try
            {
                var dataDir = Path.Combine(Config.ContentDir, roomID);

                if (!Directory.Exists(dataDir))
                {
                    Directory.CreateDirectory(dataDir);
                }

                var file = Path.Combine(dataDir, batchID + ".html");

                File.WriteAllText(file, html);

                statusCode = 200;
                return $"http://{Config.FQD}/{roomID}/{batchID}";
            }
            catch (Exception)
            {
                statusCode = 500;
                return $"An unknown error occurred while processing report {roomID}/{batchID}.";
            }
        }
    }
}
